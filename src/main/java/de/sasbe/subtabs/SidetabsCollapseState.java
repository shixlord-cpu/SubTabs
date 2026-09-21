package de.sasbe.subtabs;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.PROJECT)
public final class SidetabsCollapseState {
    public static @NotNull SidetabsCollapseState getInstance(@NotNull Project project) {
        return project.getService(SidetabsCollapseState.class);
    }

    public boolean isCollapsed() {
        return !SubtabsSettings.getInstance().isSidetabsExpanded();
    }

    public void setCollapsed(@NotNull Project project, boolean collapsed) {
        boolean expanded = !collapsed;
        if (SubtabsSettings.getInstance().isSidetabsExpanded() == expanded) {
            return;
        }
        SubtabsSettings.getInstance().setSidetabsExpanded(expanded);
        SidetabsManager.refreshAllOpenProjects();
    }

    public void toggle(@NotNull Project project) {
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            return;
        }
        setCollapsed(project, !isCollapsed());
    }
}
