package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import java.awt.Dimension;
import java.awt.Rectangle;
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
        ComponentSubtabIconButton button = createExpandButton(project);
        Handle handle = new Handle(editor, editorComponent, button);
        editor.putUserData(OVERLAY_KEY, handle);
        handle.install();
    }

    static @Nullable JComponent visibleButton(@NotNull FileEditor editor) {
        Handle handle = editor.getUserData(OVERLAY_KEY);
        if (handle == null) {
            return null;
        }
        return handle.visibleButton();
    }

    @TestOnly
    static boolean isInstalled(@NotNull FileEditor editor) {
        return editor.getUserData(OVERLAY_KEY) != null;
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
            handle.layoutButton();
        }
    }

    private static @NotNull ComponentSubtabIconButton createExpandButton(@NotNull Project project) {
        ComponentSubtabIconButton button = new ComponentSubtabIconButton(SubtabsIcons.INACTIVE);
        button.setToolTipText("SubTabs ausklappen");
        button.getAccessibleContext().setAccessibleName("SubTabs ausklappen");
        button.addActionListener(event -> SubtabsCollapseState.getInstance(project).toggle(project));
        return button;
    }

    private static final class Handle {
        private final FileEditor fileEditor;
        private final JComponent editorComponent;
        private final ComponentSubtabIconButton button;
        private final ComponentListener componentListener;
        private final HierarchyListener hierarchyListener;
        private JLayeredPane layeredPane;

        private Handle(
                @NotNull FileEditor fileEditor,
                @NotNull JComponent editorComponent,
                @NotNull ComponentSubtabIconButton button
        ) {
            this.fileEditor = fileEditor;
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

        private @Nullable JComponent visibleButton() {
            return button.isShowing() ? button : null;
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
            Rectangle bounds = SidetabIconLayout.layoutSubtabsIcon(
                    fileEditor,
                    editorComponent,
                    layeredPane,
                    size
            );
            button.setBounds(bounds);
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
