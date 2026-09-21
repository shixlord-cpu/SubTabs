package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubtabRulePrecedenceTest {
    @Test
    void lowerStateFolderRuleIsShadowedByState() {
        List<CustomSubtabRule> rules = SubtabRulesDefaults.createDefaults();

        int stateIndex = indexOf(rules, "State");
        int stateFolderIndex = indexOf(rules, "State Folder");

        assertTrue(SubtabRulePrecedence.canWinFirstMatch(rules, stateIndex));
        assertTrue(SubtabRulePrecedence.isShadowed(rules, stateFolderIndex));
    }

    @Test
    void folderRuleAboveShadowsLowerPatternRules() {
        List<CustomSubtabRule> rules = new ArrayList<>();
        rules.add(SubtabRulesDefaults.folderRule());
        rules.add(SubtabRulesDefaults.createDefaults().stream()
                .filter(rule -> "State".equals(rule.name))
                .findFirst()
                .orElseThrow());

        assertTrue(SubtabRulePrecedence.canWinFirstMatch(rules, 0));
        assertTrue(SubtabRulePrecedence.isShadowed(rules, 1));
    }

    @Test
    void nonOverlappingRulesAreNotShadowed() {
        List<CustomSubtabRule> rules = SubtabRulesDefaults.createDefaults();

        assertFalse(SubtabRulePrecedence.isShadowed(rules, indexOf(rules, "npm")));
        assertFalse(SubtabRulePrecedence.isShadowed(rules, indexOf(rules, "State")));
    }

    @Test
    void disabledRulesAreNotMarkedShadowed() {
        List<CustomSubtabRule> rules = SubtabRulesDefaults.createDefaults();
        rules.get(indexOf(rules, "State")).enabled = false;

        assertFalse(SubtabRulePrecedence.isShadowed(rules, indexOf(rules, "State Folder")));
    }

    private static int indexOf(List<CustomSubtabRule> rules, String name) {
        for (int index = 0; index < rules.size(); index++) {
            if (name.equals(rules.get(index).name)) {
                return index;
            }
        }
        throw new AssertionError("Missing rule: " + name);
    }
}
