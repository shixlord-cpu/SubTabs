package de.sasbe.subtabs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTree;
final class ComponentSubtabsFileEditorListener
        implements FileEditorManagerListener, FileEditorManagerListener.Before, DumbAware {
    @Override
    public void beforeFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            return;
        }
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
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            return;
        }
        ComponentSubtabsDocumentListener.install(source.getProject());
        ComponentSubtabsManager.attachIfNeeded(source.getProject(), file);
        SidetabsManager.attachIfNeeded(source.getProject(), file);
        ComponentSubtabsManager.syncSelectionForFile(source.getProject(), file);
        scheduleMainTabPresentationRefresh(source.getProject(), file);
        ComponentSubtabGroupSplitNavigation.reapplyAll(source.getProject());
        refreshOpenPopups(source.getProject());
        ComponentSubtabMainTabSelectPopup.installOn(source.getProject());
        ComponentSubtabMainTabColors.refresh(source.getProject());
    }

    @Override
    public void beforeFileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        ComponentSubtabsManager.detachFromFile(source.getProject(), file);
        SidetabsManager.detachFromFile(source.getProject(), file);
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        ComponentSubtabGroupSplitNavigation.clearSplitPresentation(source.getProject(), file);
        // Closing a file detaches the subtab bar from every editor of that file, so a surviving
        // split has to rebuild its presentation afterwards.
        ComponentSubtabGroupSplitNavigation.reapplyAll(source.getProject());
        ComponentSubtabMainTabSelectPopup.hideAllPopups(source.getProject());
        ComponentSubtabsManager.refreshOpenStates(source.getProject());
        refreshOpenPopups(source.getProject());
        ComponentSubtabMainTabSelectPopup.installOn(source.getProject());
        ComponentSubtabMainTabColors.refresh(source.getProject());
        ComponentSubtabsManager.refreshAllMainTabPresentations(source.getProject());
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            return;
        }
        Project project = event.getManager().getProject();
        VirtualFile newFile = event.getNewFile();
        VirtualFile oldFile = event.getOldFile();
        boolean selectionUpdated = false;
        if (newFile != null) {
            selectionUpdated |= ComponentSubtabsManager.updateSelectionForFile(project, newFile);
            ComponentSubtabsManager.refreshMainTabPresentation(project, newFile);
            SidetabsManager.attachIfNeeded(project, newFile);
        }
        if (oldFile != null && !oldFile.equals(newFile)) {
            selectionUpdated |= ComponentSubtabsManager.updateSelectionForFile(project, oldFile);
            ComponentSubtabsManager.refreshMainTabPresentation(project, oldFile);
        }
        if (selectionUpdated) {
            ComponentSubtabsManager.refreshOpenStates(project);
        }
        ComponentSubtabMainTabSelectPopup.installOn(project);
        ComponentSubtabGroupSplitNavigation.reapplyAll(project);
        refreshOpenPopups(project);
        ComponentSubtabMainTabColors.refresh(project);
    }

    private static void refreshOpenPopups(@NotNull Project project) {
        ComponentSubtabMainTabSelectPopup.refreshPopupPresentation(project);
        var toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);
        if (toolWindow != null) {
            for (JTree tree : UIUtil.findComponentsOfType(toolWindow.getComponent(), JTree.class)) {
                SubtabGroupLocationHover.refreshPopupPresentation(project, tree);
            }
        }
    }

    private static void scheduleMainTabPresentationRefresh(@NotNull Project project, @NotNull VirtualFile file) {
        ApplicationManager.getApplication().invokeLater(
                () -> ComponentSubtabsManager.refreshMainTabPresentation(project, file),
                ModalityState.nonModal()
        );
    }

    static void attachToAlreadyOpenFiles(@NotNull Project project) {
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            return;
        }
        ComponentSubtabsDocumentListener.install(project);
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (VirtualFile file : manager.getOpenFiles()) {
            ComponentSubtabsManager.attachIfNeeded(project, file);
            ComponentSubtabsManager.updateSelectionForFile(project, file);
            SidetabsManager.attachIfNeeded(project, file);
        }
        ComponentSubtabsManager.refreshOpenStates(project);
        ComponentSubtabsManager.refreshAllMainTabPresentations(project);
        ComponentSubtabMainTabColors.refresh(project);
    }
}
