package de.sasbe.subtabs;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;

final class SubtabsPresentation {
    private SubtabsPresentation() {
    }

    static void applySettingsChange() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (project.isDisposed()) {
                continue;
            }
            ComponentSubtabGroupRegistry.getInstance(project).clearGroups();
            ComponentSubtabsManager.applyPresentationState(project);
            ComponentSubtabsManager.refreshAppearance(project);
            ComponentSubtabsFileEditorListener.attachToAlreadyOpenFiles(project);
            ComponentSubtabMainTabSelectPopup.installOn(project);
        }
        refreshProjectViews();
    }

    static void refreshProjectViews() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (!project.isDisposed()) {
                ProjectView.getInstance(project).refresh();
                SubtabGroupTreeControl.installOn(project);
                ComponentSubtabProjectViewEditorHover.installOn(project);
            }
        }
    }

    static void refreshGroupColors() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (project.isDisposed()) {
                continue;
            }
            ComponentSubtabsManager.refreshAppearance(project);
        }
        refreshProjectViews();
    }
}
