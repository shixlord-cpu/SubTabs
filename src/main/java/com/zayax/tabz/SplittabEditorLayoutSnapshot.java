package com.zayax.tabz;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class SplittabEditorLayoutSnapshot {
    static final int FORMAT_VERSION = 3;

    static final class State {
        public int formatVersion = FORMAT_VERSION;
        public @Nullable Element layoutElement;
        public int editorWindowCount;
        public @Nullable String selectedFilePath;
        public int currentWindowIndex;
    }

    private SplittabEditorLayoutSnapshot() {
    }

    static @NotNull State copy(@NotNull State source) {
        State copy = new State();
        copy.formatVersion = source.formatVersion;
        copy.layoutElement = source.layoutElement == null ? null : (Element) source.layoutElement.clone();
        copy.editorWindowCount = source.editorWindowCount;
        copy.selectedFilePath = source.selectedFilePath;
        copy.currentWindowIndex = source.currentWindowIndex;
        return copy;
    }

    static @NotNull State capture(@NotNull Project project) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        State state = new State();
        if (manager instanceof FileEditorManagerImpl impl) {
            // A write action would wait for background editor builds that hold a read lock.
            ApplicationManager.getApplication().runReadAction(() -> {
                state.layoutElement = SplittabEditorLayoutPlatformBridge.captureLayoutRoot(impl);
            });
        }
        state.editorWindowCount = manager.getWindows().length;
        VirtualFile[] selectedFiles = manager.getSelectedFiles();
        if (selectedFiles.length > 0) {
            state.selectedFilePath = selectedFiles[0].getPath();
        }
        EditorWindow currentWindow = manager.getCurrentWindow();
        EditorWindow[] windows = manager.getWindows();
        if (currentWindow != null) {
            for (int index = 0; index < windows.length; index++) {
                if (windows[index] == currentWindow) {
                    state.currentWindowIndex = index;
                    break;
                }
            }
        }
        return state;
    }

    static void restore(@NotNull Project project, @NotNull State state) {
        restore(project, state, null);
    }

    /** {@code focusFile} is reopened and focused after the layout instead of the captured selection. */
    static void restore(@NotNull Project project, @NotNull State state, @Nullable VirtualFile focusFile) {
        Runnable work = () -> restoreBlocking(project, state, focusFile);
        if (ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().executeOnPooledThread(work);
        } else {
            work.run();
        }
    }

    private static void restoreBlocking(@NotNull Project project, @NotNull State state, @Nullable VirtualFile focusFile) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        if (!(manager instanceof FileEditorManagerImpl impl)) {
            return;
        }
        int expectedWindows = state.editorWindowCount;
        if (state.layoutElement != null && SplittabEditorLayoutPlatformBridge.isAvailable()) {
            SplittabEditorLayoutPlatformBridge.restoreLayoutRoot(impl, state.layoutElement, expectedWindows);
        } else if (state.layoutElement != null) {
            ApplicationManager.getApplication().invokeAndWait(() ->
                    ApplicationManager.getApplication().runWriteAction(() ->
                            impl.loadState((Element) state.layoutElement.clone())
                    )
            );
        } else {
            ApplicationManager.getApplication().invokeAndWait(impl::closeAllFiles);
        }

        ApplicationManager.getApplication().invokeAndWait(() -> {
            if (project.isDisposed()) {
                return;
            }
            if (focusFile != null && focusFile.isValid()) {
                focusFileInCurrentWindow(manager, state, focusFile);
            } else {
                applySelection(manager, state);
            }
        });
    }

    private static void focusFileInCurrentWindow(
            @NotNull FileEditorManagerEx manager,
            @NotNull State state,
            @NotNull VirtualFile focusFile
    ) {
        EditorWindow[] windows = manager.getWindows();
        EditorWindow target = windowForFile(manager, focusFile);
        if (target == null && state.currentWindowIndex >= 0 && state.currentWindowIndex < windows.length) {
            target = windows[state.currentWindowIndex];
        }
        if (target == null) {
            target = manager.getCurrentWindow();
        }
        if (target != null) {
            manager.setCurrentWindow(target);
        }
        manager.openFile(focusFile, target, ComponentSubtabNavigation.nonBlockingOpenOptions(true, true));
    }

    private static void applySelection(@NotNull FileEditorManagerEx manager, @NotNull State state) {
        VirtualFile selected = state.selectedFilePath == null ? null : resolvePath(state.selectedFilePath);
        EditorWindow[] windows = manager.getWindows();
        EditorWindow focusWindow = null;
        if (state.currentWindowIndex >= 0 && state.currentWindowIndex < windows.length) {
            focusWindow = windows[state.currentWindowIndex];
        }
        if (selected != null && manager.isFileOpen(selected)) {
            EditorWindow selectedWindow = windowForFile(manager, selected);
            if (selectedWindow != null) {
                focusWindow = selectedWindow;
                manager.setCurrentWindow(selectedWindow);
                selectedWindow.setSelectedComposite(selected, true);
            } else {
                manager.openFile(selected, true);
            }
        } else if (focusWindow != null) {
            manager.setCurrentWindow(focusWindow);
        }
    }

    private static @Nullable EditorWindow windowForFile(@NotNull FileEditorManagerEx manager, @NotNull VirtualFile file) {
        for (EditorWindow window : manager.getWindows()) {
            if (window.isFileOpen(file)) {
                return window;
            }
        }
        return null;
    }

    private static @Nullable VirtualFile resolvePath(@NotNull String path) {
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
        if (file != null && file.isValid()) {
            return file;
        }
        return null;
    }
}
