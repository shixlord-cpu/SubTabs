package de.sasbe.subtabs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Screenshot-based checks that reorder drag matches the concept diagram:
 * hidden source tab, blue placeholder slot, shifted neighbors, floating ghost.
 */
public class ComponentSubtabBarReorderScreenshotTest extends HeavyPlatformTestCase {
    public void testFirstTabDragStatesMatchConceptDiagram() throws Exception {
        VirtualFile dir = getVirtualFile(createTempDir("reorder-screenshots"));
        VirtualFile tabA = WriteAction.computeAndWait(() -> dir.createChildData(this, "effects.component.ts"));
        VirtualFile tabB = WriteAction.computeAndWait(() -> dir.createChildData(this, "selectors.component.ts"));
        VirtualFile tabC = WriteAction.computeAndWait(() -> dir.createChildData(this, "reducer.component.ts"));

        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(tabA);
        assertNotNull(match);
        assertEquals(3, match.relatedFiles().size());
        tabA = match.relatedFiles().get(0).file();
        tabB = match.relatedFiles().get(1).file();
        tabC = match.relatedFiles().get(2).file();

        ComponentSubtabBarPanel panel = new ComponentSubtabBarPanel(
                getProject(),
                new ComponentSubtabGroup(match.relatedFiles()),
                tabA
        );
        panel.layOutTabsForTests(900);
        tabA = panel.visualFilesForTests().get(0);
        tabB = panel.visualFilesForTests().get(1);
        tabC = panel.visualFilesForTests().get(2);
        ComponentSubtabModifiedUi.applyToToggleButton(panel.buttonFor(tabA), "effects", false, false);
        ComponentSubtabModifiedUi.applyToToggleButton(panel.buttonFor(tabB), "selectors", false, false);
        ComponentSubtabModifiedUi.applyToToggleButton(panel.buttonFor(tabC), "reducer", false, false);
        panel.layOutTabsForTests(900);

        JToggleButton buttonA = panel.buttonFor(tabA);
        JToggleButton buttonB = panel.buttonFor(tabB);
        JToggleButton buttonC = panel.buttonFor(tabC);
        assertNotNull(buttonA);
        assertNotNull(buttonB);
        assertNotNull(buttonC);
        assertTrue(buttonA.getX() < buttonB.getX());
        assertTrue(buttonB.getX() < buttonC.getX());

        int startB = buttonB.getX();
        int startC = buttonC.getX();

        assertDragState(
                panel,
                tabA,
                tabB,
                tabC,
                1,
                new Point(buttonA.getX() + buttonA.getWidth() / 2, buttonA.getHeight() / 2),
                "reorder-drag-position-1-original-slot.png",
                startB,
                startC,
                true,
                false,
                false,
                tabB
        );

        assertDragState(
                panel,
                tabA,
                tabB,
                tabC,
                2,
                new Point(startB + buttonB.getWidth(), buttonB.getHeight() / 2),
                "reorder-drag-position-2-between-b-and-c.png",
                startB,
                startC,
                false,
                true,
                false,
                tabB
        );

        assertDragState(
                panel,
                tabA,
                tabB,
                tabC,
                3,
                new Point(startC + buttonC.getWidth(), buttonC.getHeight() / 2),
                "reorder-drag-position-3-end-slot.png",
                startB,
                startC,
                false,
                false,
                true,
                tabB
        );
    }

    public void testDragUsesGroupOrderWhenHostChildOrderWasStale() throws Exception {
        VirtualFile dir = getVirtualFile(createTempDir("reorder-stale-host-order"));
        VirtualFile tabA = WriteAction.computeAndWait(() -> dir.createChildData(this, "effects.component.ts"));
        VirtualFile tabB = WriteAction.computeAndWait(() -> dir.createChildData(this, "selectors.component.ts"));
        VirtualFile tabC = WriteAction.computeAndWait(() -> dir.createChildData(this, "reducer.component.ts"));

        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(tabA);
        assertNotNull(match);
        tabA = match.relatedFiles().get(0).file();
        tabB = match.relatedFiles().get(1).file();
        tabC = match.relatedFiles().get(2).file();

        ComponentSubtabBarPanel panel = new ComponentSubtabBarPanel(
                getProject(),
                new ComponentSubtabGroup(match.relatedFiles()),
                tabA
        );
        panel.layOutTabsForTests(900);
        panel.scrambleTabsHostOrderForTests();

        panel.applyReorderLayoutForTests(tabA, 2);
        panel.updateReorderDragState(
                2,
                tabA,
                new Point(panel.reorderGapXForTests() + panel.reorderGapWidthForTests() / 2, 8)
        );
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertTrue(panel.isReorderHiddenForTests(tabA));
        assertEquals(tabB, panel.leftmostVisibleTabFileForTests());
        assertTrue(panel.buttonFor(tabC).getX() > panel.reorderGapXForTests());
        assertTrue(panel.buttonFor(tabB).getX() < panel.reorderGapXForTests());
    }

