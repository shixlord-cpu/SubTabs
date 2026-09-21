package de.sasbe.subtabs;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JTree;
import javax.swing.tree.TreePath;

/**
 * Scrolls the project view to a subtab file or, when grouping is set to {@link
 * SubtabGroupTreeControlStyle#NONE}, to the subtab group row that contains the file.
 */
final class SubtabProjectViewReveal {
    private SubtabProjectViewReveal() {
    }

    static void revealSubtab(@NotNull Project project, @NotNull VirtualFile file) {
        if (shouldRevealGroup(project)) {
            if (revealGroupContaining(project, file)) {
                return;
            }
        }
        ProjectView.getInstance(project).select(null, file, true);
    }

    static void revealMainTabFile(@NotNull Project project, @NotNull VirtualFile file) {
        revealSubtab(project, file);
    }

    private static boolean shouldRevealGroup(@NotNull Project project) {
        return SubtabProjectViewGrouping.isEnabled()
                && SubtabsSettings.getInstance().getGroupTreeControlStyle() == SubtabGroupTreeControlStyle.NONE;
    }

    private static boolean revealGroupContaining(@NotNull Project project, @NotNull VirtualFile file) {
        JTree tree = projectViewTree(project);
        if (tree == null) {
            return false;
        }

        TreePath groupPath = findGroupPath(tree, file);
        if (groupPath == null) {
            return false;
        }

        activateProjectView(project);
        tree.setSelectionPath(groupPath);
        tree.scrollPathToVisible(groupPath);
        return true;
    }

    static @Nullable TreePath findGroupPath(@NotNull JTree tree, @NotNull VirtualFile file) {
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath path = tree.getPathForRow(row);
            if (path == null) {
                continue;
            }
            Object userObject = TreeUtil.getLastUserObject(path);
            if (userObject instanceof SubtabGroupProjectViewNode groupNode && groupNode.contains(file)) {
                return path;
            }
        }
        return null;
    }

    private static void activateProjectView(@NotNull Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);
        if (toolWindow != null && !toolWindow.isActive()) {
            toolWindow.activate(null);
        }
    }

    private static @Nullable JTree projectViewTree(@NotNull Project project) {
        AbstractProjectViewPane pane = ProjectView.getInstance(project).getCurrentProjectViewPane();
        return pane == null ? null : pane.getTree();
    }
}
