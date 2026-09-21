package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;

record SidetabSection(
        @NotNull String name,
        int startOffset,
        int endOffset,
        int depth,
        boolean fixedDepth,
        boolean foldable
) {
    SidetabSection(@NotNull String name, int startOffset, int endOffset) {
        this(name, startOffset, endOffset, 0, false, true);
    }

    SidetabSection(
            @NotNull String name,
            int startOffset,
            int endOffset,
            int depth,
            boolean fixedDepth
    ) {
        this(name, startOffset, endOffset, depth, fixedDepth, true);
    }

    boolean contains(int offset, int textLength) {
        if (endOffset <= startOffset) {
            return false;
        }
        if (offset >= startOffset && offset < endOffset) {
            return true;
        }
        return offset == textLength && endOffset == textLength && startOffset < endOffset;
    }

    boolean isBlank(@NotNull String text) {
        int from = Math.max(0, Math.min(startOffset, text.length()));
        int to = Math.max(from, Math.min(endOffset, text.length()));
        return text.substring(from, to).isBlank();
    }

    @NotNull SidetabSection withDepth(int nextDepth, boolean nextFixedDepth) {
        return new SidetabSection(name, startOffset, endOffset, nextDepth, nextFixedDepth, foldable);
    }
}