package de.sasbe.subtabs;

import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.JToggleButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Compact left/right placement switch shown beside the SideTabs flag.
 */
final class SidetabsSideSwitchButton extends JToggleButton {
    private static final Color ACCENT = new Color(0x3B82F6);

    SidetabsSideSwitchButton() {
        super();
        setSelected(true);
        setFocusable(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setToolTipText(
                "Rechtsbündig: SideTabs rechts vom Code-Fenster anzeigen. "
                        + "Ausgeschaltet: links vom Editor."
        );
        getAccessibleContext().setAccessibleName("SideTabs rechtsbündig");
        int size = JBUI.scale(22);
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            float pad = JBUI.scale(3f);
            float stroke = Math.max(1.2f, JBUI.scale(1.4f));
            g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            Color frame = ACCENT;
            g2.setColor(new Color(frame.getRed(), frame.getGreen(), frame.getBlue(), 40));
            g2.fill(new RoundRectangle2D.Float(pad, pad, width - pad * 2, height - pad * 2, JBUI.scale(4f), JBUI.scale(4f)));
            g2.setColor(frame);
            g2.draw(new RoundRectangle2D.Float(pad, pad, width - pad * 2, height - pad * 2, JBUI.scale(4f), JBUI.scale(4f)));

            float innerPad = pad + JBUI.scale(2.2f);
            float innerWidth = width - innerPad * 2;
            float innerHeight = height - innerPad * 2;
            float bar = Math.max(innerWidth * 0.28f, JBUI.scale(3.5f));
            float editorWidth = innerWidth - bar - JBUI.scale(1.5f);
            boolean onRight = isSelected();
            float editorX = onRight ? innerPad : innerPad + bar + JBUI.scale(1.5f);
            float barX = onRight ? innerPad + editorWidth + JBUI.scale(1.5f) : innerPad;

            g2.setColor(new Color(frame.getRed(), frame.getGreen(), frame.getBlue(), 70));
            g2.fill(new RoundRectangle2D.Float(editorX, innerPad, editorWidth, innerHeight, JBUI.scale(2f), JBUI.scale(2f)));
            g2.setColor(frame);
            g2.fill(new RoundRectangle2D.Float(barX, innerPad, bar, innerHeight, JBUI.scale(2f), JBUI.scale(2f)));
        } finally {
            g2.dispose();
        }
    }
}
