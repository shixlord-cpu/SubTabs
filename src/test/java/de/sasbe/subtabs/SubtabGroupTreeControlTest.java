package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubtabGroupTreeControlTest {
    @Test
    void collapsedCubeFillsBlueWithGrayBorder() {
        BufferedImage image = render(SubtabGroupTreeControlStyle.CUBES, true);
        assertTrue(isFilledBlue(image.getRGB(8, 8)));
        assertTrue(hasGrayOutline(image));
    }

    @Test
    void expandedCubeUsesGrayBorderOnly() {
        BufferedImage image = render(SubtabGroupTreeControlStyle.CUBES, false);
        assertEquals(0, alpha(image.getRGB(8, 8)));
        assertTrue(hasGrayOutline(image));
    }

    @Test
    void collapsedCircleFillsBlueWithGrayBorder() {
        BufferedImage image = render(SubtabGroupTreeControlStyle.CIRCLES, true);
        assertTrue(isFilledBlue(image.getRGB(8, 8)));
        assertTrue(hasGrayOutline(image));
    }

    @Test
    void defaultStyleUsesPlatformArrows() {
        assertNull(SubtabGroupTreeControl.controlFor(SubtabGroupTreeControlStyle.DEFAULT));
        assertNotNull(SubtabGroupTreeControl.controlFor(SubtabGroupTreeControlStyle.CUBES));
        assertNotNull(SubtabGroupTreeControl.controlFor(SubtabGroupTreeControlStyle.CIRCLES));
        assertNotNull(SubtabGroupTreeControl.controlFor(SubtabGroupTreeControlStyle.BLUE_ARROWS));
        assertNotNull(SubtabGroupTreeControl.controlFor(SubtabGroupTreeControlStyle.NONE));
    }

    @Test
    void noneStyleUsesEmptyControlWithoutSize() {
        var control = SubtabGroupTreeControl.controlFor(SubtabGroupTreeControlStyle.NONE);
        assertNotNull(control);
        assertEquals(0, control.getWidth());
        assertEquals(0, control.getHeight());
    }

    @Test
    void ignoresPathsThatAreNotSubtabGroups() {
        assertFalse(SubtabGroupTreeControl.isSubtabGroupPath(null));
    }

    @Test
    void fillsCollapsedGroupsByDefault() {
        assertTrue(SubtabGroupTreeControl.shapeFilled(false, false));
        assertFalse(SubtabGroupTreeControl.shapeFilled(true, false));
    }

    @Test
    void fillsExpandedGroupsWhenInverted() {
        assertTrue(SubtabGroupTreeControl.shapeFilled(true, true));
        assertFalse(SubtabGroupTreeControl.shapeFilled(false, true));
    }

    private static BufferedImage render(SubtabGroupTreeControlStyle style, boolean filled) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            SubtabGroupTreeControl.paintShape(graphics, style, 0, 0, 16, 16, filled);
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

    private static boolean hasGrayOutline(BufferedImage image) {
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                if (isGray(image.getRGB(x, y))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isGray(int argb) {
        if (alpha(argb) < 128) {
            return false;
        }
        int r = red(argb);
        int g = green(argb);
        int b = blue(argb);
        return Math.abs(r - g) < 20 && Math.abs(g - b) < 20 && r > 80 && r < 180;
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
