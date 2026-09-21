package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SidetabOverlayStackLayoutTest {
    @Test
    void stackHeightGrowsLinearlyWithSectionCount() {
        int one = SidetabBarPanel.overlayStackHeight(1);
        int four = SidetabBarPanel.overlayStackHeight(4);
        int eight = SidetabBarPanel.overlayStackHeight(8);
        assertTrue(four > one);
        assertTrue(eight > four);
        assertTrue(eight < com.intellij.util.ui.JBUI.scale(160), "Eight overlay bars must stay compact");
    }
}
