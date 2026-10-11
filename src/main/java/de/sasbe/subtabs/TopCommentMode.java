package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;

enum TopCommentMode {
    OVERRIDE,
    COMBINE;

    static @NotNull TopCommentMode fromPersisted(@NotNull String value) {
        return "COMBINE".equalsIgnoreCase(value) ? COMBINE : OVERRIDE;
    }

    @NotNull String label() {
        return this == COMBINE ? "Combine" : "Override";
    }

    @NotNull String summary() {
        return this == COMBINE
                ? "Combine with file rules"
                : "Override file rules";
    }

    @Override
    public @NotNull String toString() {
        return label();
    }
}
