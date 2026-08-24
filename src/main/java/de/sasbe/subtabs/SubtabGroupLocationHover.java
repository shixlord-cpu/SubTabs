package de.sasbe.subtabs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;

final class SubtabGroupLocationHover {
    private static final int SHOW_DELAY_MS = 0;
    private static final String HOVERED_LOCATION_ROW_KEY = "componentSubtabs.locationHoveredRow";
    private static final String SHOW_TIMER_KEY = "componentSubtabs.locationShowTimer";
    private static final String POPUP_KEY = "componentSubtabs.locationPopup";
    private static final String POPUP_PANEL_KEY = "componentSubtabs.locationPopupPanel";
    private static final String POPUP_ROW_KEY = "componentSubtabs.locationPopupRow";
    private static final String POPUP_MODE_KEY = "componentSubtabs.locationPopupMode";
    private static final String RENDERER_INSTALLED = "componentSubtabs.locationHoverRenderer";

    private enum PopupMode {
        LOCATION,
        GROUP_ROW
    }

    private SubtabGroupLocationHover() {
    }

    static void installRenderer(@NotNull JTree tree) {
        if (Boolean.TRUE.equals(tree.getClientProperty(RENDERER_INSTALLED))) {
            return;
        }

        TreeCellRenderer baseRenderer = tree.getCellRenderer();
        tree.setCellRenderer(new SubtabGroupTreeCellRenderer(baseRenderer));
        tree.putClientProperty(RENDERER_INSTALLED, Boolean.TRUE);
    }

    static boolean handleMouseMoved(
            @NotNull Project project,
            @NotNull JTree tree,
            @NotNull MouseEvent event
    ) {
        if (!SubtabProjectViewGrouping.isEnabled()) {
            return false;
        }

        Point screenPoint = event.getLocationOnScreen();
        if (isMouseOverPopup(tree, screenPoint)) {
            cancelShowTimer(tree);
            return true;
        }

        int row = tree.getRowForLocation(event.getX(), event.getY());
        if (isMouseOverLocationString(tree, row, event.getX())) {
            ComponentSubtabBarHover.onExit(tree);
            ComponentSubtabMainTabHover.onExit(tree);
            setHoveredLocationRow(tree, row);
            schedulePopup(project, tree, row, PopupMode.LOCATION);
            return true;
        }

        SubtabGroupProjectViewNode groupNode = groupNodeAt(tree, row);
        if (groupNode != null && isNoneStyle() && isMouseOverGroupRow(tree, row, event.getX())) {
            ComponentSubtabBarHover.onExit(tree);
            ComponentSubtabMainTabHover.onEnterGroup(project, groupNode.groupKey(), tree);
            setHoveredLocationRow(tree, row);
            schedulePopup(project, tree, row, PopupMode.GROUP_ROW);
            return true;
        }

        clearHoveredLocationRow(tree);
        cancelShowTimer(tree);
        hidePopup(tree);
        return false;
    }

    static boolean isMouseOverLocationString(@NotNull JTree tree, int row, int mouseX) {
        SubtabGroupProjectViewNode groupNode = groupNodeAt(tree, row);
        if (groupNode == null || groupNode.members().size() <= 1) {
            return false;
        }

        Rectangle bounds = tree.getRowBounds(row);
        if (bounds == null) {
            return false;
        }

        int locationStart = locationStartX(tree, bounds, groupNode.members().size());
        return mouseX >= locationStart && mouseX <= bounds.x + bounds.width;
    }

    static boolean isMouseOverGroupRow(@NotNull JTree tree, int row, int mouseX) {
        SubtabGroupProjectViewNode groupNode = groupNodeAt(tree, row);
        if (groupNode == null || groupNode.members().size() <= 1) {
            return false;
        }
        if (isMouseOverLocationString(tree, row, mouseX)) {
            return false;
        }

        Rectangle bounds = tree.getRowBounds(row);
        return bounds != null && mouseX >= bounds.x && mouseX <= bounds.x + bounds.width;
    }

    private static boolean isNoneStyle() {
        return SubtabsSettings.getInstance().getGroupTreeControlStyle() == SubtabGroupTreeControlStyle.NONE;
    }

    private static int locationStartX(@NotNull JTree tree, @NotNull Rectangle rowBounds, int fileCount) {
        String location = fileCount + " Dateien";
        FontMetrics metrics = tree.getFontMetrics(tree.getFont());
        int locationWidth = metrics.stringWidth(location);
        return rowBounds.x + rowBounds.width - locationWidth - JBUI.scale(8);
    }

