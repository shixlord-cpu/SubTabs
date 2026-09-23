package de.sasbe.subtabs;

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
    void deeperSubDotsIncreaseDotCount() {
        JToggleButton depthOne = labelButton("", 1);
        JToggleButton depthTwo = labelButton("", 2);
        JToggleButton depthThree = labelButton("", 3);

        assertEquals(1, countDepthDots(depthOne, 1));
        assertEquals(2, countDepthDots(depthTwo, 2));
        assertEquals(3, countDepthDots(depthThree, 3));
    }

    @Test
    void subDepthDotsStayFixedOnHover() {
        JToggleButton button = labelButton("", 2);
        int beforeHover = firstDotX(button);
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
        int afterHover = firstDotX(button);
        assertEquals(beforeHover, afterHover);
    }

    private static int firstDotX(@NotNull JToggleButton button) {
        BufferedImage image = render(button);
        int dotSize = SidetabBarPanel.subDepthDotSize();
        int centerY = button.getHeight() / 2;
        int expectedX = SidetabBarPanel.subDepthDotBaseX();
        for (int x = 0; x < button.getWidth(); x++) {
            if (dotPixelHitsAt(image, x, centerY, dotSize) >= dotSize) {
                return x;
            }
        }
        return expectedX;
    }

    private static int countDepthDots(@NotNull JToggleButton button, int depth) {
        int baseX = SidetabBarPanel.subDepthDotBaseX();
        int spacing = SidetabBarPanel.subDepthDotSpacing();
        int count = 0;
        for (int dotIndex = 0; dotIndex < depth; dotIndex++) {
            if (hasDotCluster(button, baseX + dotIndex * spacing)) {
                count++;
            }
        }
        return count;
    }

    @Test
    void subDepthDotOffsetScalesWithDepth() {
        assertTrue(SidetabBarPanel.subDepthDotOffset(0) < 0);
        assertTrue(SidetabBarPanel.subDepthDotOffset(2) > SidetabBarPanel.subDepthDotOffset(1));
        assertTrue(SidetabBarPanel.subDepthDotOffset(1) >= 0);
    }

    @Test
    void labelTextStaysCenteredForSubSection() {
        JToggleButton button = labelButton("Navigation", 1);
        BufferedImage image = render(button);
        int textMinX = button.getWidth();
        int textMaxX = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (!isBackground(image.getRGB(x, y))) {
                    textMinX = Math.min(textMinX, x);
                    textMaxX = Math.max(textMaxX, x);
                }
            }
        }
        int textCenter = (textMinX + textMaxX) / 2;
        assertTrue(Math.abs(textCenter - button.getWidth() / 2) <= 8);
    }

    private static JToggleButton labelButton(@NotNull String label, int depth) {
        JToggleButton button = ComponentSubtabUi.createSubtabButton(label, false);
        button.putClientProperty(SidetabBarPanel.DEPTH_KEY, depth);
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

    private static boolean hasDotCluster(@NotNull JToggleButton button, int dotX) {
        return dotPixelHits(button, dotX) >= SidetabBarPanel.subDepthDotSize();
    }

    private static int dotPixelHits(@NotNull JToggleButton button, int dotX) {
        BufferedImage image = render(button);
        int dotSize = SidetabBarPanel.subDepthDotSize();
        int centerY = button.getHeight() / 2;
        return dotPixelHitsAt(image, dotX, centerY, dotSize);
    }

    private static int dotPixelHitsAt(
            @NotNull BufferedImage image,
            int dotX,
            int centerY,
            int dotSize
    ) {
        int hits = 0;
        for (int y = centerY - dotSize; y <= centerY + dotSize; y++) {
            if (y < 0 || y >= image.getHeight()) {
                continue;
            }
            for (int x = dotX; x < dotX + dotSize; x++) {
                if (x < 0 || x >= image.getWidth()) {
                    continue;
                }
                if (!isBackground(image.getRGB(x, y))) {
                    hits++;
                }
            }
        }
        return hits;
    }

    private static boolean isBackground(int argb) {
        return (argb >>> 24) == 0 || argb == Color.WHITE.getRGB();
    }

    private static BufferedImage render(@NotNull JToggleButton button) {
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
