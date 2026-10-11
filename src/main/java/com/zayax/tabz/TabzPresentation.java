package com.zayax.tabz;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.NotNull;

final class TabzPresentation {
    private TabzPresentation() {
    }

    static void refreshTypography() {
        if (!TabzSettings.getInstance().isTabzEnabled()) {
            return;
        }
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (!project.isDisposed()) {
                ComponentSubtabsManager.refreshAppearance(project);
            }
        }
    }

    static void applySettingsChange() {
        ComponentFileNaming.invalidateRulesCache();
        if (!TabzSettings.getInstance().isTabzEnabled()) {
            TabzLifecycle.shutdownAll();
            return;
        }
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (project.isDisposed()) {
                continue;
            }
            ComponentSubtabsManager.applySettingsChange(project);
            TabzLifecycle.activate(project);
        }
        TabzSettings settings = TabzSettings.getInstance();
        if (settings.isProjectViewGroupingActive() || !settings.isProjectViewGroupingEnabled()) {
            refreshProjectViews();
        } else {
            refreshProjectViewOverlaysOnly();
        }
    }

    static boolean refreshesProjectViewOnSettingsChange() {
        return TabzSettings.getInstance().isProjectViewGroupingActive();
    }

    private static void refreshProjectViewOverlaysOnly() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (!project.isDisposed()) {
                SubtabsProjectViewGroupingOverlay.refresh(project);
            }
        }
    }

    static void refreshProjectViews() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (!project.isDisposed()) {
                refreshProjectViewGrouping(project);
            }
        }
    }

    static void refreshProjectViewGrouping(@NotNull Project project) {
        if (project.isDisposed()) {
            return;
        }
        startProjectViewGroupingBusy(project);
        ApplicationManager.getApplication().invokeLater(
                () -> runProjectViewGroupingRefresh(project),
                project.getDisposed()
        );
    }

    private static void startProjectViewGroupingBusy(@NotNull Project project) {
        SubtabProjectViewGroupingBusyState busyState = SubtabProjectViewGroupingBusyState.getInstance(project);
        if (!busyState.isBusy()) {
            busyState.begin();
            SubtabsProjectViewGroupingOverlay.flushBusyPresentation(project);
        }
    }

    private static void runProjectViewGroupingRefresh(@NotNull Project project) {
        if (project.isDisposed()) {
            SubtabProjectViewGroupingBusyState.getInstance(project).end();
            return;
        }
        ProjectView.getInstance(project).refresh();
        SubtabGroupTreeControl.installOn(project);
        SubtabsProjectViewGroupingOverlay.installNow(project);
        SubtabsProjectViewGroupingOverlay.finishBusyWhenReady(project);
    }

    static void refreshGroupColors() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (project.isDisposed()) {
                continue;
            }
            ComponentSubtabMainTabColors.refresh(project);
        }
        refreshProjectViews();
    }
}
