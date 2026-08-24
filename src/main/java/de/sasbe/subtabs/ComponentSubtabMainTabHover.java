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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
        onEnterAny(project, List.of(file), source);
    }

    static void onEnterAny(
            @NotNull Project project,
            @NotNull Iterable<VirtualFile> files,
            @NotNull JComponent source
    ) {
        onExit(source);

        List<Handle> handles = new ArrayList<>();
        for (VirtualFile file : files) {
            handles.addAll(findVisibleTabHandles(project, file));
        }
        applyHandles(source, handles);
    }

    static void onEnterGroup(
            @NotNull Project project,
            @NotNull String groupKey,
            @NotNull JComponent source
    ) {
        onExit(source);
        applyHandles(source, findVisibleTabHandlesForGroup(project, groupKey));
    }

    private static void applyHandles(@NotNull JComponent source, @NotNull List<Handle> handles) {
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

    private static @NotNull List<Handle> findVisibleTabHandlesForGroup(
            @NotNull Project project,
            @NotNull String groupKey
    ) {
        String targetMergeKey = SubtabProjectViewGrouping.mergeKey(groupKey);
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        List<Handle> handles = new ArrayList<>();
        Set<TabLabel> seenLabels = new LinkedHashSet<>();

        for (EditorWindow window : manager.getWindows()) {
            JBTabs tabs = window.getTabbedPane().getTabs();
            if (!(tabs instanceof JBTabsImpl tabsImpl)) {
                continue;
            }

            for (TabInfo tabInfo : tabsImpl.getTabs()) {
                if (tabInfo.isHidden()) {
                    continue;
                }
                Object tabObject = tabInfo.getObject();
                if (!(tabObject instanceof VirtualFile file)) {
                    continue;
                }
                if (!belongsToMergeGroup(file, targetMergeKey)) {
                    continue;
                }

                TabLabel label = tabsImpl.getTabLabel(tabInfo);
                if (label == null || !label.isVisible() || !seenLabels.add(label)) {
                    continue;
                }
                handles.add(new Handle(tabsImpl, label));
            }
        }
        return handles;
    }

    private static boolean belongsToMergeGroup(
            @NotNull VirtualFile file,
            @NotNull String targetMergeKey
    ) {
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(file);
        if (match == null) {
            return false;
        }
        return targetMergeKey.equals(SubtabProjectViewGrouping.mergeKey(match.baseName()));
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
            if (label != null && label.isVisible()) {
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
