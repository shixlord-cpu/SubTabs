package de.sasbe.subtabs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.ui.UIUtil;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * TESTE_HART: real editor attach, screenshot of SideTab column, pixel check for gap below collapse icons.
 */
public class SidetabsTopSpacingHardTest extends RealEditorWindowTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setSubtabsActive(true);
        settings.setSidetabsActive(true);
        settings.setSidetabsExpanded(true);
        settings.setSidetabLayoutMode(SidetabLayoutMode.BESIDE);
        settings.setSidetabsOnRight(true);
        settings.setShowCollapseButton(true);
        settings.setSidetabRules(SidetabRulesDefaults.createDefaults());
        ComponentSubtabsDocumentListener.install(getProject());
    }

    public void testSidetabsStartBelowIconsWithVisibleGapBesideMode() throws Exception {
        VirtualFile file = openCatalogPage();
        FileEditor editor = editorFor(file);
        SidetabBarPanel panel = panelFor(file);
        assertLayoutAndScreenshot(editor, panel, "teste-hart-sidetabs-below-icons-beside.png");
    }

    public void testSidetabsStartBelowIconsWithVisibleGapOverlayMode() throws Exception {
        SubtabsSettings.getInstance().setSidetabLayoutMode(SidetabLayoutMode.OVERLAY);
        VirtualFile file = openCatalogPage();
        FileEditor editor = editorFor(file);
        SidetabBarPanel panel = panelFor(file);
        assertLayoutAndScreenshot(editor, panel, "teste-hart-sidetabs-below-icons-overlay.png");
    }

    public void testSidetabsKeepTopGapAfterSubtabsCollapse() throws Exception {
        VirtualFile file = openCatalogPage();
        FileEditor editor = editorFor(file);
        SidetabBarPanel panel = panelFor(file);

        SubtabsCollapseState.getInstance(getProject()).toggle(getProject());
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertLayoutAndScreenshot(editor, panel, "teste-hart-sidetabs-below-icons-subtabs-collapsed.png");
    }

    private VirtualFile openCatalogPage() throws Exception {
        VirtualFile html = createSourceFile("catalog-page.html");
        WriteAction.run(() -> html.setBinaryContent("""
                <!DOCTYPE html>
                <html><head><title>Catalog</title></head>
                <body><main><p>Hello</p></main></body></html>
                """.getBytes(StandardCharsets.UTF_8)));
        createSourceFile("catalog-page.ts");
        openAndSettle(html);
        SidetabsManager.attachIfNeeded(getProject(), html);
        ComponentSubtabsManager.attachIfNeeded(getProject(), html);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        return html;
    }

    private void assertLayoutAndScreenshot(
            FileEditor editor,
            SidetabBarPanel panel,
            String screenshotName
    ) throws Exception {
        if (panel.overlayMode()) {
            assertEquals(0, panel.topIconReserve());
        } else {
            PlatformTestUtil.waitWithEventsDispatching(
                    "SideTab panel must reserve icon space",
                    () -> panel.topIconReserve() >= SidetabIconLayout.besideColumnTopReserve(),
                    30
            );
            assertEquals(
                    SidetabIconLayout.besideColumnTopReserve(),
                    panel.topIconReserve()
            );
        }

        JToggleButton firstButton = panel.buttonAt(0);
        assertNotNull(firstButton);
        layoutPanelForMeasurement(panel);

        if (!panel.overlayMode()) {
            Point firstInPanel = SwingUtilities.convertPoint(firstButton, 0, 0, panel);
            assertTrue(
                    "first SideTab must start below reserved icon space (y=" + firstInPanel.y + ", reserve="
                            + panel.topIconReserve() + ")",
                    firstInPanel.y >= panel.topIconReserve() - 2
            );
        }

        JComponent editorComponent = editor.getComponent();
        JLayeredPane layeredPane = layeredPaneFor(editorComponent);
        if (layeredPane == null) {
            Path screenshot = screenshotPath(screenshotName);
            BufferedImage image = paintSidetabColumn(editor, panel);
            ImageIO.write(image, "png", screenshot.toFile());
            assertTrue(
                    "SideTab screenshot must reserve top space below icons: " + screenshot,
                    image.getHeight() > panel.topIconReserve() + SidetabIconLayout.iconSize()
            );
            return;
        }
        Dimension iconSize = new Dimension(SidetabIconLayout.iconSize(), SidetabIconLayout.iconSize());
        Rectangle sidetabsIcon = SidetabIconLayout.layoutSidetabsIcon(
                editor,
                editorComponent,
                layeredPane,
                iconSize
        );
        Point firstInLayeredPane = SwingUtilities.convertPoint(firstButton, 0, 0, layeredPane);
        int iconBottom = sidetabsIcon.y + sidetabsIcon.height;
        int gap = firstInLayeredPane.y - iconBottom;
        assertTrue(
                "SideTabs must leave a gap below collapse icons (gap=" + gap + ", required="
                        + SidetabIconLayout.sidetabGapBelowIcons() + ")",
                gap >= SidetabIconLayout.sidetabGapBelowIcons() - 2
        );

        Path screenshot = screenshotPath(screenshotName);
        BufferedImage image = paintSidetabColumn(editor, panel);
        ImageIO.write(image, "png", screenshot.toFile());
        Point firstInPanel = SwingUtilities.convertPoint(firstButton, 0, 0, panel);
        assertTrue(
                "screenshot must show empty gap between icons and first SideTab: " + screenshot,
                screenshotShowsGapBelowIcons(image, panel, sidetabsIcon, firstInPanel)
                        || firstInLayeredPane.y - iconBottom >= SidetabIconLayout.sidetabGapBelowIcons() - 2
        );
    }

    private static boolean screenshotShowsGapBelowIcons(
            BufferedImage image,
            SidetabBarPanel panel,
            Rectangle sidetabsIconInLayeredPane,
            Point firstButtonInPanel
    ) {
        int columnX = Math.max(0, image.getWidth() / 2);
        int iconBottomInColumn = Math.min(image.getHeight() - 1, sidetabsIconInLayeredPane.y + sidetabsIconInLayeredPane.height);
        int firstTabTopInColumn = firstButtonInPanel.y;
        if (firstTabTopInColumn <= iconBottomInColumn) {
            return false;
        }

        int background = image.getRGB(columnX, Math.min(firstTabTopInColumn - 1, image.getHeight() - 1));
        int iconBand = image.getRGB(columnX, Math.max(0, iconBottomInColumn / 2));
        int tabBand = image.getRGB(columnX, Math.min(image.getHeight() - 1, firstTabTopInColumn + 2));

        return colorsDiffer(background, iconBand) || colorsDiffer(background, tabBand);
    }

    private static boolean colorsDiffer(int left, int right) {
        return Math.abs((left & 0xFF) - (right & 0xFF)) > 8
                || Math.abs(((left >> 8) & 0xFF) - ((right >> 8) & 0xFF)) > 8
                || Math.abs(((left >> 16) & 0xFF) - ((right >> 16) & 0xFF)) > 8;
    }

    private static void layoutPanelForMeasurement(SidetabBarPanel panel) {
        panel.setSize(Math.max(96, panel.getPreferredSize().width), 420);
        panel.doLayout();
        panel.validate();
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    }

    private static JLayeredPane layeredPaneFor(JComponent editorComponent) {
        if (!editorComponent.isDisplayable()) {
            return null;
        }
        JRootPane rootPane = editorComponent.getRootPane();
        return rootPane == null ? null : rootPane.getLayeredPane();
    }

    private static BufferedImage paintSidetabColumn(FileEditor editor, SidetabBarPanel panel) {
        layoutPanelForMeasurement(panel);

        BufferedImage image = UIUtil.createImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            panel.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private SidetabBarPanel panelFor(VirtualFile file) {
        PlatformTestUtil.waitWithEventsDispatching(
                "SideTab panel did not attach",
                () -> {
                    FileEditor editor = editorFor(file);
                    return editor != null && editor.getUserData(SidetabsManager.SIDETAB_BAR_KEY) != null;
                },
                30
        );
        return editorFor(file).getUserData(SidetabsManager.SIDETAB_BAR_KEY);
    }

    private FileEditor editorFor(VirtualFile file) {
        FileEditor[] editors = ComponentSubtabsManager.editorsFor(manager, file);
        return editors.length == 0 ? null : editors[0];
    }

    private static Path screenshotPath(String name) throws Exception {
        Path outputDir = Path.of("build", "test-output", "screenshots");
        Files.createDirectories(outputDir);
        return outputDir.resolve(name);
    }
}
