package de.sasbe.subtabs;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.hover.TreeHoverListener;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.awt.Component;
import java.awt.IllegalComponentStateException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

final class ComponentSubtabProjectViewHover {
    private static final String ACTIVE_HOVER_KEY = "componentSubtabs.projectViewHover";
    private static final String EXTERNAL_HOVER_ROWS_KEY = "componentSubtabs.projectViewExternalHoverRows";
    private static final String EXTERNAL_HOVER_HANDLE_KEY = "componentSubtabs.projectViewExternalHoverHandle";
    private static final String HOVER_OWNER_KEY = "componentSubtabs.projectViewHoverOwner";

    private record Handle(@NotNull JTree tree, int row) {
    }

    private record ExternalHandle(@NotNull JTree tree, @NotNull Set<Integer> rows) {
    }

    private ComponentSubtabProjectViewHover() {
    }

    static void onEnter(@NotNull Project project, @NotNull VirtualFile file, @NotNull JComponent source) {
        if (SubtabHoverView.isDisabled()) {
            return;
        }
        onExit(source);

        JTree tree = projectViewTree(project);
        if (tree == null || !tree.isShowing()) {
            return;
        }

        releaseTreeHoverForOtherOwner(tree, source);

        TreePath path = findHoverTargetPath(tree, file);
        if (path == null) {
            return;
        }

        int row = rowForPath(tree, path);
        if (row < 0 || !isRowInViewport(tree, row)) {
            return;
        }

        applySingleRowHover(source, tree, row);
    }

    /**
     * Highlights project-tree rows for a hovered main tab.
     *
     * <p>Directory view: every related file (or its deepest visible closed ancestor folder).
     * Grouped view: the subtab group row plus the file that belongs to this main tab.
     */
    static void onEnterRelatedGroup(
            @NotNull Project project,
            @NotNull VirtualFile tabFile,
            @NotNull JComponent source
    ) {
        if (SubtabHoverView.isDisabled()) {
            return;
        }
        onExit(source);

        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(tabFile);
        if (match == null) {
            onEnter(project, tabFile, source);
            return;
        }

        JTree tree = projectViewTree(project);
        if (tree == null || !tree.isShowing()) {
            return;
        }

        releaseTreeHoverForOtherOwner(tree, source);

        List<VirtualFile> relatedFiles = new ArrayList<>(match.relatedFiles().size());
        for (ComponentRelatedFiles.Entry entry : match.relatedFiles()) {
            relatedFiles.add(entry.file());
        }

        Set<Integer> targetRows = new LinkedHashSet<>();
        for (TreePath path : findMainTabHoverPaths(
                tree,
                tabFile,
                relatedFiles,
                SubtabProjectViewGrouping.isEnabled()
        )) {
            int row = rowForPath(tree, path);
            if (row >= 0 && isRowInViewport(tree, row)) {
                targetRows.add(row);
            }
        }

        if (targetRows.isEmpty()) {
            onEnter(project, tabFile, source);
            return;
        }
        applyExternalRows(source, tree, targetRows);
    }

    static void onExit(@NotNull JComponent source) {
        Handle handle = (Handle) source.getClientProperty(ACTIVE_HOVER_KEY);
        ExternalHandle externalHandle = (ExternalHandle) source.getClientProperty(EXTERNAL_HOVER_HANDLE_KEY);
        source.putClientProperty(ACTIVE_HOVER_KEY, null);
        source.putClientProperty(EXTERNAL_HOVER_HANDLE_KEY, null);

        if (handle != null && isHoverOwner(handle.tree(), source)) {
            if (isPointerOverRow(handle.tree(), handle.row())) {
                source.putClientProperty(ACTIVE_HOVER_KEY, handle);
                return;
            }
            if (TreeHoverListener.getHoveredRow(handle.tree()) == handle.row()) {
                setHoveredRow(handle.tree(), -1);
            }
            clearHoverOwner(handle.tree(), source);
        }

        if (externalHandle != null && isHoverOwner(externalHandle.tree(), source)) {
            clearExternalRows(externalHandle.tree());
            clearHoverOwner(externalHandle.tree(), source);
        }
    }

    static boolean isExternalHoverRow(@NotNull JTree tree, int row) {
        Object value = tree.getClientProperty(EXTERNAL_HOVER_ROWS_KEY);
        return value instanceof Set<?> rows && rows.contains(row);
    }

    private static void applySingleRowHover(
            @NotNull JComponent source,
            @NotNull JTree tree,
            int row
    ) {
        clearExternalRows(tree);
        setHoveredRow(tree, row);
        tree.putClientProperty(HOVER_OWNER_KEY, source);
        source.putClientProperty(ACTIVE_HOVER_KEY, new Handle(tree, row));
    }

