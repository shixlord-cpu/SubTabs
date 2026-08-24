package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JScrollBar;
import javax.swing.JViewport;
import java.awt.Dimension;
import java.awt.Point;

final class SubtabBarScrolling {
    static final int DEFAULT_PIXEL_STEP = 16;

    private SubtabBarScrolling() {
    }

    static boolean canScrollLeft(int value, int minimum) {
        return value > minimum;
    }

    static boolean canScrollRight(int value, int extent, int maximum) {
        return value + extent < maximum;
    }

    static int clampValue(int value, int minimum, int extent, int maximum) {
        int maxValue = Math.max(minimum, maximum - Math.max(extent, 0));
        return Math.max(minimum, Math.min(maxValue, value));
    }

    static int step(int viewportWidth, int scaledMinimum) {
        return Math.max(scaledMinimum, Math.max(viewportWidth / 2, 1));
    }

    static int wheelPixelDelta(double preciseRotation, int wheelRotation, int pixelStep) {
        int step = Math.max(1, pixelStep);
        if (preciseRotation != 0d) {
            int delta = (int) Math.round(preciseRotation * step);
            if (delta == 0) {
                return preciseRotation > 0 ? step : -step;
            }
            return delta;
        }
        if (wheelRotation != 0) {
            return wheelRotation * step;
        }
        return 0;
    }

    static int nextViewX(int currentX, int delta, int viewportWidth, int viewWidth) {
        return clampValue(currentX + delta, 0, viewportWidth, viewWidth);
    }

    static @NotNull ViewportSnapshot snapshot(@NotNull JViewport viewport, int contentWidth) {
        int viewportWidth = Math.max(viewport.getExtentSize().width, viewport.getWidth());
        int viewWidth = Math.max(contentWidth, viewport.getViewSize().width);
        return new ViewportSnapshot(viewport.getViewPosition().x, viewportWidth, viewWidth);
    }

    static @NotNull ViewportSnapshot applyHorizontalScroll(
            @NotNull JViewport viewport,
            @Nullable JScrollBar bar,
            int contentWidth,
            int delta
    ) {
        ViewportSnapshot snapshot = snapshot(viewport, contentWidth);
        if (snapshot.viewportWidth() <= 0 || snapshot.viewWidth() <= 0) {
            return snapshot;
        }

        int nextX = snapshot.scrolledBy(delta);
        Dimension size = viewport.getViewSize();
        if (size.width < snapshot.viewWidth() || size.height < 1) {
            viewport.setViewSize(new Dimension(
                    snapshot.viewWidth(),
                    Math.max(size.height, 1)
            ));
        }
        if (bar != null) {
            bar.setValues(nextX, snapshot.viewportWidth(), 0, Math.max(snapshot.viewWidth(), snapshot.viewportWidth()));
        }
        Point current = viewport.getViewPosition();
        if (current.x != nextX) {
            viewport.setViewPosition(new Point(nextX, current.y));
        }
        return new ViewportSnapshot(viewport.getViewPosition().x, snapshot.viewportWidth(), snapshot.viewWidth());
    }

    record ViewportSnapshot(int x, int viewportWidth, int viewWidth) {
        boolean canScrollLeft() {
            return SubtabBarScrolling.canScrollLeft(x, 0);
        }

        boolean canScrollRight() {
            return SubtabBarScrolling.canScrollRight(x, viewportWidth, viewWidth);
        }

        int scrolledBy(int delta) {
            return nextViewX(x, delta, viewportWidth, viewWidth);
        }
    }
}
