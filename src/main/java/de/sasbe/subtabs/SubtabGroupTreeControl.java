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
import com.intellij.ui.tree.ui.DefaultControl;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreePath;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Function;

final class SubtabGroupTreeControl {
    static final Color FILL = new Color(0x3B82F6);
    static final Color BORDER = new JBColor(new Color(0x8A8A8A), new Color(0x6B7280));
    private static final String INSTALLED = "subtabs.groupTreeControl";
    private static final String EXPANSION_GUARD = "subtabs.groupTreeExpansionGuard";

    private SubtabGroupTreeControl() {
    }

    static void installOn(@NotNull Project project) {
        if (project.isDisposed()) {
            return;
        }
        AbstractProjectViewPane pane = ProjectView.getInstance(project).getCurrentProjectViewPane();
        if (pane != null && pane.getTree() != null) {
            refresh(pane.getTree());
            ComponentSubtabProjectViewEditorHover.installOn(project);
        }
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);
        if (toolWindow != null) {
            for (JTree tree : UIUtil.findComponentsOfType(toolWindow.getComponent(), JTree.class)) {
                refresh(tree);
            }
        }
        if (!SubtabsSettings.getInstance().getGroupTreeControlStyle().allowsGroupExpansion()) {
            collapseExpandedSubtabGroups(project);
        }
    }

    static void refresh(@NotNull JTree tree) {
        attach(tree);
        tree.repaint();
    }

    static void attach(@NotNull JTree tree) {
        Function<TreePath, Control> current = ClientProperty.get(tree, Control.CUSTOM_CONTROL);
        if (!(current instanceof Resolver)) {
            ClientProperty.put(tree, Control.CUSTOM_CONTROL, new Resolver(current));
            tree.putClientProperty(INSTALLED, Boolean.TRUE);
        }
        installExpansionGuard(tree);
    }

    static void collapseExpandedSubtabGroups(@NotNull Project project) {
        AbstractProjectViewPane pane = ProjectView.getInstance(project).getCurrentProjectViewPane();
        if (pane != null && pane.getTree() != null) {
            collapseExpandedSubtabGroups(pane.getTree());
        }
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);
        if (toolWindow != null) {
            for (JTree tree : UIUtil.findComponentsOfType(toolWindow.getComponent(), JTree.class)) {
                collapseExpandedSubtabGroups(tree);
            }
        }
    }

    private static void collapseExpandedSubtabGroups(@NotNull JTree tree) {
        Object root = tree.getModel().getRoot();
        if (root == null) {
            return;
        }
        collapseExpandedSubtabGroups(tree, new TreePath(root));
    }

    private static void collapseExpandedSubtabGroups(@NotNull JTree tree, @NotNull TreePath path) {
        if (isSubtabGroupPath(path) && tree.isExpanded(path)) {
            tree.collapsePath(path);
        }
        Object node = path.getLastPathComponent();
        int childCount = tree.getModel().getChildCount(node);
        for (int i = 0; i < childCount; i++) {
            Object child = tree.getModel().getChild(node, i);
            collapseExpandedSubtabGroups(tree, path.pathByAddingChild(child));
        }
    }

    private static void installExpansionGuard(@NotNull JTree tree) {
        if (Boolean.TRUE.equals(tree.getClientProperty(EXPANSION_GUARD))) {
            return;
        }
        tree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                if (!SubtabsSettings.getInstance().getGroupTreeControlStyle().allowsGroupExpansion()
                        && isSubtabGroupPath(event.getPath())) {
                    SwingUtilities.invokeLater(() -> tree.collapsePath(event.getPath()));
                }
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
            }
        });
        tree.putClientProperty(EXPANSION_GUARD, Boolean.TRUE);
    }

    static boolean isSubtabGroupPath(@Nullable TreePath path) {
        return path != null && TreeUtil.getLastUserObject(path) instanceof SubtabGroupProjectViewNode;
    }

    static @Nullable Control controlFor(@NotNull SubtabGroupTreeControlStyle style) {
        return switch (style) {
            case DEFAULT -> null;
            case CUBES -> ShapeControl.CUBES;
            case CIRCLES -> ShapeControl.CIRCLES;
            case BLUE_ARROWS -> BlueArrowControl.INSTANCE;
            case NONE -> NoneControl.INSTANCE;
        };
    }

    static @Nullable Control forPath(@NotNull TreePath path) {
        if (!isSubtabGroupPath(path)) {
            return null;
        }
        return controlFor(SubtabsSettings.getInstance().getGroupTreeControlStyle());
    }

    static boolean shapeFilled(boolean expanded, boolean invertFill) {
        return invertFill ? expanded : !expanded;
    }

    static boolean shapeFilled(boolean expanded) {
        return shapeFilled(expanded, SubtabsSettings.getInstance().isInvertGroupTreeControlFill());
    }

    static void paintShape(
            @NotNull Graphics2D graphics,
            @NotNull SubtabGroupTreeControlStyle style,
            int x,
            int y,
            int width,
            int height,
            boolean filled
    ) {
        int size = Math.max(JBUI.scale(8), Math.min(width, height) - JBUI.scale(4));
        int left = x + (width - size) / 2;
        int top = y + (height - size) / 2;
        float strokeWidth = Math.max(1.25f, size / 8f);
        Stroke previousStroke = graphics.getStroke();
        graphics.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (style == SubtabGroupTreeControlStyle.CIRCLES) {
            Ellipse2D.Float shape = new Ellipse2D.Float(left, top, size, size);
            if (filled) {
                graphics.setColor(FILL);
                graphics.fill(shape);
            }
            graphics.setColor(BORDER);
            graphics.draw(shape);
        } else {
            float arc = JBUI.scale(2.5f);
            RoundRectangle2D.Float shape = new RoundRectangle2D.Float(left, top, size, size, arc, arc);
            if (filled) {
                graphics.setColor(FILL);
                graphics.fill(shape);
            }
            graphics.setColor(BORDER);
            graphics.draw(shape);
        }
        graphics.setStroke(previousStroke);
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

    private static final class ShapeControl implements Control {
        private static final ShapeControl CUBES = new ShapeControl(SubtabGroupTreeControlStyle.CUBES);
        private static final ShapeControl CIRCLES = new ShapeControl(SubtabGroupTreeControlStyle.CIRCLES);

        private final SubtabGroupTreeControlStyle style;

        private ShapeControl(@NotNull SubtabGroupTreeControlStyle style) {
            this.style = style;
        }

        @Override
        public @NotNull Icon getIcon(boolean expanded, boolean selected) {
            return new ShapeIcon(style, expanded);
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
                paintShape(g2, style, x, y, width, height, shapeFilled(expanded));
            } finally {
                g2.dispose();
            }
        }
    }

    private record ShapeIcon(@NotNull SubtabGroupTreeControlStyle style, boolean expanded) implements Icon {
        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                paintShape(g2, style, x, y, getIconWidth(), getIconHeight(), shapeFilled(expanded));
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return JBUI.scale(16);
        }

        @Override
        public int getIconHeight() {
            return JBUI.scale(16);
        }
    }

    private static final class NoneControl implements Control {
        private static final NoneControl INSTANCE = new NoneControl();
        private static final Icon EMPTY = EmptyIcon.create(0);

        @Override
        public @NotNull Icon getIcon(boolean expanded, boolean selected) {
            return EMPTY;
        }

        @Override
        public int getWidth() {
            return 0;
        }

        @Override
        public int getHeight() {
            return 0;
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
        }
    }

    private static final class BlueArrowControl implements Control {
        private static final BlueArrowControl INSTANCE = new BlueArrowControl();
        private final DefaultControl delegate;

        private BlueArrowControl() {
            delegate = new DefaultControl(
                    tint(UIUtil.getTreeExpandedIcon()),
                    tint(UIUtil.getTreeCollapsedIcon()),
                    tint(UIUtil.getTreeSelectedExpandedIcon()),
                    tint(UIUtil.getTreeSelectedCollapsedIcon())
            );
        }

        private static @NotNull Icon tint(@NotNull Icon icon) {
            return IconUtil.colorize(icon, FILL);
        }

        @Override
        public @NotNull Icon getIcon(boolean expanded, boolean selected) {
            return delegate.getIcon(expanded, selected);
        }

        @Override
        public int getWidth() {
            return delegate.getWidth();
        }

        @Override
        public int getHeight() {
            return delegate.getHeight();
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
            delegate.paint(component, graphics, x, y, width, height, expanded, selected);
        }
    }
}
