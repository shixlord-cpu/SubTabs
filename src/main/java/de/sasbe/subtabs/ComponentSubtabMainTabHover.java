package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Marks the main tabs that belong to a hovered subtab or subtab group.
 *
 * <p>The platform keeps a single hovered tab per tab strip, so {@code JBTabsImpl.setHovered} can only
 * ever mark one tab and the remaining tabs of a group would stay untouched. The marking therefore
 * goes through {@link TabInfo#setTabColor}, the same channel the group colors use, and the previous
 * color is restored on exit.
 *
 * <p>Only tabs that are not the visible tab of their editor pane are marked: highlighting a tab whose
 * content is already on screen would just look like a glitch.
 */
final class ComponentSubtabMainTabHover {
    private static final String ACTIVE_HOVERS_KEY = "componentSubtabs.mainTabHovers";
    private static final double MIN_HOVER_WEIGHT = 0.45;

    private record Handle(
            @NotNull JBTabsImpl tabs,
            @NotNull TabInfo info,
            @Nullable Color previousColor
    ) {
    }

    private ComponentSubtabMainTabHover() {
    }

    static void onEnter(
            @NotNull Project project,
            @NotNull VirtualFile file,
            @NotNull JComponent source
    ) {
        if (SubtabHoverView.isDisabled()) {
            return;
        }
        onExit(source);
        applyHandles(source, collectHandles(project, tabFile -> tabFile.equals(file)));
    }

    static void onEnterGroup(
            @NotNull Project project,
            @NotNull String groupKey,
            @NotNull JComponent source
    ) {
        if (SubtabHoverView.isDisabled()) {
            return;
        }
        onExit(source);
        String targetMergeKey = SubtabProjectViewGrouping.mergeKey(groupKey);
        applyHandles(source, collectHandles(project, file -> belongsToMergeGroup(file, targetMergeKey)));
    }

    static void onExit(@NotNull JComponent source) {
        @SuppressWarnings("unchecked")
        List<Handle> handles = (List<Handle>) source.getClientProperty(ACTIVE_HOVERS_KEY);
        source.putClientProperty(ACTIVE_HOVERS_KEY, null);
        if (handles == null || handles.isEmpty()) {
            return;
        }

        for (Handle handle : handles) {
            handle.info().setTabColor(handle.previousColor());
        }
        repaint(handles);
    }

    private static void applyHandles(@NotNull JComponent source, @NotNull List<Handle> handles) {
        if (handles.isEmpty()) {
            return;
        }

        source.putClientProperty(ACTIVE_HOVERS_KEY, handles);
        for (Handle handle : handles) {
            handle.info().setTabColor(hoverTint(handle.previousColor()));
        }
        repaint(handles);
    }

    private static @NotNull Color hoverTint(@Nullable Color previousColor) {
        Color base = previousColor != null ? previousColor : UIUtil.getPanelBackground();
        Color hover = JBUI.CurrentTheme.EditorTabs.hoverBackground();
        // A tab color has to be opaque, so a translucent theme hover color is composited onto the color
        // the tab would show without the hover. The lower bound keeps the marking visible even with a
        // very transparent theme color, and it leaves a group-colored tab recognizable as such.
        double weight = Math.max(hover.getAlpha() / 255.0, MIN_HOVER_WEIGHT);
        return ColorUtil.mix(base, ColorUtil.withAlpha(hover, 1.0), weight);
    }

    private static void repaint(@NotNull List<Handle> handles) {
        Set<JBTabsImpl> touched = new LinkedHashSet<>();
        for (Handle handle : handles) {
            touched.add(handle.tabs());
        }
        for (JBTabsImpl tabs : touched) {
            tabs.revalidateAndRepaint(false);
        }
    }

    private static @NotNull List<Handle> collectHandles(
            @NotNull Project project,
            @NotNull Predicate<VirtualFile> filter
    ) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        List<Handle> handles = new ArrayList<>();
        Set<TabInfo> seen = new LinkedHashSet<>();

        for (EditorWindow window : manager.getWindows()) {
            JBTabs tabs = window.getTabbedPane().getTabs();
            if (!(tabs instanceof JBTabsImpl tabsImpl)) {
                continue;
            }

            TabInfo selected = tabsImpl.getSelectedInfo();
            for (TabInfo tabInfo : tabsImpl.getTabs()) {
                if (tabInfo.isHidden() || tabInfo == selected) {
                    continue;
                }
                if (!(tabInfo.getObject() instanceof VirtualFile file) || !filter.test(file)) {
                    continue;
                }
                if (!seen.add(tabInfo)) {
                    continue;
                }
                handles.add(new Handle(tabsImpl, tabInfo, tabInfo.getTabColor()));
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
}
