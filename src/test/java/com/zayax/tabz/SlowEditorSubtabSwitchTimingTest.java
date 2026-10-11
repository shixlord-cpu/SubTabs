package com.zayax.tabz;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;

import javax.swing.SwingConstants;

/**
 * Reproduces the manual case from the sandbox IDE: header component open, product-list in a native
 * right split, then a tabz click from html to a file whose editor needs seconds to build.
 * <p>
 * The unit-test IDE loads the JavaScript plugin, so a real ts editor is ready in milliseconds there.
 * {@link SlowLoadingFileEditorProvider} restores the slow editor creation observed in the freeze dumps.
 */
public class SlowEditorSubtabSwitchTimingTest extends RealEditorWindowTestCase {
    private static final long EDITOR_BUILD_DELAY_MS = 2_500L;
    private static final long CLICK_BUDGET_MS = 1_000L;

    private VirtualFile headerHtml;
    private VirtualFile productHtml;
    private VirtualFile productTs;
    private VirtualFile productScss;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TabzSettings.getInstance().setTabzEnabled(true);
        TabzSettings.getInstance().setSubtabsActive(true);
        FileEditorProvider.EP_FILE_EDITOR_PROVIDER.getPoint().registerExtension(
                new SlowLoadingFileEditorProvider(EDITOR_BUILD_DELAY_MS),
                getTestRootDisposable()
        );
        headerHtml = createSourceFile("header.component.html");
        createSourceFile("header.component.ts");
        productHtml = createSourceFile(SlowLoadingFileEditorProvider.FILE_PREFIX + "html");
        productTs = createSourceFile(SlowLoadingFileEditorProvider.FILE_PREFIX + "ts");
        productScss = createSourceFile(SlowLoadingFileEditorProvider.FILE_PREFIX + "scss");
    }

    public void testTabzOpenOptionsDoNotAwaitTheComposite() {
        openAndSettle(productHtml);
        EditorWindow window = windowOf(productHtml);
        assertNotNull(window);
        Object options = ComponentSubtabNavigation.nonBlockingOpenOptions(window, productHtml, true);
        assertFalse(InternalPlatformBridge.waitForCompositeOpen(options));
        assertEquals(0, InternalPlatformBridge.openOptionsIndex(options));
    }

    public void testHtmlToTsClickInNativeSplitReturnsUnderOneSecond() {
        EditorWindow productPane = openHeaderAndProductListInNativeSplit();

        long clickMs = clickTabz(productHtml, productTs);

        assertTrue(
                "tabz click html -> ts blocked the EDT for " + clickMs + "ms",
                clickMs < CLICK_BUDGET_MS
        );
        assertSame("the tab must switch right away", productPane, windowOf(productTs));
        assertFalse(manager.isFileOpen(productHtml));

        long loadedAfterMs = waitUntilTabzBarAttached(productTs);
        assertTrue(
                "the simulated editor build must really take long, finished after " + loadedAfterMs + "ms",
                loadedAfterMs >= EDITOR_BUILD_DELAY_MS - 300L
        );
    }

    public void testTsToScssClickInNativeSplitReturnsUnderOneSecond() {
        EditorWindow productPane = openHeaderAndProductListInNativeSplit();
        clickTabz(productHtml, productTs);
        waitUntilTabzBarAttached(productTs);

        long clickMs = clickTabz(productTs, productScss);

        assertTrue(
                "tabz click ts -> scss blocked the EDT for " + clickMs + "ms",
                clickMs < CLICK_BUDGET_MS
        );
        assertSame(productPane, windowOf(productScss));
        assertFalse(manager.isFileOpen(productTs));
        waitUntilTabzBarAttached(productScss);
    }

    private EditorWindow openHeaderAndProductListInNativeSplit() {
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
        assertNotNull("product html must carry the tabz bar", tabzBarOf(productHtml));
        return productPane;
    }

    /** Measures only the click handler, which is what freezes the IDE when it blocks. */
    private long clickTabz(VirtualFile from, VirtualFile to) {
        long start = System.nanoTime();
        ComponentSubtabNavigation.switchToRelatedFile(getProject(), from, to);
        return (System.nanoTime() - start) / 1_000_000L;
    }

    private long waitUntilTabzBarAttached(VirtualFile file) {
        long start = System.nanoTime();
        PlatformTestUtil.waitWithEventsDispatching(
                "tabz bar was not attached to " + file.getName() + " after its editor loaded",
                () -> tabzBarOf(file) != null,
                15
        );
        return (System.nanoTime() - start) / 1_000_000L;
    }

    private ComponentSubtabBarPanel tabzBarOf(VirtualFile file) {
        for (FileEditor editor : ComponentSubtabsManager.editorsFor(manager, file)) {
            ComponentSubtabBarPanel panel = editor.getUserData(ComponentSubtabsManager.TABZ_BAR_KEY);
            if (panel != null) {
                return panel;
            }
        }
        return null;
    }
}
