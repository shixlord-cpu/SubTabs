package de.sasbe.subtabs;

import org.jetbrains.annotations.Nullable;

/**
 * Temporary typography overrides while the settings UI previews slider values.
 * Does not persist; cleared on apply, reset, or dispose.
 */
final class TypographyPreview {
    private static @Nullable Integer barHeightPercent;
    private static @Nullable Integer textSizePercent;
    private static @Nullable TabFontStyle tabFontStyle;

    private TypographyPreview() {
    }

    static void set(@Nullable Integer barHeight, @Nullable Integer textSize, @Nullable TabFontStyle fontStyle) {
        barHeightPercent = barHeight;
        textSizePercent = textSize;
        tabFontStyle = fontStyle;
    }

    static void clear() {
        barHeightPercent = null;
        textSizePercent = null;
        tabFontStyle = null;
    }

    static int barHeightPercent(int fallback) {
        Integer preview = barHeightPercent;
        return preview != null ? preview : fallback;
    }

    static int textSizePercent(int fallback) {
        Integer preview = textSizePercent;
        return preview != null ? preview : fallback;
    }

    static @Nullable TabFontStyle tabFontStyle() {
        return tabFontStyle;
    }
}
