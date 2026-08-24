package de.sasbe.subtabs;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.PROJECT)
public final class SubtabsCollapseState {
    private boolean collapsed;

    public static @NotNull SubtabsCollapseState getInstance(@NotNull Project project) {
        return project.getService(SubtabsCollapseState.class);
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(@NotNull Project project, boolean collapsed) {
        if (this.collapsed == collapsed) {
            return;
        }
        this.collapsed = collapsed;
        ComponentSubtabsManager.applyCollapsedState(project, collapsed);
    }

    public void toggle(@NotNull Project project) {
        setCollapsed(project, !collapsed);
    }
}
