package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.SwingConstants;

public class SplittabRestoreOverlayTest extends RealEditorWindowTestCase {
    private VirtualFile htmlFile;
    private VirtualFile specFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings.getInstance().setSplittabBehaviorMode(SplittabBehaviorMode.INTEGRATED);
        SubtabsSettings.getInstance().setFamiliaEnabled(true);
        SubtabsSettings.getInstance().setShowCollapseButton(true);
        htmlFile = createSourceFile("product-list.component.html");
        specFile = createSourceFile("product-list.component.spec.ts");
    }

    public void testRestoreIconAppearsWhenSplittabIsSaved() throws Exception {
        VirtualFile styleFile = createSourceFile("product-list.component.scss");
        openAndSettle(htmlFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        drainDeferredEditorEvents();

        var pair = ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, specFile);
        assertNotNull(pair);
        ComponentSubtabEditorSplitMainTab.closeSplittabForeground(getProject(), pair);
        drainDeferredEditorEvents();

        openAndSettle(styleFile);
        drainDeferredEditorEvents();

        FileEditor[] editors = FileEditorManager.getInstance(getProject()).getEditors(styleFile);
        FileEditor editor = editors.length == 0 ? null : editors[0];
        assertNotNull(editor);
        assertTrue(
                "saved splittabs must offer the quick-restore icon beside collapse",
                SplittabRestoreOverlay.isVisibleOnEditor(getProject(), editor)
        );
    }

    public void testRestoreIconAppearsForNativeTwoPaneWithoutSavedPair() throws Exception {
        SubtabsSettings.getInstance().setSplittabsEnabled(true);
        openAndSettle(htmlFile);
        openAndSettle(specFile);

        var leftPane = windowOf(htmlFile);
        assertNotNull(leftPane);
        leftPane.closeFile(specFile);
        var rightPane = leftPane.split(SwingConstants.VERTICAL, true, specFile, true);
        assertNotNull(rightPane);
        drainDeferredEditorEvents();

        assertFalse(ComponentSubtabEditorSplitRegistry.getInstance(getProject()).hasSavedSplittabs());
        assertTrue(ComponentSubtabEditorSplitNavigation.hasNativeTwoPaneSplitCandidate(getProject()));

        FileEditor leftEditor = FileEditorManager.getInstance(getProject()).getEditors(htmlFile)[0];
        FileEditor rightEditor = FileEditorManager.getInstance(getProject()).getEditors(specFile)[0];
        assertFalse(
                "side-by-side split must not show the icon on the left pane",
                SplittabRestoreOverlay.isVisibleOnEditor(getProject(), leftEditor)
        );
        assertTrue(
                "side-by-side split must show the split-pair icon on the right pane",
                SplittabRestoreOverlay.isVisibleOnEditor(getProject(), rightEditor)
        );
    }

    public void testActivatePairRestoresRememberedStackedArrangement() throws Exception {
        openAndSettle(htmlFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        drainDeferredEditorEvents();

        assertTrue(ComponentSubtabEditorSplitNavigation.toggleTwoPaneSplitOrientation(getProject()));
        drainDeferredEditorEvents();
        assertTrue(ComponentSubtabEditorSplitNavigation.isTwoPaneStackedVertically(getProject()));

        var pair = ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, specFile);
        assertNotNull(pair);
        assertEquals(
                "STACKED",
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).pairTwoPaneArrangementOrDefault(pair.id())
        );

        ComponentSubtabEditorSplitMainTab.closeSplittabForeground(getProject(), pair);
        drainDeferredEditorEvents();

        ComponentSubtabEditorSplitNavigation.activatePair(getProject(), pair.id());
        drainDeferredEditorEvents();

        assertTrue(
                "reactivating a split pair must restore the last stacked layout",
                ComponentSubtabEditorSplitNavigation.isTwoPaneStackedVertically(getProject())
        );
    }

    public void testNativeTwoPaneAllowsCreateSplitPairFromOpenPanes() throws Exception {
        SubtabsSettings.getInstance().setSplittabsEnabled(true);
        openAndSettle(htmlFile);
        openAndSettle(specFile);

        var leftPane = windowOf(htmlFile);
        assertNotNull(leftPane);
        leftPane.closeFile(specFile);
        leftPane.split(SwingConstants.VERTICAL, true, specFile, true);
        drainDeferredEditorEvents();

        VirtualFile partner = ComponentSubtabEditorSplitNavigation.nativeTwoPanePartner(getProject(), htmlFile);
        assertNotNull(partner);
        assertEquals(specFile, partner);
        assertTrue(ComponentSubtabEditorSplitNavigation.canCreateSplitPairFromNativeTwoPane(
                getProject(),
                htmlFile,
                partner
        ));
    }

    public void testRightClickToggleSwapsSideBySideToStacked() throws Exception {
        SubtabsSettings.getInstance().setSplittabsEnabled(true);
        openAndSettle(htmlFile);
        openAndSettle(specFile);

        var leftPane = windowOf(htmlFile);
        assertNotNull(leftPane);
        leftPane.closeFile(specFile);
        leftPane.split(SwingConstants.VERTICAL, true, specFile, true);
        drainDeferredEditorEvents();

        assertFalse(ComponentSubtabEditorSplitNavigation.isTwoPaneStackedVertically(getProject()));
        assertTrue(ComponentSubtabEditorSplitNavigation.canToggleTwoPaneSplitOrientation(getProject()));
        assertTrue(ComponentSubtabEditorSplitNavigation.toggleTwoPaneSplitOrientation(getProject()));
        drainDeferredEditorEvents();

        assertTrue(ComponentSubtabEditorSplitNavigation.isTwoPaneStackedVertically(getProject()));
        assertTrue(manager.isFileOpen(htmlFile));
        assertTrue(manager.isFileOpen(specFile));

        FileEditor topEditor = topPaneEditor();
        FileEditor bottomEditor = bottomPaneEditor();
        assertNotNull(topEditor);
        assertNotNull(bottomEditor);
        assertTrue(SplittabRestoreOverlay.isVisibleOnEditor(getProject(), topEditor));
        assertFalse(SplittabRestoreOverlay.isVisibleOnEditor(getProject(), bottomEditor));
    }

    private FileEditor topPaneEditor() {
        return editorInTopPane(htmlFile, specFile);
    }

    private FileEditor bottomPaneEditor() {
        VirtualFile topFile = windowOf(htmlFile) == topEditorWindow() ? htmlFile : specFile;
        VirtualFile bottomFile = topFile.equals(htmlFile) ? specFile : htmlFile;
        return FileEditorManager.getInstance(getProject()).getEditors(bottomFile)[0];
    }

    private com.intellij.openapi.fileEditor.impl.EditorWindow topEditorWindow() {
        var windows = manager.getWindows();
        if (windows.length != 2) {
            return windows[0];
        }
        var c0 = windows[0].getTabbedPane().getComponent();
        var c1 = windows[1].getTabbedPane().getComponent();
        if (c0.isShowing() && c1.isShowing()) {
            return c0.getLocationOnScreen().y <= c1.getLocationOnScreen().y ? windows[0] : windows[1];
        }
        return windows[0];
    }

    private FileEditor editorInTopPane(VirtualFile first, VirtualFile second) {
        com.intellij.openapi.fileEditor.impl.EditorWindow top = topEditorWindow();
        VirtualFile topFile = windowOf(first) == top ? first : second;
        return FileEditorManager.getInstance(getProject()).getEditors(topFile)[0];
    }
}
