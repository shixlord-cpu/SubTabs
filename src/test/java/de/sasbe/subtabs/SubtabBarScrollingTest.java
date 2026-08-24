package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubtabBarScrollingTest {
    @Test
    void showsRightArrowWhenContentOverflowsToTheRight() {
        assertTrue(SubtabBarScrolling.canScrollRight(0, 100, 240));
        assertFalse(SubtabBarScrolling.canScrollLeft(0, 0));
    }

    @Test
    void showsLeftArrowAfterScrolling() {
        assertTrue(SubtabBarScrolling.canScrollLeft(40, 0));
        assertTrue(SubtabBarScrolling.canScrollRight(40, 100, 240));
    }

    @Test
    void hidesArrowsWhenEverythingFits() {
        assertFalse(SubtabBarScrolling.canScrollLeft(0, 0));
        assertFalse(SubtabBarScrolling.canScrollRight(0, 200, 200));
    }

    @Test
    void convertsMouseWheelRotationIntoHorizontalPixels() {
        assertEquals(16, SubtabBarScrolling.wheelPixelDelta(0d, 1, 16));
        assertEquals(-16, SubtabBarScrolling.wheelPixelDelta(0d, -1, 16));
        assertEquals(32, SubtabBarScrolling.wheelPixelDelta(2d, 0, 16));
    }

    @Test
    void clampsViewportScroll() {
        assertEquals(140, SubtabBarScrolling.nextViewX(0, 200, 100, 240));
        assertEquals(0, SubtabBarScrolling.nextViewX(20, -80, 100, 240));
    }
}
