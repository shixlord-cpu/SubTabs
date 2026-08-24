package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubtabsSettingsTest {
    @Test
    void doesNotScrollProjectViewOnHoverByDefault() {
        assertFalse(new SubtabsSettings.State().scrollProjectViewOnSubtabHover);
    }

    @Test
    void showsSubtabsByDefault() {
        assertTrue(new SubtabsSettings.State().subtabsActive);
    }

    @Test
    void showsCollapseButtonByDefault() {
        assertTrue(new SubtabsSettings.State().showCollapseButton);
    }

    @Test
    void groupsRelatedFilesInProjectViewByDefault() {
        assertTrue(new SubtabsSettings.State().groupRelatedFilesInProjectView);
    }

    @Test
    void fitsTabsToEditorWidthByDefault() {
        assertTrue(new SubtabsSettings.State().fitTabsToEditorWidth);
    }

    @Test
    void usesCompactBarHeightByDefault() {
        assertEquals(75, new SubtabsSettings.State().barHeightPercent);
    }

    @Test
    void usesRelativeTextSizeByDefault() {
        assertEquals(75, new SubtabsSettings.State().textSizePercent);
    }

    @Test
    void usesScrollbarOverflowByDefault() {
        assertEquals("SCROLLBAR", new SubtabsSettings.State().overflowMode);
    }

    @Test
    void shipsWithBuiltInRules() {
        assertEquals(6, new SubtabsSettings.State().rules.size());
        assertEquals("npm", new SubtabsSettings.State().rules.get(0).name);
        assertEquals("tsconfig", new SubtabsSettings.State().rules.get(1).name);
        assertEquals("Komponente", new SubtabsSettings.State().rules.get(5).name);
    }
}
