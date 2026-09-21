package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class ComponentSubtabIconButton extends JButton {
    private static final Color LOADING_ARC_COLOR = new Color(0x3B82F6);
    private static final int LOADING_ARC_DEGREES = 270;

    private boolean hovered;
    private boolean loading;
    private float loadingAngle;
    private @Nullable Timer loadingTimer;

    ComponentSubtabIconButton(@NotNull Icon icon) {
        super(icon);
        setFocusable(false);
        setBorder(BorderFactory.createEmptyBorder());
        setContentAreaFilled(false);
        setOpaque(false);
        updateSize();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hovered = false;
                repaint();
            }
        });
    }

    void updateSize() {
        int size = ComponentSubtabUi.tabHeight();
        Dimension dimension = new Dimension(size, size);
        setPreferredSize(dimension);
        setMinimumSize(dimension);
        setMaximumSize(dimension);
    }

    void setLoading(boolean loading) {
        if (this.loading == loading) {
            return;
        }
        this.loading = loading;
        setEnabled(!loading);
        if (loading) {
            if (loadingTimer == null) {
                loadingTimer = new Timer(16, event -> {
                    loadingAngle = (loadingAngle + 10f) % 360f;
                    repaint();
                });
            }
            loadingTimer.start();
        } else if (loadingTimer != null) {
            loadingTimer.stop();
        }
        repaint();
    }

    boolean isLoading() {
        return loading;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        if (loading) {
            paintLoadingIndicator(graphics);
            return;
        }
        if (hovered && isEnabled()) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(com.intellij.util.ui.JBUI.CurrentTheme.ActionButton.hoverBackground());
                int diameter = Math.min(getWidth(), getHeight());
                int x = (getWidth() - diameter) / 2;
                int y = (getHeight() - diameter) / 2;
                g2.fillOval(x, y, diameter, diameter);
            } finally {
                g2.dispose();
            }
        }
        super.paintComponent(graphics);
    }

    private void paintLoadingIndicator(@NotNull Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(LOADING_ARC_COLOR);
            float strokeWidth = Math.max(1.5f, com.intellij.util.ui.JBUI.scale(2f));
            g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int inset = com.intellij.util.ui.JBUI.scale(4);
            int diameter = Math.min(getWidth(), getHeight()) - inset * 2;
            int x = (getWidth() - diameter) / 2;
            int y = (getHeight() - diameter) / 2;
            g2.drawArc(x, y, diameter, diameter, Math.round(loadingAngle), LOADING_ARC_DEGREES);
        } finally {
            g2.dispose();
        }
    }
}
