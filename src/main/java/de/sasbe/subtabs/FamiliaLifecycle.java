package de.sasbe.subtabs;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * Tears down or restores all Familia UI when the plugin is disabled in settings.
 */
final class FamiliaLifecycle {
    private FamiliaLifecycle() {
    }

    static void shutdownAll() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (!project.isDisposed()) {
                shutdown(project);
            }
        }
    }

    static void shutdown(@NotNull Project project) {
        if (project.isDisposed()) {
            return;
        }

        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : manager.getAllEditors()) {
            hideEditorOverlays(editor);
        }

        ComponentSubtabsManager.shutdown(project);
        SidetabsManager.shutdown(project);
        ComponentSubtabGroupSplitRegistry.getInstance(project).clear();

        ComponentRelatedFilesCache.getInstance(project).clear();
        SidetabSectionsCache.getInstance(project).clear();
        ComponentSubtabGroupRegistry.getInstance(project).clearGroups();

        SubtabsProjectViewGroupingOverlay.disposeAll(project);
        ComponentSubtabMainTabSelectPopup.hideAllPopups(project);
        ComponentSubtabMainTabColors.refresh(project);
        ComponentSubtabMainTabErrorWaves.refresh(project);

        for (VirtualFile file : manager.getOpenFiles()) {
            manager.updateFilePresentation(file);
        }

        if (SubtabProjectViewGrouping.isEnabled()) {
            ProjectView.getInstance(project).refresh();
        } else if (SubtabsSettings.getInstance().isGroupRelatedFilesInProjectView()) {
            ProjectView.getInstance(project).refresh();
        }
    }

    static void activate(@NotNull Project project) {
        if (project.isDisposed() || !SubtabsSettings.getInstance().isFamiliaEnabled()) {
            return;
        }
        ComponentSubtabsDocumentListener.install(project);
        ComponentSubtabMainTabSelectPopup.installOn(project);
        ComponentSubtabMainTabColors.refresh(project);
        SubtabGroupTreeControl.installOn(project);
        ComponentSubtabProjectViewEditorHover.installOn(project);
        SubtabsProjectViewGroupingOverlay.installOn(project);
        ComponentSubtabsFileEditorListener.attachToAlreadyOpenFiles(project);
    }

    static void activateAll() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (!project.isDisposed()) {
                activate(project);
            }
        }
    }

    private static void hideEditorOverlays(@NotNull FileEditor editor) {
        SubtabsExpandOverlay.hide(editor);
        SubtabsCollapseOverlay.hide(editor);
        RuleSwitchOverlay.hide(editor);
        SidetabsToggleOverlay.hide(editor);
        SidetabBarOverlay.hide(editor);
    }
}
