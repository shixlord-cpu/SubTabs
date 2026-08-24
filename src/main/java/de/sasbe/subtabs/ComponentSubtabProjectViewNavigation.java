package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
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
        if (ComponentRelatedFiles.find(targetFile) == null) {
            return false;
        }

        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        if (ComponentSubtabEditorLookup.findWindowWithFile(manager, targetFile) != null) {
            ComponentSubtabNavigation.focusExistingFile(project, targetFile, requestFocus);
            return true;
        }

        VirtualFile anchorFile = resolveAnchorForTarget(project, targetFile);
        if (anchorFile == null) {
            return false;
        }

        ComponentSubtabNavigation.switchInSelectedEditor(
                project,
                anchorFile,
                targetFile,
                requestFocus
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

        FileEditorManager manager = FileEditorManager.getInstance(project);
        FileEditor selectedEditor = manager.getSelectedEditor();
        if (selectedEditor == null) {
            return null;
        }

        VirtualFile selectedFile = selectedEditor.getFile();
        if (selectedFile == null || selectedFile.equals(targetFile)) {
            return null;
        }
        if (!ComponentSubtabNavigation.sameSubtabGroup(selectedFile, targetFile)) {
            return null;
        }
        if (!manager.isFileOpen(selectedFile)) {
            return null;
        }
        return selectedFile;
    }
}
