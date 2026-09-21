package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentRelatedFilesOrderTest {
    @Test
    void sameGroupIdentityUsesGroupNameSegmentsNotFileStem() {
        assertTrue(CustomSubtabRuleMatcher.sameGroupIdentity(
                "rule:3:products",
                "rule:3:products"
        ));
        assertFalse(CustomSubtabRuleMatcher.sameGroupIdentity(
                "rule:3:products",
                "rule:3:products-state"
        ));
    }

    @Test
    void sameGroupIdentityKeepsDisambiguatedComponentFamiliesSeparate() {
        assertTrue(CustomSubtabRuleMatcher.sameGroupIdentity(
                "rule:7:user-card#user-card.component",
                "rule:7:user-card#user-card.component"
        ));
        assertFalse(CustomSubtabRuleMatcher.sameGroupIdentity(
                "rule:7:user-card#user-card.component",
                "rule:7:user-card#user-card"
        ));
    }

    @Test
    void matchingPatternIndexPrefersLongestSuffix() {
        CustomSubtabRule component = SubtabRulesDefaults.createDefaults().stream()
                .filter(rule -> "Komponente".equals(rule.name))
                .findFirst()
                .orElseThrow();

        assertTrue(CustomSubtabRuleMatcher.matchingPatternIndex(component, "app.component.ts") >= 0);
        assertTrue(CustomSubtabRuleMatcher.matchingPatternIndex(component, "app.component.spec.ts") >= 0);
    }
}
