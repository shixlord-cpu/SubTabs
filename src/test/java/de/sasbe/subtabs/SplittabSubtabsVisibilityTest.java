package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JToggleButton;

public class SplittabSubtabsVisibilityTest extends RealEditorWindowTestCase {
    private VirtualFile htmlFile;
    private VirtualFile specFile;
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setFamiliaEnabled(true);
        settings.setSubtabsActive(true);
        settings.setSplittabsEnabled(true);
        settings.setSplittabBehaviorMode(SplittabBehaviorMode.INTEGRATED);
        settings.setShowCollapseButton(true);
        htmlFile = createSourceFile("product-list.component.html");
        specFile = createSourceFile("product-list.component.spec.ts");
    }

    public void testSwitchBarInstallsSplittabPairHoverSync() throws Exception {
        openAndSettle(htmlFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        drainDeferredEditorEvents();

        ComponentSubtabEditorSplitRegistry.SplittabPair pair =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair();
        assertNotNull(pair);

        FileEditor leftEditor = editorFor(htmlFile);
        assertNotNull(leftEditor);
        SplittabSwitchBarPanel switchBar =
                leftEditor.getUserData(ComponentSubtabsSplittabUi.SPLITTAB_SWITCH_BAR_KEY);
        assertNotNull(switchBar);

        JToggleButton button = switchBar.pairButtonForTests(pair.id());
        assertTrue(
                "switch-bar pair tabs must wire Hover Sync for splittab pairs",
                switchBar.isPairHoverSyncInstalledForTests(button)
        );
    }

    public void testGlobalSubtabsDeactivationHidesSplittabLinkBarAndHeaderTitle() throws Exception {
        openAndSettle(htmlFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        drainDeferredEditorEvents();

        FileEditor leftEditor = editorFor(htmlFile);
        FileEditor rightEditor = editorFor(specFile);
        assertNotNull(leftEditor);
        assertNotNull(rightEditor);
        SplittabSwitchBarPanel switchBar =
                leftEditor.getUserData(ComponentSubtabsSplittabUi.SPLITTAB_SWITCH_BAR_KEY);
        assertNotNull(switchBar);
        assertTrue(switchBar.pairTabButtonCount() >= 1);

        SubtabsSettings.getInstance().setSubtabsActive(false);
        ComponentSubtabsManager.applyPresentationState(getProject());
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertNull(leftEditor.getUserData(ComponentSubtabsSplittabUi.SPLITTAB_SWITCH_BAR_KEY));
        SplittabPaneHeaderPanel header =
                rightEditor.getUserData(ComponentSubtabsSplittabUi.SPLITTAB_HEADER_KEY);
        assertNotNull(header);
        assertFalse(header.isTitleVisible());
        assertInactiveExpandIcon(leftEditor);
        assertInactiveExpandIcon(rightEditor);
    }

    public void testStackedPairCollapseKeepsExpandAndRestoreIconsOnTopPane() throws Exception {
        openAndSettle(htmlFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        drainDeferredEditorEvents();
        if (!ComponentSubtabEditorSplitNavigation.isTwoPaneStackedVertically(getProject())) {
            ComponentSubtabEditorSplitNavigation.toggleTwoPaneSplitOrientation(getProject());
            drainDeferredEditorEvents();
        }
        assertTrue(ComponentSubtabEditorSplitNavigation.isTwoPaneStackedVertically(getProject()));

        ComponentSubtabEditorSplitRegistry.SplittabPair pair =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair();
        assertNotNull(pair);

        ComponentSubtabsScopedVisibility.getInstance(getProject())
                .setSplittabPairSubtabsCollapsed(getProject(), pair.id(), true);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        FileEditor hostEditor = ComponentSubtabEditorSplitNavigation.findSplitPairIconHostEditor(getProject());
        assertNotNull(hostEditor);
        assertInactiveExpandIcon(hostEditor);
        assertTrue(
                "stacked collapsed pair must keep the split-pair icon on the top pane",
                SplittabRestoreOverlay.isVisibleOnEditor(getProject(), hostEditor)
        );

        FileEditor leftEditor = editorFor(htmlFile);
        FileEditor rightEditor = editorFor(specFile);
        assertNotNull(leftEditor);
        assertNotNull(rightEditor);
        FileEditor nonHost = hostEditor == leftEditor ? rightEditor : leftEditor;
        assertFalse(SubtabsExpandOverlay.isInstalled(nonHost));
        assertFalse(SplittabRestoreOverlay.isVisibleOnEditor(getProject(), nonHost));
    }

    public void testPairLevelCollapseAffectsRightSplittabChromeOnlyOnLeftPane() throws Exception {
        openAndSettle(htmlFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        drainDeferredEditorEvents();

        ComponentSubtabEditorSplitRegistry.SplittabPair pair =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair();
        assertNotNull(pair);

        FileEditor leftEditor = editorFor(htmlFile);
        FileEditor rightEditor = editorFor(specFile);
        assertNotNull(leftEditor);
        assertNotNull(rightEditor);

        ComponentSubtabsScopedVisibility.getInstance(getProject())
                .setSplittabPairSubtabsCollapsed(getProject(), pair.id(), true);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertNull(leftEditor.getUserData(ComponentSubtabsSplittabUi.SPLITTAB_SWITCH_BAR_KEY));
        assertFalse(SubtabsExpandOverlay.isInstalled(leftEditor));
        SplittabPaneHeaderPanel header =
                rightEditor.getUserData(ComponentSubtabsSplittabUi.SPLITTAB_HEADER_KEY);
        assertNotNull(header);
        assertFalse(header.isTitleVisible());
        assertInactiveExpandIcon(rightEditor);
    }

    public void testScopedCollapseHidesSplittabLinkBarAndHeaderTitle() throws Exception {
        openAndSettle(htmlFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        drainDeferredEditorEvents();

        FileEditor leftEditor = editorFor(htmlFile);
        FileEditor rightEditor = editorFor(specFile);
        assertNotNull(leftEditor);
        assertNotNull(rightEditor);

        ComponentSubtabsScopedVisibility.getInstance(getProject())
                .setSubtabGroupCollapsed(getProject(), htmlFile, true);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertNull(leftEditor.getUserData(ComponentSubtabsSplittabUi.SPLITTAB_SWITCH_BAR_KEY));
        SplittabPaneHeaderPanel header =
                rightEditor.getUserData(ComponentSubtabsSplittabUi.SPLITTAB_HEADER_KEY);
        assertNotNull(header);
        assertFalse(header.isTitleVisible());
        assertInactiveExpandIcon(leftEditor);
        assertInactiveExpandIcon(rightEditor);

        ComponentSubtabsScopedVisibility.getInstance(getProject())
                .setSubtabGroupCollapsed(getProject(), htmlFile, false);
        ComponentSubtabsManager.applyPresentationState(getProject());
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertNotNull(leftEditor.getUserData(ComponentSubtabsSplittabUi.SPLITTAB_SWITCH_BAR_KEY));
        header = rightEditor.getUserData(ComponentSubtabsSplittabUi.SPLITTAB_HEADER_KEY);
        assertNotNull(header);
        assertTrue(header.isTitleVisible());
    }

    private static void assertInactiveExpandIcon(FileEditor editor) {
        assertTrue(
                "collapsed splittab must show the gray expand icon",
                SubtabsExpandOverlay.isInstalled(editor)
        );
        assertSame(SubtabsIcons.INACTIVE, SubtabsExpandOverlay.expandIconOnEditor(editor));
    }

    private FileEditor editorFor(VirtualFile file) {
        FileEditor[] editors = FileEditorManager.getInstance(getProject()).getEditors(file);
        return editors.length == 0 ? null : editors[0];
    }

}
