package com.zayax.tabz;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves the tabz group behind an editor-tab or project-view context menu.
 */
final class TabzContext {
    private TabzContext() {
    }

    static void updateColorAction(@NotNull AnActionEvent event) {
        event.getPresentation().setEnabledAndVisible(
                SubtabGroupColors.isEnabled() && colorStorageKey(event) != null
        );
    }

    static void updateRevealAction(@NotNull AnActionEvent event) {
        event.getPresentation().setEnabledAndVisible(editorTabFile(event) != null);
    }

    static @Nullable VirtualFile editorTabFile(@NotNull AnActionEvent event) {
        if (!TabzSettings.getInstance().isTabzEnabled()) {
            return null;
        }
        if (!ActionPlaces.EDITOR_TAB_POPUP.equals(event.getPlace())) {
            return null;
        }
        return event.getData(CommonDataKeys.VIRTUAL_FILE);
    }

    static boolean showInProjectViewPopup(@NotNull AnActionEvent event) {
        if (!TabzSettings.getInstance().isTabzEnabled()
                || !TabzSettings.getInstance().isSubtabsActive()) {
            return false;
        }
        if (!ActionPlaces.PROJECT_VIEW_POPUP.equals(event.getPlace())) {
            return false;
        }
        if (SubtabGroupProjectViewContext.selectedGroupNode(event) != null) {
            return true;
        }
        if (!SubtabGroupColors.isEnabled()) {
            return false;
        }
        VirtualFile file = SubtabGroupProjectViewContext.selectedVirtualFile(event);
        return file != null && belongsToSubtabGroup(file);
    }

    static @Nullable String colorStorageKey(@NotNull AnActionEvent event) {
        SubtabGroupProjectViewNode groupNode = SubtabGroupProjectViewContext.selectedGroupNode(event);
        if (groupNode != null) {
            return SubtabGroupProjectViewContext.colorStorageKey(groupNode);
        }
        VirtualFile file = fileForColorActions(event);
        if (file == null) {
            return null;
        }
        return SubtabGroupColors.colorKey(file);
    }

    static @Nullable VirtualFile fileForColorActions(@NotNull AnActionEvent event) {
        VirtualFile editorFile = editorTabFile(event);
        if (editorFile != null && belongsToSubtabGroup(editorFile)) {
            return editorFile;
        }
        if (!ActionPlaces.PROJECT_VIEW_POPUP.equals(event.getPlace())) {
            return null;
        }
        VirtualFile projectFile = SubtabGroupProjectViewContext.selectedVirtualFile(event);
        return projectFile != null && belongsToSubtabGroup(projectFile) ? projectFile : null;
    }

    static boolean belongsToSubtabGroup(@NotNull VirtualFile file) {
        return hasTabzGroup(file);
    }

    private static boolean hasTabzGroup(@NotNull VirtualFile file) {
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(file);
        return match != null && match.relatedFiles().size() >= 2;
    }
}
