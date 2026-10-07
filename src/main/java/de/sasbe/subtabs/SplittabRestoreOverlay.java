package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import java.awt.Dimension;
import java.awt.IllegalComponentStateException;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class SplittabRestoreOverlay {
    private static final Key<Handle> OVERLAY_KEY = Key.create("componentSubtabs.splittabRestoreOverlay");
    private static final Key<Boolean> SUBTAB_DRAG_DROP_KEY =
            Key.create("componentSubtabs.splittabRestoreSubtabDragDrop");

    private SplittabRestoreOverlay() {
    }

    static void syncEditor(@NotNull Project project, @NotNull FileEditor editor) {
        if (!shouldShowForEditor(project, editor)) {
            hide(editor);
            return;
        }
        show(project, editor);
    }

    static void syncProject(@NotNull Project project) {
        if (project.isDisposed()) {
            return;
        }
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : manager.getAllEditors()) {
            syncEditor(project, editor);
        }
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

    static boolean isVisibleOnEditor(@NotNull Project project, @NotNull FileEditor editor) {
        return shouldShowForEditor(project, editor) && editor.getUserData(OVERLAY_KEY) != null;
    }

    static void showForSubtabDragDrop(@NotNull Project project, @NotNull FileEditor editor) {
        if (!supportsSplittabIcon(project, editor)) {
            return;
        }
        editor.putUserData(SUBTAB_DRAG_DROP_KEY, Boolean.TRUE);
        show(project, editor);
    }

    static void clearSubtabDragDropPresentation(@NotNull Project project, @NotNull FileEditor editor) {
        editor.putUserData(SUBTAB_DRAG_DROP_KEY, null);
        syncEditor(project, editor);
    }

    static void setDropTargetHighlighted(@NotNull FileEditor editor, boolean highlighted) {
        Handle handle = editor.getUserData(OVERLAY_KEY);
        if (handle != null) {
            handle.setDropTargetHighlighted(highlighted);
        }
    }

    static boolean isPointerOverDropTarget(@NotNull FileEditor editor, @NotNull java.awt.Point screenPoint) {
        Handle handle = editor.getUserData(OVERLAY_KEY);
        return handle != null && handle.containsScreenPoint(screenPoint);
    }

    @TestOnly
    static @Nullable java.awt.Point dropTargetScreenPointForTest(@NotNull FileEditor editor) {
        Handle handle = editor.getUserData(OVERLAY_KEY);
        if (handle == null || !handle.button.isShowing()) {
            return null;
        }
        try {
            java.awt.Point origin = handle.button.getLocationOnScreen();
            return new java.awt.Point(
                    origin.x + handle.button.getWidth() / 2,
                    origin.y + handle.button.getHeight() / 2
            );
        } catch (IllegalComponentStateException ignored) {
            return null;
        }
    }

    private static boolean shouldShowForEditor(@NotNull Project project, @NotNull FileEditor editor) {
        if (!SubtabsSettings.getInstance().isSplittabsEnabled()) {
            return false;
        }
        if (!supportsSplittabIcon(project, editor)) {
            return false;
        }
        if (Boolean.TRUE.equals(editor.getUserData(SUBTAB_DRAG_DROP_KEY))) {
            return true;
        }
        return ComponentSubtabEditorSplitRegistry.getInstance(project).hasSavedSplittabs();
    }

    private static boolean supportsSplittabIcon(@NotNull Project project, @NotNull FileEditor editor) {
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            return false;
        }
        if (!SubtabsSettings.getInstance().isShowCollapseButton()) {
            return false;
        }
        if (SidetabIconLayout.splittabRestoreSlotFromRight() <= 0) {
            return false;
        }
        return SubtabsCollapseOverlay.isInstalled(editor) || SubtabsExpandOverlay.isInstalled(editor);
    }

    private static void show(@NotNull Project project, @NotNull FileEditor editor) {
        Handle existing = editor.getUserData(OVERLAY_KEY);
        if (existing != null) {
            refreshButtonPresentation(project, existing.button);
            existing.layoutButton();
            return;
        }
        hide(editor);
        JComponent editorComponent = editor.getComponent();
        ComponentSubtabIconButton button = createButton(project);
        Handle handle = new Handle(editor, editorComponent, button);
        editor.putUserData(OVERLAY_KEY, handle);
        handle.install();
    }

    private static @NotNull ComponentSubtabIconButton createButton(@NotNull Project project) {
        ComponentSubtabIconButton button = new ComponentSubtabIconButton(
                iconForProject(project)
        );
        button.setToolTipText(tooltipForProject(project));
        button.getAccessibleContext().setAccessibleName(tooltipForProject(project));
        button.addActionListener(event -> SplittabDedicatedViewService.getInstance(project).toggleDedicatedView());
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                SplittabRestoreSelectPopup.onButtonEnter(project, button);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                SplittabRestoreSelectPopup.onButtonExit(button);
            }
        });
        return button;
    }

    private static @NotNull javax.swing.Icon iconForProject(@NotNull Project project) {
        return SplittabViewIcons.forProject(project);
    }

    private static @NotNull String tooltipForProject(@NotNull Project project) {
        if (SplittabDedicatedViewService.usesDedicatedBehavior(project)) {
            return SplittabDedicatedViewService.getInstance(project).isDedicatedViewActive()
                    ? "Zur Editor-Ansicht wechseln (Mixed beenden)"
                    : "Zu Mixed wechseln";
        }
        return "Gespeicherte Split Pairs";
    }

    static void refreshButtonPresentation(@NotNull Project project, @NotNull ComponentSubtabIconButton button) {
        button.setIcon(iconForProject(project));
        button.setToolTipText(tooltipForProject(project));
        button.getAccessibleContext().setAccessibleName(tooltipForProject(project));
    }

    private static final class Handle {
        private final FileEditor fileEditor;
        private final JComponent editorComponent;
        private final ComponentSubtabIconButton button;
        private final ComponentListener componentListener;
        private final HierarchyListener hierarchyListener;
        private @Nullable JLayeredPane layeredPane;

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
            Rectangle bounds = SidetabIconLayout.layoutSplittabRestoreIcon(
                    fileEditor,
                    editorComponent,
                    layeredPane,
                    size
            );
            button.setBounds(bounds);
        }

        private void setDropTargetHighlighted(boolean highlighted) {
            button.setDropTargetHighlight(highlighted);
        }

        private boolean containsScreenPoint(@NotNull java.awt.Point screenPoint) {
            if (!button.isShowing()) {
                return false;
            }
            try {
                java.awt.Point origin = button.getLocationOnScreen();
                Rectangle bounds = new Rectangle(origin, button.getSize());
                return bounds.contains(screenPoint);
            } catch (IllegalComponentStateException ignored) {
                return false;
            }
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
