package com.zayax.tabz;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * Tears down or restores all Tabz UI when the plugin is disabled in settings.
 */
final class TabzLifecycle {
    private TabzLifecycle() {
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

        ComponentSubtabEditorSplitPresentation.shutdownForTabz(project);
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

        TabzSettings settings = TabzSettings.getInstance();
        if (SubtabProjectViewGrouping.isEnabled() || settings.isProjectViewGroupingEnabled()) {
            ProjectView.getInstance(project).refresh();
        }
    }

    static void activate(@NotNull Project project) {
        if (project.isDisposed() || !TabzSettings.getInstance().isTabzEnabled()) {
            return;
        }
        ComponentSubtabDocumentListener.install(project);
        ComponentSubtabMainTabSelectPopup.installOn(project);
        ComponentSubtabMainTabColors.refresh(project);
        SubtabGroupTreeControl.installOn(project);
        ComponentSubtabProjectViewEditorHover.installOn(project);
        SubtabsProjectViewGroupingOverlay.installOn(project);
        ComponentSubtabFileEditorListener.attachToAlreadyOpenFiles(project);
        SplittabRestoreOverlay.syncProject(project);
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
        SplittabRestoreOverlay.hide(editor);
    }
}
