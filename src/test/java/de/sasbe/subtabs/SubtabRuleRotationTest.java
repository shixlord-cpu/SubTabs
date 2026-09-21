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
        assertEquals("State", rules.get(0).name);
        assertEquals("State Nachbar", rules.get(1).name);

        assertTrue(SubtabRuleRotation.rotateMatchingRules("cart.actions.ts", rules));
        assertEquals("State Nachbar", rules.get(0).name);
        assertEquals("State", rules.get(1).name);
        assertEquals("rule:0:cart", CustomSubtabRuleMatcher.match("cart.actions.ts", rules).groupKey());

        assertTrue(SubtabRuleRotation.rotateMatchingRules("cart.actions.ts", rules));
        assertEquals("State", rules.get(0).name);
        assertEquals("State Nachbar", rules.get(1).name);
    }

    @Test
    void matchAllReturnsEveryEnabledMatchInOrder() {
        List<CustomSubtabRule> rules = twoOverlappingStateRules();
        List<CustomSubtabRuleMatcher.Match> matches = CustomSubtabRuleMatcher.matchAll("cart.actions.ts", rules);

        assertEquals(2, matches.size());
        assertEquals("rule:0:cart", matches.get(0).groupKey());
        assertEquals("rule:1:cart", matches.get(1).groupKey());
    }

    @Test
    void ignoresSingleMatchFiles() {
        List<CustomSubtabRule> rules = twoOverlappingStateRules();
        assertFalse(SubtabRuleRotation.hasMultipleMatches("notes.md", rules));
        assertFalse(SubtabRuleRotation.rotateMatchingRules("notes.md", rules));
    }

    private static @NotNull List<CustomSubtabRule> twoOverlappingStateRules() {
        CustomSubtabRule primary = SubtabRulesDefaults.createDefaults().stream()
                .filter(rule -> "State".equals(rule.name))
                .findFirst()
                .orElseThrow();
        CustomSubtabRule neighborState = primary.copy();
        neighborState.name = "State Nachbar";
        neighborState.searchNeighbors = true;
        return new ArrayList<>(List.of(primary, neighborState));
    }
}
