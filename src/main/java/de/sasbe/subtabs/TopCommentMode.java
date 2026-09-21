package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;

enum TopCommentMode {
    OVERRIDE,
    COMBINE;

    static @NotNull TopCommentMode fromPersisted(@NotNull String value) {
        return "COMBINE".equalsIgnoreCase(value) ? COMBINE : OVERRIDE;
    }

    @NotNull String label() {
        return this == COMBINE ? "Kombinieren" : "Überschreiben";
    }

    @NotNull String summary() {
        return this == COMBINE
                ? "Mit Datei-Regeln kombinieren"
                : "Datei-Regeln überschreiben";
    }

    @Override
    public @NotNull String toString() {
        return label();
    }
}
