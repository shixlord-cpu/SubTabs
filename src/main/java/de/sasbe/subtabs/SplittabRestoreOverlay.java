package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
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
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        if (manager.getWindows().length != 2) {
            ComponentSubtabEditorSplitNavigation.clearTwoPaneArrangementHint(project);
        }
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
            return ComponentSubtabEditorSplitNavigation.isSplitPairIconHostEditor(project, editor);
        }
        boolean showAffordance = ComponentSubtabEditorSplitRegistry.getInstance(project).hasSavedSplittabs()
                || ComponentSubtabEditorSplitNavigation.hasNativeTwoPaneSplitCandidate(project)
                || ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(project);
        if (!showAffordance) {
            return false;
        }
        return ComponentSubtabEditorSplitNavigation.isSplitPairIconHostEditor(project, editor);
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
        if (SubtabsCollapseOverlay.isInstalled(editor) || SubtabsExpandOverlay.isInstalled(editor)) {
            return true;
        }
        return editor.getUserData(ComponentSubtabsSplittabUi.SPLITTAB_SWITCH_BAR_KEY) != null
                || editor.getUserData(ComponentSubtabsSplittabUi.SPLITTAB_HEADER_KEY) != null;
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
        ComponentSubtabIconButton button = createButton(project, editor);
        Handle handle = new Handle(editor, editorComponent, button);
        editor.putUserData(OVERLAY_KEY, handle);
        handle.install();
    }

    private static @NotNull ComponentSubtabIconButton createButton(
            @NotNull Project project,
            @NotNull FileEditor editor
    ) {
        ComponentSubtabIconButton button = new ComponentSubtabIconButton(
                iconForProject(project)
        );
        refreshButtonPresentation(project, button);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                SplittabRestoreSelectPopup.onButtonEnter(project, button);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                SplittabRestoreSelectPopup.onButtonExit(button);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (!button.isEnabled() || !button.contains(event.getPoint())) {
                    return;
                }
                if (SwingUtilities.isLeftMouseButton(event)) {
                    onRestoreIconClick(project, editor, true);
                } else if (SwingUtilities.isRightMouseButton(event)) {
                    onRestoreIconClick(project, editor, false);
                }
            }
        });
        return button;
    }

    private static void onRestoreIconClick(
            @NotNull Project project,
            @NotNull FileEditor editor,
            boolean leftButton
    ) {
        VirtualFile paneFile = editor.getFile();
        if (paneFile == null) {
            return;
        }
        VirtualFile partnerFile = ComponentSubtabEditorSplitNavigation.nativeTwoPanePartner(project, paneFile);
        boolean canCreate = partnerFile != null
                && ComponentSubtabEditorSplitNavigation.canCreateSplitPairFromNativeTwoPane(
                project,
                paneFile,
                partnerFile
        );
        boolean savedPairs = ComponentSubtabEditorSplitRegistry.getInstance(project).hasSavedSplittabs();

        if (leftButton) {
            if (!savedPairs && canCreate) {
                ComponentSubtabEditorSplitNavigation.createSplit(project, paneFile, partnerFile);
                syncProject(project);
                return;
            }
            if (savedPairs) {
                SplittabDedicatedViewService.getInstance(project).toggleDedicatedView();
                syncProject(project);
            }
            return;
        }

        if (ComponentSubtabEditorSplitNavigation.canToggleTwoPaneSplitOrientation(project)) {
            if (ComponentSubtabEditorSplitNavigation.toggleTwoPaneSplitOrientation(project)) {
                syncProject(project);
            }
            return;
        }
        if (canCreate && savedPairs) {
            ComponentSubtabEditorSplitNavigation.createSplit(project, paneFile, partnerFile);
            syncProject(project);
        }
    }

    private static @NotNull javax.swing.Icon iconForProject(@NotNull Project project) {
        return SplittabViewIcons.forProject(project);
    }

    private static @NotNull String tooltipForProject(@NotNull Project project) {
        if (!ComponentSubtabEditorSplitRegistry.getInstance(project).hasSavedSplittabs()
                && ComponentSubtabEditorSplitNavigation.hasNativeTwoPaneSplitCandidate(project)) {
            return "Create split pair (click). Swap split direction (right-click)";
        }
        if (SplittabDedicatedViewService.usesDedicatedBehavior(project)) {
            return SplittabDedicatedViewService.getInstance(project).isDedicatedViewActive()
                    ? "Return to editor view (leave Switch)"
                    : "Switch to Mixed";
        }
        return "Saved split pairs";
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
