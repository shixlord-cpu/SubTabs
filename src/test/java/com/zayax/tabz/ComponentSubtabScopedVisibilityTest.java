package com.zayax.tabz;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;

public class ComponentSubtabScopedVisibilityTest extends RealEditorWindowTestCase {
    private VirtualFile htmlFile;
    private VirtualFile specFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TabzSettings.getInstance().setTabzEnabled(true);
        TabzSettings.getInstance().setSubtabsActive(true);
        TabzSettings.getInstance().setSidetabsExpanded(true);
        htmlFile = createSourceFile("product-list.component.html");
        specFile = createSourceFile("product-list.component.spec.ts");
    }

    public void testSubtabGroupCollapseIsScopedToGroupKey() {
        ComponentSubtabScopedVisibility visibility =
                ComponentSubtabScopedVisibility.getInstance(getProject());
        assertTrue(ComponentSubtabScopedVisibility.tabzVisibleForFile(getProject(), htmlFile));
        assertTrue(ComponentSubtabScopedVisibility.tabzVisibleForFile(getProject(), specFile));

        visibility.setSubtabGroupCollapsed(getProject(), htmlFile, true);

        assertFalse(ComponentSubtabScopedVisibility.tabzVisibleForFile(getProject(), htmlFile));
        assertFalse(ComponentSubtabScopedVisibility.tabzVisibleForFile(getProject(), specFile));
    }

    public void testTabzLeftClickExpandGroupKeepsGlobalActive() throws Exception {
        openAndSettle(htmlFile);
        ComponentSubtabScopedVisibility visibility =
                ComponentSubtabScopedVisibility.getInstance(getProject());
        visibility.setSubtabGroupCollapsed(getProject(), htmlFile, true);
        assertTrue(TabzSettings.getInstance().isSubtabsActive());

        com.intellij.openapi.fileEditor.FileEditor editor = editorFor(htmlFile);
        assertNotNull(editor);
        ComponentSubtabScopedVisibility.handleTabzIconLeftClick(getProject(), editor);

        assertTrue(TabzSettings.getInstance().isSubtabsActive());
        assertTrue(ComponentSubtabScopedVisibility.tabzVisibleForFile(getProject(), htmlFile));
    }

    public void testSidetabLeftClickExpandFileKeepsGlobalExpanded() throws Exception {
        openAndSettle(htmlFile);
        ComponentSubtabScopedVisibility visibility =
                ComponentSubtabScopedVisibility.getInstance(getProject());
        visibility.setSidetabFileCollapsed(getProject(), htmlFile, true);
        assertTrue(TabzSettings.getInstance().isSidetabsExpanded());

        com.intellij.openapi.fileEditor.FileEditor editor = editorFor(htmlFile);
        assertNotNull(editor);
        ComponentSubtabScopedVisibility.handleSidetabIconLeftClick(getProject(), editor);

        assertTrue(TabzSettings.getInstance().isSidetabsExpanded());
        assertTrue(ComponentSubtabScopedVisibility.sidetabsExpandedForFile(getProject(), htmlFile));
    }

    public void testSidetabCollapseIsScopedToSingleFile() {
        ComponentSubtabScopedVisibility visibility =
                ComponentSubtabScopedVisibility.getInstance(getProject());
        assertTrue(ComponentSubtabScopedVisibility.sidetabsExpandedForFile(getProject(), htmlFile));

        visibility.setSidetabFileCollapsed(getProject(), htmlFile, true);

        assertFalse(ComponentSubtabScopedVisibility.sidetabsExpandedForFile(getProject(), htmlFile));
        assertTrue(ComponentSubtabScopedVisibility.sidetabsExpandedForFile(getProject(), specFile));
    }

    private FileEditor editorFor(VirtualFile file) {
        FileEditor[] editors = FileEditorManager.getInstance(getProject()).getEditors(file);
        return editors.length == 0 ? null : editors[0];
    }
}
