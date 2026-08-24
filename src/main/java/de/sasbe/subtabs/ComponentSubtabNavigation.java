package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImplKt;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

final class ComponentSubtabNavigation {
    private ComponentSubtabNavigation() {
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
}
