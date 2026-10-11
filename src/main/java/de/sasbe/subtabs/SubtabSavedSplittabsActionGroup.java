package de.sasbe.subtabs;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Familia-Untermenü: gespeicherte Splittab-Verknüpfungen öffnen.
 */
public final class SubtabSavedSplittabsActionGroup extends DefaultActionGroup implements DumbAware {
    public SubtabSavedSplittabsActionGroup() {
        super("Open split pairs", true);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            event.getPresentation().setEnabledAndVisible(false);
            return;
        }
        Project project = event.getProject();
        if (project == null) {
            event.getPresentation().setEnabledAndVisible(false);
            return;
        }
        Context context = resolveContext(event);
        List<ComponentSubtabEditorSplitRegistry.SplittabPair> pairs =
                ComponentSubtabEditorSplitFamiliaMenu.closedPairsForContext(
                        project,
                        context.file(),
                        context.groupNode()
                );
        boolean visible = context.file() != null || context.groupNode() != null;
        event.getPresentation().setEnabledAndVisible(visible && !pairs.isEmpty());
    }

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent event) {
        if (event == null) {
            return EMPTY_ARRAY;
        }
        Project project = event.getProject();
        if (project == null) {
            return EMPTY_ARRAY;
        }
        Context context = resolveContext(event);
        DefaultActionGroup group = new DefaultActionGroup();
        ComponentSubtabEditorSplitFamiliaMenu.addOpenSavedSplittabActions(
                group,
                project,
                context.file(),
                context.groupNode(),
                false
        );
        return group.getChildActionsOrStubs();
    }

    private static @NotNull Context resolveContext(@NotNull AnActionEvent event) {
        SubtabGroupProjectViewNode groupNode = SubtabGroupProjectViewContext.selectedGroupNode(event);
        VirtualFile file = SubtabFamiliaContext.editorTabFile(event);
        if (file == null) {
            file = SubtabFamiliaContext.fileForColorActions(event);
        }
        if (file == null && groupNode == null) {
            file = SubtabGroupProjectViewContext.selectedVirtualFile(event);
        }
        return new Context(file, groupNode);
    }

    private record Context(@Nullable VirtualFile file, @Nullable SubtabGroupProjectViewNode groupNode) {
    }
}
