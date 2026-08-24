package de.sasbe.subtabs;

import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManagerKeys;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.DockableEditor;
import com.intellij.openapi.fileEditor.impl.EditorComposite;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.MouseDragHelper;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.docking.DockContainer;
import com.intellij.ui.docking.DockManager;
import com.intellij.ui.docking.DragSession;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

final class ComponentSubtabEditorDragSupport {
    private ComponentSubtabEditorDragSupport() {
    }

    static void install(
            @NotNull Project project,
            @NotNull JComponent host,
            @NotNull Map<VirtualFile, JToggleButton> buttonsByFile,
            @NotNull AtomicBoolean ignoreNextClick
    ) {
        MouseDragHelper<JComponent> helper = new MouseDragHelper<>(project, host) {
            private VirtualFile dragFile;
            private JComponent dragSourceComponent;
            private Component dragUiAnchor;
            private DragSession dragSession;
            private TabInfo draggedTab;
            private EditorWindow dragWindow;
            private boolean sourceWasOpen;

            @Override
            protected boolean canStartDragging(
                    @NotNull JComponent dragComponent,
                    @NotNull Point dragComponentPoint
            ) {
                return findButtonAt(host, buttonsByFile, dragComponentPoint) != null;
            }

            @Override
            protected boolean isDragOut(
                    @NotNull MouseEvent event,
                    @NotNull Point dragToScreenPoint,
                    @NotNull Point startScreenPoint
            ) {
                return dragFile != null || findPressedFile(event) != null;
            }

            @Override
            protected void processMousePressed(@NotNull MouseEvent event) {
                JToggleButton button = findButtonAt(
                        host,
                        buttonsByFile,
                        SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), host)
                );
                dragSourceComponent = button != null ? button : host;
            }

            @Override
            protected void processDrag(
                    @NotNull MouseEvent event,
                    @NotNull Point dragToScreenPoint,
                    @NotNull Point startScreenPoint
            ) {
                // Subtabs always detach like editor tabs; in-bar reordering is not used.
            }

            @Override
            protected void processDragOut(
                    @NotNull MouseEvent event,
                    @NotNull Point dragToScreenPoint,
                    @NotNull Point startScreenPoint,
                    boolean justStarted
            ) {
                if (justStarted) {
                    if (!startDockSession(event)) {
                        cancelDragging();
                        return;
                    }
                    ignoreNextClick.set(true);
                }
                processDockEvent(event);
                event.consume();
            }

            @Override
            protected void processDragOutFinish(@NotNull MouseEvent event) {
                finishDockSession(event, false);
            }

            @Override
            protected void processDragOutCancel() {
                finishDockSession(null, true);
            }

            @Override
            protected boolean canFinishDragging(@NotNull JComponent component, @NotNull RelativePoint point) {
                return true;
            }

            private boolean startDockSession(@NotNull MouseEvent event) {
                VirtualFile file = findPressedFile(event);
                if (file == null) {
                    return false;
                }

                FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
                dragWindow = ComponentSubtabEditorLookup.windowForFileOrCurrent(manager, file);
                if (dragWindow == null) {
                    return false;
                }

                dragFile = file;
                sourceWasOpen = dragWindow.isFileOpen(file);
                draggedTab = sourceWasOpen ? findTabInfo(dragWindow, file) : null;
                JToggleButton button = buttonsByFile.get(file);
                dragSourceComponent = button != null ? button : host;
                dragUiAnchor = captureUiAnchor(event, host);

                Image preview = createPreviewImage(draggedTab, dragSourceComponent);
                Presentation presentation = new Presentation(
                        draggedTab != null ? draggedTab.getText() : file.getName()
                );
                if (draggedTab != null) {
                    presentation.setIcon(draggedTab.getIcon());
                }

                if (draggedTab != null) {
                    selectFallbackTabBeforeHide(dragWindow, file);
                    draggedTab.setHidden(true);
                }

                List<FileEditor> editors = sourceWasOpen
                        ? editorsOf(dragWindow, file)
                        : List.of();
                MouseEvent dockEvent = dockEvent(event);
                try {
                    dragSession = DockManager.getInstance(project).createDragSession(
                            dockEvent,
                            new DockableEditor(
                                    preview,
                                    file,
                                    presentation,
                                    dragWindow.getSize(),
                                    sourceWasOpen && dragWindow.isFilePinned(file),
                                    isSingletonEditorInWindow(editors),
                                    true
                            )
                    );
                    return true;
                } catch (RuntimeException | Error exception) {
                    restoreHiddenTab();
                    disposeDragSession(dragSession);
                    resetSession();
                    return false;
                }
            }

            private void finishDockSession(@Nullable MouseEvent event, boolean cancelled) {
                DragSession session = dragSession;
                boolean closedSourceTab = false;
                try {
                    if (session == null || cancelled || event == null) {
                        return;
                    }

                    MouseEvent dockEvent = dockEvent(event);
                    boolean copy = UIUtil.isControlKeyDown(dockEvent)
                            || session.getResponse(dockEvent) == DockContainer.ContentResponse.ACCEPT_COPY;
                    if (copy || !sourceWasOpen) {
                        restoreHiddenTab();
                    } else if (dragFile != null && dragWindow != null && dragWindow.isFileOpen(dragFile)) {
                        dragFile.putUserData(FileEditorManagerImpl.CLOSING_TO_REOPEN, true);
                        dragWindow.closeFile(dragFile);
                        closedSourceTab = true;
                    }

                    session.process(dockEvent);
                    if (dragFile != null) {
                        dragFile.putUserData(FileEditorManagerImpl.CLOSING_TO_REOPEN, null);
                    }
                } catch (RuntimeException | Error ignored) {
                    // DevicePoint throws Error when the original subtab was detached.
                    // The preview dialog and drop-over ghost must still be removed.
                } finally {
                    disposeDragSession(session);
                    if (!closedSourceTab) {
                        restoreHiddenTab();
                    }
                    Component uiAnchor = dragUiAnchor;
                    resetSession();
                    ComponentSubtabDragMouseEvents.repaintDragSurfaces(
                            host,
                            uiAnchor,
                            event != null ? event.getComponent() : null
                    );
                }
            }

