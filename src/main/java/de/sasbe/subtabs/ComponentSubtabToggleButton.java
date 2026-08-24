package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;

import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

final class ComponentSubtabToggleButton extends JToggleButton {
    private static final String MODIFIED_KEY = "componentSubtabs.modified";
    private static final String OPEN_ELSEWHERE_KEY = "componentSubtabs.openElsewhere";

    ComponentSubtabToggleButton(@NotNull String label) {
        super(label);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        boolean modified = Boolean.TRUE.equals(getClientProperty(MODIFIED_KEY));
        if (!modified) {
            super.paintComponent(graphics);
            return;
        }

        String savedText = getText();
        setText("");
        super.paintComponent(graphics);
        setText(savedText);

        String plainLabel = ComponentSubtabModifiedUi.plainLabel(this);
        boolean grayed = !isSelected() && Boolean.TRUE.equals(getClientProperty(OPEN_ELSEWHERE_KEY));
        Color color = ComponentSubtabModifiedUi.foreground(true, grayed);

        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setColor(color);
            g2.setFont(getFont());
            FontMetrics metrics = g2.getFontMetrics();
            Rectangle textRect = new Rectangle();
            Rectangle viewRect = new Rectangle(getSize());
            Rectangle iconRect = new Rectangle();
            String text = SwingUtilities.layoutCompoundLabel(
                    this,
                    metrics,
                    plainLabel,
                    null,
                    getVerticalAlignment(),
                    getHorizontalAlignment(),
                    getVerticalTextPosition(),
                    getHorizontalTextPosition(),
                    viewRect,
                    iconRect,
                    textRect,
                    getIconTextGap()
            );
            g2.drawString(plainLabel, textRect.x, textRect.y + metrics.getAscent());
        } finally {
            g2.dispose();
        }
    }
}
