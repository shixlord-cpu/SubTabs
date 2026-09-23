package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes visual positions while reordering subtabs inside the bar.
 */
final class ComponentSubtabReorderLayout {
    record TabPlacement(int tabIndex, int x, int width) {
    }

    record Result(
            @NotNull List<TabPlacement> placements,
            int gapX,
            int gapWidth,
            int totalWidth
    ) {
        boolean hasGap() {
            return gapX >= 0 && gapWidth > 0;
        }
    }

    private ComponentSubtabReorderLayout() {
    }

    static @NotNull Result layout(
            @NotNull List<Integer> tabWidths,
            int draggedIndex,
            int dropIndex,
            int gapWidth,
            int hgap,
            int insetLeft
    ) {
        if (tabWidths.isEmpty()) {
            return new Result(List.of(), -1, 0, insetLeft);
        }

        int safeDraggedIndex = clamp(draggedIndex, 0, tabWidths.size() - 1);
        int safeGapWidth = gapWidth > 0 ? gapWidth : tabWidths.get(safeDraggedIndex);
        int safeDropIndex = clamp(dropIndex, 0, tabWidths.size());

        int x = insetLeft;
        int index = 0;
        int gapX = -1;
        List<TabPlacement> placements = new ArrayList<>(tabWidths.size());

        for (int tabIndex = 0; tabIndex < tabWidths.size(); tabIndex++) {
            if (index == safeDropIndex) {
                gapX = x;
                x += safeGapWidth + hgap;
            }
            if (tabIndex == safeDraggedIndex) {
                index++;
                continue;
            }
            int width = tabWidths.get(tabIndex);
            placements.add(new TabPlacement(tabIndex, x, width));
            x += width + hgap;
            index++;
        }
        if (index == safeDropIndex) {
            gapX = x;
            x += safeGapWidth;
        }

        int maxRight = insetLeft;
        for (TabPlacement placement : placements) {
            maxRight = Math.max(maxRight, placement.x() + placement.width());
        }
        if (gapX >= 0) {
            maxRight = Math.max(maxRight, gapX + safeGapWidth);
        }

        return new Result(List.copyOf(placements), gapX, safeGapWidth, maxRight);
    }

    static int dropIndexForPointer(
            int pointerX,
            @NotNull List<Integer> tabWidths,
            int draggedIndex,
            int hgap,
            int insetLeft
    ) {
        if (tabWidths.isEmpty()) {
            return 0;
        }

        if (draggedIndex < 0 || draggedIndex >= tabWidths.size()) {
            int x = insetLeft;
            for (int index = 0; index < tabWidths.size(); index++) {
                int width = tabWidths.get(index);
                if (pointerX < x + width / 2) {
                    return index;
                }
                x += width + hgap;
            }
            return tabWidths.size();
        }

        int x = insetLeft;
        int visibleIndex = 0;
        for (int tabIndex = 0; tabIndex < tabWidths.size(); tabIndex++) {
            if (tabIndex == draggedIndex) {
                continue;
            }
            int width = tabWidths.get(tabIndex);
            if (pointerX < x + width / 2) {
                return layoutDropIndex(visibleIndex, draggedIndex);
            }
            x += width + hgap;
            visibleIndex++;
        }
        return tabWidths.size();
    }

    static int layoutDropIndex(int visibleInsertIndex, int draggedIndex) {
        return visibleInsertIndex + (visibleInsertIndex >= draggedIndex ? 1 : 0);
    }

    /**
     * Resolves the layout drop index from the current on-screen tab bounds while dragging.
     */
    static int dropIndexForCurrentBounds(
            int pointerX,
            @NotNull List<BoundsSlot> visibleSlots,
            int draggedIndex,
            int tabCount,
            int gapX,
            int gapWidth
    ) {
        if (gapX >= 0 && gapWidth > 0 && pointerX >= gapX && pointerX < gapX + gapWidth) {
            return dropIndexForGapPosition(gapX, visibleSlots, draggedIndex, tabCount, gapWidth);
        }

        int visibleIndex = 0;
        for (BoundsSlot slot : visibleSlots) {
            if (slot.tabIndex() == draggedIndex) {
                continue;
            }
            if (pointerX < slot.x() + slot.width() / 2) {
                return layoutDropIndex(visibleIndex, draggedIndex);
            }
            visibleIndex++;
        }
        return tabCount;
    }

    private static int dropIndexForGapPosition(
            int gapX,
            @NotNull List<BoundsSlot> visibleSlots,
            int draggedIndex,
            int tabCount,
            int gapWidth
    ) {
        int visibleIndex = 0;
        for (BoundsSlot slot : visibleSlots) {
            if (slot.tabIndex() == draggedIndex) {
                continue;
            }
            if (gapX < slot.x() + slot.width() / 2) {
                return layoutDropIndex(visibleIndex, draggedIndex);
            }
            visibleIndex++;
        }
        if (visibleSlots.isEmpty()) {
            return draggedIndex >= 0 ? Math.min(draggedIndex, tabCount) : 0;
        }
        BoundsSlot last = visibleSlots.get(visibleSlots.size() - 1);
        if (gapX >= last.x() + last.width() - gapWidth / 2) {
            return tabCount;
        }
        return layoutDropIndex(Math.max(0, visibleIndex - 1), draggedIndex);
    }

    record BoundsSlot(int tabIndex, int x, int width) {
    }

    static boolean isNoOpDrop(int fromIndex, int dropIndex) {
        return fromIndex == dropIndex || fromIndex + 1 == dropIndex;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
