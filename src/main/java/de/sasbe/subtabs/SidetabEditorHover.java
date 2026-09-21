package de.sasbe.subtabs;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.Key;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

final class SidetabEditorHover {
    static final int HOVER_LAYER = HighlighterLayer.SELECTION - 1;
    private static final Key<RangeHighlighter> HIGHLIGHTER_KEY = Key.create("componentSubtabs.sidetabHoverHighlighter");

    private SidetabEditorHover() {
    }

    static void show(@NotNull Editor editor, @NotNull SidetabSection section) {
        if (SubtabHoverView.isDisabled() || editor.isDisposed()) {
            return;
        }
        hide(editor);

        Document document = editor.getDocument();
        int textLength = document.getTextLength();
        int sectionStart = Math.max(0, Math.min(section.startOffset(), textLength));
        int sectionEnd = Math.max(sectionStart, Math.min(section.endOffset(), textLength));
        if (sectionEnd <= sectionStart) {
            return;
        }

        int startLine = document.getLineNumber(sectionStart);
        int endLine = document.getLineNumber(Math.max(sectionStart, sectionEnd - 1));
        int startOffset = document.getLineStartOffset(startLine);
        int endOffset = Math.min(textLength, document.getLineEndOffset(endLine));

        TextAttributes attributes = new TextAttributes();
        attributes.setBackgroundColor(hoverBackground());

        RangeHighlighter highlighter = editor.getMarkupModel().addRangeHighlighter(
                startOffset,
                endOffset,
                HOVER_LAYER,
                attributes,
                HighlighterTargetArea.LINES_IN_RANGE
        );
        editor.putUserData(HIGHLIGHTER_KEY, highlighter);
    }

    static void hide(@NotNull Editor editor) {
        if (editor.isDisposed()) {
            return;
        }
        RangeHighlighter highlighter = editor.getUserData(HIGHLIGHTER_KEY);
        if (highlighter != null && highlighter.isValid()) {
            editor.getMarkupModel().removeHighlighter(highlighter);
        }
        editor.putUserData(HIGHLIGHTER_KEY, null);
    }

    static boolean isActive(@NotNull Editor editor) {
        RangeHighlighter highlighter = editor.getUserData(HIGHLIGHTER_KEY);
        return highlighter != null && highlighter.isValid();
    }

    private static @NotNull Color hoverBackground() {
        Color hover = JBUI.CurrentTheme.EditorTabs.hoverBackground();
        if (hover.getAlpha() < 255) {
            return hover;
        }
        return JBColor.namedColor(
                "ComponentSubtabs.SidetabHoverBackground",
                new Color(hover.getRed(), hover.getGreen(), hover.getBlue(), 72)
        );
    }
}
