package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

enum SplittabDissolveMode {
    /** Verknüpfung entfernen; Editor-Tabs bleiben als normale Tabs geöffnet. */
    DISSOLVE,
    /** Beide Dateien schließen und Verknüpfung entfernen. */
    CLOSE_PAIR;

    static @NotNull SplittabDissolveMode fromPersisted(@Nullable String value) {
        if ("CLOSE_PAIR".equalsIgnoreCase(value)) {
            return CLOSE_PAIR;
        }
        return DISSOLVE;
    }

    @NotNull String label() {
        return this == CLOSE_PAIR ? "Close" : "Convert";
    }

    @Override
    public @NotNull String toString() {
        return label();
    }
}
