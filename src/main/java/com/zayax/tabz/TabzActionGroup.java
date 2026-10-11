package com.zayax.tabz;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

/**
 * Shared {@code Tabz} submenu for the project view and editor-tab platform context menus.
 */
public final class TabzActionGroup extends DefaultActionGroup implements DumbAware {
    public TabzActionGroup() {
        super("TabZ", true);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        boolean visible = TabzContext.showInProjectViewPopup(event)
                || isEditorTabTabzMenu(event);
        event.getPresentation().setEnabledAndVisible(visible);
    }

    private static boolean isEditorTabTabzMenu(@NotNull AnActionEvent event) {
        return TabzSettings.getInstance().isTabzEnabled()
                && TabzSettings.getInstance().isSubtabsActive()
                && ActionPlaces.EDITOR_TAB_POPUP.equals(event.getPlace())
                && TabzContext.editorTabFile(event) != null;
    }
}
