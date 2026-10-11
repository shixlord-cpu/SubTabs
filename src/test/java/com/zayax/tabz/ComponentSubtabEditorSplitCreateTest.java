package com.zayax.tabz;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;

public class ComponentSubtabEditorSplitCreateTest extends RealEditorWindowTestCase {
    private VirtualFile htmlFile;
    private VirtualFile specFile;
    private VirtualFile scssFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TabzSettings.getInstance().setSplittabBehaviorMode(SplittabBehaviorMode.INTEGRATED);
        TabzSettings.getInstance().setTabzEnabled(true);
        htmlFile = createSourceFile("product-list.component.html");
        specFile = createSourceFile("product-list.component.spec.ts");
        scssFile = createSourceFile("product-list.component.scss");
    }

    public void testCreateExistingPairActivatesSavedSplittab() throws Exception {
        openAndSettle(htmlFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        var pair = ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findPairUnordered(htmlFile, specFile);
        assertNotNull(pair);

        ComponentSubtabEditorSplitMainTab.closeSplittabForeground(getProject(), pair);
        drainDeferredEditorEvents();
        assertTrue(manager.isFileOpen(htmlFile));
        assertTrue(manager.isFileOpen(specFile));

        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        drainDeferredEditorEvents();

        assertTrue(manager.isFileOpen(htmlFile));
        assertTrue(manager.isFileOpen(specFile));
        assertEquals(pair.id(), ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair().id());
        assertEquals(1, ComponentSubtabEditorSplitRegistry.getInstance(getProject()).all().size());
    }

    public void testCreateNewSplittabWhileAnotherWasActive() throws Exception {
        openAndSettle(htmlFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        var pairSpec = ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findPairUnordered(htmlFile, specFile);
        assertNotNull(pairSpec);
        assertTrue(ComponentSubtabEditorSplitNavigation.isSplittabWorkspace(getProject(), pairSpec));

        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, scssFile);
        drainDeferredEditorEvents();

        var pairScss = ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findPairUnordered(htmlFile, scssFile);
        assertNotNull(pairScss);
        assertTrue(manager.isFileOpen(htmlFile));
        assertTrue(manager.isFileOpen(scssFile));
        assertTrue(manager.isFileOpen(specFile));
        assertEquals(pairScss.id(), ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair().id());
        assertTrue(ComponentSubtabEditorSplitNavigation.isSplittabWorkspace(getProject(), pairScss));
    }

    public void testBackgroundFileStaysOnRightPaneWhenCreatingSplittab() throws Exception {
        openAndSettle(htmlFile);
        var leftPane = manager.getCurrentWindow();
        assertNotNull(leftPane);
        var rightPane = leftPane.split(javax.swing.SwingConstants.VERTICAL, true, scssFile, true);
        assertNotNull(rightPane);
        openAndSettle(scssFile);
        manager.setCurrentWindow(leftPane);
        leftPane.setSelectedComposite(htmlFile, true);
        drainDeferredEditorEvents();

        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        drainDeferredEditorEvents();
        drainDeferredEditorEvents();

        assertTrue(manager.isFileOpen(scssFile));
        assertTrue(ComponentSubtabEditorLookup.isRightSplitPane(manager, windowOf(scssFile)));
        assertFalse(ComponentSubtabEditorLookup.isRightSplitPane(manager, windowOf(htmlFile)));
        assertTrue(ComponentSubtabEditorLookup.isRightSplitPane(manager, windowOf(specFile)));
    }
}
