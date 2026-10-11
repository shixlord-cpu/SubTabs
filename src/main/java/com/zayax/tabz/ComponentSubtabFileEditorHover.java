package com.zayax.tabz;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Window;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.BorderFactory;

/**
 * Full code-editor highlight (markup + top chrome) for project-view and hover-select-box hover.
 */
final class ComponentSubtabFileEditorHover {
    private static final String ACTIVE_HANDLES_KEY = "componentTabz.fileEditorHoverHandles";
    private static final String PANE_PREVIOUS_BORDER_KEY = "componentTabz.fileEditorPanePreviousBorder";
    private static final int HOVER_LAYER = HighlighterLayer.SELECTION - 1;

    private record Handle(
            @NotNull FileEditor fileEditor,
            @Nullable Editor editor,
            @Nullable RangeHighlighter highlighter,
            @Nullable Border previousBorder
    ) {
    }

    private ComponentSubtabFileEditorHover() {
    }

    static void onEnter(
            @NotNull Project project,
            @NotNull VirtualFile file,
            @NotNull JComponent source
    ) {
        onEnterFiles(project, List.of(file), source);
    }

    static void onEnterRelatedGroup(
            @NotNull Project project,
            @NotNull VirtualFile anchorFile,
            @NotNull JComponent source
    ) {
        if (!SubtabHoverView.isEditorHighlightEnabled()) {
            return;
        }
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(anchorFile);
        if (match == null) {
            onEnter(project, anchorFile, source);
            return;
        }
        FileEditorManager manager = FileEditorManager.getInstance(project);
        Set<VirtualFile> openRelated = new LinkedHashSet<>();
        for (ComponentRelatedFiles.Entry entry : match.relatedFiles()) {
            VirtualFile file = entry.file();
            if (manager.isFileOpen(file)) {
                openRelated.add(file);
            }
        }
        onEnterFiles(project, List.copyOf(openRelated), source);
    }

    static void onEnterMergeGroup(
            @NotNull Project project,
            @NotNull String groupKey,
            @NotNull JComponent source
    ) {
        if (!SubtabHoverView.isEditorHighlightEnabled()) {
            return;
        }
        String mergeKey = SubtabProjectViewGrouping.mergeKey(groupKey);
        FileEditorManager manager = FileEditorManager.getInstance(project);
        Set<VirtualFile> openInGroup = new LinkedHashSet<>();
        for (VirtualFile file : manager.getOpenFiles()) {
            if (belongsToMergeGroup(file, mergeKey)) {
                openInGroup.add(file);
            }
        }
        onEnterFiles(project, List.copyOf(openInGroup), source);
    }

    static void onEnterFiles(
            @NotNull Project project,
            @NotNull List<VirtualFile> files,
            @NotNull JComponent source
    ) {
        if (!SubtabHoverView.isEditorHighlightEnabled()) {
            return;
        }
        onExit(source);

        List<Handle> handles = new ArrayList<>();
        FileEditorManager manager = FileEditorManager.getInstance(project);
        Set<FileEditor> seenFileEditors = new LinkedHashSet<>();
        for (VirtualFile file : files) {
            for (FileEditor fileEditor : manager.getEditors(file)) {
                if (!seenFileEditors.add(fileEditor)) {
                    continue;
                }
                Editor editor = null;
                RangeHighlighter highlighter = null;
                if (fileEditor instanceof TextEditor textEditor) {
                    editor = textEditor.getEditor();
                    if (!editor.isDisposed()) {
                        highlighter = addFullEditorHighlight(editor);
                    }
                }
                Border previousBorder = applyPaneHighlight(fileEditor);
                handles.add(new Handle(fileEditor, editor, highlighter, previousBorder));
            }
        }
        if (!handles.isEmpty()) {
            source.putClientProperty(ACTIVE_HANDLES_KEY, handles);
        }
    }

    static void onExit(@NotNull JComponent source) {
        @SuppressWarnings("unchecked")
        List<Handle> handles = (List<Handle>) source.getClientProperty(ACTIVE_HANDLES_KEY);
        source.putClientProperty(ACTIVE_HANDLES_KEY, null);
        if (handles == null || handles.isEmpty()) {
            return;
        }
        for (Handle handle : handles) {
            removeHighlight(handle);
        }
    }

    static boolean isIdeEditorForeground(@NotNull Project project) {
        Window window = WindowManager.getInstance().getFrame(project);
        if (!(window instanceof Frame frame)) {
            return true;
        }
        return frame.isActive();
    }

    private static boolean belongsToMergeGroup(
            @NotNull VirtualFile file,
            @NotNull String targetMergeKey
    ) {
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(file);
        if (match == null) {
            return false;
        }
        return targetMergeKey.equals(SubtabProjectViewGrouping.mergeKey(match.baseName()));
    }

    private static @Nullable Border applyPaneHighlight(@NotNull FileEditor fileEditor) {
        JComponent root = fileEditor.getComponent();
        Object stored = root.getClientProperty(PANE_PREVIOUS_BORDER_KEY);
        if (stored instanceof Border) {
            return (Border) stored;
        }
        Border previous = root.getBorder();
        root.putClientProperty(PANE_PREVIOUS_BORDER_KEY, previous);
        Border top = BorderFactory.createMatteBorder(JBUI.scale(2), 0, 0, 0, hoverAccentColor());
        root.setBorder(previous == null ? top : new CompoundBorder(top, previous));
        root.revalidate();
        root.repaint();
        return previous;
    }

    private static void restorePaneHighlight(@NotNull Handle handle) {
        JComponent root = handle.fileEditor().getComponent();
        Object stored = root.getClientProperty(PANE_PREVIOUS_BORDER_KEY);
        if (!(stored instanceof Border)) {
            stored = handle.previousBorder();
        }
        root.setBorder(stored instanceof Border border ? border : null);
        root.putClientProperty(PANE_PREVIOUS_BORDER_KEY, null);
        root.revalidate();
        root.repaint();
    }

    private static @Nullable RangeHighlighter addFullEditorHighlight(@NotNull Editor editor) {
        Document document = editor.getDocument();
        int textLength = document.getTextLength();
        if (textLength <= 0) {
            return null;
        }

        TextAttributes attributes = new TextAttributes();
        attributes.setBackgroundColor(hoverBackground());

        return editor.getMarkupModel().addRangeHighlighter(
                0,
                textLength,
                HOVER_LAYER,
                attributes,
                HighlighterTargetArea.LINES_IN_RANGE
        );
    }

    private static void removeHighlight(@NotNull Handle handle) {
        restorePaneHighlight(handle);
        Editor editor = handle.editor();
        RangeHighlighter highlighter = handle.highlighter();
        if (editor != null && !editor.isDisposed() && highlighter != null && highlighter.isValid()) {
            editor.getMarkupModel().removeHighlighter(highlighter);
        }
    }

    private static @NotNull Color hoverAccentColor() {
        return JBUI.CurrentTheme.EditorTabs.hoverBackground();
    }

    private static @NotNull Color hoverBackground() {
        Color hover = hoverAccentColor();
        if (hover.getAlpha() < 255) {
            return hover;
        }
        return JBColor.namedColor(
                "ComponentSubtab.FileEditorHoverBackground",
                new Color(hover.getRed(), hover.getGreen(), hover.getBlue(), 72)
        );
    }
}
