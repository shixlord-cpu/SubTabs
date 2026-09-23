package de.sasbe.subtabs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class ComponentSubtabUi {
    static final String FILE_KEY = "componentSubtabs.tabFile";
    static final String OPEN_ELSEWHERE_KEY = "componentSubtabs.openElsewhere";
    static final String REORDER_HIDDEN_KEY = "componentSubtabs.reorderHidden";

    static boolean isReorderHidden(@NotNull JToggleButton button) {
        return Boolean.TRUE.equals(button.getClientProperty(REORDER_HIDDEN_KEY));
    }
    private static final String SPLIT_PARTNER_KEY = "componentSubtabs.splitPartner";
    static final String MODIFIED_KEY = "componentSubtabs.modified";
    static final String ERROR_KEY = "componentSubtabs.hasErrors";
    private static final String FIT_KEY = "componentSubtabs.fitScale";
    private static final String EXTERNAL_HOVER_KEY = "componentSubtabs.externalHover";
    static final String MANUAL_HOVER_KEY = "componentSubtabs.manualHover";
    private static final String MAIN_TAB_SYNC_KEY = "componentSubtabs.mainTabSyncHighlight";
    private static final String VERTICAL_SIDE_KEY = "componentSubtabs.verticalSide";
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

    static int barRowHeight() {
        return tabHeight() + 2 * verticalGap();
    }

    static @NotNull Color popupHighlightedBackground() {
        return new JBColor(new Color(0xDEEAF6), new Color(0x2D4A5E));
    }

    static @NotNull Color highlightBackground(@Nullable VirtualFile file) {
        return groupTintedBackground(file, popupHighlightedBackground());
    }

    static @NotNull Color selectedTabBackground(@Nullable VirtualFile file) {
        return groupTintedBackground(file, SELECTED_BACKGROUND);
    }

    static @NotNull Color selectedTabUnderline(@Nullable VirtualFile file) {
        if (SubtabGroupColors.isEnabled() && file != null) {
            Color groupColor = SubtabGroupColors.colorForFile(file);
            if (groupColor != null) {
                return groupColor;
            }
        }
        return SELECTED_UNDERLINE;
    }

    static @Nullable VirtualFile tabFile(@NotNull JToggleButton button) {
        Object value = button.getClientProperty(FILE_KEY);
        return value instanceof VirtualFile file ? file : null;
    }

    static @NotNull JToggleButton createSubtabButton(@NotNull String label, boolean selected) {
        JToggleButton button = new ComponentSubtabToggleButton(label);
        button.setSelected(selected);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setFocusable(false);
        button.putClientProperty("JButton.buttonType", "segmented");
        button.putClientProperty(OPEN_ELSEWHERE_KEY, false);
        button.putClientProperty(SPLIT_PARTNER_KEY, false);
        button.putClientProperty(MODIFIED_KEY, false);
        button.putClientProperty(ERROR_KEY, false);
        button.putClientProperty(ComponentSubtabModifiedUi.PLAIN_LABEL_KEY, label);
        button.putClientProperty(FIT_KEY, SubtabFitScale.Result.FULL);
        button.addChangeListener(event -> applyAppearance(button));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                if (!button.isSelected()) {
                    button.putClientProperty(MANUAL_HOVER_KEY, Boolean.TRUE);
                    button.setBackground(HOVER_BACKGROUND);
                    button.repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                button.putClientProperty(MANUAL_HOVER_KEY, null);
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

    static void setMainTabSyncHighlight(@NotNull JToggleButton button, boolean highlighted) {
        Boolean current = (Boolean) button.getClientProperty(MAIN_TAB_SYNC_KEY);
        if (current != null && current == highlighted) {
            return;
        }
        if (highlighted) {
            button.putClientProperty(MAIN_TAB_SYNC_KEY, Boolean.TRUE);
        } else {
            button.putClientProperty(MAIN_TAB_SYNC_KEY, null);
        }
        applyAppearance(button);
    }

    static boolean isMainTabSyncHighlight(@NotNull JToggleButton button) {
        return Boolean.TRUE.equals(button.getClientProperty(MAIN_TAB_SYNC_KEY));
    }

    static void refreshButton(@NotNull JToggleButton button) {
        applyAppearance(button);
    }

    static void applyFit(@NotNull JToggleButton button, @NotNull SubtabFitScale.Result fit) {
        button.putClientProperty(FIT_KEY, fit);
        applyAppearance(button);
    }

    /** Marks the file that the opposite pane of the same group split currently shows. */
    static void setSplitPartner(@NotNull JToggleButton button, boolean splitPartner) {
        Boolean current = (Boolean) button.getClientProperty(SPLIT_PARTNER_KEY);
        if (current != null && current == splitPartner) {
            return;
        }
        button.putClientProperty(SPLIT_PARTNER_KEY, splitPartner);
        applyAppearance(button);
    }

    static boolean isSplitPartner(@NotNull JToggleButton button) {
        return Boolean.TRUE.equals(button.getClientProperty(SPLIT_PARTNER_KEY));
    }

    static boolean isOpenElsewhere(@NotNull JToggleButton button) {
        return Boolean.TRUE.equals(button.getClientProperty(OPEN_ELSEWHERE_KEY));
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
        setPresentation(button, modified, hasErrors(button));
    }

    static void setHasErrors(@NotNull JToggleButton button, boolean hasErrors) {
        setPresentation(button, isModified(button), hasErrors);
    }

    static void setPresentation(@NotNull JToggleButton button, boolean modified, boolean hasErrors) {
        Boolean currentModified = (Boolean) button.getClientProperty(MODIFIED_KEY);
        Boolean currentErrors = (Boolean) button.getClientProperty(ERROR_KEY);
        if (currentModified != null && currentModified == modified
                && currentErrors != null && currentErrors == hasErrors) {
            return;
        }
        button.putClientProperty(MODIFIED_KEY, modified);
        button.putClientProperty(ERROR_KEY, hasErrors);
        applyAppearance(button);
    }

    static boolean isModified(@NotNull JToggleButton button) {
        return Boolean.TRUE.equals(button.getClientProperty(MODIFIED_KEY));
    }

    static boolean hasErrors(@NotNull JToggleButton button) {
        return Boolean.TRUE.equals(button.getClientProperty(ERROR_KEY));
    }

    private static void applyAppearance(@NotNull JToggleButton button) {
        boolean selected = button.isSelected();
        boolean splitPartner = Boolean.TRUE.equals(button.getClientProperty(SPLIT_PARTNER_KEY));
        boolean openElsewhere = Boolean.TRUE.equals(button.getClientProperty(OPEN_ELSEWHERE_KEY));
        boolean modified = Boolean.TRUE.equals(button.getClientProperty(MODIFIED_KEY));
        boolean externalHover = Boolean.TRUE.equals(button.getClientProperty(EXTERNAL_HOVER_KEY));
        boolean mainTabSync = Boolean.TRUE.equals(button.getClientProperty(MAIN_TAB_SYNC_KEY));
        SubtabFitScale.Result fit = fitOf(button);
        int height = tabHeight();
        String plainLabel = ComponentSubtabModifiedUi.plainLabel(button);
        VirtualFile file = tabFile(button);
        button.setMargin(JBUI.insets(0, horizontalMargin(fit)));
        button.setOpaque(true);
        if (mainTabSync) {
            button.setBackground(highlightBackground(file));
        } else if (selected) {
            button.setBackground(selectedTabBackground(file));
        } else if (externalHover || Boolean.TRUE.equals(button.getClientProperty(MANUAL_HOVER_KEY))) {
            button.setBackground(HOVER_BACKGROUND);
        } else {
            button.setBackground(UIUtil.getPanelBackground());
        }
        ComponentSubtabModifiedUi.applyToToggleButton(
                button,
                plainLabel,
                modified,
                !selected && !splitPartner && openElsewhere
        );
        // The partner keeps the bold label so both split files read as active, but only the pane's
        // own file gets the selected background and underline.
        button.setFont(scaledFont(selected || splitPartner, fit, height));
        button.setBorder(createBorder(button, selected, fit));
        if (!isSidetabSectionButton(button)) {
            int width = preferredWidth(button, fit);
            Dimension size = new Dimension(width, height);
            button.setPreferredSize(size);
            button.setMinimumSize(size);
        }
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        button.repaint();
    }

    static boolean isSidetabSectionButton(@NotNull JToggleButton button) {
        return button.getClientProperty(SidetabBarPanel.SECTION_INDEX_KEY) != null;
    }

    static void setVerticalPlacement(@NotNull JToggleButton button, boolean tabsOnRight) {
        button.putClientProperty(VERTICAL_SIDE_KEY, tabsOnRight ? "right" : "left");
        applyAppearance(button);
    }

    private static int preferredWidth(@NotNull JToggleButton button, @NotNull SubtabFitScale.Result fit) {
        return preferredLabelWidth(ComponentSubtabModifiedUi.plainLabel(button), fit, button.isSelected() || isSplitPartner(button));
    }

    static int preferredLabelWidth(@NotNull String label) {
        return preferredLabelWidth(label, SubtabFitScale.Result.FULL, true);
    }

    private static int preferredLabelWidth(
            @NotNull String label,
            @NotNull SubtabFitScale.Result fit,
            boolean selected
    ) {
        Font font = scaledFont(selected, fit, tabHeight());
        FontMetrics metrics = new JLabel().getFontMetrics(font);
        int textWidth = metrics.stringWidth(label);
        return textWidth + 2 * JBUI.scale(horizontalMargin(fit)) + 2 * JBUI.scale(Math.max(1, Math.round(2 * fit.paddingScale()))) + JBUI.scale(8);
    }

    private static @NotNull SubtabFitScale.Result fitOf(@NotNull JToggleButton button) {
        Object value = button.getClientProperty(FIT_KEY);
        return value instanceof SubtabFitScale.Result result ? result : SubtabFitScale.Result.FULL;
    }

    private static @NotNull Font scaledFont(boolean selected, @NotNull SubtabFitScale.Result fit, int tabHeight) {
        TabFontStyle fontStyle = TabFontStyle.current();
        Font base = fontStyle.baseFont();
        Font styled = fontStyle.styledFont(base, selected);
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
        if (!selected) {
            return empty;
        }

        int thickness = Math.max(1, JBUI.scale(2));
        Color underlineColor = selectedTabUnderline(tabFile(button));
        Object vertical = button.getClientProperty(VERTICAL_SIDE_KEY);
        Border underline;
        if ("right".equals(vertical)) {
            underline = BorderFactory.createMatteBorder(0, thickness, 0, 0, underlineColor);
        } else if ("left".equals(vertical)) {
            underline = BorderFactory.createMatteBorder(0, 0, 0, thickness, underlineColor);
        } else {
            underline = BorderFactory.createMatteBorder(0, 0, thickness, 0, underlineColor);
        }
        return BorderFactory.createCompoundBorder(underline, empty);
    }

    private static @NotNull Color groupTintedBackground(@Nullable VirtualFile file, @NotNull Color fallback) {
        if (SubtabGroupColors.isEnabled() && file != null) {
            Color groupColor = SubtabGroupColors.colorForFile(file);
            if (groupColor != null) {
                return ComponentSubtabMainTabColors.blend(
                        groupColor,
                        UIUtil.getPanelBackground(),
                        ComponentSubtabMainTabColors.FOCUSED_SELECTED_MIX
                );
            }
        }
        return fallback;
    }

    private static int horizontalMargin(@NotNull SubtabFitScale.Result fit) {
        return Math.max(3, Math.round(10 * fit.paddingScale()));
    }

    private static int heightPercent() {
        if (ApplicationManager.getApplication() == null) {
            return 75;
        }
        return TypographyPreview.barHeightPercent(SubtabsSettings.getInstance().getBarHeightPercent());
    }

    private static float heightFactor() {
        return heightPercent() / 100f;
    }

    private static float textSizeFactor() {
        if (ApplicationManager.getApplication() == null) {
            return 0.75f;
        }
        return TypographyPreview.textSizePercent(SubtabsSettings.getInstance().getTextSizePercent()) / 100f;
    }
}
