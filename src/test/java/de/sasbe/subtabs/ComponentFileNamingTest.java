package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentFileNamingTest {
    private static final String NPM_GROUP = "rule:0:@files";
    private static final String TSCONFIG_GROUP = "rule:1:@files";
    private static final String ENV_GROUP = "rule:2:@files";
    private static final String STATE_GROUP = "rule:3:cart";
    private static final String MODEL_GROUP = "rule:4:user";
    private static final String COMPONENT_GROUP = "rule:5:user-card.component";

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
    void ignoresUnrelatedFiles() {
        assertNull(ComponentFileNaming.componentBaseName("README.md"));
        assertNull(ComponentFileNaming.componentBaseName(".ts"));
        assertNull(ComponentFileNaming.componentBaseName("composer.json"));
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
    void groupsStateFilesByEntityAndSearchesNeighbors() {
        assertEquals("rule:3:cart", ComponentFileNaming.componentBaseName("cart.actions.ts"));
        assertEquals("rule:3:cart", ComponentFileNaming.componentBaseName("cart.reducer.ts"));
        assertEquals("rule:3:products", ComponentFileNaming.componentBaseName("products.selectors.ts"));
        assertEquals("cart", ComponentFileNaming.displayName(STATE_GROUP));
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
        assertNull(ComponentFileNaming.componentBaseName("notes.md"));
    }

    @Test
    void createsCandidatesInVisibleTabOrder() {
        List<SubtabCandidate> candidates = ComponentFileNaming.candidates("rule:5:app.component");

        assertEquals(".spec.ts", candidates.get(0).slotId());
        assertEquals("app.component.spec.ts", candidates.get(0).fileName());
        assertEquals(".test.ts", candidates.get(1).slotId());
        assertEquals(".ts", candidates.get(2).slotId());
        assertEquals("app.component.ts", candidates.get(2).fileName());
        assertEquals(".html", candidates.get(3).slotId());
        assertEquals("style", candidates.get(4).slotId());
    }

    @Test
    void usesShortComponentNameForTabTitle() {
        assertEquals("user-card", ComponentFileNaming.displayName(COMPONENT_GROUP));
        assertEquals("app", ComponentFileNaming.displayName("rule:5:app.component"));
        assertEquals("header", ComponentFileNaming.displayName("rule:5:header"));
    }
}
