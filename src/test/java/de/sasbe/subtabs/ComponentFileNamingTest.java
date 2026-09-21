package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentFileNamingTest {
    private static final String NPM_GROUP = "rule:0:npm";
    private static final String TSCONFIG_GROUP = "rule:1:tsconfig";
    private static final String ENV_GROUP = "rule:2:env";
    private static final String STATE_GROUP = "rule:3:cart";
    private static final String MODEL_GROUP = "rule:5:user";
    private static final String HTML_GROUP = "rule:6:catalog-page";
    private static final String COMPONENT_GROUP = "rule:7:user-card#user-card.component";
    private static final String FOLDER_GROUP = "rule:9:@folder";

    @Test
    void findsTheSameBaseForEveryComponentPart() {
        List<String> fileNames = List.of(
                "user-card.component.ts",
                "user-card.component.spec.ts",
                "user-card.component.test.ts",
                "user-card.component.html",
                "user-card.component.scss",
                "user-card.component.css"
        );

        for (String fileName : fileNames) {
            assertEquals(COMPONENT_GROUP, ComponentFileNaming.componentBaseName(fileName), fileName);
        }
    }

    @Test
    void fallsBackToFolderRuleForUnmatchedFiles() {
        assertEquals(FOLDER_GROUP, ComponentFileNaming.componentBaseName("README.md"));
        assertEquals(FOLDER_GROUP, ComponentFileNaming.componentBaseName("composer.json"));
    }

    @Test
    void ignoresUnmatchedFilesWhenFolderRuleIsDisabled() {
        CustomSubtabRule folderRule = SubtabRulesDefaults.folderRule();
        folderRule.enabled = false;
        List<CustomSubtabRule> rules = SubtabRulesDefaults.createDefaults();
        rules.set(9, folderRule);

        assertNull(CustomSubtabRuleMatcher.match("README.md", rules));
    }

    @Test
    void groupsNpmConfigFilesTogether() {
        List<String> fileNames = List.of(
                "package.json",
                "package-lock.json",
                "npm-shrinkwrap.json",
                "yarn.lock",
                "pnpm-lock.yaml",
                "bun.lock",
                ".npmrc",
                ".nvmrc",
                ".node-version"
        );

        for (String fileName : fileNames) {
            assertEquals(NPM_GROUP, ComponentFileNaming.componentBaseName(fileName), fileName);
        }
    }

    @Test
    void prefersNpmLockThenShrinkwrap() {
        List<SubtabCandidate> candidates = ComponentFileNaming.candidates(NPM_GROUP);

        assertEquals("package.json", candidates.get(0).slotId());
        assertEquals("package.json", candidates.get(0).fileName());
        assertEquals("lock", candidates.get(1).slotId());
        assertEquals("package-lock.json", candidates.get(1).fileName());
        assertEquals("lock", candidates.get(2).slotId());
        assertEquals("npm-shrinkwrap.json", candidates.get(2).fileName());
        assertEquals(".npmrc", candidates.get(7).slotId());
        assertEquals(".npmrc", candidates.get(7).fileName());
    }

    @Test
    void usesNpmAsTabTitleForPackageConfigs() {
        assertEquals("npm", ComponentFileNaming.displayName(NPM_GROUP));
    }

    @Test
    void groupsTypescriptConfigFilesTogether() {
        List<String> fileNames = List.of(
                "tsconfig.json",
                "tsconfig.app.json",
                "tsconfig.spec.json",
                "tsconfig.lib.json",
                "tsconfig.base.json"
        );

        for (String fileName : fileNames) {
            assertEquals(TSCONFIG_GROUP, ComponentFileNaming.componentBaseName(fileName), fileName);
        }
        assertEquals("tsconfig", ComponentFileNaming.displayName(TSCONFIG_GROUP));
    }

    @Test
    void groupsEnvFilesTogether() {
        List<String> fileNames = List.of(
                ".env",
                ".env.local",
                ".env.example",
                ".env.development",
                ".env.production",
                ".env.test"
        );

        for (String fileName : fileNames) {
            assertEquals(ENV_GROUP, ComponentFileNaming.componentBaseName(fileName), fileName);
        }
        assertEquals("env", ComponentFileNaming.displayName(ENV_GROUP));
    }

    @Test
    void displayNameFollowsGroupNameSegmentFromGroupKey() {
        assertEquals("reducer-state", ComponentFileNaming.displayName("rule:4:reducer#cart"));
        assertEquals("actions-state", ComponentFileNaming.displayName("rule:4:actions#catalog"));
    }

    @Test
    void groupsStateFilesByEntityAndSearchesNeighbors() {
        assertEquals("rule:3:cart", ComponentFileNaming.componentBaseName("cart.actions.ts"));
        assertEquals("rule:3:cart", ComponentFileNaming.componentBaseName("cart.reducer.ts"));
        assertEquals("rule:3:products", ComponentFileNaming.componentBaseName("products.selectors.ts"));
        assertEquals("cart-state", ComponentFileNaming.displayName(STATE_GROUP));
        assertTrue(ComponentFileNaming.searchNeighbors("rule:3:cart"));
        assertFalse(ComponentFileNaming.searchNeighbors(MODEL_GROUP));
    }

    @Test
    void groupsModelFilesByEntityInTheSameFolder() {
        assertEquals(MODEL_GROUP, ComponentFileNaming.componentBaseName("user.model.ts"));
        assertEquals(MODEL_GROUP, ComponentFileNaming.componentBaseName("user.dto.ts"));
        assertEquals(MODEL_GROUP, ComponentFileNaming.componentBaseName("user.entity.ts"));
        assertEquals("user", ComponentFileNaming.displayName(MODEL_GROUP));
    }

    @Test
    void doesNotTreatComponentOrStateFilesAsModels() {
        assertEquals(COMPONENT_GROUP, ComponentFileNaming.componentBaseName("user-card.component.ts"));
        assertEquals("rule:3:cart", ComponentFileNaming.componentBaseName("cart.state.ts"));
        assertEquals(FOLDER_GROUP, ComponentFileNaming.componentBaseName("notes.md"));
    }

    @Test
    void groupsPlainHtmlPagesByStemWithoutComponentSuffix() {
        assertEquals(HTML_GROUP, ComponentFileNaming.componentBaseName("catalog-page.html"));
        assertEquals(HTML_GROUP, ComponentFileNaming.componentBaseName("catalog-page.css"));
        assertEquals(HTML_GROUP, ComponentFileNaming.componentBaseName("catalog-page.js"));
        assertEquals("catalog-page", ComponentFileNaming.displayName(HTML_GROUP));
    }

    @Test
    void keepsComponentHtmlOnComponentRule() {
        assertEquals(COMPONENT_GROUP, ComponentFileNaming.componentBaseName("user-card.component.html"));
        assertEquals(COMPONENT_GROUP, ComponentFileNaming.componentBaseName("user-card.component.css"));
        assertEquals("user-card-component", ComponentFileNaming.displayName(COMPONENT_GROUP));
    }

    @Test
    void plainHtmlDoesNotInheritComponentNaming() {
        assertEquals("catalog-page", ComponentFileNaming.displayName(HTML_GROUP));
        assertEquals("landing-page", ComponentFileNaming.displayName("rule:6:landing-page"));
        assertEquals(
                "landing-page",
                CustomSubtabRuleMatcher.displayNameWithSuffix(
                        "landing-page",
                        SubtabRulesDefaults.createDefaults().get(6)
                )
        );
        assertEquals(
                "landing-page-component",
                CustomSubtabRuleMatcher.displayNameWithSuffix(
                        "landing-page",
                        SubtabRulesDefaults.createDefaults().get(7)
                )
        );
    }

    @Test
    void createsCandidatesInVisibleTabOrder() {
        List<SubtabCandidate> candidates = ComponentFileNaming.candidates("rule:7:app#app.component");

        assertEquals(".spec.ts", candidates.get(0).slotId());
        assertEquals("app.component.spec.ts", candidates.get(0).fileName());
        assertEquals(".test.ts", candidates.get(1).slotId());
        assertEquals(".ts", candidates.get(2).slotId());
        assertEquals("app.component.ts", candidates.get(2).fileName());
        assertEquals(".html", candidates.get(3).slotId());
        assertEquals("style", candidates.get(4).slotId());
    }

    @Test
    void folderGroupRulesCreateSubtabsButUserGroupsDoNot() {
        assertTrue(ComponentFileNaming.createsSubtabs(FOLDER_GROUP));
        assertFalse(ComponentFileNaming.createsSubtabs("rule:8:@nesting:package.json"));
        assertTrue(ComponentFileNaming.createsSubtabs(COMPONENT_GROUP));
        assertTrue(ComponentFileNaming.createsSubtabs(NPM_GROUP));
    }

    @Test
    void usesShortComponentNameForTabTitle() {
        assertEquals("user-card-component", ComponentFileNaming.displayName(COMPONENT_GROUP));
        assertEquals("app-component", ComponentFileNaming.displayName("rule:7:app#app.component"));
        assertEquals("landing-page", ComponentFileNaming.displayName("rule:6:landing-page"));
    }

    @Test
    void usesStateFolderRuleWhenItHasPriority() {
        CustomSubtabRule state = SubtabRulesDefaults.createDefaults().stream()
                .filter(rule -> "State Central".equals(rule.name))
                .findFirst()
                .orElseThrow();
        CustomSubtabRule stateFeature = SubtabRulesDefaults.stateFeatureRule();
        List<CustomSubtabRule> rules = List.of(stateFeature, state);

        CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.match("cart.actions.ts", rules);
        assertNotNull(match);
        assertEquals("rule:0:actions#cart", match.groupKey());
        assertFalse(match.searchNeighbors());
    }
}
