package de.sasbe.subtabs;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.problems.WolfTheProblemSolver;
import org.jetbrains.annotations.NotNull;

record ComponentSubtabFilePresentation(boolean modified, boolean hasErrors) {
    static final ComponentSubtabFilePresentation CLEAN = new ComponentSubtabFilePresentation(false, false);

    static @NotNull ComponentSubtabFilePresentation compute(@NotNull Project project, @NotNull VirtualFile file) {
        if (project.isDisposed() || file.isDirectory()) {
            return CLEAN;
        }
        FileDocumentManager manager = FileDocumentManager.getInstance();
        Document document = manager.getCachedDocument(file);
        if (document != null) {
            return computeForDocument(project, document, file);
        }
        return ReadAction.compute(() -> computeInReadAction(project, file));
    }

    static @NotNull ComponentSubtabFilePresentation computeForDocument(
            @NotNull Project project,
            @NotNull Document document
    ) {
        if (project.isDisposed()) {
            return CLEAN;
        }
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null || file.isDirectory()) {
            return CLEAN;
        }
        return computeForDocument(project, document, file);
    }

    static @NotNull ComponentSubtabFilePresentation computeForDocument(
            @NotNull Project project,
            @NotNull Document document,
            @NotNull VirtualFile file
    ) {
        boolean unsaved = document.getModificationStamp() != file.getModificationStamp();
        boolean errors = hasMarkupErrors(project, document) || hasWolfErrors(project, file);
        if (unsaved) {
            return new ComponentSubtabFilePresentation(true, errors);
        }
        return ReadAction.compute(() -> new ComponentSubtabFilePresentation(
                ComponentSubtabModifiedUi.hasUncommittedVcsChanges(project, file),
                errors || hasWolfErrors(project, file)
        ));
    }

    private static @NotNull ComponentSubtabFilePresentation computeInReadAction(
            @NotNull Project project,
            @NotNull VirtualFile file
    ) {
        return new ComponentSubtabFilePresentation(
                ComponentSubtabModifiedUi.isModifiedInReadAction(project, file),
                hasWolfErrors(project, file)
        );
    }

    static boolean hasErrorsInReadAction(@NotNull Project project, @NotNull VirtualFile file) {
        FileDocumentManager manager = FileDocumentManager.getInstance();
        Document document = manager.getCachedDocument(file);
        if (document != null && hasMarkupErrors(project, document)) {
            return true;
        }
        return hasWolfErrors(project, file);
    }

    private static boolean hasWolfErrors(@NotNull Project project, @NotNull VirtualFile file) {
        if (project.isDefault()) {
            return false;
        }
        WolfTheProblemSolver wolf = WolfTheProblemSolver.getInstance(project);
        if (wolf == null) {
            return false;
        }
        return wolf.isProblemFile(file) || wolf.hasSyntaxErrors(file);
    }

    static boolean hasMarkupErrors(@NotNull Project project, @NotNull Document document) {
        return hasMarkupErrorsInRange(project, document, 0, document.getTextLength());
    }

    static boolean hasMarkupErrorsInRange(
            @NotNull Project project,
            @NotNull Document document,
            int start,
            int end
    ) {
        if (end <= start) {
            return false;
        }
        var model = com.intellij.openapi.editor.impl.DocumentMarkupModel.forDocument(document, project, false);
        if (model == null) {
            return false;
        }
        for (RangeHighlighter highlighter : model.getAllHighlighters()) {
            if (!highlighter.isValid()) {
                continue;
            }
            if (highlighter.getEndOffset() <= start || highlighter.getStartOffset() >= end) {
                continue;
            }
            HighlightInfo info = HighlightInfo.fromRangeHighlighter(highlighter);
            if (info != null && info.getSeverity().compareTo(HighlightSeverity.ERROR) >= 0) {
                return true;
            }
        }
        return false;
    }
}
