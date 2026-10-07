package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;

import javax.swing.SwingConstants;

/**
 * Guards the subtab switch path against accidental Splittab work.
 * <p>
 * These tests use empty fixture files on a real {@link FileEditorManagerImpl}; wall-clock time here
 * does <strong>not</strong> predict a full IDE with language services (often several seconds). We
 * assert behavioral isolation instead: Splittab presentation runs only while plugin Splittab chrome
 * is mounted, not for saved pairs or native two-pane layouts alone.
 */
public class SubtabSwitchPerformanceHardTest extends RealEditorWindowTestCase {
    private VirtualFile htmlFile;
    private VirtualFile specFile;
    private VirtualFile scssFile;
    private VirtualFile tsFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings.getInstance().setSplittabBehaviorMode(SplittabBehaviorMode.INTEGRATED);
        ComponentSubtabEditorSplitPresentation.resetInvocationCountersForTests();
        ComponentSubtabsFileEditorListener.resetDeferredRunCountersForTests();
        SubtabsSettings.getInstance().setFamiliaEnabled(true);
        SubtabsSettings.getInstance().setSubtabsActive(true);
        htmlFile = createSourceFile("product-list.component.html");
        specFile = createSourceFile("product-list.component.spec.ts");
        scssFile = createSourceFile("product-list.component.scss");
        tsFile = createSourceFile("product-list.component.ts");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            ComponentSubtabEditorSplitPresentation.resetInvocationCountersForTests();
        } finally {
            super.tearDown();
        }
    }

    public void testOpenFileAfterNativeSplitsUsesInTabSwapWithoutSplittabPresentation() {
        openAndSettle(htmlFile);

        EditorWindow leftPane = manager.getCurrentWindow();
        assertNotNull(leftPane);
        leftPane.split(SwingConstants.VERTICAL, true, htmlFile, true);
        drainDeferredEditorEvents();

        openAndSettle(specFile);
        openAndSettle(scssFile);
        drainDeferredEditorEvents();

        manager.setCurrentWindow(leftPane);
        leftPane.setSelectedComposite(htmlFile, true);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        ComponentSubtabEditorSplitPresentation.resetInvocationCountersForTests();
        assertFalse(ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(getProject()));

        manager.openFile(tsFile, true);
        drainDeferredEditorEvents();

        assertTrue(manager.isFileOpen(tsFile));
        assertSame("in-tab swap must target the anchor pane", leftPane, windowOf(tsFile));
        assertEquals("target file must exist in exactly one editor pane", 1, paneCountFor(tsFile));
        assertFalse(ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(getProject()));
        assertEquals(
                "native splits alone must not run Splittab presentation",
                0,
                ComponentSubtabEditorSplitPresentation.applySplittabPresentationInvocationCount
        );
    }

    public void testSavedPairInNativeTwoPaneLayoutDoesNotEngageSplittabUi() throws Exception {
        openAndSettle(htmlFile);

        EditorWindow leftPane = manager.getCurrentWindow();
        assertNotNull(leftPane);
        EditorWindow rightPane = leftPane.split(SwingConstants.VERTICAL, true, htmlFile, true);
        assertNotNull(rightPane);
        drainDeferredEditorEvents();

        openAndSettle(specFile);
        drainDeferredEditorEvents();

        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject());
        ComponentSubtabEditorSplitRegistry.SplittabPair pair = registry.register(htmlFile, specFile);
        assertNotNull(registry.activePair());
        assertEquals(pair.id(), registry.activePair().id());
        assertFalse(
                "saved pair + native layout is not plugin Splittab foreground",
                ComponentSubtabEditorSplitNavigation.isSplittabChromeShowing(getProject(), pair)
        );
        assertFalse(ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(getProject()));
    }

    public void testSubtabBarSwitchWithSavedPairSkipsSplittabPresentation() throws Exception {
        VirtualFile headerHtml = createSourceFile("header.component.html");
        openAndSettle(htmlFile);
        openAndSettle(headerHtml);
        drainDeferredEditorEvents();

        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject());
        registry.register(htmlFile, specFile);
        assertNotNull(registry.activePair());

        EditorWindow productPane = windowOf(htmlFile);
        assertNotNull(productPane);
        manager.setCurrentWindow(productPane);
        productPane.setSelectedComposite(htmlFile, true);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertFalse(ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(getProject()));
        ComponentSubtabEditorSplitPresentation.resetInvocationCountersForTests();

        ComponentSubtabNavigation.switchToRelatedFile(getProject(), htmlFile, tsFile);
        drainDeferredEditorEvents();

        assertTrue(manager.isFileOpen(tsFile));
        assertFalse(ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(getProject()));
        assertEquals(
                "subtab bar switch must not apply Splittab chrome without plugin Splittab UI",
                0,
                ComponentSubtabEditorSplitPresentation.applySplittabPresentationInvocationCount
        );
    }

    public void testPluginSplittabCreateSplitEngagesChromeAndAppliesPresentation() {
        openAndSettle(htmlFile);
        ComponentSubtabEditorSplitPresentation.resetInvocationCountersForTests();

        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        drainDeferredEditorEvents();

        assertTrue(ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(getProject()));
        assertTrue(
                "creating a plugin Splittab must mount presentation",
                ComponentSubtabEditorSplitPresentation.applySplittabPresentationInvocationCount > 0
        );
    }

    public void testHeaderAndProductListNativeSplitSubtabSwitchUsesSingleDeferredChromeBatch() throws Exception {
        VirtualFile headerHtml = createSourceFile("header.component.html");
        VirtualFile productHtml = createSourceFile("product-list.component.html");
        VirtualFile productTs = createSourceFile("product-list.component.ts");

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

        ComponentSubtabsFileEditorListener.resetDeferredRunCountersForTests();
        ComponentSubtabEditorSplitPresentation.resetInvocationCountersForTests();

        ComponentSubtabNavigation.switchToRelatedFile(getProject(), productHtml, productTs);
        drainDeferredEditorEvents();

        assertTrue(manager.isFileOpen(productTs));
        assertSame(productPane, windowOf(productTs));
        assertEquals(
                "subtab swap should batch chrome once",
                1,
                ComponentSubtabsFileEditorListener.deferredSwitchPresentationRunCount
        );
        assertEquals(
                "switch chrome must block duplicate selection-chrome pass",
                0,
                ComponentSubtabsFileEditorListener.deferredSelectionChromeRunCount
        );
        assertEquals(
                0,
                ComponentSubtabEditorSplitPresentation.applySplittabPresentationInvocationCount
        );
    }

    public void testCoalescedPlatformOpenSkipsDuplicateHeavyFileOpenedPass() {
        openAndSettle(htmlFile);
        ComponentSubtabsFileEditorListener.markCoalescedSubtabPlatformOpen(getProject(), tsFile);
        ComponentSubtabEditorSplitPresentation.resetInvocationCountersForTests();

        ComponentSubtabNavigation.switchInTabOf(getProject(), htmlFile, tsFile, true);
        manager.openFile(tsFile, true);
        drainDeferredEditorEvents();

        assertTrue(manager.isFileOpen(tsFile));
        assertEquals(1, paneCountFor(tsFile));
        assertEquals(
                0,
                ComponentSubtabEditorSplitPresentation.applySplittabPresentationInvocationCount
        );
    }

    private int paneCountFor(VirtualFile file) {
        int count = 0;
        for (EditorWindow window : manager.getWindows()) {
            if (window.isFileOpen(file)) {
                count++;
            }
        }
        return count;
    }
}
