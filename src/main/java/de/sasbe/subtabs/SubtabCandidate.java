package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;

public final class SubtabCandidate {
    private final @NotNull String slotId;
    private final @NotNull String label;
    private final @NotNull String fileName;

    public SubtabCandidate(
            @NotNull String slotId,
            @NotNull String label,
            @NotNull String fileName
    ) {
        this.slotId = slotId;
        this.label = label;
        this.fileName = fileName;
    }

    public @NotNull String slotId() {
        return slotId;
    }

    public @NotNull String label() {
        return label;
    }

    public @NotNull String fileName() {
        return fileName;
    }

    public @NotNull String displayLabel(@NotNull String matchedFileName) {
        if (!"STYLE".equals(slotId)) {
            return label;
        }
        int dot = matchedFileName.lastIndexOf('.');
        if (dot < 0 || dot == matchedFileName.length() - 1) {
            return label;
        }
        return matchedFileName.substring(dot + 1).toUpperCase();
    }
}
