package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ComponentTabTitlesTest {
    @Test
    void usesComponentNameWhileSubtabsAreVisible() {
        assertEquals("header", ComponentTabTitles.mainTabTitle(false, "header.component.scss"));
        assertEquals("user-card", ComponentTabTitles.mainTabTitle(false, "user-card.component.ts"));
    }

    @Test
    void usesDefaultFileNameWhenSubtabsAreCollapsed() {
        assertNull(ComponentTabTitles.mainTabTitle(true, "header.component.scss"));
        assertNull(ComponentTabTitles.mainTabTitle(true, "user-card.component.html"));
    }

    @Test
    void ignoresUnrelatedFiles() {
        assertNull(ComponentTabTitles.mainTabTitle(false, "misc.xml"));
    }

    @Test
    void usesNpmTitleForPackageConfigs() {
        assertEquals("npm", ComponentTabTitles.mainTabTitle(false, "package.json"));
        assertEquals("npm", ComponentTabTitles.mainTabTitle(false, ".npmrc"));
        assertNull(ComponentTabTitles.mainTabTitle(true, "package-lock.json"));
    }

    @Test
    void usesGroupTitlesForConfigsStateAndModels() {
        assertEquals("tsconfig", ComponentTabTitles.mainTabTitle(false, "tsconfig.app.json"));
        assertEquals("env", ComponentTabTitles.mainTabTitle(false, ".env.local"));
        assertEquals("cart", ComponentTabTitles.mainTabTitle(false, "cart.reducer.ts"));
        assertEquals("user", ComponentTabTitles.mainTabTitle(false, "user.dto.ts"));
    }
}
