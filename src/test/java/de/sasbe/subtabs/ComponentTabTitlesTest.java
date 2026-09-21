package de.sasbe.subtabs;

import com.intellij.testFramework.LightVirtualFile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ComponentTabTitlesTest {
    @Test
    void usesComponentNameWhileSubtabsAreVisible() {
        assertEquals("header-component", ComponentTabTitles.displayGroupedTitle("header.component.scss"));
        assertEquals("user-card-component", ComponentTabTitles.displayGroupedTitle("user-card.component.ts"));
    }

    @Test
    void usesGroupedNameForVirtualFileWithoutRelatedFilesLookup() {
        LightVirtualFile file = new LightVirtualFile("header.component.html");
        assertEquals("header-component", ComponentTabTitles.displayGroupedTitle(file));
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