            private void processDockEvent(@NotNull MouseEvent event) {
                if (dragSession == null) {
                    return;
                }
                try {
                    dragSession.process(dockEvent(event));
                } catch (RuntimeException | Error ignored) {
                    // Keep the session until mouse release so the preview can still be disposed.
                }
            }

            private @NotNull MouseEvent dockEvent(@NotNull MouseEvent event) {
                return ComponentSubtabDragMouseEvents.forDockSession(
                        event,
                        dragUiAnchor,
                        host
                );
            }

            private void restoreHiddenTab() {
                if (draggedTab != null) {
                    draggedTab.setHidden(false);
                }
            }

            private void disposeDragSession(@Nullable DragSession session) {
                if (session == null) {
                    return;
                }
                try {
                    session.cancel();
                } catch (RuntimeException | Error ignored) {
                    // Hiding the drag image is best-effort; never leave the session dangling.
                }
            }

            private void resetSession() {
                dragSession = null;
                draggedTab = null;
                dragWindow = null;
                dragFile = null;
                dragSourceComponent = null;
                dragUiAnchor = null;
                sourceWasOpen = false;
            }

            private @Nullable VirtualFile findPressedFile(@NotNull MouseEvent event) {
                if (dragFile != null) {
                    return dragFile;
                }
                return findFileAt(
                        host,
                        buttonsByFile,
                        SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), host)
                );
            }
        };
        helper.start();
    }

    private static @NotNull Component captureUiAnchor(@NotNull MouseEvent event, @NotNull JComponent host) {
        Window hostWindow = SwingUtilities.getWindowAncestor(host);
        if (hostWindow == null) {
            hostWindow = SwingUtilities.getWindowAncestor(event.getComponent());
        }
        if (hostWindow != null) {
            return hostWindow;
        }
        Component usable = ComponentSubtabDragMouseEvents.firstUsableForDevicePoint(event.getComponent(), host);
        return usable != null ? usable : host;
    }

    static @Nullable VirtualFile findFileAt(
            @NotNull JComponent host,
            @NotNull Map<VirtualFile, JToggleButton> buttonsByFile,
            @NotNull Point pointInHost
    ) {
        JToggleButton button = findButtonAt(host, buttonsByFile, pointInHost);
        if (button == null) {
            return null;
        }
        for (var entry : buttonsByFile.entrySet()) {
            if (entry.getValue() == button) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static @Nullable JToggleButton findButtonAt(
            @NotNull JComponent host,
            @NotNull Map<VirtualFile, JToggleButton> buttonsByFile,
            @NotNull Point pointInHost
    ) {
        Component component = SwingUtilities.getDeepestComponentAt(host, pointInHost.x, pointInHost.y);
        while (component != null && component != host) {
            if (component instanceof JToggleButton button && buttonsByFile.containsValue(button)) {
                return button;
            }
            component = component.getParent();
        }
        return null;
    }

    private static @Nullable TabInfo findTabInfo(@NotNull EditorWindow window, @NotNull VirtualFile file) {
        JBTabs tabs = window.getTabbedPane().getTabs();
        for (TabInfo info : tabs.getTabs()) {
            if (file.equals(info.getObject())) {
                return info;
            }
        }
        return null;
    }

    private static void selectFallbackTabBeforeHide(
            @NotNull EditorWindow window,
            @NotNull VirtualFile fileBeingHidden
    ) {
        EditorComposite selected = window.getSelectedComposite(false);
        if (selected == null || !fileBeingHidden.equals(selected.getFile())) {
            return;
        }

        for (TabInfo info : window.getTabbedPane().getTabs().getTabs()) {
            VirtualFile openFile = (VirtualFile) info.getObject();
            if (openFile != null && !fileBeingHidden.equals(openFile) && !info.isHidden()) {
                window.setSelectedComposite(openFile, false);
                return;
            }
        }
    }

    private static @NotNull Image createPreviewImage(
            @Nullable TabInfo draggedTab,
            @NotNull JComponent fallback
    ) {
        if (draggedTab != null) {
            try {
                return JBTabsImpl.getComponentImage(draggedTab);
            } catch (RuntimeException ignored) {
                // Fall through and snapshot the subtab button instead.
            }
        }

        int width = Math.max(fallback.getWidth(), 1);
        int height = Math.max(fallback.getHeight(), 1);
        BufferedImage image = UIUtil.createImage(fallback, width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            fallback.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static @NotNull List<FileEditor> editorsOf(
            @NotNull EditorWindow window,
            @NotNull VirtualFile file
    ) {
        EditorComposite composite = window.getComposite(file);
        return composite == null ? List.of() : composite.getAllEditors();
    }

    private static boolean isSingletonEditorInWindow(@NotNull List<FileEditor> editors) {
        for (FileEditor editor : editors) {
            if (FileEditorManagerKeys.SINGLETON_EDITOR_IN_WINDOW.get(editor, false)) {
                return true;
            }
        }
        return false;
    }
}
