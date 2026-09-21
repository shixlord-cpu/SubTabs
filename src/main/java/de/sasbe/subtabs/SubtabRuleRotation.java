package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

final class SubtabRuleRotation {
    private SubtabRuleRotation() {
    }

    static boolean hasMultipleMatches(@NotNull String fileName, @NotNull List<CustomSubtabRule> rules) {
        return matchingRuleIndices(fileName, rules).size() >= 2;
    }

    static @NotNull List<Integer> matchingRuleIndices(
            @NotNull String fileName,
            @NotNull List<CustomSubtabRule> rules
    ) {
        List<Integer> indices = new ArrayList<>();
        for (CustomSubtabRuleMatcher.Match match : CustomSubtabRuleMatcher.matchAll(fileName, rules)) {
            CustomSubtabRuleMatcher.ParsedGroupKey parsed = CustomSubtabRuleMatcher.parseGroupKey(match.groupKey());
            if (parsed != null) {
                indices.add(parsed.ruleIndex());
            }
        }
        return List.copyOf(indices);
    }

    /**
     * Rotates priority among rules that match the file: the last matching rule moves before the first.
     */
    static boolean rotateMatchingRules(
            @NotNull String fileName,
            @NotNull List<CustomSubtabRule> rules
    ) {
        List<Integer> matching = matchingRuleIndices(fileName, rules);
        if (matching.size() < 2) {
            return false;
        }
        int firstIndex = matching.get(0);
        int lastIndex = matching.get(matching.size() - 1);
        CustomSubtabRule lastRule = rules.remove(lastIndex);
        rules.add(firstIndex, lastRule);
        return true;
    }
}
