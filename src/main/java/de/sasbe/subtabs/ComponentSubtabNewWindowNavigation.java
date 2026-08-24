package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

final class ComponentSubtabNewWindowNavigation {
    private ComponentSubtabNewWindowNavigation() {
    }

    static void openSubtabInNewWindow(
            @NotNull Project project,
            @NotNull VirtualFile targetFile
    ) {
        ComponentSubtabNavigation.runWithSwitchGuard(project, () -> {
            FileEditorManagerImpl manager = (FileEditorManagerImpl) FileEditorManager.getInstance(project);
            manager.openFileInNewWindow(targetFile);
            ComponentSubtabsManager.attachIfNeeded(project, targetFile);
            ComponentSubtabsManager.syncSelectionForFile(project, targetFile);
        });
    }
}
