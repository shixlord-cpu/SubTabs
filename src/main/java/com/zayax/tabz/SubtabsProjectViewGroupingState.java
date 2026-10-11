package com.zayax.tabz;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.PROJECT)
public final class SubtabsProjectViewGroupingState {
    public static @NotNull SubtabsProjectViewGroupingState getInstance(@NotNull Project project) {
        return project.getService(SubtabsProjectViewGroupingState.class);
    }

    public boolean isCollapsed() {
        return !TabzSettings.getInstance().isProjectViewGroupingActive();
    }

    public void setCollapsed(@NotNull Project project, boolean collapsed) {
        boolean active = !collapsed;
        if (TabzSettings.getInstance().isProjectViewGroupingActive() == active) {
            return;
        }
        TabzSettings.getInstance().setProjectViewGroupingActive(active);
        TabzPresentation.refreshProjectViewGrouping(project);
        SubtabsProjectViewGroupingOverlay.refresh(project);
    }

    public void toggle(@NotNull Project project) {
        setCollapsed(project, !isCollapsed());
    }
}
