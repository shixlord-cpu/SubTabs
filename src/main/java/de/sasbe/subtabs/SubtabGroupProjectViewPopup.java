package de.sasbe.subtabs;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ColorChooserService;
import com.intellij.ui.PopupHandler;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.TreePath;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;

final class SubtabGroupProjectViewPopup {
    private static final String POPUP_INSTALLED = "componentSubtabs.groupColorPopup";

    private SubtabGroupProjectViewPopup() {
    }

    static void installOn(@NotNull Project project, @NotNull JTree tree) {
        if (Boolean.TRUE.equals(tree.getClientProperty(POPUP_INSTALLED))) {
            return;
        }
        tree.putClientProperty(POPUP_INSTALLED, Boolean.TRUE);
        tree.addMouseListener(new PopupHandler() {
            @Override
            public void invokePopup(@NotNull Component comp, int x, int y) {
                if (!(comp instanceof JTree targetTree) || !SubtabGroupColors.isEnabled()) {
                    return;
                }

                TreePath path = targetTree.getPathForLocation(x, y);
                if (path == null) {
                    return;
                }

                Object userObject = TreeUtil.getLastUserObject(path);
                if (!(userObject instanceof SubtabGroupProjectViewNode groupNode)) {
                    return;
                }

                showContextMenu(project, groupNode, targetTree, x, y);
            }
        });
    }

    static void showContextMenu(
            @NotNull Project project,
            @NotNull SubtabGroupProjectViewNode groupNode,
            @NotNull JComponent component,
            int x,
            int y
    ) {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new ChangeGroupColorAction(project, groupNode));
        group.addSeparator();
        group.add(new ResetGroupColorAction(groupNode));
        ActionManager.getInstance()
                .createActionPopupMenu("SubTabs.GroupColorContextMenu", group)
                .getComponent()
                .show(component, x, y);
    }

    private static final class ChangeGroupColorAction extends AnAction {
        private final SubtabGroupProjectViewNode groupNode;

        private ChangeGroupColorAction(@NotNull Project project, @NotNull SubtabGroupProjectViewNode groupNode) {
            super("Gruppenfarbe ändern…");
            this.groupNode = groupNode;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            String key = colorStorageKey(groupNode);
            if (key == null) {
                return;
            }

            Color current = SubtabGroupColors.colorForKey(key);
            Component parent = event.getInputEvent() instanceof MouseEvent mouseEvent
                    ? mouseEvent.getComponent()
                    : null;
            Color chosen = ColorChooserService.getInstance().showDialog(
                    parent,
                    "Gruppenfarbe wählen",
                    current != null ? current : Color.GRAY,
                    false,
                    null
            );
            if (chosen == null) {
                return;
            }

            SubtabGroupColors.setColor(key, chosen);
            SubtabsPresentation.refreshGroupColors();
        }
    }

    private static final class ResetGroupColorAction extends AnAction {
        private final SubtabGroupProjectViewNode groupNode;

        private ResetGroupColorAction(@NotNull SubtabGroupProjectViewNode groupNode) {
            super("Gruppenfarbe neu zuweisen");
            this.groupNode = groupNode;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            String key = colorStorageKey(groupNode);
            if (key == null) {
                return;
            }

            SubtabsSettings.getInstance().getGroupColorHexes().remove(key);
            SubtabGroupColors.ensureColor(key);
            SubtabsPresentation.refreshGroupColors();
        }
    }

    private static @Nullable String colorStorageKey(@NotNull SubtabGroupProjectViewNode groupNode) {
        if (groupNode.getVirtualFile() == null) {
            return null;
        }
        String key = SubtabGroupColors.colorKey(groupNode.getVirtualFile());
        return key != null ? key : groupNode.groupKey();
    }
}
