package de.sasbe.subtabs;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;

import javax.swing.SwingConstants;

/**
 * Opening a non-primary subtab must stay on the fast in-tab swap path when native splits are open.
 */
public class NativeEditorSplitSubtabNavigationTest extends RealEditorWindowTestCase {
    private VirtualFile htmlFile;
    private VirtualFile specFile;
    private VirtualFile scssFile;
    private VirtualFile tsFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings.getInstance().setFamiliaEnabled(true);
        SubtabsSettings.getInstance().setSubtabsActive(true);
        htmlFile = createSourceFile("product-list.component.html");
        specFile = createSourceFile("product-list.component.spec.ts");
        scssFile = createSourceFile("product-list.component.scss");
        tsFile = createSourceFile("product-list.component.ts");
    }

    public void testResolveAnchorUsesGroupFileInBackgroundSplitPane() throws Exception {
        openAndSettle(htmlFile);

        var leftPane = manager.getCurrentWindow();
        assertNotNull(leftPane);
        var rightPane = leftPane.split(SwingConstants.VERTICAL, true, htmlFile, true);
        assertNotNull(rightPane);
        drainDeferredEditorEvents();

        openAndSettle(specFile);
        drainDeferredEditorEvents();

        manager.setCurrentWindow(leftPane);
        leftPane.setSelectedComposite(htmlFile, true);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        VirtualFile anchor = ComponentSubtabProjectViewNavigation.resolveAnchorForTarget(getProject(), scssFile);
        assertNotNull(anchor);
        assertTrue(ComponentSubtabNavigation.sameSubtabGroup(anchor, scssFile));
    }

    public void testNavigateRelatedFileUsesInTabSwapWithNativeSplits() throws Exception {
        openAndSettle(htmlFile);

        var leftPane = manager.getCurrentWindow();
        assertNotNull(leftPane);
        leftPane.split(SwingConstants.VERTICAL, true, htmlFile, true);
        drainDeferredEditorEvents();

        openAndSettle(specFile);
        drainDeferredEditorEvents();

        manager.setCurrentWindow(leftPane);
        leftPane.setSelectedComposite(htmlFile, true);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertTrue(
                ComponentSubtabProjectViewNavigation.navigateRelatedFileFromProjectView(
                        getProject(),
                        scssFile,
                        true
                )
        );
        drainDeferredEditorEvents();

        assertTrue(manager.isFileOpen(scssFile));
        assertSame(leftPane, windowOf(scssFile));
    }

    public void testFourthSubtabSwitchWithMultipleNativeSplitsDoesNotRunSplittabPresentation() throws Exception {
        openAndSettle(htmlFile);

        var leftPane = manager.getCurrentWindow();
        assertNotNull(leftPane);
        leftPane.split(SwingConstants.VERTICAL, true, htmlFile, true);
        var thirdPane = leftPane.split(SwingConstants.HORIZONTAL, true, htmlFile, true);
        assertNotNull(thirdPane);
        drainDeferredEditorEvents();

        openAndSettle(specFile);
        openAndSettle(scssFile);
        drainDeferredEditorEvents();

        manager.setCurrentWindow(leftPane);
        leftPane.setSelectedComposite(htmlFile, true);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        ComponentSubtabEditorSplitPresentation.resetInvocationCountersForTests();
        assertFalse(ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(getProject()));

        assertTrue(
                ComponentSubtabProjectViewNavigation.navigateRelatedFileFromProjectView(
                        getProject(),
                        tsFile,
                        true
                )
        );
        drainDeferredEditorEvents();

        assertTrue(manager.isFileOpen(tsFile));
        assertFalse("in-tab swap must close the anchor tab", manager.isFileOpen(htmlFile));
        assertFalse(ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(getProject()));
        assertEquals(
                0,
                ComponentSubtabEditorSplitPresentation.applySplittabPresentationInvocationCount
        );
    }
}
