package com.zayax.tabz;

import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;

public class ComponentSubtabEditorSplitSwitchTest extends RealEditorWindowTestCase {
    private VirtualFile htmlFile;
    private VirtualFile specFile;
    private VirtualFile scssFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TabzSettings.getInstance().setTabzEnabled(true);
        TabzSettings.getInstance().setSplittabsEnabled(true);
        TabzSettings.getInstance().setSplittabBehaviorMode(SplittabBehaviorMode.INTEGRATED);
        htmlFile = createSourceFile("product-list.component.html");
        specFile = createSourceFile("product-list.component.spec.ts");
        scssFile = createSourceFile("product-list.component.scss");
    }

    public void testSwitchBarReorderDragShowsGapLikeTabzBar() throws Exception {
        openAndSettle(htmlFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        ComponentSubtabEditorSplitRegistry.SplittabPair pairSpec =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, specFile);
        assertNotNull(pairSpec);
        ComponentSubtabEditorSplitMainTab.closeSplittabForeground(getProject(), pairSpec);
        drainDeferredEditorEvents();

        openAndSettle(htmlFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, scssFile);
        drainDeferredEditorEvents();

        SplittabSwitchBarPanel switchBar = switchBarOn(htmlFile);
        assertNotNull(switchBar);
        assertTrue(switchBar.pairTabButtonCount() >= 2);

        switchBar.updateReorderDragState(1, scssFile, new java.awt.Point(120, 8));
        switchBar.layOutTabsForTests(900);
        assertTrue(
                "switch-bar reorder drag must reserve a visible drop gap like the tabz bar",
                switchBar.reorderGapWidthForTests() > 0
        );
        switchBar.clearReorderPreview();
    }

    public void testSwitchBarActivatesOtherSavedSplittab() throws Exception {
        openAndSettle(htmlFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        ComponentSubtabEditorSplitRegistry.SplittabPair pairSpec =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, specFile);
        assertNotNull(pairSpec);

        ComponentSubtabEditorSplitMainTab.closeSplittabForeground(getProject(), pairSpec);
        drainDeferredEditorEvents();

        openAndSettle(htmlFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, scssFile);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        ComponentSubtabEditorSplitRegistry.SplittabPair pairScss =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, scssFile);
        assertNotNull(pairScss);
        assertTrue(manager.isFileOpen(htmlFile));
        assertTrue(manager.isFileOpen(scssFile));
        assertTrue(manager.isFileOpen(specFile));

        ComponentSubtabEditorSplitNavigation.activatePair(getProject(), pairSpec.id());
        drainDeferredEditorEvents();
        drainDeferredEditorEvents();

        assertNotNull(ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair());
        assertTrue(manager.isFileOpen(htmlFile));
        assertTrue(manager.isFileOpen(specFile));
        assertTrue(manager.isFileOpen(scssFile));
        assertEquals(pairSpec.id(), ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair().id());
        assertTrue(ComponentSubtabEditorSplitNavigation.isSplittabWorkspace(getProject(), pairSpec));
    }

    public void testStaleSelectionDuringPairActivationDoesNotSwitchPair() throws Exception {
        openAndSettle(htmlFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        ComponentSubtabEditorSplitRegistry.SplittabPair pairSpec =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, specFile);
        assertNotNull(pairSpec);

        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, scssFile);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        ComponentSubtabEditorSplitRegistry.SplittabPair pairScss =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, scssFile);
        assertNotNull(pairScss);
        assertTrue(manager.isFileOpen(specFile));
        assertTrue(manager.isFileOpen(scssFile));

        ComponentSubtabEditorSplitNavigation.activatePair(getProject(), pairScss.id());
        assertEquals(
                pairScss.id(),
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair().id()
        );
        ComponentSubtabEditorSplitPresentation.handleMainTabSelection(
                getProject(),
                specFile,
                scssFile
        );
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        assertEquals(
                "background pair tab selected during activation must not hijack the switch",
                pairScss.id(),
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair().id()
        );
    }

    public void testActivatePairKeepsLeftFileOnLeftPane() throws Exception {
        openAndSettle(htmlFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, scssFile);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        ComponentSubtabEditorSplitRegistry.SplittabPair pairScss =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, scssFile);
        assertNotNull(pairScss);

        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        ComponentSubtabEditorSplitRegistry.SplittabPair pairSpec =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, specFile);
        assertNotNull(pairSpec);

        FileEditorManagerEx managerEx = FileEditorManagerEx.getInstanceEx(getProject());
        EditorWindow leftPane = manager.getWindows()[0];
        EditorWindow rightPane = manager.getWindows()[1];
        if (leftPane.isFileOpen(htmlFile)) {
            leftPane.closeFile(htmlFile);
        }
        if (rightPane.isFileOpen(scssFile)) {
            rightPane.closeFile(scssFile);
        }
        manager.openFile(
                htmlFile,
                rightPane,
                ComponentSubtabNavigation.nonBlockingOpenOptions(false, false)
        );
        manager.openFile(
                scssFile,
                leftPane,
                ComponentSubtabNavigation.nonBlockingOpenOptions(false, false)
        );
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        ComponentSubtabEditorSplitNavigation.activatePair(getProject(), pairScss.id());
        drainDeferredEditorEvents();
        drainDeferredEditorEvents();

        EditorWindow htmlWindow = ComponentSubtabEditorLookup.findWindowWithFile(managerEx, htmlFile);
        EditorWindow scssWindow = ComponentSubtabEditorLookup.findWindowWithFile(managerEx, scssFile);
        assertNotNull(htmlWindow);
        assertNotNull(scssWindow);
        assertFalse(ComponentSubtabEditorLookup.isRightSplitPane(managerEx, htmlWindow));
        assertTrue(ComponentSubtabEditorLookup.isRightSplitPane(managerEx, scssWindow));
        assertTrue(hasSwitchBar(htmlFile));
        assertTrue(hasHeader(scssFile));
    }

    private @org.jetbrains.annotations.Nullable SplittabSwitchBarPanel switchBarOn(VirtualFile leftFile) {
        var editors = ComponentSubtabsManager.editorsFor(manager, leftFile);
        if (editors.length == 0) {
            return null;
        }
        return editors[0].getUserData(ComponentSubtabSplittabUi.SPLITTAB_SWITCH_BAR_KEY);
    }

    private boolean hasSwitchBar(VirtualFile file) {
        return switchBarOn(file) != null;
    }

    private boolean hasHeader(VirtualFile file) {
        var editors = ComponentSubtabsManager.editorsFor(manager, file);
        return editors.length > 0
                && editors[0].getUserData(ComponentSubtabSplittabUi.SPLITTAB_HEADER_KEY) != null;
    }

    public void testContextMenuListsAllClosedSplittabsInGroup() throws Exception {
        openAndSettle(htmlFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        ComponentSubtabEditorSplitRegistry.SplittabPair pairSpec =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, specFile);
        assertNotNull(pairSpec);
        ComponentSubtabEditorSplitMainTab.closeSplittabForeground(getProject(), pairSpec);
        drainDeferredEditorEvents();

        openAndSettle(htmlFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, scssFile);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        ComponentSubtabEditorSplitRegistry.SplittabPair pairScss =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, scssFile);
        assertNotNull(pairScss);
        ComponentSubtabEditorSplitMainTab.closeSplittabForeground(getProject(), pairScss);
        drainDeferredEditorEvents();

        var closed = ComponentSubtabEditorSplitTabzMenu.closedPairsForContext(getProject(), htmlFile, null);
        assertEquals(2, closed.size());
    }
}
