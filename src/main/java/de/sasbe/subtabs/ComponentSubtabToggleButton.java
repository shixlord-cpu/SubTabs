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
    static final String MODIFIED_KEY = "componentSubtabs.modified";
    private static final String OPEN_ELSEWHERE_KEY = "componentSubtabs.openElsewhere";

    ComponentSubtabToggleButton(@NotNull String label) {
        super(label);
    }

    @Override
    public void paint(Graphics graphics) {
        boolean modified = Boolean.TRUE.equals(getClientProperty(MODIFIED_KEY));
        String savedText = getText();
        if (modified) {
            setText("");
        }
        super.paint(graphics);
        if (modified) {
            setText(savedText);
            paintModifiedText(graphics);
        }
    }

    private void paintModifiedText(@NotNull Graphics graphics) {
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
            SwingUtilities.layoutCompoundLabel(
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
