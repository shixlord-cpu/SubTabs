package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;

/**
 * Haupttab-Verhalten für Splittabs (Split-A/B, Schließen, Wiederherstellen).
 */
public class ComponentSubtabEditorSplitMainTabTest extends RealEditorWindowTestCase {
    private VirtualFile htmlFile;
    private VirtualFile specFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings.getInstance().setSplittabBehaviorMode(SplittabBehaviorMode.INTEGRATED);
        SubtabsSettings.getInstance().setFamiliaEnabled(true);
        SubtabsSettings.getInstance().setSubtabsActive(true);
        htmlFile = createSourceFile("product-list.component.html");
        specFile = createSourceFile("product-list.component.spec.ts");
    }

    public void testForegroundShowsSplittabAOnLeftAndSplittabBOnRight() {
        createActiveSplittabPair();

        ComponentEditorTabTitleProvider titles = new ComponentEditorTabTitleProvider();
        assertEquals(
                ComponentSubtabEditorSplitPresentation.splittabMainTabTitle(
                        ComponentSubtabEditorSplitPresentation.SPLITTAB_A_MAIN_TAB_TITLE,
                        htmlFile
                ),
                titles.getEditorTabTitle(getProject(), htmlFile)
        );
        assertEquals(
                ComponentSubtabEditorSplitPresentation.splittabMainTabTitle(
                        ComponentSubtabEditorSplitPresentation.SPLITTAB_B_MAIN_TAB_TITLE,
                        specFile
                ),
                titles.getEditorTabTitle(getProject(), specFile)
        );
    }

    public void testSwitchBetweenSplittabMainTabsKeepsBothOpen() {
        createActiveSplittabPair();
        assertTrue(manager.isFileOpen(htmlFile));
        assertTrue(manager.isFileOpen(specFile));

        ComponentSubtabEditorSplitPresentation.handleMainTabSelection(getProject(), specFile, htmlFile);
        drainCloseEvents();

        assertTrue(manager.isFileOpen(htmlFile));
        assertTrue(manager.isFileOpen(specFile));
        assertNotNull(ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair());
    }

    public void testLeaveSplittabMainTabKeepsPairFilesAsNormalTabs() throws Exception {
        createActiveSplittabPair();
        VirtualFile styleFile = createSourceFile("product-list.component.scss");
        openAndSettle(styleFile);
        drainCloseEvents();

        assertTrue(manager.isFileOpen(htmlFile));
        assertTrue(manager.isFileOpen(specFile));
        assertTrue(manager.isFileOpen(styleFile));
        assertNull(ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair());
        FileEditor htmlEditor = editorFor(htmlFile);
        assertNotNull(htmlEditor);
        assertNull(htmlEditor.getUserData(ComponentSubtabsSplittabUi.SPLITTAB_SWITCH_BAR_KEY));
        assertNotNull(htmlEditor.getUserData(ComponentSubtabsManager.SUBTAB_BAR_KEY));
    }

    public void testRestorePresentedPairReopensSplittabWithChrome() throws Exception {
        createActiveSplittabPair();
        ComponentSubtabEditorSplitRegistry.SplittabPair pair =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, specFile);
        assertNotNull(pair);

        VirtualFile styleFile = createSourceFile("product-list.component.scss");
        openAndSettle(styleFile);
        drainCloseEvents();

        ComponentSubtabEditorSplitRegistry.getInstance(getProject()).setActive(pair.id());
        ComponentSubtabEditorSplitNavigation.restorePresentedPair(getProject(), pair);
        drainCloseEvents();

        assertTrue(manager.isFileOpen(htmlFile));
        assertTrue(manager.isFileOpen(specFile));
        FileEditor htmlEditor = editorFor(htmlFile);
        assertNotNull(htmlEditor);
        assertNotNull(htmlEditor.getUserData(ComponentSubtabsSplittabUi.SPLITTAB_SWITCH_BAR_KEY));
        assertEquals(
                ComponentSubtabEditorSplitPresentation.splittabMainTabTitle(
                        ComponentSubtabEditorSplitPresentation.SPLITTAB_A_MAIN_TAB_TITLE,
                        htmlFile
                ),
                new ComponentEditorTabTitleProvider().getEditorTabTitle(getProject(), htmlFile)
        );
        assertEquals(
                ComponentSubtabEditorSplitPresentation.splittabMainTabTitle(
                        ComponentSubtabEditorSplitPresentation.SPLITTAB_B_MAIN_TAB_TITLE,
                        specFile
                ),
                new ComponentEditorTabTitleProvider().getEditorTabTitle(getProject(), specFile)
        );
    }

    public void testLeavingSplittabKeepsRegistryEntry() throws Exception {
        createActiveSplittabPair();
        ComponentSubtabEditorSplitRegistry.SplittabPair pair =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, specFile);
        assertNotNull(pair);

        VirtualFile styleFile = createSourceFile("product-list.component.scss");
        openAndSettle(styleFile);
        drainCloseEvents();

        assertNotNull(
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, specFile)
        );
        assertEquals(pair.id(), ComponentSubtabEditorSplitRegistry.getInstance(getProject()).lastPresentedPairId());
    }

    public void testClosingBothSplittabSidesKeepsRegistryEntry() throws Exception {
        createActiveSplittabPair();
        ComponentSubtabEditorSplitRegistry.SplittabPair pair =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, specFile);
        assertNotNull(pair);

        manager.closeFile(htmlFile);
        manager.closeFile(specFile);
        drainCloseEvents();

        assertFalse(manager.isFileOpen(htmlFile));
        assertFalse(manager.isFileOpen(specFile));
        assertNull(ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair());
        assertNotNull(
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, specFile)
        );
        assertEquals(pair.id(), ComponentSubtabEditorSplitRegistry.getInstance(getProject()).lastPresentedPairId());
    }

    public void testClosingOneSplittabSideKeepsOtherAsNormalTab() throws Exception {
        createActiveSplittabPair();
        manager.closeFile(specFile);
        drainCloseEvents();
        drainCloseEvents();

        assertFalse(manager.isFileOpen(specFile));
        assertTrue("surviving pair side must stay open", manager.isFileOpen(htmlFile));
        assertNull(ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair());
        ComponentSubtabsManager.attachIfNeeded(getProject(), htmlFile);
        drainCloseEvents();
        FileEditor htmlEditor = editorFor(htmlFile);
        assertNotNull(htmlEditor);
        assertNull(htmlEditor.getUserData(ComponentSubtabsSplittabUi.SPLITTAB_SWITCH_BAR_KEY));
        assertNotNull(htmlEditor.getUserData(ComponentSubtabsManager.SUBTAB_BAR_KEY));
        assertNotNull(
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, specFile)
        );
    }

    public void testFamiliaMenuListsClosedSplittabForGroupFile() throws Exception {
        createActiveSplittabPair();
        ComponentSubtabEditorSplitRegistry.SplittabPair pair =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair();
        assertNotNull(pair);

        ComponentSubtabEditorSplitMainTab.closeSplittabForeground(getProject(), pair);
        drainCloseEvents();

        assertFalse(
                ComponentSubtabEditorSplitFamiliaMenu.closedPairsForContext(getProject(), htmlFile, null).isEmpty()
        );
    }

    private void createActiveSplittabPair() {
        openAndSettle(htmlFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        assertNotNull(ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair());
    }

    private void drainCloseEvents() {
        for (int attempt = 0; attempt < 30; attempt++) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        }
    }

    private FileEditor editorFor(VirtualFile file) {
        FileEditor[] editors = FileEditorManager.getInstance(getProject()).getEditors(file);
        return editors.length == 0 ? null : editors[0];
    }
}
