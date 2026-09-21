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
import java.util.LinkedHashSet;
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
        return mergeSiblingGroupNodes(parent.getProject(), result, settings);
    }

    private static @NotNull List<AbstractTreeNode<?>> mergeSiblingGroupNodes(
            @Nullable Project project,
            @NotNull List<AbstractTreeNode<?>> nodes,
            @NotNull ViewSettings settings
    ) {
        if (project == null || nodes.size() < 2) {
            return nodes;
        }

        Map<String, List<PsiFileNode>> filesByMergeKey = new LinkedHashMap<>();
        Map<String, String> groupKeyByMergeKey = new LinkedHashMap<>();
        Map<String, Integer> groupNodesByMergeKey = new LinkedHashMap<>();
        for (AbstractTreeNode<?> node : nodes) {
            if (!(node instanceof SubtabGroupProjectViewNode groupNode)) {
                continue;
            }
            String mergeKey = SubtabProjectViewGrouping.mergeKey(groupNode.groupKey());
            groupKeyByMergeKey.putIfAbsent(mergeKey, groupNode.groupKey());
            groupNodesByMergeKey.merge(mergeKey, 1, Integer::sum);
            List<PsiFileNode> combined = filesByMergeKey.computeIfAbsent(mergeKey, key -> new ArrayList<>());
            appendUniqueFiles(combined, groupNode.members());
        }

        boolean needsMerge = groupNodesByMergeKey.values().stream().anyMatch(count -> count > 1);
        if (!needsMerge) {
            return nodes;
        }

        Set<String> emitted = new LinkedHashSet<>();
        List<AbstractTreeNode<?>> merged = new ArrayList<>(nodes.size());
        for (AbstractTreeNode<?> node : nodes) {
            if (!(node instanceof SubtabGroupProjectViewNode groupNode)) {
                merged.add(node);
                continue;
            }

            String mergeKey = SubtabProjectViewGrouping.mergeKey(groupNode.groupKey());
            if (!emitted.add(mergeKey)) {
                continue;
            }

            List<PsiFileNode> files = filesByMergeKey.get(mergeKey);
            if (files == null || files.size() < 2) {
                merged.addAll(files == null ? List.of() : files);
                continue;
            }
            String mergedGroupKey = groupKeyByMergeKey.getOrDefault(mergeKey, groupNode.groupKey());
            merged.add(new SubtabGroupProjectViewNode(
                    project,
                    mergedGroupKey,
                    files,
                    settings
            ));
        }
        return merged;
    }

    private static void appendUniqueFiles(
            @NotNull List<PsiFileNode> target,
            @NotNull List<PsiFileNode> source
    ) {
        Set<String> seen = new LinkedHashSet<>();
        for (PsiFileNode fileNode : target) {
            VirtualFile file = fileNode.getVirtualFile();
            if (file != null) {
                seen.add(file.getPath());
            }
        }
        for (PsiFileNode fileNode : source) {
            VirtualFile file = fileNode.getVirtualFile();
            if (file == null || seen.add(file.getPath())) {
                target.add(fileNode);
            }
        }
    }

    private static @NotNull Collection<AbstractTreeNode<?>> nestRelatedFiles(
            @Nullable Project project,
            @NotNull List<PsiFileNode> fileNodes,
            @NotNull ViewSettings settings
    ) {
        if (project == null || fileNodes.isEmpty()) {
            return List.copyOf(fileNodes);
        }

        List<String> fileNames = fileNames(fileNodes);
        Map<String, PsiFileNode> nodesByName = nodesByName(fileNodes);
        Set<String> inAnyGroup = new LinkedHashSet<>();
        List<AbstractTreeNode<?>> result = new ArrayList<>();

        for (SubtabProjectViewGrouping.ProjectViewGroup group : SubtabProjectViewGrouping.nestGroups(fileNames)) {
            List<PsiFileNode> members = fileNodesForNames(group.fileNames(), nodesByName);
            if (members.size() < 2) {
                continue;
            }
            inAnyGroup.addAll(group.fileNames());
            result.add(new SubtabGroupProjectViewNode(project, group.groupKey(), members, settings));
        }

        for (PsiFileNode fileNode : fileNodes) {
            VirtualFile file = fileNode.getVirtualFile();
            if (file == null || !inAnyGroup.contains(file.getName())) {
                result.add(fileNode);
            }
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
        if (subdirectories.length > 0) {
            return null;
        }

        PsiFile[] files = directory.getFiles();
        List<String> fileNames = new ArrayList<>(files.length);
        for (PsiFile file : files) {
            if (file.getVirtualFile() != null) {
                fileNames.add(file.getVirtualFile().getName());
            }
        }
        if (fileNames.size() < 2) {
            return null;
        }

        List<SubtabProjectViewGrouping.ProjectViewGroup> groups =
                SubtabProjectViewGrouping.partitionedGroups(fileNames);
        if (groups.isEmpty()) {
            return null;
        }
        Set<String> grouped = new LinkedHashSet<>();
        for (SubtabProjectViewGrouping.ProjectViewGroup group : groups) {
            grouped.addAll(group.fileNames());
        }
        if (grouped.size() != fileNames.size()) {
            return null;
        }

        Map<String, PsiFileNode> nodesByName = new LinkedHashMap<>();
        for (PsiFile file : files) {
            if (file.getVirtualFile() == null) {
                return null;
            }
            nodesByName.put(
                    file.getVirtualFile().getName(),
                    new PsiFileNode(project, file, settings)
            );
        }

        List<AbstractTreeNode<?>> replacement = new ArrayList<>(groups.size());
        for (SubtabProjectViewGrouping.ProjectViewGroup group : groups) {
            List<PsiFileNode> nodes = fileNodesForNames(group.fileNames(), nodesByName);
            if (nodes.size() < 2) {
                return null;
            }
            replacement.add(new SubtabGroupProjectViewNode(project, group.groupKey(), nodes, settings));
        }
        return replacement;
    }

    private static @NotNull List<String> fileNames(@NotNull List<PsiFileNode> fileNodes) {
        List<String> fileNames = new ArrayList<>(fileNodes.size());
        for (PsiFileNode fileNode : fileNodes) {
            VirtualFile file = fileNode.getVirtualFile();
            fileNames.add(file == null ? "" : file.getName());
        }
        return fileNames;
    }

    private static @NotNull Map<String, PsiFileNode> nodesByName(@NotNull List<PsiFileNode> fileNodes) {
        Map<String, PsiFileNode> nodesByName = new LinkedHashMap<>();
        for (PsiFileNode fileNode : fileNodes) {
            VirtualFile file = fileNode.getVirtualFile();
            if (file != null) {
                nodesByName.put(file.getName(), fileNode);
            }
        }
        return nodesByName;
    }

    private static @NotNull List<PsiFileNode> fileNodesForNames(
            @NotNull List<String> fileNames,
            @NotNull Map<String, PsiFileNode> nodesByName
    ) {
        List<PsiFileNode> nodes = new ArrayList<>(fileNames.size());
        for (String fileName : fileNames) {
            PsiFileNode fileNode = nodesByName.get(fileName);
            if (fileNode != null) {
                nodes.add(fileNode);
            }
        }
        return nodes;
    }
}
