package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StateOrdnerRuleTest {
    @Test
    void defaultStateFeatureRuleUsesSameFolderSearchOnly() {
        CustomSubtabRule stateFeature = SubtabRulesDefaults.stateFeatureRule();
        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.match(
                "cart.actions.ts",
                List.of(stateFeature)
        );

        assertNotNull(match);
        assertEquals("rule:0:actions#cart", match.groupKey());
        assertEquals("actions", match.displayName());
        assertFalse(match.searchNeighbors());
    }

    @Test
    void defaultRulesIncludeStateFeatureAfterStateCentral() {
        List<CustomSubtabRule> rules = SubtabRulesDefaults.createDefaults();
        assertEquals("State Central", rules.get(3).name);
        assertEquals("State Feature", rules.get(4).name);
        assertTrue(rules.get(3).searchNeighbors);
        assertFalse(rules.get(4).searchNeighbors);
    }

    @Test
    void centralAndFeatureStateRulesBothMatchSameFile() {
        List<CustomSubtabRule> rules = SubtabRulesDefaults.createDefaults();
        List<CustomSubtabRuleMatcher.Match> matches = CustomSubtabRuleMatcher.matchAll(
                "cart.actions.ts",
                rules
        );

        assertTrue(matches.size() >= 2);
        assertEquals("rule:3:cart", matches.get(0).groupKey());
        assertEquals("rule:4:actions#cart", matches.get(1).groupKey());
        assertTrue(SubtabRuleRotation.hasMultipleMatches("cart.actions.ts", rules));
    }

    @Test
    void resetsStaleRulesVersionToFactoryDefaults() {
        SubtabsSettings.State state = new SubtabsSettings.State();
        state.rulesVersion = 14;
        CustomSubtabRule broken = state.rules.get(3).copy();
        broken.type = CustomSubtabRule.Type.FILES;
        broken.patterns = ".broken.ts";
        state.rules.set(3, broken);

        SubtabsSettings settings = new SubtabsSettings();
        settings.loadState(state);

        CustomSubtabRule loadedCentral = settings.getRules().stream()
                .filter(rule -> "State Central".equals(rule.name))
                .findFirst()
                .orElseThrow();
        assertNull(loadedCentral.type);
        assertTrue(loadedCentral.patterns.contains(".actions.ts"));
        assertEquals(17, settings.getState().rulesVersion);
        assertEquals(
                "rule:3:cart",
                CustomSubtabRuleMatcher.match("cart.actions.ts", settings.getRules()).groupKey()
        );
        assertTrue(SubtabRuleRotation.hasMultipleMatches("cart.actions.ts", settings.getRules()));
    }

    @Test
    void resetsLegacyRuleNamesWithoutRestart() {
        SubtabsSettings.State state = new SubtabsSettings.State();
        state.rulesVersion = 16;
        state.rules.get(4).name = "State Typ";
        state.rules.get(4).searchNeighbors = true;

        SubtabsSettings settings = new SubtabsSettings();
        settings.loadState(state);
        settings.getRules();

        CustomSubtabRule stateFeature = settings.getRules().stream()
                .filter(rule -> "State Feature".equals(rule.name))
                .findFirst()
                .orElseThrow();
        assertFalse(stateFeature.searchNeighbors);
        assertEquals("state", stateFeature.groupSuffix);
        assertEquals("2", stateFeature.groupNameSegments);
    }
}
