package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.LayoutManager;

/**
 * Wraps an editor component so SideTabs can sit EAST/WEST of the code without calling
 * {@code TextEditorComponent.add}, which rejects external callers.
 */
final class SidetabEditorHost extends JPanel {
    SidetabEditorHost() {
        super(new BorderLayout());
        setOpaque(false);
    }

    void setCenter(@NotNull JComponent editorComponent) {
        if (editorComponent.getParent() == this) {
            return;
        }
        add(editorComponent, BorderLayout.CENTER);
    }

    void setSide(@NotNull SidetabBarPanel panel, boolean onRight) {
        Container parent = panel.getParent();
        if (parent != null) {
            parent.remove(panel);
        }
        add(panel, onRight ? BorderLayout.EAST : BorderLayout.WEST);
        revalidate();
        repaint();
    }

    void clearSide() {
        for (Component component : getComponents()) {
            if (component instanceof SidetabBarPanel) {
                remove(component);
            }
        }
        revalidate();
        repaint();
    }

    static void installAround(
            @NotNull JComponent editorComponent,
            @NotNull SidetabBarPanel panel,
            boolean onRight
    ) {
        Container parent = editorComponent.getParent();
        if (parent instanceof SidetabEditorHost host) {
            host.setSide(panel, onRight);
            return;
        }
        if (parent == null) {
            return;
        }

        SidetabEditorHost host = new SidetabEditorHost();
        Object constraint = constraintOf(parent, editorComponent);
        int index = indexOf(parent, editorComponent);
        parent.remove(editorComponent);
        host.setCenter(editorComponent);
        host.setSide(panel, onRight);
        addBack(parent, host, constraint, index);
        parent.revalidate();
        parent.repaint();
    }

    static void detachSide(@NotNull JComponent editorComponent, @Nullable SidetabBarPanel panel) {
        SidetabEditorHost host = findHost(editorComponent);
        if (panel != null && panel.getParent() != null) {
            panel.getParent().remove(panel);
        }
        if (host != null) {
            host.clearSide();
        }
    }

    static @Nullable SidetabEditorHost findHost(@NotNull JComponent editorComponent) {
        Container current = editorComponent.getParent();
        while (current != null) {
            if (current instanceof SidetabEditorHost host) {
                return host;
            }
            current = current.getParent();
        }
        return null;
    }

    private static @Nullable Object constraintOf(@NotNull Container parent, @NotNull Component child) {
        LayoutManager layout = parent.getLayout();
        if (layout instanceof BorderLayout borderLayout) {
            return borderLayout.getConstraints(child);
        }
        return null;
    }

    private static int indexOf(@NotNull Container parent, @NotNull Component child) {
        Component[] children = parent.getComponents();
        for (int index = 0; index < children.length; index++) {
            if (children[index] == child) {
                return index;
            }
        }
        return -1;
    }

    private static void addBack(
            @NotNull Container parent,
            @NotNull SidetabEditorHost host,
            @Nullable Object constraint,
            int index
    ) {
        if (constraint != null) {
            parent.add(host, constraint);
            return;
        }
        if (index >= 0 && index <= parent.getComponentCount()) {
            parent.add(host, index);
            return;
        }
        parent.add(host);
    }
}
