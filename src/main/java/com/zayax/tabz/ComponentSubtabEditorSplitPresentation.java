package com.zayax.tabz;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.JBTabsPresentation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.List;

final class ComponentSubtabEditorSplitPresentation {
    @TestOnly
    static int reapplyAllInvocationCount;

    @TestOnly
    static int applySplittabPresentationInvocationCount;

    @TestOnly
    static void resetInvocationCountersForTests() {
        reapplyAllInvocationCount = 0;
        applySplittabPresentationInvocationCount = 0;
    }

    static final String SPLITTAB_A_MAIN_TAB_TITLE = "Split-A";
    static final String SPLITTAB_B_MAIN_TAB_TITLE = "Split-B";
    /** @deprecated legacy tests; use {@link #foregroundSplittabMainTabTitle} */
    @Deprecated
    static final String SPLITTABS_MAIN_TAB_TITLE = SPLITTAB_A_MAIN_TAB_TITLE;

    private ComponentSubtabEditorSplitPresentation() {
    }

    static @NotNull String splittabMainTabTitle(
            @NotNull String baseTitle,
            @NotNull VirtualFile sideFile
    ) {
        return baseTitle + " (" + ComponentTabTitles.displayTabzLabel(sideFile) + ")";
    }

    static boolean isSplittabsMainTabFile(
            @NotNull Project project,
            @NotNull VirtualFile file
    ) {
        return foregroundSplittabMainTabTitle(project, file) != null;
    }

    static @Nullable String foregroundSplittabMainTabTitle(
            @NotNull Project project,
            @NotNull VirtualFile file
    ) {
        return ComponentSubtabEditorSplitMainTab.foregroundMainTabTitle(project, file);
    }

    static boolean isSplittabsForeground(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        return ComponentSubtabEditorSplitMainTab.isForeground(project, pair);
    }

    static @NotNull String defaultHeaderText(@NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair) {
        return ComponentTabTitles.displayGroupName(pair.leftFile())
                + " ("
                + ComponentTabTitles.displayTabzLabel(pair.leftFile())
                + " & "
                + ComponentTabTitles.displayTabzLabel(pair.rightFile())
                + ")";
    }

    static @NotNull String paneHeaderText(@NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair) {
        String custom = pair.headerLabel();
        if (custom != null && !custom.isBlank()) {
            return custom;
        }
        return defaultHeaderText(pair);
    }

    static @NotNull String linkBarText(
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair,
            int zeroBasedIndex
    ) {
        String custom = pair.linkName();
        if (custom != null && !custom.isBlank()) {
            return custom;
        }
        return String.valueOf(zeroBasedIndex + 1);
    }

    /** @deprecated use {@link #paneHeaderText} or {@link #defaultHeaderText} */
    @Deprecated
    static @NotNull String headerText(@NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair) {
        return paneHeaderText(pair);
    }

    static void refreshDedicatedPairSwitch(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        ComponentSubtabSplittabUi.refreshDedicatedPairSwitch(project, pair);
        refreshSplittabsMainTabTitles(project);
    }

    static void applySplittabPresentation(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        applySplittabPresentationInvocationCount++;
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        EditorWindow leftWindow = ComponentSubtabEditorLookup.findWindowWithFile(manager, pair.leftFile());
        EditorWindow rightWindow = ComponentSubtabEditorLookup.findWindowWithFile(manager, pair.rightFile());
        if (leftWindow == null || rightWindow == null || leftWindow == rightWindow) {
            return;
        }

        ComponentSubtabSplittabUi.attachForPair(project, pair);
        ComponentSubtabsManager.refreshOpenStates(project);
        refreshSplittabsMainTabTitles(project);
        ComponentSubtabMainTabSelectPopup.installOn(project);
        ComponentSubtabMainTabColors.refresh(project);
        SplittabRestoreOverlay.syncProject(project);
    }

    static void reapplyAll(@NotNull Project project) {
        reapplyAllInvocationCount++;
        if (!ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(project)) {
            return;
        }
        ComponentSubtabEditorSplitRegistry registry = ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair active = registry.activePair();
        if (active == null) {
            return;
        }
        VirtualFile selected = selectedEditorFile(project);
        if (selected == null || !active.covers(selected)) {
            return;
        }
        if (ComponentSubtabEditorSplitMainTab.isForeground(project, active)) {
            applySplittabPresentation(project, active);
        }
    }

    static void handleMainTabSelection(
            @NotNull Project project,
            @Nullable VirtualFile newFile,
            @Nullable VirtualFile oldFile
    ) {
        ComponentSubtabEditorSplitMainTab.handleSelection(project, newFile, oldFile);
    }

    static void handleMainTabSelection(
            @NotNull Project project,
            @Nullable VirtualFile newFile
    ) {
        handleMainTabSelection(project, newFile, null);
    }

