package de.sasbe.subtabs;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ex.LineStatusTracker;
import com.intellij.openapi.vcs.impl.LineStatusTrackerManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

record SidetabSectionPresentation(boolean modified, boolean hasErrors) {
    static final SidetabSectionPresentation CLEAN = new SidetabSectionPresentation(false, false);

    static @NotNull SidetabSectionPresentation compute(
            @NotNull Project project,
            @NotNull Document document,
            @NotNull VirtualFile file,
            @NotNull SidetabSection section
    ) {
        if (project.isDisposed()) {
            return CLEAN;
        }
        int start = section.startOffset();
        int end = section.endOffset();
        boolean errors = ComponentSubtabFilePresentation.hasMarkupErrorsInRange(project, document, start, end);
        boolean modified = isModifiedInRange(project, document, file, start, end);
        return new SidetabSectionPresentation(modified, errors);
    }

    private static boolean isModifiedInRange(
            @NotNull Project project,
            @NotNull Document document,
            @NotNull VirtualFile file,
            int start,
            int end
    ) {
        if (end <= start) {
            return false;
        }
        if (rangeDiffersFromSaved(document, file, start, end)) {
            return true;
        }
        return ReadAction.compute(() -> hasVcsChangesInRange(project, document, start, end));
    }

    private static boolean rangeDiffersFromSaved(
            @NotNull Document document,
            @NotNull VirtualFile file,
            int start,
            int end
    ) {
        int textLength = document.getTextLength();
        int safeStart = Math.max(0, Math.min(start, textLength));
        int safeEnd = Math.max(safeStart, Math.min(end, textLength));
        if (safeEnd <= safeStart) {
            return false;
        }
        String current = document.getCharsSequence().subSequence(safeStart, safeEnd).toString();
        try {
            String savedText = VfsUtil.loadText(file);
            if (safeStart >= savedText.length()) {
                return true;
            }
            int savedEnd = Math.min(safeEnd, savedText.length());
            String savedRange = savedText.substring(safeStart, savedEnd);
            if (safeEnd > savedText.length()) {
                return true;
            }
            return !current.equals(savedRange);
        } catch (IOException ignored) {
            return true;
        }
    }

    private static boolean hasVcsChangesInRange(
            @NotNull Project project,
            @NotNull Document document,
            int start,
            int end
    ) {
        if (project.isDefault()) {
            return false;
        }
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null || !ComponentSubtabModifiedUi.hasUncommittedVcsChanges(project, file)) {
            return false;
        }
        LineStatusTracker<?> tracker = LineStatusTrackerManager.getInstance(project).getLineStatusTracker(document);
        if (tracker == null) {
            return false;
        }
        int textLength = document.getTextLength();
        int safeStart = Math.max(0, Math.min(start, textLength));
        int safeEnd = Math.max(safeStart, Math.min(end, textLength));
        if (safeEnd <= safeStart) {
            return false;
        }
        int startLine = document.getLineNumber(safeStart);
        int endLine = document.getLineNumber(Math.max(safeStart, safeEnd - 1));
        for (int line = startLine; line <= endLine; line++) {
            if (tracker.getRangeForLine(line) != null) {
                return true;
            }
        }
        return false;
    }
}
