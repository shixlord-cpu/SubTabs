package com.zayax.tabz;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Verhalten, wenn bei offenem Split Pair eine Datei eines anderen gespeicherten Split Pairs geöffnet wird. */
enum SplittabOtherPairFileMode {
    /** Aktuelles Split Pair verlassen und die Datei normal öffnen. */
    OPEN_NORMALLY,
    /** Zu dem gespeicherten Split Pair wechseln, zu dem die Datei gehört. */
    SWITCH_TO_PAIR;

    static @NotNull SplittabOtherPairFileMode fromPersisted(@Nullable String value) {
        if ("SWITCH_TO_PAIR".equalsIgnoreCase(value)) {
            return SWITCH_TO_PAIR;
        }
        return OPEN_NORMALLY;
    }

    @NotNull String label() {
        return this == SWITCH_TO_PAIR
                ? "Switch to the split pair that owns the file"
                : "Leave split pair and open the file normally";
    }

    @Override
    public @NotNull String toString() {
        return label();
    }
}
