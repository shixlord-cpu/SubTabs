package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

final class ComponentSubtabNewTabNavigation {
    private ComponentSubtabNewTabNavigation() {
    }

    static void openSubtabInNewTab(
            @NotNull Project project,
            @NotNull VirtualFile targetFile
    ) {
        ComponentSubtabNavigation.runWithSwitchGuard(project, () -> openSubtabInNewTabImpl(project, targetFile));
    }

    private static void openSubtabInNewTabImpl(
            @NotNull Project project,
            @NotNull VirtualFile targetFile
    ) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        EditorWindow currentWindow = manager.getCurrentWindow();

        if (manager.isFileOpen(targetFile)) {
            if (currentWindow != null && currentWindow.isFileOpen(targetFile)) {
                manager.setCurrentWindow(currentWindow);
                currentWindow.setSelectedComposite(targetFile, true);
            } else {
                ComponentSubtabNavigation.focusExistingFile(project, targetFile, true);
            }
            ComponentSubtabsManager.syncSelectionForFile(project, targetFile);
            return;
        }

        if (currentWindow == null) {
            manager.openFile(targetFile, true);
        } else {
            manager.openFileWithProviders(targetFile, true, currentWindow);
        }
        ComponentSubtabsManager.attachIfNeeded(project, targetFile);
        ComponentSubtabsManager.syncSelectionForFile(project, targetFile);
    }
}
