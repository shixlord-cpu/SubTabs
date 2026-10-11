package com.zayax.tabz;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.PROJECT)
final class SubtabProjectViewGroupingBusyState {
    private int depth;

    static @NotNull SubtabProjectViewGroupingBusyState getInstance(@NotNull Project project) {
        return project.getService(SubtabProjectViewGroupingBusyState.class);
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

    SubtabProjectViewGroupingBusyState(@NotNull Project project) {
        this.project = project;
    }
}
