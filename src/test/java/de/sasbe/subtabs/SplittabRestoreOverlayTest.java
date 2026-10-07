package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;

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
}
