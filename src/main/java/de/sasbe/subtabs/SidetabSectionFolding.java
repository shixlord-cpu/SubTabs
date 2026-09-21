package de.sasbe.subtabs;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.FoldingModel;
import com.intellij.openapi.editor.ex.FoldingModelEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

final class SidetabSectionFolding {
    private static final Pattern CLASS_DECLARATION_LINE = Pattern.compile(
            "^(?:export\\s+)?(?:default\\s+)?(?:abstract\\s+)?class\\s+\\w+.*"
    );

    private SidetabSectionFolding() {
    }

    static boolean toggle(@NotNull Editor editor, @NotNull SidetabSection section) {
        if (!section.foldable()) {
            return false;
        }
        FoldingModel model = editor.getFoldingModel();
        if (model instanceof FoldingModelEx modelEx) {
            modelEx.setFoldingEnabled(true);
        }
        FoldRange range = foldRange(editor, section);
        if (range == null) {
            return false;
        }
        boolean[] folded = {false};
        model.runBatchFoldingOperation(() -> {
            FoldRegion region = findMatchingRegion(model, range);
            if (region == null) {
                region = model.addFoldRegion(range.start(), range.end(), section.name());
            }
            if (region != null) {
                region.setExpanded(!region.isExpanded());
                folded[0] = !region.isExpanded();
            }
        });
        return folded[0];
    }

    static boolean isFolded(@NotNull Editor editor, @NotNull SidetabSection section) {
        FoldingModel model = editor.getFoldingModel();
        FoldRange range = foldRange(editor, section);
        if (range == null) {
            return false;
        }
        FoldRegion region = findMatchingRegion(model, range);
        return region != null && !region.isExpanded();
    }

    static void setExpanded(@NotNull Editor editor, @NotNull SidetabSection section, boolean expanded) {
        FoldingModel model = editor.getFoldingModel();
        if (model instanceof FoldingModelEx modelEx) {
            modelEx.setFoldingEnabled(true);
        }
        FoldRange range = foldRange(editor, section);
        if (range == null) {
            return;
        }
        model.runBatchFoldingOperation(() -> {
            FoldRegion region = findMatchingRegion(model, range);
            if (region == null) {
                region = model.addFoldRegion(range.start(), range.end(), section.name());
            }
            if (region != null) {
                region.setExpanded(expanded);
            }
        });
    }

    private static @Nullable FoldRegion findMatchingRegion(
            @NotNull FoldingModel model,
            @NotNull FoldRange range
    ) {
        FoldRegion exact = model.getFoldRegion(range.start(), range.end());
        if (exact != null) {
            return exact;
        }
        for (FoldRegion region : model.getAllFoldRegions()) {
            if (region.getStartOffset() == range.start() && region.getEndOffset() == range.end()) {
                return region;
            }
        }
        return null;
    }

    private static @Nullable FoldRange foldRange(@NotNull Editor editor, @NotNull SidetabSection section) {
        Document document = editor.getDocument();
        int textLength = document.getTextLength();
        int sectionStart = Math.max(0, Math.min(section.startOffset(), textLength));
        int rawSectionEnd = Math.max(sectionStart, Math.min(section.endOffset(), textLength));
        if (rawSectionEnd <= sectionStart) {
            return null;
        }

        int sectionEnd = adjustSectionEnd(document, sectionStart, rawSectionEnd);
        if (sectionEnd <= sectionStart) {
            return null;
        }

        int startLine = document.getLineNumber(sectionStart);
        int endLine = document.getLineNumber(Math.max(sectionStart, sectionEnd - 1));
        if (startLine >= endLine) {
            if (sectionEnd - sectionStart > 1) {
                return new FoldRange(sectionStart, sectionEnd);
            }
            return null;
        }

        int foldStart = document.getLineStartOffset(startLine);
        int foldEnd = Math.min(sectionEnd, document.getLineEndOffset(endLine));
        if (foldEnd <= foldStart) {
            return null;
        }
        return new FoldRange(foldStart, foldEnd);
    }

    static int adjustSectionEnd(@NotNull Document document, int sectionStart, int sectionEnd) {
        int end = sectionEnd;
        CharSequence text = document.getCharsSequence();
        while (end > sectionStart && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        if (end > sectionStart && text.charAt(end - 1) == '{') {
            end--;
            while (end > sectionStart && Character.isWhitespace(text.charAt(end - 1))) {
                end--;
            }
            end = trimClassDeclarationLine(document, sectionStart, end);
        }
        return Math.max(sectionStart + 1, end);
    }

    private static int trimClassDeclarationLine(@NotNull Document document, int sectionStart, int end) {
        if (end <= sectionStart) {
            return end;
        }
        int lineStart = document.getLineStartOffset(document.getLineNumber(end - 1));
        if (lineStart < sectionStart || lineStart >= end) {
            return end;
        }
        String line = document.getText().substring(lineStart, end).trim();
        if (!CLASS_DECLARATION_LINE.matcher(line).matches()) {
            return end;
        }
        end = lineStart;
        CharSequence text = document.getCharsSequence();
        while (end > sectionStart && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        return end;
    }

    private record FoldRange(int start, int end) {
    }

    static @Nullable int[] foldRangeOffsetsForTest(@NotNull Editor editor, @NotNull SidetabSection section) {
        FoldRange range = foldRange(editor, section);
        return range == null ? null : new int[] {range.start(), range.end()};
    }

    static @Nullable FoldRegion foldRegionForTest(@NotNull Editor editor, @NotNull SidetabSection section) {
        FoldRange range = foldRange(editor, section);
        if (range == null) {
            return null;
        }
        return findMatchingRegion(editor.getFoldingModel(), range);
    }
}
