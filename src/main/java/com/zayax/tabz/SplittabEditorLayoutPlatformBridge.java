package com.zayax.tabz;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorsSplitters;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Applies the same editor-splitter persistence the platform uses on project reload at runtime.
 */
final class SplittabEditorLayoutPlatformBridge {
    private static final Logger LOG = Logger.getInstance(SplittabEditorLayoutPlatformBridge.class);

    private static final Method READ_EXTERNAL = findReadExternalMethod();
    private static final Method OPEN_FILES_ASYNC = findOpenFilesAsyncMethod();

    private SplittabEditorLayoutPlatformBridge() {
    }

    static boolean isAvailable() {
        return READ_EXTERNAL != null && OPEN_FILES_ASYNC != null;
    }

    static void restoreLayoutRoot(
            @NotNull FileEditorManagerEx manager,
            @NotNull Element layoutRoot,
            int expectedEditorWindowCount
    ) {
        if (!InternalPlatformBridge.isFileEditorManagerImpl(manager)) {
            LOG.warn("Editor layout bridge requires FileEditorManager implementation");
            return;
        }
        if (READ_EXTERNAL == null || OPEN_FILES_ASYNC == null) {
            LOG.warn("Editor layout bridge unavailable; skipping layout restore");
            return;
        }
        Object splitters;
        try {
            splitters = manager.getClass().getMethod("getMainSplitters").invoke(manager);
        } catch (ReflectiveOperationException exception) {
            LOG.error("getMainSplitters unavailable", exception);
            return;
        }
        if (!(splitters instanceof EditorsSplitters editorsSplitters)) {
            return;
        }

        Element cloned = (Element) layoutRoot.clone();
        Object[] restoreJob = new Object[1];
        ApplicationManager.getApplication().invokeAndWait(() ->
                ApplicationManager.getApplication().runWriteAction(() -> {
                    manager.closeAllFiles();
                    try {
                        READ_EXTERNAL.invoke(editorsSplitters, cloned);
                        restoreJob[0] = OPEN_FILES_ASYNC.invoke(editorsSplitters, false);
                    } catch (ReflectiveOperationException exception) {
                        throw new RuntimeException(exception);
                    }
                })
        );

        if (restoreJob[0] == null) {
            LOG.error("Failed to start editor layout restore");
            return;
        }

        awaitLayoutRestore(manager, expectedEditorWindowCount);
    }

    private static void awaitLayoutRestore(@NotNull FileEditorManagerEx manager, int expectedEditorWindowCount) {
        long deadlineMs = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadlineMs) {
            int windowCount = manager.getWindows().length;
            if (expectedEditorWindowCount <= 0 || windowCount >= expectedEditorWindowCount) {
                return;
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static @Nullable Method findReadExternalMethod() {
        for (Method method : EditorsSplitters.class.getDeclaredMethods()) {
            if (!method.getName().startsWith("readExternal")) {
                continue;
            }
            if (method.getParameterCount() == 1 && Element.class.isAssignableFrom(method.getParameterTypes()[0])) {
                method.setAccessible(true);
                return method;
            }
        }
        LOG.error("EditorsSplitters.readExternal not found");
        return null;
    }

    private static @Nullable Method findOpenFilesAsyncMethod() {
        try {
            Method method = EditorsSplitters.class.getDeclaredMethod("openFilesAsync", boolean.class);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException exception) {
            LOG.error("EditorsSplitters.openFilesAsync not found", exception);
            return null;
        }
    }
}
