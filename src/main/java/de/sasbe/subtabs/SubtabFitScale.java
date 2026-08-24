package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;

final class SubtabFitScale {
    static final float MIN_FONT_SCALE = 0.78f;
    static final float MIN_PADDING_SCALE = 0.42f;
    private static final float FONT_FRACTION = 0.62f;
    private static final float PADDING_FRACTION = 0.38f;

    record Result(float fontScale, float paddingScale) {
        static final Result FULL = new Result(1f, 1f);

        boolean isFull() {
            return fontScale >= 0.999f && paddingScale >= 0.999f;
        }
    }

    private SubtabFitScale() {
    }

    static @NotNull Result compute(int availableWidth, int naturalWidth, boolean enabled) {
        if (!enabled || availableWidth <= 0 || naturalWidth <= 0 || naturalWidth <= availableWidth) {
            return Result.FULL;
        }

        float target = availableWidth / (float) naturalWidth;
        float minPaddingWidth = FONT_FRACTION + PADDING_FRACTION * MIN_PADDING_SCALE;
        if (target >= minPaddingWidth) {
            float paddingScale = (target - FONT_FRACTION) / PADDING_FRACTION;
            return new Result(1f, clamp(paddingScale, MIN_PADDING_SCALE, 1f));
        }

        float minWidth = FONT_FRACTION * MIN_FONT_SCALE + PADDING_FRACTION * MIN_PADDING_SCALE;
        if (target >= minWidth) {
            float fontScale = (target - PADDING_FRACTION * MIN_PADDING_SCALE) / FONT_FRACTION;
            return new Result(clamp(fontScale, MIN_FONT_SCALE, 1f), MIN_PADDING_SCALE);
        }
        return new Result(MIN_FONT_SCALE, MIN_PADDING_SCALE);
    }

    static boolean differs(@NotNull Result left, @NotNull Result right) {
        return Math.abs(left.fontScale() - right.fontScale()) >= 0.015f
                || Math.abs(left.paddingScale() - right.paddingScale()) >= 0.015f;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
