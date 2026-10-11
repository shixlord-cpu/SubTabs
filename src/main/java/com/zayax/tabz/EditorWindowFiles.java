package com.zayax.tabz;

import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class EditorWindowFiles {
    private EditorWindowFiles() {
    }

    static @NotNull List<VirtualFile> files(@NotNull EditorWindow window) {
        return window.getFileList();
    }

    static @NotNull VirtualFile[] fileArray(@NotNull EditorWindow window) {
        List<VirtualFile> files = files(window);
        return files.toArray(VirtualFile.EMPTY_ARRAY);
    }
}
