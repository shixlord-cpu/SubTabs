package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;

final class SubtabNameSegment {
    private SubtabNameSegment() {
    }

    static @NotNull String resolve(@NotNull String fileName, int segmentIndex) {
        if (segmentIndex == 0) {
            return "?";
        }

        String[] parts = fileName.split("\\.", -1);
        if (parts.length == 0) {
            return fileName;
        }

        if (segmentIndex > 0) {
            if (segmentIndex <= parts.length) {
                return parts[segmentIndex - 1];
            }
            return parts[parts.length - 1];
        }

        int index = parts.length + segmentIndex;
        if (index >= 0 && index < parts.length) {
            return parts[index];
        }
        return parts[0];
    }
}
