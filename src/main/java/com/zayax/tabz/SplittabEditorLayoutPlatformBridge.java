package com.zayax.tabz;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.impl.EditorsSplitters;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Applies the same editor-splitter persistence the platform uses on project reload at runtime.
 * {@link FileEditorManagerImpl#loadState(org.jdom.Element)} only stores state for startup;
 * {@link EditorsSplitters#readExternal(org.jdom.Element)} plus {@code openFilesAsync} rebuilds panes.
 */
final class SplittabEditorLayoutPlatformBridge {
    private static final Logger LOG = Logger.getInstance(SplittabEditorLayoutPlatformBridge.class);

    private static final Method READ_EXTERNAL = findReadExternalMethod();
    private static final Method OPEN_FILES_ASYNC = findOpenFilesAsyncMethod();
    private static final Method WRITE_EXTERNAL_ELEMENT = findWriteExternalMethod();

    private SplittabEditorLayoutPlatformBridge() {
    }

    static boolean isAvailable() {
        return READ_EXTERNAL != null && OPEN_FILES_ASYNC != null && WRITE_EXTERNAL_ELEMENT != null;
    }

    static @Nullable Element captureLayoutRoot(@NotNull FileEditorManagerImpl impl) {
        Element fromManager = impl.getState();
        if (fromManager != null) {
            return (Element) fromManager.clone();
        }
        Element state = new Element("state");
        EditorsSplitters splitters = impl.getMainSplitters();
        try {
            WRITE_EXTERNAL_ELEMENT.invoke(splitters, state);
        } catch (ReflectiveOperationException exception) {
            LOG.error("Failed to capture editor layout via writeExternal", exception);
            return null;
        }
        return state;
    }

    static void restoreLayoutRoot(@NotNull FileEditorManagerImpl impl, @NotNull Element layoutRoot, int expectedEditorWindowCount) {
        if (!isAvailable()) {
            LOG.warn("Editor layout bridge unavailable; skipping layout restore");
            return;
        }
        EditorsSplitters splitters = impl.getMainSplitters();
        Element cloned = (Element) layoutRoot.clone();
        Object[] restoreJob = new Object[1];
        ApplicationManager.getApplication().invokeAndWait(() ->
                ApplicationManager.getApplication().runWriteAction(() -> {
                    impl.closeAllFiles();
                    try {
                        READ_EXTERNAL.invoke(splitters, cloned);
                        restoreJob[0] = OPEN_FILES_ASYNC.invoke(splitters, false);
                    } catch (ReflectiveOperationException exception) {
                        throw new RuntimeException(exception);
                    }
                })
        );

        if (restoreJob[0] == null) {
            LOG.error("Failed to start editor layout restore");
            return;
        }

        awaitLayoutRestore(impl, expectedEditorWindowCount);
    }

    private static void awaitLayoutRestore(@NotNull FileEditorManagerImpl impl, int expectedEditorWindowCount) {
        long deadlineMs = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadlineMs) {
            int windowCount = impl.getWindows().length;
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

    private static @Nullable Method findWriteExternalMethod() {
        for (Method method : EditorsSplitters.class.getDeclaredMethods()) {
            if (!"writeExternal".equals(method.getName())) {
                continue;
            }
            if (method.getParameterCount() == 1 && Element.class.isAssignableFrom(method.getParameterTypes()[0])) {
                method.setAccessible(true);
                return method;
            }
        }
        LOG.error("EditorsSplitters.writeExternal(Element) not found");
        return null;
    }
}
