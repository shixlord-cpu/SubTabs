package de.sasbe.subtabs;

import com.intellij.openapi.actionSystem.ActionPlaces;
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
        boolean visible = SubtabFamiliaContext.showInProjectViewPopup(event)
                || isEditorTabFamiliaMenu(event);
        event.getPresentation().setEnabledAndVisible(visible);
    }

    private static boolean isEditorTabFamiliaMenu(@NotNull AnActionEvent event) {
        return SubtabsSettings.getInstance().isFamiliaEnabled()
                && SubtabsSettings.getInstance().isSubtabsActive()
                && ActionPlaces.EDITOR_TAB_POPUP.equals(event.getPlace())
                && SubtabFamiliaContext.editorTabFile(event) != null;
    }
}
