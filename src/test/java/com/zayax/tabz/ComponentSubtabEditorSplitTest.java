package com.zayax.tabz;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;

import javax.swing.SwingConstants;

import java.util.ArrayList;
import java.util.List;

public class ComponentSubtabEditorSplitTest extends RealEditorWindowTestCase {
    private VirtualFile htmlFile;
    private VirtualFile specFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TabzSettings.getInstance().setSplittabBehaviorMode(SplittabBehaviorMode.INTEGRATED);
        TabzSettings.getInstance().setTabzEnabled(true);
        htmlFile = createSourceFile("product-list.component.html");
        specFile = createSourceFile("product-list.component.spec.ts");
    }

    public void testContextMenuOffersSplittabForAnotherTabz() {
        openAndSettle(htmlFile);

        List<String> texts = menuTexts(htmlFile, specFile);

        assertTrue(
                "another tabz must offer Splittab: " + texts,
                texts.stream().anyMatch(text -> text.contains("Splittab"))
        );
    }

    public void testContextMenuHidesSplittabForTheActiveTabz() {
        openAndSettle(htmlFile);

        List<String> texts = menuTexts(htmlFile, htmlFile, true);

        assertFalse(
                "the active tabz must not offer Splittab: " + texts,
                texts.stream().anyMatch(text -> text.contains("Splittab"))
        );
    }

    public void testSplittabPlacesAnchorLeftAndTargetRight() {
        openAndSettle(htmlFile);
        EditorWindow anchorPane = manager.getCurrentWindow();
        assertNotNull(anchorPane);
        assertTrue(anchorPane.isFileOpen(htmlFile));

        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertEquals("split must create two panes", 2, manager.getWindows().length);
        assertTrue(manager.isFileOpen(htmlFile));
        assertTrue(manager.isFileOpen(specFile));

        EditorWindow htmlWindow = windowOf(htmlFile);
        EditorWindow specWindow = windowOf(specFile);
        assertNotNull(htmlWindow);
        assertNotNull(specWindow);
        assertSame("the anchor file must stay in the original pane", anchorPane, htmlWindow);
        assertNotSame(htmlWindow, specWindow);
        assertSame("the right pane must become current", specWindow, manager.getCurrentWindow());
        assertEquals("the right file must end up focused", specFile, selectedFile());
    }

    public void testLeavingRightSplittabMainTabClosesLeftSide() throws Exception {
        openAndSettle(htmlFile);
        EditorWindow leftPane = manager.getCurrentWindow();
        assertNotNull(leftPane);
        EditorWindow rightPane = leftPane.split(SwingConstants.VERTICAL, true, specFile, true);
        assertNotNull(rightPane);
        manager.setCurrentWindow(rightPane);
        rightPane.setSelectedComposite(specFile, true);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), specFile, htmlFile);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        VirtualFile tsFile = createSourceFile("product-list.component.ts");
        manager.setCurrentWindow(rightPane);
        rightPane.setSelectedComposite(specFile, true);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        openAndSettle(tsFile);
        drainDeferredEditorEvents();

        assertTrue("pair files stay open as normal tabs", manager.isFileOpen(htmlFile));
        assertTrue(manager.isFileOpen(specFile));
        assertTrue(manager.isFileOpen(tsFile));
        assertNull(ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair());
    }

    public void testCreateSplitFromRightPaneRegistersRightHalfForInitiatingFile() {
        openAndSettle(htmlFile);
        EditorWindow leftPane = manager.getCurrentWindow();
        assertNotNull(leftPane);
        EditorWindow rightPane = leftPane.split(SwingConstants.VERTICAL, true, specFile, true);
        assertNotNull(rightPane);
        manager.setCurrentWindow(rightPane);
        rightPane.setSelectedComposite(specFile, true);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), specFile, htmlFile);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        ComponentSubtabEditorSplitRegistry.SplittabPair pair =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, specFile);
        assertNotNull(pair);
        assertEquals(htmlFile, pair.leftFile());
        assertEquals(specFile, pair.rightFile());
        assertSame(leftPane, windowOf(htmlFile));
        assertSame(rightPane, windowOf(specFile));
    }

    public void testCreateSplitRegistersPairAndShowsSplittabChrome() {
        openAndSettle(htmlFile);

        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        drainDeferredEditorEvents();

        ComponentSubtabEditorSplitRegistry.SplittabPair pair =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair();
        assertNotNull(pair);
        assertEquals(htmlFile, pair.leftFile());
        assertEquals(specFile, pair.rightFile());

        FileEditor leftEditor = editorFor(htmlFile);
        FileEditor rightEditor = editorFor(specFile);
        assertNotNull(leftEditor);
        assertNotNull(rightEditor);
        assertNotNull(leftEditor.getUserData(ComponentSubtabSplittabUi.SPLITTAB_SWITCH_BAR_KEY));
        assertNull(leftEditor.getUserData(ComponentSubtabsManager.TABZ_BAR_KEY));

        SplittabPaneHeaderPanel headerPanel =
                rightEditor.getUserData(ComponentSubtabSplittabUi.SPLITTAB_HEADER_KEY);
        assertNotNull(headerPanel);
        assertTrue(
                "dissolve control must live in the Split-B header beside the title",
                headerPanel.getComponentCount() >= 2
        );
        String headerText = ComponentSubtabEditorSplitPresentation.headerText(pair);
        assertTrue("header must use group name and tabz labels: " + headerText, headerText.contains(" & "));
        assertFalse(
                "header must not use legacy Splittabs prefix: " + headerText,
                headerText.startsWith("Splittabs (")
        );
        assertEquals(
                ComponentTabTitles.displayGroupName(htmlFile)
                        + " ("
                        + ComponentTabTitles.displayTabzLabel(htmlFile)
                        + " & "
                        + ComponentTabTitles.displayTabzLabel(specFile)
                        + ")",
                headerText
        );
    }

    public void testMainTabSwitchReleasesSplittabChromeButKeepsPair() throws Exception {
        VirtualFile styleFile = createSourceFile("product-list.component.scss");
        openAndSettle(htmlFile);

        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        ComponentSubtabEditorSplitRegistry.SplittabPair pair =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair();
        assertNotNull(pair);
        assertNotNull(editorFor(htmlFile).getUserData(ComponentSubtabSplittabUi.SPLITTAB_SWITCH_BAR_KEY));
        assertTrue(manager.isFileOpen(htmlFile));
        assertTrue(manager.isFileOpen(specFile));

        openAndSettle(styleFile);
        drainDeferredEditorEvents();
        assertTrue(manager.isFileOpen(specFile));
        assertTrue(manager.isFileOpen(htmlFile));
        assertTrue(manager.isFileOpen(styleFile));
        FileEditor htmlEditor = editorFor(htmlFile);
        assertNotNull(htmlEditor);
        assertNull(htmlEditor.getUserData(ComponentSubtabSplittabUi.SPLITTAB_SWITCH_BAR_KEY));
        assertNotNull(htmlEditor.getUserData(ComponentSubtabsManager.TABZ_BAR_KEY));
        assertNotNull(
                "saved splittab pairs must stay registered",
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, specFile)
        );
        assertNull(
                "active splittab presentation must end with the main-tab switch",
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair()
        );
        assertEquals(pair.id(), ComponentSubtabEditorSplitRegistry.getInstance(getProject()).lastPresentedPairId());

        ComponentSubtabEditorSplitNavigation.restorePresentedPair(getProject(), pair);
        for (int attempt = 0; attempt < 30; attempt++) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        }

        FileEditor htmlEditorAfterRestore = editorFor(htmlFile);
        assertNotNull(htmlEditorAfterRestore);
        assertNotNull(
                "restore must show splittab chrome again",
                htmlEditorAfterRestore.getUserData(ComponentSubtabSplittabUi.SPLITTAB_SWITCH_BAR_KEY)
        );
        assertTrue(manager.isFileOpen(specFile));
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

    public void testSelectingLeftFileKeepsPartnerOpenInRightPane() {
        openAndSettle(htmlFile);

        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        manager.setCurrentWindow(windowOf(htmlFile));
        windowOf(htmlFile).setSelectedComposite(htmlFile, true);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        ComponentSubtabEditorSplitNavigation.onFileSelected(getProject(), htmlFile);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertTrue(windowOf(specFile).isFileOpen(specFile));
    }

    public void testSplittabConsolidatesOtherFilesIntoLeftPane() throws Exception {
        VirtualFile styleFile = createSourceFile("product-list.component.scss");
        openAndSettle(htmlFile);
        openAndSettle(styleFile);

        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertEquals("only the split pair of panes may remain", 2, manager.getWindows().length);
        assertTrue("relocated files must stay open", manager.isFileOpen(styleFile));
        assertSame(
                "background tabs belong in the left pane",
                windowOf(htmlFile),
                windowOf(styleFile)
        );
        assertEquals("the right pane keeps only the split target selected", 1, windowOf(specFile).getTabCount());
    }

    public void testGroupSplitMenuEntriesStayDisabled() {
        openAndSettle(htmlFile);

        List<String> texts = menuTexts(htmlFile, specFile);

        assertFalse(
                "legacy group split entries must stay hidden: " + texts,
                texts.stream().anyMatch(text -> text.contains("Im Split"))
        );
    }

    private List<String> menuTexts(VirtualFile anchorFile, VirtualFile targetFile) {
        return menuTexts(anchorFile, targetFile, false);
    }

    private FileEditor editorFor(VirtualFile file) {
        FileEditor[] editors = FileEditorManager.getInstance(getProject()).getEditors(file);
        return editors.length == 0 ? null : editors[0];
    }

    private List<String> menuTexts(VirtualFile anchorFile, VirtualFile targetFile, boolean revealOnly) {
        DefaultActionGroup group = ComponentSubtabBarPopup.buildMenu(
                getProject(), anchorFile, targetFile, revealOnly);
        List<String> texts = new ArrayList<>();
        for (AnAction action : group.getChildActionsOrStubs()) {
            String text = action.getTemplatePresentation().getText();
            if (text != null) {
                texts.add(text);
            }
        }
        return texts;
    }
}
