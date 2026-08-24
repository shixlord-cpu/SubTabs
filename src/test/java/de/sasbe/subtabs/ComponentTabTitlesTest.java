package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ComponentTabTitlesTest {
    @Test
    void usesComponentNameWhileSubtabsAreVisible() {
        assertEquals("header-components", ComponentTabTitles.displayGroupedTitle("header.component.scss"));
        assertEquals("user-card-components", ComponentTabTitles.displayGroupedTitle("user-card.component.ts"));
    }

    @Test
    void usesFolderRuleTitleForUnmatchedFiles() {
        assertEquals("Ordner", ComponentTabTitles.displayGroupedTitle("misc.xml"));
    }

    @Test
    void usesNpmTitleForPackageConfigs() {
        assertEquals("npm", ComponentTabTitles.displayGroupedTitle("package.json"));
        assertEquals("npm", ComponentTabTitles.displayGroupedTitle(".npmrc"));
    }

    @Test
    void usesGroupTitlesForConfigsStateAndModels() {
        assertEquals("tsconfig", ComponentTabTitles.displayGroupedTitle("tsconfig.app.json"));
        assertEquals("env", ComponentTabTitles.displayGroupedTitle(".env.local"));
        assertEquals("cart-state", ComponentTabTitles.displayGroupedTitle("cart.reducer.ts"));
        assertEquals("user", ComponentTabTitles.displayGroupedTitle("user.dto.ts"));
    }
}
