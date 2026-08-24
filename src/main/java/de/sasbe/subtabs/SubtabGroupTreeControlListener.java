package de.sasbe.subtabs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import org.jetbrains.annotations.NotNull;

final class SubtabGroupTreeControlListener implements ToolWindowManagerListener {
    private final Project project;

    SubtabGroupTreeControlListener(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void toolWindowShown(@NotNull ToolWindow toolWindow) {
        if (ToolWindowId.PROJECT_VIEW.equals(toolWindow.getId())) {
            SubtabGroupTreeControl.installOn(project);
            ComponentSubtabProjectViewEditorHover.installOn(project);
        }
    }

    @Override
    public void stateChanged(@NotNull ToolWindowManager toolWindowManager) {
        SubtabGroupTreeControl.installOn(project);
    }
}
