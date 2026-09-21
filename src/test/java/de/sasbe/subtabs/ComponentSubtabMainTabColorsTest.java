package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentSubtabMainTabColorsTest {
    @Test
    void usesStrongerTintForFocusedSelectedTabs() {
        Color group = new Color(255, 0, 0);
        Color background = new Color(40, 40, 40);

        Color focused = ComponentSubtabMainTabColors.blend(
                group,
                background,
                ComponentSubtabMainTabColors.FOCUSED_SELECTED_MIX
        );
        Color unfocused = ComponentSubtabMainTabColors.blend(
                group,
                background,
                ComponentSubtabMainTabColors.UNFOCUSED_ACTIVE_MIX
        );
        Color inactive = ComponentSubtabMainTabColors.blend(
                group,
                background,
                ComponentSubtabMainTabColors.INACTIVE_MIX
        );

        assertTrue(focused.getRed() > unfocused.getRed());
        assertTrue(unfocused.getRed() > inactive.getRed());
        assertEquals(focused.getAlpha(), 255);
    }
}
