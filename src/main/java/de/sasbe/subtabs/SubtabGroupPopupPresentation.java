package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Decides which entries of a group popup are highlighted and which are greyed out.
 *
 * <p>The state is scoped to a single main tab, never to the editor window: several main tabs of the
 * same group can be open at once, and each of them has to highlight its own file.
 */
final class SubtabGroupPopupPresentation {
    record Context(
            @NotNull Set<VirtualFile> highlightedFiles,
            @NotNull Set<VirtualFile> openInThisTab
    ) {
        static @NotNull Context projectView() {
            return new Context(Set.of(), Set.of());
        }

        @Nullable VirtualFile primaryHighlight() {
            return highlightedFiles.isEmpty() ? null : highlightedFiles.iterator().next();
        }
    }

    private SubtabGroupPopupPresentation() {
    }

    static @NotNull Context forMainTab(@NotNull Project project, @NotNull VirtualFile tabFile) {
        ComponentSubtabGroupSplitRegistry.SplitState split =
                ComponentSubtabGroupSplitRegistry.getInstance(project).findByFile(tabFile);
        if (split == null) {
            return new Context(Set.of(tabFile), Set.of(tabFile));
        }

        // A group split shows two files under one main tab, so both count as active here.
        Set<VirtualFile> paneFiles = new LinkedHashSet<>();
        paneFiles.add(split.leftFile());
        paneFiles.add(split.rightFile());
        Set<VirtualFile> immutable = Set.copyOf(paneFiles);
        return new Context(immutable, immutable);
    }

    static boolean isHighlighted(@NotNull Context context, @NotNull VirtualFile file) {
        return context.highlightedFiles().contains(file);
    }

    static boolean isOpenElsewhere(
            @NotNull Project project,
            @NotNull Context context,
            @NotNull VirtualFile file
    ) {
        if (!FileEditorManager.getInstance(project).isFileOpen(file)) {
            return false;
        }
        return !context.openInThisTab().contains(file);
    }
}
