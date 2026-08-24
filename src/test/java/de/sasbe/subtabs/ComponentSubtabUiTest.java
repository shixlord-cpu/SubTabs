package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentSubtabUiTest {
    @Test
    void usesShortestHeightAtTwentyFivePercent() {
        assertEquals(14, ComponentSubtabUi.tabHeightUnscaled(25));
    }

    @Test
    void usesTallestHeightAtOneHundredPercent() {
        assertEquals(32, ComponentSubtabUi.tabHeightUnscaled(100));
    }

    @Test
    void interpolatesHeightBetweenMinAndMax() {
        assertEquals(26, ComponentSubtabUi.tabHeightUnscaled(75));
        assertTrue(ComponentSubtabUi.tabHeightUnscaled(25) < ComponentSubtabUi.tabHeightUnscaled(75));
        assertTrue(ComponentSubtabUi.tabHeightUnscaled(75) < ComponentSubtabUi.tabHeightUnscaled(100));
    }
}
