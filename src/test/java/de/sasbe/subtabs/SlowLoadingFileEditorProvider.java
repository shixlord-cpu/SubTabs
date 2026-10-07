package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.AsyncFileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * Mirrors the sandbox IDE without the Ultimate language plugins: ts/scss/spec editors are built by a
 * slow background highlighter (TextMate), while html keeps its fast native editor.
 */
final class SlowLoadingFileEditorProvider implements AsyncFileEditorProvider {
    static final String FILE_PREFIX = "slow-list.component.";

    private final long delayMs;

    SlowLoadingFileEditorProvider(long delayMs) {
        this.delayMs = delayMs;
    }

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        String name = file.getName();
        return name.startsWith(FILE_PREFIX) && !name.endsWith(".html");
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return TextEditorProvider.getInstance().createEditor(project, file);
    }

    @Override
    public @NotNull Builder createEditorAsync(@NotNull Project project, @NotNull VirtualFile file) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new Builder() {
            @Override
            public FileEditor build() {
                return createEditor(project, file);
            }
        };
    }

    @Override
    public @NotNull String getEditorTypeId() {
        return "subtabs-slow-loading-test-editor";
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
