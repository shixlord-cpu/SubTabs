package com.zayax.tabz;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SidetabSubDepthLabelTest {
    @Test
    void deeperSubSectionsIndentTextFurtherRight() {
        assertTrue(SidetabBarPanel.subDepthTextIndent(2) > SidetabBarPanel.subDepthTextIndent(1));
        assertTrue(SidetabBarPanel.subDepthTextIndent(3) > SidetabBarPanel.subDepthTextIndent(2));
    }

    @Test
    void subDepthIndentStaysFixedOnHover() {
        JToggleButton button = labelButton("Navigation", 2);
        int beforeHover = textStartX(button);
        button.dispatchEvent(new java.awt.event.MouseEvent(
                button,
                java.awt.event.MouseEvent.MOUSE_ENTERED,
                System.currentTimeMillis(),
                0,
                Math.max(1, button.getWidth() / 2),
                Math.max(1, button.getHeight() / 2),
                0,
                false
        ));
        ComponentSubtabUi.refreshButton(button);
        int afterHover = textStartX(button);
        assertEquals(beforeHover, afterHover);
    }

    @Test
    void subDepthTextIndentScalesWithDepth() {
        assertEquals(0, SidetabBarPanel.subDepthTextIndent(0));
        assertTrue(SidetabBarPanel.subDepthTextIndent(2) > SidetabBarPanel.subDepthTextIndent(1));
        assertTrue(SidetabBarPanel.subDepthTextIndent(1) > 0);
    }

    @Test
    void labelTextIsLeftAlignedForSubSection() {
        JToggleButton button = labelButton("Navigation", 1);
        BufferedImage image = render(button);
        int textMinX = button.getWidth();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (!isBackground(image.getRGB(x, y))) {
                    textMinX = Math.min(textMinX, x);
                }
            }
        }
        assertTrue(textMinX <= SidetabBarPanel.subDepthTextIndent(1) + 4);
        assertTrue(textMinX < button.getWidth() / 2);
    }

    private static int textStartX(@NotNull JToggleButton button) {
        BufferedImage image = render(button);
        int minX = button.getWidth();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (!isBackground(image.getRGB(x, y))) {
                    minX = Math.min(minX, x);
                }
            }
        }
        return minX;
    }

    private static JToggleButton labelButton(@NotNull String label, int depth) {
        JToggleButton button = ComponentSubtabUi.createTabzButton(label, false);
        button.putClientProperty(SidetabBarPanel.DEPTH_KEY, depth);
        button.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        if (depth > 0) {
            button.putClientProperty("JButton.buttonType", null);
            button.setRolloverEnabled(false);
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
        }
        button.setSize(120, ComponentSubtabUi.tabHeight());
        ComponentSubtabUi.refreshButton(button);
        return button;
    }

    private static boolean isBackground(int argb) {
        return (argb >>> 24) == 0 || argb == Color.WHITE.getRGB();
    }

    private static BufferedImage render(@NotNull JToggleButton button) {
        button.validate();
        BufferedImage image = new BufferedImage(button.getWidth(), button.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, button.getWidth(), button.getHeight());
            button.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }
}
