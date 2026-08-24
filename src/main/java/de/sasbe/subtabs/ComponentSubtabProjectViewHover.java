package de.sasbe.subtabs;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.ui.hover.TreeHoverListener;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.IllegalComponentStateException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

final class ComponentSubtabProjectViewHover {
    private static final String ACTIVE_HOVER_KEY = "componentSubtabs.projectViewHover";

    private record Handle(@NotNull JTree tree, int row) {
    }

    private ComponentSubtabProjectViewHover() {
    }

    static void onEnter(@NotNull Project project, @NotNull VirtualFile file, @NotNull JComponent source) {
        onExit(source);

        JTree tree = projectViewTree(project);
        if (tree == null || !tree.isShowing()) {
            return;
        }

        TreePath path = findVisiblePath(tree, file);
        if (path == null) {
            return;
        }

        int row = tree.getRowForPath(path);
        if (row < 0) {
            return;
        }

        boolean scroll = SubtabsSettings.getInstance().isScrollProjectViewOnSubtabHover();
        if (scroll) {
            tree.scrollPathToVisible(path);
            row = tree.getRowForPath(path);
            if (row < 0) {
                return;
            }
        } else if (!isRowInViewport(tree, row)) {
            return;
        }

        setHoveredRow(tree, row);
        source.putClientProperty(ACTIVE_HOVER_KEY, new Handle(tree, row));
    }

    static void onExit(@NotNull JComponent source) {
        Handle handle = (Handle) source.getClientProperty(ACTIVE_HOVER_KEY);
        if (handle == null) {
            return;
        }

        if (isPointerOverRow(handle.tree(), handle.row())) {
            source.putClientProperty(ACTIVE_HOVER_KEY, null);
            return;
        }

        if (TreeHoverListener.getHoveredRow(handle.tree()) == handle.row()) {
            setHoveredRow(handle.tree(), -1);
        }
        source.putClientProperty(ACTIVE_HOVER_KEY, null);
    }

    private static void setHoveredRow(@NotNull JTree tree, int row) {
        if (TreeHoverListener.DEFAULT instanceof TreeHoverListener listener) {
            listener.onHover(tree, row);
        }
    }

    private static @Nullable JTree projectViewTree(@NotNull Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);
        if (toolWindow == null || !toolWindow.isVisible()) {
            return null;
        }

        AbstractProjectViewPane pane = ProjectView.getInstance(project).getCurrentProjectViewPane();
        if (pane != null && pane.getTree() != null) {
            return pane.getTree();
        }

        return UIUtil.findComponentOfType(toolWindow.getComponent(), JTree.class);
    }

    private static @Nullable TreePath findVisiblePath(@NotNull JTree tree, @NotNull VirtualFile file) {
        TreePath groupPath = null;
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath path = tree.getPathForRow(row);
            if (path == null) {
                continue;
            }
            Object userObject = TreeUtil.getUserObject(path.getLastPathComponent());
            if (isDirectFileNode(userObject) && file.equals(virtualFileOf(path))) {
                return path;
            }
            if (groupPath == null && isSubtabGroupContaining(path, file)) {
                groupPath = path;
            }
        }
        return groupPath;
    }

    static boolean isDirectFileNode(@NotNull Object userObject) {
        return !(userObject instanceof SubtabGroupProjectViewNode);
    }

    private static boolean isSubtabGroupContaining(@NotNull TreePath path, @NotNull VirtualFile file) {
        Object userObject = TreeUtil.getUserObject(path.getLastPathComponent());
        return userObject instanceof SubtabGroupProjectViewNode groupNode && groupNode.contains(file);
    }

    static @Nullable VirtualFile virtualFileOf(@NotNull TreePath path) {
        Object userObject = TreeUtil.getUserObject(path.getLastPathComponent());
        if (userObject instanceof SubtabGroupProjectViewNode) {
            return null;
        }
        if (userObject instanceof ProjectViewNode<?> node) {
            return node.getVirtualFile();
        }
        if (userObject instanceof AbstractTreeNode<?> node) {
            Object value = node.getValue();
            if (value instanceof VirtualFile virtualFile) {
                return virtualFile;
            }
            if (value instanceof PsiFileSystemItem item) {
                return item.getVirtualFile();
            }
        }
        if (userObject instanceof VirtualFile virtualFile) {
            return virtualFile;
        }
        return null;
    }

    private static boolean isRowInViewport(@NotNull JTree tree, int row) {
        Rectangle bounds = tree.getRowBounds(row);
        return bounds != null && tree.getVisibleRect().intersects(bounds);
    }

    private static boolean isPointerOverRow(@NotNull JTree tree, int row) {
        if (!tree.isShowing() || row < 0) {
            return false;
        }
        Rectangle bounds = tree.getRowBounds(row);
        if (bounds == null) {
            return false;
        }
        try {
            Point origin = tree.getLocationOnScreen();
            Point pointer = MouseInfo.getPointerInfo().getLocation();
            return new Rectangle(origin.x + bounds.x, origin.y + bounds.y, bounds.width, bounds.height)
                    .contains(pointer);
        } catch (IllegalComponentStateException | NullPointerException ignored) {
            return false;
        }
    }
}
