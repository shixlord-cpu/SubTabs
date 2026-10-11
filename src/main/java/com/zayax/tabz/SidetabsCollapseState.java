package com.zayax.tabz;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.PROJECT)
public final class SidetabsCollapseState {
    public static @NotNull SidetabsCollapseState getInstance(@NotNull Project project) {
        return project.getService(SidetabsCollapseState.class);
    }

    public boolean isCollapsed() {
        return !TabzSettings.getInstance().isSidetabsExpanded();
    }

    public void setCollapsed(@NotNull Project project, boolean collapsed) {
        boolean expanded = !collapsed;
        if (TabzSettings.getInstance().isSidetabsExpanded() == expanded) {
            return;
        }
        TabzSettings.getInstance().setSidetabsExpanded(expanded);
        SidetabsManager.refreshAllOpenProjects();
    }

    public void toggle(@NotNull Project project) {
        if (!TabzSettings.getInstance().isTabzEnabled()) {
            return;
        }
        setCollapsed(project, !isCollapsed());
    }
}
