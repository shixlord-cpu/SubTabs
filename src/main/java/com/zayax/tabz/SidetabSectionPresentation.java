package com.zayax.tabz;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ex.LineStatusTracker;
import com.intellij.openapi.vcs.impl.LineStatusTrackerManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

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
        boolean modified = isModifiedInRange(project, document, file, section);
        return new SidetabSectionPresentation(modified, errors);
    }

    private static boolean isModifiedInRange(
            @NotNull Project project,
            @NotNull Document document,
            @NotNull VirtualFile file,
            @NotNull SidetabSection section
    ) {
        if (section.endOffset() <= section.startOffset()) {
            return false;
        }
        if (document.getModificationStamp() != file.getModificationStamp()
                && rangeDiffersFromSaved(document, file, section)) {
            return true;
        }
        return TabzReadActions.compute(() -> hasVcsChangesInRange(
                project,
                document,
                section.startOffset(),
                section.endOffset()
        ));
    }

    private static boolean rangeDiffersFromSaved(
            @NotNull Document document,
            @NotNull VirtualFile file,
            @NotNull SidetabSection section
    ) {
        String currentRange = textInRange(document, section.startOffset(), section.endOffset());
        try {
            String savedText = VfsUtil.loadText(file);
            List<CustomSidetabRule> rules = TabzSettings.getInstance().getSidetabRules();
            List<SidetabSection> currentSections = SidetabSections.split(
                    file.getName(),
                    document.getText(),
                    rules
            );
            List<SidetabSection> savedSections = SidetabSections.split(
                    file.getName(),
                    savedText,
                    rules
            );
            int sectionIndex = sectionIndex(currentSections, section);
            if (sectionIndex < 0) {
                return !currentRange.isEmpty();
            }
            if (sectionIndex >= savedSections.size()) {
                return !currentRange.isBlank();
            }
            SidetabSection savedSection = savedSections.get(sectionIndex);
            String savedRange = textInRange(savedText, savedSection.startOffset(), savedSection.endOffset());
            return !currentRange.equals(savedRange);
        } catch (IOException ignored) {
            return true;
        }
    }

    private static int sectionIndex(@NotNull List<SidetabSection> sections, @NotNull SidetabSection section) {
        for (int index = 0; index < sections.size(); index++) {
            SidetabSection candidate = sections.get(index);
            if (candidate.name().equals(section.name())
                    && candidate.depth() == section.depth()
                    && candidate.startOffset() == section.startOffset()) {
                return index;
            }
        }
        return -1;
    }

    private static @NotNull String textInRange(@NotNull Document document, int start, int end) {
        int textLength = document.getTextLength();
        int safeStart = Math.max(0, Math.min(start, textLength));
        int safeEnd = Math.max(safeStart, Math.min(end, textLength));
        if (safeEnd <= safeStart) {
            return "";
        }
        return document.getCharsSequence().subSequence(safeStart, safeEnd).toString();
    }

    private static @NotNull String textInRange(@NotNull String text, int start, int end) {
        int textLength = text.length();
        int safeStart = Math.max(0, Math.min(start, textLength));
        int safeEnd = Math.max(safeStart, Math.min(end, textLength));
        if (safeEnd <= safeStart) {
            return "";
        }
        return text.substring(safeStart, safeEnd);
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
