package de.sasbe.subtabs;

import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SubtabGroupTreeStructureProvider implements TreeStructureProvider, DumbAware {
    @Override
    public @NotNull Collection<AbstractTreeNode<?>> modify(
            @NotNull AbstractTreeNode<?> parent,
            @NotNull Collection<AbstractTreeNode<?>> children,
            @NotNull ViewSettings settings
    ) {
        if (!SubtabProjectViewGrouping.isEnabled()) {
            return children;
        }
        if (!(parent instanceof PsiDirectoryNode)) {
            return children;
        }

        List<AbstractTreeNode<?>> result = new ArrayList<>(children.size());
        List<PsiFileNode> fileNodes = new ArrayList<>();
        for (AbstractTreeNode<?> child : children) {
            if (child instanceof PsiDirectoryNode directoryNode) {
                List<AbstractTreeNode<?>> replacement = replacementFor(directoryNode, settings);
                if (replacement != null) {
                    result.addAll(replacement);
                    continue;
                }
                result.add(child);
                continue;
            }
            if (child instanceof PsiFileNode fileNode) {
                fileNodes.add(fileNode);
                continue;
            }
            result.add(child);
        }

        result.addAll(nestRelatedFiles(parent.getProject(), fileNodes, settings));
        return result;
    }

    private static @NotNull Collection<AbstractTreeNode<?>> nestRelatedFiles(
            @Nullable Project project,
            @NotNull List<PsiFileNode> fileNodes,
            @NotNull ViewSettings settings
    ) {
        if (project == null || fileNodes.isEmpty()) {
            return List.copyOf(fileNodes);
        }

        List<String> fileNames = new ArrayList<>(fileNodes.size());
        for (PsiFileNode fileNode : fileNodes) {
            VirtualFile file = fileNode.getVirtualFile();
            fileNames.add(file == null ? "" : file.getName());
        }
        Set<String> nestedKeys = Set.copyOf(SubtabProjectViewGrouping.groupKeysForNesting(fileNames));
        if (nestedKeys.isEmpty()) {
            return List.copyOf(fileNodes);
        }

        Map<String, List<PsiFileNode>> grouped = new LinkedHashMap<>();
        List<AbstractTreeNode<?>> result = new ArrayList<>(fileNodes.size());
        for (PsiFileNode fileNode : fileNodes) {
            VirtualFile file = fileNode.getVirtualFile();
            String groupKey = file == null ? null : ComponentFileNaming.componentBaseName(file.getName());
            if (groupKey == null || !nestedKeys.contains(groupKey)) {
                result.add(fileNode);
                continue;
            }
            grouped.computeIfAbsent(groupKey, key -> new ArrayList<>()).add(fileNode);
        }
        for (Map.Entry<String, List<PsiFileNode>> entry : grouped.entrySet()) {
            result.add(new SubtabGroupProjectViewNode(project, entry.getKey(), entry.getValue(), settings));
        }
        return result;
    }

    private static @Nullable List<AbstractTreeNode<?>> replacementFor(
            @NotNull PsiDirectoryNode directoryNode,
            @NotNull ViewSettings settings
    ) {
        PsiDirectory directory = directoryNode.getValue();
        Project project = directoryNode.getProject();
        if (directory == null || project == null) {
            return null;
        }

        PsiDirectory[] subdirectories = directory.getSubdirectories();
        PsiFile[] files = directory.getFiles();
        List<String> fileNames = new ArrayList<>(files.length);
        for (PsiFile file : files) {
            if (file.getVirtualFile() != null) {
                fileNames.add(file.getVirtualFile().getName());
            }
        }

        List<String> groupKeys = SubtabProjectViewGrouping.groupKeysIfReplaceable(
                fileNames,
                subdirectories.length
        );
        if (groupKeys.isEmpty()) {
            return null;
        }

        Map<String, List<PsiFileNode>> nodesByGroup = new LinkedHashMap<>();
        for (PsiFile file : files) {
            if (file.getVirtualFile() == null) {
                continue;
            }
            String groupKey = ComponentFileNaming.componentBaseName(file.getVirtualFile().getName());
            if (groupKey == null) {
                return null;
            }
            nodesByGroup.computeIfAbsent(groupKey, key -> new ArrayList<>())
                    .add(new PsiFileNode(project, file, settings));
        }

        List<AbstractTreeNode<?>> replacement = new ArrayList<>(groupKeys.size());
        for (String groupKey : groupKeys) {
            List<PsiFileNode> nodes = nodesByGroup.get(groupKey);
            if (nodes == null || nodes.size() < 2) {
                return null;
            }
            replacement.add(new SubtabGroupProjectViewNode(project, groupKey, nodes, settings));
        }
        return replacement;
    }
}
