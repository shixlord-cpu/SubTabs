package de.sasbe.subtabs;

import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.Color;
import java.awt.Component;

final class SubtabGroupTreeCellRenderer implements TreeCellRenderer {
    private static final float LOCATION_COLOR_OPACITY = 0.5f;
    private final TreeCellRenderer delegate;

    SubtabGroupTreeCellRenderer(@NotNull TreeCellRenderer delegate) {
        this.delegate = delegate;
    }

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus
    ) {
        Component component = delegate.getTreeCellRendererComponent(
                tree,
                value,
                selected,
                expanded,
                leaf,
                row,
                hasFocus
        );

        SimpleColoredComponent colored = findColoredComponent(component);
        if (colored == null) {
            return component;
        }

        Project project = projectFor(tree, row);
        if (project == null) {
            return component;
        }

        TreePath path = tree.getPathForRow(row);
        if (path == null) {
            return component;
        }

        Object userObject = TreeUtil.getLastUserObject(path);
        if (userObject instanceof SubtabGroupProjectViewNode groupNode) {
            if (groupNode.hasModifiedMember()) {
                applyModifiedMainText(colored, ComponentSubtabModifiedUi.foreground(true, false));
            }
            Color groupColor = SubtabGroupColors.colorForGroupNode(groupNode);
            if (groupColor != null && groupNode.members().size() > 1) {
                boolean locationHovered = SubtabGroupLocationHover.isLocationFragmentHovered(tree, row);
                applyGroupColorToLocationFragment(colored, groupColor, locationHovered);
            } else if (SubtabGroupLocationHover.isLocationFragmentHovered(tree, row)) {
                SubtabGroupLocationHover.brightenLocationFragment(colored);
            }
            return component;
        }

        if (SubtabGroupLocationHover.isLocationFragmentHovered(tree, row)) {
            SubtabGroupLocationHover.brightenLocationFragment(colored);
        }

        VirtualFile file = ComponentSubtabProjectViewHover.virtualFileOf(path);
        if (file != null
                && ComponentFileNaming.componentBaseName(file.getName()) != null
                && ComponentSubtabModifiedUi.isModified(project, file)) {
            applyModifiedMainText(colored, ComponentSubtabModifiedUi.foreground(true, false));
        }

        return component;
    }

    static void applyModifiedMainText(@NotNull SimpleColoredComponent colored, @NotNull Color color) {
        SimpleTextAttributes attributes = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, color);
        for (SimpleColoredComponent.ColoredIterator iterator = colored.iterator(); iterator.hasNext(); ) {
            String fragment = iterator.next();
            if (!fragment.isBlank() && !fragment.contains("Dateien")) {
                iterator.setTextAttributes(attributes);
                return;
            }
        }
    }

    static void applyGroupColorToLocationFragment(
            @NotNull SimpleColoredComponent colored,
            @NotNull Color groupColor,
            boolean hovered
    ) {
        Color textColor = hovered ? groupColor : withOpacity(groupColor, LOCATION_COLOR_OPACITY);
        SimpleTextAttributes attributes = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, textColor);
        for (SimpleColoredComponent.ColoredIterator iterator = colored.iterator(); iterator.hasNext(); ) {
            String fragment = iterator.next();
            if (fragment.contains("Dateien")) {
                iterator.setTextAttributes(attributes);
                return;
            }
        }
    }

    private static @NotNull Color withOpacity(@NotNull Color color, float opacity) {
        int alpha = Math.max(0, Math.min(255, Math.round(255f * opacity)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private static @Nullable Project projectFor(@NotNull JTree tree, int row) {
        TreePath path = tree.getPathForRow(row);
        if (path == null) {
            return null;
        }
        Object userObject = TreeUtil.getLastUserObject(path);
        if (userObject instanceof AbstractTreeNode<?> node) {
            return node.getProject();
        }
        return null;
    }

    private static @Nullable SimpleColoredComponent findColoredComponent(@NotNull Component component) {
        if (component instanceof SimpleColoredComponent colored) {
            return colored;
        }
        if (component instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                SimpleColoredComponent colored = findColoredComponent(child);
                if (colored != null) {
                    return colored;
                }
            }
        }
        return null;
    }
}
