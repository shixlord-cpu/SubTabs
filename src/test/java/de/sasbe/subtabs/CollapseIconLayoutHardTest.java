package de.sasbe.subtabs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.ui.UIUtil;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TESTE_HART: opens real editors, mounts the real editor-composite anchor in a visible frame,
 * reads actual overlay bounds, and screenshots product-list.component.html vs dual baseline.
 */
public class CollapseIconLayoutHardTest extends RealEditorWindowTestCase {
    private static final int TOLERANCE = 2;
    private static final int FRAME_WIDTH = 960;
    private static final int FRAME_HEIGHT = 640;

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

    public void testProductListHtmlMatchesDualLayoutIconOnRealMountedChrome() throws Exception {
        VirtualFile dualHtml = createSourceFile("header.component.html");
        WriteAction.run(() -> dualHtml.setBinaryContent("""
                <header>Title</header>
                <main>Content</main>
                """.getBytes(StandardCharsets.UTF_8)));
        createSourceFile("header.component.ts");
        openAndSettle(dualHtml);
        SidetabsManager.attachIfNeeded(getProject(), dualHtml);
        ComponentSubtabsManager.attachIfNeeded(getProject(), dualHtml);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        VirtualFile html = createSourceFile("product-list.component.html");
        WriteAction.run(() -> html.setBinaryContent("""
                <ul class="product-list">
                  <li>Product A</li>
                  <li>Product B</li>
                </ul>
                """.getBytes(StandardCharsets.UTF_8)));
        createSourceFile("product-list.component.ts");
        createSourceFile("product-list.component.scss");
        openAndSettle(html);
        SidetabsManager.attachIfNeeded(getProject(), html);
        ComponentSubtabsManager.attachIfNeeded(getProject(), html);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        FileEditor dualEditor = editorFor(dualHtml);
        FileEditor productEditor = editorFor(html);

        assertNull(SidetabsToggleOverlay.visibleButton(productEditor));
        assertNull(ComponentSubtabsManager.visibleCollapseButton(productEditor));
        assertTrue(SubtabsCollapseOverlay.isInstalled(productEditor));

        MeasuredChrome dual = measureChrome(dualEditor, "teste-hart-dual-header-icon-baseline.png");
        MeasuredChrome product = measureChrome(productEditor, "teste-hart-product-list-subtabs-only-icon.png");

        assertNear("product-list SubTabs X vs dual baseline", dual.iconBounds.x, product.iconBounds.x);
        assertNear("product-list SubTabs Y vs dual baseline", dual.iconBounds.y, product.iconBounds.y);
        assertTrue(
                "dual baseline screenshot must show the collapse icon row: " + dual.screenshot,
                screenshotShowsIcon(dual.image, dual.iconBounds)
        );
        assertTrue(
                "product-list screenshot must show the collapse icon above the code area: " + product.screenshot,
                screenshotShowsIcon(product.image, product.iconBounds)
        );
    }

    public void testDualFileSubtabsIconReturnsToOverlayRowAfterBothCollapsedThenSubtabsExpanded() throws Exception {
        VirtualFile html = createSourceFile("header.component.html");
        WriteAction.run(() -> html.setBinaryContent("""
                <header>Title</header>
                <main>Content</main>
                """.getBytes(StandardCharsets.UTF_8)));
        createSourceFile("header.component.ts");
        openAndSettle(html);
        SidetabsManager.attachIfNeeded(getProject(), html);
        ComponentSubtabsManager.attachIfNeeded(getProject(), html);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        SidetabsCollapseState.getInstance(getProject()).setCollapsed(getProject(), true);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        FileEditor editor = editorFor(html);
        DualMeasuredChrome baseline = measureDualIcons(editor, "teste-hart-dual-header-restore-baseline.png");

        SubtabsCollapseState.getInstance(getProject()).setCollapsed(getProject(), true);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        SubtabsCollapseState.getInstance(getProject()).setCollapsed(getProject(), false);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        editor = editorFor(html);
        assertNull(ComponentSubtabsManager.visibleCollapseButton(editor));
        assertTrue(SubtabsCollapseOverlay.isInstalled(editor));
        assertTrue(SidetabsToggleOverlay.isInstalled(editor));

        DualMeasuredChrome restored = measureDualIcons(editor, "teste-hart-dual-header-restore-after-subtabs.png");
        assertNear("restored SubTabs X", baseline.subtabsIconBounds.x, restored.subtabsIconBounds.x);
        assertNear("restored SubTabs Y", baseline.subtabsIconBounds.y, restored.subtabsIconBounds.y);
        assertNear("restored SideTabs X", baseline.sidetabsIconBounds.x, restored.sidetabsIconBounds.x);
        assertNear("restored SideTabs Y", baseline.sidetabsIconBounds.y, restored.sidetabsIconBounds.y);
        assertNear("restored icons share one row", baseline.sidetabsIconBounds.y, restored.subtabsIconBounds.y);
    }

