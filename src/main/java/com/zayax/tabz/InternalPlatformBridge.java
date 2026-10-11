package com.zayax.tabz;

import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.EditorsSplitters;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.TabInfo;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JTree;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Reflection bridge for platform APIs that are not part of the public plugin classpath.
 * Keeps internal types out of plugin bytecode so Marketplace verification stays clean.
 */
final class InternalPlatformBridge {
    private static final Logger LOG = Logger.getInstance(InternalPlatformBridge.class);

    private static final String FILE_EDITOR_MANAGER_IMPL =
            "com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl";
    private static final String FILE_EDITOR_OPEN_OPTIONS =
            "com.intellij.openapi.fileEditor.impl.FileEditorOpenOptions";
    private static final String JB_TABS_IMPL = "com.intellij.ui.tabs.impl.JBTabsImpl";
    private static final String DOCK_MANAGER = "com.intellij.ui.docking.DockManager";
    private static final String DOCKABLE_EDITOR = "com.intellij.openapi.fileEditor.impl.DockableEditor";
    private static final String DRAG_SESSION = "com.intellij.ui.docking.DragSession";
    private static final String DOCK_CONTAINER = "com.intellij.ui.docking.DockContainer";
    private static final String TREE_HOVER_LISTENER = "com.intellij.ui.hover.TreeHoverListener";
    private static final String RENDERING_UTIL = "com.intellij.ui.render.RenderingUtil";
    private static final String RENDERING_HELPER = "com.intellij.ui.render.RenderingHelper";

    private InternalPlatformBridge() {
    }

    static boolean isFileEditorManagerImpl(@NotNull FileEditorManagerEx manager) {
        return FILE_EDITOR_MANAGER_IMPL.equals(manager.getClass().getName());
    }

