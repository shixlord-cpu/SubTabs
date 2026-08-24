package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImplKt;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class ComponentSubtabNavigation {
    private ComponentSubtabNavigation() {
    }

    static boolean canSwitchAdjacent(@NotNull Project project, int direction) {
        if (!SubtabsSettings.getInstance().isSubtabsActive()) {
            return false;
        }

        VirtualFile currentFile = selectedFile(project);
        return currentFile != null && adjacentSubtabFile(currentFile, direction) != null;
    }

    static void switchAdjacentSubtab(@NotNull Project project, int direction) {
        VirtualFile currentFile = selectedFile(project);
        if (currentFile == null) {
            return;
        }

        VirtualFile targetFile = adjacentSubtabFile(currentFile, direction);
        if (targetFile != null) {
            switchToRelatedFile(project, currentFile, targetFile);
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
        if (currentFile.equals(targetFile)) {
            return;
        }

        FileEditorManager manager = FileEditorManager.getInstance(project);
        if (manager.isFileOpen(targetFile)) {
            manager.openFile(targetFile, true);
            ComponentSubtabsManager.syncSelectionForFile(project, targetFile);
            ComponentSubtabsManager.syncSelectionForFile(project, currentFile);
            return;
        }

        if (manager.isFileOpen(currentFile)) {
            FileEditor currentEditor = ComponentSubtabsManager.selectedEditorFor(manager, currentFile);
            if (currentEditor != null) {
                ComponentSubtabsManager.prepareTransfer(project, currentEditor, targetFile);
            }
            FileEditorManagerImplKt.reopenVirtualFileEditor(project, currentFile, targetFile, true);
            ComponentSubtabsManager.attachIfNeeded(project, targetFile);
            ComponentSubtabsManager.syncSelectionForFile(project, targetFile);
            return;
        }

        manager.openFile(targetFile, true);
        ComponentSubtabsManager.syncSelectionForFile(project, targetFile);
    }

    private static @Nullable VirtualFile selectedFile(@NotNull Project project) {
        VirtualFile[] selectedFiles = FileEditorManager.getInstance(project).getSelectedFiles();
        return selectedFiles.length == 0 ? null : selectedFiles[0];
    }

    private static @Nullable VirtualFile adjacentSubtabFile(
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
}
