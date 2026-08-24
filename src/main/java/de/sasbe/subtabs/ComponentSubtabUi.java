package de.sasbe.subtabs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.BorderFactory;
import javax.swing.JToggleButton;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class ComponentSubtabUi {
    private static final String OPEN_ELSEWHERE_KEY = "componentSubtabs.openElsewhere";
    private static final String FIT_KEY = "componentSubtabs.fitScale";
    private static final Color HOVER_BACKGROUND = JBUI.CurrentTheme.TabbedPane.HOVER_COLOR;
    private static final Color SELECTED_BACKGROUND = JBUI.CurrentTheme.TabbedPane.FOCUS_COLOR;
    private static final Color SELECTED_UNDERLINE = JBUI.CurrentTheme.TabbedPane.ENABLED_SELECTED_COLOR;
    private static final int MIN_HEIGHT_UNSCALED = 14;
    private static final int MAX_HEIGHT_UNSCALED = 32;

    private ComponentSubtabUi() {
    }

    static int tabHeight() {
        return JBUI.scale(tabHeightUnscaled(heightPercent()));
    }

    static int tabHeightUnscaled(int heightPercent) {
        float t = Math.max(0f, Math.min(1f, (heightPercent - 25) / 75f));
        return Math.round(MIN_HEIGHT_UNSCALED + t * (MAX_HEIGHT_UNSCALED - MIN_HEIGHT_UNSCALED));
    }

    static int compactVertical(int value) {
        return Math.max(0, Math.round(value * heightFactor()));
    }

    static int compactVerticalScaled(int value) {
        return Math.max(1, Math.round(JBUI.scale(value) * heightFactor()));
    }

    static int horizontalGap(@NotNull SubtabFitScale.Result fit) {
        return Math.max(1, Math.round(JBUI.scale(2) * fit.paddingScale()));
    }

    static int verticalGap() {
        return Math.max(0, Math.round(JBUI.scale(1) * heightFactor()));
    }

    static @NotNull JToggleButton createSubtabButton(@NotNull String label, boolean selected) {
        JToggleButton button = new JToggleButton(label);
        button.setSelected(selected);
        button.setFocusable(false);
        button.putClientProperty("JButton.buttonType", "segmented");
        button.putClientProperty(OPEN_ELSEWHERE_KEY, false);
        button.putClientProperty(FIT_KEY, SubtabFitScale.Result.FULL);
        button.addChangeListener(event -> applyAppearance(button));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                if (!button.isSelected()) {
                    button.setBackground(HOVER_BACKGROUND);
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                applyAppearance(button);
            }
        });
        applyAppearance(button);
        return button;
    }

    static void refreshButton(@NotNull JToggleButton button) {
        applyAppearance(button);
    }

    static void applyFit(@NotNull JToggleButton button, @NotNull SubtabFitScale.Result fit) {
        button.putClientProperty(FIT_KEY, fit);
        applyAppearance(button);
    }

    static void setOpenElsewhere(@NotNull JToggleButton button, boolean openElsewhere) {
        Boolean current = (Boolean) button.getClientProperty(OPEN_ELSEWHERE_KEY);
        if (current != null && current == openElsewhere) {
            return;
        }
        button.putClientProperty(OPEN_ELSEWHERE_KEY, openElsewhere);
        applyAppearance(button);
    }

    private static void applyAppearance(@NotNull JToggleButton button) {
        boolean selected = button.isSelected();
        boolean openElsewhere = Boolean.TRUE.equals(button.getClientProperty(OPEN_ELSEWHERE_KEY));
        SubtabFitScale.Result fit = fitOf(button);
        int height = tabHeight();
        button.setMargin(JBUI.insets(0, horizontalMargin(fit)));
        button.setOpaque(true);
        button.setBackground(selected ? SELECTED_BACKGROUND : UIUtil.getPanelBackground());
        button.setForeground(selected || !openElsewhere
                ? UIUtil.getLabelForeground()
                : UIUtil.getInactiveTextColor());
        button.setFont(scaledFont(selected, fit, height));
        button.setBorder(createBorder(selected, fit));
        int width = preferredWidth(button, fit);
        Dimension size = new Dimension(width, height);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
    }

    private static int preferredWidth(@NotNull JToggleButton button, @NotNull SubtabFitScale.Result fit) {
        FontMetrics metrics = button.getFontMetrics(button.getFont());
        int textWidth = metrics.stringWidth(button.getText());
        return textWidth + 2 * JBUI.scale(horizontalMargin(fit)) + 2 * JBUI.scale(Math.max(1, Math.round(2 * fit.paddingScale()))) + JBUI.scale(8);
    }

    private static @NotNull SubtabFitScale.Result fitOf(@NotNull JToggleButton button) {
        Object value = button.getClientProperty(FIT_KEY);
        return value instanceof SubtabFitScale.Result result ? result : SubtabFitScale.Result.FULL;
    }

    private static @NotNull Font scaledFont(boolean selected, @NotNull SubtabFitScale.Result fit, int tabHeight) {
        Font base = UIUtil.getLabelFont();
        Font styled = selected ? bold(base) : plain(base);
        int maxFont = Math.max(8, Math.round(tabHeight * 0.72f));
        float size = maxFont * textSizeFactor() * fit.fontScale();
        return styled.deriveFont(Math.max(8f, size));
    }

    private static @NotNull Border createBorder(boolean selected, @NotNull SubtabFitScale.Result fit) {
        int horizontal = Math.max(1, Math.round(2 * fit.paddingScale()));
        Border empty = JBUI.Borders.empty(0, horizontal);
        if (!selected) {
            return empty;
        }

        Border underline = BorderFactory.createMatteBorder(
                0,
                0,
                Math.max(1, JBUI.scale(2)),
                0,
                SELECTED_UNDERLINE
        );
        return BorderFactory.createCompoundBorder(underline, empty);
    }

    private static int horizontalMargin(@NotNull SubtabFitScale.Result fit) {
        return Math.max(3, Math.round(10 * fit.paddingScale()));
    }

    private static int heightPercent() {
        if (ApplicationManager.getApplication() == null) {
            return 75;
        }
        return SubtabsSettings.getInstance().getBarHeightPercent();
    }

    private static float heightFactor() {
        return heightPercent() / 100f;
    }

    private static float textSizeFactor() {
        if (ApplicationManager.getApplication() == null) {
            return 0.75f;
        }
        return SubtabsSettings.getInstance().getTextSizePercent() / 100f;
    }

    private static @NotNull Font bold(@NotNull Font font) {
        return font.deriveFont(Font.BOLD);
    }

    private static @NotNull Font plain(@NotNull Font font) {
        return font.deriveFont(Font.PLAIN);
    }
}
