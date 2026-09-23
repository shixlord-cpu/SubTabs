package de.sasbe.subtabs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.intellij.ui.tabs.impl.TabLabel;
import com.intellij.util.IconUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.awt.Color;
import java.util.Objects;

/**
 * Tints the platform file-type icon on main editor tabs with the file group's color.
 * The icon shape is unchanged ({@link IconUtil#colorize}); only the color is adjusted.
 */
final class ComponentSubtabMainTabIcons {
    private static final Key<Boolean> DEFERRED_ICON_REFRESH_SCHEDULED =
            Key.create("componentSubtabs.deferredMainTabIconRefresh");

    private ComponentSubtabMainTabIcons() {
    }

    /**
     * Re-applies group tint after {@code FileEditorManager.updateFilePresentation}, which often
     * resets tab icons asynchronously when selection changes.
     */
    static void scheduleRefreshAfterPlatformUpdate(@NotNull Project project) {
        if (project.isDisposed() || !SubtabGroupColors.isEnabled()) {
            return;
        }
        if (Boolean.TRUE.equals(project.getUserData(DEFERRED_ICON_REFRESH_SCHEDULED))) {
            return;
        }
        project.putUserData(DEFERRED_ICON_REFRESH_SCHEDULED, Boolean.TRUE);
        ApplicationManager.getApplication().invokeLater(() -> {
            project.putUserData(DEFERRED_ICON_REFRESH_SCHEDULED, null);
            if (project.isDisposed() || !SubtabGroupColors.isEnabled()) {
                return;
            }
            refresh(project);
            ApplicationManager.getApplication().invokeLater(
                    () -> {
                        if (!project.isDisposed() && SubtabGroupColors.isEnabled()) {
                            refresh(project);
                        }
                    },
                    ModalityState.nonModal()
            );
        }, ModalityState.nonModal());
    }

    /** After project open / session restore, tabs and Java file icons appear after our first refresh. */
    static void scheduleStartupRefresh(@NotNull Project project) {
        if (project.isDisposed()) {
            return;
        }
        scheduleRefreshAfterPlatformUpdate(project);
        ApplicationManager.getApplication().invokeLater(
                () -> {
                    if (!project.isDisposed() && SubtabGroupColors.isEnabled()) {
                        refresh(project);
                        scheduleRefreshAfterPlatformUpdate(project);
                    }
                },
                ModalityState.nonModal()
        );
        DumbService.getInstance(project).runWhenSmart(() -> {
            if (!project.isDisposed() && SubtabGroupColors.isEnabled()) {
                refresh(project);
                scheduleRefreshAfterPlatformUpdate(project);
            }
        });
    }

    static void refresh(@NotNull Project project) {
        if (project.isDisposed()) {
            return;
        }

        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        if (!SubtabGroupColors.isEnabled()) {
            clearAll(manager, project);
            return;
        }

        applyAll(manager, project);
    }

    private static void applyAll(@NotNull FileEditorManagerEx manager, @NotNull Project project) {
        for (EditorWindow window : manager.getWindows()) {
            JBTabs tabs = window.getTabbedPane().getTabs();
            if (!(tabs instanceof JBTabsImpl tabsImpl)) {
                continue;
            }

            for (TabInfo tabInfo : tabsImpl.getTabs()) {
                if (!(tabInfo.getObject() instanceof VirtualFile file)) {
                    continue;
                }
                Color groupColor = SubtabGroupColors.colorForFile(file);
                if (groupColor == null) {
                    restoreStandardIcon(project, tabsImpl, tabInfo, file);
                    continue;
                }
                applyGroupColor(project, tabsImpl, tabInfo, file, groupColor);
            }

            tabsImpl.revalidateAndRepaint(false);
        }
    }

    static void refreshFile(@NotNull Project project, @NotNull VirtualFile file) {
        if (project.isDisposed() || !SubtabGroupColors.isEnabled()) {
            return;
        }
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        for (EditorWindow window : manager.getWindows()) {
            JBTabs tabs = window.getTabbedPane().getTabs();
            if (!(tabs instanceof JBTabsImpl tabsImpl)) {
                continue;
            }
            for (TabInfo tabInfo : tabsImpl.getTabs()) {
                if (!file.equals(tabInfo.getObject())) {
                    continue;
                }
                Color groupColor = SubtabGroupColors.colorForFile(file);
                if (groupColor == null) {
                    restoreStandardIcon(project, tabsImpl, tabInfo, file);
                } else {
                    applyGroupColor(project, tabsImpl, tabInfo, file, groupColor);
                }
            }
            tabsImpl.revalidateAndRepaint(false);
        }
    }

    private static void clearAll(@NotNull FileEditorManagerEx manager, @NotNull Project project) {
        for (EditorWindow window : manager.getWindows()) {
            JBTabs tabs = window.getTabbedPane().getTabs();
            if (!(tabs instanceof JBTabsImpl tabsImpl)) {
                continue;
            }

            for (TabInfo tabInfo : tabsImpl.getTabs()) {
                if (!(tabInfo.getObject() instanceof VirtualFile file)) {
                    continue;
                }
                if (SubtabGroupColors.colorKey(file) != null) {
                    restoreStandardIcon(project, tabsImpl, tabInfo, file);
                }
            }

            tabsImpl.revalidateAndRepaint(false);
        }
    }

    private static void applyGroupColor(
            @NotNull Project project,
            @NotNull JBTabsImpl tabsImpl,
            @NotNull TabInfo tabInfo,
            @NotNull VirtualFile file,
            @NotNull Color groupColor
    ) {
        Icon tinted = tintedIcon(project, file, groupColor);
        if (tinted == null) {
            return;
        }
        setTabIcon(tabsImpl, tabInfo, tinted);
    }

    private static void restoreStandardIcon(
            @NotNull Project project,
            @NotNull JBTabsImpl tabsImpl,
            @NotNull TabInfo tabInfo,
            @NotNull VirtualFile file
    ) {
        Icon base = standardFileIcon(project, file);
        if (base == null) {
            return;
        }
        setTabIcon(tabsImpl, tabInfo, base);
    }

    private static void setTabIcon(
            @NotNull JBTabsImpl tabsImpl,
            @NotNull TabInfo tabInfo,
            @NotNull Icon icon
    ) {
        if (!Objects.equals(tabInfo.getIcon(), icon)) {
            tabInfo.setIcon(icon);
        }
        TabLabel label = tabsImpl.getTabLabel(tabInfo);
        if (label != null) {
            label.setIcon(icon);
        }
    }

    static @Nullable Icon standardFileIcon(@NotNull Project project, @NotNull VirtualFile file) {
        return SubtabGroupFileIconProvider.uncoloredPlatformIcon(
                file,
                Iconable.ICON_FLAG_READ_STATUS,
                project
        );
    }

    private static @Nullable Icon tintedIcon(
            @NotNull Project project,
            @NotNull VirtualFile file,
            @NotNull Color groupColor
    ) {
        Icon base = standardFileIcon(project, file);
        if (base == null) {
            return null;
        }
        return IconUtil.colorize(base, groupColor);
    }
}
