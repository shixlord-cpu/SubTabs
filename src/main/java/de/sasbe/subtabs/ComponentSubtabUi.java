package de.sasbe.subtabs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private static final String MODIFIED_KEY = "componentSubtabs.modified";
    private static final String FIT_KEY = "componentSubtabs.fitScale";
    private static final String EXTERNAL_HOVER_KEY = "componentSubtabs.externalHover";
    private static final String GROUP_COLOR_KEY = "componentSubtabs.groupColor";
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
        JToggleButton button = new ComponentSubtabToggleButton(label);
        button.setSelected(selected);
        button.setFocusable(false);
        button.putClientProperty("JButton.buttonType", "segmented");
        button.putClientProperty(OPEN_ELSEWHERE_KEY, false);
        button.putClientProperty(MODIFIED_KEY, false);
        button.putClientProperty(ComponentSubtabModifiedUi.PLAIN_LABEL_KEY, label);
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

    static void setExternalHover(@NotNull JToggleButton button, boolean hovered) {
        Boolean current = (Boolean) button.getClientProperty(EXTERNAL_HOVER_KEY);
        if (current != null && current == hovered) {
            return;
        }
        button.putClientProperty(EXTERNAL_HOVER_KEY, hovered);
        applyAppearance(button);
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

    static void setModified(@NotNull JToggleButton button, boolean modified) {
        button.putClientProperty(MODIFIED_KEY, modified);
        applyAppearance(button);
    }

    static void setGroupColor(@NotNull JToggleButton button, @Nullable Color groupColor) {
        Color current = (Color) button.getClientProperty(GROUP_COLOR_KEY);
        if ((current != null && current.equals(groupColor)) || (current == null && groupColor == null)) {
            return;
        }
        button.putClientProperty(GROUP_COLOR_KEY, groupColor);
        applyAppearance(button);
    }

    private static void applyAppearance(@NotNull JToggleButton button) {
        boolean selected = button.isSelected();
        boolean openElsewhere = Boolean.TRUE.equals(button.getClientProperty(OPEN_ELSEWHERE_KEY));
        boolean modified = Boolean.TRUE.equals(button.getClientProperty(MODIFIED_KEY));
        boolean externalHover = Boolean.TRUE.equals(button.getClientProperty(EXTERNAL_HOVER_KEY));
        SubtabFitScale.Result fit = fitOf(button);
        int height = tabHeight();
        String plainLabel = ComponentSubtabModifiedUi.plainLabel(button);
        button.setMargin(JBUI.insets(0, horizontalMargin(fit)));
        button.setOpaque(true);
        if (selected) {
            button.setBackground(SELECTED_BACKGROUND);
        } else if (externalHover) {
            button.setBackground(HOVER_BACKGROUND);
        } else {
            button.setBackground(UIUtil.getPanelBackground());
        }
        ComponentSubtabModifiedUi.applyToToggleButton(
                button,
                plainLabel,
                modified,
                !selected && openElsewhere
        );
        button.setFont(scaledFont(selected, fit, height));
        button.setBorder(createBorder(button, selected, fit));
        int width = preferredWidth(button, fit);
        Dimension size = new Dimension(width, height);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        button.repaint();
    }

    private static int preferredWidth(@NotNull JToggleButton button, @NotNull SubtabFitScale.Result fit) {
        FontMetrics metrics = button.getFontMetrics(button.getFont());
        int textWidth = metrics.stringWidth(ComponentSubtabModifiedUi.plainLabel(button));
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

    private static @NotNull Border createBorder(
            @NotNull JToggleButton button,
            boolean selected,
            @NotNull SubtabFitScale.Result fit
    ) {
        int horizontal = Math.max(1, Math.round(2 * fit.paddingScale()));
        Border empty = JBUI.Borders.empty(0, horizontal);
        Color groupColor = SubtabGroupColors.isEnabled()
                ? (Color) button.getClientProperty(GROUP_COLOR_KEY)
                : null;

        if (groupColor != null) {
            Border sides = BorderFactory.createMatteBorder(0, 1, 0, 1, groupColor);
            if (selected) {
                Border underline = BorderFactory.createMatteBorder(
                        0,
                        0,
                        Math.max(1, JBUI.scale(2)),
                        0,
                        groupColor
                );
                return BorderFactory.createCompoundBorder(underline, BorderFactory.createCompoundBorder(sides, empty));
            }
            return BorderFactory.createCompoundBorder(sides, empty);
        }

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