    static void shutdownForTabz(@NotNull Project project) {
        if (project.isDisposed()) {
            return;
        }
        SplittabDedicatedViewService.getInstance(project).resetForTabzShutdown();
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        List<ComponentSubtabEditorSplitRegistry.SplittabPair> pairs =
                new ArrayList<>(registry.all());
        for (ComponentSubtabEditorSplitRegistry.SplittabPair pair : pairs) {
            releasePresentation(project, pair, false);
        }
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : manager.getAllEditors()) {
            SplittabRestoreOverlay.hide(editor);
        }
        registry.pauseForTabzShutdown();
    }

    static void pauseSplittabPresentation(@NotNull Project project) {
        ComponentSubtabEditorSplitRegistry registry = ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair active = registry.activePair();
        if (active == null) {
            return;
        }
        registry.rememberPresentedPair(active.id());
        pauseSplittabPresentation(project, active);
    }

    static void pauseSplittabPresentation(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        releasePresentation(project, pair, false);
    }

    static void leaveSplittabToNormalEditors(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        releasePresentation(project, pair, true);
    }

    static void refreshSplittabsMainTabTitles(@NotNull Project project) {
        ComponentSubtabEditorSplitRegistry registry = ComponentSubtabEditorSplitRegistry.getInstance(project);
        for (ComponentSubtabEditorSplitRegistry.SplittabPair pair : registry.all()) {
            ComponentSubtabsManager.refreshMainTabPresentation(project, pair.leftFile());
            ComponentSubtabsManager.refreshMainTabPresentation(project, pair.rightFile());
        }
    }

    static void dissolvePair(@NotNull Project project, @NotNull String pairId) {
        ComponentSubtabEditorSplitRegistry registry = ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair pair = registry.findById(pairId);
        if (pair == null) {
            return;
        }
        SplittabDissolveMode mode = TabzSettings.getInstance().getSplittabDissolveMode();
        SplittabDedicatedViewService dedicated = SplittabDedicatedViewService.getInstance(project);
        boolean dedicatedActive = dedicated.isDedicatedViewActive();
        boolean switchBehavior = SplittabDedicatedViewService.usesDedicatedBehavior(project);

        dedicated.runSuppressingDedicatedCloseHandler(() -> {
            if (mode == SplittabDissolveMode.CLOSE_PAIR) {
                closePairEditorTabs(project, pair);
            } else {
                releasePresentation(project, pair, true);
            }
            registry.unregister(pairId);
            refreshSplittabsMainTabTitles(project);
            ComponentSubtabMainTabSelectPopup.installOn(project);
            ComponentSubtabMainTabColors.refresh(project);

            if (mode == SplittabDissolveMode.CLOSE_PAIR && switchBehavior && dedicatedActive) {
                dedicated.afterDissolveClosePairInDedicatedView();
            } else if (mode == SplittabDissolveMode.DISSOLVE && switchBehavior && dedicatedActive) {
                dedicated.exitDedicatedView(true);
            }
            SplittabRestoreOverlay.syncProject(project);
        });
    }

    private static void closePairEditorTabs(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        releasePresentation(project, pair, false);
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (VirtualFile side : List.of(pair.leftFile(), pair.rightFile())) {
            if (manager.isFileOpen(side)) {
                manager.closeFile(side);
            }
        }
        ComponentSubtabsManager.refreshOpenStates(project);
    }

    private static @Nullable VirtualFile selectedEditorFile(@NotNull Project project) {
        VirtualFile[] selected = FileEditorManager.getInstance(project).getSelectedFiles();
        return selected.length == 0 ? null : selected[0];
    }

    static void clearSplittabPresentation(@NotNull Project project, @NotNull VirtualFile file) {
        ComponentSubtabEditorSplitRegistry registry = ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair pair = registry.findByFile(file);
        if (pair == null) {
            return;
        }

        FileEditorManager manager = FileEditorManager.getInstance(project);
        if (manager.isFileOpen(pair.leftFile()) || manager.isFileOpen(pair.rightFile())) {
            return;
        }

        registry.rememberPresentedPair(pair.id());
        registry.clearPendingExternalOpenPairId();
        registry.clearBackgroundAnchor();
        releasePresentation(project, pair, true);
        registry.clearActivePair();
        SplittabRestoreOverlay.syncProject(project);
    }

    static void releasePresentation(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        releasePresentation(project, pair, true);
    }

    static void releasePresentation(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair,
            boolean reinstallTabzBars
    ) {
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair active = registry.activePair();
        if (active != null && active.id().equals(pair.id())) {
            registry.clearActivePair();
        }
        ComponentSubtabSplittabUi.detachForPair(project, pair);
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        restoreTabStrip(manager, pair.leftFile());
        restoreTabStrip(manager, pair.rightFile());
        if (manager.isFileOpen(pair.leftFile())) {
            ComponentSubtabsManager.attachIfNeeded(project, pair.leftFile());
        }
        if (manager.isFileOpen(pair.rightFile())) {
            ComponentSubtabsManager.attachIfNeeded(project, pair.rightFile());
        }
        if (reinstallTabzBars) {
            ComponentSubtabsManager.refreshOpenStates(project);
        }
        refreshSplittabsMainTabTitles(project);
    }

    private static void restoreTabStrip(
            @NotNull FileEditorManagerEx manager,
            @NotNull VirtualFile file
    ) {
        EditorWindow window = ComponentSubtabEditorLookup.findWindowWithFile(manager, file);
        if (window != null) {
            setTabStripVisible(window, true);
        }
    }

    static void setTabStripVisible(@NotNull EditorWindow window, boolean visible) {
        JBTabs tabs = window.getTabbedPane().getTabs();
        if (tabs instanceof JBTabsPresentation presentation && presentation.isHideTabs() == visible) {
            presentation.setHideTabs(!visible);
        }
    }

    static @NotNull List<FileEditor> editorsFor(
            @NotNull Project project,
            @NotNull VirtualFile file
    ) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        List<FileEditor> editors = new ArrayList<>();
        for (FileEditor editor : ComponentSubtabsManager.editorsFor(manager, file)) {
            editors.add(editor);
        }
        return editors;
    }
}
