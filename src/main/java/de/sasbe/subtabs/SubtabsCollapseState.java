package de.sasbe.subtabs;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.PROJECT)
public final class SubtabsCollapseState {
    public static @NotNull SubtabsCollapseState getInstance(@NotNull Project project) {
        return project.getService(SubtabsCollapseState.class);
    }

    public boolean isCollapsed() {
        return !SubtabsSettings.getInstance().isSubtabsActive();
    }

    public void setCollapsed(@NotNull Project project, boolean collapsed) {
        boolean active = !collapsed;
        if (SubtabsSettings.getInstance().isSubtabsActive() == active) {
            return;
        }
        SubtabsSettings.getInstance().setSubtabsActive(active);
        ComponentSubtabsManager.refreshAllOpenProjects();
    }

    public void toggle(@NotNull Project project) {
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            return;
        }
        setCollapsed(project, !isCollapsed());
    }
}
