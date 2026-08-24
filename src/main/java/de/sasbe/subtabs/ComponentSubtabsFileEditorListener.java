package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
final class ComponentSubtabsFileEditorListener
        implements FileEditorManagerListener, FileEditorManagerListener.Before, DumbAware {
    @Override
    public void beforeFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        if (ComponentSubtabNavigation.isSwitchInProgress(source.getProject())) {
            return;
        }

        if (ComponentSubtabProjectViewNavigation.navigateRelatedFileFromProjectView(
                source.getProject(),
                file,
                true
        )) {
            return;
        }
    }

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        ComponentSubtabsManager.attachIfNeeded(source.getProject(), file);
        ComponentSubtabsManager.syncSelectionForFile(source.getProject(), file);
        ComponentSubtabsManager.refreshPresentationStates(source.getProject());
        ComponentSubtabMainTabSelectPopup.installOn(source.getProject());
    }

    @Override
    public void beforeFileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        ComponentSubtabsManager.detachFromFile(source.getProject(), file);
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        ComponentSubtabMainTabSelectPopup.hideAllPopups(source.getProject());
        ComponentSubtabsManager.refreshOpenStates(source.getProject());
        ComponentSubtabMainTabSelectPopup.installOn(source.getProject());
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        VirtualFile newFile = event.getNewFile();
        if (newFile != null) {
            ComponentSubtabsManager.syncSelectionForFile(event.getManager().getProject(), newFile);
        }
        VirtualFile oldFile = event.getOldFile();
        if (oldFile != null && !oldFile.equals(newFile)) {
            ComponentSubtabsManager.syncSelectionForFile(event.getManager().getProject(), oldFile);
        }
        ComponentSubtabMainTabSelectPopup.installOn(event.getManager().getProject());
    }

    static void attachToAlreadyOpenFiles(@NotNull Project project) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (VirtualFile file : manager.getOpenFiles()) {
            ComponentSubtabsManager.attachIfNeeded(project, file);
            ComponentSubtabsManager.syncSelectionForFile(project, file);
        }
    }
}
