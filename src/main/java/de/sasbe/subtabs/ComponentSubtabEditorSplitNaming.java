package de.sasbe.subtabs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ComponentSubtabEditorSplitNaming {
    private ComponentSubtabEditorSplitNaming() {
    }

    static void renameLinkName(
            @NotNull Project project,
            @NotNull String pairId,
            @Nullable SplittabSwitchBarPanel barPanel
    ) {
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair pair = registry.findById(pairId);
        if (pair == null) {
            return;
        }
        int index = indexOfPair(registry, pairId);
        String current = pair.linkName() != null
                ? pair.linkName()
                : ComponentSubtabEditorSplitPresentation.linkBarText(pair, index);
        String entered = Messages.showInputDialog(
                project,
                "Name for this split-pair link (left bar):",
                "Rename split pair",
                null,
                current,
                null
        );
        if (entered == null) {
            return;
        }
        String normalized = entered.isBlank() ? null : entered.trim();
        String defaultNumber = String.valueOf(index + 1);
        if (normalized != null && normalized.equals(defaultNumber)) {
            normalized = null;
        }
        registry.setLinkName(pairId, normalized);
        refreshAfterRename(project, pairId, barPanel);
    }

    static void renameHeaderLabel(
            @NotNull Project project,
            @NotNull String pairId
    ) {
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair pair = registry.findById(pairId);
        if (pair == null) {
            return;
        }
        String current = ComponentSubtabEditorSplitPresentation.paneHeaderText(pair);
        String entered = Messages.showInputDialog(
                project,
                "Label in the right split-pair header:",
                "Rename split-pair label",
                null,
                current,
                null
        );
        if (entered == null) {
            return;
        }
        String normalized = entered.isBlank() ? null : entered.trim();
        String defaultLabel = ComponentSubtabEditorSplitPresentation.defaultHeaderText(pair);
        if (normalized != null && normalized.equals(defaultLabel)) {
            normalized = null;
        }
        registry.setHeaderLabel(pairId, normalized);
        refreshAfterRename(project, pairId, null);
    }

    private static int indexOfPair(
            @NotNull ComponentSubtabEditorSplitRegistry registry,
            @NotNull String pairId
    ) {
        int index = 0;
        for (ComponentSubtabEditorSplitRegistry.SplittabPair candidate : registry.all()) {
            if (candidate.id().equals(pairId)) {
                return index;
            }
            index++;
        }
        return 0;
    }

    private static void refreshAfterRename(
            @NotNull Project project,
            @NotNull String pairId,
            @Nullable SplittabSwitchBarPanel barPanel
    ) {
        if (barPanel != null) {
            barPanel.refresh();
        } else {
            ComponentSubtabsSplittabUi.refreshPairChrome(project, pairId);
        }
        SplittabRestoreOverlay.syncProject(project);
    }
}
