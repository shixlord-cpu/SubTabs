package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.intellij.ui.tabs.impl.TabLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.IllegalComponentStateException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

final class ComponentSubtabMainTabHover {
    private static final String ACTIVE_HOVERS_KEY = "componentSubtabs.mainTabHovers";

    private record Handle(@NotNull JBTabsImpl tabs, @NotNull TabLabel label) {
    }

    private ComponentSubtabMainTabHover() {
    }

    static void onEnter(
            @NotNull Project project,
            @NotNull VirtualFile file,
            @NotNull JComponent source
    ) {
        onExit(source);

        List<Handle> handles = findVisibleTabHandles(project, file);
        if (handles.isEmpty()) {
            return;
        }

        source.putClientProperty(ACTIVE_HOVERS_KEY, handles);
        for (Handle handle : handles) {
            handle.tabs().setHovered(handle.label());
        }
    }

    static void onExit(@NotNull JComponent source) {
        @SuppressWarnings("unchecked")
        List<Handle> handles = (List<Handle>) source.getClientProperty(ACTIVE_HOVERS_KEY);
        if (handles == null || handles.isEmpty()) {
            return;
        }

        Point pointer = MouseInfo.getPointerInfo().getLocation();
        for (Handle handle : handles) {
            if (isPointerOver(handle.label(), pointer)) {
                source.putClientProperty(ACTIVE_HOVERS_KEY, null);
                return;
            }
        }

        clear(handles);
        source.putClientProperty(ACTIVE_HOVERS_KEY, null);
    }

    private static @NotNull List<Handle> findVisibleTabHandles(
            @NotNull Project project,
            @NotNull VirtualFile file
    ) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        List<Handle> handles = new ArrayList<>();

        for (EditorWindow window : manager.getWindows()) {
            if (!window.isFileOpen(file)) {
                continue;
            }

            JBTabs tabs = window.getTabbedPane().getTabs();
            if (!(tabs instanceof JBTabsImpl tabsImpl)) {
                continue;
            }

            TabInfo tabInfo = findTabInfo(tabsImpl, file);
            if (tabInfo == null || tabInfo.isHidden()) {
                continue;
            }

            TabLabel label = tabsImpl.getTabLabel(tabInfo);
            if (label != null && label.isShowing()) {
                handles.add(new Handle(tabsImpl, label));
            }
        }
        return handles;
    }

    private static void clear(@NotNull List<Handle> handles) {
        for (Handle handle : handles) {
            handle.tabs().unHover(handle.label());
        }
    }

    private static boolean isPointerOver(@NotNull TabLabel label, @NotNull Point pointerOnScreen) {
        if (!label.isShowing()) {
            return false;
        }
        try {
            Point origin = label.getLocationOnScreen();
            return new Rectangle(origin, label.getSize()).contains(pointerOnScreen);
        } catch (IllegalComponentStateException ignored) {
            return false;
        }
    }

    private static @Nullable TabInfo findTabInfo(@NotNull JBTabsImpl tabs, @NotNull VirtualFile file) {
        for (TabInfo info : tabs.getTabs()) {
            if (file.equals(info.getObject())) {
                return info;
            }
        }
        return null;
    }
}
