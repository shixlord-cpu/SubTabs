package de.sasbe.subtabs;

import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;

final class ComponentSubtabToggleButton extends JToggleButton {
    private boolean hidingLookAndFeelText;

    ComponentSubtabToggleButton(@NotNull String label) {
        super(label);
    }

    @Override
    public String getText() {
        return hidingLookAndFeelText ? "" : super.getText();
    }

    @Override
    public void paint(Graphics graphics) {
        hidingLookAndFeelText = true;
        try {
            super.paint(graphics);
        } finally {
            hidingLookAndFeelText = false;
        }
        paintLabelText(graphics);
        paintSubDepthDot(graphics);
    }

    private void paintSubDepthDot(@NotNull Graphics graphics) {
        Object depthValue = getClientProperty(SidetabBarPanel.DEPTH_KEY);
        int depth = depthValue instanceof Integer value ? Math.max(0, value) : 0;
        if (depth <= 0) {
            return;
        }
        int dotSize = SidetabBarPanel.subDepthDotSize();
        int dotOffset = SidetabBarPanel.subDepthDotOffset(depth);
        if (dotOffset < 0) {
            return;
        }
        Insets insets = getInsets();
        int dotX = insets.left + dotOffset;
        int centerY = getHeight() / 2;

        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setColor(getForeground());
            g2.fillOval(dotX, centerY - dotSize / 2, dotSize, dotSize);
        } finally {
            g2.dispose();
        }
    }

    private void paintLabelText(@NotNull Graphics graphics) {
        String plainLabel = ComponentSubtabModifiedUi.plainLabel(this);
        if (plainLabel.isEmpty()) {
            return;
        }
        boolean modified = Boolean.TRUE.equals(getClientProperty(ComponentSubtabUi.MODIFIED_KEY));
        boolean hasErrors = Boolean.TRUE.equals(getClientProperty(ComponentSubtabUi.ERROR_KEY));
        boolean grayed = !isSelected() && Boolean.TRUE.equals(getClientProperty(ComponentSubtabUi.OPEN_ELSEWHERE_KEY));
        Color color = ComponentSubtabModifiedUi.foreground(modified, grayed);

        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setFont(getFont());
            FontMetrics metrics = g2.getFontMetrics();
            Insets insets = getInsets();
            Rectangle textRect = new Rectangle();
            Rectangle viewRect = new Rectangle(
                    insets.left,
                    insets.top,
                    Math.max(0, getWidth() - insets.left - insets.right),
                    Math.max(0, getHeight() - insets.top - insets.bottom)
            );
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
            boolean folded = Boolean.TRUE.equals(getClientProperty(SidetabBarPanel.FOLDED_KEY));
            ComponentSubtabTextPainter.paint(
                    g2,
                    getFont(),
                    plainLabel,
                    textRect.x,
                    textRect.y + metrics.getAscent(),
                    color,
                    hasErrors,
                    folded
            );
        } finally {
            g2.dispose();
        }
    }
}
