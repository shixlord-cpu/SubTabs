package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

enum SubtabOverflowMode {
    SCROLLBAR,
    ARROWS;

    static @NotNull SubtabOverflowMode fromPersisted(@Nullable String value) {
        if ("ARROWS".equalsIgnoreCase(value)) {
            return ARROWS;
        }
        return SCROLLBAR;
    }

    @NotNull String label() {
        return this == ARROWS ? "Randpfeile" : "Scrollbalken";
    }

    @Override
    public String toString() {
        return label();
    }
}
