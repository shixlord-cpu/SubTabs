package de.sasbe.subtabs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

final class ComponentSubtabEditorSplitOrder {
    private ComponentSubtabEditorSplitOrder() {
    }

    static boolean canMoveLeft(@NotNull Project project, @NotNull String pairId) {
        return indexOf(project, pairId) > 0;
    }

    static boolean canMoveRight(@NotNull Project project, @NotNull String pairId) {
        ComponentSubtabEditorSplitRegistry registry = ComponentSubtabEditorSplitRegistry.getInstance(project);
        int index = indexOf(project, pairId);
        return index >= 0 && index < registry.all().size() - 1;
    }

    static void moveLeft(@NotNull Project project, @NotNull String pairId) {
        int index = indexOf(project, pairId);
        if (index > 0) {
            ComponentSubtabEditorSplitRegistry.getInstance(project).reorder(pairId, index - 1);
            refreshBar(project);
        }
    }

    static void moveRight(@NotNull Project project, @NotNull String pairId) {
        int index = indexOf(project, pairId);
        if (index >= 0 && canMoveRight(project, pairId)) {
            ComponentSubtabEditorSplitRegistry.getInstance(project).reorder(pairId, index + 1);
            refreshBar(project);
        }
    }

    static void reorderByLeftFile(
            @NotNull Project project,
            @NotNull VirtualFile leftFile,
            int dropIndex
    ) {
        ComponentSubtabEditorSplitRegistry registry = ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair pair = null;
        for (ComponentSubtabEditorSplitRegistry.SplittabPair candidate : registry.all()) {
            if (candidate.leftFile().equals(leftFile)) {
                pair = candidate;
                break;
            }
        }
        if (pair == null) {
            return;
        }
        registry.reorder(pair.id(), dropIndex);
        refreshBar(project);
    }

    /** Switch-bar tabs use a unique drag key (typically the pair's right file). */
    static void reorderByPairDragKey(
            @NotNull Project project,
            @NotNull VirtualFile dragKeyFile,
            int dropIndex
    ) {
        ComponentSubtabEditorSplitRegistry registry = ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair pair = null;
        for (ComponentSubtabEditorSplitRegistry.SplittabPair candidate : registry.all()) {
            if (candidate.leftFile().equals(dragKeyFile) || candidate.rightFile().equals(dragKeyFile)) {
                pair = candidate;
                break;
            }
        }
        if (pair == null) {
            return;
        }
        registry.reorder(pair.id(), dropIndex);
        refreshBar(project);
    }

    private static int indexOf(@NotNull Project project, @NotNull String pairId) {
        int index = 0;
        for (ComponentSubtabEditorSplitRegistry.SplittabPair pair
                : ComponentSubtabEditorSplitRegistry.getInstance(project).all()) {
            if (pair.id().equals(pairId)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private static void refreshBar(@NotNull Project project) {
        ComponentSubtabEditorSplitRegistry.SplittabPair active =
                ComponentSubtabEditorSplitRegistry.getInstance(project).activePair();
        if (active != null) {
            ComponentSubtabEditorSplitPresentation.applySplittabPresentation(project, active);
        }
    }
}
