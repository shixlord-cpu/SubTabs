package com.zayax.tabz;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Splittab-Haupttabs: im Vordergrund {@code Split-A} (links) und {@code Split-B} (rechts).
 * Verlassen des Splittab-Modus (Haupttab-Wechsel, Schließen einer Seite, externes Öffnen) beendet
 * nur die Splittab-Präsentation; offene Dateien bleiben als normale Tabs mit Tabz-UI.
 */
final class ComponentSubtabEditorSplitMainTab {
    private ComponentSubtabEditorSplitMainTab() {
    }

    static void handleSelection(
            @NotNull Project project,
            @Nullable VirtualFile newFile,
            @Nullable VirtualFile oldFile
    ) {
        if (ComponentSubtabNavigation.isSwitchInProgress(project)) {
            return;
        }
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        if (registry.all().isEmpty()) {
            return;
        }

        ComponentSubtabEditorSplitRegistry.SplittabPair active = registry.activePair();
        if (active == null) {
            return;
        }
        ComponentSubtabEditorSplitRegistry.SplittabPair pairContext = active;
        if (pairContext == null && oldFile != null) {
            pairContext = registry.findByFile(oldFile);
        }
        if (pairContext == null && newFile != null) {
            pairContext = registry.findByFile(newFile);
        }
        boolean foreground = isForeground(project, pairContext);
        boolean sessionActive = pairContext != null
                && isActiveSplittabSession(project, pairContext);

        if (sessionActive && shouldCloseSplittabForeground(oldFile, newFile, pairContext)) {
            if (newFile != null && !pairContext.covers(newFile)) {
                ComponentSubtabEditorSplitRegistry.SplittabPair targetPair = registry.findByFile(newFile);
                if (targetPair != null
                        && targetPair.covers(newFile)
                        && !targetPair.id().equals(pairContext.id())) {
                    // The platform may deliver a selection event late, after a pair switch already
                    // closed that file; following it would undo the switch.
                    if (ComponentSubtabEditorSplitNavigation.shouldIgnoreCrossPairSelection(
                            project,
                            targetPair.id()
                    )) {
                        return;
                    }
                    if (!ComponentSubtabEditorSplitNavigation.isUserInitiatedEditorSelection(project, newFile)) {
                        return;
                    }
                    if (TabzSettings.getInstance().getSplittabOtherPairFileMode()
                            == SplittabOtherPairFileMode.SWITCH_TO_PAIR) {
                        if (FileEditorManager.getInstance(project).isFileOpen(newFile)) {
                            ComponentSubtabEditorSplitNavigation.activatePair(project, targetPair.id());
                        }
                        return;
                    }
                    if (!FileEditorManager.getInstance(project).isFileOpen(newFile)) {
                        notePendingExternalFileOpen(project, pairContext, newFile);
                        return;
                    }
                    finishExternalFileOpenDuringSplittab(project, pairContext, newFile);
                    return;
                }
                if (registry.pendingExternalOpenPairId() != null) {
                    if (newFile != null
                            && FileEditorManager.getInstance(project).isFileOpen(newFile)
                            && completePendingExternalFileOpen(project, newFile)) {
                        return;
                    }
                    return;
                }
                if (!FileEditorManager.getInstance(project).isFileOpen(newFile)) {
                    notePendingExternalFileOpen(project, pairContext, newFile);
                    return;
                }
                finishExternalFileOpenDuringSplittab(project, pairContext, newFile);
                return;
            }
            leaveSplittabToNormalEditors(project, pairContext);
            return;
        }

        if (foreground
                && pairContext != null
                && newFile != null
                && pairContext.covers(newFile)) {
            registry.setActive(pairContext.id());
        }
    }

    private static boolean shouldCloseSplittabForeground(
            @Nullable VirtualFile oldFile,
            @Nullable VirtualFile newFile,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        if (oldFile == null || !pair.covers(oldFile)) {
            return false;
        }
        if (newFile == null) {
            return false;
        }
        return !pair.covers(newFile);
    }

    static @Nullable String foregroundMainTabTitle(
            @NotNull Project project,
            @NotNull VirtualFile file
    ) {
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair pair = registry.findByFile(file);
        if (pair == null || !isForeground(project, pair)) {
            return null;
        }
        if (file.equals(pair.leftFile())) {
            return ComponentSubtabEditorSplitPresentation.splittabMainTabTitle(
                    ComponentSubtabEditorSplitPresentation.SPLITTAB_A_MAIN_TAB_TITLE,
                    pair.leftFile()
            );
        }
        if (file.equals(pair.rightFile())) {
            return ComponentSubtabEditorSplitPresentation.splittabMainTabTitle(
                    ComponentSubtabEditorSplitPresentation.SPLITTAB_B_MAIN_TAB_TITLE,
                    pair.rightFile()
            );
        }
        return null;
    }

    static boolean isForeground(
            @NotNull Project project,
            @Nullable ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        if (pair == null) {
            return false;
        }
        ComponentSubtabEditorSplitRegistry.SplittabPair active =
                ComponentSubtabEditorSplitRegistry.getInstance(project).activePair();
        if (active == null || !active.id().equals(pair.id())) {
            return false;
        }
        if (SplittabDedicatedViewService.getInstance(project).isDedicatedViewActive()) {
            return ComponentSubtabEditorSplitNavigation.isActiveSplittabPairOpen(project, pair);
        }
        return ComponentSubtabEditorSplitNavigation.isSplittabWorkspace(project, pair);
    }