    private static void assertDragState(
            @NotNull ComponentSubtabBarPanel panel,
            @NotNull VirtualFile dragged,
            @NotNull VirtualFile tabB,
            @NotNull VirtualFile tabC,
            int dropIndex,
            @NotNull Point pointer,
            @NotNull String screenshotName,
            int startBX,
            int startCX,
            boolean neighborsUnshifted,
            boolean bShiftedLeft,
            boolean cShiftedLeft,
            @NotNull VirtualFile expectedLeftmost
    ) throws Exception {
        panel.clearReorderPreview();
        panel.layOutTabsForTests(900);
        Point inTabs = new Point(pointer);
        int resolved = panel.resolveReorderDropIndex(inTabs, dragged);
        assertEquals("drop index for " + screenshotName, dropIndex, resolved);
        panel.updateReorderDragState(resolved, dragged, inTabs);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        JToggleButton buttonB = panel.buttonFor(tabB);
        JToggleButton buttonC = panel.buttonFor(tabC);
        assertTrue(panel.isReorderHiddenForTests(dragged));
        assertTrue(panel.buttonFor(dragged).getX() < -1000);
        assertEquals("leftmost visible tab", expectedLeftmost, panel.leftmostVisibleTabFileForTests());
        assertFalse("dragged tab must not stay leftmost", dragged.equals(panel.leftmostVisibleTabFileForTests()));
        if (neighborsUnshifted) {
            assertEquals(startBX, buttonB.getX());
            assertEquals(startCX, buttonC.getX());
        }
        if (bShiftedLeft) {
            assertTrue(buttonB.getX() < startBX);
        }
        if (cShiftedLeft) {
            assertTrue(buttonC.getX() < startCX);
        }
        if (dropIndex == 1) {
            assertTrue(panel.reorderGapXForTests() < buttonB.getX());
        }
        if (dropIndex == 2) {
            assertTrue(buttonC.getX() > panel.reorderGapXForTests());
            assertTrue(buttonB.getX() < panel.reorderGapXForTests());
        }
        if (dropIndex == 3) {
            assertTrue(panel.reorderGapXForTests() > buttonC.getX());
        }
        assertTrue(panel.reorderGapWidthForTests() > 0);
        assertTrue(
                "blue placeholder expected at drop index " + dropIndex,
                panel.reorderGapXForTests() >= 0
        );

        Path screenshot = screenshotPath(screenshotName);
        BufferedImage image = panel.paintTabsHostForTests();
        ImageIO.write(image, "png", screenshot.toFile());
        assertTrue("screenshot saved: " + screenshot, Files.exists(screenshot));
        assertTrue(
                "screenshot must contain the blue drop placeholder: " + screenshot,
                regionHasAccentColor(
                        image,
                        panel.reorderGapXForTests(),
                        buttonB.getY(),
                        panel.reorderGapWidthForTests(),
                        buttonB.getHeight()
                )
        );
        assertTrue(
                "dragged tab must not paint in the row: " + screenshot,
                !regionHasInk(image, panel.buttonFor(dragged))
        );
        assertTrue(
                "remaining tabs must stay visible: " + screenshot,
                regionHasInk(image, buttonB) && regionHasInk(image, buttonC)
        );
    }

    private static Path screenshotPath(@NotNull String name) throws Exception {
        Path outputDir = Path.of("build", "test-output", "screenshots");
        Files.createDirectories(outputDir);
        return outputDir.resolve(name);
    }

    private static boolean regionHasAccentColor(
            @NotNull BufferedImage image,
            int x,
            int y,
            int width,
            int height
    ) {
        Color accent = JBUI.CurrentTheme.TabbedPane.ENABLED_SELECTED_COLOR;
        int maxX = Math.min(image.getWidth(), x + width);
        int maxY = Math.min(image.getHeight(), y + height);
        int top = Math.max(0, y);
        int left = Math.max(0, x);
        int matches = 0;
        for (int px = left; px < maxX; px++) {
            if (colorNear(new Color(image.getRGB(px, top), true), accent, 80)) {
                matches++;
            }
            if (colorNear(new Color(image.getRGB(px, maxY - 1), true), accent, 80)) {
                matches++;
            }
        }
        for (int py = top; py < maxY; py++) {
            if (colorNear(new Color(image.getRGB(left, py), true), accent, 80)) {
                matches++;
            }
            if (colorNear(new Color(image.getRGB(maxX - 1, py), true), accent, 80)) {
                matches++;
            }
        }
        return matches >= 8;
    }

    private static boolean colorNear(@NotNull Color actual, @NotNull Color expected, int tolerance) {
        return Math.abs(actual.getRed() - expected.getRed()) <= tolerance
                && Math.abs(actual.getGreen() - expected.getGreen()) <= tolerance
                && Math.abs(actual.getBlue() - expected.getBlue()) <= tolerance
                && actual.getAlpha() > 0;
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
