package de.sasbe.subtabs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Font;

enum TabFontStyle {
    IDE_STANDARD,
    SANS_SERIF,
    SERIF,
    MONOSPACED,
    BOLD,
    ITALIC;

    static @NotNull TabFontStyle fromPersisted(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return IDE_STANDARD;
        }
        for (TabFontStyle style : values()) {
            if (style.name().equalsIgnoreCase(value)) {
                return style;
            }
        }
        return IDE_STANDARD;
    }

    static @NotNull TabFontStyle current() {
        TabFontStyle preview = TypographyPreview.tabFontStyle();
        if (preview != null) {
            return preview;
        }
        if (ApplicationManager.getApplication() == null) {
            return IDE_STANDARD;
        }
        return SubtabsSettings.getInstance().getTabFontStyle();
    }

    @NotNull String label() {
        return switch (this) {
            case IDE_STANDARD -> "IDE-Standard";
            case SANS_SERIF -> "Sans Serif";
            case SERIF -> "Serif";
            case MONOSPACED -> "Monospace";
            case BOLD -> "Durchgehend fett";
            case ITALIC -> "Kursiv";
        };
    }

    @NotNull Font baseFont() {
        Font ide = UIUtil.getLabelFont();
        return switch (this) {
            case IDE_STANDARD, BOLD, ITALIC -> ide;
            case SANS_SERIF -> new Font(Font.SANS_SERIF, Font.PLAIN, ide.getSize());
            case SERIF -> new Font(Font.SERIF, Font.PLAIN, ide.getSize());
            case MONOSPACED -> new Font(Font.MONOSPACED, Font.PLAIN, ide.getSize());
        };
    }

    @NotNull Font styledFont(@NotNull Font base, boolean selected) {
        return switch (this) {
            case IDE_STANDARD, SANS_SERIF, SERIF, MONOSPACED -> selected ? withStyle(base, Font.BOLD) : withStyle(base, Font.PLAIN);
            case BOLD -> withStyle(base, Font.BOLD);
            case ITALIC -> selected ? withStyle(base, Font.BOLD | Font.ITALIC) : withStyle(base, Font.ITALIC);
        };
    }

    private static @NotNull Font withStyle(@NotNull Font font, int style) {
        return font.deriveFont(style);
    }

    @Override
    public @NotNull String toString() {
        return label();
    }
}
