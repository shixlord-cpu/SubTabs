package de.sasbe.subtabs;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComponentSubtabModifiedUiTest {
    @Test
    void foregroundUsesRegularColorsWhenNotModified() {
        assertEquals(UIUtil.getLabelForeground(), ComponentSubtabModifiedUi.foreground(false, false));
        assertEquals(UIUtil.getInactiveTextColor(), ComponentSubtabModifiedUi.foreground(false, true));
    }

    @Test
    void foregroundUsesBlueWhenModified() {
        Color active = ComponentSubtabModifiedUi.foreground(true, false);
        Color inactive = ComponentSubtabModifiedUi.foreground(true, true);

        assertEquals(new JBColor(new Color(0x0042AA), new Color(0x589DF6)).getRGB(), active.getRGB());
        assertEquals(new JBColor(new Color(0x728AAB), new Color(0x6A8FB8)).getRGB(), inactive.getRGB());
    }
}
