package de.sasbe.subtabs;

import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

final class ComponentSubtabModifiedLabel extends JBLabel {
    private boolean modified;
    private boolean grayed;
    private @NotNull String plainText = "";

    void applyPresentation(@NotNull String plain, boolean isModified, boolean isGrayed) {
        plainText = plain;
        modified = isModified;
        grayed = isGrayed;
        putClientProperty(ComponentSubtabModifiedUi.PLAIN_LABEL_KEY, plain);
        setText(plain);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        if (isOpaque()) {
            graphics.setColor(getBackground());
            graphics.fillRect(0, 0, getWidth(), getHeight());
        }

        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setFont(getFont());
            Insets insets = getInsets();
            int x = insets.left;
            int y = insets.top + g2.getFontMetrics().getAscent();
            g2.setColor(ComponentSubtabModifiedUi.foreground(modified, grayed));
            g2.drawString(plainText, x, y);
        } finally {
            g2.dispose();
        }
    }
}
