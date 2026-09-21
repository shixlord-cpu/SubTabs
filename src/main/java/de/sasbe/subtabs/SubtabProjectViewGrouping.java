package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class SubtabProjectViewGrouping {
    record ProjectViewGroup(@NotNull String groupKey, @NotNull List<String> fileNames) {
    }

    private SubtabProjectViewGrouping() {
    }

    static boolean isEnabled() {
        SubtabsSettings settings = SubtabsSettings.getInstance();
        return settings.isFamiliaEnabled() && settings.isGroupRelatedFilesInProjectView();
    }

    static boolean shouldReplaceFolder(@NotNull List<String> visibleFileNames, int visibleDirectoryCount) {
        return !groupKeysIfReplaceable(visibleFileNames, visibleDirectoryCount).isEmpty();
    }

    static @NotNull List<String> groupKeysForNesting(@NotNull List<String> visibleFileNames) {
        List<String> mergeKeys = new ArrayList<>();
        for (ProjectViewGroup group : nestGroups(visibleFileNames)) {
            mergeKeys.add(mergeKey(group.groupKey()));
        }
        return mergeKeys;
    }

    static @NotNull List<String> groupKeysIfReplaceable(
            @NotNull List<String> visibleFileNames,
            int visibleDirectoryCount
    ) {
        if (visibleDirectoryCount > 0 || visibleFileNames.size() < 2) {
            return List.of();
        }

        List<ProjectViewGroup> groups = partitionedGroups(visibleFileNames);
        if (!coversEveryFile(visibleFileNames, groups)) {
            return List.of();
        }

        List<String> mergeKeys = new ArrayList<>(groups.size());
        for (ProjectViewGroup group : groups) {
            if (group.fileNames().size() < 2) {
                return List.of();
            }
            mergeKeys.add(mergeKey(group.groupKey()));
        }
        return mergeKeys;
    }

    static @NotNull List<ProjectViewGroup> partitionedGroups(@NotNull List<String> visibleFileNames) {
        List<ProjectViewGroup> groups = groupsForActiveRules(visibleFileNames);
        return coversEveryFile(visibleFileNames, groups) ? groups : List.of();
    }

    static @NotNull List<ProjectViewGroup> nestGroups(@NotNull List<String> visibleFileNames) {
        return groupsForActiveRules(visibleFileNames);
    }

    static @NotNull List<ProjectViewGroup> groupsForActiveRules(@NotNull List<String> visibleFileNames) {
        Map<Integer, List<String>> filesByRule = new LinkedHashMap<>();
        List<CustomSubtabRule> rules = ComponentFileNaming.rules();
        for (String fileName : visibleFileNames) {
            int ruleIndex = CustomSubtabRuleMatcher.firstMatchingRuleIndex(fileName, rules);
            if (ruleIndex < 0) {
                continue;
            }
            filesByRule.computeIfAbsent(ruleIndex, key -> new ArrayList<>()).add(fileName);
        }

        List<ProjectViewGroup> groups = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> entry : filesByRule.entrySet()) {
            groups.addAll(groupsForRule(entry.getValue(), entry.getKey()));
        }
        return groups;
    }

    static @NotNull List<ProjectViewGroup> groupsForRule(
            @NotNull List<String> visibleFileNames,
            int ruleIndex
    ) {
        List<CustomSubtabRule> rules = ComponentFileNaming.rules();
        if (ruleIndex < 0 || ruleIndex >= rules.size()) {
            return List.of();
        }
        CustomSubtabRule rule = rules.get(ruleIndex);
        if (!rule.enabled || rule.isSpecial()) {
            return List.of();
        }

        Map<String, List<String>> filesByMergeKey = new LinkedHashMap<>();
        Map<String, String> groupKeyByMergeKey = new LinkedHashMap<>();
        for (String fileName : visibleFileNames) {
            CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.matchAtIndex(fileName, rules, ruleIndex);
            if (match == null) {
                continue;
            }
            String mergeKey = mergeKey(match.groupKey());
            filesByMergeKey.computeIfAbsent(mergeKey, key -> new ArrayList<>()).add(fileName);
            groupKeyByMergeKey.putIfAbsent(mergeKey, match.groupKey());
        }

        List<ProjectViewGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : filesByMergeKey.entrySet()) {
            if (entry.getValue().size() < 2) {
                continue;
            }
            String groupKey = groupKeyByMergeKey.get(entry.getKey());
            if (groupKey == null) {
                continue;
            }
            groups.add(new ProjectViewGroup(groupKey, List.copyOf(entry.getValue())));
        }
        return groups;
    }

    private static boolean coversEveryFile(
            @NotNull List<String> visibleFileNames,
            @NotNull List<ProjectViewGroup> groups
    ) {
        Set<String> grouped = new LinkedHashSet<>();
        for (ProjectViewGroup group : groups) {
            grouped.addAll(group.fileNames());
        }
        return grouped.size() == visibleFileNames.size();
    }

    static @NotNull String mergeKey(@NotNull String groupKey) {
        if (!CustomSubtabRuleMatcher.isRuleGroupKey(groupKey)) {
            return groupKey;
        }

        CustomSubtabRuleMatcher.ParsedGroupKey parsed = CustomSubtabRuleMatcher.parseGroupKey(groupKey);
        if (parsed == null || parsed.ruleIndex() < 0 || parsed.ruleIndex() >= ComponentFileNaming.rules().size()) {
            return groupKey;
        }

        return "merge:rule:" + parsed.ruleIndex() + ":" + parsed.groupName();
    }
}
