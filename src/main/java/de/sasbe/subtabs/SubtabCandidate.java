package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;

public final class SubtabCandidate {
    private final @NotNull String slotId;
    private final int nameSegment;
    private final @NotNull String fileName;

    public SubtabCandidate(
            @NotNull String slotId,
            int nameSegment,
            @NotNull String fileName
    ) {
        this.slotId = slotId;
        this.nameSegment = nameSegment;
        this.fileName = fileName;
    }

    public @NotNull String slotId() {
        return slotId;
    }

    public int nameSegment() {
        return nameSegment;
    }

    public @NotNull String fileName() {
        return fileName;
    }

    public @NotNull String resolveLabel(@NotNull String matchedFileName) {
        return SubtabNameSegment.resolve(matchedFileName, nameSegment);
    }
}