    private static void applyExternalRows(
            @NotNull JComponent source,
            @NotNull JTree tree,
            @NotNull Set<Integer> rows
    ) {
        Set<Integer> copied = Set.copyOf(rows);
        if (TreeHoverListener.getHoveredRow(tree) >= 0) {
            setHoveredRow(tree, -1);
        }
        clearExternalRows(tree);
        tree.putClientProperty(EXTERNAL_HOVER_ROWS_KEY, copied);
        tree.putClientProperty(HOVER_OWNER_KEY, source);
        source.putClientProperty(EXTERNAL_HOVER_HANDLE_KEY, new ExternalHandle(tree, copied));
        repaintHover(tree);
    }

    static @NotNull Set<Integer> paintedHoverRows(@NotNull JTree tree) {
        Object value = tree.getClientProperty(EXTERNAL_HOVER_ROWS_KEY);
        if (!(value instanceof Set<?> rows) || rows.isEmpty()) {
            return Set.of();
        }
        Set<Integer> copied = new LinkedHashSet<>();
        for (Object row : rows) {
            if (row instanceof Integer integer) {
                copied.add(integer);
            }
        }
        return copied;
    }

    private static void repaintHover(@NotNull JTree tree) {
        tree.repaint();
        Component parent = tree.getParent();
        if (parent != null) {
            parent.repaint();
        }
    }

    private static boolean isHoverOwner(@NotNull JTree tree, @NotNull JComponent source) {
        return source.equals(tree.getClientProperty(HOVER_OWNER_KEY));
    }

    private static void clearHoverOwner(@NotNull JTree tree, @NotNull JComponent source) {
        if (isHoverOwner(tree, source)) {
            tree.putClientProperty(HOVER_OWNER_KEY, null);
        }
    }

    private static void clearExternalRows(@NotNull JTree tree) {
        tree.putClientProperty(EXTERNAL_HOVER_ROWS_KEY, null);
        repaintHover(tree);
    }

    private static void releaseTreeHoverForOtherOwner(@NotNull JTree tree, @NotNull JComponent source) {
        Object owner = tree.getClientProperty(HOVER_OWNER_KEY);
        if (owner instanceof JComponent current && !current.equals(source)) {
            onExit(current);
        }
    }


    private static int rowForPath(@NotNull JTree tree, @NotNull TreePath path) {
        return tree.getRowForPath(path);
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

    /**
     * Resolves the visible project-tree row to highlight for a single file (subtab hover):
     * the file itself when expanded, otherwise its subtab group row, otherwise the deepest
     * visible closed ancestor directory.
     */
    static @Nullable TreePath findHoverTargetPath(@NotNull JTree tree, @NotNull VirtualFile file) {
        TreePath filePath = findVisibleFilePath(tree, file);
        if (filePath != null) {
            return filePath;
        }
        TreePath groupPath = findVisibleGroupPath(tree, file);
        if (groupPath != null) {
            return groupPath;
        }
        return findClosedAncestorDirectoryPath(tree, file);
    }

    /**
     * Rows to mark when hovering a main tab. Directory view marks every related file;
     * grouped view marks the group node plus this tab's own file.
     */
    static @NotNull List<TreePath> findMainTabHoverPaths(
            @NotNull JTree tree,
            @NotNull VirtualFile tabFile,
            @NotNull List<VirtualFile> relatedFiles,
            boolean groupingEnabled
    ) {
        if (groupingEnabled) {
            List<TreePath> paths = new ArrayList<>();
            TreePath groupPath = findVisibleGroupPath(tree, tabFile);
            TreePath filePath = findVisibleFilePath(tree, tabFile);
            if (groupPath != null) {
                paths.add(groupPath);
            }
            if (filePath != null) {
                paths.add(filePath);
            }
            if (paths.isEmpty()) {
                TreePath ancestor = findClosedAncestorDirectoryPath(tree, tabFile);
                if (ancestor != null) {
                    paths.add(ancestor);
                }
            }
            return paths;
        }

        Set<TreePath> paths = new LinkedHashSet<>();
        for (VirtualFile related : relatedFiles) {
            TreePath filePath = findVisibleFilePath(tree, related);
            if (filePath != null) {
                paths.add(filePath);
                continue;
            }
            TreePath ancestor = findClosedAncestorDirectoryPath(tree, related);
            if (ancestor != null) {
                paths.add(ancestor);
            }
        }
        return new ArrayList<>(paths);
    }

    private static @Nullable TreePath findVisibleFilePath(@NotNull JTree tree, @NotNull VirtualFile file) {
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath path = tree.getPathForRow(row);
            if (path == null) {
                continue;
            }
            if (isDirectFileNode(path) && file.equals(virtualFileOf(path))) {
                return path;
            }
        }
        return null;
    }

