package de.sasbe.subtabs;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.awt.Container;
import java.util.List;

final class SidetabsManager {
    static final Key<SidetabBarPanel> SIDETAB_BAR_KEY = Key.create("componentSubtabs.sidetabBar");
    private static final Key<CaretListener> CARET_KEY = Key.create("componentSubtabs.sidetabCaret");
    private static final Key<AttachState> ATTACH_STATE_KEY = Key.create("componentSubtabs.sidetabAttachState");

    private record AttachState(
            @NotNull SidetabLayoutMode layoutMode,
            boolean onRight,
            boolean expanded,
            @NotNull List<SidetabSection> sections
    ) {
    }

    private SidetabsManager() {
    }

    static void attachIfNeeded(@NotNull Project project, @NotNull VirtualFile file) {
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            return;
        }
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : ComponentSubtabsManager.editorsFor(manager, file)) {
            install(project, editor, file);
        }
    }

    static void applyPresentationState(@NotNull Project project) {
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            shutdown(project);
            return;
        }
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : manager.getAllEditors()) {
            VirtualFile file = editor.getFile();
            if (file != null) {
                install(project, editor, file);
            } else {
                detach(project, editor);
                SidetabsToggleOverlay.hide(editor);
                SidetabBarOverlay.hide(editor);
            }
        }
    }

    static void refreshAllOpenProjects() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (!project.isDisposed()) {
                applyPresentationState(project);
            }
        }
    }

    static void refreshForDocument(@NotNull Project project, @NotNull Document document) {
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            return;
        }
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null) {
            return;
        }
        attachIfNeeded(project, file);
        refreshPresentationForDocument(project, document);
    }

    static void refreshPresentationForDocument(@NotNull Project project, @NotNull Document document) {
        if (!SubtabsSettings.getInstance().isSidetabsActive()) {
            return;
        }
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null) {
            return;
        }
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : ComponentSubtabsManager.editorsFor(manager, file)) {
            SidetabBarPanel panel = editor.getUserData(SIDETAB_BAR_KEY);
            if (panel != null) {
                panel.refreshSectionPresentation();
            }
        }
    }

    static void refreshPresentationForFile(@NotNull Project project, @NotNull VirtualFile file) {
        if (!SubtabsSettings.getInstance().isSidetabsActive()) {
            return;
        }
        Document document = FileDocumentManager.getInstance().getCachedDocument(file);
        if (document != null) {
            refreshPresentationForDocument(project, document);
        }
    }

    static void detachFromFile(@NotNull Project project, @NotNull VirtualFile file) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : ComponentSubtabsManager.editorsFor(manager, file)) {
            detach(project, editor);
            SidetabsToggleOverlay.hide(editor);
            SidetabBarOverlay.hide(editor);
        }
    }

    private static void install(
            @NotNull Project project,
            @NotNull FileEditor editor,
            @NotNull VirtualFile file
    ) {
        SubtabsSettings settings = SubtabsSettings.getInstance();
        if (!settings.isFamiliaEnabled()) {
            detach(project, editor);
            SidetabsToggleOverlay.hide(editor);
            SidetabBarOverlay.hide(editor);
            return;
        }
        if (!settings.isSidetabsActive()) {
            detach(project, editor);
            SidetabsToggleOverlay.hide(editor);
            SidetabBarOverlay.hide(editor);
            return;
        }

        String text = documentText(editor, file);
        List<SidetabSection> sections = SidetabSectionsCache.getInstance(project).split(
                file,
                file.getName(),
                text,
                settings.getSidetabRulesGeneration(),
                settings.getSidetabRules()
        );
        if (sections.isEmpty()) {
            detach(project, editor);
            SidetabsToggleOverlay.hide(editor);
            SidetabBarOverlay.hide(editor);
            editor.putUserData(ATTACH_STATE_KEY, null);
            return;
        }

        boolean expanded = settings.isSidetabsExpanded();
        boolean showCollapse = settings.isShowCollapseButton();
        SidetabLayoutMode layoutMode = settings.getSidetabLayoutMode();
        boolean onRight = settings.isSidetabsOnRight();

        if (!expanded) {
            detach(project, editor);
            if (showCollapse) {
                SidetabsToggleOverlay.show(project, editor, false);
            } else {
                SidetabsToggleOverlay.hide(editor);
            }
            SidetabBarOverlay.hide(editor);
            editor.putUserData(ATTACH_STATE_KEY, null);
            return;
        }

        SidetabBarPanel panel = editor.getUserData(SIDETAB_BAR_KEY);
        AttachState attachState = editor.getUserData(ATTACH_STATE_KEY);
        int caret = caretOffset(editor);
        if (panel != null
                && attachState != null
                && attachState.expanded
                && attachState.layoutMode == layoutMode
                && attachState.onRight == onRight
                && sectionsEqual(attachState.sections, sections)) {
            int iconReserve = SidetabIconLayout.besideColumnTopReserve();
            panel.setTopIconReserve(layoutMode == SidetabLayoutMode.BESIDE ? iconReserve : 0);
            panel.refreshSelectionAndBlankStates(caret, text.length(), text);
            if (showCollapse) {
                SidetabsToggleOverlay.show(project, editor, true);
            } else {
                SidetabsToggleOverlay.hide(editor);
            }
            relayoutCollapseIcons(editor);
            SidetabBarOverlay.relayout(editor);
            return;
        }

        if (panel == null) {
            panel = new SidetabBarPanel(project, editor);
            editor.putUserData(SIDETAB_BAR_KEY, panel);
        }
        int iconReserve = SidetabIconLayout.besideColumnTopReserve();
        panel.setTopIconReserve(layoutMode == SidetabLayoutMode.BESIDE ? iconReserve : 0);
        panel.bind(sections, layoutMode, onRight, panel.selectedName(), caret, text.length());
        attachPanel(project, editor, panel, layoutMode, onRight);
        editor.putUserData(ATTACH_STATE_KEY, new AttachState(layoutMode, onRight, true, List.copyOf(sections)));

        if (showCollapse) {
            SidetabsToggleOverlay.show(project, editor, true);
        } else {
            SidetabsToggleOverlay.hide(editor);
        }
        installCaretListener(editor, panel);
        SidetabBarPanel.installFoldingListener(editor, panel);
        relayoutCollapseIcons(editor);
        SidetabBarOverlay.relayout(editor);
    }

    private static void relayoutCollapseIcons(@NotNull FileEditor editor) {
        SidetabsToggleOverlay.relayout(editor);
        SubtabsCollapseOverlay.relayout(editor);
        SubtabsExpandOverlay.relayout(editor);
    }

    static void refreshSeparatorBorders(@NotNull Project project) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : manager.getAllEditors()) {
            SidetabBarPanel panel = editor.getUserData(SIDETAB_BAR_KEY);
            if (panel != null) {
                panel.refreshSeparatorBorder();
            }
        }
    }

    static void refreshAppearance(@NotNull Project project) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : manager.getAllEditors()) {
            SidetabBarPanel panel = editor.getUserData(SIDETAB_BAR_KEY);
            if (panel != null) {
                panel.refreshAppearance();
            }
            relayoutCollapseIcons(editor);
            SidetabBarOverlay.relayout(editor);
        }
    }

    private static boolean sectionsEqual(@NotNull List<SidetabSection> left, @NotNull List<SidetabSection> right) {
        if (left.size() != right.size()) {
            return false;
        }
        for (int index = 0; index < left.size(); index++) {
            if (!left.get(index).equals(right.get(index))) {
                return false;
            }
        }
        return true;
    }

    private static void attachPanel(
            @NotNull Project project,
            @NotNull FileEditor editor,
            @NotNull SidetabBarPanel panel,
            @NotNull SidetabLayoutMode layoutMode,
            boolean onRight
    ) {
        AttachState attachState = editor.getUserData(ATTACH_STATE_KEY);
        if (attachState != null
                && attachState.layoutMode == layoutMode
                && attachState.onRight == onRight
                && isPanelAttached(editor, panel, layoutMode)) {
            return;
        }

        FileEditorManager manager = FileEditorManager.getInstance(project);
        manager.removeTopComponent(editor, panel);
        SidetabEditorHost.detachSide(editor.getComponent(), panel);
        SidetabBarOverlay.hide(editor);

        if (layoutMode == SidetabLayoutMode.OVERLAY) {
            SidetabBarOverlay.show(editor, panel, onRight);
            return;
        }
        SidetabEditorHost.installAround(wrapTarget(editor), panel, onRight);
    }

    private static boolean isPanelAttached(
            @NotNull FileEditor editor,
            @NotNull SidetabBarPanel panel,
            @NotNull SidetabLayoutMode layoutMode
    ) {
        if (layoutMode == SidetabLayoutMode.OVERLAY) {
            return panel.getParent() != null && panel.isShowing();
        }
        return panel.getParent() instanceof SidetabEditorHost;
    }

    private static @NotNull JComponent wrapTarget(@NotNull FileEditor editor) {
        JComponent editorComponent = editor.getComponent();
        Container parent = editorComponent.getParent();
        if (parent instanceof SidetabEditorHost) {
            return editorComponent;
        }
        if (parent instanceof JComponent composite) {
            return composite;
        }
        return editorComponent;
    }

    static void shutdown(@NotNull Project project) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : manager.getAllEditors()) {
            detach(project, editor);
            SidetabsToggleOverlay.hide(editor);
            SidetabBarOverlay.hide(editor);
        }
    }

    private static void detach(@NotNull Project project, @NotNull FileEditor editor) {
        SidetabBarPanel panel = editor.getUserData(SIDETAB_BAR_KEY);
        if (panel != null) {
            FileEditorManager.getInstance(project).removeTopComponent(editor, panel);
            SidetabEditorHost.detachSide(editor.getComponent(), panel);
            SidetabBarOverlay.hide(editor);
        }
        editor.putUserData(SIDETAB_BAR_KEY, null);
        editor.putUserData(ATTACH_STATE_KEY, null);
        if (editor instanceof TextEditor textEditor) {
            Editor ed = textEditor.getEditor();
            if (!ed.isDisposed()) {
                SidetabEditorHover.hide(ed);
            }
        }
        uninstallCaretListener(editor);
        SidetabBarPanel.uninstallFoldingListener(editor);
    }

    private static void installCaretListener(@NotNull FileEditor editor, @NotNull SidetabBarPanel panel) {
        if (editor.getUserData(CARET_KEY) != null) {
            return;
        }
        if (!(editor instanceof TextEditor textEditor)) {
            return;
        }
        Editor ed = textEditor.getEditor();
        CaretListener listener = new CaretListener() {
            @Override
            public void caretPositionChanged(@NotNull CaretEvent event) {
                int length = ed.getDocument().getTextLength();
                panel.selectOffset(ed.getCaretModel().getOffset(), length);
            }
        };
        ed.getCaretModel().addCaretListener(listener);
        editor.putUserData(CARET_KEY, listener);
    }

    private static void uninstallCaretListener(@NotNull FileEditor editor) {
        CaretListener listener = editor.getUserData(CARET_KEY);
        if (listener == null) {
            return;
        }
        if (editor instanceof TextEditor textEditor) {
            Editor ed = textEditor.getEditor();
            if (!ed.isDisposed()) {
                ed.getCaretModel().removeCaretListener(listener);
            }
        }
        editor.putUserData(CARET_KEY, null);
    }

    private static @NotNull String documentText(@NotNull FileEditor editor, @NotNull VirtualFile file) {
        if (editor instanceof TextEditor textEditor) {
            return textEditor.getEditor().getDocument().getText();
        }
        Document document = FileDocumentManager.getInstance().getDocument(file);
        return document == null ? "" : document.getText();
    }

    private static int caretOffset(@NotNull FileEditor editor) {
        if (editor instanceof TextEditor textEditor) {
            return textEditor.getEditor().getCaretModel().getOffset();
        }
        return 0;
    }
}
