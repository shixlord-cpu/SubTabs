package com.zayax.tabz;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;

import javax.swing.SwingConstants;

/**
 * Native IDE editor splits must keep working while Tabz / Splittab is enabled.
 */
public class NativeEditorSplitTest extends RealEditorWindowTestCase {
    private VirtualFile htmlFile;
    private VirtualFile specFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TabzSettings.getInstance().setTabzEnabled(true);
        htmlFile = createSourceFile("product-list.component.html");
        specFile = createSourceFile("product-list.component.spec.ts");
    }

    public void testSplitRightKeepsFileInTwoPanes() throws Exception {
        openAndSettle(htmlFile);

        var leftPane = manager.getCurrentWindow();
        assertNotNull(leftPane);
        var rightPane = leftPane.split(SwingConstants.VERTICAL, true, htmlFile, true);
        assertNotNull(rightPane);
        drainDeferredEditorEvents();

        assertEquals("native split must keep two editor panes", 2, manager.getWindows().length);
        assertEquals("split right must show the file in both panes", 2, paneCountOf(htmlFile));
    }

    public void testSplitAndMoveLayoutKeepsOneFilePerPane() throws Exception {
        openAndSettle(htmlFile);
        openAndSettle(specFile);

        var leftPane = windowOf(specFile);
        assertNotNull(leftPane);
        leftPane.closeFile(specFile);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        var rightPane = leftPane.split(SwingConstants.VERTICAL, true, specFile, true);
        assertNotNull(rightPane);
        drainDeferredEditorEvents();

        assertEquals(2, manager.getWindows().length);
        assertTrue(manager.isFileOpen(htmlFile));
        assertTrue(manager.isFileOpen(specFile));
        assertSame(leftPane, windowOf(htmlFile));
        assertSame(rightPane, windowOf(specFile));
    }
}