    private static @Nullable TreePath findVisibleGroupPath(@NotNull JTree tree, @NotNull VirtualFile file) {
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath path = tree.getPathForRow(row);
            if (path != null && isSubtabGroupContaining(path, file)) {
                return path;
            }
        }
        return null;
    }

    private static @Nullable TreePath findClosedAncestorDirectoryPath(
            @NotNull JTree tree,
            @NotNull VirtualFile file
    ) {
        for (VirtualFile directory : directoryAncestors(file)) {
            TreePath path = findVisibleDirectoryPath(tree, directory);
            if (path != null) {
                return path;
            }
        }
        return null;
    }

    private static @Nullable TreePath findVisibleDirectoryPath(
            @NotNull JTree tree,
            @NotNull VirtualFile directory
    ) {
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath path = tree.getPathForRow(row);
            if (path == null) {
                continue;
            }
            VirtualFile file = virtualFileOf(path);
            if (directory.equals(file) && file.isDirectory()) {
                return path;
            }
        }
        return null;
    }

    private static @NotNull List<VirtualFile> directoryAncestors(@NotNull VirtualFile file) {
        List<VirtualFile> ancestors = new ArrayList<>();
        VirtualFile current = file.getParent();
        while (current != null) {
            ancestors.add(current);
            current = current.getParent();
        }
        return ancestors;
    }

    private static boolean isDirectFileNode(@NotNull TreePath path) {
        Object userObject = TreeUtil.getLastUserObject(path);
        return userObject != null && isDirectFileNode(userObject);
    }

    static boolean isDirectFileNode(@NotNull Object userObject) {
        return !(userObject instanceof GroupNode);
    }

    private static boolean isSubtabGroupContaining(@NotNull TreePath path, @NotNull VirtualFile file) {
        Object userObject = TreeUtil.getLastUserObject(path);
        return userObject instanceof GroupNode groupNode && groupNode.contains(file);
    }

    interface GroupNode {
        boolean contains(@NotNull VirtualFile file);
    }

    static @Nullable VirtualFile virtualFileOf(@NotNull TreePath path) {
        Object userObject = TreeUtil.getLastUserObject(path);
        if (userObject instanceof GroupNode) {
            return null;
        }
        if (userObject instanceof com.intellij.ide.projectView.ProjectViewNode<?> node) {
            return node.getVirtualFile();
        }
        if (userObject instanceof com.intellij.ide.util.treeView.AbstractTreeNode<?> node) {
            Object value = node.getValue();
            if (value instanceof VirtualFile virtualFile) {
                return virtualFile;
            }
            if (value instanceof com.intellij.psi.PsiFileSystemItem item) {
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
        if (bounds == null) {
            return false;
        }
        Rectangle visible = tree.getVisibleRect();
        if (visible.width <= 0 || visible.height <= 0) {
            return true;
        }
        return visible.intersects(bounds);
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

    @TestOnly
    static @NotNull Set<Integer> collectMainTabHoverRows(
            @NotNull JTree tree,
            @NotNull ComponentRelatedFiles.Match match,
            @NotNull VirtualFile tabFile
    ) {
        List<VirtualFile> relatedFiles = new ArrayList<>(match.relatedFiles().size());
        for (ComponentRelatedFiles.Entry entry : match.relatedFiles()) {
            relatedFiles.add(entry.file());
        }
        Set<Integer> rows = new LinkedHashSet<>();
        for (TreePath path : findMainTabHoverPaths(
                tree,
                tabFile,
                relatedFiles,
                SubtabProjectViewGrouping.isEnabled()
        )) {
            int row = rowForPath(tree, path);
            if (row >= 0) {
                rows.add(row);
            }
        }
        return rows;
    }

    @TestOnly
    static void activateMainTabHoverForTest(
            @NotNull JComponent source,
            @NotNull JTree tree,
            @NotNull Set<Integer> rows
    ) {
        onExit(source);
        releaseTreeHoverForOtherOwner(tree, source);
        if (rows.isEmpty()) {
            return;
        }
        applyExternalRows(source, tree, rows);
    }

    @TestOnly
    static void activateSubtabHoverForTest(
            @NotNull JComponent source,
            @NotNull JTree tree,
            @NotNull VirtualFile file
    ) {
        onExit(source);
        releaseTreeHoverForOtherOwner(tree, source);
        TreePath path = findHoverTargetPath(tree, file);
        if (path == null) {
            return;
        }
        int row = rowForPath(tree, path);
        if (row >= 0) {
            applySingleRowHover(source, tree, row);
        }
    }

    @TestOnly
    static @Nullable Set<Integer> externalHoverRows(@NotNull JTree tree) {
        Object value = tree.getClientProperty(EXTERNAL_HOVER_ROWS_KEY);
        if (value instanceof Set<?> rows) {
            Set<Integer> copied = new LinkedHashSet<>();
            for (Object row : rows) {
                if (row instanceof Integer integer) {
                    copied.add(integer);
                }
            }
            return copied;
        }
        return null;
    }

    @TestOnly
    static @Nullable JComponent hoverOwner(@NotNull JTree tree) {
        Object value = tree.getClientProperty(HOVER_OWNER_KEY);
        return value instanceof JComponent component ? component : null;
    }
}