    public void testProductListHtmlMatchesDualLayoutWhenSidetabsGloballyCollapsed() throws Exception {
        VirtualFile dualHtml = createSourceFile("header.component.html");
        WriteAction.run(() -> dualHtml.setBinaryContent("""
                <header>Title</header>
                <main>Content</main>
                """.getBytes(StandardCharsets.UTF_8)));
        createSourceFile("header.component.ts");
        openAndSettle(dualHtml);
        SidetabsManager.attachIfNeeded(getProject(), dualHtml);
        ComponentSubtabsManager.attachIfNeeded(getProject(), dualHtml);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        MeasuredChrome expandedDual = measureChrome(editorFor(dualHtml), "teste-hart-dual-expanded-baseline.png");

        VirtualFile html = createSourceFile("product-list.component.html");
        WriteAction.run(() -> html.setBinaryContent("""
                <ul class="product-list">
                  <li>Product A</li>
                </ul>
                """.getBytes(StandardCharsets.UTF_8)));
        createSourceFile("product-list.component.ts");
        createSourceFile("product-list.component.scss");

        SubtabsSettings.getInstance().setSidetabsExpanded(false);
        openAndSettle(html);
        SidetabsManager.attachIfNeeded(getProject(), html);
        ComponentSubtabsManager.attachIfNeeded(getProject(), html);
        ComponentSubtabsManager.applyPresentationState(getProject());
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        MeasuredChrome product = measureChrome(
                editorFor(html),
                "teste-hart-product-list-subtabs-only-collapsed-global.png"
        );
        assertNear("collapsed-global SubTabs X", expandedDual.iconBounds.x, product.iconBounds.x);
        assertNear("collapsed-global SubTabs Y", expandedDual.iconBounds.y, product.iconBounds.y);
    }

