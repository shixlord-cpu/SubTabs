package de.sasbe.subtabs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

final class ComponentSubtabMainTabColors {
    static final float FOCUSED_SELECTED_MIX = 0.38f;
    static final float UNFOCUSED_ACTIVE_MIX = 0.24f;
    static final float INACTIVE_MIX = 0.12f;

    private ComponentSubtabMainTabColors() {
    }

    static void refreshForFile(@NotNull Project project, @NotNull VirtualFile file) {
        if (project.isDisposed()) {
            return;
        }
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        Set<VirtualFile> groupOpenFiles = openGroupFiles(manager, file);
        if (groupOpenFiles.isEmpty()) {
            return;
        }

        if (SubtabGroupColors.isEnabled()) {
            SubtabGroupMainTabColorRegistry registry = SubtabGroupMainTabColorRegistry.getInstance(project);
            Set<VirtualFile> affectedFiles = new LinkedHashSet<>();
            collectRegistryEntries(manager, registry, affectedFiles, groupOpenFiles);
            for (VirtualFile affected : affectedFiles) {
                if (groupOpenFiles.contains(affected)) {
                    manager.updateFileColor(affected);
                }
            }
        }

        applyDirectTabColors(manager, groupOpenFiles);
        for (VirtualFile openFile : groupOpenFiles) {
            ComponentSubtabMainTabIcons.refreshFile(project, openFile);
        }
        SidetabsManager.refreshSeparatorBorders(project);
    }

