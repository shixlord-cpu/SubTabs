package com.zayax.tabz;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Tabz- und Tabz-Kontextmenüs: gespeicherte Splittabs öffnen, wenn sie nicht im Vordergrund sind.
 */
final class ComponentSubtabEditorSplitTabzMenu {
    private ComponentSubtabEditorSplitTabzMenu() {
    }

    static void addOpenSavedSplittabActions(
            @NotNull DefaultActionGroup group,
            @NotNull Project project,
            @Nullable VirtualFile contextFile,
            @Nullable SubtabGroupProjectViewNode groupNode
    ) {
        addOpenSavedSplittabActions(group, project, contextFile, groupNode, true);
    }

    static void addOpenSavedSplittabActions(
            @NotNull DefaultActionGroup group,
            @NotNull Project project,
            @Nullable VirtualFile contextFile,
            @Nullable SubtabGroupProjectViewNode groupNode,
            boolean leadingSeparator
    ) {
        List<ComponentSubtabEditorSplitRegistry.SplittabPair> pairs =
                closedPairsForContext(project, contextFile, groupNode);
        if (pairs.isEmpty()) {
            return;
        }
        if (leadingSeparator) {
            group.addSeparator();
        }
        int index = 0;
        for (ComponentSubtabEditorSplitRegistry.SplittabPair pair : pairs) {
            group.add(new OpenSavedSplittabAction(project, pair, index));
            index++;
        }
    }

    static @NotNull List<ComponentSubtabEditorSplitRegistry.SplittabPair> closedPairsForContext(
            @NotNull Project project,
            @Nullable VirtualFile contextFile,
            @Nullable SubtabGroupProjectViewNode groupNode
    ) {
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        if (registry.all().isEmpty()) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        List<ComponentSubtabEditorSplitRegistry.SplittabPair> result = new ArrayList<>();
        if (groupNode != null) {
            for (var member : groupNode.members()) {
                VirtualFile file = member.getVirtualFile();
                if (file != null) {
                    collectClosedPairsForFile(project, file, seen, result);
                }
            }
            return result;
        }
        if (contextFile != null) {
            collectClosedPairsForFile(project, contextFile, seen, result);
        }
        return result;
    }

    private static void collectClosedPairsForFile(
            @NotNull Project project,
            @NotNull VirtualFile file,
            @NotNull Set<String> seen,
            @NotNull List<ComponentSubtabEditorSplitRegistry.SplittabPair> result
    ) {
        for (ComponentSubtabEditorSplitRegistry.SplittabPair pair
                : ComponentSubtabEditorSplitRegistry.getInstance(project).all()) {
            if (!pairMatchesContext(file, pair)) {
                continue;
            }
            if (isClosedPair(project, pair) && seen.add(pair.id())) {
                result.add(pair);
            }
        }
    }

    private static boolean pairMatchesContext(
            @NotNull VirtualFile contextFile,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        if (pair.covers(contextFile)) {
            return true;
        }
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(contextFile);
        if (match == null) {
            return false;
        }
        Set<VirtualFile> groupFiles = new HashSet<>();
        for (ComponentRelatedFiles.Entry entry : match.relatedFiles()) {
            groupFiles.add(entry.file());
        }
        return groupFiles.contains(pair.leftFile()) && groupFiles.contains(pair.rightFile());
    }

    static boolean isClosedPair(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        return !ComponentSubtabEditorSplitMainTab.isForeground(project, pair);
    }

    private static final class OpenSavedSplittabAction extends AnAction {
        private final Project project;
        private final ComponentSubtabEditorSplitRegistry.SplittabPair pair;

        private OpenSavedSplittabAction(
                @NotNull Project project,
                @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair,
                int index
        ) {
            super(
                    "Open split pair: "
                            + ComponentSubtabEditorSplitPresentation.linkBarText(pair, index)
            );
            this.project = project;
            this.pair = pair;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            ComponentSubtabEditorSplitNavigation.activatePair(project, pair.id());
        }
    }
}
