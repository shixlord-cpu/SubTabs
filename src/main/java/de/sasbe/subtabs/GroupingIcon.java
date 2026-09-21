package de.sasbe.subtabs;

import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

final class GroupingIcon implements Icon {
    static final Color ACTIVE = new Color(0x3B82F6);
    static final Color INACTIVE = new Color(0x8A8A8A);

    private final boolean active;

    GroupingIcon(boolean active) {
        this.active = active;
    }

    @Override
    public void paintIcon(@Nullable Component component, @NotNull Graphics graphics, int x, int y) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
            g2.translate(x, y);

            int size = getIconWidth();
            float unit = size / 16f;
            float stroke = Math.max(1.35f, unit * 1.35f);
            g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(active ? ACTIVE : INACTIVE);

            float topY = 3f * unit;
            float topLeft = 2f * unit;
            float topRight = 14f * unit;
            float stackLeft = 4f * unit;
            float stackRight = 12f * unit;
            float stackStartY = 7f * unit;
            float stackGap = 2.5f * unit;

            g2.drawLine(Math.round(topLeft), Math.round(topY), Math.round(topRight), Math.round(topY));

            for (int index = 0; index < 3; index++) {
                float lineY = stackStartY + index * stackGap;
                g2.drawLine(
                        Math.round(stackLeft),
                        Math.round(lineY),
                        Math.round(stackRight),
                        Math.round(lineY)
                );
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    public int getIconWidth() {
        return JBUI.scale(16);
    }

    @Override
    public int getIconHeight() {
        return JBUI.scale(16);
    }
}
