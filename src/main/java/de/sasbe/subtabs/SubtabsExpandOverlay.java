package de.sasbe.subtabs;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

final class SubtabsExpandOverlay {
    private static final Key<Handle> OVERLAY_KEY = Key.create("componentSubtabs.expandOverlay");

    private SubtabsExpandOverlay() {
    }

    static void show(@NotNull Project project, @NotNull FileEditor editor) {
        hide(editor);

        JComponent editorComponent = editor.getComponent();
        JButton button = createExpandButton(project);
        Handle handle = new Handle(editorComponent, button);
        editor.putUserData(OVERLAY_KEY, handle);
        handle.install();
    }

    static void hide(@NotNull FileEditor editor) {
        Handle handle = editor.getUserData(OVERLAY_KEY);
        if (handle == null) {
            return;
        }
        handle.dispose();
        editor.putUserData(OVERLAY_KEY, null);
    }

    private static @NotNull JButton createExpandButton(@NotNull Project project) {
        JButton button = new JButton(AllIcons.General.ChevronDown);
        button.setFocusable(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setToolTipText("Subtabs ausklappen");
        button.getAccessibleContext().setAccessibleName("Subtabs ausklappen");
        button.addActionListener(event -> SubtabsCollapseState.getInstance(project).toggle(project));
        Dimension size = button.getPreferredSize();
        button.setSize(size);
        return button;
    }

    private static final class Handle {
        private final JComponent editorComponent;
        private final JButton button;
        private final ComponentListener componentListener;
        private final HierarchyListener hierarchyListener;
        private JLayeredPane layeredPane;

        private Handle(@NotNull JComponent editorComponent, @NotNull JButton button) {
            this.editorComponent = editorComponent;
            this.button = button;
            this.componentListener = new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent event) {
                    layoutButton();
                }

                @Override
                public void componentMoved(ComponentEvent event) {
                    layoutButton();
                }

                @Override
                public void componentShown(ComponentEvent event) {
                    layoutButton();
                }
            };
            this.hierarchyListener = event -> {
                if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0
                        || (event.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0) {
                    attachToLayeredPane();
                    layoutButton();
                }
            };
        }

        private void install() {
            editorComponent.addComponentListener(componentListener);
            editorComponent.addHierarchyListener(hierarchyListener);
            attachToLayeredPane();
            layoutButton();
        }

        private void attachToLayeredPane() {
            JLayeredPane nextPane = null;
            if (editorComponent.getRootPane() != null) {
                nextPane = editorComponent.getRootPane().getLayeredPane();
            }
            if (nextPane == layeredPane) {
                return;
            }
            if (layeredPane != null) {
                layeredPane.remove(button);
                layeredPane.repaint();
            }
            layeredPane = nextPane;
            if (layeredPane != null) {
                layeredPane.add(button, JLayeredPane.POPUP_LAYER);
                layeredPane.revalidate();
                layeredPane.repaint();
            }
        }

        private void layoutButton() {
            if (layeredPane == null || !editorComponent.isShowing() || !layeredPane.isShowing()) {
                button.setVisible(false);
                return;
            }

            button.setVisible(true);
            Dimension size = button.getPreferredSize();
            Point topRight = SwingUtilities.convertPoint(
                    editorComponent,
                    editorComponent.getWidth() - size.width,
                    0,
                    layeredPane
            );
            button.setBounds(topRight.x, topRight.y, size.width, size.height);
        }

        private void dispose() {
            editorComponent.removeComponentListener(componentListener);
            editorComponent.removeHierarchyListener(hierarchyListener);
            if (layeredPane != null) {
                layeredPane.remove(button);
                layeredPane.repaint();
            }
        }
    }
}
