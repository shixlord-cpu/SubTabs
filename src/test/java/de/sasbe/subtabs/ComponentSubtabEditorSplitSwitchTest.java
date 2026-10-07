package de.sasbe.subtabs;

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
        SubtabsSettings.getInstance().setFamiliaEnabled(true);
        SubtabsSettings.getInstance().setSplittabsEnabled(true);
        SubtabsSettings.getInstance().setSplittabBehaviorMode(SplittabBehaviorMode.INTEGRATED);
        htmlFile = createSourceFile("product-list.component.html");
        specFile = createSourceFile("product-list.component.spec.ts");
        scssFile = createSourceFile("product-list.component.scss");
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

    private boolean hasSwitchBar(VirtualFile file) {
        var editors = ComponentSubtabsManager.editorsFor(manager, file);
        return editors.length > 0
                && editors[0].getUserData(ComponentSubtabsSplittabUi.SPLITTAB_SWITCH_BAR_KEY) != null;
    }

    private boolean hasHeader(VirtualFile file) {
        var editors = ComponentSubtabsManager.editorsFor(manager, file);
        return editors.length > 0
                && editors[0].getUserData(ComponentSubtabsSplittabUi.SPLITTAB_HEADER_KEY) != null;
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

        var closed = ComponentSubtabEditorSplitFamiliaMenu.closedPairsForContext(getProject(), htmlFile, null);
        assertEquals(2, closed.size());
    }
}
