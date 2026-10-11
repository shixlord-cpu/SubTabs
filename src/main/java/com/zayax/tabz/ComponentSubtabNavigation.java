package com.zayax.tabz;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.EditorComposite;
import com.intellij.openapi.fileEditor.impl.FileEditorOpenOptions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class ComponentSubtabNavigation {
    private static final Key<Boolean> SWITCH_IN_PROGRESS = Key.create("componentTabz.switchInProgress");

    private ComponentSubtabNavigation() {
    }

    static boolean isSwitchInProgress(@NotNull Project project) {
        return Boolean.TRUE.equals(SWITCH_IN_PROGRESS.get(project));
    }

    static boolean sameSubtabGroup(
            @NotNull VirtualFile firstFile,
            @NotNull VirtualFile secondFile
    ) {
        return sharesCurrentTabzGrouping(firstFile, secondFile);
    }

    static boolean sharesCurrentTabzGrouping(
            @NotNull VirtualFile firstFile,
            @NotNull VirtualFile secondFile
    ) {
        if (firstFile.equals(secondFile)) {
            return true;
        }
        return isInActiveSubtabGroupOf(firstFile, secondFile)
                || isInActiveSubtabGroupOf(secondFile, firstFile);
    }

    /**
     * Whether {@code file} belongs to the tabz group currently shown for {@code groupHost}
     * (respects rule rotation / alternative grouping via {@link ComponentRelatedFiles#find}).
     */
    static boolean isInActiveSubtabGroupOf(
            @NotNull VirtualFile groupHost,
            @NotNull VirtualFile file
    ) {
        if (groupHost.equals(file)) {
            return true;
        }
        ComponentRelatedFiles.Match hostMatch = ComponentRelatedFiles.find(groupHost);
        if (hostMatch == null) {
            return false;
        }
        ComponentRelatedFiles.Match fileMatch = ComponentRelatedFiles.find(file);
        if (fileMatch != null && hostMatch.key().equals(fileMatch.key())) {
            return true;
        }
        for (ComponentRelatedFiles.Entry entry : hostMatch.relatedFiles()) {
            if (entry.file().equals(file)) {
                return true;
            }
        }
        return false;
    }

    static boolean canSwitchAdjacent(@NotNull Project project, int direction) {
        if (!TabzSettings.getInstance().isTabzEnabled()
                || !TabzSettings.getInstance().isSubtabsActive()) {
            return false;
        }

        VirtualFile currentFile = selectedFile(project);
        return currentFile != null && adjacentTabzFile(currentFile, direction) != null;
    }

    static void switchAdjacentSubtab(@NotNull Project project, int direction) {
        VirtualFile currentFile = selectedFile(project);
        if (currentFile == null) {
            return;
        }

        VirtualFile targetFile = adjacentTabzFile(currentFile, direction);
        if (targetFile != null) {
            switchInTabOf(project, currentFile, targetFile, true);
        }
    }

    static @Nullable VirtualFile adjacentInList(
            @NotNull VirtualFile currentFile,
            @NotNull List<ComponentRelatedFiles.Entry> relatedFiles,
            int direction
    ) {
        if (relatedFiles.size() < 2 || direction == 0) {
            return null;
        }

        int index = indexOf(relatedFiles, currentFile);
        if (index < 0) {
            return null;
        }

        int targetIndex = index + direction;
        if (targetIndex < 0 || targetIndex >= relatedFiles.size()) {
            return null;
        }
        return relatedFiles.get(targetIndex).file();
    }

    static void switchToRelatedFile(
            @NotNull Project project,
            @NotNull VirtualFile currentFile,
            @NotNull VirtualFile targetFile
    ) {
        switchInTabOf(project, currentFile, targetFile, true);
    }

    static void focusExistingFile(
            @NotNull Project project,
            @NotNull VirtualFile targetFile,
            boolean requestFocus
    ) {
        runWithSwitchGuard(project, () -> focusExistingFileImpl(project, targetFile, requestFocus));
        consolidateAfterSubtabGroupNavigation(project, targetFile);
    }

    static void runWithSwitchGuard(@NotNull Project project, @NotNull Runnable action) {
        SWITCH_IN_PROGRESS.set(project, true);
        try {
            action.run();
        } finally {
            SWITCH_IN_PROGRESS.set(project, null);
        }
    }

    /**
     * Replaces the file of the main tab that currently shows {@code anchorFile} and focuses it.
     *
     * <p>The anchor is not necessarily the focused tab: the hover select box of a background main tab
     * switches that tab, so its editor pane has to become the current one before the swap. Otherwise
     * the platform would replace the tab of whichever pane happens to be focused.
     */
    static void switchInTabOf(
            @NotNull Project project,
            @NotNull VirtualFile anchorFile,
            @NotNull VirtualFile targetFile,
            boolean requestFocus
    ) {
        switchInTabOf(project, anchorFile, targetFile, requestFocus, false);
    }

    static void switchInTabOf(
            @NotNull Project project,
            @NotNull VirtualFile anchorFile,
            @NotNull VirtualFile targetFile,
            boolean requestFocus,
            boolean waitForCompositeOpen
    ) {
        if (anchorFile.equals(targetFile)) {
            return;
        }

        runWithSwitchGuard(
                project,
                () -> switchInTabOfImpl(project, anchorFile, targetFile, requestFocus, waitForCompositeOpen)
        );
        consolidateAfterSubtabGroupNavigation(project, targetFile);
    }

    static void consolidateAfterSubtabGroupNavigation(
            @NotNull Project project,
            @NotNull VirtualFile keepFile
    ) {
        if (ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(project)) {
            return;
        }
        consolidateSiblingGroupMainTabs(project, keepFile);
    }

    private static void switchInTabOfImpl(
            @NotNull Project project,
            @NotNull VirtualFile anchorFile,
            @NotNull VirtualFile targetFile,
            boolean requestFocus,
            boolean waitForCompositeOpen
    ) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        if (manager.isFileOpen(targetFile)) {
            ComponentSubtabFileEditorListener.markCoalescedTabzPlatformOpen(project, targetFile);
            ComponentSubtabFileEditorListener.scheduleDeferredSwitchPresentation(project, targetFile);
            focusExistingFileImpl(project, targetFile, requestFocus);
            return;
        }

        if (!manager.isFileOpen(anchorFile)) {
            ComponentSubtabFileEditorListener.markCoalescedTabzPlatformOpen(project, targetFile);
            ComponentSubtabFileEditorListener.scheduleDeferredSwitchPresentation(project, targetFile);
            manager.openFile(targetFile, requestFocus);
            ComponentSubtabsManager.updateSelectionForFile(project, targetFile);
            return;
        }

        ComponentSubtabFileEditorListener.markCoalescedTabzPlatformOpen(project, targetFile);
        ComponentSubtabFileEditorListener.scheduleDeferredSwitchPresentation(project, targetFile);

        EditorWindow anchorWindow = ComponentSubtabEditorLookup.findWindowWithFile(manager, anchorFile);
        if (anchorWindow != null) {
            manager.setCurrentWindow(anchorWindow);
            anchorWindow.setSelectedComposite(anchorFile, false);
        }

        FileEditor anchorEditor = editorForFile(manager, anchorFile);
        if (anchorEditor != null) {
            ComponentSubtabsManager.prepareTransfer(project, anchorEditor, targetFile);
        }

        for (EditorWindow window : manager.getWindows()) {
            if (window != anchorWindow && window.isFileOpen(anchorFile)) {
                replaceFileInEditorWindow(
                        manager,
                        window,
                        anchorFile,
                        targetFile,
                        false,
                        waitForCompositeOpen
                );
            }
        }
        if (anchorWindow != null) {
            replaceFileInEditorWindow(
                    manager,
                    anchorWindow,
                    anchorFile,
                    targetFile,
                    requestFocus,
                    waitForCompositeOpen
            );
        }
        ComponentSubtabsManager.attachIfNeeded(project, targetFile);
        ComponentSubtabsManager.updateSelectionForFile(project, targetFile);
    }

    /**
     * Replaces {@code oldFile} with {@code newFile} in a single editor pane only. The composite is not
     * awaited: building a file editor (highlighter, language services) can take seconds, and a
     * synchronous open blocks the EDT for that whole time.
     */
    private static void replaceFileInEditorWindow(
            @NotNull FileEditorManagerEx manager,
            @NotNull EditorWindow window,
            @NotNull VirtualFile oldFile,
            @NotNull VirtualFile newFile,
            boolean requestFocus,
            boolean waitForCompositeOpen
    ) {
        if (!window.isFileOpen(oldFile)) {
            return;
        }
        EditorComposite selectedComposite = window.getSelectedComposite();
        boolean active = selectedComposite != null && oldFile.equals(selectedComposite.getFile());
        manager.setCurrentWindow(window);
        if (!window.isFileOpen(newFile)) {
            FileEditorOpenOptions options = waitForCompositeOpen
                    ? blockingOpenOptions(window, oldFile, requestFocus && active)
                    : nonBlockingOpenOptions(window, oldFile, requestFocus && active);
            manager.openFile(newFile, window, options);
        }
        if (window.isFileOpen(newFile)) {
            window.setSelectedComposite(newFile, requestFocus && active);
            ComponentSubtabBarHover.transferMainTabSync(window, oldFile, newFile);
            if (window.isFileOpen(oldFile)) {
                window.closeFile(oldFile);
            }
        }
    }

    static @NotNull FileEditorOpenOptions nonBlockingOpenOptions(
            @NotNull EditorWindow window,
            @NotNull VirtualFile oldFile,
            boolean requestFocus
    ) {
        return openOptions(window, oldFile, requestFocus, false);
    }

    static @NotNull FileEditorOpenOptions blockingOpenOptions(
            @NotNull EditorWindow window,
            @NotNull VirtualFile oldFile,
            boolean requestFocus
    ) {
        return openOptions(window, oldFile, requestFocus, true);
    }

    private static @NotNull FileEditorOpenOptions openOptions(
            @NotNull EditorWindow window,
            @NotNull VirtualFile oldFile,
            boolean requestFocus,
            boolean waitForCompositeOpen
    ) {
        List<VirtualFile> files = List.of(window.getFiles());
        return new FileEditorOpenOptions(
                true,
                false,
                false,
                requestFocus,
                window.isFilePinned(oldFile),
                files.indexOf(oldFile),
                false,
                null,
                false,
                false,
                waitForCompositeOpen,
                null
        );
    }

    static @NotNull FileEditorOpenOptions nonBlockingOpenOptions(boolean requestFocus, boolean selectAsCurrent) {
        return new FileEditorOpenOptions(
                selectAsCurrent,
                false,
                false,
                requestFocus,
                false,
                -1,
                false,
                null,
                false,
                false,
                false,
                null
        );
    }

    private static void focusExistingFileImpl(
            @NotNull Project project,
            @NotNull VirtualFile targetFile,
            boolean requestFocus
    ) {
        if (!ComponentSubtabNavigation.isSwitchInProgress(project)) {
            ComponentSubtabFileEditorListener.markCoalescedTabzPlatformOpen(project, targetFile);
            ComponentSubtabFileEditorListener.scheduleDeferredSwitchPresentation(project, targetFile);
        }
        FileEditorManagerEx managerEx = FileEditorManagerEx.getInstanceEx(project);
        EditorWindow window = ComponentSubtabEditorLookup.findWindowWithFile(managerEx, targetFile);
        if (window != null) {
            managerEx.setCurrentWindow(window);
            window.setSelectedComposite(targetFile, requestFocus);
        } else {
            managerEx.openFile(targetFile, requestFocus);
        }
        ComponentSubtabsManager.updateSelectionForFile(project, targetFile);
    }

    private static @Nullable FileEditor editorForFile(
            @NotNull FileEditorManager manager,
            @NotNull VirtualFile file
    ) {
        FileEditor selected = manager.getSelectedEditor();
        if (selected != null && file.equals(selected.getFile())) {
            return selected;
        }
        FileEditor[] editors = ComponentSubtabsManager.editorsFor(manager, file);
        return editors.length == 0 ? null : editors[0];
    }

    private static @Nullable VirtualFile selectedFile(@NotNull Project project) {
        VirtualFile[] selectedFiles = FileEditorManager.getInstance(project).getSelectedFiles();
        return selectedFiles.length == 0 ? null : selectedFiles[0];
    }

    private static @Nullable VirtualFile adjacentTabzFile(
            @NotNull VirtualFile currentFile,
            int direction
    ) {
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(currentFile);
        if (match == null) {
            return null;
        }
        return adjacentInList(currentFile, match.relatedFiles(), direction);
    }

    private static int indexOf(
            @NotNull List<ComponentRelatedFiles.Entry> relatedFiles,
            @NotNull VirtualFile currentFile
    ) {
        for (int index = 0; index < relatedFiles.size(); index++) {
            if (relatedFiles.get(index).file().equals(currentFile)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * After the platform opens a related file, drop other main tabs of the same group in that pane
     * when {@link TabzSettings#isReuseOpenSubtabGroupMainTab()} is on.
     */
    static void consolidateSiblingGroupMainTabs(
            @NotNull Project project,
            @NotNull VirtualFile keepFile
    ) {
        if (!TabzSettings.getInstance().isSubtabsActive()
                || !TabzSettings.getInstance().isReuseOpenSubtabGroupMainTab()) {
            return;
        }
        if (ComponentRelatedFiles.find(keepFile) == null || isSwitchInProgress(project)) {
            return;
        }
        ComponentSubtabEditorSplitRegistry.SplittabPair activeSplittab =
                ComponentSubtabEditorSplitRegistry.getInstance(project).activePair();
        if (activeSplittab != null
                && ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(project)
                && ComponentSubtabEditorSplitNavigation.blocksExternalFileOpen(
                project,
                activeSplittab,
                keepFile
        )) {
            return;
        }
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        EditorWindow window = ComponentSubtabEditorLookup.findWindowWithFile(manager, keepFile);
        if (window == null) {
            return;
        }
        for (VirtualFile candidate : window.getFiles()) {
            if (candidate.equals(keepFile)) {
                continue;
            }
            if (isInActiveSubtabGroupOf(keepFile, candidate)) {
                window.closeFile(candidate);
            }
        }
    }
}
