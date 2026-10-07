package de.sasbe.subtabs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.util.ui.JBUI;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Dimension;
import java.awt.IllegalComponentStateException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.util.List;

final class SplittabRestoreSelectPopup {
    private static final int EXIT_DELAY_MS = 120;
    private static final String POPUP_KEY = "componentSubtabs.splittabRestoreSelectPopup";
    private static final String PANEL_KEY = "componentSubtabs.splittabRestoreSelectPanel";
    private static final String EXIT_TIMER_KEY = "componentSubtabs.splittabRestoreSelectExitTimer";

    private SplittabRestoreSelectPopup() {
    }

    static void onButtonEnter(@NotNull Project project, @NotNull JComponent button) {
        cancelExitTimer(button);
        if (isPopupVisible(button)) {
            return;
        }
        showPopup(project, button);
    }

    static void onButtonExit(@NotNull JComponent button) {
        scheduleExit(button);
    }

    private static void showPopup(@NotNull Project project, @NotNull JComponent button) {
        List<ComponentSubtabEditorSplitRegistry.SplittabPair> pairs =
                ComponentSubtabEditorSplitRegistry.getInstance(project).all();
        if (pairs.isEmpty()) {
            return;
        }

        hidePopup(button);

        int fixedWidth = Math.max(button.getWidth(), JBUI.scale(120));
        SplittabSavedPairsPopupPanel panel = new SplittabSavedPairsPopupPanel(
                project,
                pairs,
                fixedWidth,
                pairId -> {
                    ComponentSubtabEditorSplitNavigation.activatePair(project, pairId);
                    hidePopup(button);
                }
        );
        button.putClientProperty(PANEL_KEY, panel);

        Point showPoint = new Point(0, button.getHeight());
        Dimension preferred = panel.getPreferredSize();
        if (preferred.width != fixedWidth) {
            panel.setPreferredSize(new Dimension(fixedWidth, preferred.height));
        }

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, null)
                .setRequestFocus(false)
                .setCancelOnClickOutside(true)
                .setCancelOnOtherWindowOpen(true)
                .setCancelCallback(() -> {
                    hidePopup(button);
                    return true;
                })
                .createPopup();

        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(@NotNull LightweightWindowEvent event) {
                button.putClientProperty(POPUP_KEY, null);
                button.putClientProperty(PANEL_KEY, null);
            }
        });

        button.putClientProperty(POPUP_KEY, popup);
        popup.show(new RelativePoint(button, showPoint));
    }

    private static void hidePopup(@NotNull JComponent button) {
        cancelExitTimer(button);
        Object popup = button.getClientProperty(POPUP_KEY);
        if (popup instanceof JBPopup jbPopup) {
            jbPopup.cancel();
        }
        button.putClientProperty(POPUP_KEY, null);
        button.putClientProperty(PANEL_KEY, null);
    }

    private static void scheduleExit(@NotNull JComponent button) {
        if (isMouseOverPopup(button)) {
            return;
        }
        cancelExitTimer(button);
        Timer timer = new Timer(EXIT_DELAY_MS, event -> {
            if (!isPointerOver(button) && !isMouseOverPopup(button)) {
                hidePopup(button);
            }
        });
        timer.setRepeats(false);
        button.putClientProperty(EXIT_TIMER_KEY, timer);
        timer.start();
    }

    private static void cancelExitTimer(@NotNull JComponent button) {
        Object timer = button.getClientProperty(EXIT_TIMER_KEY);
        if (timer instanceof Timer swingTimer) {
            swingTimer.stop();
        }
        button.putClientProperty(EXIT_TIMER_KEY, null);
    }

    private static boolean isPopupVisible(@NotNull JComponent button) {
        Object popup = button.getClientProperty(POPUP_KEY);
        return popup instanceof JBPopup jbPopup && jbPopup.isVisible();
    }

    private static boolean isPointerOver(@NotNull JComponent button) {
        if (!button.isShowing()) {
            return false;
        }
        try {
            Point screen = MouseInfo.getPointerInfo().getLocation();
            Point local = button.getLocationOnScreen();
            return screen.x >= local.x
                    && screen.x < local.x + button.getWidth()
                    && screen.y >= local.y
                    && screen.y < local.y + button.getHeight();
        } catch (IllegalComponentStateException ignored) {
            return false;
        }
    }

    private static boolean isMouseOverPopup(@NotNull JComponent button) {
        Object panel = button.getClientProperty(PANEL_KEY);
        if (!(panel instanceof SplittabSavedPairsPopupPanel popupPanel) || !popupPanel.isShowing()) {
            return false;
        }
        try {
            Point screen = MouseInfo.getPointerInfo().getLocation();
            Point local = popupPanel.getLocationOnScreen();
            return screen.x >= local.x
                    && screen.x < local.x + popupPanel.getWidth()
                    && screen.y >= local.y
                    && screen.y < local.y + popupPanel.getHeight();
        } catch (IllegalComponentStateException ignored) {
            return false;
        }
    }

}
