package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class ComponentFileNaming {
    private ComponentFileNaming() {
    }

    static @Nullable String componentBaseName(@NotNull String fileName) {
        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.match(fileName, rules());
        return match == null ? null : match.groupKey();
    }

    static boolean searchNeighbors(@NotNull String baseName) {
        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.resolveGroup(baseName, rules());
        return match != null && match.searchNeighbors();
    }

    static @NotNull String displayName(@NotNull String baseName) {
        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.resolveGroup(baseName, rules());
        return match == null ? baseName : match.displayName();
    }

    static @NotNull List<SubtabCandidate> candidates(@NotNull String baseName) {
        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.resolveGroup(baseName, rules());
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
