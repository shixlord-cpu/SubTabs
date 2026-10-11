package com.zayax.tabz;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;

public class SplittabSubtabDragDropTest extends RealEditorWindowTestCase {
    private VirtualFile htmlFile;
    private VirtualFile specFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TabzSettings.getInstance().setTabzEnabled(true);
        TabzSettings.getInstance().setSubtabsActive(true);
        TabzSettings.getInstance().setShowCollapseButton(true);
        htmlFile = createSourceFile("product-list.component.html");
        specFile = createSourceFile("product-list.component.spec.ts");
    }

    public void testDragDropShowsSplittabIconWithoutSavedPair() throws Exception {
        openAndSettle(htmlFile);
        drainDeferredEditorEvents();

        FileEditor editor = editorFor(htmlFile);
        assertNotNull(editor);
        assertFalse(ComponentSubtabEditorSplitRegistry.getInstance(getProject()).hasSavedSplittabs());

        ComponentSubtabBarPanel bar = editor.getUserData(ComponentSubtabsManager.TABZ_BAR_KEY);
        assertNotNull(bar);

        SplittabSubtabDragDrop.begin(getProject(), bar, specFile);
        assertTrue(SplittabRestoreOverlay.isVisibleOnEditor(getProject(), editor));

        SplittabSubtabDragDrop.end();
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        assertFalse(SplittabRestoreOverlay.isVisibleOnEditor(getProject(), editor));
    }

    public void testDropOnTargetCreatesSplittabPair() throws Exception {
        openAndSettle(htmlFile);
        drainDeferredEditorEvents();

        FileEditor editor = editorFor(htmlFile);
        assertNotNull(editor);
        ComponentSubtabBarPanel bar = editor.getUserData(ComponentSubtabsManager.TABZ_BAR_KEY);
        assertNotNull(bar);

        SplittabSubtabDragDrop.begin(getProject(), bar, specFile);
        assertTrue(SplittabSubtabDragDrop.tryCompleteActiveDropForTest(editor));

        assertNotNull(
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, specFile)
        );
        assertTrue(manager.isFileOpen(htmlFile));
        assertTrue(manager.isFileOpen(specFile));
    }

    private FileEditor editorFor(VirtualFile file) {
        FileEditor[] editors = FileEditorManager.getInstance(getProject()).getEditors(file);
        return editors.length == 0 ? null : editors[0];
    }
}
