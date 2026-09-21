package de.sasbe.subtabs;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

/**
 * Shared {@code Familia} submenu for the project view and editor-tab platform context menus.
 */
public final class SubtabFamiliaActionGroup extends DefaultActionGroup implements DumbAware {
    public SubtabFamiliaActionGroup() {
        super("Familia", true);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        boolean projectView = SubtabGroupProjectViewContext.selectedGroupNode(event) != null;
        boolean editorTab = SubtabFamiliaContext.editorTabFile(event) != null;
        boolean visible = SubtabsSettings.getInstance().isFamiliaEnabled()
                && SubtabsSettings.getInstance().isSubtabsActive()
                && (projectView || editorTab);
        event.getPresentation().setEnabledAndVisible(visible);
    }
}
