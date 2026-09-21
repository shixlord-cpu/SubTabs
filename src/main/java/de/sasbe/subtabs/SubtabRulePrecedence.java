package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

final class SubtabRulePrecedence {
    private SubtabRulePrecedence() {
    }

    static boolean isShadowed(@NotNull List<CustomSubtabRule> rules, int ruleIndex) {
        if (ruleIndex < 0 || ruleIndex >= rules.size()) {
            return false;
        }
        CustomSubtabRule rule = rules.get(ruleIndex);
        if (!rule.enabled) {
            return false;
        }
        return !canWinFirstMatch(rules, ruleIndex);
    }

    static boolean canWinFirstMatch(@NotNull List<CustomSubtabRule> rules, int ruleIndex) {
        if (ruleIndex < 0 || ruleIndex >= rules.size() || !rules.get(ruleIndex).enabled) {
            return false;
        }
        CustomSubtabRule rule = rules.get(ruleIndex);
        for (String probe : probeFileNames(rule)) {
            if (CustomSubtabRuleMatcher.firstMatchingRuleIndex(probe, rules) == ruleIndex) {
                return true;
            }
        }
        return false;
    }

    private static @NotNull List<String> probeFileNames(@NotNull CustomSubtabRule rule) {
        if (rule.isSpecial()) {
            return switch (rule.type) {
                case FOLDER -> List.of("README.md");
                case USER_GROUPS -> List.of("application.properties", "application-dev.properties");
                default -> List.of();
            };
        }

        List<String> probes = new ArrayList<>();
        for (String pattern : parseCsv(rule.patterns)) {
            if (CustomSubtabRuleMatcher.usesSuffixMatching(pattern)) {
                probes.add("probe" + (pattern.startsWith(".") ? pattern : "." + pattern));
            } else {
                probes.add(pattern);
            }
        }
        return List.copyOf(probes);
    }

    private static @NotNull List<String> parseCsv(@NotNull String raw) {
        if (raw.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }
}
