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
import java.awt.geom.RoundRectangle2D;

final class SidetabIcon implements Icon {
    private static final Color ACTIVE = new Color(0x3B82F6);
    private static final Color INACTIVE = new Color(0x8A8A8A);
    private static final int SECTION_COUNT = 4;
    private static final int FILLED_SECTION = 1;

    private final boolean active;

    SidetabIcon(boolean active) {
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
            float stroke = Math.max(1.15f, unit * 1.15f);
            g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            Color color = active ? ACTIVE : INACTIVE;
            float left = 3.6f * unit;
            float width = 8.8f * unit;
            float height = 2.55f * unit;
            float gap = 0.7f * unit;
            float top = 1.35f * unit;
            float arc = 1.2f * unit;

            for (int index = 0; index < SECTION_COUNT; index++) {
                float sectionY = top + index * (height + gap);
                RoundRectangle2D.Float section = new RoundRectangle2D.Float(left, sectionY, width, height, arc, arc);
                if (index == FILLED_SECTION) {
                    g2.setColor(color);
                    g2.fill(section);
                    continue;
                }
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 48));
                g2.fill(section);
                g2.setColor(color);
                g2.draw(section);
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