    private DualMeasuredChrome measureDualIcons(FileEditor editor, String screenshotName) throws Exception {
        AtomicReference<DualMeasuredChrome> result = new AtomicReference<>();
        AtomicReference<Exception> failure = new AtomicReference<>();
        ApplicationManager.getApplication().invokeAndWait(() -> {
            JFrame frame = new JFrame();
            try {
                JComponent anchor = collapseIconAnchor(editor);
                if (anchor.getParent() instanceof Container previousParent) {
                    previousParent.remove(anchor);
                }
                anchor.setSize(FRAME_WIDTH, FRAME_HEIGHT);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setContentPane(anchor);
                frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                frame.validate();
                SubtabsCollapseOverlay.relayout(editor);
                SubtabsExpandOverlay.relayout(editor);
                SidetabsToggleOverlay.relayout(editor);

                JComponent subtabsButton = firstShowing(
                        SubtabsCollapseOverlay.visibleButton(editor),
                        SubtabsExpandOverlay.visibleButton(editor),
                        null
                );
                JComponent sidetabsButton = firstShowing(
                        SidetabsToggleOverlay.visibleButton(editor),
                        null,
                        null
                );
                assertNotNull("SubTabs collapse overlay must be visible on mounted chrome", subtabsButton);
                assertNotNull("SideTabs toggle overlay must be visible on mounted chrome", sidetabsButton);

                Rectangle subtabsIconBounds = iconBoundsOnAnchor(anchor, subtabsButton);
                Rectangle sidetabsIconBounds = iconBoundsOnAnchor(anchor, sidetabsButton);

                BufferedImage image = UIUtil.createImage(anchor, FRAME_WIDTH, FRAME_HEIGHT, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                try {
                    anchor.paint(graphics);
                    paintPopupLayer(frame, anchor, graphics);
                } finally {
                    graphics.dispose();
                }

                Path screenshot = screenshotPath(screenshotName);
                ImageIO.write(image, "png", screenshot.toFile());
                result.set(new DualMeasuredChrome(subtabsIconBounds, sidetabsIconBounds, screenshot, image));
            } catch (Exception exception) {
                failure.set(exception);
            } finally {
                frame.dispose();
            }
        });
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        if (failure.get() != null) {
            throw failure.get();
        }
        assertNotNull(result.get());
        return result.get();
    }

    private MeasuredChrome measureChrome(FileEditor editor, String screenshotName) throws Exception {
        AtomicReference<MeasuredChrome> result = new AtomicReference<>();
        AtomicReference<Exception> failure = new AtomicReference<>();
        ApplicationManager.getApplication().invokeAndWait(() -> {
            JFrame frame = new JFrame();
            try {
                JComponent anchor = collapseIconAnchor(editor);
                if (anchor.getParent() instanceof Container previousParent) {
                    previousParent.remove(anchor);
                }
                anchor.setSize(FRAME_WIDTH, FRAME_HEIGHT);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setContentPane(anchor);
                frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                frame.validate();
                SubtabsCollapseOverlay.relayout(editor);
                SubtabsExpandOverlay.relayout(editor);
                SidetabsToggleOverlay.relayout(editor);

                JComponent button = firstShowing(
                        SubtabsCollapseOverlay.visibleButton(editor),
                        SubtabsExpandOverlay.visibleButton(editor),
                        SidetabsToggleOverlay.visibleButton(editor)
                );
                assertNotNull("collapse overlay must be visible on mounted chrome", button);
                Point origin = SwingUtilities.convertPoint(button, 0, 0, anchor);
                Rectangle iconBounds = new Rectangle(origin.x, origin.y, button.getWidth(), button.getHeight());

                BufferedImage image = UIUtil.createImage(anchor, FRAME_WIDTH, FRAME_HEIGHT, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                try {
                    anchor.paint(graphics);
                    paintPopupLayer(frame, anchor, graphics);
                } finally {
                    graphics.dispose();
                }

                Path screenshot = screenshotPath(screenshotName);
                ImageIO.write(image, "png", screenshot.toFile());
                result.set(new MeasuredChrome(iconBounds, screenshot, image));
            } catch (Exception exception) {
                failure.set(exception);
            } finally {
                frame.dispose();
            }
        });
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        if (failure.get() != null) {
            throw failure.get();
        }
        assertNotNull(result.get());
        return result.get();
    }

    private static Rectangle iconBoundsOnAnchor(JComponent anchor, JComponent button) {
        Point origin = SwingUtilities.convertPoint(button, 0, 0, anchor);
        return new Rectangle(origin.x, origin.y, button.getWidth(), button.getHeight());
    }

    private static void paintPopupLayer(JFrame frame, JComponent anchor, Graphics2D graphics) {
        JLayeredPane layeredPane = frame.getLayeredPane();
        for (Component component : layeredPane.getComponentsInLayer(JLayeredPane.POPUP_LAYER)) {
            if (!component.isShowing()) {
                continue;
            }
            Point overlayOrigin = SwingUtilities.convertPoint(component, 0, 0, anchor);
            graphics.translate(overlayOrigin.x, overlayOrigin.y);
            component.paint(graphics);
            graphics.translate(-overlayOrigin.x, -overlayOrigin.y);
        }
    }

    private static JComponent collapseIconAnchor(FileEditor editor) {
        JComponent editorComponent = editor.getComponent();
        JComponent composite = findEditorComposite(editorComponent);
        if (composite != null) {
            return composite;
        }
        SidetabEditorHost host = SidetabEditorHost.findHost(editorComponent);
        if (host != null) {
            return host;
        }
        return editorContentWrapper(editorComponent);
    }

    private static JComponent editorContentWrapper(JComponent editorComponent) {
        Container parent = editorComponent.getParent();
        return parent instanceof JComponent parentComponent ? parentComponent : editorComponent;
    }

    private static JComponent findEditorComposite(JComponent editorComponent) {
        Container current = editorComponent.getParent();
        while (current instanceof JComponent component) {
            String name = component.getClass().getName();
            if (name.endsWith("EditorCompositePanel") || name.endsWith("EditorComposite")) {
                return component;
            }
            current = component.getParent();
        }
        return null;
    }

    private static JComponent firstShowing(JComponent first, JComponent second, JComponent third) {
        if (first != null && first.isShowing()) {
            return first;
        }
        if (second != null && second.isShowing()) {
            return second;
        }
        if (third != null && third.isShowing()) {
            return third;
        }
        return first != null ? first : second != null ? second : third;
    }

    private static void assertNear(String message, int expected, int actual) {
        assertTrue(
                message + " (expected=" + expected + ", actual=" + actual + ", tolerance=" + TOLERANCE + ")",
                Math.abs(expected - actual) <= TOLERANCE
        );
    }

    private static boolean screenshotShowsIcon(BufferedImage image, Rectangle iconBounds) {
        if (iconBounds.width <= 0 || iconBounds.height <= 0) {
            return false;
        }
        int sampleX = Math.min(image.getWidth() - 1, iconBounds.x + iconBounds.width / 2);
        int sampleY = Math.min(image.getHeight() - 1, iconBounds.y + iconBounds.height / 2);
        int left = image.getRGB(Math.max(0, sampleX - iconBounds.width), sampleY);
        int center = image.getRGB(sampleX, sampleY);
        return colorsDiffer(left, center);
    }

    private static boolean colorsDiffer(int left, int right) {
        return Math.abs((left & 0xFF) - (right & 0xFF)) > 8
                || Math.abs(((left >> 8) & 0xFF) - ((right >> 8) & 0xFF)) > 8
                || Math.abs(((left >> 16) & 0xFF) - ((right >> 16) & 0xFF)) > 8;
    }

    private FileEditor editorFor(VirtualFile file) {
        FileEditor[] editors = ComponentSubtabsManager.editorsFor(manager, file);
        assertTrue(editors.length > 0);
        return editors[0];
    }

    private static Path screenshotPath(String name) throws Exception {
        Path outputDir = Path.of("build", "test-output", "screenshots");
        Files.createDirectories(outputDir);
        return outputDir.resolve(name);
    }

    private record MeasuredChrome(Rectangle iconBounds, Path screenshot, BufferedImage image) {
    }

    private record DualMeasuredChrome(
            Rectangle subtabsIconBounds,
            Rectangle sidetabsIconBounds,
            Path screenshot,
            BufferedImage image
    ) {
    }
}
