package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.intellij.ui.tabs.impl.TabLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.IllegalComponentStateException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class ComponentSubtabMainTabSelectPopup {
    private static final int SHOW_DELAY_MS = 0;
    private static final String INSTALLED = "componentSubtabs.mainTabSelectPopup";
    private static final String TAB_FILE_KEY = "componentSubtabs.mainTabSelectPopup.file";
    private static final String SHOW_TIMER_KEY = "componentSubtabs.mainTabSelectPopup.timer";
    private static final String POPUP_KEY = "componentSubtabs.mainTabSelectPopup.popup";
    private static final String POPUP_PANEL_KEY = "componentSubtabs.mainTabSelectPopup.panel";

    private ComponentSubtabMainTabSelectPopup() {
    }

    static void installOn(@NotNull Project project) {
        if (project.isDisposed() || !SubtabsSettings.getInstance().isSubtabsActive()) {
            return;
        }
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        Set<TabLabel> seen = new LinkedHashSet<>();
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
                TabLabel label = tabsImpl.getTabLabel(tabInfo);
                if (label == null || !seen.add(label)) {
                    continue;
                }
                attach(project, label, file);
            }
        }
    }

    private static void attach(
            @NotNull Project project,
            @NotNull TabLabel label,
            @NotNull VirtualFile tabFile
    ) {
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(tabFile);
        if (match == null || match.relatedFiles().size() < 2) {
            return;
        }
        if (Boolean.TRUE.equals(label.getClientProperty(INSTALLED))) {
            label.putClientProperty(TAB_FILE_KEY, tabFile);
            return;
        }

        label.putClientProperty(INSTALLED, Boolean.TRUE);
        label.putClientProperty(TAB_FILE_KEY, tabFile);
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                schedulePopup(project, label, tabFile, match);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                scheduleExitCheck(label);
            }
        });
        label.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                Point screenPoint = event.getLocationOnScreen();
                if (isMouseOverPopup(label, screenPoint)) {
                    cancelShowTimer(label);
                }
            }
        });
    }

    private static void schedulePopup(
            @NotNull Project project,
            @NotNull TabLabel label,
            @NotNull VirtualFile tabFile,
            @NotNull ComponentRelatedFiles.Match match
    ) {
        Point screenPoint = MouseInfo.getPointerInfo().getLocation();
        if (isMouseOverPopup(label, screenPoint)) {
            cancelShowTimer(label);
            return;
        }

        if (isPopupVisible(label)) {
            return;
        }

        cancelShowTimer(label);
        Timer showTimer = new Timer(SHOW_DELAY_MS, event -> {
            if (!isPointerOver(label)) {
                return;
            }
            showPopup(project, label, tabFile, match);
        });
        showTimer.setRepeats(false);
        label.putClientProperty(SHOW_TIMER_KEY, showTimer);
        showTimer.start();
    }

    private static void showPopup(
            @NotNull Project project,
            @NotNull TabLabel label,
            @NotNull VirtualFile tabFile,
            @NotNull ComponentRelatedFiles.Match match
    ) {
        hidePopup(label);

        List<VirtualFile> files = match.relatedFiles().stream()
                .map(ComponentRelatedFiles.Entry::file)
                .sorted(Comparator.comparing(VirtualFile::getName))
                .toList();
        int fixedWidth = label.getWidth();

        SubtabGroupFilePopupPanel panel = new SubtabGroupFilePopupPanel(
                project,
                files,
                tabFile,
                fixedWidth,
                file -> {
                    openFromPopup(project, tabFile, file);
                    hidePopup(label);
                },
                () -> hidePopup(label)
        );
        label.putClientProperty(POPUP_PANEL_KEY, panel);

        Point showPoint = popupShowPoint(label, panel);
        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, null)
                .setRequestFocus(false)
                .setCancelOnClickOutside(true)
                .setCancelOnOtherWindowOpen(true)
                .setCancelCallback(() -> {
                    hidePopup(label);
                    return true;
                })
                .createPopup();

        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(@NotNull LightweightWindowEvent event) {
                label.putClientProperty(POPUP_KEY, null);
                label.putClientProperty(POPUP_PANEL_KEY, null);
                ComponentSubtabBarHover.onExit(panel);
                ComponentSubtabMainTabHover.onExit(panel);
            }
        });

        label.putClientProperty(POPUP_KEY, popup);
        popup.show(new RelativePoint(label, showPoint));
    }

    private static void openFromPopup(
            @NotNull Project project,
            @NotNull VirtualFile anchorFile,
            @NotNull VirtualFile targetFile
    ) {
        if (anchorFile.equals(targetFile)) {
            FileEditorManager.getInstance(project).openFile(targetFile, true);
            return;
        }
        ComponentSubtabNavigation.switchInSelectedEditor(project, anchorFile, targetFile, true);
    }

    private static @NotNull Point popupShowPoint(@NotNull TabLabel label, @NotNull SubtabGroupFilePopupPanel panel) {
        int width = Math.max(label.getWidth(), panel.getPreferredSize().width);
        Point showPoint = new Point(0, label.getHeight());
        Dimension preferred = panel.getPreferredSize();
        if (preferred.width != width) {
            panel.setPreferredSize(new Dimension(width, preferred.height));
            panel.setMinimumSize(new Dimension(width, preferred.height));
            panel.setMaximumSize(new Dimension(width, preferred.height));
        }
        return showPoint;
    }

    static void refreshPopupPresentation(@NotNull Project project) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        for (EditorWindow window : manager.getWindows()) {
            JBTabs tabs = window.getTabbedPane().getTabs();
            if (!(tabs instanceof JBTabsImpl tabsImpl)) {
                continue;
            }
            for (TabInfo tabInfo : tabsImpl.getTabs()) {
                TabLabel label = tabsImpl.getTabLabel(tabInfo);
                if (label == null) {
                    continue;
                }
                SubtabGroupFilePopupPanel panel = getPopupPanel(label);
                if (panel == null || !panel.isShowing()) {
                    continue;
                }
                Object tabObject = tabInfo.getObject();
                VirtualFile highlighted = tabObject instanceof VirtualFile file ? file : null;
                panel.refreshPresentation(project, highlighted);
            }
        }
    }

    static void hideAllPopups(@NotNull Project project) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        for (EditorWindow window : manager.getWindows()) {
            JBTabs tabs = window.getTabbedPane().getTabs();
            if (!(tabs instanceof JBTabsImpl tabsImpl)) {
                continue;
            }
            for (TabInfo tabInfo : tabsImpl.getTabs()) {
                TabLabel label = tabsImpl.getTabLabel(tabInfo);
                if (label != null) {
                    hidePopup(label);
                    cancelShowTimer(label);
                }
            }
        }
    }

    private static void scheduleExitCheck(@NotNull TabLabel label) {
        Timer timer = new Timer(100, event -> {
            Point pointer = MouseInfo.getPointerInfo().getLocation();
            if (isMouseOverPopup(label, pointer) || isPointerOver(label)) {
                return;
            }
            hidePopup(label);
            cancelShowTimer(label);
        });
        timer.setRepeats(false);
        timer.start();
    }

    private static void hidePopup(@NotNull TabLabel label) {
        JBPopup popup = getPopup(label);
        if (popup != null && popup.isVisible()) {
            popup.cancel();
        }
        label.putClientProperty(POPUP_KEY, null);
        label.putClientProperty(POPUP_PANEL_KEY, null);
    }

    private static void cancelShowTimer(@NotNull TabLabel label) {
        Timer timer = getShowTimer(label);
        if (timer != null) {
            timer.stop();
            label.putClientProperty(SHOW_TIMER_KEY, null);
        }
    }

    private static boolean isPopupVisible(@NotNull TabLabel label) {
        JBPopup popup = getPopup(label);
        return popup != null && popup.isVisible();
    }

    private static boolean isMouseOverPopup(@NotNull TabLabel label, @NotNull Point screenPoint) {
        SubtabGroupFilePopupPanel panel = getPopupPanel(label);
        if (panel == null || !panel.isShowing()) {
            return false;
        }
        try {
            Point origin = panel.getLocationOnScreen();
            return new Rectangle(origin, panel.getSize()).contains(screenPoint);
        } catch (IllegalComponentStateException ignored) {
            return false;
        }
    }

    private static boolean isPointerOver(@NotNull Component component) {
        if (!component.isShowing()) {
            return false;
        }
        try {
            Point origin = component.getLocationOnScreen();
            Point pointer = MouseInfo.getPointerInfo().getLocation();
            return new Rectangle(origin, component.getSize()).contains(pointer);
        } catch (IllegalComponentStateException ignored) {
            return false;
        }
    }

    private static @Nullable JBPopup getPopup(@NotNull TabLabel label) {
        Object value = label.getClientProperty(POPUP_KEY);
        return value instanceof JBPopup popup ? popup : null;
    }

    private static @Nullable SubtabGroupFilePopupPanel getPopupPanel(@NotNull TabLabel label) {
        Object value = label.getClientProperty(POPUP_PANEL_KEY);
        return value instanceof SubtabGroupFilePopupPanel panel ? panel : null;
    }

    private static @Nullable Timer getShowTimer(@NotNull TabLabel label) {
        Object value = label.getClientProperty(SHOW_TIMER_KEY);
        return value instanceof Timer timer ? timer : null;
    }
}
