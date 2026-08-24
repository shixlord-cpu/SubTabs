package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

final class SubtabGroupNavigation {
    private SubtabGroupNavigation() {
    }

    static void navigateGroup(
            @NotNull Project project,
            @NotNull List<VirtualFile> groupFiles,
            boolean requestFocus,
            @NotNull Runnable openPrimaryFile
    ) {
        VirtualFile openFile = preferredOpenFile(
                FileEditorManager.getInstance(project).getSelectedFiles(),
                groupFiles,
                file -> FileEditorManager.getInstance(project).isFileOpen(file)
        );
        if (openFile != null) {
            ComponentSubtabNavigation.focusExistingFile(project, openFile, requestFocus);
            return;
        }
        openPrimaryFile.run();
    }

    static @Nullable VirtualFile preferredOpenFile(
            @NotNull VirtualFile[] selectedFiles,
            @NotNull Iterable<VirtualFile> groupFiles,
            @NotNull Predicate<VirtualFile> isOpen
    ) {
        VirtualFile selected = selectedFiles.length == 0 ? null : selectedFiles[0];
        if (selected != null) {
            for (VirtualFile file : groupFiles) {
                if (file.equals(selected) && isOpen.test(file)) {
                    return file;
                }
            }
        }

        for (VirtualFile file : groupFiles) {
            if (isOpen.test(file)) {
                return file;
            }
        }
        return null;
    }
}
