package com.zayax.tabz;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;

import javax.swing.SwingConstants;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Uses the real demo-project component sources (large TS/HTML) and the same layout as manual testing:
 * header component open, product-list in a native right split, then switch off the default html tabz.
 */
public class DemoProjectSubtabSwitchTimingTest extends RealEditorWindowTestCase {
    private static final long TABZ_SWITCH_BUDGET_MS = 1_000L;

    private VirtualFile headerHtml;
    private VirtualFile headerTs;
    private VirtualFile productHtml;
    private VirtualFile productTs;
    private VirtualFile productScss;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TabzSettings.getInstance().setTabzEnabled(true);
        TabzSettings.getInstance().setSubtabsActive(true);
        headerHtml = copyDemoComponent("header.component.html");
        headerTs = copyDemoComponent("header.component.ts");
        productHtml = copyDemoComponent("product-list.component.html");
        productTs = copyDemoComponent("product-list.component.ts");
        productScss = copyDemoComponent("product-list.component.scss");
    }

    public void testHeaderAndProductListSplitSwitchToTypeScriptUnderOneSecond() throws Exception {
        openAndSettle(headerHtml);
        EditorWindow headerPane = manager.getCurrentWindow();
        assertNotNull(headerPane);
        headerPane.split(SwingConstants.VERTICAL, true, headerHtml, true);
        drainDeferredEditorEvents();

        openAndSettle(productHtml);
        drainDeferredEditorEvents();

        EditorWindow productPane = windowOf(productHtml);
        assertNotNull(productPane);
        manager.setCurrentWindow(productPane);
        productPane.setSelectedComposite(productHtml, true);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        long elapsedMs = switchTabzAndDrain(productHtml, productTs);
        assertTrue(manager.isFileOpen(productTs));
        assertSame(productPane, windowOf(productTs));
        assertFalse(manager.isFileOpen(productHtml));
        assertTrue(
                "product-list html -> ts with demo sources took " + elapsedMs + "ms",
                elapsedMs < TABZ_SWITCH_BUDGET_MS
        );
    }

    public void testSwitchBackToHtmlTabzIsAlsoUnderOneSecond() throws Exception {
        openAndSettle(headerHtml);
        EditorWindow headerPane = manager.getCurrentWindow();
        assertNotNull(headerPane);
        headerPane.split(SwingConstants.VERTICAL, true, headerHtml, true);
        drainDeferredEditorEvents();
        openAndSettle(productHtml);
        drainDeferredEditorEvents();

        EditorWindow productPane = windowOf(productHtml);
        assertNotNull(productPane);
        manager.setCurrentWindow(productPane);
        switchTabzAndDrain(productHtml, productTs);

        long elapsedMs = switchTabzAndDrain(productTs, productHtml);
        assertTrue(manager.isFileOpen(productHtml));
        assertSame(productPane, windowOf(productHtml));
        assertTrue(
                "product-list ts -> html took " + elapsedMs + "ms",
                elapsedMs < TABZ_SWITCH_BUDGET_MS
        );
    }

    public void testHtmlTabzClickIsImmediateNoOp() throws Exception {
        openAndSettle(productHtml);
        long start = System.nanoTime();
        ComponentSubtabNavigation.switchToRelatedFile(getProject(), productHtml, productHtml);
        drainDeferredEditorEvents();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        assertTrue("html -> html should be instant, took " + elapsedMs + "ms", elapsedMs < 50L);
    }

    public void testHeaderPaneSwitchToScssUnderOneSecond() throws Exception {
        VirtualFile headerScss = copyDemoComponent("header.component.scss");
        openAndSettle(headerHtml);
        long elapsedMs = switchTabzAndDrain(headerHtml, headerScss);
        assertTrue(manager.isFileOpen(headerScss));
        assertTrue(
                "header html -> scss took " + elapsedMs + "ms",
                elapsedMs < TABZ_SWITCH_BUDGET_MS
        );
    }

    private long switchTabzAndDrain(VirtualFile anchor, VirtualFile target) {
        long startNanos = System.nanoTime();
        ComponentSubtabNavigation.switchToRelatedFile(getProject(), anchor, target);
        drainDeferredEditorEvents();
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private VirtualFile copyDemoComponent(String fileName) throws Exception {
        Path demoPath = Path.of("demo-project/src/app").resolve(fileName);
        String text = Files.readString(demoPath, StandardCharsets.UTF_8);
        VirtualFile file = createSourceFile(fileName);
        WriteAction.run(() -> file.setBinaryContent(text.getBytes(StandardCharsets.UTF_8)));
        return file;
    }
}
