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
        for (int index = 0; index < rules.size(); index++) {
            CustomSubtabRule rule = rules.get(index);
            if (!participatesInRuleSwitch(rule)) {
                continue;
            }
            if (CustomSubtabRuleMatcher.matchesRuleIndex(fileName, rules, index)) {
                indices.add(index);
            }
        }
        return List.copyOf(indices);
    }

    static boolean participatesInRuleSwitch(@NotNull CustomSubtabRule rule) {
        return rule.type != CustomSubtabRule.Type.FOLDER
                && rule.type != CustomSubtabRule.Type.USER_GROUPS;
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
