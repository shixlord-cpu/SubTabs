package de.sasbe.subtabs;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

abstract class SubtabSelectAdjacentAction extends AnAction implements DumbAware {
    private final int direction;

    SubtabSelectAdjacentAction(@NotNull String text, @NotNull String description, int direction) {
        super(text, description, null);
        this.direction = direction;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        event.getPresentation().setVisible(false);
        event.getPresentation().setEnabled(
                project != null
                        && editor != null
                        && ComponentSubtabNavigation.canSwitchAdjacent(project, direction)
        );
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project != null) {
            ComponentSubtabNavigation.switchAdjacentSubtab(project, direction);
        }
    }
}
