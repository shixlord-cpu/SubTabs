package de.sasbe.subtabs;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.PROJECT)
public final class SubtabsProjectViewGroupingState {
    public static @NotNull SubtabsProjectViewGroupingState getInstance(@NotNull Project project) {
        return project.getService(SubtabsProjectViewGroupingState.class);
    }

    public boolean isCollapsed() {
        return !SubtabsSettings.getInstance().isProjectViewGroupingActive();
    }

    public void setCollapsed(@NotNull Project project, boolean collapsed) {
        boolean active = !collapsed;
        if (SubtabsSettings.getInstance().isProjectViewGroupingActive() == active) {
            return;
        }
        SubtabsSettings.getInstance().setProjectViewGroupingActive(active);
        SubtabsPresentation.refreshProjectViewGrouping(project);
        SubtabsProjectViewGroupingOverlay.refresh(project);
    }

    public void toggle(@NotNull Project project) {
        setCollapsed(project, !isCollapsed());
    }
}
