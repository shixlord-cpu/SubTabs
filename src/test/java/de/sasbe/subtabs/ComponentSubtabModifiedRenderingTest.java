package de.sasbe.subtabs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;

import javax.imageio.ImageIO;
import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ComponentSubtabModifiedRenderingTest extends LightPlatformTestCase {
    public void testModifiedSubtabButtonUsesPlainTextAndCustomPaint() throws Exception {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            JToggleButton button = ComponentSubtabUi.createSubtabButton("Reducer", false);
            ComponentSubtabUi.setModified(button, true);

            assertEquals("Reducer", button.getText());
            assertTrue(button instanceof ComponentSubtabToggleButton);
        });
    }

    public void testModifiedSubtabButtonRendersBluePixels() throws Exception {
        Path outputDir = Path.of("build", "test-output", "screenshots");
        Files.createDirectories(outputDir);

        ApplicationManager.getApplication().invokeAndWait(() -> {
            JToggleButton button = ComponentSubtabUi.createSubtabButton("Reducer", false);
            ComponentSubtabUi.setModified(button, true);
            Dimension size = button.getPreferredSize();
            button.setSize(size);

            BufferedImage image = UIUtil.createImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                button.paint(graphics);
            } finally {
                graphics.dispose();
            }

            Color expected = new JBColor(new Color(0x0042AA), new Color(0x589DF6));
            assertTrue("Rendered subtab should contain modified blue pixels", containsSimilarColor(image, expected));

            try {
                ImageIO.write(image, "png", outputDir.resolve("modified-subtab-button.png").toFile());
            } catch (IOException ignored) {
            }
        });
    }

    public void testModifiedPopupLabelUsesCustomPaint() throws Exception {
        Path outputDir = Path.of("build", "test-output", "screenshots");
        Files.createDirectories(outputDir);

        ApplicationManager.getApplication().invokeAndWait(() -> {
            ComponentSubtabModifiedLabel label = new ComponentSubtabModifiedLabel();
            ComponentSubtabModifiedUi.applyToLabel(label, "Reducer", true, false);
            Dimension size = label.getPreferredSize();
            label.setSize(size);

            BufferedImage image = UIUtil.createImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                label.paint(graphics);
            } finally {
                graphics.dispose();
            }

            Color expected = ComponentSubtabModifiedUi.foreground(true, false);
            assertTrue(containsSimilarColor(image, expected));

            try {
                ImageIO.write(image, "png", outputDir.resolve("modified-popup-label.png").toFile());
            } catch (IOException ignored) {
            }
        });
    }

    public void testModifiedPopupLabelUsesPlainText() {
        ComponentSubtabModifiedLabel label = new ComponentSubtabModifiedLabel();
        ComponentSubtabModifiedUi.applyToLabel(label, "Reducer", true, false);
        assertEquals("Reducer", label.getText());
    }

    public void testTreeRendererRendersModifiedGroupName() throws Exception {
        Path outputDir = Path.of("build", "test-output", "screenshots");
        Files.createDirectories(outputDir);

        ApplicationManager.getApplication().invokeAndWait(() -> {
            SimpleColoredComponent colored = new SimpleColoredComponent();
            colored.append("products-state", SimpleTextAttributes.GRAYED_ATTRIBUTES);
            colored.append(" 5 Dateien", SimpleTextAttributes.GRAY_ATTRIBUTES);
            SubtabGroupTreeCellRenderer.applyModifiedMainText(
                    colored,
                    ComponentSubtabModifiedUi.foreground(true, false)
            );

            Dimension size = colored.getPreferredSize();
            colored.setSize(size);
            BufferedImage image = UIUtil.createImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                colored.paint(graphics);
            } finally {
                graphics.dispose();
            }

            Color expected = ComponentSubtabModifiedUi.foreground(true, false);
            assertTrue(containsSimilarColor(image, expected));

            try {
                ImageIO.write(image, "png", outputDir.resolve("modified-group-node.png").toFile());
            } catch (IOException ignored) {
            }
        });
    }

    public void testTreeRendererAppliesModifiedColorToMainText() {
        SimpleColoredComponent colored = new SimpleColoredComponent();
        colored.append("products-state", SimpleTextAttributes.GRAYED_ATTRIBUTES);
        colored.append(" 5 Dateien", SimpleTextAttributes.GRAY_ATTRIBUTES);

        Color blue = ComponentSubtabModifiedUi.foreground(true, false);
        SubtabGroupTreeCellRenderer.applyModifiedMainText(colored, blue);

        SimpleColoredComponent.ColoredIterator iterator = colored.iterator();
        assertTrue(iterator.hasNext());
        iterator.next();
        assertEquals(blue, iterator.getTextAttributes().getFgColor());
    }

    private static boolean containsSimilarColor(BufferedImage image, Color expected) {
        int targetRgb = expected.getRGB();
        int targetRed = (targetRgb >> 16) & 0xFF;
        int targetGreen = (targetRgb >> 8) & 0xFF;
        int targetBlue = targetRgb & 0xFF;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int alpha = (rgb >> 24) & 0xFF;
                if (alpha < 32) {
                    continue;
                }
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                if (Math.abs(red - targetRed) <= 40
                        && Math.abs(green - targetGreen) <= 40
                        && Math.abs(blue - targetBlue) <= 40) {
                    return true;
                }
            }
        }
        return false;
    }
}
