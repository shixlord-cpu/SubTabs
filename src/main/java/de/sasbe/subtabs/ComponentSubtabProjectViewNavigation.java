package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ComponentSubtabProjectViewNavigation {
    private ComponentSubtabProjectViewNavigation() {
    }

    static boolean tryNavigateAsSubtabSwitch(
            @NotNull Project project,
            @NotNull VirtualFile targetFile,
            boolean requestFocus
    ) {
        return navigateRelatedFileFromProjectView(project, targetFile, requestFocus);
    }

    static boolean navigateRelatedFileFromProjectView(
            @NotNull Project project,
            @NotNull VirtualFile targetFile,
            boolean requestFocus
    ) {
        if (!SubtabsSettings.getInstance().isSubtabsActive()) {
            return false;
        }
        if (!SubtabsSettings.getInstance().isReuseOpenSubtabGroupMainTab()) {
            return false;
        }
        if (ComponentRelatedFiles.find(targetFile) == null) {
            return false;
        }

        ComponentSubtabEditorSplitRegistry.SplittabPair activeSplittab =
                ComponentSubtabEditorSplitRegistry.getInstance(project).activePair();
        if (activeSplittab != null
                && ComponentSubtabEditorSplitNavigation.blocksExternalFileOpen(
                project,
                activeSplittab,
                targetFile
        )) {
            return false;
        }

        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        if (ComponentSubtabEditorLookup.findWindowWithFile(manager, targetFile) != null) {
            if (activeSplittab != null
                    && ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(project)
                    && ComponentSubtabEditorSplitNavigation.blocksExternalFileOpen(
                    project,
                    activeSplittab,
                    targetFile
            )) {
                openExternalFileDuringSplittabSession(project, activeSplittab, targetFile, requestFocus);
                return true;
            }
            ComponentSubtabsFileEditorListener.markCoalescedSubtabPlatformOpen(project, targetFile);
            ComponentSubtabNavigation.focusExistingFile(project, targetFile, requestFocus);
            return true;
        }

        VirtualFile anchorFile = resolveAnchorForTarget(project, targetFile);
        if (anchorFile == null) {
            return false;
        }

        ComponentSubtabsFileEditorListener.markCoalescedSubtabPlatformOpen(project, targetFile);
        ComponentSubtabNavigation.switchInTabOf(
                project,
                anchorFile,
                targetFile,
                requestFocus,
                true
        );
        return true;
    }

    static void openFromGroupFilePopup(
            @NotNull Project project,
            @NotNull VirtualFile targetFile
    ) {
        if (navigateRelatedFileFromProjectView(project, targetFile, true)) {
            return;
        }

        FileEditorManager.getInstance(project).openFile(targetFile, true);
        ComponentSubtabsManager.attachIfNeeded(project, targetFile);
        ComponentSubtabsManager.syncSelectionForFile(project, targetFile);
    }

    static @Nullable VirtualFile resolveAnchorForTarget(
            @NotNull Project project,
            @NotNull VirtualFile targetFile
    ) {
        if (!SubtabsSettings.getInstance().isSubtabsActive()) {
            return null;
        }
        if (ComponentRelatedFiles.find(targetFile) == null) {
            return null;
        }

        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);

        VirtualFile inCurrentWindow = anchorInWindow(project, manager.getCurrentWindow(), targetFile);
        if (inCurrentWindow != null) {
            return inCurrentWindow;
        }

        FileEditor selectedEditor = manager.getSelectedEditor();
        if (selectedEditor != null) {
            VirtualFile selectedFile = selectedEditor.getFile();
            if (selectedFile != null
                    && !selectedFile.equals(targetFile)
                    && manager.isFileOpen(selectedFile)) {
                VirtualFile anchor = preferNonSplittabAnchor(project, selectedFile, targetFile);
                if (anchor != null
                        && ComponentSubtabNavigation.isInActiveSubtabGroupOf(anchor, targetFile)) {
                    return anchor;
                }
            }
        }

        for (VirtualFile openFile : manager.getOpenFiles()) {
            if (openFile.equals(targetFile) || !manager.isFileOpen(openFile)) {
                continue;
            }
            if (!ComponentSubtabNavigation.isInActiveSubtabGroupOf(openFile, targetFile)) {
                continue;
            }
            VirtualFile anchor = preferNonSplittabAnchor(project, openFile, targetFile);
            if (anchor != null) {
                return anchor;
            }
        }
        return null;
    }

    private static @Nullable VirtualFile anchorInWindow(
            @NotNull Project project,
            @Nullable EditorWindow window,
            @NotNull VirtualFile targetFile
    ) {
        if (window == null) {
            return null;
        }
        for (VirtualFile candidate : window.getFiles()) {
            if (candidate.equals(targetFile)) {
                continue;
            }
            VirtualFile anchor = preferNonSplittabAnchor(project, candidate, targetFile);
            if (anchor != null && ComponentSubtabNavigation.isInActiveSubtabGroupOf(anchor, targetFile)) {
                return anchor;
            }
        }
        return null;
    }

    static void openExternalFileDuringSplittabSession(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair,
            @NotNull VirtualFile targetFile,
            boolean requestFocus
    ) {
        ComponentSubtabEditorSplitMainTab.notePendingExternalFileOpen(project, pair, targetFile);
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        EditorWindow window = manager.getCurrentWindow();
        if (window == null) {
            manager.openFile(targetFile, requestFocus);
            return;
        }
        if (!window.isFileOpen(targetFile)) {
            manager.openFileWithProviders(targetFile, requestFocus, window);
        } else {
            window.setSelectedComposite(targetFile, requestFocus);
        }
        if (requestFocus) {
            manager.setCurrentWindow(window);
        }
    }

    private static @Nullable VirtualFile preferNonSplittabAnchor(
            @NotNull Project project,
            @NotNull VirtualFile selectedFile,
            @NotNull VirtualFile targetFile
    ) {
        ComponentSubtabEditorSplitRegistry.SplittabPair active =
                ComponentSubtabEditorSplitRegistry.getInstance(project).activePair();
        if (active == null
                || !ComponentSubtabEditorSplitNavigation.isSplittabWorkspace(project, active)
                || !active.covers(selectedFile)) {
            return selectedFile;
        }
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        EditorWindow window = manager.getCurrentWindow();
        if (window == null) {
            return null;
        }
        for (VirtualFile candidate : window.getFiles()) {
            if (candidate.equals(targetFile) || active.covers(candidate)) {
                continue;
            }
            if (ComponentSubtabNavigation.isInActiveSubtabGroupOf(candidate, targetFile)) {
                return candidate;
            }
        }
        return null;
    }
}
