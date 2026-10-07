package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

final class DemoReplayFileEditorListener implements FileEditorManagerListener {
    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        notify(source.getProject(), "fileOpened:" + file.getName());
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        notify(source.getProject(), "fileClosed:" + file.getName());
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        VirtualFile newFile = event.getNewFile();
        notify(event.getManager().getProject(), newFile == null
                ? "selectionChanged:null"
                : "selectionChanged:" + newFile.getName());
    }

    private static void notify(@NotNull Project project, @NotNull String trigger) {
        DemoReplayRecorder recorder = DemoReplayRecorder.getInstance(project);
        if (recorder != null) {
            recorder.onEditorStateChanged(trigger);
        }
    }
}
