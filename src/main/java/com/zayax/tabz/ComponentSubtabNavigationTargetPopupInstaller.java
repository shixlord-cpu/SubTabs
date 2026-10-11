package com.zayax.tabz;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JList;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;

final class ComponentSubtabNavigationTargetPopupInstaller implements AppLifecycleListener {
    private static boolean listenerInstalled;

    @Override
    public void appStarted() {
        if (listenerInstalled) {
            return;
        }
        listenerInstalled = true;
        Toolkit.getDefaultToolkit().addAWTEventListener(
                ComponentSubtabNavigationTargetPopupInstaller::handleAwtEvent,
                AWTEvent.MOUSE_MOTION_EVENT_MASK
        );
    }

    private static void handleAwtEvent(@NotNull AWTEvent event) {
        if (!(event instanceof MouseEvent mouseEvent)) {
            return;
        }
        if (!ApplicationManager.getApplication().isDispatchThread()) {
            return;
        }
        if (!TabzSettings.getInstance().isTabzEnabled()
                && SubtabHoverView.isDisabled()) {
            return;
        }
        Component component = mouseEvent.getComponent();
        if (component == null) {
            return;
        }
        JList<?> list = findNavigationTargetList(component);
        if (list == null) {
            return;
        }
        ComponentSubtabNavigationTargetPopupHover.ensureInstalled(list);
        if (SubtabHoverView.isDisabled()) {
            return;
        }
        PointInList point = pointInList(list, mouseEvent);
        if (point == null) {
            ComponentSubtabNavigationTargetPopupHover.handleMouse(list, new java.awt.Point(-1, -1));
            return;
        }
        ComponentSubtabNavigationTargetPopupHover.handleMouse(list, point.point());
    }

    private static @Nullable PointInList pointInList(@NotNull JList<?> list, @NotNull MouseEvent event) {
        Component source = event.getComponent();
        java.awt.Point onList = SwingUtilities.convertPoint(source, event.getPoint(), list);
        if (!list.contains(onList)) {
            return null;
        }
        return new PointInList(onList);
    }

    private static @Nullable JList<?> findNavigationTargetList(@NotNull Component component) {
        for (Component each : UIUtil.uiTraverser(component).traverse()) {
            if (!(each instanceof JList<?> list)) {
                continue;
            }
            if (ComponentSubtabNavigationTargetPopupHover.isPopupNavigationList(list)) {
                return list;
            }
        }
        return null;
    }

    private record PointInList(@NotNull java.awt.Point point) {
    }
}
