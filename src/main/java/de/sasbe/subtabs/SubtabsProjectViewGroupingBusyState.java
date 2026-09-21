package de.sasbe.subtabs;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.PROJECT)
final class SubtabsProjectViewGroupingBusyState {
    private int depth;

    static @NotNull SubtabsProjectViewGroupingBusyState getInstance(@NotNull Project project) {
        return project.getService(SubtabsProjectViewGroupingBusyState.class);
    }

    void begin() {
        if (depth++ == 0) {
            SubtabsProjectViewGroupingOverlay.setBusy(project, true);
        }
    }

    void end() {
        if (depth > 0 && --depth == 0) {
            SubtabsProjectViewGroupingOverlay.setBusy(project, false);
        }
    }

    boolean isBusy() {
        return depth > 0;
    }

    void reset() {
        depth = 0;
        SubtabsProjectViewGroupingBusyWatcher.getInstance(project).cancelPending();
        SubtabsProjectViewGroupingOverlay.setBusy(project, false);
    }

    private final @NotNull Project project;

    SubtabsProjectViewGroupingBusyState(@NotNull Project project) {
        this.project = project;
    }
}
