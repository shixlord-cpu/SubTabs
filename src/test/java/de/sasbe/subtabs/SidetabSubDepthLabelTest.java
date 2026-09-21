package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SidetabSubDepthLabelTest {
    @Test
    void deeperSubDotsMoveFurtherRight() {
        JToggleButton depthOne = labelButton("Nav", 1);
        JToggleButton depthTwo = labelButton("Detail", 2);
        Insets insets = depthOne.getInsets();

        int dotXOne = insets.left + SidetabBarPanel.subDepthDotOffset(1);
        int dotXTwo = insets.left + SidetabBarPanel.subDepthDotOffset(2);

        assertTrue(hasDotCluster(depthOne, dotXOne));
        assertTrue(hasDotCluster(depthTwo, dotXTwo));
        assertTrue(dotXTwo > dotXOne);
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
