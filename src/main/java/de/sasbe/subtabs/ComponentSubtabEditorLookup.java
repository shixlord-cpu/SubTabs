package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ComponentSubtabEditorLookup {
    private ComponentSubtabEditorLookup() {
    }

    static @Nullable EditorWindow windowForFileOrCurrent(
            @NotNull FileEditorManagerEx manager,
            @NotNull VirtualFile file
    ) {
        EditorWindow currentWindow = manager.getCurrentWindow();
        if (currentWindow != null && currentWindow.isFileOpen(file)) {
            return currentWindow;
        }

        for (EditorWindow window : manager.getWindows()) {
            if (window.isFileOpen(file)) {
                return window;
            }
        }
        return currentWindow;
    }
}
