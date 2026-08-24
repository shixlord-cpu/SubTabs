package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomSubtabRuleMatcherTest {
    @Test
    void matchesStemRuleBeforeBuiltIns() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "Stories";
        rule.type = CustomSubtabRule.Type.STEM;
        rule.patterns = ".stories.ts, .stories.tsx";
        rule.labels = "Stories, Stories";

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
        rule.type = CustomSubtabRule.Type.FILES;
        rule.patterns = "Dockerfile, compose.yaml";
        rule.labels = "Dockerfile, Compose";

        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.match(
                "compose.yaml",
                List.of(rule)
        );

        assertNotNull(match);
        assertEquals("rule:0:@files", match.groupKey());
        assertEquals("Docker", match.displayName());
        assertEquals(2, match.candidates().size());
    }

    @Test
    void resolvesStoredGroupKey() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.type = CustomSubtabRule.Type.STEM;
        rule.patterns = ".dto.ts, .model.ts";
        rule.labels = "DTO, Model";

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
    void stripsComponentSuffixForDisplayName() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.type = CustomSubtabRule.Type.STEM;
        rule.patterns = ".ts";
        rule.stripComponentSuffix = true;

        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.resolveGroup(
                "rule:0:user-card.component",
                List.of(rule)
        );

        assertNotNull(match);
        assertEquals("user-card", match.displayName());
    }

    @Test
    void appendsProjectViewSuffixForStemGroups() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.type = CustomSubtabRule.Type.STEM;
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
        rule.type = CustomSubtabRule.Type.STEM;
        rule.patterns = ".ts, .html";
        rule.stripComponentSuffix = true;
        rule.groupSuffix = "components";

        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.resolveGroup(
                "rule:0:products.component",
                List.of(rule)
        );

        assertNotNull(match);
        assertEquals("products", match.displayName());
        assertEquals(
                "products-components",
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
        rule.type = CustomSubtabRule.Type.FILES;
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
    void keepsExactDotfileListsAsFileRules() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "env";
        rule.type = CustomSubtabRule.Type.FILES;
        rule.patterns = ".env, .env.local";

        assertEquals("rule:0:@files", CustomSubtabRuleMatcher.match(".env", List.of(rule)).groupKey());
        assertNull(CustomSubtabRuleMatcher.match(".env.sample", List.of(rule)));
    }

    @Test
    void skipsDisabledRules() {
        CustomSubtabRule disabled = new CustomSubtabRule();
        disabled.enabled = false;
        disabled.type = CustomSubtabRule.Type.STEM;
        disabled.patterns = ".ts";

        assertNull(CustomSubtabRuleMatcher.match("user.ts", List.of(disabled)));
    }

    @Test
    void matchesCustomGroupDefinitions() {
        CustomSubtabRule rule = SubtabRulesDefaults.customGroupsRule();
        CustomSubtabGroupDefinition docker = new CustomSubtabGroupDefinition();
        docker.name = "Docker";
        docker.patterns = "Dockerfile, compose.yaml";
        docker.labels = "Dockerfile, Compose";

        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.match(
                "compose.yaml",
                List.of(rule),
                List.of(docker)
        );

        assertNotNull(match);
        assertEquals("rule:0:custom:Docker", match.groupKey());
        assertEquals("Docker", match.displayName());
        assertEquals(2, match.candidates().size());
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

    @Test
    void ignoresCustomGroupsWhenSpecialRuleIsDisabled() {
        CustomSubtabRule rule = SubtabRulesDefaults.customGroupsRule();
        rule.enabled = false;
        CustomSubtabGroupDefinition docker = new CustomSubtabGroupDefinition();
        docker.name = "Docker";
        docker.patterns = "Dockerfile";

        assertNull(CustomSubtabRuleMatcher.match("Dockerfile", List.of(rule), List.of(docker)));
    }
}
