package de.sasbe.subtabs;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.ClientProperty;
import com.intellij.ui.JBColor;
import com.intellij.ui.tree.ui.Control;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.TreePath;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.function.Function;

final class SubtabGroupTreeControl implements Control {
    static final SubtabGroupTreeControl INSTANCE = new SubtabGroupTreeControl();
    static final Color FILL = new Color(0x3B82F6);
    private static final String INSTALLED = "subtabs.groupTreeControl";

    private SubtabGroupTreeControl() {
    }

    static void installOn(@NotNull Project project) {
        if (project.isDisposed()) {
            return;
        }
        AbstractProjectViewPane pane = ProjectView.getInstance(project).getCurrentProjectViewPane();
        if (pane != null && pane.getTree() != null) {
            attach(pane.getTree());
        }
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);
        if (toolWindow != null) {
            for (JTree tree : UIUtil.findComponentsOfType(toolWindow.getComponent(), JTree.class)) {
                attach(tree);
            }
        }
    }

    static void attach(@NotNull JTree tree) {
        if (Boolean.TRUE.equals(tree.getClientProperty(INSTALLED))) {
            return;
        }
        Function<TreePath, Control> previous = ClientProperty.get(tree, Control.CUSTOM_CONTROL);
        ClientProperty.put(tree, Control.CUSTOM_CONTROL, new Resolver(previous));
        tree.putClientProperty(INSTALLED, Boolean.TRUE);
        tree.repaint();
    }

    static boolean isSubtabGroupPath(@Nullable TreePath path) {
        return path != null && TreeUtil.getLastUserObject(path) instanceof SubtabGroupProjectViewNode;
    }

    static @Nullable Control forPath(@NotNull TreePath path) {
        return isSubtabGroupPath(path) ? INSTANCE : null;
    }

    static void paintSquare(
            @NotNull Graphics2D graphics,
            int x,
            int y,
            int width,
            int height,
            boolean filled,
            @NotNull Color color
    ) {
        int size = Math.max(JBUI.scale(8), Math.min(width, height) - JBUI.scale(4));
        int left = x + (width - size) / 2;
        int top = y + (height - size) / 2;
        graphics.setColor(color);
        if (filled) {
            graphics.fillRect(left, top, size, size);
            return;
        }
        float strokeWidth = Math.max(1.5f, size / 7f);
        Stroke previous = graphics.getStroke();
        graphics.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        int inset = Math.round(strokeWidth / 2f);
        graphics.drawRect(left + inset, top + inset, size - 2 * inset - 1, size - 2 * inset - 1);
        graphics.setStroke(previous);
    }

    @Override
    public @NotNull Icon getIcon(boolean expanded, boolean selected) {
        return new SquareIcon(expanded, selected);
    }

    @Override
    public int getWidth() {
        return JBUI.scale(16);
    }

    @Override
    public int getHeight() {
        return JBUI.scale(16);
    }

    @Override
    public void paint(
            @NotNull Component component,
            @NotNull Graphics graphics,
            int x,
            int y,
            int width,
            int height,
            boolean expanded,
            boolean selected
    ) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            paintSquare(g2, x, y, width, height, !expanded, color(selected));
        } finally {
            g2.dispose();
        }
    }

    private static @NotNull Color color(boolean selected) {
        if (!selected) {
            return FILL;
        }
        Color foreground = UIUtil.getTreeForeground(true, true);
        return foreground == null ? JBColor.WHITE : foreground;
    }

    private record Resolver(@Nullable Function<TreePath, Control> previous)
            implements Function<TreePath, Control> {
        @Override
        public Control apply(TreePath path) {
            Control group = forPath(path);
            if (group != null) {
                return group;
            }
            return previous == null ? null : previous.apply(path);
        }
    }

    private record SquareIcon(boolean expanded, boolean selected) implements Icon {
        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            INSTANCE.paint(
                    component == null ? new JPanel() : component,
                    graphics,
                    x,
                    y,
                    getIconWidth(),
                    getIconHeight(),
                    expanded,
                    selected
            );
        }

        @Override
        public int getIconWidth() {
            return INSTANCE.getWidth();
        }

        @Override
        public int getIconHeight() {
            return INSTANCE.getHeight();
        }
    }
}
