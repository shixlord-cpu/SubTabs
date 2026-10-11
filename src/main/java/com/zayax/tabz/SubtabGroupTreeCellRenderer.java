package com.zayax.tabz;

import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.JComponent;
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
        if (file != null) {
            SubtabGroupProjectViewNode enclosingGroup = enclosingGroupNode(path);
            Color groupColor = SubtabGroupColors.colorForProjectViewFile(file, enclosingGroup);
            if (groupColor != null) {
                applyGroupColoredFileIcon(component, colored, project, file, groupColor, tree);
            }
            if (ComponentFileNaming.componentBaseName(file.getName()) != null
                    && ComponentSubtabModifiedUi.isModified(project, file)) {
                applyModifiedMainText(colored, ComponentSubtabModifiedUi.foreground(true, false));
            }
            appendSplittabHoverMarker(colored, tree, row);
        }

        return component;
    }

    private static void appendSplittabHoverMarker(
            @NotNull SimpleColoredComponent colored,
            @NotNull JTree tree,
            int row
    ) {
        String marker = ComponentSubtabProjectViewHover.splittabHoverMarkerForRow(tree, row);
        if (marker == null || marker.isEmpty()) {
            return;
        }
        colored.append("  ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
        colored.append(
                marker,
                new SimpleTextAttributes(
                        SimpleTextAttributes.STYLE_PLAIN,
                        UIUtil.getInactiveTextColor()
                )
        );
    }

    private static @Nullable SubtabGroupProjectViewNode enclosingGroupNode(@NotNull TreePath path) {
        for (TreePath current = path.getParentPath(); current != null; current = current.getParentPath()) {
            Object userObject = TreeUtil.getLastUserObject(current);
            if (userObject instanceof SubtabGroupProjectViewNode groupNode) {
                return groupNode;
            }
        }
        return null;
    }

    private static void applyGroupColoredFileIcon(
            @NotNull Component component,
            @Nullable SimpleColoredComponent colored,
            @NotNull Project project,
            @NotNull VirtualFile file,
            @NotNull Color groupColor,
            @NotNull JTree tree
    ) {
        int flags = Iconable.ICON_FLAG_READ_STATUS;
        Icon tinted = SubtabGroupIconTintCache.peekTintedFileIcon(project, file, groupColor, flags);
        if (tinted == null) {
            SubtabGroupIconTintCache.scheduleTintedFileIcon(project, file, groupColor, flags, tree);
            return;
        }
        if (colored != null) {
            colored.setIcon(tinted);
            return;
        }
        if (component instanceof JComponent jComponent) {
            for (JLabel label : UIUtil.findComponentsOfType(jComponent, JLabel.class)) {
                if (label.getIcon() != null) {
                    label.setIcon(tinted);
                    return;
                }
            }
        }
    }

    static void applyModifiedMainText(@NotNull SimpleColoredComponent colored, @NotNull Color color) {
        SimpleTextAttributes attributes = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, color);
        for (SimpleColoredComponent.ColoredIterator iterator = colored.iterator(); iterator.hasNext(); ) {
            String fragment = iterator.next();
            if (!fragment.isBlank() && !isFileCountFragment(fragment)) {
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
            if (isFileCountFragment(fragment)) {
                iterator.setTextAttributes(attributes);
                return;
            }
        }
    }

    private static boolean isFileCountFragment(@NotNull String fragment) {
        return fragment.contains(" files") || fragment.contains(" Dateien");
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