    static void notePendingExternalFileOpen(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair,
            @NotNull VirtualFile incomingFile
    ) {
        if (!ComponentSubtabEditorSplitNavigation.blocksExternalFileOpen(project, pair, incomingFile)) {
            return;
        }
        ComponentSubtabEditorSplitRegistry.getInstance(project).setPendingExternalOpenPairId(pair.id());
    }

    static boolean isActiveSplittabSession(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        ComponentSubtabEditorSplitRegistry.SplittabPair active =
                ComponentSubtabEditorSplitRegistry.getInstance(project).activePair();
        return active != null
                && active.id().equals(pair.id())
                && ComponentSubtabEditorSplitNavigation.isActiveSplittabPairOpen(project, pair);
    }

    static boolean completePendingExternalFileOpen(
            @NotNull Project project,
            @NotNull VirtualFile openedFile
    ) {
        if (!FileEditorManager.getInstance(project).isFileOpen(openedFile)) {
            return false;
        }
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        String pendingId = registry.pendingExternalOpenPairId();
        ComponentSubtabEditorSplitRegistry.SplittabPair pair = pendingId == null
                ? null
                : registry.findById(pendingId);
        if (pair == null) {
            ComponentSubtabEditorSplitRegistry.SplittabPair active = registry.activePair();
            if (active == null
                    || !ComponentSubtabEditorSplitNavigation.blocksExternalFileOpen(project, active, openedFile)) {
                return false;
            }
            pair = active;
        } else {
            registry.clearPendingExternalOpenPairId();
        }
        finishExternalFileOpenDuringSplittab(project, pair, openedFile);
        return true;
    }

    static void finishExternalFileOpenDuringSplittab(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair,
            @NotNull VirtualFile openedFile
    ) {
        ComponentSubtabEditorSplitRegistry.getInstance(project).clearPendingExternalOpenPairId();
        leaveSplittabToNormalEditors(project, pair);
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        if (manager.isFileOpen(openedFile)) {
            ComponentSubtabNavigation.runWithSwitchGuard(
                    project,
                    () -> ComponentSubtabEditorSplitNavigation.collapseEmptyEditorWindows(project)
            );
        }
    }

    static void onSplittabPairFileClosed(
            @NotNull Project project,
            @NotNull VirtualFile closedFile
    ) {
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair pair = registry.findByFile(closedFile);
        if (pair == null || !pair.covers(closedFile)) {
            return;
        }
        ComponentSubtabEditorSplitRegistry.SplittabPair active = registry.activePair();
        if (active == null || !active.id().equals(pair.id())) {
            return;
        }
        if (SplittabDedicatedViewService.getInstance(project).isDedicatedViewActive()) {
            SplittabDedicatedViewService.getInstance(project).onDedicatedPairSideClosed(closedFile);
            return;
        }
        leaveSplittabToNormalEditors(project, pair);
    }

    static void refreshNormalUiForOpenPairSides(
            @NotNull Project project,
            @NotNull VirtualFile closedPairSide
    ) {
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair pair = registry.findByFile(closedPairSide);
        if (pair == null || !pair.covers(closedPairSide)) {
            return;
        }
        if (SplittabDedicatedViewService.getInstance(project).isDedicatedViewActive()) {
            ComponentSubtabEditorSplitRegistry.SplittabPair active = registry.activePair();
            if (active == null || !active.id().equals(pair.id())) {
                return;
            }
        }
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (VirtualFile side : java.util.List.of(pair.leftFile(), pair.rightFile())) {
            if (manager.isFileOpen(side)) {
                ComponentSubtabsManager.attachIfNeeded(project, side);
                ComponentSubtabsManager.refreshMainTabPresentation(project, side);
            }
        }
        ComponentSubtabsManager.refreshOpenStates(project);
    }

    static void dedupeOpenFileIfNeeded(@NotNull Project project, @NotNull VirtualFile file) {
        if (!FileEditorManager.getInstance(project).isFileOpen(file)) {
            return;
        }
        ComponentSubtabNavigation.runWithSwitchGuard(
                project,
                () -> ComponentSubtabEditorSplitNavigation.normalizeFileToSinglePane(project, file)
        );
    }

    static void leaveSplittabToNormalEditors(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        registry.rememberPresentedPair(pair.id());
        registry.clearPendingExternalOpenPairId();
        registry.clearBackgroundAnchor();
        ComponentSubtabEditorSplitPresentation.leaveSplittabToNormalEditors(project, pair);
        registry.clearActivePair();
        ComponentSubtabEditorSplitPresentation.refreshSplittabsMainTabTitles(project);
        SplittabRestoreOverlay.syncProject(project);
    }

    /** Beendet Splittab-Vordergrund ohne Pair-Dateien zu schließen. */
    static void closeSplittabForeground(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        leaveSplittabToNormalEditors(project, pair);
    }

    static void leaveForegroundFromSide(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair,
            @NotNull VirtualFile fromSideFile
    ) {
        if (!pair.covers(fromSideFile)) {
            return;
        }
        leaveSplittabToNormalEditors(project, pair);
    }
}