    private static @Nullable SubtabGroupProjectViewNode groupNodeAt(@NotNull JTree tree, int row) {
        if (row < 0) {
            return null;
        }
        TreePath path = tree.getPathForRow(row);
        Object userObject = path == null ? null : TreeUtil.getLastUserObject(path);
        return userObject instanceof SubtabGroupProjectViewNode groupNode ? groupNode : null;
    }

    private static void schedulePopup(
            @NotNull Project project,
            @NotNull JTree tree,
            int row,
            @NotNull PopupMode mode
    ) {
        Integer popupRow = getPopupRow(tree);
        PopupMode popupMode = getPopupMode(tree);
        if (isPopupVisible(tree) && popupRow != null && popupRow == row && popupMode == mode) {
            return;
        }

        if (isPopupVisible(tree)) {
            hidePopup(tree);
        }

        cancelShowTimer(tree);

        Timer showTimer = new Timer(SHOW_DELAY_MS, event -> {
            if (row != getHoveredLocationRow(tree)) {
                return;
            }
            showPopup(project, tree, row, mode);
        });
        showTimer.setRepeats(false);
        tree.putClientProperty(SHOW_TIMER_KEY, showTimer);
        showTimer.start();
    }

    private static void showPopup(
            @NotNull Project project,
            @NotNull JTree tree,
            int row,
            @NotNull PopupMode mode
    ) {
        SubtabGroupProjectViewNode groupNode = groupNodeAt(tree, row);
        if (groupNode == null) {
            return;
        }

        hidePopup(tree);

        List<VirtualFile> files = groupNode.memberVirtualFiles().stream()
                .sorted(Comparator.comparing(VirtualFile::getName))
                .toList();
        Rectangle rowBounds = tree.getRowBounds(row);
        if (rowBounds == null) {
            return;
        }
        int fixedWidth = mode == PopupMode.GROUP_ROW ? rowBounds.width : 0;

        SubtabGroupFilePopupPanel panel = new SubtabGroupFilePopupPanel(
                project,
                files,
                fixedWidth,
                file -> {
                    ComponentSubtabProjectViewNavigation.openFromGroupFilePopup(project, file);
                    hidePopup(tree);
                    clearHoveredLocationRow(tree);
                },
                () -> {
                    hidePopup(tree);
                    clearHoveredLocationRow(tree);
                }
        );
        tree.putClientProperty(POPUP_PANEL_KEY, panel);

        Rectangle visible = tree.getVisibleRect();
        boolean showAbove = rowBounds.y > visible.y + visible.height / 2;
        Point showPoint = popupShowPoint(tree, rowBounds, files.size(), mode, panel, showAbove);

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, null)
                .setRequestFocus(false)
                .setCancelOnClickOutside(true)
                .setCancelOnOtherWindowOpen(true)
                .setCancelCallback(() -> {
                    hidePopup(tree);
                    clearHoveredLocationRow(tree);
                    return true;
                })
                .createPopup();

        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(@NotNull LightweightWindowEvent event) {
                tree.putClientProperty(POPUP_KEY, null);
                tree.putClientProperty(POPUP_PANEL_KEY, null);
                tree.putClientProperty(POPUP_ROW_KEY, null);
                tree.putClientProperty(POPUP_MODE_KEY, null);
                ComponentSubtabBarHover.onExit(panel);
                ComponentSubtabMainTabHover.onExit(panel);
            }
        });

        tree.putClientProperty(POPUP_KEY, popup);
        tree.putClientProperty(POPUP_ROW_KEY, row);
        tree.putClientProperty(POPUP_MODE_KEY, mode);
        popup.show(new RelativePoint(tree, showPoint));
    }

    private static @NotNull Point popupShowPoint(
            @NotNull JTree tree,
            @NotNull Rectangle rowBounds,
            int fileCount,
            @NotNull PopupMode mode,
            @NotNull SubtabGroupFilePopupPanel panel,
            boolean showAbove
    ) {
        int anchorX = mode == PopupMode.GROUP_ROW
                ? rowBounds.x
                : locationStartX(tree, rowBounds, fileCount);
        int anchorY = showAbove ? rowBounds.y : rowBounds.y + rowBounds.height;
        Point showPoint = new Point(anchorX, anchorY);
        if (showAbove) {
            Dimension preferred = panel.getPreferredSize();
            showPoint.y -= preferred.height;
        }
        return showPoint;
    }

    static void hidePopup(@NotNull JTree tree) {
        JBPopup popup = getPopup(tree);
        if (popup != null && popup.isVisible()) {
            popup.cancel();
        }
        tree.putClientProperty(POPUP_KEY, null);
        tree.putClientProperty(POPUP_PANEL_KEY, null);
        tree.putClientProperty(POPUP_ROW_KEY, null);
        tree.putClientProperty(POPUP_MODE_KEY, null);
    }

    static void handleTreeMouseExited(@NotNull JTree tree) {
        Timer timer = new Timer(100, event -> {
            Point pointer = MouseInfo.getPointerInfo().getLocation();
            if (isMouseOverPopup(tree, pointer)) {
                return;
            }
            if (isMouseOverAnyPopupTrigger(tree, pointer)) {
                return;
            }
            hidePopup(tree);
            clearHoveredLocationRow(tree);
        });
        timer.setRepeats(false);
        timer.start();
    }

    static void refreshPopupPresentation(@NotNull Project project, @NotNull JTree tree) {
        SubtabGroupFilePopupPanel panel = getPopupPanel(tree);
        if (panel == null || !panel.isShowing()) {
            return;
        }
        panel.refreshPresentation(project);
    }

    private static boolean isMouseOverAnyPopupTrigger(@NotNull JTree tree, @NotNull Point screenPoint) {
        Point treePoint = new Point(screenPoint);
        SwingUtilities.convertPointFromScreen(treePoint, tree);
        int row = tree.getRowForLocation(treePoint.x, treePoint.y);
        return isMouseOverLocationString(tree, row, treePoint.x)
                || (isNoneStyle() && isMouseOverGroupRow(tree, row, treePoint.x));
    }

    private static boolean isPopupVisible(@NotNull JTree tree) {
        JBPopup popup = getPopup(tree);
        return popup != null && popup.isVisible();
    }

    private static boolean isMouseOverPopup(@NotNull JTree tree, @NotNull Point screenPoint) {
        SubtabGroupFilePopupPanel panel = getPopupPanel(tree);
        if (panel == null || !panel.isShowing()) {
            return false;
        }
        try {
            Point origin = panel.getLocationOnScreen();
            Rectangle bounds = new Rectangle(origin, panel.getSize());
            return bounds.contains(screenPoint);
        } catch (java.awt.IllegalComponentStateException ignored) {
            return false;
        }
    }

    private static @Nullable JBPopup getPopup(@NotNull JTree tree) {
        Object value = tree.getClientProperty(POPUP_KEY);
        return value instanceof JBPopup popup ? popup : null;
    }

    private static @Nullable SubtabGroupFilePopupPanel getPopupPanel(@NotNull JTree tree) {
        Object value = tree.getClientProperty(POPUP_PANEL_KEY);
        return value instanceof SubtabGroupFilePopupPanel panel ? panel : null;
    }

    private static @Nullable Integer getPopupRow(@NotNull JTree tree) {
        Object value = tree.getClientProperty(POPUP_ROW_KEY);
        return value instanceof Integer row ? row : null;
    }

    private static @Nullable PopupMode getPopupMode(@NotNull JTree tree) {
        Object value = tree.getClientProperty(POPUP_MODE_KEY);
        return value instanceof PopupMode mode ? mode : null;
    }

    private static @Nullable Timer getShowTimer(@NotNull JTree tree) {
        Object value = tree.getClientProperty(SHOW_TIMER_KEY);
        return value instanceof Timer timer ? timer : null;
    }

    private static void cancelShowTimer(@NotNull JTree tree) {
        Timer timer = getShowTimer(tree);
        if (timer != null) {
            timer.stop();
            tree.putClientProperty(SHOW_TIMER_KEY, null);
        }
    }

    private static void setHoveredLocationRow(@NotNull JTree tree, int row) {
        Integer previous = getHoveredLocationRow(tree);
        if (previous != null && previous == row) {
            return;
        }
        tree.putClientProperty(HOVERED_LOCATION_ROW_KEY, row);
        tree.repaint();
    }

    private static void clearHoveredLocationRow(@NotNull JTree tree) {
        if (getHoveredLocationRow(tree) == null) {
            return;
        }
        tree.putClientProperty(HOVERED_LOCATION_ROW_KEY, null);
        tree.repaint();
    }

    static @Nullable Integer getHoveredLocationRow(@NotNull JTree tree) {
        Object value = tree.getClientProperty(HOVERED_LOCATION_ROW_KEY);
        return value instanceof Integer row ? row : null;
    }

    static void brightenLocationFragment(@NotNull SimpleColoredComponent colored) {
        for (SimpleColoredComponent.ColoredIterator iterator = colored.iterator(); iterator.hasNext(); ) {
            String fragment = iterator.next();
            if (fragment.contains("Dateien")) {
                iterator.setTextAttributes(SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }
        }
    }
}
