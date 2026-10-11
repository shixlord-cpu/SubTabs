package com.zayax.tabz;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

final class ComponentSubtabEditorLookup {
    private ComponentSubtabEditorLookup() {
    }

    static @Nullable EditorWindow findWindowWithFile(
            @NotNull FileEditorManagerEx manager,
            @NotNull VirtualFile file
    ) {
        for (EditorWindow window : manager.getWindows()) {
            if (window.isFileOpen(file)) {
                return window;
            }
        }
        return null;
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

    /**
     * For a simple two-pane editor split, returns whether {@code window} is the right-hand pane.
     */
    static @Nullable EditorWindow windowHostingEditor(
            @NotNull FileEditorManagerEx manager,
            @NotNull FileEditor editor
    ) {
        JComponent editorComponent = editor.getComponent();
        for (EditorWindow window : manager.getWindows()) {
            JComponent windowRoot = window.getTabbedPane().getComponent();
            if (SwingUtilities.isDescendingFrom(editorComponent, windowRoot)) {
                return window;
            }
        }
        VirtualFile file = editor.getFile();
        return file == null ? null : findWindowWithFile(manager, file);
    }

    static boolean isRightSplitPane(
            @NotNull FileEditorManagerEx manager,
            @NotNull EditorWindow window
    ) {
        EditorWindow[] windows = manager.getWindows();
        if (windows.length <= 1) {
            return false;
        }
        for (int index = 0; index < windows.length; index++) {
            if (windows[index] == window) {
                return index > 0;
            }
        }
        return false;
    }
}
