package de.sasbe.subtabs;

import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Wraps the project tree so a trailing control can stay on the top-right of the visible viewport.
 */
final class ProjectViewTreeOverlayPanel extends JPanel implements Scrollable {
    private static final Key<ViewportLayoutListener> VIEWPORT_LAYOUT_LISTENER_KEY =
            Key.create("componentSubtabs.projectViewTreeOverlayViewportListener");

    private final JTree tree;
    private final JComponent trailingControl;
    private final ComponentAdapter scrollPaneResizeListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent event) {
            layoutChildren();
        }
    };

    ProjectViewTreeOverlayPanel(@NotNull JTree tree, @NotNull JComponent trailingControl) {
        this.tree = tree;
        this.trailingControl = trailingControl;
        setLayout(null);
        setOpaque(false);
        add(tree);
        add(trailingControl);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                layoutChildren();
            }
        });
        tree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                scheduleRelayout();
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                scheduleRelayout();
            }
        });
        tree.getModel().addTreeModelListener(new TreeModelListener() {
            @Override
            public void treeNodesChanged(TreeModelEvent event) {
                scheduleRelayout();
            }

            @Override
            public void treeNodesInserted(TreeModelEvent event) {
                scheduleRelayout();
            }

            @Override
            public void treeNodesRemoved(TreeModelEvent event) {
                scheduleRelayout();
            }

            @Override
            public void treeStructureChanged(TreeModelEvent event) {
                scheduleRelayout();
            }
        });
        layoutChildren();
    }

    @NotNull JTree tree() {
        return tree;
    }

    @NotNull JComponent trailingControl() {
        return trailingControl;
    }

    void relayout() {
        layoutChildren();
        revalidate();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        installViewportTracking();
        layoutChildren();
    }

    @Override
    public void removeNotify() {
        uninstallViewportTracking();
        super.removeNotify();
    }

    @Override
    public Dimension getPreferredSize() {
        return tree.getPreferredSize();
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return tree.getPreferredScrollableViewportSize();
    }

    @Override
    public int getScrollableUnitIncrement(@NotNull Rectangle visibleRect, int orientation, int direction) {
        return tree.getScrollableUnitIncrement(visibleRect, orientation, direction);
    }

    @Override
    public int getScrollableBlockIncrement(@NotNull Rectangle visibleRect, int orientation, int direction) {
        return tree.getScrollableBlockIncrement(visibleRect, orientation, direction);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return tree.getScrollableTracksViewportWidth();
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    private void scheduleRelayout() {
        SwingUtilities.invokeLater(this::relayout);
    }

    private void installViewportTracking() {
        JViewport viewport = viewport();
        if (viewport == null) {
            return;
        }

        if (viewport.getClientProperty(VIEWPORT_LAYOUT_LISTENER_KEY) == null) {
            ViewportLayoutListener listener = new ViewportLayoutListener();
            viewport.addChangeListener(listener);
            viewport.putClientProperty(VIEWPORT_LAYOUT_LISTENER_KEY, listener);
        }

        JScrollPane scrollPane = scrollPane();
        if (scrollPane != null) {
            scrollPane.removeComponentListener(scrollPaneResizeListener);
            scrollPane.addComponentListener(scrollPaneResizeListener);
            scrollPane.getVerticalScrollBar().addComponentListener(scrollPaneResizeListener);
        }
    }

    private void uninstallViewportTracking() {
        JViewport viewport = viewport();
        if (viewport != null) {
            Object listener = viewport.getClientProperty(VIEWPORT_LAYOUT_LISTENER_KEY);
            if (listener instanceof ViewportLayoutListener viewportListener) {
                viewport.removeChangeListener(viewportListener);
            }
            viewport.putClientProperty(VIEWPORT_LAYOUT_LISTENER_KEY, null);
        }

        JScrollPane scrollPane = scrollPane();
        if (scrollPane != null) {
            scrollPane.removeComponentListener(scrollPaneResizeListener);
            scrollPane.getVerticalScrollBar().removeComponentListener(scrollPaneResizeListener);
        }
    }

    private void layoutChildren() {
        Dimension treePreferred = tree.getPreferredSize();
        if (treePreferred.width <= 0 || treePreferred.height <= 0) {
            return;
        }

        tree.setBounds(0, 0, treePreferred.width, treePreferred.height);

        Dimension preferred = trailingControl.getPreferredSize();
        int rowHeight = Math.max(tree.getRowHeight(), 1);
        int controlWidth = Math.max(preferred.width, rowHeight);
        int controlHeight = Math.max(preferred.height, rowHeight);

        VisibleViewport visibleViewport = visibleViewport();
        int buttonX = visibleViewport.x() + Math.max(0, visibleViewport.width() - controlWidth);
        int buttonY = visibleViewport.y();
        trailingControl.setBounds(buttonX, buttonY, controlWidth, controlHeight);
        trailingControl.setVisible(true);
        setComponentZOrder(trailingControl, 0);
    }

    private @NotNull VisibleViewport visibleViewport() {
        JViewport viewport = viewport();
        if (viewport == null) {
            Dimension treePreferred = tree.getPreferredSize();
            return new VisibleViewport(0, 0, treePreferred.width, treePreferred.height);
        }

        Point viewPosition = viewport.getViewPosition();
        Dimension extent = viewport.getExtentSize();
        int width = Math.max(extent.width - verticalScrollBarWidth(), 0);
        int height = Math.max(extent.height, 0);
        return new VisibleViewport(viewPosition.x, viewPosition.y, width, height);
    }

    private int verticalScrollBarWidth() {
        JScrollPane scrollPane = scrollPane();
        if (scrollPane == null) {
            return 0;
        }
        JComponent scrollBar = scrollPane.getVerticalScrollBar();
        return scrollBar.isVisible() ? scrollBar.getWidth() : 0;
    }

    private @Nullable JViewport viewport() {
        Component parent = getParent();
        return parent instanceof JViewport viewport ? viewport : null;
    }

    private @Nullable JScrollPane scrollPane() {
        return (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
    }

    private record VisibleViewport(int x, int y, int width, int height) {
    }

    private final class ViewportLayoutListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent event) {
            layoutChildren();
        }
    }
}
