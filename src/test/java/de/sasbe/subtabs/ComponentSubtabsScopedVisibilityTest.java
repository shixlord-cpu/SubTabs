package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;

public class ComponentSubtabsScopedVisibilityTest extends RealEditorWindowTestCase {
    private VirtualFile htmlFile;
    private VirtualFile specFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings.getInstance().setFamiliaEnabled(true);
        SubtabsSettings.getInstance().setSubtabsActive(true);
        SubtabsSettings.getInstance().setSidetabsExpanded(true);
        htmlFile = createSourceFile("product-list.component.html");
        specFile = createSourceFile("product-list.component.spec.ts");
    }

    public void testSubtabGroupCollapseIsScopedToGroupKey() {
        ComponentSubtabsScopedVisibility visibility =
                ComponentSubtabsScopedVisibility.getInstance(getProject());
        assertTrue(ComponentSubtabsScopedVisibility.subtabsVisibleForFile(getProject(), htmlFile));
        assertTrue(ComponentSubtabsScopedVisibility.subtabsVisibleForFile(getProject(), specFile));

        visibility.setSubtabGroupCollapsed(getProject(), htmlFile, true);

        assertFalse(ComponentSubtabsScopedVisibility.subtabsVisibleForFile(getProject(), htmlFile));
        assertFalse(ComponentSubtabsScopedVisibility.subtabsVisibleForFile(getProject(), specFile));
    }

    public void testSubtabLeftClickExpandGroupKeepsGlobalActive() throws Exception {
        openAndSettle(htmlFile);
        ComponentSubtabsScopedVisibility visibility =
                ComponentSubtabsScopedVisibility.getInstance(getProject());
        visibility.setSubtabGroupCollapsed(getProject(), htmlFile, true);
        assertTrue(SubtabsSettings.getInstance().isSubtabsActive());

        com.intellij.openapi.fileEditor.FileEditor editor = editorFor(htmlFile);
        assertNotNull(editor);
        ComponentSubtabsScopedVisibility.handleSubtabIconLeftClick(getProject(), editor);

        assertTrue(SubtabsSettings.getInstance().isSubtabsActive());
        assertTrue(ComponentSubtabsScopedVisibility.subtabsVisibleForFile(getProject(), htmlFile));
    }

    public void testSidetabLeftClickExpandFileKeepsGlobalExpanded() throws Exception {
        openAndSettle(htmlFile);
        ComponentSubtabsScopedVisibility visibility =
                ComponentSubtabsScopedVisibility.getInstance(getProject());
        visibility.setSidetabFileCollapsed(getProject(), htmlFile, true);
        assertTrue(SubtabsSettings.getInstance().isSidetabsExpanded());

        com.intellij.openapi.fileEditor.FileEditor editor = editorFor(htmlFile);
        assertNotNull(editor);
        ComponentSubtabsScopedVisibility.handleSidetabIconLeftClick(getProject(), editor);

        assertTrue(SubtabsSettings.getInstance().isSidetabsExpanded());
        assertTrue(ComponentSubtabsScopedVisibility.sidetabsExpandedForFile(getProject(), htmlFile));
    }

    public void testSidetabCollapseIsScopedToSingleFile() {
        ComponentSubtabsScopedVisibility visibility =
                ComponentSubtabsScopedVisibility.getInstance(getProject());
        assertTrue(ComponentSubtabsScopedVisibility.sidetabsExpandedForFile(getProject(), htmlFile));

        visibility.setSidetabFileCollapsed(getProject(), htmlFile, true);

        assertFalse(ComponentSubtabsScopedVisibility.sidetabsExpandedForFile(getProject(), htmlFile));
        assertTrue(ComponentSubtabsScopedVisibility.sidetabsExpandedForFile(getProject(), specFile));
    }

    private FileEditor editorFor(VirtualFile file) {
        FileEditor[] editors = FileEditorManager.getInstance(getProject()).getEditors(file);
        return editors.length == 0 ? null : editors[0];
    }
}
