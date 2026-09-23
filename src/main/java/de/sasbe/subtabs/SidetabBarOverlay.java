package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.util.Key;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

final class SidetabBarOverlay {
    private static final Key<Handle> OVERLAY_KEY = Key.create("componentSubtabs.sidetabBarOverlay");

    private SidetabBarOverlay() {
    }

    static void show(
            @NotNull FileEditor editor,
            @NotNull SidetabBarPanel panel,
            boolean onRight
    ) {
        hide(editor);
        JComponent editorComponent = editor.getComponent();
        Handle handle = new Handle(editor, editorComponent, panel, onRight);
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

    static void relayout(@NotNull FileEditor editor) {
        Handle handle = editor.getUserData(OVERLAY_KEY);
        if (handle != null) {
            handle.layoutPanel();
        }
    }

    private static final class Handle {
        private final FileEditor fileEditor;
        private final JComponent editorComponent;
        private final SidetabBarPanel panel;
        private final boolean onRight;
        private final ComponentListener componentListener;
        private final HierarchyListener hierarchyListener;
        private JLayeredPane layeredPane;

        private Handle(
                @NotNull FileEditor fileEditor,
                @NotNull JComponent editorComponent,
                @NotNull SidetabBarPanel panel,
                boolean onRight
        ) {
            this.fileEditor = fileEditor;
            this.editorComponent = editorComponent;
            this.panel = panel;
            this.onRight = onRight;
            this.componentListener = new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent event) {
                    layoutPanel();
                }

                @Override
                public void componentMoved(ComponentEvent event) {
                    layoutPanel();
                }

                @Override
                public void componentShown(ComponentEvent event) {
                    layoutPanel();
                }
            };
            this.hierarchyListener = event -> {
                if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0
                        || (event.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0) {
                    attachToLayeredPane();
                    layoutPanel();
                }
            };
        }

        private void install() {
            editorComponent.addComponentListener(componentListener);
            editorComponent.addHierarchyListener(hierarchyListener);
            panel.addComponentListener(componentListener);
            attachToLayeredPane();
            layoutPanel();
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
                layeredPane.remove(panel);
                layeredPane.repaint();
            }
            layeredPane = nextPane;
            if (layeredPane != null) {
                layeredPane.add(panel, JLayeredPane.POPUP_LAYER);
                layeredPane.revalidate();
                layeredPane.repaint();
            }
        }

        private void layoutPanel() {
            if (layeredPane == null || !editorComponent.isShowing() || !layeredPane.isShowing()) {
                panel.setVisible(false);
                return;
            }

            panel.setVisible(true);
            Dimension barSize = panel.getPreferredSize();
            int barWidth = Math.max(JBUI.scale(SidetabBarPanel.overlayHitWidth()), barSize.width);
            Point origin = SwingUtilities.convertPoint(editorComponent, 0, 0, layeredPane);
            int edgeInset = JBUI.scale(4);
            int x = onRight
                    ? origin.x + editorComponent.getWidth() - barWidth - edgeInset
                    : origin.x + edgeInset;
            int y = SidetabIconLayout.sidetabContentTopY(fileEditor, editorComponent, layeredPane);
            panel.revalidate();
            int height = Math.max(origin.y + editorComponent.getHeight() - y, JBUI.scale(8));
            panel.setBounds(x, y, barWidth, height);
            panel.revalidate();
            SidetabsToggleOverlay.relayout(fileEditor);
            SubtabsCollapseOverlay.relayout(fileEditor);
            SubtabsExpandOverlay.relayout(fileEditor);
            RuleSwitchOverlay.relayout(fileEditor);
        }

        private void dispose() {
            editorComponent.removeComponentListener(componentListener);
            editorComponent.removeHierarchyListener(hierarchyListener);
            panel.removeComponentListener(componentListener);
            if (layeredPane != null) {
                layeredPane.remove(panel);
                layeredPane.repaint();
            }
        }
    }
}
