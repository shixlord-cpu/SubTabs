package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ComponentSubtabReorderLayoutTest extends junit.framework.TestCase {
    private static final int HGAP = 4;

    public void testDragMiddleTabBeforeLastInsertsGapBetweenNeighbors() {
        ComponentSubtabReorderLayout.Result result = ComponentSubtabReorderLayout.layout(
                List.of(100, 100, 100),
                1,
                2,
                100,
                HGAP,
                0
        );

        assertTrue(result.hasGap());
        assertEquals(104, result.gapX());
        assertEquals(0, placementX(result, 0));
        assertEquals(208, placementX(result, 2));
        assertEquals(-1, placementX(result, 1));
    }

    public void testDragMiddleTabToEndReservesGapAfterLastVisibleTab() {
        ComponentSubtabReorderLayout.Result result = ComponentSubtabReorderLayout.layout(
                List.of(100, 100, 100),
                1,
                3,
                100,
                HGAP,
                0
        );

        assertTrue(result.hasGap());
        assertEquals(208, result.gapX());
        assertEquals(0, placementX(result, 0));
        assertEquals(104, placementX(result, 2));
    }

    public void testDragLastTabToStartOpensGapAtStart() {
        ComponentSubtabReorderLayout.Result result = ComponentSubtabReorderLayout.layout(
                List.of(100, 100, 100),
                2,
                0,
                100,
                HGAP,
                0
        );

        assertTrue(result.hasGap());
        assertEquals(0, result.gapX());
        assertEquals(104, placementX(result, 0));
        assertEquals(208, placementX(result, 1));
        assertEquals(-1, placementX(result, 2));
    }

    public void testDropIndexForPointerUsesNaturalWidthsWithoutExistingGap() {
        assertEquals(0, ComponentSubtabReorderLayout.dropIndexForPointer(10, List.of(100, 100, 100), -1, HGAP, 0));
        assertEquals(1, ComponentSubtabReorderLayout.dropIndexForPointer(120, List.of(100, 100, 100), -1, HGAP, 0));
        assertEquals(2, ComponentSubtabReorderLayout.dropIndexForPointer(220, List.of(100, 100, 100), -1, HGAP, 0));
        assertEquals(3, ComponentSubtabReorderLayout.dropIndexForPointer(400, List.of(100, 100, 100), -1, HGAP, 0));
    }

    public void testDropIndexForPointerIgnoresDraggedTabSlotWhileDragging() {
        List<Integer> widths = List.of(100, 100, 100);
        assertEquals(0, ComponentSubtabReorderLayout.dropIndexForPointer(10, widths, 1, HGAP, 0));
        assertEquals(2, ComponentSubtabReorderLayout.dropIndexForPointer(120, widths, 1, HGAP, 0));
        assertEquals(3, ComponentSubtabReorderLayout.dropIndexForPointer(220, widths, 1, HGAP, 0));
        assertEquals(3, ComponentSubtabReorderLayout.dropIndexForPointer(400, widths, 1, HGAP, 0));
    }

    public void testAdjacentDropTargetsAreTreatedAsNoOp() {
        assertTrue(ComponentSubtabReorderLayout.isNoOpDrop(1, 1));
        assertTrue(ComponentSubtabReorderLayout.isNoOpDrop(1, 2));
        assertFalse(ComponentSubtabReorderLayout.isNoOpDrop(1, 3));
    }

    public void testDragFirstTabBetweenSecondAndThirdIsNotNoOp() {
        assertTrue(ComponentSubtabReorderLayout.isNoOpDrop(0, 0));
        assertTrue(ComponentSubtabReorderLayout.isNoOpDrop(0, 1));
        assertFalse(ComponentSubtabReorderLayout.isNoOpDrop(0, 2));
        assertFalse(ComponentSubtabReorderLayout.isNoOpDrop(0, 3));
    }

    public void testDropIndexForCurrentBoundsUsesShiftedTabPositions() {
        List<ComponentSubtabReorderLayout.BoundsSlot> slots = List.of(
                new ComponentSubtabReorderLayout.BoundsSlot(1, 0, 100),
                new ComponentSubtabReorderLayout.BoundsSlot(2, 208, 100)
        );
        assertEquals(1, ComponentSubtabReorderLayout.dropIndexForCurrentBounds(25, slots, 0, 3, -1, 0));
        assertEquals(2, ComponentSubtabReorderLayout.dropIndexForCurrentBounds(150, slots, 0, 3, 104, 100));
        assertEquals(3, ComponentSubtabReorderLayout.dropIndexForCurrentBounds(320, slots, 0, 3, 104, 100));
        assertEquals(2, ComponentSubtabReorderLayout.dropIndexForCurrentBounds(120, slots, 0, 3, 104, 100));
    }

    public void testDragFirstTabBetweenSecondAndThirdOpensGapAtDropIndexTwo() {
        ComponentSubtabReorderLayout.Result result = ComponentSubtabReorderLayout.layout(
                List.of(100, 100, 100, 100),
                0,
                2,
                100,
                HGAP,
                0
        );

        assertTrue(result.hasGap());
        assertEquals(104, result.gapX());
        assertEquals(0, placementX(result, 1));
        assertEquals(208, placementX(result, 2));
        assertEquals(312, placementX(result, 3));
        assertEquals(-1, placementX(result, 0));
    }

    private static int placementX(
            @NotNull ComponentSubtabReorderLayout.Result result,
            int tabIndex
    ) {
        for (ComponentSubtabReorderLayout.TabPlacement placement : result.placements()) {
            if (placement.tabIndex() == tabIndex) {
                return placement.x();
            }
        }
        return -1;
    }
}
