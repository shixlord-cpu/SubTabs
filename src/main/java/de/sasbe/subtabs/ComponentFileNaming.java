package de.sasbe.subtabs;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class ComponentFileNaming {
    private static volatile @NotNull List<CustomSubtabRule> cachedRules = List.of();
    private static volatile int cachedRulesGeneration = -1;

    private ComponentFileNaming() {
    }

    static void invalidateRulesCache() {
        cachedRulesGeneration = -1;
    }

    static @Nullable String componentBaseName(@NotNull String fileName) {
        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.match(fileName, rules());
        return match == null ? null : match.groupKey();
    }

    static boolean searchNeighbors(@NotNull String baseName) {
        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.resolveGroup(baseName, rules());
        return match != null && match.searchNeighbors();
    }

    static boolean createsSubtabs(@NotNull String groupKey) {
        return !CustomSubtabRuleMatcher.isUserGroupKey(groupKey);
    }

    static @NotNull String displayName(@NotNull String groupKey) {
        return displayName(groupKey, null);
    }

    static @NotNull String displayName(@NotNull String groupKey, @Nullable VirtualFile contextFile) {
        if (contextFile != null && CustomSubtabRuleMatcher.isFolderGroupKey(groupKey)) {
            VirtualFile parent = contextFile.getParent();
            if (parent != null && !parent.getName().isBlank()) {
                return parent.getName();
            }
        }

        CustomSubtabRuleMatcher.ParsedGroupKey parsed = CustomSubtabRuleMatcher.parseGroupKey(groupKey);
        if (parsed != null && parsed.ruleIndex() >= 0 && parsed.ruleIndex() < rules().size()) {
            return CustomSubtabRuleMatcher.displayNameWithSuffix(
                    parsed.groupName(),
                    rules().get(parsed.ruleIndex())
            );
        }
        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.resolveGroup(groupKey, rules());
        if (match == null) {
            return groupKey;
        }
        return match.displayName();
    }

    static @NotNull List<SubtabCandidate> candidates(@NotNull String baseName) {
        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.resolveGroup(baseName, rules());
        return match == null ? List.of() : match.candidates();
    }

    static @NotNull List<CustomSubtabRule> rules() {
        if (com.intellij.openapi.application.ApplicationManager.getApplication() == null) {
            return SubtabRulesDefaults.createDefaults();
        }
        SubtabsSettings settings = SubtabsSettings.getInstance();
        int generation = settings.getRulesGeneration();
        if (generation != cachedRulesGeneration) {
            List<CustomSubtabRule> configured = settings.getRules();
            cachedRules = configured.isEmpty()
                    ? SubtabRulesDefaults.createDefaults()
                    : List.copyOf(configured);
            cachedRulesGeneration = generation;
        }
        return cachedRules;
    }
}
