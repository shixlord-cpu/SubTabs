package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImplKt;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class ComponentSubtabNavigation {
    private static final Key<Boolean> SWITCH_IN_PROGRESS = Key.create("componentSubtabs.switchInProgress");

    private ComponentSubtabNavigation() {
    }

    static boolean isSwitchInProgress(@NotNull Project project) {
        return Boolean.TRUE.equals(SWITCH_IN_PROGRESS.get(project));
    }

    static boolean sameSubtabGroup(
            @NotNull VirtualFile firstFile,
            @NotNull VirtualFile secondFile
    ) {
        ComponentRelatedFiles.Match firstMatch = ComponentRelatedFiles.find(firstFile);
        ComponentRelatedFiles.Match secondMatch = ComponentRelatedFiles.find(secondFile);
        if (firstMatch == null || secondMatch == null) {
            return false;
        }
        return firstMatch.key().equals(secondMatch.key());
    }

    static boolean canSwitchAdjacent(@NotNull Project project, int direction) {
        if (!SubtabsSettings.getInstance().isSubtabsActive()) {
            return false;
        }

        VirtualFile currentFile = selectedFile(project);
        return currentFile != null && adjacentSubtabFile(currentFile, direction) != null;
    }

    static void switchAdjacentSubtab(@NotNull Project project, int direction) {
        VirtualFile currentFile = selectedFile(project);
        if (currentFile == null) {
            return;
        }

        VirtualFile targetFile = adjacentSubtabFile(currentFile, direction);
        if (targetFile != null) {
            switchInSelectedEditor(project, currentFile, targetFile, true);
        }
    }

    static @Nullable VirtualFile adjacentInList(
            @NotNull VirtualFile currentFile,
            @NotNull List<ComponentRelatedFiles.Entry> relatedFiles,
            int direction
    ) {
        if (relatedFiles.size() < 2 || direction == 0) {
            return null;
        }

        int index = indexOf(relatedFiles, currentFile);
        if (index < 0) {
            return null;
        }

        int targetIndex = index + direction;
        if (targetIndex < 0 || targetIndex >= relatedFiles.size()) {
            return null;
        }
        return relatedFiles.get(targetIndex).file();
    }

    static void switchToRelatedFile(
            @NotNull Project project,
            @NotNull VirtualFile currentFile,
            @NotNull VirtualFile targetFile
    ) {
        switchInSelectedEditor(project, currentFile, targetFile, true);
    }

    static void focusExistingFile(
            @NotNull Project project,
            @NotNull VirtualFile targetFile,
            boolean requestFocus
    ) {
        runWithSwitchGuard(project, () -> focusExistingFileImpl(project, targetFile, requestFocus));
    }

    static void runWithSwitchGuard(@NotNull Project project, @NotNull Runnable action) {
        SWITCH_IN_PROGRESS.set(project, true);
        try {
            action.run();
        } finally {
            SWITCH_IN_PROGRESS.set(project, null);
        }
    }

    static void switchInSelectedEditor(
            @NotNull Project project,
            @NotNull VirtualFile anchorFile,
            @NotNull VirtualFile targetFile,
            boolean requestFocus
    ) {
        if (anchorFile.equals(targetFile)) {
            return;
        }

        runWithSwitchGuard(project, () -> switchInSelectedEditorImpl(project, anchorFile, targetFile, requestFocus));
    }

    private static void switchInSelectedEditorImpl(
            @NotNull Project project,
            @NotNull VirtualFile anchorFile,
            @NotNull VirtualFile targetFile,
            boolean requestFocus
    ) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        if (manager.isFileOpen(targetFile)) {
            focusExistingFileImpl(project, targetFile, requestFocus);
            return;
        }

        if (!manager.isFileOpen(anchorFile)) {
            manager.openFile(targetFile, requestFocus);
            ComponentSubtabsManager.syncSelectionForFile(project, targetFile);
            return;
        }

        FileEditor anchorEditor = editorForFile(manager, anchorFile);
        if (anchorEditor != null) {
            ComponentSubtabsManager.prepareTransfer(project, anchorEditor, targetFile);
        }

        FileEditorManagerImplKt.reopenVirtualFileEditor(project, anchorFile, targetFile, requestFocus);
        ComponentSubtabsManager.attachIfNeeded(project, targetFile);
        ComponentSubtabsManager.syncSelectionForFile(project, targetFile);
    }

    private static void focusExistingFileImpl(
            @NotNull Project project,
            @NotNull VirtualFile targetFile,
            boolean requestFocus
    ) {
        FileEditorManagerEx managerEx = FileEditorManagerEx.getInstanceEx(project);
        EditorWindow window = ComponentSubtabEditorLookup.findWindowWithFile(managerEx, targetFile);
        if (window != null) {
            managerEx.setCurrentWindow(window);
            window.setSelectedComposite(targetFile, requestFocus);
        } else {
            managerEx.openFile(targetFile, requestFocus);
        }
        ComponentSubtabsManager.syncSelectionForFile(project, targetFile);
    }

    private static @Nullable FileEditor editorForFile(
            @NotNull FileEditorManager manager,
            @NotNull VirtualFile file
    ) {
        FileEditor selected = manager.getSelectedEditor();
        if (selected != null && file.equals(selected.getFile())) {
            return selected;
        }
        FileEditor[] editors = manager.getEditors(file);
        return editors.length == 0 ? null : editors[0];
    }

    private static @Nullable VirtualFile selectedFile(@NotNull Project project) {
        VirtualFile[] selectedFiles = FileEditorManager.getInstance(project).getSelectedFiles();
        return selectedFiles.length == 0 ? null : selectedFiles[0];
    }

    private static @Nullable VirtualFile adjacentSubtabFile(
            @NotNull VirtualFile currentFile,
            int direction
    ) {
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(currentFile);
        if (match == null) {
            return null;
        }
        return adjacentInList(currentFile, match.relatedFiles(), direction);
    }

    private static int indexOf(
            @NotNull List<ComponentRelatedFiles.Entry> relatedFiles,
            @NotNull VirtualFile currentFile
    ) {
        for (int index = 0; index < relatedFiles.size(); index++) {
            if (relatedFiles.get(index).file().equals(currentFile)) {
                return index;
            }
        }
        return -1;
    }
}
