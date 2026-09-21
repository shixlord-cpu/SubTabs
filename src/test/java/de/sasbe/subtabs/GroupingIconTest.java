package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupingIconTest {
    @Test
    void activeIconUsesSubTabsBlue() {
        BufferedImage image = render(new GroupingIcon(true), 16);
        Color topLine = sampleLineColor(image, 8, 4);
        assertTrue(topLine.getBlue() > topLine.getRed());
        assertTrue(topLine.getAlpha() > 0);
    }

    @Test
    void inactiveIconUsesMutedGray() {
        BufferedImage image = render(new GroupingIcon(false), 16);
        Color inactive = sampleLineColor(image, 8, 4);
        Color active = sampleLineColor(render(new GroupingIcon(true), 16), 8, 4);
        assertNotEquals(active.getRGB(), inactive.getRGB());
        assertTrue(Math.abs(inactive.getRed() - inactive.getGreen()) < 20);
    }

    @Test
    void drawsThreeStackedLinesBelowTopBar() {
        BufferedImage active = render(new GroupingIcon(true), 16);
        assertTrue(hasColoredPixel(active, 8, 7));
        assertTrue(hasColoredPixel(active, 8, 10));
        assertTrue(hasColoredPixel(active, 8, 12));
    }

    @Test
    void fitsStandardIconSize() {
        GroupingIcon icon = new GroupingIcon(true);
        assertEquals(16, icon.getIconWidth());
        assertEquals(16, icon.getIconHeight());
    }

    private static BufferedImage render(GroupingIcon icon, int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        icon.paintIcon(null, image.getGraphics(), 0, 0);
        return image;
    }

    private static boolean hasColoredPixel(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) >>> 24) != 0;
    }

    private static Color sampleLineColor(BufferedImage image, int centerX, int centerY) {
        int alpha = 0;
        int red = 0;
        int green = 0;
        int blue = 0;
        for (int y = centerY - 1; y <= centerY + 1; y++) {
            for (int x = centerX - 1; x <= centerX + 1; x++) {
                Color pixel = new Color(image.getRGB(x, y), true);
                alpha = Math.max(alpha, pixel.getAlpha());
                red = Math.max(red, pixel.getRed());
                green = Math.max(green, pixel.getGreen());
                blue = Math.max(blue, pixel.getBlue());
            }
        }
        return new Color(red, green, blue, alpha);
    }
}
