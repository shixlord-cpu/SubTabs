package de.sasbe.subtabs;

import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.BorderFactory;
import javax.swing.JToggleButton;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class ComponentSubtabUi {
    private static final String OPEN_ELSEWHERE_KEY = "componentSubtabs.openElsewhere";
    private static final Color HOVER_BACKGROUND = JBUI.CurrentTheme.TabbedPane.HOVER_COLOR;
    private static final Color SELECTED_BACKGROUND = JBUI.CurrentTheme.TabbedPane.FOCUS_COLOR;
    private static final Color SELECTED_UNDERLINE = JBUI.CurrentTheme.TabbedPane.ENABLED_SELECTED_COLOR;

    private ComponentSubtabUi() {
    }

    static @NotNull JToggleButton createSubtabButton(@NotNull String label, boolean selected) {
        JToggleButton button = new JToggleButton(label);
        button.setSelected(selected);
        button.setFocusable(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMargin(JBUI.insets(4, 10));
        button.putClientProperty("JButton.buttonType", "segmented");
        button.putClientProperty(OPEN_ELSEWHERE_KEY, false);
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
        button.setOpaque(true);
        button.setBackground(selected ? SELECTED_BACKGROUND : UIUtil.getPanelBackground());
        button.setForeground(selected || !openElsewhere
                ? UIUtil.getLabelForeground()
                : UIUtil.getInactiveTextColor());
        button.setFont(selected ? bold(button.getFont()) : plain(button.getFont()));
        button.setBorder(createBorder(selected));
    }

    private static @NotNull Border createBorder(boolean selected) {
        Border padding = JBUI.Borders.empty(2, 2, selected ? 0 : 2, 2);
        if (!selected) {
            return padding;
        }

        Border underline = BorderFactory.createMatteBorder(0, 0, JBUI.scale(2), 0, SELECTED_UNDERLINE);
        return BorderFactory.createCompoundBorder(underline, padding);
    }

    private static @NotNull Font bold(@NotNull Font font) {
        return font.deriveFont(Font.BOLD);
    }

    private static @NotNull Font plain(@NotNull Font font) {
        return font.deriveFont(Font.PLAIN);
    }
}
