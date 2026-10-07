package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

enum SplittabBehaviorMode {
    INTEGRATED,
    DEDICATED_VIEW;

    static @NotNull SplittabBehaviorMode fromPersisted(@Nullable String value) {
        if ("DEDICATED_VIEW".equalsIgnoreCase(value)) {
            return DEDICATED_VIEW;
        }
        return INTEGRATED;
    }

    @NotNull String label() {
        return this == DEDICATED_VIEW ? "Switch" : "Mixed";
    }

    @Override
    public @NotNull String toString() {
        return label();
    }
}
