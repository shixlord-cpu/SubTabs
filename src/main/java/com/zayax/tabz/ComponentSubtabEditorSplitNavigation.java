package com.zayax.tabz;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.SwingConstants;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Opens two tabz files in a normal IDE editor split: anchor file on the left, target on the right.
 */
final class ComponentSubtabEditorSplitNavigation {
    private static final Key<String> TWO_PANE_ARRANGEMENT_KEY =
            Key.create("componentTabz.twoPaneArrangement");

    private ComponentSubtabEditorSplitNavigation() {
    }

    static void restorePersistedActiveSplittab(@NotNull Project project) {
        if (!TabzSettings.getInstance().isTabzEnabled()) {
            return;
        }
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        boolean switchSessionAtSave = registry.switchDedicatedSessionAtLastSave();
        if (switchSessionAtSave && SplittabDedicatedViewService.usesDedicatedBehavior(project)) {
            TabzSettings settings = TabzSettings.getInstance();
            if (!settings.isRestoreSwitchSplittabSessionOnProjectOpen()) {
                SplittabEditorLayoutSnapshot.State normalLayout = registry.normalLayoutBeforeSwitch();
                registry.clearActivePair();
                registry.clearSwitchSessionRestoreHints();
                SplittabDedicatedViewService.getInstance(project).abandonDedicatedViewForNormalLayoutRestore();
                if (normalLayout != null) {
                    ApplicationManager.getApplication().invokeLater(
                            () -> {
                                if (project.isDisposed()) {
                                    return;
                                }
                                SplittabEditorLayoutSnapshot.restore(project, normalLayout);
                            },
                            ModalityState.nonModal(),
                            project.getDisposed()
                    );
                }
                return;
            }
        } else if (switchSessionAtSave) {
            registry.clearSwitchSessionRestoreHints();
        }

        ComponentSubtabEditorSplitRegistry.SplittabPair pair = registry.activePair();
        if (pair == null) {
            return;
        }
        String restorePairId = pair.id();
        boolean restoreSwitchDedicated = switchSessionAtSave
                && SplittabDedicatedViewService.usesDedicatedBehavior(project)
                && TabzSettings.getInstance().isRestoreSwitchSplittabSessionOnProjectOpen();
        ApplicationManager.getApplication().invokeLater(
                () -> {
                    if (project.isDisposed()) {
                        return;
                    }
                    ComponentSubtabEditorSplitRegistry.SplittabPair active =
                            registry.activePair();
                    if (active == null || !restorePairId.equals(active.id())) {
                        return;
                    }
                    if (restoreSwitchDedicated) {
                        SplittabDedicatedViewService dedicated =
                                SplittabDedicatedViewService.getInstance(project);
                        dedicated.restorePersistedDedicatedSession();
                    }
                    activatePair(project, restorePairId);
                    if (restoreSwitchDedicated) {
                        registry.clearSwitchSessionRestoreHints();
                        SplittabDedicatedViewService.getInstance(project).enforceDedicatedWorkspace();
                        SplittabRestoreOverlay.syncProject(project);
                    }
                },
                ModalityState.nonModal(),
                project.getDisposed()
        );
    }

    static void createSplit(
            @NotNull Project project,
            @NotNull VirtualFile initiatingPaneFile,
            @NotNull VirtualFile linkedFile
    ) {
        if (!TabzSettings.getInstance().isSplittabsEnabled()) {
            return;
        }
        if (initiatingPaneFile.equals(linkedFile)) {
            return;
        }
        ComponentSubtabNavigation.runWithSwitchGuard(
                project,
                () -> {
                    SplitFiles splitFiles = resolveSplitFiles(project, initiatingPaneFile, linkedFile);
                    VirtualFile leftFile = splitFiles.leftFile();
                    VirtualFile rightFile = splitFiles.rightFile();
                    closeOtherActiveSplittabSessions(project, null);

                    ComponentSubtabEditorSplitRegistry registry =
                            ComponentSubtabEditorSplitRegistry.getInstance(project);
                    ComponentSubtabEditorSplitRegistry.SplittabPair existing =
                            registry.findPairUnordered(leftFile, rightFile);
                    if (existing != null) {
                        openExistingPairFromCreate(
                                project,
                                existing,
                                initiatingPaneFile,
                                linkedFile
                        );
                        return;
                    }
                    createSplitImpl(
                            project,
                            leftFile,
                            rightFile,
                            initiatingPaneFile,
                            linkedFile
                    );
                }
        );
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        registry.restoreActivePairIfMissing();
        SplittabDedicatedViewService dedicated = SplittabDedicatedViewService.getInstance(project);
        if (dedicated.isDedicatedViewActive()) {
            dedicated.enforceDedicatedWorkspace();
        }
        refreshActiveSplittabPresentationIfNeeded(project);
    }

    private static void closeDedicatedFilesExcept(
            @NotNull Project project,
            @NotNull VirtualFile leftFile,
            @NotNull VirtualFile rightFile
    ) {
        if (!SplittabDedicatedViewService.getInstance(project).isDedicatedViewActive()) {
            return;
        }
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        for (VirtualFile open : manager.getOpenFiles()) {
            if (!leftFile.equals(open) && !rightFile.equals(open)) {
                manager.closeFile(open);
            }
        }
    }

    private static void closeOtherActiveSplittabSessions(
            @NotNull Project project,
            @Nullable ComponentSubtabEditorSplitRegistry.SplittabPair except
    ) {
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair active = registry.activePair();
        if (active == null) {
            return;
        }
        if (except != null && except.id().equals(active.id())) {
            return;
        }
        if (SplittabDedicatedViewService.getInstance(project).isDedicatedViewActive()) {
            if (except == null) {
                return;
            }
            registry.clearActivePair();
            return;
        }
        if (ComponentSubtabEditorSplitMainTab.isActiveSplittabSession(project, active)) {
            ComponentSubtabEditorSplitMainTab.closeSplittabForeground(project, active);
        }
    }

    private static void openExistingPairFromCreate(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair,
            @NotNull VirtualFile initiatingPaneFile,
            @NotNull VirtualFile linkedFile
    ) {
        activatePairImpl(project, pair);
        SplittabDedicatedViewService.getInstance(project).enterDedicatedViewForNewSplittab();
        closeSpareEditorFilesAfterCreate(project, pair, initiatingPaneFile, linkedFile);
    }

    private record SplitFiles(@NotNull VirtualFile leftFile, @NotNull VirtualFile rightFile) {
    }

