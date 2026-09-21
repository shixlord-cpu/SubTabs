package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubtabRuleRotationTest {
    @Test
    void detectsMultipleMatchingStateRules() {
        List<CustomSubtabRule> rules = twoOverlappingStateRules();

        assertTrue(SubtabRuleRotation.hasMultipleMatches("cart.actions.ts", rules));
        assertEquals(List.of(0, 1), SubtabRuleRotation.matchingRuleIndices("cart.actions.ts", rules));
    }

    @Test
    void rotatesLastMatchingRuleBeforeFirst() {
        List<CustomSubtabRule> rules = new ArrayList<>(twoOverlappingStateRules());
        assertEquals("State Central", rules.get(0).name);
        assertEquals("State Feature", rules.get(1).name);

        assertTrue(SubtabRuleRotation.rotateMatchingRules("cart.actions.ts", rules));
        assertEquals("State Feature", rules.get(0).name);
        assertEquals("State Central", rules.get(1).name);
        assertEquals("rule:0:actions#cart", CustomSubtabRuleMatcher.match("cart.actions.ts", rules).groupKey());

        assertTrue(SubtabRuleRotation.rotateMatchingRules("cart.actions.ts", rules));
        assertEquals("State Central", rules.get(0).name);
        assertEquals("State Feature", rules.get(1).name);
    }

    @Test
    void matchAllReturnsEveryEnabledMatchInOrder() {
        List<CustomSubtabRule> rules = twoOverlappingStateRules();
        List<CustomSubtabRuleMatcher.Match> matches = CustomSubtabRuleMatcher.matchAll("cart.actions.ts", rules);

        assertEquals(2, matches.size());
        assertEquals("rule:0:cart", matches.get(0).groupKey());
        assertEquals("rule:1:actions#cart", matches.get(1).groupKey());
    }

    @Test
    void ignoresFolderAndUserGroupsForRuleSwitch() {
        List<CustomSubtabRule> rules = new ArrayList<>(twoOverlappingStateRules());
        rules.add(0, SubtabRulesDefaults.folderRule());
        rules.add(SubtabRulesDefaults.userGroupsRule());

        assertTrue(SubtabRuleRotation.hasMultipleMatches("cart.actions.ts", rules));
        assertEquals(List.of(1, 2), SubtabRuleRotation.matchingRuleIndices("cart.actions.ts", rules));
    }

    @Test
    void ignoresSingleMatchFiles() {
        List<CustomSubtabRule> rules = twoOverlappingStateRules();
        assertFalse(SubtabRuleRotation.hasMultipleMatches("notes.md", rules));
        assertFalse(SubtabRuleRotation.rotateMatchingRules("notes.md", rules));
    }

    private static @NotNull List<CustomSubtabRule> twoOverlappingStateRules() {
        CustomSubtabRule central = SubtabRulesDefaults.createDefaults().stream()
                .filter(rule -> "State Central".equals(rule.name))
                .findFirst()
                .orElseThrow();
        return new ArrayList<>(List.of(central, SubtabRulesDefaults.stateFeatureRule()));
    }
}
