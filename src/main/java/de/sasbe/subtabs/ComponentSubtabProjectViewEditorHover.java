package de.sasbe.subtabs;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.hover.TreeHoverListener;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

final class ComponentSubtabProjectViewEditorHover {
    private static final String INSTALLED = "componentSubtabs.projectViewEditorHover";
    private static final String LAST_ROW_KEY = "componentSubtabs.projectViewEditorHoverRow";

    private ComponentSubtabProjectViewEditorHover() {
    }

    static void installOn(@NotNull Project project) {
        if (project.isDisposed() || !SubtabsSettings.getInstance().isSubtabsActive()) {
            return;
        }

        AbstractProjectViewPane pane = ProjectView.getInstance(project).getCurrentProjectViewPane();
        if (pane != null && pane.getTree() != null) {
            attach(project, pane.getTree());
        }

        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);
        if (toolWindow != null) {
            for (JTree tree : UIUtil.findComponentsOfType(toolWindow.getComponent(), JTree.class)) {
                attach(project, tree);
            }
        }
    }

    private static void attach(@NotNull Project project, @NotNull JTree tree) {
        if (Boolean.TRUE.equals(tree.getClientProperty(INSTALLED))) {
            return;
        }
        tree.putClientProperty(INSTALLED, Boolean.TRUE);
        SubtabGroupLocationHover.installRenderer(tree);

        tree.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                if (SubtabGroupLocationHover.handleMouseMoved(project, tree, event)) {
                    return;
                }

                int row = tree.getRowForLocation(event.getX(), event.getY());
                if (row < 0) {
                    row = TreeHoverListener.getHoveredRow(tree);
                }
                handleRow(project, tree, row);
            }
        });

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() != 2 || !SwingUtilities.isLeftMouseButton(event)) {
                    return;
                }
                if (tryOpenRelatedFileAsSubtabSwitch(project, tree, event)) {
                    event.consume();
                }
            }

            @Override
            public void mousePressed(MouseEvent event) {
                if (event.getClickCount() != 2 || !SwingUtilities.isLeftMouseButton(event)) {
                    return;
                }
                if (tryOpenRelatedFileAsSubtabSwitch(project, tree, event)) {
                    event.consume();
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                SubtabGroupLocationHover.handleTreeMouseExited(tree);
                clearHover(tree);
            }
        });

        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent event) {
                if (!tree.isShowing()) {
                    clearHover(tree);
                }
            }
        });
    }

    private static void handleRow(@NotNull Project project, @NotNull JTree tree, int row) {
        Integer lastRow = (Integer) tree.getClientProperty(LAST_ROW_KEY);
        if (lastRow != null && lastRow == row) {
            return;
        }
        tree.putClientProperty(LAST_ROW_KEY, row);

        if (row < 0) {
            clearHover(tree);
            return;
        }

        TreePath path = tree.getPathForRow(row);
        if (path == null) {
            clearHover(tree);
            return;
        }

        Object userObject = TreeUtil.getLastUserObject(path);
        if (userObject instanceof SubtabGroupProjectViewNode groupNode) {
            ComponentSubtabBarHover.onExit(tree);
            ComponentSubtabMainTabHover.onEnterGroup(project, groupNode.groupKey(), tree);
            return;
        }

        VirtualFile file = ComponentSubtabProjectViewHover.virtualFileOf(path);
        if (file == null) {
            clearHover(tree);
            return;
        }

        ComponentSubtabBarHover.onEnter(project, file, tree);
        if (FileEditorManager.getInstance(project).isFileOpen(file)) {
            ComponentSubtabMainTabHover.onEnter(project, file, tree);
        } else {
            ComponentSubtabMainTabHover.onExit(tree);
        }
    }

    private static boolean tryOpenRelatedFileAsSubtabSwitch(
            @NotNull Project project,
            @NotNull JTree tree,
            @NotNull MouseEvent event
    ) {
        int row = tree.getRowForLocation(event.getX(), event.getY());
        if (row < 0) {
            return false;
        }

        TreePath path = tree.getPathForRow(row);
        VirtualFile file = path == null ? null : ComponentSubtabProjectViewHover.virtualFileOf(path);
        if (file == null) {
            return false;
        }

        return ComponentSubtabProjectViewNavigation.tryNavigateAsSubtabSwitch(project, file, true);
    }

    private static void clearHover(@NotNull JTree tree) {
        tree.putClientProperty(LAST_ROW_KEY, null);
        ComponentSubtabBarHover.onExit(tree);
        ComponentSubtabMainTabHover.onExit(tree);
    }
}
