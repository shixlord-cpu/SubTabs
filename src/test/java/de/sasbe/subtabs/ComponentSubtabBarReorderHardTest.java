package de.sasbe.subtabs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * TESTE_HART: real editor subtab bar, production reorder-drag path, screenshot of the row.
 */
public class ComponentSubtabBarReorderHardTest extends RealEditorWindowTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setSubtabsActive(true);
        settings.setShowCollapseButton(false);
        settings.setRules(SubtabRulesDefaults.createDefaults());
        ComponentSubtabsDocumentListener.install(getProject());
    }

    public void testDraggingMiddleSubtabRemovesOnlyThatTabFromTheRealBar() throws Exception {
        VirtualFile dir = WriteAction.computeAndWait(() -> sourceDir.createChildDirectory(this, "product-list"));
        VirtualFile htmlFile = WriteAction.computeAndWait(() -> dir.createChildData(this, "product-list.component.html"));
        WriteAction.run(() -> htmlFile.setBinaryContent("<p>list</p>".getBytes(StandardCharsets.UTF_8)));
        VirtualFile scssFile = WriteAction.computeAndWait(() -> dir.createChildData(this, "product-list.component.scss"));
        WriteAction.run(() -> scssFile.setBinaryContent(".list{}".getBytes(StandardCharsets.UTF_8)));
        VirtualFile tsFile = WriteAction.computeAndWait(() -> dir.createChildData(this, "product-list.component.ts"));
        WriteAction.run(() -> tsFile.setBinaryContent("export const x = 1;".getBytes(StandardCharsets.UTF_8)));

        openAndSettle(htmlFile);
        ComponentSubtabsManager.attachIfNeeded(getProject(), htmlFile);
        ComponentSubtabBarPanel panel = barFor(htmlFile);
        assertNotNull(panel.buttonFor(scssFile));
        assertNotNull(panel.buttonFor(tsFile));

        panel.layOutTabsForTests(900);
        int fromIndex = panel.tabIndexForFile(scssFile);
        JToggleButton dragged = panel.buttonFor(scssFile);
        panel.updateReorderDragState(
                fromIndex + 1,
                scssFile,
                new Point(dragged.getX() + Math.max(dragged.getWidth(), 1) / 2, Math.max(dragged.getHeight(), 1) / 2)
        );
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertTrue("dragged subtab leaves the row", dragged.getX() < -1000);
        assertTrue("html stays in the row", panel.buttonFor(htmlFile).getX() >= 0 && panel.buttonFor(htmlFile).getWidth() > 0);
        assertTrue("ts stays in the row", panel.buttonFor(tsFile).getX() >= 0 && panel.buttonFor(tsFile).getWidth() > 0);
        assertTrue(
                "only the dragged subtab is suppressed",
                panel.isReorderHiddenForTests(scssFile)
                        && !panel.isReorderHiddenForTests(htmlFile)
                        && !panel.isReorderHiddenForTests(tsFile)
        );
        assertTrue(panel.reorderGapXForTests() >= panel.buttonFor(htmlFile).getX());
        assertTrue(panel.buttonFor(tsFile).getX() > panel.reorderGapXForTests());

        Path screenshot = screenshotPath("reorder-drag-hides-only-dragged-tab.png");
        BufferedImage image = paintTabStrip(panel.buttonFor(htmlFile));
        ImageIO.write(image, "png", screenshot.toFile());
        assertTrue("screenshot must show the bar: " + screenshot, image.getWidth() > 50 && image.getHeight() > 8);
        assertTrue(
                "screenshot must keep the non-dragged tabs: " + screenshot,
                regionHasInk(image, panel.buttonFor(htmlFile)) && regionHasInk(image, panel.buttonFor(tsFile))
        );
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

    private static BufferedImage paintTabStrip(@NotNull JToggleButton buttonInStrip) {
        JComponent tabs = (JComponent) buttonInStrip.getParent();
        int width = Math.max(tabs.getWidth(), 900);
        int height = Math.max(tabs.getHeight(), 24);
        tabs.setSize(width, height);
        tabs.doLayout();
        tabs.setDoubleBuffered(false);
        BufferedImage image = UIUtil.createImage(tabs, width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setClip(0, 0, width, height);
            tabs.print(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static boolean regionHasInk(@NotNull BufferedImage image, @NotNull JToggleButton button) {
        int x = Math.max(0, button.getX());
        int y = Math.max(0, button.getY());
        int width = Math.min(button.getWidth(), image.getWidth() - x);
        int height = Math.min(button.getHeight(), image.getHeight() - y);
        if (width <= 2 || height <= 2) {
            return false;
        }
        int first = image.getRGB(x + 1, y + 1);
        int different = 0;
        int samples = 0;
        for (int py = y + 1; py < y + height - 1; py += 2) {
            for (int px = x + 1; px < x + width - 1; px += 2) {
                samples++;
                if (image.getRGB(px, py) != first) {
                    different++;
                }
            }
        }
        return samples > 4 && different > 2;
    }
}
