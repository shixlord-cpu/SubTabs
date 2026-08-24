package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Component;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;

final class ComponentSubtabDragMouseEvents {
    private ComponentSubtabDragMouseEvents() {
    }

    static @NotNull MouseEvent forDockSession(@NotNull MouseEvent event, Component @Nullable ... fallbacks) {
        Component target = firstUsableForDevicePoint(event.getComponent(), fallbacks);
        if (target == null || target == event.getComponent()) {
            return event;
        }
        return retarget(event, target);
    }

    static @Nullable Component firstUsableForDevicePoint(
            @Nullable Component primary,
            Component @Nullable ... fallbacks
    ) {
        if (isUsableForDevicePoint(primary)) {
            return primary;
        }
        if (fallbacks != null) {
            for (Component fallback : fallbacks) {
                if (isUsableForDevicePoint(fallback)) {
                    return fallback;
                }
                Window window = windowOf(fallback);
                if (isUsableForDevicePoint(window)) {
                    return window;
                }
            }
        }
        Window window = windowOf(primary);
        return isUsableForDevicePoint(window) ? window : primary;
    }

    static boolean isUsableForDevicePoint(@Nullable Component component) {
        return component != null
                && component.isDisplayable()
                && component.getGraphicsConfiguration() != null;
    }

    static void repaintDragSurfaces(Component @Nullable ... surfaces) {
        if (surfaces == null) {
            return;
        }
        for (Component surface : surfaces) {
            if (surface == null) {
                continue;
            }
            Window window = windowOf(surface);
            if (window != null) {
                window.repaint();
            } else {
                surface.repaint();
            }
        }
    }

    static @NotNull MouseEvent retarget(@NotNull MouseEvent event, @NotNull Component target) {
        if (event.getComponent() == target) {
            return event;
        }

        Point point = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), target);
        int screenX = event.getX();
        int screenY = event.getY();
        boolean hasScreenCoordinates = false;
        try {
            screenX = event.getXOnScreen();
            screenY = event.getYOnScreen();
            hasScreenCoordinates = true;
        } catch (IllegalComponentStateException ignored) {
            // Tests and not-yet-showing components have no screen location.
        }

        if (hasScreenCoordinates) {
            return new MouseEvent(
                    target,
                    event.getID(),
                    event.getWhen(),
                    event.getModifiersEx(),
                    point.x,
                    point.y,
                    screenX,
                    screenY,
                    event.getClickCount(),
                    event.isPopupTrigger(),
                    event.getButton()
            );
        }

        return new MouseEvent(
                target,
                event.getID(),
                event.getWhen(),
                event.getModifiersEx(),
                point.x,
                point.y,
                event.getClickCount(),
                event.isPopupTrigger(),
                event.getButton()
        );
    }

    private static @Nullable Window windowOf(@Nullable Component component) {
        if (component == null) {
            return null;
        }
        return component instanceof Window window ? window : SwingUtilities.getWindowAncestor(component);
    }
}
