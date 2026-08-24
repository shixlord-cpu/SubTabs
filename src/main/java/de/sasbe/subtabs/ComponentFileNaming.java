package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class ComponentFileNaming {
    private ComponentFileNaming() {
    }

    static @Nullable String componentBaseName(@NotNull String fileName) {
        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.match(
                fileName,
                rules(),
                customGroups()
        );
        return match == null ? null : match.groupKey();
    }

    static @NotNull List<CustomSubtabGroupDefinition> customGroups() {
        if (com.intellij.openapi.application.ApplicationManager.getApplication() == null) {
            return List.of();
        }
        return SubtabsSettings.getInstance().getCustomGroups();
    }

    static boolean searchNeighbors(@NotNull String baseName) {
        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.resolveGroup(
                baseName,
                rules(),
                customGroups()
        );
        return match != null && match.searchNeighbors();
    }

    static @NotNull String displayName(@NotNull String groupKey) {
        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.resolveGroup(
                groupKey,
                rules(),
                customGroups()
        );
        if (match == null) {
            return groupKey;
        }
        CustomSubtabRuleMatcher.ParsedGroupKey parsed = CustomSubtabRuleMatcher.parseGroupKey(groupKey);
        if (parsed == null || parsed.ruleIndex() < 0 || parsed.ruleIndex() >= rules().size()) {
            return match.displayName();
        }
        return CustomSubtabRuleMatcher.displayNameWithSuffix(
                match.displayName(),
                rules().get(parsed.ruleIndex())
        );
    }

    static @NotNull List<SubtabCandidate> candidates(@NotNull String baseName) {
        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.resolveGroup(
                baseName,
                rules(),
                customGroups()
        );
        return match == null ? List.of() : match.candidates();
    }

    static @NotNull List<CustomSubtabRule> rules() {
        if (com.intellij.openapi.application.ApplicationManager.getApplication() == null) {
            return SubtabRulesDefaults.createDefaults();
        }
        List<CustomSubtabRule> configured = SubtabsSettings.getInstance().getRules();
        return configured.isEmpty() ? SubtabRulesDefaults.createDefaults() : configured;
    }
}
