package de.sasbe.subtabs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.ui.UIUtil;

import javax.imageio.ImageIO;
import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * TESTE_HART: drives the real subtab bar, updates it through document events (no reopen),
 * and checks screenshots of that bar.
 */
public class ComponentSubtabPresentationUpdateTest extends RealEditorWindowTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings.getInstance().setSubtabsActive(true);
        ComponentSubtabsDocumentListener.install(getProject());
    }

    public void testDocumentChangeTurnsSelectedSubtabBlueWithoutReopen() throws Exception {
        VirtualFile jsonFile = createSourceFile("tsconfig.json");
        WriteAction.run(() -> jsonFile.setBinaryContent("""
                {
                  "compilerOptions": {}
                }
                """.getBytes(StandardCharsets.UTF_8)));
        createSourceFile("tsconfig.app.json");
        openAndSettle(jsonFile);

        ComponentSubtabBarPanel bar = barFor(jsonFile);
        JToggleButton button = bar.buttonFor(jsonFile);
        assertNotNull(button);
        assertTrue("the open file must be the selected subtab", button.isSelected());
        assertFalse(ComponentSubtabUi.isModified(button));

        Document document = FileDocumentManager.getInstance().getDocument(jsonFile);
        assertNotNull(document);
        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                document.insertString(document.getTextLength(), "\n\"x\": 1"));
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertTrue(
                "the selected subtab must turn modified from the document event, without close/reopen",
                ComponentSubtabUi.isModified(button)
        );

        Path screenshot = screenshotPath("teste-hart-bar-after-edit.png");
        BufferedImage image = paintProductButton(button);
        ImageIO.write(image, "png", screenshot.toFile());
        ImageIO.write(image, "png", screenshot.toFile());
        assertTrue(
                "real subtab bar must show modified blue on the selected tab after an edit: " + screenshot,
                containsBlueText(image)
        );
    }

    public void testSelectedSubtabShowsErrorWaveOnRealBar() throws Exception {
        VirtualFile jsonFile = createSourceFile("tsconfig.json");
        WriteAction.run(() -> jsonFile.setBinaryContent("{".getBytes(StandardCharsets.UTF_8)));
        createSourceFile("tsconfig.app.json");
        openAndSettle(jsonFile);

        Document document = FileDocumentManager.getInstance().getDocument(jsonFile);
        assertNotNull(document);
        WriteCommandAction.runWriteCommandAction(getProject(), () -> document.setText("{"));
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        ComponentSubtabBarPanel bar = barFor(jsonFile);
        JToggleButton button = bar.buttonFor(jsonFile);
        assertNotNull(button);

        var model = com.intellij.openapi.editor.impl.DocumentMarkupModel.forDocument(document, getProject(), true);
        var highlighter = model.addRangeHighlighter(
                0,
                1,
                com.intellij.openapi.editor.markup.HighlighterLayer.ERROR,
                null,
                com.intellij.openapi.editor.markup.HighlighterTargetArea.EXACT_RANGE
        );
        highlighter.setErrorStripeTooltip(
                com.intellij.codeInsight.daemon.impl.HighlightInfo.newHighlightInfo(
                                com.intellij.codeInsight.daemon.impl.HighlightInfoType.ERROR)
                        .range(0, 1)
                        .descriptionAndTooltip("invalid")
                        .createUnconditionally()
        );
        ComponentSubtabsManager.refreshModifiedStateForFile(getProject(), jsonFile);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertTrue("markup errors must mark the subtab", ComponentSubtabUi.hasErrors(button));

        Path screenshot = screenshotPath("teste-hart-bar-error-wave.png");
        BufferedImage image = paintProductButton(button);
        ImageIO.write(image, "png", screenshot.toFile());
        assertTrue("selected subtab stays blue while showing errors: " + screenshot, containsBlueText(image));
        assertTrue("real subtab bar must paint a red error wave: " + screenshot, containsRedWave(image));
    }

    public void testStampMismatchCountsAsModifiedImmediately() throws Exception {
        VirtualFile jsonFile = createSourceFile("plain.json");
        Document document = FileDocumentManager.getInstance().getDocument(jsonFile);
        assertNotNull(document);
        assertFalse(ComponentSubtabModifiedUi.isModified(getProject(), jsonFile));

        WriteCommandAction.runWriteCommandAction(getProject(), () -> document.insertString(0, " "));

        assertTrue(
                "modification stamp must mark the file dirty without waiting for FileDocumentManager",
                document.getModificationStamp() != jsonFile.getModificationStamp()
        );
        assertTrue(ComponentSubtabFilePresentation.computeForDocument(getProject(), document).modified());
        assertTrue(ComponentSubtabModifiedUi.isModified(getProject(), jsonFile));
    }

    private ComponentSubtabBarPanel barFor(VirtualFile file) {
        PlatformTestUtil.waitWithEventsDispatching(
                "no subtab bar appeared for " + file.getName(),
                () -> attachedBarFor(file) != null,
                30
        );
        return attachedBarFor(file);
    }

    private ComponentSubtabBarPanel attachedBarFor(VirtualFile file) {
        for (FileEditor editor : ComponentSubtabsManager.editorsFor(
                FileEditorManager.getInstance(getProject()), file)) {
            ComponentSubtabBarPanel panel = editor.getUserData(ComponentSubtabsManager.SUBTAB_BAR_KEY);
            if (panel != null) {
                return panel;
            }
        }
        return null;
    }

    private static Path screenshotPath(String name) throws Exception {
        Path outputDir = Path.of("build", "test-output", "screenshots");
        Files.createDirectories(outputDir);
        return outputDir.resolve(name);
    }

    private static BufferedImage paintProductButton(JToggleButton button) {
        Dimension size = button.getPreferredSize();
        button.setSize(Math.max(size.width, 72), Math.max(size.height, 22));
        BufferedImage image = UIUtil.createImage(button.getWidth(), button.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            button.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static boolean containsBlueText(BufferedImage image) {
        Color expected = ComponentSubtabModifiedUi.foreground(true, false);
        int bottomExclude = Math.max(3, image.getHeight() / 5);
        int targetRed = expected.getRed();
        int targetGreen = expected.getGreen();
        int targetBlue = expected.getBlue();
        for (int y = 0; y < image.getHeight() - bottomExclude; y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (closeTo(image.getRGB(x, y), targetRed, targetGreen, targetBlue)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsRedWave(BufferedImage image) {
        int yStart = Math.max(0, image.getHeight() / 4);
        int yEnd = image.getHeight() - 1;
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

    private static boolean closeTo(int rgb, int targetRed, int targetGreen, int targetBlue) {
        int alpha = (rgb >> 24) & 0xFF;
        if (alpha < 32) {
            return false;
        }
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        return Math.abs(red - targetRed) <= 40
                && Math.abs(green - targetGreen) <= 40
                && Math.abs(blue - targetBlue) <= 40;
    }
}
