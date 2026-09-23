package de.sasbe.subtabs;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JTree;
import javax.swing.tree.TreePath;

final class SubtabGroupProjectViewContext {
    private SubtabGroupProjectViewContext() {
    }

    static @Nullable VirtualFile selectedVirtualFile(@NotNull AnActionEvent event) {
        VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file != null) {
            return file;
        }

        Project project = event.getProject();
        if (project == null) {
            return null;
        }

        AbstractProjectViewPane pane = ProjectView.getInstance(project).getCurrentProjectViewPane();
        if (pane == null) {
            return null;
        }

        JTree tree = pane.getTree();
        if (tree == null) {
            return null;
        }

        TreePath path = tree.getSelectionPath();
        return path == null ? null : ComponentSubtabProjectViewHover.virtualFileOf(path);
    }

    static @Nullable SubtabGroupProjectViewNode selectedGroupNode(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return null;
        }

        AbstractProjectViewPane pane = ProjectView.getInstance(project).getCurrentProjectViewPane();
        if (pane == null) {
            return null;
        }

        JTree tree = pane.getTree();
        if (tree == null) {
            return null;
        }

        TreePath path = tree.getSelectionPath();
        if (path == null) {
            return null;
        }

        Object userObject = TreeUtil.getLastUserObject(path);
        return userObject instanceof SubtabGroupProjectViewNode groupNode ? groupNode : null;
    }

    static @Nullable String colorStorageKey(@NotNull SubtabGroupProjectViewNode groupNode) {
        return SubtabGroupColors.storageKey(groupNode);
    }
}
