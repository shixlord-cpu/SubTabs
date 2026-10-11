package com.zayax.tabz;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

enum SidetabLayoutMode {
    BESIDE,
    OVERLAY;

    static @NotNull SidetabLayoutMode fromPersisted(@Nullable String value) {
        if ("OVERLAY".equalsIgnoreCase(value) || "ABOVE".equalsIgnoreCase(value)) {
            return OVERLAY;
        }
        return BESIDE;
    }

    @NotNull String label() {
        return this == OVERLAY ? "Empty bars" : "As labels";
    }

    @Override
    public @NotNull String toString() {
        return label();
    }
}
