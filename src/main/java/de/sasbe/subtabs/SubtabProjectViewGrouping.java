package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SubtabProjectViewGrouping {
    private SubtabProjectViewGrouping() {
    }

    static boolean isEnabled() {
        SubtabsSettings settings = SubtabsSettings.getInstance();
        return settings.isGroupRelatedFilesInProjectView() && settings.isSubtabsActive();
    }

    static boolean shouldReplaceFolder(@NotNull List<String> visibleFileNames, int visibleDirectoryCount) {
        return !groupKeysIfReplaceable(visibleFileNames, visibleDirectoryCount).isEmpty();
    }

    static @NotNull List<String> groupKeysForNesting(@NotNull List<String> visibleFileNames) {
        Map<String, Integer> counts = countsByGroup(visibleFileNames, false);
        List<String> groupKeys = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() >= 2) {
                groupKeys.add(entry.getKey());
            }
        }
        return groupKeys;
    }

    static @NotNull List<String> groupKeysIfReplaceable(
            @NotNull List<String> visibleFileNames,
            int visibleDirectoryCount
    ) {
        if (visibleDirectoryCount > 0 || visibleFileNames.size() < 2) {
            return List.of();
        }

        Map<String, Integer> counts = countsByGroup(visibleFileNames, true);
        if (counts.isEmpty()) {
            return List.of();
        }

        List<String> groupKeys = new ArrayList<>(counts.size());
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() < 2) {
                return List.of();
            }
            groupKeys.add(entry.getKey());
        }
        return groupKeys;
    }

    private static @NotNull Map<String, Integer> countsByGroup(
            @NotNull List<String> visibleFileNames,
            boolean requireEveryFileGrouped
    ) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String fileName : visibleFileNames) {
            String groupKey = ComponentFileNaming.componentBaseName(fileName);
            if (groupKey == null) {
                if (requireEveryFileGrouped) {
                    return Map.of();
                }
                continue;
            }
            counts.merge(groupKey, 1, Integer::sum);
        }
        return counts;
    }

    static @NotNull String mergeKey(@NotNull String groupKey) {
        if (!CustomSubtabRuleMatcher.isRuleGroupKey(groupKey)) {
            return groupKey;
        }

        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.resolveGroup(
                groupKey,
                ComponentFileNaming.rules()
        );
        if (match == null || !match.searchNeighbors()) {
            return groupKey;
        }

        CustomSubtabRuleMatcher.ParsedGroupKey parsed = CustomSubtabRuleMatcher.parseGroupKey(groupKey);
        if (parsed == null) {
            return groupKey;
        }
        return "merge:rule:" + parsed.ruleIndex() + ":" + parsed.stem();
    }
}
