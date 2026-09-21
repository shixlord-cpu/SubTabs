package de.sasbe.subtabs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

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
        Path outputDir = screenshotDir();
        ApplicationManager.getApplication().invokeAndWait(() -> {
            JToggleButton button = ComponentSubtabUi.createSubtabButton("Reducer", false);
            ComponentSubtabUi.setModified(button, true);
            BufferedImage image = paintButton(button);
            writeScreenshot(image, outputDir.resolve("modified-subtab-button.png"));

            Color expected = new JBColor(new Color(0x0042AA), new Color(0x589DF6));
            assertTrue("Rendered subtab should contain modified blue pixels", containsSimilarColor(image, expected));
        });
    }

    public void testSelectedModifiedSubtabRendersBlueTextNotLookAndFeelForeground() throws Exception {
        Path outputDir = screenshotDir();
        ApplicationManager.getApplication().invokeAndWait(() -> {
            JToggleButton button = ComponentSubtabUi.createSubtabButton("JSON", true);
            ComponentSubtabUi.setModified(button, true);
            BufferedImage image = paintButton(button);
            writeScreenshot(image, outputDir.resolve("selected-modified-subtab-button.png"));

            Color expected = ComponentSubtabModifiedUi.foreground(true, false);
            assertTrue(
                    "Selected modified subtab text must be blue, not the selected-button foreground",
                    containsSimilarColorInTextBand(image, expected)
            );
        });
    }

    public void testSubtabWithErrorsRendersRedWave() throws Exception {
        Path outputDir = screenshotDir();
        ApplicationManager.getApplication().invokeAndWait(() -> {
            JToggleButton button = ComponentSubtabUi.createSubtabButton("JSON", true);
            ComponentSubtabUi.setPresentation(button, true, true);
            BufferedImage image = paintButton(button);
            writeScreenshot(image, outputDir.resolve("error-wave-subtab-button.png"));

            assertTrue("Modified selected subtab stays blue", containsSimilarColorInTextBand(
                    image,
                    ComponentSubtabModifiedUi.foreground(true, false)
            ));
            assertTrue("Subtab with errors must paint a red wave underline", containsErrorWave(image));
        });
    }

    public void testPopupLabelWithErrorsRendersRedWave() throws Exception {
        Path outputDir = screenshotDir();
        ApplicationManager.getApplication().invokeAndWait(() -> {
            ComponentSubtabModifiedLabel label = new ComponentSubtabModifiedLabel();
            ComponentSubtabModifiedUi.applyToLabel(label, "JSON", true, false, true);
            Dimension size = label.getPreferredSize();
            label.setSize(Math.max(size.width, 48), Math.max(size.height, 18));

            BufferedImage image = UIUtil.createImage(label.getWidth(), label.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                label.paint(graphics);
            } finally {
                graphics.dispose();
            }
            writeScreenshot(image, outputDir.resolve("error-wave-popup-label.png"));

            assertTrue(containsSimilarColor(image, ComponentSubtabModifiedUi.foreground(true, false)));
            assertTrue(containsErrorWave(image));
        });
    }

    public void testMainTabErrorAttributesPaintRedWave() throws Exception {
        Path outputDir = screenshotDir();
        ApplicationManager.getApplication().invokeAndWait(() -> {
            SimpleColoredComponent colored = new SimpleColoredComponent();
            colored.append(
                    "tsconfig",
                    new SimpleTextAttributes(
                            SimpleTextAttributes.STYLE_WAVED,
                            ComponentSubtabModifiedUi.foreground(true, false),
                            ComponentSubtabTextPainter.errorWaveColor()
                    )
            );
            Dimension size = colored.getPreferredSize();
            colored.setSize(Math.max(size.width, 64), Math.max(size.height, 18));

            BufferedImage image = UIUtil.createImage(colored.getWidth(), colored.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                colored.paint(graphics);
            } finally {
                graphics.dispose();
            }
            writeScreenshot(image, outputDir.resolve("error-wave-main-tab.png"));

            assertTrue(containsSimilarColor(image, ComponentSubtabModifiedUi.foreground(true, false)));
            assertTrue("Main tab error style must paint a red wave", containsErrorWave(image));
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

    private static @NotNull Path screenshotDir() throws IOException {
        Path outputDir = Path.of("build", "test-output", "screenshots");
        Files.createDirectories(outputDir);
        return outputDir;
    }

    private static @NotNull BufferedImage paintButton(@NotNull JToggleButton button) {
        Dimension size = button.getPreferredSize();
        button.setSize(Math.max(size.width, 48), Math.max(size.height, 18));
        BufferedImage image = UIUtil.createImage(button.getWidth(), button.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            button.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static void writeScreenshot(@NotNull BufferedImage image, @NotNull Path path) {
        try {
            ImageIO.write(image, "png", path.toFile());
        } catch (IOException ignored) {
        }
    }

    private static boolean containsSimilarColorInTextBand(BufferedImage image, Color expected) {
        int bottomExclude = Math.max(3, image.getHeight() / 5);
        return containsSimilarColor(image, expected, 0, image.getHeight() - bottomExclude);
    }

    private static boolean containsSimilarColor(BufferedImage image, Color expected) {
        return containsSimilarColor(image, expected, 0, image.getHeight());
    }

    private static boolean containsSimilarColor(BufferedImage image, Color expected, int yStart, int yEnd) {
        int targetRgb = expected.getRGB();
        int targetRed = (targetRgb >> 16) & 0xFF;
        int targetGreen = (targetRgb >> 8) & 0xFF;
        int targetBlue = targetRgb & 0xFF;

        for (int y = Math.max(0, yStart); y < Math.min(image.getHeight(), yEnd); y++) {
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

    private static boolean containsErrorWave(BufferedImage image) {
        int yStart = Math.max(0, image.getHeight() / 3);
        int yEnd = image.getHeight() - Math.max(2, image.getHeight() / 8);
        for (int y = yStart; y < yEnd; y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int alpha = (rgb >> 24) & 0xFF;
                if (alpha < 32) {
                    continue;
                }
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                if (red >= 140 && red > green + 35 && red > blue + 35) {
                    return true;
                }
            }
        }
        return false;
    }
}
