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
    protected void paintComponent(Graphics graphics) {
        if (ComponentSubtabUi.isReorderHidden(this)) {
            return;
        }
        super.paintComponent(graphics);
    }

    @Override
    public void paint(Graphics graphics) {
        if (ComponentSubtabUi.isReorderHidden(this)) {
            return;
        }
        if (subDepth() > 0) {
            paintSubDepthButton(graphics);
            return;
        }
        hidingLookAndFeelText = true;
        try {
            super.paint(graphics);
        } finally {
            hidingLookAndFeelText = false;
        }
        paintLabelText(graphics);
    }

    private int subDepth() {
        Object depthValue = getClientProperty(SidetabBarPanel.DEPTH_KEY);
        return depthValue instanceof Integer value ? Math.max(0, value) : 0;
    }

    private void paintSubDepthButton(@NotNull Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setClip(0, 0, getWidth(), getHeight());
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (getBorder() != null) {
                getBorder().paintBorder(this, g2, 0, 0, getWidth(), getHeight());
            }
            paintSubDepthDot(g2);
            paintLabelText(g2);
        } finally {
            g2.dispose();
        }
    }

    private void paintSubDepthDot(@NotNull Graphics graphics) {
        int depth = subDepth();
        if (depth <= 0) {
            return;
        }
        int dotSize = SidetabBarPanel.subDepthDotSize();
        int dotSpacing = SidetabBarPanel.subDepthDotSpacing();
        int baseX = SidetabBarPanel.subDepthDotBaseX();
        int centerY = getHeight() / 2;

        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setClip(0, 0, getWidth(), getHeight());
            g2.setColor(getForeground());
            for (int dotIndex = 0; dotIndex < depth; dotIndex++) {
                int dotX = baseX + dotIndex * dotSpacing;
                g2.fillOval(dotX, centerY - dotSize / 2, dotSize, dotSize);
            }
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
