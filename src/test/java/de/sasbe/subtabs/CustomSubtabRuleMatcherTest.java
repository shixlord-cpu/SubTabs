package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomSubtabRuleMatcherTest {
    @Test
    void matchAllCollectsEveryEnabledRuleMatch() {
        CustomSubtabRule state = SubtabRulesDefaults.createDefaults().stream()
                .filter(rule -> "State Central".equals(rule.name))
                .findFirst()
                .orElseThrow();
        CustomSubtabRule neighbor = state.copy();
        neighbor.name = "State Feature";
        neighbor.searchNeighbors = true;
        List<CustomSubtabRule> rules = List.of(state, neighbor);

        List<CustomSubtabRuleMatcher.Match> matches = CustomSubtabRuleMatcher.matchAll("cart.actions.ts", rules);
        assertEquals(2, matches.size());
        assertEquals("rule:0:cart", matches.get(0).groupKey());
        assertEquals("rule:1:cart", matches.get(1).groupKey());
    }

    @Test
    void matchesSuffixPatternFromShape() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "Stories";
        rule.patterns = ".stories.ts, .stories.tsx";
        rule.nameSegments = "2, 2";

        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.match(
                "button.stories.ts",
                List.of(rule)
        );

        assertNotNull(match);
        assertEquals("rule:0:button", match.groupKey());
        assertEquals("button", match.displayName());
        assertEquals(2, match.candidates().size());
        assertEquals("button.stories.ts", match.candidates().get(0).fileName());
        assertEquals("button.stories.tsx", match.candidates().get(1).fileName());
    }

    @Test
    void matchesExactFileRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "Docker";
        rule.patterns = "Dockerfile, compose.yaml";
        rule.nameSegments = "1, 1";

        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.match(
                "compose.yaml",
                List.of(rule)
        );

        assertNotNull(match);
        assertEquals("rule:0:Docker", match.groupKey());
        assertEquals("Docker", match.displayName());
        assertEquals(2, match.candidates().size());
    }

    @Test
    void resolvesStoredGroupKey() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.patterns = ".dto.ts, .model.ts";
        rule.nameSegments = "2, 2";

        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.resolveGroup(
                "rule:0:user",
                List.of(rule)
        );

        assertNotNull(match);
        assertEquals("user.dto.ts", match.candidates().get(0).fileName());
        assertEquals("user.model.ts", match.candidates().get(1).fileName());
        assertTrue(CustomSubtabRuleMatcher.isRuleGroupKey("rule:0:user"));
    }

    @Test
    void usesGroupNameSegmentsForGroupKeyAndDisplayName() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.patterns = ".ts, .html";
        rule.nameSegments = "2, -1";
        rule.groupNameSegments = "1";

        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.match(
                "user-card.component.ts",
                List.of(rule)
        );

        assertNotNull(match);
        assertEquals("rule:0:user-card#user-card.component", match.groupKey());
        assertEquals("user-card", match.displayName());
    }

    @Test
    void resolveGroupUsesGroupNameFromKeyNotFirstSuffix() {
        CustomSubtabRule rule = SubtabRulesDefaults.stateFeatureRule();

        CustomSubtabRuleMatcher.Match reducer = CustomSubtabRuleMatcher.resolveGroup(
                "rule:0:reducer#cart",
                List.of(rule)
        );
        CustomSubtabRuleMatcher.Match actions = CustomSubtabRuleMatcher.resolveGroup(
                "rule:0:actions#cart",
                List.of(rule)
        );

        assertNotNull(reducer);
        assertNotNull(actions);
        assertEquals("reducer", reducer.displayName());
        assertEquals("actions", actions.displayName());
    }

    @Test
    void appendsProjectViewSuffixForGroupName() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.patterns = ".actions.ts, .reducer.ts";
        rule.groupSuffix = "state";

        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.resolveGroup(
                "rule:0:products",
                List.of(rule)
        );

        assertNotNull(match);
        assertEquals("products", match.displayName());
        assertEquals(
                "products-state",
                CustomSubtabRuleMatcher.displayNameWithSuffix(match.displayName(), rule)
        );
    }

    @Test
    void appendsSuffixForComponentGroups() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.patterns = ".ts, .html";
        rule.groupNameSegments = "1";
        rule.groupSuffix = "component";

        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.resolveGroup(
                "rule:0:products#products.component",
                List.of(rule)
        );

        assertNotNull(match);
        assertEquals("products", match.displayName());
        assertEquals(
                "products-component",
                CustomSubtabRuleMatcher.displayNameWithSuffix(match.displayName(), rule)
        );
    }

    @Test
    void ignoresEmptyPatterns() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.patterns = "  ,  ";

        assertNull(CustomSubtabRuleMatcher.match("button.stories.ts", List.of(rule)));
    }

    @Test
    void groupsAllMatchingExtensionsInFolderRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "xml";
        rule.patterns = ".xml";

        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.match(
                "workspace.xml",
                List.of(rule)
        );

        assertNotNull(match);
        assertEquals("rule:0:@ext:.xml", match.groupKey());
        assertEquals("xml", match.displayName());
        assertNull(CustomSubtabRuleMatcher.match("README.md", List.of(rule)));
    }

    @Test
    void groupsExactDotfileListsByRuleName() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "env";
        rule.patterns = ".env, .env.local";

        assertEquals("rule:0:env", CustomSubtabRuleMatcher.match(".env", List.of(rule)).groupKey());
        assertEquals("rule:0:env", CustomSubtabRuleMatcher.match(".env.local", List.of(rule)).groupKey());
        assertNull(CustomSubtabRuleMatcher.match(".env.sample", List.of(rule)));
    }

    @Test
    void skipsDisabledRules() {
        CustomSubtabRule disabled = new CustomSubtabRule();
        disabled.enabled = false;
        disabled.patterns = ".ts";

        assertNull(CustomSubtabRuleMatcher.match("user.ts", List.of(disabled)));
    }

    @Test
    void skipsSuffixRuleWhenPrefixEndsWithExcludedSuffix() {
        CustomSubtabRule htmlRule = SubtabRulesDefaults.htmlRule();
        CustomSubtabRule componentRule = SubtabRulesDefaults.createDefaults().stream()
                .filter(rule -> "Komponente".equals(rule.name))
                .findFirst()
                .orElseThrow();
        List<CustomSubtabRule> rules = List.of(htmlRule, componentRule);

        assertNull(CustomSubtabRuleMatcher.match("header.component.html", List.of(htmlRule)));
        assertEquals(
                "rule:1:header#header.component",
                CustomSubtabRuleMatcher.match("header.component.html", rules).groupKey()
        );
        assertEquals(
                "rule:0:catalog-page",
                CustomSubtabRuleMatcher.match("catalog-page.html", rules).groupKey()
        );
    }

    @Test
    void singleTabSegmentAppliesToEveryPattern() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.patterns = ".actions.ts, .reducer.ts, .effects.ts";
        rule.nameSegments = "1";

        assertEquals("cart", CustomSubtabRuleMatcher.resolveTabName(rule, "cart.actions.ts"));
        assertEquals("cart", CustomSubtabRuleMatcher.resolveTabName(rule, "cart.reducer.ts"));
        assertEquals("cart", CustomSubtabRuleMatcher.resolveTabName(rule, "cart.effects.ts"));
    }

    @Test
    void resolvesTabNamesFromNameSegments() {
        CustomSubtabRule rule = SubtabRulesDefaults.createDefaults().stream()
                .filter(stored -> "State Central".equals(stored.name))
                .findFirst()
                .orElseThrow();

        assertEquals("actions", CustomSubtabRuleMatcher.resolveTabName(rule, "cart.actions.ts"));
        assertEquals("reducer", CustomSubtabRuleMatcher.resolveTabName(rule, "cart.reducer.ts"));
    }

    @Test
    void resolvesGroupNamesFromGroupNameSegments() {
        CustomSubtabRule rule = SubtabRulesDefaults.createDefaults().stream()
                .filter(stored -> "State Central".equals(stored.name))
                .findFirst()
                .orElseThrow();

        assertEquals("cart", CustomSubtabRuleMatcher.resolveGroupName(rule, "cart"));
        assertEquals("products", CustomSubtabRuleMatcher.resolveGroupName(rule, "products"));
    }

    @Test
    void groupKeyFollowsGroupNameSegmentsNotFileStem() {
        CustomSubtabRule rule = SubtabRulesDefaults.createDefaults().stream()
                .filter(stored -> "State Central".equals(stored.name))
                .findFirst()
                .orElseThrow()
                .copy();
        rule.groupNameSegments = "2";

        assertEquals(
                "rule:0:actions#cart",
                CustomSubtabRuleMatcher.match("cart.actions.ts", List.of(rule)).groupKey()
        );
        assertEquals(
                "rule:0:reducer#cart",
                CustomSubtabRuleMatcher.match("cart.reducer.ts", List.of(rule)).groupKey()
        );
        assertNotEquals(
                CustomSubtabRuleMatcher.match("cart.actions.ts", List.of(rule)).groupKey(),
                CustomSubtabRuleMatcher.match("cart.reducer.ts", List.of(rule)).groupKey()
        );
    }

    @Test
    void detectsSuffixPatternsFromShape() {
        assertTrue(CustomSubtabRuleMatcher.usesSuffixMatching(".actions.ts"));
        assertFalse(CustomSubtabRuleMatcher.usesSuffixMatching("package.json"));
        assertFalse(CustomSubtabRuleMatcher.usesSuffixMatching(".env"));
    }

    @Test
    void excludesMatchingFilesFromRule() {
        CustomSubtabRule rule = SubtabRulesDefaults.createDefaults().stream()
                .filter(stored -> "Komponente".equals(stored.name))
                .findFirst()
                .orElseThrow();

        assertNull(CustomSubtabRuleMatcher.match("cart.actions.ts", List.of(rule)));
        assertNull(CustomSubtabRuleMatcher.match("cart.reducer.ts", List.of(rule)));
        assertNotNull(CustomSubtabRuleMatcher.match("header.component.ts", List.of(rule)));
        assertEquals(
                "rule:0:header#header.component",
                CustomSubtabRuleMatcher.match("header.component.ts", List.of(rule)).groupKey()
        );
    }

    @Test
    void excludePatternsAllowRuleMatchWhenEmpty() {
        CustomSubtabRule rule = SubtabRulesDefaults.createDefaults().stream()
                .filter(stored -> "Komponente".equals(stored.name))
                .findFirst()
                .orElseThrow()
                .copy();
        rule.excludePatterns = "";

        assertNotNull(CustomSubtabRuleMatcher.match("cart.actions.ts", List.of(rule)));
    }

    @Test
    void matchesFolderRuleForAnyFile() {
        CustomSubtabRule rule = SubtabRulesDefaults.folderRule();

        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.match(
                "README.md",
                List.of(rule)
        );

        assertNotNull(match);
        assertEquals("rule:0:@folder", match.groupKey());
        assertTrue(CustomSubtabRuleMatcher.isFolderGroupKey(match.groupKey()));
    }
}
