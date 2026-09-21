package de.sasbe.subtabs;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves the subtab group behind an editor-tab or project-view context menu.
 */
final class SubtabFamiliaContext {
    private SubtabFamiliaContext() {
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
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            return null;
        }
        return event.getData(CommonDataKeys.VIRTUAL_FILE);
    }

    static @Nullable String colorStorageKey(@NotNull AnActionEvent event) {
        SubtabGroupProjectViewNode groupNode = SubtabGroupProjectViewContext.selectedGroupNode(event);
        if (groupNode != null) {
            return SubtabGroupProjectViewContext.colorStorageKey(groupNode);
        }
        VirtualFile file = editorTabFile(event);
        if (file == null || !hasSubtabGroup(file)) {
            return null;
        }
        return SubtabGroupColors.colorKey(file);
    }

    private static boolean hasSubtabGroup(@NotNull VirtualFile file) {
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(file);
        return match != null && match.relatedFiles().size() >= 2;
    }
}
