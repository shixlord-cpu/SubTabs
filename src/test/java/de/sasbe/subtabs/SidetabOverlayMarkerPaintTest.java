package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SidetabOverlayMarkerPaintTest {
    @Test
    void foldedMarkerDrawsCenteredDot() throws Exception {
        JToggleButton button = newOverlayButton();
        button.setSize(40, 32);
        button.putClientProperty(SidetabBarPanel.FOLDED_KEY, true);

        Point center = paintedMarkerCenter(button);
        assertEquals(20, center.x, 1);
        assertEquals(16, center.y, 1);
    }

    @Test
    void blankMarkerIsThreeTimesTallerThanLegacyBlankBar() throws Exception {
        JToggleButton button = newOverlayButton();
        button.setSize(40, 32);
        button.putClientProperty(SidetabBarPanel.BLANK_KEY, true);
        button.putClientProperty("overlayLineWidth", 6);
        assertTrue(paintedVerticalSpan(button) >= 6, "Blank overlay bars must stay visibly tall");
    }

    @Test
    void openOverlayBarIsThreeTimesTallerThanLegacyWidth() throws Exception {
        JToggleButton button = newOverlayButton();
        button.setSize(40, 32);
        button.putClientProperty(SidetabBarPanel.FOLDED_KEY, false);
        button.putClientProperty("overlayLineWidth", 12);
        assertTrue(paintedVerticalSpan(button) >= 10, "Overlay bars must stay visibly tall");
    }

    @Test
    void foldedMarkerDrawsDotWithoutChangingHitArea() throws Exception {
        JToggleButton button = newOverlayButton();
        button.setSize(40, 32);

        button.putClientProperty(SidetabBarPanel.FOLDED_KEY, false);
        int barPixels = countMarkerPixels(button);

        button.putClientProperty(SidetabBarPanel.FOLDED_KEY, true);
        int dotPixels = countMarkerPixels(button);

        assertTrue(barPixels > dotPixels, "Folded marker should render a smaller dot than the open bar");
        assertTrue(dotPixels > 0);
        assertEquals(40, button.getWidth(), "Click target width must stay the same for bar and dot");
    }

    @Test
    void selectedMarkerRendersVisibleBar() throws Exception {
        JToggleButton button = newOverlayButton();
        button.setSize(40, 32);
        button.setSelected(true);
        button.putClientProperty(SidetabBarPanel.FOLDED_KEY, false);

        assertTrue(countMarkerPixels(button) > 0);
    }

    @Test
    void openBarUsesWideRightAlignedMarker() throws Exception {
        JToggleButton button = newOverlayButton();
        button.setSize(40, 16);
        button.putClientProperty(SidetabBarPanel.FOLDED_KEY, false);

        int span = paintedHorizontalSpan(button);
        assertTrue(span >= 24, "Overlay bars must stay visibly wide, got span=" + span);
    }

    @Test
    void nestedBlankBarStaysRightAligned() throws Exception {
        assertNestedBarGeometry(true);
    }

    @Test
    void nestedOpenBarStaysRightAligned() throws Exception {
        assertNestedBarGeometry(false);
    }

    private static void assertNestedBarGeometry(boolean blank) throws Exception {
        JToggleButton depthZero = newOverlayButton();
        depthZero.setSize(40, 32);
        depthZero.putClientProperty(SidetabBarPanel.DEPTH_KEY, 0);
        depthZero.putClientProperty(SidetabBarPanel.BLANK_KEY, blank);
        depthZero.putClientProperty(SidetabBarPanel.FOLDED_KEY, false);

        JToggleButton depthOne = newOverlayButton();
        depthOne.setSize(40, 32);
        depthOne.putClientProperty(SidetabBarPanel.DEPTH_KEY, 1);
        depthOne.putClientProperty(SidetabBarPanel.BLANK_KEY, blank);
        depthOne.putClientProperty(SidetabBarPanel.FOLDED_KEY, false);

        JToggleButton depthTwo = newOverlayButton();
        depthTwo.setSize(40, 32);
        depthTwo.putClientProperty(SidetabBarPanel.DEPTH_KEY, 2);
        depthTwo.putClientProperty(SidetabBarPanel.BLANK_KEY, blank);
        depthTwo.putClientProperty(SidetabBarPanel.FOLDED_KEY, false);

        int depthZeroMaxX = paintedMaxX(depthZero);
        int depthOneMaxX = paintedMaxX(depthOne);
        int depthTwoMaxX = paintedMaxX(depthTwo);
        assertEquals(depthZeroMaxX, depthOneMaxX, 1, "Nested bars must share the same right edge");
        assertEquals(depthOneMaxX, depthTwoMaxX, 1, "Deeper nested bars must share the same right edge");
        assertTrue(paintedMinX(depthOne) > paintedMinX(depthZero), "Depth 1 must start further right than depth 0");
        assertTrue(paintedMinX(depthTwo) > paintedMinX(depthOne), "Depth 2 must start further right than depth 1");
        assertTrue(
                paintedHorizontalSpan(depthZero) > paintedHorizontalSpan(depthOne),
                "Depth 0 bar must stay wider than depth 1"
        );
        assertTrue(
                paintedHorizontalSpan(depthOne) > paintedHorizontalSpan(depthTwo),
                "Depth 1 bar must stay wider than depth 2"
        );
    }

    @Test
    void foldedNestedDotUsesBarCenter() throws Exception {
        JToggleButton open = newOverlayButton();
        open.setSize(40, 32);
        open.putClientProperty(SidetabBarPanel.DEPTH_KEY, 1);
        open.putClientProperty(SidetabBarPanel.BLANK_KEY, true);
        open.putClientProperty(SidetabBarPanel.FOLDED_KEY, false);

        JToggleButton folded = newOverlayButton();
        folded.setSize(40, 32);
        folded.putClientProperty(SidetabBarPanel.DEPTH_KEY, 1);
        folded.putClientProperty(SidetabBarPanel.BLANK_KEY, true);
        folded.putClientProperty(SidetabBarPanel.FOLDED_KEY, true);

        Point openCenter = paintedMarkerCenter(open);
        Point foldedCenter = paintedMarkerCenter(folded);
        assertEquals(openCenter.x, foldedCenter.x, 1);
    }

    private static int paintedMaxX(JToggleButton button) throws Exception {
        BufferedImage image = render(button);
        int maxX = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (((image.getRGB(x, y) >>> 24) & 0xFF) > 0) {
                    maxX = Math.max(maxX, x);
                }
            }
        }
        return maxX;
    }

    private static int paintedMinX(JToggleButton button) throws Exception {
        BufferedImage image = render(button);
        int minX = button.getWidth();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (((image.getRGB(x, y) >>> 24) & 0xFF) > 0) {
                    minX = Math.min(minX, x);
                }
            }
        }
        return minX == button.getWidth() ? -1 : minX;
    }

    private static int paintedVerticalSpan(JToggleButton button) throws Exception {
        BufferedImage image = render(button);
        int minY = button.getHeight();
        int maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (((image.getRGB(x, y) >>> 24) & 0xFF) > 0) {
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        return maxY >= minY ? maxY - minY + 1 : 0;
    }

    private static Point paintedMarkerCenter(JToggleButton button) throws Exception {
        BufferedImage image = render(button);
        int minX = button.getWidth();
        int maxX = -1;
        int minY = button.getHeight();
        int maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (((image.getRGB(x, y) >>> 24) & 0xFF) > 0) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return new Point(-1, -1);
        }
        return new Point((minX + maxX) / 2, (minY + maxY) / 2);
    }

    private static BufferedImage render(JToggleButton button) throws Exception {
        BufferedImage image = new BufferedImage(button.getWidth(), button.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            Method paint = button.getClass().getDeclaredMethod("paintComponent", java.awt.Graphics.class);
            paint.setAccessible(true);
            paint.invoke(button, graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static int paintedHorizontalSpan(JToggleButton button) throws Exception {
        BufferedImage image = render(button);
        int minX = button.getWidth();
        int maxX = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (((image.getRGB(x, y) >>> 24) & 0xFF) > 0) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                }
            }
        }
        return maxX >= minX ? maxX - minX + 1 : 0;
    }

    private static JToggleButton newOverlayButton() throws Exception {
        Class<?> type = Class.forName("de.sasbe.subtabs.SidetabBarPanel$SegmentButton");
        var constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return (JToggleButton) constructor.newInstance();
    }

    @Test
    void overlayBarsUseHalfOpacityWhenNotHovered() throws Exception {
        JToggleButton button = newOverlayButton();
        button.setSize(40, 32);
        button.putClientProperty(SidetabBarPanel.FOLDED_KEY, false);

        BufferedImage idle = render(button);
        button.getModel().setRollover(true);
        BufferedImage hovered = render(button);

        assertTrue(averageAlpha(idle) < averageAlpha(hovered),
                "Idle overlay bars must be more transparent than hovered bars");
        assertTrue(averageAlpha(idle) <= 140, "Idle overlay bars should stay near 50% opacity");
    }

    private static double averageAlpha(BufferedImage image) {
        long alphaSum = 0;
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = (image.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha > 0) {
                    alphaSum += alpha;
                    count++;
                }
            }
        }
        return count == 0 ? 0 : (double) alphaSum / count;
    }

    private static int countMarkerPixels(JToggleButton button) throws Exception {
        BufferedImage image = render(button);
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (((image.getRGB(x, y) >>> 24) & 0xFF) > 0) {
                    count++;
                }
            }
        }
        return count;
    }
}