    private static @NotNull SplitFiles resolveSplitFiles(
            @NotNull Project project,
            @NotNull VirtualFile initiatingPaneFile,
            @NotNull VirtualFile linkedFile
    ) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        EditorWindow initiatingWindow =
                ComponentSubtabEditorLookup.windowForFileOrCurrent(manager, initiatingPaneFile);
        if (initiatingWindow != null
                && ComponentSubtabEditorLookup.isRightSplitPane(manager, initiatingWindow)) {
            return new SplitFiles(linkedFile, initiatingPaneFile);
        }
        return new SplitFiles(initiatingPaneFile, linkedFile);
    }

    static void closeOppositePartnerSide(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair,
            @NotNull VirtualFile switchedFromSideFile
    ) {
        ComponentSubtabEditorSplitMainTab.leaveForegroundFromSide(project, pair, switchedFromSideFile);
    }

    static void restorePresentedPair(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        ComponentSubtabNavigation.runWithSwitchGuard(project, () -> {
            ComponentSubtabEditorSplitRegistry.getInstance(project).setActive(pair.id());
            if (!isSplittabWorkspace(project, pair)) {
                activatePairImpl(project, pair);
                return;
            }
            ComponentSubtabEditorSplitPresentation.applySplittabPresentation(project, pair);
        });
        refreshActiveSplittabPresentationIfNeeded(project);
    }

    static void activatePair(@NotNull Project project, @NotNull String pairId) {
        if (!TabzSettings.getInstance().isSplittabsEnabled()) {
            return;
        }
        ComponentSubtabEditorSplitRegistry registry = ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair pair = registry.findById(pairId);
        if (pair == null) {
            return;
        }
        markPendingPairActivation(project, pairId);
        boolean[] dedicatedSharedLeftSwap = {false};
        ComponentSubtabNavigation.runWithSwitchGuard(
                project,
                () -> {
                    ComponentSubtabEditorSplitRegistry.SplittabPair previous = registry.activePair();
                    closeOtherActiveSplittabSessions(project, pair);
                    dedicatedSharedLeftSwap[0] = activatePairImpl(project, pair, previous);
                }
        );
        SplittabDedicatedViewService dedicated = SplittabDedicatedViewService.getInstance(project);
        if (dedicated.isDedicatedViewActive()) {
            registry.restoreActivePairIfMissing();
            if (!dedicatedSharedLeftSwap[0]) {
                dedicated.enforceDedicatedWorkspace();
                ComponentSubtabEditorSplitRegistry.SplittabPair activeAfter = registry.activePair();
                if (activeAfter != null) {
                    dedicated.scheduleAfterDedicatedPairSwitch(project, activeAfter);
                }
            }
        }
        if (!dedicatedSharedLeftSwap[0]) {
            refreshActiveSplittabPresentationIfNeeded(project);
        }
    }

    private static void refreshActiveSplittabPresentationIfNeeded(@NotNull Project project) {
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair active = registry.activePair();
        if (active == null || !isSplittabWorkspace(project, active)) {
            return;
        }
        ComponentSubtabEditorSplitPresentation.applySplittabPresentation(project, active);
        if (splittabEditorsReady(project, active)) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(
                () -> {
                    if (project.isDisposed()) {
                        return;
                    }
                    ComponentSubtabEditorSplitRegistry.SplittabPair pair = registry.activePair();
                    if (pair == null || !isSplittabWorkspace(project, pair)) {
                        return;
                    }
                    ComponentSubtabEditorSplitPresentation.applySplittabPresentation(project, pair);
                },
                ModalityState.nonModal(),
                project.getDisposed()
        );
    }

    private static boolean splittabEditorsReady(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        return ComponentSubtabsManager.editorsFor(manager, pair.leftFile()).length > 0
                && ComponentSubtabsManager.editorsFor(manager, pair.rightFile()).length > 0;
    }

    /**
     * True only while plugin Splittab chrome (switch bar / pane header) is mounted — not for saved pairs
     * or coincidental native two-pane layouts with the same files.
     */
    static boolean editorSplittabUiEngaged(@NotNull Project project) {
        ComponentSubtabEditorSplitRegistry registry = ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair active = registry.activePair();
        if (active == null) {
            return false;
        }
        return isSplittabChromeShowing(project, active);
    }

    static boolean isSplittabChromeShowing(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (VirtualFile file : java.util.List.of(pair.leftFile(), pair.rightFile())) {
            if (!manager.isFileOpen(file)) {
                continue;
            }
            for (com.intellij.openapi.fileEditor.FileEditor editor : ComponentSubtabsManager.editorsFor(manager, file)) {
                if (editor.getUserData(ComponentSubtabSplittabUi.SPLITTAB_HEADER_KEY) != null
                        || editor.getUserData(ComponentSubtabSplittabUi.SPLITTAB_SWITCH_BAR_KEY) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean shouldPresentSplittabChrome(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        if (!ComponentSubtabEditorSplitMainTab.isForeground(project, pair)) {
            return false;
        }
        if (!isActiveSplittabPairOpen(project, pair)) {
            return false;
        }
        if (SplittabDedicatedViewService.getInstance(project).isDedicatedViewActive()) {
            return true;
        }
        return isSplittabWorkspace(project, pair) || isSplittabChromeShowing(project, pair);
    }

    static void onFileSelected(@NotNull Project project, @NotNull VirtualFile selectedFile) {
        if (ComponentSubtabNavigation.isSwitchInProgress(project)) {
            return;
        }
        if (!editorSplittabUiEngaged(project)) {
            return;
        }
        ComponentSubtabEditorSplitRegistry registry = ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair pair = registry.findByFile(selectedFile);
        if (pair == null) {
            return;
        }

        ComponentSubtabNavigation.runWithSwitchGuard(project, () -> {
            ComponentSubtabEditorSplitRegistry.SplittabPair active = registry.activePair();
            if (active == null || !active.id().equals(pair.id())) {
                return;
            }
            if (!isSplittabWorkspace(project, pair)) {
                disengageSplittabPresentationForNormalEditor(project, pair);
                return;
            }
            syncPartnerInWorkspace(project, pair, selectedFile, false);
            ComponentSubtabEditorSplitPresentation.applySplittabPresentation(project, pair);
        });
    }

    static void onFileOpened(@NotNull Project project, @NotNull VirtualFile file) {
        if (ComponentSubtabNavigation.isSwitchInProgress(project)) {
            return;
        }
        if (!editorSplittabUiEngaged(project)) {
            return;
        }
        ComponentSubtabEditorSplitRegistry registry = ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair pair = registry.findByFile(file);
        if (pair == null) {
            return;
        }
        ComponentSubtabNavigation.runWithSwitchGuard(project, () -> {
            ComponentSubtabEditorSplitRegistry.SplittabPair active = registry.activePair();
            if (active == null || !active.id().equals(pair.id())) {
                return;
            }
            if (!isSplittabWorkspace(project, pair)) {
                disengageSplittabPresentationForNormalEditor(project, pair);
                return;
            }
            ComponentSubtabEditorSplitPresentation.applySplittabPresentation(project, pair);
        });
    }

    /**
     * Datei gehört zu einem gespeicherten Splittab, ist aber nicht im Splittab-Vordergrund geöffnet
     * → normale Tabz-UI, kein automatisches Wiederherstellen des Splits.
     */
    static void disengageSplittabPresentationForNormalEditor(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair active = registry.activePair();
        if (active == null || !active.id().equals(pair.id())) {
            return;
        }
        if (ComponentSubtabEditorSplitMainTab.isForeground(project, pair)) {
            return;
        }
        registry.clearActivePair();
        ComponentSubtabEditorSplitPresentation.pauseSplittabPresentation(project, pair);
    }

    static void disengageIfPairFileOutsideSplittabWorkspace(
            @NotNull Project project,
            @NotNull VirtualFile file
    ) {
        ComponentSubtabEditorSplitRegistry.SplittabPair pair =
                ComponentSubtabEditorSplitRegistry.getInstance(project).findByFile(file);
        if (pair == null || ComponentSubtabEditorSplitNavigation.isSplittabWorkspace(project, pair)) {
            return;
        }
        disengageSplittabPresentationForNormalEditor(project, pair);
    }

    static boolean isSplittabWorkspace(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        EditorWindow leftWindow = ComponentSubtabEditorLookup.findWindowWithFile(manager, pair.leftFile());
        EditorWindow rightWindow = ComponentSubtabEditorLookup.findWindowWithFile(manager, pair.rightFile());
        return leftWindow != null
                && rightWindow != null
                && leftWindow != rightWindow
                && manager.getWindows().length == 2;
    }

    /**
     * Two native IDE editor panes with different selected files, without TabZ splittab chrome active.
     */
    static boolean hasNativeTwoPaneSplitCandidate(@NotNull Project project) {
        if (!TabzSettings.getInstance().isSplittabsEnabled()) {
            return false;
        }
        if (editorSplittabUiEngaged(project)) {
            return false;
        }
        return nativeTwoPaneDistinctSelections(project) != null;
    }

    static @Nullable NativeTwoPaneSelections nativeTwoPaneDistinctSelections(@NotNull Project project) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        EditorWindow[] windows = manager.getWindows();
        if (windows.length != 2) {
            return null;
        }
        VirtualFile first = selectedFileInWindow(windows[0]);
        VirtualFile second = selectedFileInWindow(windows[1]);
        if (first == null || second == null || first.equals(second)) {
            return null;
        }
        EditorWindow leftWindow = windows[0];
        EditorWindow rightWindow = windows[1];
        if (ComponentSubtabEditorLookup.isRightSplitPane(manager, windows[0])) {
            leftWindow = windows[1];
            rightWindow = windows[0];
        }
        VirtualFile leftFile = selectedFileInWindow(leftWindow);
        VirtualFile rightFile = selectedFileInWindow(rightWindow);
        if (leftFile == null || rightFile == null || leftFile.equals(rightFile)) {
            return null;
        }
        return new NativeTwoPaneSelections(leftFile, rightFile);
    }

    static @Nullable VirtualFile nativeTwoPanePartner(
            @NotNull Project project,
            @NotNull VirtualFile file
    ) {
        NativeTwoPaneSelections selections = nativeTwoPaneDistinctSelections(project);
        if (selections == null) {
            return null;
        }
        if (file.equals(selections.leftFile())) {
            return selections.rightFile();
        }
        if (file.equals(selections.rightFile())) {
            return selections.leftFile();
        }
        return null;
    }

    static boolean canCreateSplitPairFromNativeTwoPane(
            @NotNull Project project,
            @NotNull VirtualFile initiatingPaneFile,
            @NotNull VirtualFile linkedFile
    ) {
        if (initiatingPaneFile.equals(linkedFile)) {
            return false;
        }
        if (!TabzSettings.getInstance().isSplittabsEnabled()) {
            return false;
        }
        if (isSplittabLinkedFileInWorkspace(project, linkedFile)
                || isSplittabLinkedFileInWorkspace(project, initiatingPaneFile)) {
            return false;
        }
        NativeTwoPaneSelections selections = nativeTwoPaneDistinctSelections(project);
        if (selections == null) {
            return false;
        }
        return (initiatingPaneFile.equals(selections.leftFile()) && linkedFile.equals(selections.rightFile()))
                || (initiatingPaneFile.equals(selections.rightFile()) && linkedFile.equals(selections.leftFile()));
    }

    private static @Nullable VirtualFile selectedFileInWindow(@NotNull EditorWindow window) {
        if (window.getTabCount() == 0) {
            return null;
        }
        return window.getSelectedFile();
    }

    private static boolean isSplittabLinkedFileInWorkspace(
            @NotNull Project project,
            @NotNull VirtualFile file
    ) {
        if (!editorSplittabUiEngaged(project)) {
            return false;
        }
        ComponentSubtabEditorSplitRegistry.SplittabPair pair =
                ComponentSubtabEditorSplitRegistry.getInstance(project).findByFile(file);
        return pair != null && isSplittabWorkspace(project, pair);
    }

    record NativeTwoPaneSelections(@NotNull VirtualFile leftFile, @NotNull VirtualFile rightFile) {
    }

    enum TwoPaneArrangement {
        SIDE_BY_SIDE,
        STACKED
    }

    static boolean isTwoPaneStackedVertically(@NotNull Project project) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        if (manager.getWindows().length != 2) {
            return false;
        }
        TwoPaneArrangement arrangement = detectTwoPaneArrangement(project, manager);
        return arrangement == TwoPaneArrangement.STACKED;
    }

    static void clearTwoPaneArrangementHint(@NotNull Project project) {
        project.putUserData(TWO_PANE_ARRANGEMENT_KEY, null);
    }

    /**
     * With two editor panes, the split-pair affordance lives on the top pane (stacked) or right pane (side by side).
     */
    static boolean isSplitPairIconHostEditor(@NotNull Project project, @NotNull FileEditor editor) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        EditorWindow[] windows = manager.getWindows();
        if (windows.length != 2) {
            return true;
        }
        EditorWindow host = ComponentSubtabEditorLookup.windowHostingEditor(manager, editor);
        EditorWindow preferred = preferredSplitPairIconHostWindow(project, manager);
        return host != null && host == preferred;
    }

    static @Nullable FileEditor findSplitPairIconHostEditor(@NotNull Project project) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        for (FileEditor editor : manager.getAllEditors()) {
            if (isSplitPairIconHostEditor(project, editor)) {
                return editor;
            }
        }
        return null;
    }

    private static @NotNull EditorWindow preferredSplitPairIconHostWindow(
            @NotNull Project project,
            @NotNull FileEditorManagerEx manager
    ) {
        EditorWindow[] windows = manager.getWindows();
        EditorWindow first = windows[0];
        EditorWindow second = windows[1];
        TwoPaneArrangement arrangement = detectTwoPaneArrangement(project, manager);
        if (arrangement == TwoPaneArrangement.STACKED) {
            return topEditorWindow(first, second);
        }
        return rightEditorWindow(manager, first, second);
    }

    private static @NotNull EditorWindow topEditorWindow(
            @NotNull EditorWindow first,
            @NotNull EditorWindow second
    ) {
        java.awt.Component c0 = first.getTabbedPane().getComponent();
        java.awt.Component c1 = second.getTabbedPane().getComponent();
        if (c0.isShowing() && c1.isShowing()) {
            int y0 = c0.getLocationOnScreen().y;
            int y1 = c1.getLocationOnScreen().y;
            if (y0 != y1) {
                return y0 < y1 ? first : second;
            }
            int bottom0 = y0 + c0.getHeight();
            int bottom1 = y1 + c1.getHeight();
            return bottom0 <= bottom1 ? first : second;
        }
        return first;
    }

    private static @NotNull EditorWindow rightEditorWindow(
            @NotNull FileEditorManagerEx manager,
            @NotNull EditorWindow first,
            @NotNull EditorWindow second
    ) {
        java.awt.Component c0 = first.getTabbedPane().getComponent();
        java.awt.Component c1 = second.getTabbedPane().getComponent();
        if (c0.isShowing() && c1.isShowing()) {
            return c0.getLocationOnScreen().x >= c1.getLocationOnScreen().x ? first : second;
        }
        if (ComponentSubtabEditorLookup.isRightSplitPane(manager, second)) {
            return second;
        }
        if (ComponentSubtabEditorLookup.isRightSplitPane(manager, first)) {
            return first;
        }
        return second;
    }

    static boolean canToggleTwoPaneSplitOrientation(@NotNull Project project) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        return twoPaneSelectionsForOrientationToggle(project) != null
                && detectTwoPaneArrangement(project, manager) != null;
    }

    static boolean toggleTwoPaneSplitOrientation(@NotNull Project project) {
        NativeTwoPaneSelections selections = twoPaneSelectionsForOrientationToggle(project);
        if (selections == null) {
            return false;
        }
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        TwoPaneArrangement arrangement = detectTwoPaneArrangement(project, manager);
        if (arrangement == null) {
            return false;
        }
        TwoPaneArrangement nextArrangement = arrangement == TwoPaneArrangement.SIDE_BY_SIDE
                ? TwoPaneArrangement.STACKED
                : TwoPaneArrangement.SIDE_BY_SIDE;
        if (!reorientTwoPaneToArrangement(project, selections.leftFile(), selections.rightFile(), nextArrangement)) {
            return false;
        }
        rememberTwoPaneArrangement(project, selections.leftFile(), selections.rightFile(), nextArrangement);

        if (editorSplittabUiEngaged(project)) {
            ComponentSubtabEditorSplitRegistry.SplittabPair active =
                    ComponentSubtabEditorSplitRegistry.getInstance(project).activePair();
            if (active != null && isSplittabWorkspace(project, active)) {
                ComponentSubtabEditorSplitPresentation.applySplittabPresentation(project, active);
            }
        }
        return true;
    }

    static void applyPairTwoPaneArrangementIfNeeded(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        if (SplittabDedicatedViewService.getInstance(project).isDedicatedViewActive()) {
            return;
        }
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        if (manager.getWindows().length != 2) {
            return;
        }
        TwoPaneArrangement preferred = preferredTwoPaneArrangement(project, pair.id());
        TwoPaneArrangement current = detectTwoPaneArrangement(project, manager);
        if (current == preferred) {
            rememberTwoPaneArrangement(project, pair.leftFile(), pair.rightFile(), preferred);
            return;
        }
        if (reorientTwoPaneToArrangement(project, pair.leftFile(), pair.rightFile(), preferred)) {
            rememberTwoPaneArrangement(project, pair.leftFile(), pair.rightFile(), preferred);
        }
    }

    private static @NotNull TwoPaneArrangement preferredTwoPaneArrangement(
            @NotNull Project project,
            @NotNull String pairId
    ) {
        String stored = ComponentSubtabEditorSplitRegistry.getInstance(project)
                .pairTwoPaneArrangementOrDefault(pairId);
        try {
            return TwoPaneArrangement.valueOf(stored);
        } catch (IllegalArgumentException ignored) {
            return TwoPaneArrangement.SIDE_BY_SIDE;
        }
    }

    private static void rememberTwoPaneArrangement(
            @NotNull Project project,
            @NotNull VirtualFile leftFile,
            @NotNull VirtualFile rightFile,
            @NotNull TwoPaneArrangement arrangement
    ) {
        project.putUserData(TWO_PANE_ARRANGEMENT_KEY, arrangement.name());
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair pair = registry.findPairUnordered(leftFile, rightFile);
        if (pair != null) {
            registry.setPairTwoPaneArrangement(pair.id(), arrangement.name());
        }
    }

    private static boolean reorientTwoPaneToArrangement(
            @NotNull Project project,
            @NotNull VirtualFile leftFile,
            @NotNull VirtualFile rightFile,
            @NotNull TwoPaneArrangement targetArrangement
    ) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        TwoPaneArrangement current = detectTwoPaneArrangement(project, manager);
        if (current == targetArrangement) {
            return false;
        }
        if (manager.getWindows().length != 2) {
            return false;
        }
        int splitDirection = targetArrangement == TwoPaneArrangement.STACKED
                ? SwingConstants.HORIZONTAL
                : SwingConstants.VERTICAL;

        EditorWindow leftWindow = ComponentSubtabEditorLookup.findWindowWithFile(manager, leftFile);
        EditorWindow rightWindow = ComponentSubtabEditorLookup.findWindowWithFile(manager, rightFile);
        if (leftWindow == null || rightWindow == null || leftWindow == rightWindow) {
            return false;
        }

        ComponentSubtabNavigation.runWithSwitchGuard(project, () -> {
            ensureFileInWindow(manager, leftFile, leftWindow);
            ensureFileInWindow(manager, rightFile, rightWindow);
            java.util.List<VirtualFile> rightPaneFiles = new ArrayList<>(filesInWindow(rightWindow));
            EditorWindow paneToMerge = rightWindow;
            if (paneToMerge != leftWindow && windowInManager(manager, paneToMerge)) {
                paneToMerge.unsplit(true);
            }
            EditorWindow anchor = ComponentSubtabEditorLookup.findWindowWithFile(manager, leftFile);
            if (anchor == null || !windowInManager(manager, anchor)) {
                return;
            }
            anchor.setSelectedComposite(leftFile, true);
            manager.setCurrentWindow(anchor);
            EditorWindow splitPane = anchor.split(splitDirection, true, rightFile, false);
            if (splitPane == null || splitPane == anchor) {
                return;
            }
            ensureFileInWindow(manager, leftFile, anchor);
            ensureFileInWindow(manager, rightFile, splitPane);
            for (VirtualFile file : rightPaneFiles) {
                if (file.equals(leftFile) || file.equals(rightFile)) {
                    continue;
                }
                if (!splitPane.isFileOpen(file)) {
                    openInWindowNonBlocking(manager, file, splitPane, false);
                }
            }
        });
        return true;
    }

    private static @Nullable NativeTwoPaneSelections twoPaneSelectionsForOrientationToggle(
            @NotNull Project project
    ) {
        if (!TabzSettings.getInstance().isSplittabsEnabled()) {
            return null;
        }
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        if (manager.getWindows().length != 2) {
            return null;
        }
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        if (!registry.hasSavedSplittabs()) {
            return nativeTwoPaneDistinctSelections(project);
        }
        if (!editorSplittabUiEngaged(project)) {
            return null;
        }
        ComponentSubtabEditorSplitRegistry.SplittabPair active = registry.activePair();
        if (active == null || !isSplittabWorkspace(project, active)) {
            return null;
        }
        return new NativeTwoPaneSelections(active.leftFile(), active.rightFile());
    }

    private static @Nullable TwoPaneArrangement detectTwoPaneArrangement(
            @NotNull Project project,
            @NotNull FileEditorManagerEx manager
    ) {
        EditorWindow[] windows = manager.getWindows();
        if (windows.length != 2) {
            clearTwoPaneArrangementHint(project);
            return null;
        }
        java.awt.Component first = windows[0].getTabbedPane().getComponent();
        java.awt.Component second = windows[1].getTabbedPane().getComponent();
        if (first.isShowing() && second.isShowing()) {
            Point firstOrigin = first.getLocationOnScreen();
            Point secondOrigin = second.getLocationOnScreen();
            int deltaX = Math.abs(firstOrigin.x - secondOrigin.x);
            int deltaY = Math.abs(firstOrigin.y - secondOrigin.y);
            TwoPaneArrangement fromGeometry = deltaX >= deltaY
                    ? TwoPaneArrangement.SIDE_BY_SIDE
                    : TwoPaneArrangement.STACKED;
            project.putUserData(TWO_PANE_ARRANGEMENT_KEY, fromGeometry.name());
            if (editorSplittabUiEngaged(project)) {
                ComponentSubtabEditorSplitRegistry.SplittabPair active =
                        ComponentSubtabEditorSplitRegistry.getInstance(project).activePair();
                if (active != null && isSplittabWorkspace(project, active)) {
                    ComponentSubtabEditorSplitRegistry.getInstance(project)
                            .setPairTwoPaneArrangement(active.id(), fromGeometry.name());
                }
            }
            return fromGeometry;
        }
        String stored = project.getUserData(TWO_PANE_ARRANGEMENT_KEY);
        if (stored != null) {
            try {
                return TwoPaneArrangement.valueOf(stored);
            } catch (IllegalArgumentException ignored) {
                clearTwoPaneArrangementHint(project);
            }
        }
        return TwoPaneArrangement.SIDE_BY_SIDE;
    }

    /**
     * Mixed + „Zum Split Pair wechseln“: kein aktives Pair, aber genau zwei Editor-Panes im normalen
     * IDE-Split und die jeweils fokussierte Datei im anderen Pane ist der Partner — Haupttab-Wechsel
     * aktiviert das gespeicherte Split Pair.
     */
    static boolean tryActivateSavedPairFromOrdinaryTwoPaneMainTabSelection(
            @NotNull Project project,
            @NotNull VirtualFile selectedFile,
            @Nullable VirtualFile previousFile
    ) {
        if (ComponentSubtabFileEditorListener.isAnyEditorFileClosing(project)) {
            return false;
        }
        TabzSettings settings = TabzSettings.getInstance();
        if (!settings.isSplittabsEnabled()
                || settings.getSplittabBehaviorMode() != SplittabBehaviorMode.INTEGRATED
                || settings.getSplittabOtherPairFileMode() != SplittabOtherPairFileMode.SWITCH_TO_PAIR) {
            return false;
        }
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        if (registry.activePair() != null || editorSplittabUiEngaged(project)) {
            return false;
        }
        if (previousFile == null
                || previousFile.equals(selectedFile)
                || ComponentSubtabFileEditorListener.isClosingEditorFile(project, previousFile)
                || ComponentSubtabFileEditorListener.isClosingEditorFile(project, selectedFile)) {
            return false;
        }
        if (!isUserInitiatedEditorSelection(project, selectedFile)) {
            return false;
        }
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        if (!manager.isFileOpen(previousFile)) {
            return false;
        }
        EditorWindow previousPane = ComponentSubtabEditorLookup.findWindowWithFile(manager, previousFile);
        EditorWindow selectedPane = ComponentSubtabEditorLookup.findWindowWithFile(manager, selectedFile);
        if (previousPane == null
                || selectedPane == null
                || previousPane == selectedPane) {
            return false;
        }
        if (manager.getWindows().length != 2) {
            return false;
        }
        EditorWindow current = manager.getCurrentWindow();
        if (current == null || !current.isFileOpen(selectedFile)) {
            return false;
        }
        EditorWindow otherPane = null;
        for (EditorWindow window : manager.getWindows()) {
            if (window != current && window.getTabCount() > 0) {
                otherPane = window;
                break;
            }
        }
        if (otherPane == null) {
            return false;
        }
        VirtualFile otherSelected = otherPane.getSelectedFile();
        if (otherSelected == null || otherSelected.equals(selectedFile)) {
            return false;
        }
        ComponentSubtabEditorSplitRegistry.SplittabPair pair =
                registry.findPairUnordered(selectedFile, otherSelected);
        if (pair == null) {
            return false;
        }
        if (!manager.isFileOpen(pair.leftFile()) || !manager.isFileOpen(pair.rightFile())) {
            return false;
        }
        EditorWindow paneForSelected =
                ComponentSubtabEditorLookup.findWindowWithFile(manager, selectedFile);
        EditorWindow paneForOther =
                ComponentSubtabEditorLookup.findWindowWithFile(manager, otherSelected);
        if (paneForSelected == null
                || paneForOther == null
                || paneForSelected == paneForOther) {
            return false;
        }
        if (isSplittabChromeShowing(project, pair)) {
            return false;
        }
        for (ComponentSubtabEditorSplitRegistry.SplittabPair saved : registry.all()) {
            if (isSplittabChromeShowing(project, saved)) {
                return false;
            }
        }
        activatePair(project, pair.id());
        return true;
    }

    /** Both pair files are open — splittab session is active even if pane layout is not exactly two windows. */
    static boolean isActiveSplittabPairOpen(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        return manager.isFileOpen(pair.leftFile()) && manager.isFileOpen(pair.rightFile());
    }

    /**
     * Opening a file of another saved split pair while a split pair is shown: switches to that pair when
     * configured. Returns true if the switch was scheduled.
     */
    static boolean switchToOtherPairForOpenedFile(@NotNull Project project, @NotNull VirtualFile file) {
        TabzSettings settings = TabzSettings.getInstance();
        if (!settings.isSplittabsEnabled()
                || settings.getSplittabOtherPairFileMode() != SplittabOtherPairFileMode.SWITCH_TO_PAIR) {
            return false;
        }
        if (!SplittabDedicatedViewService.getInstance(project).isDedicatedViewActive()
                && !editorSplittabUiEngaged(project)) {
            return false;
        }
        ComponentSubtabEditorSplitRegistry registry = ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair active = registry.activePair();
        if (active != null && active.covers(file)) {
            return false;
        }
        PendingPairActivation pending = project.getUserData(PENDING_PAIR_ACTIVATION);
        if (pending != null && System.currentTimeMillis() <= pending.deadlineEpochMs()) {
            return false;
        }
        ComponentSubtabEditorSplitRegistry.SplittabPair target = null;
        for (ComponentSubtabEditorSplitRegistry.SplittabPair pair : registry.all()) {
            if (pair.covers(file)) {
                target = pair;
                break;
            }
        }
        if (target == null) {
            return false;
        }
        rememberIncomingOtherPairFile(project, file);
        String pairId = target.id();
        ApplicationManager.getApplication().invokeLater(
                () -> activateOtherPairAndFocus(project, pairId, file, OTHER_PAIR_ACTIVATION_ATTEMPTS),
                ModalityState.nonModal(),
                project.getDisposed()
        );
        return true;
    }

    private static final int OTHER_PAIR_ACTIVATION_ATTEMPTS = 25;
    private static final int OTHER_PAIR_ACTIVATION_RETRY_MS = 40;
    private static final long OTHER_PAIR_RELOCATION_WINDOW_MS = 3_000;
    private static final com.intellij.openapi.util.Key<OtherPairRelocation> OTHER_PAIR_RELOCATION =
            com.intellij.openapi.util.Key.create("tabz.otherPairRelocation");
    private static final com.intellij.openapi.util.Key<PendingPairActivation> PENDING_PAIR_ACTIVATION =
            com.intellij.openapi.util.Key.create("tabz.pendingPairActivation");
    private static final long PENDING_PAIR_ACTIVATION_MS = 750;

    private record OtherPairRelocation(@NotNull Set<VirtualFile> files, long deadline) {
    }

    private record PendingPairActivation(@NotNull String pairId, long deadlineEpochMs) {
    }

    /**
     * While {@link #activatePair} rearranges panes, the platform may emit selection events for
     * background tabs of other saved pairs; those must not trigger another pair activation.
     */
    /**
     * True when {@code newFile} is the selected tab in the focused editor pane (not a stale event from
     * the other pane while a pair switch is settling).
     */
    static boolean isUserInitiatedEditorSelection(
            @NotNull Project project,
            @NotNull VirtualFile newFile
    ) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        EditorWindow current = manager.getCurrentWindow();
        return current != null && newFile.equals(current.getSelectedFile());
    }

    static boolean shouldIgnoreCrossPairSelection(
            @NotNull Project project,
            @NotNull String targetPairId
    ) {
        PendingPairActivation pending = project.getUserData(PENDING_PAIR_ACTIVATION);
        if (pending == null) {
            return false;
        }
        if (System.currentTimeMillis() > pending.deadlineEpochMs()) {
            project.putUserData(PENDING_PAIR_ACTIVATION, null);
            return false;
        }
        return !pending.pairId().equals(targetPairId);
    }

    private static void markPendingPairActivation(@NotNull Project project, @NotNull String pairId) {
        project.putUserData(
                PENDING_PAIR_ACTIVATION,
                new PendingPairActivation(pairId, System.currentTimeMillis() + PENDING_PAIR_ACTIVATION_MS)
        );
    }

    /**
     * Activating another pair moves the remaining open files into background tabs without awaiting them,
     * so their fileOpened arrives after the switch guard and must not count as an external open.
     */
    static boolean consumeOtherPairRelocatedFile(@NotNull Project project, @NotNull VirtualFile file) {
        OtherPairRelocation relocation = project.getUserData(OTHER_PAIR_RELOCATION);
        if (relocation == null) {
            return false;
        }
        if (System.currentTimeMillis() > relocation.deadline()) {
            project.putUserData(OTHER_PAIR_RELOCATION, null);
            return false;
        }
        return relocation.files().remove(file);
    }

    private static void rememberIncomingOtherPairFile(@NotNull Project project, @NotNull VirtualFile file) {
        OtherPairRelocation existing = project.getUserData(OTHER_PAIR_RELOCATION);
        Set<VirtualFile> files = existing != null && System.currentTimeMillis() <= existing.deadline()
                ? existing.files()
                : new LinkedHashSet<>();
        files.add(file);
        project.putUserData(
                OTHER_PAIR_RELOCATION,
                new OtherPairRelocation(files, System.currentTimeMillis() + OTHER_PAIR_RELOCATION_WINDOW_MS)
        );
    }

    private static void rememberOtherPairRelocatedFiles(@NotNull Project project, @NotNull String pairId) {
        ComponentSubtabEditorSplitRegistry.SplittabPair target =
                ComponentSubtabEditorSplitRegistry.getInstance(project).findById(pairId);
        Set<VirtualFile> files = new java.util.HashSet<>(
                collectOpenFiles(FileEditorManagerEx.getInstanceEx(project)));
        if (target != null) {
            files.remove(target.leftFile());
            files.remove(target.rightFile());
        }
        project.putUserData(
                OTHER_PAIR_RELOCATION,
                new OtherPairRelocation(files, System.currentTimeMillis() + OTHER_PAIR_RELOCATION_WINDOW_MS)
        );
    }

    /** Pair files open without awaiting their editors, so activation is retried until both panes exist. */
    private static void activateOtherPairAndFocus(
            @NotNull Project project,
            @NotNull String pairId,
            @NotNull VirtualFile file,
            int attemptsLeft
    ) {
        if (project.isDisposed()) {
            return;
        }
        rememberOtherPairRelocatedFiles(project, pairId);
        activatePair(project, pairId);
        ComponentSubtabEditorSplitRegistry.SplittabPair active =
                ComponentSubtabEditorSplitRegistry.getInstance(project).activePair();
        boolean activated = active != null
                && active.id().equals(pairId)
                && isActiveSplittabPairOpen(project, active)
                && isSplittabWorkspace(project, active);
        if (!activated && attemptsLeft > 0) {
            javax.swing.Timer retry = new javax.swing.Timer(
                    OTHER_PAIR_ACTIVATION_RETRY_MS,
                    event -> ApplicationManager.getApplication().invokeLater(
                            () -> activateOtherPairAndFocus(project, pairId, file, attemptsLeft - 1),
                            ModalityState.nonModal(),
                            project.getDisposed()
                    )
            );
            retry.setRepeats(false);
            retry.start();
            return;
        }
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        EditorWindow window = ComponentSubtabEditorLookup.findWindowWithFile(manager, file);
        if (window != null) {
            window.setSelectedComposite(file, true);
            manager.setCurrentWindow(window);
        }
    }

    static boolean blocksExternalFileOpen(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair,
            @NotNull VirtualFile targetFile
    ) {
        if (!editorSplittabUiEngaged(project)) {
            return false;
        }
        if (pair.covers(targetFile)) {
            return false;
        }
        ComponentSubtabEditorSplitRegistry.SplittabPair active =
                ComponentSubtabEditorSplitRegistry.getInstance(project).activePair();
        if (TabzSettings.getInstance().getSplittabOtherPairFileMode() == SplittabOtherPairFileMode.SWITCH_TO_PAIR
                && active != null
                && !active.covers(targetFile)
                && ComponentSubtabEditorSplitRegistry.getInstance(project).findByFile(targetFile) != null) {
            return false;
        }
        return active != null
                && active.id().equals(pair.id())
                && isActiveSplittabPairOpen(project, pair);
    }

    private static void createSplitImpl(
            @NotNull Project project,
            @NotNull VirtualFile leftFile,
            @NotNull VirtualFile rightFile,
            @NotNull VirtualFile initiatingPaneFile,
            @NotNull VirtualFile linkedFile
    ) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        java.util.Map<VirtualFile, Boolean> priorRightSide = snapshotRightSideByFile(manager);
        EditorWindow leftWindow = ComponentSubtabEditorLookup.windowForFileOrCurrent(manager, leftFile);
        if (leftWindow == null) {
            return;
        }

        if (!leftWindow.isFileOpen(leftFile)) {
            openInWindowNonBlocking(manager, leftFile, leftWindow, true);
        }
        leftWindow.setSelectedComposite(leftFile, true);
        manager.setCurrentWindow(leftWindow);

        EditorWindow rightWindow = ComponentSubtabEditorLookup.findWindowWithFile(manager, rightFile);
        if (rightWindow == null || rightWindow == leftWindow) {
            rightWindow = siblingWindow(manager, leftWindow);
        }
        TwoPaneArrangement preferredArrangement = TwoPaneArrangement.SIDE_BY_SIDE;
        if (rightWindow == null || rightWindow == leftWindow) {
            rightWindow = splitRightNonBlocking(
                    manager,
                    leftWindow,
                    leftFile,
                    rightFile,
                    true,
                    preferredArrangement
            );
        }
        if (rightWindow == null || rightWindow == leftWindow) {
            return;
        }

        ensureFileInWindow(manager, leftFile, leftWindow);
        ensureFileInWindow(manager, rightFile, rightWindow);
        rightWindow.setSelectedComposite(rightFile, true);
        manager.setCurrentWindow(rightWindow);

        Set<VirtualFile> backgroundFiles = new LinkedHashSet<>(collectOpenFiles(manager));
        backgroundFiles.remove(leftFile);
        backgroundFiles.remove(rightFile);

        consolidateToTwoPanes(
                manager,
                leftWindow,
                rightWindow,
                leftFile,
                rightFile,
                priorRightSide
        );
        repairSplitPaneLayoutIfNeeded(
                manager,
                leftFile,
                rightFile,
                priorRightSide
        );
        leftWindow = ComponentSubtabEditorLookup.findWindowWithFile(manager, leftFile);
        rightWindow = ComponentSubtabEditorLookup.findWindowWithFile(manager, rightFile);
        if (leftWindow != null && rightWindow != null && leftWindow != rightWindow
                && !SplittabDedicatedViewService.getInstance(project).skipBackgroundRestoreOnCreate()) {
            restoreBackgroundFiles(
                    manager,
                    leftWindow,
                    rightWindow,
                    backgroundFiles,
                    priorRightSide
            );
        }

        ComponentSubtabEditorSplitRegistry.SplittabPair pair =
                ComponentSubtabEditorSplitRegistry.getInstance(project).register(leftFile, rightFile);
        TwoPaneArrangement detected = detectTwoPaneArrangement(project, manager);
        if (detected != null) {
            rememberTwoPaneArrangement(project, leftFile, rightFile, detected);
        }
        applyPairTwoPaneArrangementIfNeeded(project, pair);
        ComponentSubtabEditorSplitPresentation.applySplittabPresentation(project, pair);
        closeSpareEditorFilesAfterCreate(project, pair, initiatingPaneFile, linkedFile);
        focusSplittabPair(project, pair, rightFile);
        SplittabDedicatedViewService dedicated = SplittabDedicatedViewService.getInstance(project);
        dedicated.enterDedicatedViewForNewSplittab();
        dedicated.enforceDedicatedWorkspace();
        closeDedicatedFilesExcept(project, pair.leftFile(), pair.rightFile());
    }

    private static void focusSplittabPair(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair,
            @NotNull VirtualFile focusFile
    ) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        EditorWindow focusWindow = ComponentSubtabEditorLookup.findWindowWithFile(manager, focusFile);
        if (focusWindow == null) {
            return;
        }
        focusWindow.setSelectedComposite(focusFile, true);
        manager.setCurrentWindow(focusWindow);
    }

    private static void closeSpareEditorFilesAfterCreate(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair,
            @NotNull VirtualFile initiatingPaneFile,
            @NotNull VirtualFile linkedFile
    ) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        for (VirtualFile spare : List.of(initiatingPaneFile, linkedFile)) {
            if (pair.covers(spare)) {
                continue;
            }
            if (manager.isFileOpen(spare)) {
                manager.closeFile(spare);
            }
        }
    }

    private static @NotNull java.util.Map<VirtualFile, Boolean> snapshotRightSideByFile(
            @NotNull FileEditorManagerEx manager
    ) {
        java.util.Map<VirtualFile, Boolean> priorRightSide = new java.util.HashMap<>();
        for (EditorWindow window : manager.getWindows()) {
            boolean rightSide = ComponentSubtabEditorLookup.isRightSplitPane(manager, window);
            for (VirtualFile file : filesInWindow(window)) {
                priorRightSide.put(file, rightSide);
            }
        }
        return priorRightSide;
    }

    private static void activatePairImpl(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        activatePairImpl(project, pair, null);
    }

    private static boolean activatePairImpl(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair,
            @Nullable ComponentSubtabEditorSplitRegistry.SplittabPair previousPair
    ) {
        ComponentSubtabEditorSplitRegistry.getInstance(project).setActive(pair.id());
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        SplittabDedicatedViewService dedicated = SplittabDedicatedViewService.getInstance(project);
        if (dedicated.isDedicatedViewActive()) {
            if (previousPair != null && !previousPair.id().equals(pair.id())) {
                return swapDedicatedPartnerPane(project, pair, previousPair, manager, dedicated);
            }
            activatePairInDedicatedView(project, pair, manager);
            return false;
        }

        EditorWindow leftWindow = ComponentSubtabEditorLookup.findWindowWithFile(manager, pair.leftFile());
        if (leftWindow == null) {
            openInWindowNonBlocking(manager, pair.leftFile(), null, false);
            leftWindow = ComponentSubtabEditorLookup.findWindowWithFile(manager, pair.leftFile());
        }
        if (leftWindow == null) {
            return false;
        }

        EditorWindow rightWindow = ComponentSubtabEditorLookup.findWindowWithFile(manager, pair.rightFile());
        if (rightWindow == null || rightWindow == leftWindow) {
            rightWindow = siblingWindow(manager, leftWindow);
        }
        TwoPaneArrangement preferredArrangement = preferredTwoPaneArrangement(project, pair.id());
        if (rightWindow == null || rightWindow == leftWindow) {
            rightWindow = splitRightNonBlocking(
                    manager,
                    leftWindow,
                    pair.leftFile(),
                    pair.rightFile(),
                    false,
                    preferredArrangement
            );
        }
        if (rightWindow == null || rightWindow == leftWindow) {
            return false;
        }

        java.util.Map<VirtualFile, Boolean> priorRightSide = snapshotRightSideByFile(manager);
        ensurePairFilesOnCorrectSides(manager, pair);
        EditorWindow physicalLeft = physicalLeftWindow(manager);
        EditorWindow physicalRight = physicalRightWindow(manager, physicalLeft);
        if (physicalLeft == null || physicalRight == null || physicalLeft == physicalRight) {
            return false;
        }
        consolidateToTwoPanes(
                manager,
                physicalLeft,
                physicalRight,
                pair.leftFile(),
                pair.rightFile(),
                priorRightSide
        );
        repairSplitPaneLayoutIfNeeded(
                manager,
                pair.leftFile(),
                pair.rightFile(),
                priorRightSide
        );

        ensurePairFilesOnCorrectSides(manager, pair);
        physicalLeft = physicalLeftWindow(manager);
        physicalRight = physicalRightWindow(manager, physicalLeft);
        if (physicalLeft == null || physicalRight == null || physicalLeft == physicalRight) {
            return false;
        }

        physicalLeft.setSelectedComposite(pair.leftFile(), false);
        physicalRight.setSelectedComposite(pair.rightFile(), true);
        if (windowInManager(manager, physicalRight)) {
            manager.setCurrentWindow(physicalRight);
        }

        applyPairTwoPaneArrangementIfNeeded(project, pair);
        ComponentSubtabEditorSplitPresentation.applySplittabPresentation(project, pair);
        focusSplittabPair(project, pair, pair.rightFile());
        SplittabDedicatedViewService.getInstance(project).enforceDedicatedWorkspace();
        return false;
    }

    private static boolean swapDedicatedPartnerPane(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair previousPair,
            @NotNull FileEditorManagerEx manager,
            @NotNull SplittabDedicatedViewService dedicated
    ) {
        boolean[] sharedLeftFastPath = {false};
        dedicated.runSuppressingDedicatedCloseHandler(() -> {
            if (!previousPair.leftFile().equals(pair.leftFile())) {
                activatePairInDedicatedView(project, pair, manager);
                return;
            }
            EditorWindow leftWindow = dedicatedLeftPane(manager);
            EditorWindow rightWindow = dedicatedRightPane(
                    manager,
                    leftWindow != null ? leftWindow : manager.getWindows()[0]
            );
            if (leftWindow == null || rightWindow == null || leftWindow == rightWindow) {
                activatePairInDedicatedView(project, pair, manager);
                return;
            }
            sharedLeftFastPath[0] = true;
            VirtualFile previousPartner = previousPair.rightFile();
            if (!rightWindow.isFileOpen(pair.rightFile())) {
                ComponentSubtabFileEditorListener.markCoalescedTabzPlatformOpen(project, pair.rightFile());
                manager.setCurrentWindow(rightWindow);
                // Awaiting the composite here starves the background editor build (EDT has priority)
                // and freezes the IDE for seconds; the chrome is attached in fileOpened instead.
                InternalPlatformBridge.openFile(
                        manager,
                        pair.rightFile(),
                        rightWindow,
                        ComponentSubtabNavigation.nonBlockingOpenOptions(rightWindow, previousPartner, true)
                );
            }
            if (rightWindow.isFileOpen(pair.rightFile())) {
                rightWindow.setSelectedComposite(pair.rightFile(), true);
            }
            if (!previousPartner.equals(pair.rightFile()) && rightWindow.isFileOpen(previousPartner)) {
                rightWindow.closeFile(previousPartner);
            }
            closeOtherTabsInWindow(rightWindow, pair.rightFile());
            leftWindow.setSelectedComposite(pair.leftFile(), false);
            manager.setCurrentWindow(rightWindow);
            ComponentSubtabEditorSplitPresentation.refreshDedicatedPairSwitch(project, pair);
        });
        return sharedLeftFastPath[0];
    }

    private static void activatePairInDedicatedView(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair,
            @NotNull FileEditorManagerEx manager
    ) {
        SplittabDedicatedViewService dedicated = SplittabDedicatedViewService.getInstance(project);
        dedicated.runSuppressingDedicatedCloseHandler(() -> {
            EditorWindow leftWindow = dedicatedLeftPane(manager);
            if (leftWindow == null) {
                openInWindowNonBlocking(manager, pair.leftFile(), null, false);
                leftWindow = dedicatedLeftPane(manager);
            }
            if (leftWindow == null) {
                return;
            }
            ensureFileInWindow(manager, pair.leftFile(), leftWindow);
            EditorWindow rightWindow = dedicatedRightPane(manager, leftWindow);
            TwoPaneArrangement preferredArrangement = preferredTwoPaneArrangement(project, pair.id());
            if (rightWindow == null || rightWindow == leftWindow) {
                rightWindow = splitRightNonBlocking(
                        manager,
                        leftWindow,
                        pair.leftFile(),
                        pair.rightFile(),
                        false,
                        preferredArrangement
                );
            }
            if (rightWindow == null || rightWindow == leftWindow) {
                return;
            }

            ensureFileInWindow(manager, pair.leftFile(), leftWindow);
            ensureFileInWindow(manager, pair.rightFile(), rightWindow);
            closeOtherTabsInWindow(leftWindow, pair.leftFile());
            closeOtherTabsInWindow(rightWindow, pair.rightFile());
            closeFileOutsideWindow(manager, pair.leftFile(), leftWindow);
            closeFileOutsideWindow(manager, pair.rightFile(), rightWindow);

            leftWindow.setSelectedComposite(pair.leftFile(), false);
            rightWindow.setSelectedComposite(pair.rightFile(), true);
            if (windowInManager(manager, rightWindow)) {
                manager.setCurrentWindow(rightWindow);
            }

            dedicatedEnsureBothPairFilesOpen(manager, pair, leftWindow, rightWindow);

            applyPairTwoPaneArrangementIfNeeded(project, pair);
            ComponentSubtabEditorSplitPresentation.applySplittabPresentation(project, pair);
            focusSplittabPair(project, pair, pair.rightFile());
            dedicated.enforceDedicatedWorkspace();
        });
    }

    private static void dedicatedEnsureBothPairFilesOpen(
            @NotNull FileEditorManagerEx manager,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair,
            @NotNull EditorWindow leftWindow,
            @NotNull EditorWindow rightWindow
    ) {
        ensureFileInWindow(manager, pair.leftFile(), leftWindow);
        ensureFileInWindow(manager, pair.rightFile(), rightWindow);
        closeOtherTabsInWindow(leftWindow, pair.leftFile());
        closeOtherTabsInWindow(rightWindow, pair.rightFile());
        closeFileOutsideWindow(manager, pair.leftFile(), leftWindow);
        closeFileOutsideWindow(manager, pair.rightFile(), rightWindow);
    }

    private static @Nullable EditorWindow dedicatedLeftPane(@NotNull FileEditorManagerEx manager) {
        EditorWindow fallback = null;
        for (EditorWindow window : manager.getWindows()) {
            if (ComponentSubtabEditorLookup.isRightSplitPane(manager, window)) {
                continue;
            }
            if (window.getTabCount() > 0) {
                return window;
            }
            fallback = window;
        }
        return fallback;
    }

    private static @Nullable EditorWindow dedicatedRightPane(
            @NotNull FileEditorManagerEx manager,
            @NotNull EditorWindow leftWindow
    ) {
        for (EditorWindow window : manager.getWindows()) {
            if (window != leftWindow && ComponentSubtabEditorLookup.isRightSplitPane(manager, window)) {
                return window;
            }
        }
        return siblingWindow(manager, leftWindow);
    }

    private static void ensurePairFilesOnCorrectSides(
            @NotNull FileEditorManagerEx manager,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        EditorWindow physicalLeft = physicalLeftWindow(manager);
        EditorWindow physicalRight = physicalRightWindow(manager, physicalLeft);
        if (physicalLeft == null || physicalRight == null || physicalLeft == physicalRight) {
            return;
        }
        ensureFileInWindow(manager, pair.leftFile(), physicalLeft);
        ensureFileInWindow(manager, pair.rightFile(), physicalRight);
    }

    private static @Nullable EditorWindow physicalLeftWindow(@NotNull FileEditorManagerEx manager) {
        for (EditorWindow window : manager.getWindows()) {
            if (!ComponentSubtabEditorLookup.isRightSplitPane(manager, window)) {
                return window;
            }
        }
        EditorWindow[] windows = manager.getWindows();
        return windows.length > 0 ? windows[0] : null;
    }

    private static @Nullable EditorWindow physicalRightWindow(
            @NotNull FileEditorManagerEx manager,
            @Nullable EditorWindow physicalLeft
    ) {
        if (physicalLeft == null) {
            return null;
        }
        for (EditorWindow window : manager.getWindows()) {
            if (window != physicalLeft && ComponentSubtabEditorLookup.isRightSplitPane(manager, window)) {
                return window;
            }
        }
        return siblingWindow(manager, physicalLeft);
    }

    private static @Nullable EditorWindow siblingWindow(
            @NotNull FileEditorManagerEx manager,
            @NotNull EditorWindow leftWindow
    ) {
        for (EditorWindow window : manager.getWindows()) {
            if (window != leftWindow && window.getTabCount() > 0) {
                return window;
            }
        }
        return null;
    }

    private static boolean windowInManager(
            @NotNull FileEditorManagerEx manager,
            @NotNull EditorWindow window
    ) {
        for (EditorWindow candidate : manager.getWindows()) {
            if (candidate == window) {
                return true;
            }
        }
        return false;
    }

    private static void ensureFileInWindow(
            @NotNull FileEditorManagerEx manager,
            @NotNull VirtualFile file,
            @NotNull EditorWindow window
    ) {
        if (!window.isFileOpen(file)) {
            openInWindowNonBlocking(manager, file, window, false);
        }
        closeFileOutsideWindow(manager, file, window);
    }

    /**
     * Opens without awaiting the composite: a blocking open on the EDT starves the background editor
     * build (the EDT has priority) and freezes the IDE for seconds. Chrome is attached in fileOpened.
     */
    private static void openInWindowNonBlocking(
            @NotNull FileEditorManagerEx manager,
            @NotNull VirtualFile file,
            @Nullable EditorWindow window,
            boolean requestFocus
    ) {
        if (window == null) {
            window = manager.getCurrentWindow();
        }
        if (window == null) {
            manager.openFile(file, requestFocus);
            return;
        }
        InternalPlatformBridge.openFile(
                manager,
                file,
                window,
                ComponentSubtabNavigation.nonBlockingOpenOptions(requestFocus, true)
        );
    }

    /** {@link EditorWindow#split} awaits the editor of the file it opens, so split on the open left file. */
    private static @Nullable EditorWindow splitRightNonBlocking(
            @NotNull FileEditorManagerEx manager,
            @NotNull EditorWindow leftWindow,
            @NotNull VirtualFile leftFile,
            @NotNull VirtualFile rightFile,
            boolean focusRight,
            @NotNull TwoPaneArrangement arrangement
    ) {
        int splitDirection = arrangement == TwoPaneArrangement.STACKED
                ? SwingConstants.HORIZONTAL
                : SwingConstants.VERTICAL;
        if (!leftWindow.isFileOpen(leftFile)) {
            return leftWindow.split(splitDirection, true, rightFile, focusRight);
        }
        leftWindow.setSelectedComposite(leftFile, false);
        manager.setCurrentWindow(leftWindow);
        EditorWindow rightWindow = leftWindow.split(splitDirection, true, leftFile, false);
        if (rightWindow == null || rightWindow == leftWindow) {
            return rightWindow;
        }
        openInWindowNonBlocking(manager, rightFile, rightWindow, focusRight);
        if (rightWindow.isFileOpen(rightFile) && rightWindow.isFileOpen(leftFile)) {
            rightWindow.closeFile(leftFile);
        }
        return rightWindow;
    }

    private static void syncPartnerInWorkspace(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair,
            @NotNull VirtualFile selectedFile,
            boolean focusPartner
    ) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        VirtualFile partner = pair.partnerOf(selectedFile);
        EditorWindow partnerWindow = selectedFile.equals(pair.leftFile())
                ? ComponentSubtabEditorLookup.findWindowWithFile(manager, pair.rightFile())
                : ComponentSubtabEditorLookup.findWindowWithFile(manager, pair.leftFile());
        if (partnerWindow == null) {
            return;
        }
        if (!partnerWindow.isFileOpen(partner)) {
            InternalPlatformBridge.openFileWithProviders(manager, partner, false, partnerWindow);
        }
        partnerWindow.setSelectedComposite(partner, focusPartner);
    }

    /**
     * Keeps exactly the split pair of panes and moves every other open file into the left pane as a
     * background tab.
     */
    private static void consolidateToTwoPanes(
            @NotNull FileEditorManagerEx manager,
            @NotNull EditorWindow leftWindow,
            @NotNull EditorWindow rightWindow,
            @NotNull VirtualFile leftFile,
            @NotNull VirtualFile rightFile,
            @NotNull java.util.Map<VirtualFile, Boolean> priorRightSide
    ) {
        closeFileOutsideWindow(manager, leftFile, leftWindow);
        closeFileOutsideWindow(manager, rightFile, rightWindow);

        Set<VirtualFile> splitFiles = Set.of(leftFile, rightFile);
        for (VirtualFile file : collectOpenFiles(manager)) {
            if (splitFiles.contains(file)) {
                continue;
            }
            EditorWindow currentWindow = ComponentSubtabEditorLookup.findWindowWithFile(manager, file);
            if (currentWindow != null) {
                priorRightSide.put(
                        file,
                        ComponentSubtabEditorLookup.isRightSplitPane(manager, currentWindow)
                );
            }
        }
        for (VirtualFile file : collectOpenFiles(manager)) {
            if (splitFiles.contains(file)) {
                continue;
            }
            EditorWindow host = backgroundHostWindow(
                    manager,
                    file,
                    leftWindow,
                    rightWindow,
                    priorRightSide
            );
            moveFileToBackgroundTab(manager, file, host);
        }

        purgeExtraWindows(manager, leftWindow, rightWindow, priorRightSide);

        leftWindow.setSelectedComposite(leftFile, false);
        rightWindow.setSelectedComposite(rightFile, false);
    }

    private static @NotNull Set<VirtualFile> collectOpenFiles(@NotNull FileEditorManagerEx manager) {
        Set<VirtualFile> files = new LinkedHashSet<>(Arrays.asList(manager.getOpenFiles()));
        for (EditorWindow window : manager.getWindows()) {
            files.addAll(filesInWindow(window));
        }
        return files;
    }

    private static void restoreBackgroundFiles(
            @NotNull FileEditorManagerEx manager,
            @NotNull EditorWindow leftWindow,
            @NotNull EditorWindow rightWindow,
            @NotNull Set<VirtualFile> backgroundFiles,
            @NotNull java.util.Map<VirtualFile, Boolean> priorRightSide
    ) {
        for (VirtualFile file : backgroundFiles) {
            EditorWindow host = backgroundHostWindow(
                    manager,
                    file,
                    leftWindow,
                    rightWindow,
                    priorRightSide
            );
            moveFileToBackgroundTab(manager, file, host);
        }
    }

    private static void repairSplitPaneLayoutIfNeeded(
            @NotNull FileEditorManagerEx manager,
            @NotNull VirtualFile leftFile,
            @NotNull VirtualFile rightFile,
            @NotNull java.util.Map<VirtualFile, Boolean> priorRightSide
    ) {
        EditorWindow leftWindow = ComponentSubtabEditorLookup.findWindowWithFile(manager, leftFile);
        EditorWindow rightWindow = ComponentSubtabEditorLookup.findWindowWithFile(manager, rightFile);
        if (leftWindow != null
                && rightWindow != null
                && leftWindow != rightWindow
                && manager.getWindows().length >= 2
                && !ComponentSubtabEditorLookup.isRightSplitPane(manager, leftWindow)
                && ComponentSubtabEditorLookup.isRightSplitPane(manager, rightWindow)) {
            return;
        }
        if (leftWindow == null) {
            leftWindow = ComponentSubtabEditorLookup.windowForFileOrCurrent(manager, leftFile);
        }
        if (leftWindow == null) {
            return;
        }
        if (rightWindow == null || rightWindow == leftWindow) {
            leftWindow.setSelectedComposite(leftFile, true);
            manager.setCurrentWindow(leftWindow);
            rightWindow = splitRightNonBlocking(
                    manager,
                    leftWindow,
                    leftFile,
                    rightFile,
                    false,
                    TwoPaneArrangement.SIDE_BY_SIDE
            );
        }
        if (rightWindow == null || rightWindow == leftWindow) {
            return;
        }
        ensureFileInWindow(manager, leftFile, leftWindow);
        ensureFileInWindow(manager, rightFile, rightWindow);
        consolidateToTwoPanes(
                manager,
                leftWindow,
                rightWindow,
                leftFile,
                rightFile,
                priorRightSide
        );
    }

    private static @NotNull EditorWindow backgroundHostWindow(
            @NotNull FileEditorManagerEx manager,
            @NotNull VirtualFile file,
            @NotNull EditorWindow leftWindow,
            @NotNull EditorWindow rightWindow,
            @NotNull java.util.Map<VirtualFile, Boolean> priorRightSide
    ) {
        Boolean priorRight = priorRightSide.get(file);
        if (priorRight != null) {
            return priorRight ? rightWindow : leftWindow;
        }
        EditorWindow currentWindow = ComponentSubtabEditorLookup.findWindowWithFile(manager, file);
        if (currentWindow != null) {
            return ComponentSubtabEditorLookup.isRightSplitPane(manager, currentWindow)
                    ? rightWindow
                    : leftWindow;
        }
        return leftWindow;
    }

    private static void moveFileToBackgroundTab(
            @NotNull FileEditorManagerEx manager,
            @NotNull VirtualFile file,
            @NotNull EditorWindow tabHostWindow
    ) {
        if (!tabHostWindow.isFileOpen(file)) {
            InternalPlatformBridge.openFile(
                    manager,
                    file,
                    tabHostWindow,
                    ComponentSubtabNavigation.nonBlockingOpenOptions(false, false)
            );
        }
        for (EditorWindow window : manager.getWindows()) {
            if (window == tabHostWindow || !window.isFileOpen(file)) {
                continue;
            }
            window.closeFile(file);
        }
    }

    private static void purgeExtraWindows(
            @NotNull FileEditorManagerEx manager,
            @NotNull EditorWindow leftWindow,
            @NotNull EditorWindow rightWindow,
            @NotNull java.util.Map<VirtualFile, Boolean> priorRightSide
    ) {
        for (int attempt = 0; attempt < 8; attempt++) {
            boolean changed = false;
            for (EditorWindow window : manager.getWindows()) {
                if (window == leftWindow || window == rightWindow) {
                    continue;
                }
                for (VirtualFile file : filesInWindow(window)) {
                    EditorWindow host = backgroundHostWindow(
                            manager,
                            file,
                            leftWindow,
                            rightWindow,
                            priorRightSide
                    );
                    moveFileToBackgroundTab(manager, file, host);
                    changed = true;
                }
            }
            removeEmptyWindows(manager);
            if (!changed && manager.getWindows().length <= 2 && !hasForeignWindow(manager, leftWindow, rightWindow)) {
                return;
            }
        }
    }

    private static boolean hasForeignWindow(
            @NotNull FileEditorManagerEx manager,
            @NotNull EditorWindow leftWindow,
            @NotNull EditorWindow rightWindow
    ) {
        for (EditorWindow window : manager.getWindows()) {
            if (window != leftWindow && window != rightWindow) {
                return true;
            }
        }
        return false;
    }

    static void collapseEmptyEditorWindows(@NotNull Project project) {
        removeEmptyWindows(FileEditorManagerEx.getInstanceEx(project));
    }

    static void normalizeFileToSinglePane(@NotNull Project project, @NotNull VirtualFile file) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        if (!manager.isFileOpen(file)) {
            removeEmptyWindows(manager);
            return;
        }
        EditorWindow keep = manager.getCurrentWindow();
        if (keep == null || !keep.isFileOpen(file)) {
            keep = ComponentSubtabEditorLookup.findWindowWithFile(manager, file);
        }
        if (keep != null) {
            closeFileOutsideWindow(manager, file, keep);
            if (windowInManager(manager, keep)) {
                manager.setCurrentWindow(keep);
                keep.setSelectedComposite(file, true);
            }
        }
        removeEmptyWindows(manager);
    }

    private static void removeEmptyWindows(@NotNull FileEditorManagerEx manager) {
        for (int attempt = 0; attempt < 8; attempt++) {
            boolean changed = false;
            for (EditorWindow window : manager.getWindows()) {
                if (window.getTabCount() == 0 && windowInManager(manager, window)) {
                    window.unsplit(false);
                    changed = true;
                }
            }
            if (!changed) {
                return;
            }
        }
    }

    private static @NotNull List<VirtualFile> filesInWindow(@NotNull EditorWindow window) {
        return EditorWindowFiles.files(window);
    }

    private static void closeOtherTabsInWindow(
            @NotNull EditorWindow window,
            @NotNull VirtualFile keepFile
    ) {
        for (VirtualFile file : filesInWindow(window)) {
            if (!keepFile.equals(file)) {
                window.closeFile(file);
            }
        }
    }

    private static void closeFileOutsideWindow(
            @NotNull FileEditorManagerEx manager,
            @NotNull VirtualFile file,
            @Nullable EditorWindow keepWindow
    ) {
        for (EditorWindow window : manager.getWindows()) {
            if (window == keepWindow || !window.isFileOpen(file)) {
                continue;
            }
            window.closeFile(file);
        }
    }
}
