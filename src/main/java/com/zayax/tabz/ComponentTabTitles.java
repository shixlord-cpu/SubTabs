package com.zayax.tabz;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ComponentTabTitles {
    private ComponentTabTitles() {
    }

    static @Nullable String mainTabTitle(boolean tabzCollapsed, @NotNull VirtualFile file) {
        if (tabzCollapsed) {
            return null;
        }

        String groupedTitle = groupedTitleFor(file);
        if (groupedTitle == null) {
            return null;
        }

        if (TabzSettings.getInstance().isShowSubtabNameInMainTab()) {
            String tabzLabel = tabzLabelFor(file);
            if (tabzLabel != null) {
                return groupedTitle + " (" + tabzLabel + ")";
            }
        }

        return groupedTitle;
    }

    static @NotNull String displayTabzLabel(@NotNull VirtualFile file) {
        String label = tabzLabelFor(file);
        return label != null ? label : file.getPresentableName();
    }

    static @NotNull String displayGroupName(@NotNull VirtualFile file) {
        String grouped = displayGroupedTitle(file);
        return grouped != null ? grouped : file.getPresentableName();
    }

    static @Nullable String displayGroupedTitle(@NotNull VirtualFile file) {
        String baseName = ComponentFileNaming.componentBaseName(file.getName());
        if (baseName == null) {
            return null;
        }
        return ComponentFileNaming.displayName(baseName, file);
    }

    static @Nullable String displayGroupedTitle(@NotNull String fileName) {
        String baseName = ComponentFileNaming.componentBaseName(fileName);
        if (baseName == null) {
            return null;
        }
        return ComponentFileNaming.displayName(baseName);
    }

    private static @Nullable String groupedTitleFor(@NotNull VirtualFile file) {
        return displayGroupedTitle(file);
    }

    private static @Nullable String tabzLabelFor(@NotNull VirtualFile file) {
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(file);
        if (match != null) {
            for (ComponentRelatedFiles.Entry entry : match.relatedFiles()) {
                if (entry.file().equals(file)) {
                    return entry.label();
                }
            }
        }
        return tabzLabelFromRules(file);
    }

    private static @Nullable String tabzLabelFromRules(@NotNull VirtualFile file) {
        CustomSubtabRuleMatcher.Match ruleMatch = CustomSubtabRuleMatcher.match(
                file.getName(),
                ComponentFileNaming.rules()
        );
        if (ruleMatch == null) {
            return null;
        }

        for (SubtabCandidate candidate : ComponentFileNaming.candidates(ruleMatch.groupKey())) {
            if (candidate.fileName().equals(file.getName())) {
                return candidate.resolveLabel(file.getName());
            }
        }
        return null;
    }
}