    static void refresh(@NotNull Project project) {
        if (project.isDisposed()) {
            return;
        }

        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        SubtabGroupMainTabColorRegistry registry = SubtabGroupMainTabColorRegistry.getInstance(project);
        registry.clear();

        Set<VirtualFile> affectedFiles = new LinkedHashSet<>();
        if (SubtabGroupColors.isEnabled()) {
            collectRegistryEntries(manager, registry, affectedFiles);
        }

        for (VirtualFile file : affectedFiles) {
            manager.updateFileColor(file);
        }

        if (!SubtabGroupColors.isEnabled()) {
            clearDirectTabColors(manager);
            ComponentSubtabMainTabIcons.refresh(project);
            SidetabsManager.refreshSeparatorBorders(project);
            return;
        }

        applyDirectTabColors(manager, null);
        ComponentSubtabMainTabIcons.refresh(project);
        SidetabsManager.refreshSeparatorBorders(project);
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed()) {
                applyDirectTabColors(FileEditorManagerEx.getInstanceEx(project), null);
                ComponentSubtabMainTabIcons.refresh(project);
                SidetabsManager.refreshSeparatorBorders(project);
            }
        });
    }

    private static @NotNull Set<VirtualFile> openGroupFiles(
            @NotNull FileEditorManagerEx manager,
            @NotNull VirtualFile file
    ) {
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(file);
        if (match == null) {
            return Set.of();
        }
        Set<VirtualFile> open = new HashSet<>();
        for (ComponentRelatedFiles.Entry entry : match.relatedFiles()) {
            VirtualFile related = entry.file();
            if (manager.isFileOpen(related)) {
                open.add(related);
            }
        }
        return open;
    }

    private static void collectRegistryEntries(
            @NotNull FileEditorManagerEx manager,
            @NotNull SubtabGroupMainTabColorRegistry registry,
            @NotNull Set<VirtualFile> affectedFiles
    ) {
        collectRegistryEntries(manager, registry, affectedFiles, null);
    }

    private static void collectRegistryEntries(
            @NotNull FileEditorManagerEx manager,
            @NotNull SubtabGroupMainTabColorRegistry registry,
            @NotNull Set<VirtualFile> affectedFiles,
            @Nullable Set<VirtualFile> onlyFiles
    ) {
        EditorWindow currentWindow = manager.getCurrentWindow();
        Color background = UIUtil.getPanelBackground();

        for (EditorWindow window : manager.getWindows()) {
            JBTabs tabs = window.getTabbedPane().getTabs();
            if (!(tabs instanceof JBTabsImpl tabsImpl)) {
                continue;
            }

            TabInfo selected = tabsImpl.getSelectedInfo();
            boolean windowFocused = window == currentWindow;

            for (TabInfo tabInfo : tabsImpl.getTabs()) {
                if (!(tabInfo.getObject() instanceof VirtualFile file)) {
                    continue;
                }
                if (onlyFiles != null && !onlyFiles.contains(file)) {
                    continue;
                }

                Color groupColor = SubtabGroupColors.colorForFile(file);
                if (groupColor == null) {
                    continue;
                }

                boolean selectedTab = tabInfo == selected;
                float mix = selectedTab
                        ? (windowFocused ? FOCUSED_SELECTED_MIX : UNFOCUSED_ACTIVE_MIX)
                        : INACTIVE_MIX;
                Color tint = blend(groupColor, background, mix);
                registry.put(file, tint, mix);
                affectedFiles.add(file);
            }
        }

        if (affectedFiles.isEmpty()) {
            return;
        }

        if (onlyFiles == null) {
            for (VirtualFile openFile : manager.getOpenFiles()) {
                if (SubtabGroupColors.colorKey(openFile) != null) {
                    affectedFiles.add(openFile);
                }
            }
        }
    }

    private static void applyDirectTabColors(
            @NotNull FileEditorManagerEx manager,
            @Nullable Set<VirtualFile> onlyFiles
    ) {
        EditorWindow currentWindow = manager.getCurrentWindow();
        Color background = UIUtil.getPanelBackground();

        for (EditorWindow window : manager.getWindows()) {
            JBTabs tabs = window.getTabbedPane().getTabs();
            if (!(tabs instanceof JBTabsImpl tabsImpl)) {
                continue;
            }

            TabInfo selected = tabsImpl.getSelectedInfo();
            boolean windowFocused = window == currentWindow;

            for (TabInfo tabInfo : tabsImpl.getTabs()) {
                if (!(tabInfo.getObject() instanceof VirtualFile file)) {
                    continue;
                }
                if (onlyFiles != null && !onlyFiles.contains(file)) {
                    continue;
                }

                Color groupColor = SubtabGroupColors.colorForFile(file);
                if (groupColor == null) {
                    clearTab(tabInfo);
                    continue;
                }

                boolean selectedTab = tabInfo == selected;
                float mix = selectedTab
                        ? (windowFocused ? FOCUSED_SELECTED_MIX : UNFOCUSED_ACTIVE_MIX)
                        : INACTIVE_MIX;
                applyTabColor(tabInfo, blend(groupColor, background, mix));
            }

            if (onlyFiles == null) {
                tabsImpl.revalidateAndRepaint(false);
            } else {
                boolean touched = false;
                for (TabInfo tabInfo : tabsImpl.getTabs()) {
                    if (tabInfo.getObject() instanceof VirtualFile file && onlyFiles.contains(file)) {
                        touched = true;
                        break;
                    }
                }
                if (touched) {
                    tabsImpl.revalidateAndRepaint(false);
                }
            }
        }
    }

    private static void clearDirectTabColors(@NotNull FileEditorManagerEx manager) {
        for (EditorWindow window : manager.getWindows()) {
            JBTabs tabs = window.getTabbedPane().getTabs();
            if (!(tabs instanceof JBTabsImpl tabsImpl)) {
                continue;
            }
            for (TabInfo tabInfo : tabsImpl.getTabs()) {
                if (tabInfo.getObject() instanceof VirtualFile file
                        && SubtabGroupColors.colorKey(file) != null) {
                    clearTab(tabInfo);
                }
            }
            tabsImpl.revalidateAndRepaint(false);
        }
    }

    private static void applyTabColor(@NotNull TabInfo tabInfo, @NotNull Color tint) {
        if (!Objects.equals(tabInfo.getTabColor(), tint)) {
            tabInfo.setTabColor(tint);
        }
    }

    private static void clearTab(@NotNull TabInfo tabInfo) {
        if (tabInfo.getTabColor() != null) {
            tabInfo.setTabColor(null);
        }
    }

    static @NotNull Color blend(@NotNull Color groupColor, @NotNull Color background, float mix) {
        return com.intellij.ui.ColorUtil.mix(background, groupColor, mix);
    }
}
