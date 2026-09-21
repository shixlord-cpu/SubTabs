package de.sasbe.subtabs;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

final class SubtabRevealInProjectViewAction extends AnAction implements DumbAware {
    SubtabRevealInProjectViewAction() {
        super("Im Projektbaum anzeigen");
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        SubtabFamiliaContext.updateRevealAction(event);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        VirtualFile file = SubtabFamiliaContext.editorTabFile(event);
        if (project == null || file == null) {
            return;
        }
        SubtabProjectViewReveal.revealSubtab(project, file);
    }
}
