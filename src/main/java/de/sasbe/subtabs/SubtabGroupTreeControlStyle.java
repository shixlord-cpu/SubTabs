package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

enum SubtabGroupTreeControlStyle {
    DEFAULT,
    CUBES,
    CIRCLES,
    BLUE_ARROWS,
    NONE;

    static @NotNull SubtabGroupTreeControlStyle fromPersisted(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT;
        }
        for (SubtabGroupTreeControlStyle style : values()) {
            if (style.name().equalsIgnoreCase(value.trim())) {
                return style;
            }
        }
        return DEFAULT;
    }

    @NotNull String label() {
        return switch (this) {
            case DEFAULT -> "Default";
            case CUBES -> "Cubes";
            case CIRCLES -> "Circles";
            case BLUE_ARROWS -> "Blue-Arrows";
            case NONE -> "None";
        };
    }

    boolean allowsGroupExpansion() {
        return this != NONE;
    }

    @Override
    public String toString() {
        return label();
    }
}