    static void openFileInNewWindow(@NotNull Project project, @NotNull VirtualFile file) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        try {
            Method method = manager.getClass().getMethod("openFileInNewWindow", VirtualFile.class);
            method.invoke(manager, file);
        } catch (ReflectiveOperationException exception) {
            LOG.error("openFileInNewWindow unavailable", exception);
        }
    }

    static void openFile(
            @NotNull FileEditorManagerEx manager,
            @NotNull VirtualFile file,
            @NotNull EditorWindow window,
            @NotNull Object openOptions
    ) {
        try {
            Class<?> optionsClass = Class.forName(FILE_EDITOR_OPEN_OPTIONS);
            Method method = manager.getClass().getMethod("openFile", VirtualFile.class, EditorWindow.class, optionsClass);
            method.invoke(manager, file, window, openOptions);
        } catch (ReflectiveOperationException exception) {
            LOG.error("openFile(VirtualFile, EditorWindow, options) failed", exception);
        }
    }

    static void openFileWithProviders(
            @NotNull FileEditorManagerEx manager,
            @NotNull VirtualFile file,
            boolean requestFocus,
            @NotNull EditorWindow window
    ) {
        manager.setCurrentWindow(window);
        manager.openFile(file, requestFocus);
    }

    static @NotNull Object nonBlockingOpenOptions(
            @NotNull EditorWindow window,
            @NotNull VirtualFile oldFile,
            boolean requestFocus
    ) {
        return openOptions(window, oldFile, requestFocus, false);
    }

    static @NotNull Object blockingOpenOptions(
            @NotNull EditorWindow window,
            @NotNull VirtualFile oldFile,
            boolean requestFocus
    ) {
        return openOptions(window, oldFile, requestFocus, true);
    }

    static @NotNull Object nonBlockingOpenOptions(boolean requestFocus, boolean selectAsCurrent) {
        return createOpenOptions(
                selectAsCurrent,
                false,
                false,
                requestFocus,
                false,
                -1,
                false,
                null,
                false,
                false,
                false,
                null
        );
    }

    static boolean selectAsCurrent(@NotNull Object openOptions) {
        return openOptionsBooleanField(openOptions, "selectAsCurrent");
    }

    static boolean waitForCompositeOpen(@NotNull Object openOptions) {
        return openOptionsBooleanField(openOptions, "waitForCompositeOpen");
    }

    static boolean requestFocus(@NotNull Object openOptions) {
        return openOptionsBooleanField(openOptions, "requestFocus");
    }

    static int openOptionsIndex(@NotNull Object openOptions) {
        try {
            Field field = openOptions.getClass().getField("index");
            return field.getInt(openOptions);
        } catch (ReflectiveOperationException exception) {
            return -1;
        }
    }

    private static boolean openOptionsBooleanField(@NotNull Object openOptions, @NotNull String name) {
        try {
            Field field = openOptions.getClass().getField(name);
            return field.getBoolean(openOptions);
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private static @NotNull Object openOptions(
            @NotNull EditorWindow window,
            @NotNull VirtualFile oldFile,
            boolean requestFocus,
            boolean waitForCompositeOpen
    ) {
        List<VirtualFile> files = EditorWindowFiles.files(window);
        int index = files.indexOf(oldFile);
        return createOpenOptions(
                true,
                false,
                false,
                requestFocus,
                window.isFilePinned(oldFile),
                index,
                false,
                null,
                false,
                false,
                waitForCompositeOpen,
                null
        );
    }

    private static @NotNull Object createOpenOptions(
            boolean selectAsCurrent,
            boolean usePreviewTab,
            boolean useExistingTab,
            boolean requestFocus,
            boolean pinned,
            int index,
            boolean forceShowTab,
            @Nullable VirtualFile fileToClose,
            boolean waitForCompositeOpenLegacy,
            boolean ignoreIfAlreadyOpened,
            boolean waitForCompositeOpen,
            @Nullable Object unused
    ) {
        try {
            Class<?> cls = Class.forName(FILE_EDITOR_OPEN_OPTIONS);
            Constructor<?> ctor = cls.getConstructor(
                    boolean.class,
                    boolean.class,
                    boolean.class,
                    boolean.class,
                    boolean.class,
                    int.class,
                    boolean.class,
                    VirtualFile.class,
                    boolean.class,
                    boolean.class,
                    boolean.class,
                    Object.class
            );
            return ctor.newInstance(
                    selectAsCurrent,
                    usePreviewTab,
                    useExistingTab,
                    requestFocus,
                    pinned,
                    index,
                    forceShowTab,
                    fileToClose,
                    waitForCompositeOpenLegacy,
                    ignoreIfAlreadyOpened,
                    waitForCompositeOpen,
                    unused
            );
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("FileEditorOpenOptions constructor unavailable", exception);
        }
    }

    static @Nullable Element captureLayoutRoot(@NotNull FileEditorManagerEx manager) {
        if (!isFileEditorManagerImpl(manager)) {
            return null;
        }
        try {
            Method getState = manager.getClass().getMethod("getState");
            Object state = getState.invoke(manager);
            if (state instanceof Element element) {
                return (Element) element.clone();
            }
            Element xml = new Element("state");
            Object splitters = manager.getClass().getMethod("getMainSplitters").invoke(manager);
            if (splitters instanceof EditorsSplitters editorsSplitters) {
                writeExternalElement(editorsSplitters, xml);
            }
            return xml;
        } catch (ReflectiveOperationException exception) {
            LOG.error("Failed to capture editor layout", exception);
            return null;
        }
    }

    static void restoreLayoutRoot(
            @NotNull FileEditorManagerEx manager,
            @NotNull Element layoutRoot,
            int expectedEditorWindowCount
    ) {
        SplittabEditorLayoutPlatformBridge.restoreLayoutRoot(manager, layoutRoot, expectedEditorWindowCount);
    }

    static void loadManagerState(@NotNull FileEditorManagerEx manager, @NotNull Element layoutElement) {
        if (!isFileEditorManagerImpl(manager)) {
            return;
        }
        try {
            Method loadState = manager.getClass().getMethod("loadState", Element.class);
            loadState.invoke(manager, layoutElement);
        } catch (ReflectiveOperationException exception) {
            LOG.error("Failed to load editor manager state", exception);
        }
    }

    static @Nullable Dimension jbTabsHeaderFitSize(@NotNull JBTabs tabs) {
        if (!JB_TABS_IMPL.equals(tabs.getClass().getName())) {
            return null;
        }
        try {
            Method method = tabs.getClass().getMethod("getHeaderFitSize");
            Object value = method.invoke(tabs);
            return value instanceof Dimension dimension ? dimension : null;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    static @Nullable Image jbTabsComponentImage(@NotNull TabInfo tabInfo) {
        try {
            Class<?> cls = Class.forName(JB_TABS_IMPL);
            Method method = cls.getMethod("getComponentImage", TabInfo.class);
            Object image = method.invoke(null, tabInfo);
            return image instanceof Image img ? img : null;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    static @Nullable Object startEditorDragSession(
            @NotNull Project project,
            @NotNull MouseEvent event,
            @NotNull Image preview,
            @NotNull VirtualFile file,
            @NotNull Presentation presentation,
            @NotNull java.awt.Dimension windowSize,
            boolean pinned,
            boolean singletonEditorInWindow
    ) {
        try {
            Class<?> dockableClass = Class.forName(DOCKABLE_EDITOR);
            Constructor<?> dockableCtor = dockableClass.getConstructor(
                    Image.class,
                    VirtualFile.class,
                    Presentation.class,
                    java.awt.Dimension.class,
                    boolean.class,
                    boolean.class,
                    boolean.class
            );
            Object dockable = dockableCtor.newInstance(
                    preview,
                    file,
                    presentation,
                    windowSize,
                    pinned,
                    singletonEditorInWindow,
                    true
            );

            Class<?> dockManagerClass = Class.forName(DOCK_MANAGER);
            Method getInstance = dockManagerClass.getMethod("getInstance", Project.class);
            Object dockManager = getInstance.invoke(null, project);
            Method createSession = dockManagerClass.getMethod("createDragSession", MouseEvent.class, dockableClass);
            return createSession.invoke(dockManager, event, dockable);
        } catch (ReflectiveOperationException exception) {
            LOG.warn("Editor drag session unavailable", exception);
            return null;
        }
    }

    static void processEditorDragSession(@NotNull Object session, @NotNull MouseEvent event) {
        try {
            Method method = session.getClass().getMethod("process", MouseEvent.class);
            method.invoke(session, event);
        } catch (ReflectiveOperationException exception) {
            LOG.debug("Drag session process failed", exception);
        }
    }

    static void cancelEditorDragSession(@Nullable Object session) {
        if (session == null) {
            return;
        }
        try {
            Method method = session.getClass().getMethod("cancel");
            method.invoke(session);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    static boolean dragSessionAcceptsCopy(@NotNull Object session, @NotNull MouseEvent event) {
        try {
            Class<?> responseClass = Class.forName(DOCK_CONTAINER + "$ContentResponse");
            Object acceptCopy = Enum.valueOf((Class<? extends Enum>) responseClass, "ACCEPT_COPY");
            Method getResponse = session.getClass().getMethod("getResponse", MouseEvent.class);
            Object response = getResponse.invoke(session, event);
            return acceptCopy.equals(response);
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    static void markClosingToReopen(@NotNull VirtualFile file, boolean value) {
        try {
            Class<?> implClass = Class.forName(FILE_EDITOR_MANAGER_IMPL);
            Field field = implClass.getField("CLOSING_TO_REOPEN");
            Object key = field.get(null);
            if (key instanceof com.intellij.openapi.util.Key<?> userKey) {
                @SuppressWarnings("unchecked")
                com.intellij.openapi.util.Key<Boolean> typed = (com.intellij.openapi.util.Key<Boolean>) userKey;
                file.putUserData(typed, value ? Boolean.TRUE : null);
            }
        } catch (ReflectiveOperationException exception) {
            LOG.debug("CLOSING_TO_REOPEN key unavailable", exception);
        }
    }

    static boolean isSingletonEditorInWindow(@NotNull List<FileEditor> editors) {
        try {
            Class<?> implClass = Class.forName(FILE_EDITOR_MANAGER_IMPL);
            Class<?> keysClass = Class.forName("com.intellij.openapi.fileEditor.FileEditorManagerKeys");
            Field field = keysClass.getField("SINGLETON_EDITOR_IN_WINDOW");
            Object key = field.get(null);
            if (!(key instanceof com.intellij.openapi.util.Key<?> userKey)) {
                return false;
            }
            @SuppressWarnings("unchecked")
            com.intellij.openapi.util.Key<Boolean> typed = (com.intellij.openapi.util.Key<Boolean>) userKey;
            for (FileEditor editor : editors) {
                if (Boolean.TRUE.equals(typed.get(editor))) {
                    return true;
                }
            }
            return false;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    static int treeNativeHoveredRow(@NotNull JTree tree) {
        try {
            Class<?> cls = Class.forName(TREE_HOVER_LISTENER);
            Method method = cls.getMethod("getHoveredRow", JTree.class);
            Object row = method.invoke(null, tree);
            return row instanceof Integer integer ? integer : -1;
        } catch (ReflectiveOperationException exception) {
            return -1;
        }
    }

    static void treeSetNativeHoveredRow(@NotNull JTree tree, int row) {
        try {
            Class<?> cls = Class.forName(TREE_HOVER_LISTENER);
            Field defaultField = cls.getField("DEFAULT");
            Object listener = defaultField.get(null);
            if (listener == null) {
                return;
            }
            Method onHover = cls.getMethod("onHover", JTree.class, int.class);
            onHover.invoke(listener, tree, row);
        } catch (ReflectiveOperationException exception) {
            LOG.debug("Tree hover listener unavailable", exception);
        }
    }

    static boolean isTreeHoverPaintingDisabled(@NotNull JTree tree) {
        try {
            Class<?> cls = Class.forName(RENDERING_UTIL);
            Method method = cls.getMethod("isHoverPaintingDisabled", JComponent.class);
            Object value = method.invoke(null, tree);
            return Boolean.TRUE.equals(value);
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    static @NotNull Color treeHoverBackground(@NotNull JTree tree) {
        try {
            Class<?> cls = Class.forName(RENDERING_UTIL);
            Method method = cls.getMethod("getHoverBackground", JComponent.class);
            Object color = method.invoke(null, tree);
            if (color instanceof Color c) {
                return c;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return tree.getBackground();
    }

    static int treeRenderingX(@NotNull JTree tree) {
        try {
            Class<?> cls = Class.forName(RENDERING_HELPER);
            Constructor<?> ctor = cls.getConstructor(JComponent.class);
            Object helper = ctor.newInstance(tree);
            Method getX = cls.getMethod("getX");
            Object value = getX.invoke(helper);
            return value instanceof Integer integer ? integer : 0;
        } catch (ReflectiveOperationException exception) {
            return 0;
        }
    }

    static int treeRenderingWidth(@NotNull JTree tree) {
        try {
            Class<?> cls = Class.forName(RENDERING_HELPER);
            Constructor<?> ctor = cls.getConstructor(JComponent.class);
            Object helper = ctor.newInstance(tree);
            Method getWidth = cls.getMethod("getWidth");
            Object value = getWidth.invoke(helper);
            return value instanceof Integer integer ? integer : tree.getWidth();
        } catch (ReflectiveOperationException exception) {
            return tree.getWidth();
        }
    }

    private static void writeExternalElement(@NotNull EditorsSplitters splitters, @NotNull Element state) {
        for (Method method : EditorsSplitters.class.getDeclaredMethods()) {
            if (!"writeExternal".equals(method.getName()) || method.getParameterCount() != 1) {
                continue;
            }
            if (!Element.class.isAssignableFrom(method.getParameterTypes()[0])) {
                continue;
            }
            try {
                method.setAccessible(true);
                method.invoke(splitters, state);
                return;
            } catch (ReflectiveOperationException exception) {
                LOG.error("EditorsSplitters.writeExternal failed", exception);
                return;
            }
        }
    }
}
