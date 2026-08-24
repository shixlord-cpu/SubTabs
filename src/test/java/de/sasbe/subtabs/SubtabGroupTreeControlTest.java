package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubtabGroupTreeControlTest {
    @Test
    void collapsedSquareFillsTheCenter() {
        BufferedImage image = render(true);
        assertTrue(isFilledBlue(image.getRGB(8, 8)));
    }

    @Test
    void expandedSquareLeavesTheCenterEmpty() {
        BufferedImage image = render(false);
        assertEquals(0, alpha(image.getRGB(8, 8)));
        assertTrue(hasBlueOutline(image));
    }

    @Test
    void ignoresPathsThatAreNotSubtabGroups() {
        assertFalse(SubtabGroupTreeControl.isSubtabGroupPath(null));
    }

    private static BufferedImage render(boolean filled) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            SubtabGroupTreeControl.paintSquare(graphics, 0, 0, 16, 16, filled, SubtabGroupTreeControl.FILL);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static boolean isFilledBlue(int argb) {
        return alpha(argb) > 200
                && red(argb) < 80
                && green(argb) < 150
                && blue(argb) > 180;
    }

    private static boolean hasBlueOutline(BufferedImage image) {
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                if (isFilledBlue(image.getRGB(x, y))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int alpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    private static int red(int argb) {
        return (argb >>> 16) & 0xFF;
    }

    private static int green(int argb) {
        return (argb >>> 8) & 0xFF;
    }

    private static int blue(int argb) {
        return argb & 0xFF;
    }
}
