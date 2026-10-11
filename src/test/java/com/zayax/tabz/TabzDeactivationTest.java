package com.zayax.tabz;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;

import java.nio.charset.StandardCharsets;

public class TabzDeactivationTest extends RealEditorWindowTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TabzSettings settings = TabzSettings.getInstance();
        settings.setTabzEnabled(true);
        settings.setSubtabsActive(true);
        settings.setSidetabsActive(true);
        settings.setSidetabsExpanded(true);
        settings.setShowCollapseButton(true);
        settings.setProjectViewGroupingEnabled(true);
        settings.setProjectViewGroupingActive(true);
        settings.setSidetabRules(SidetabRulesDefaults.createDefaults());
        settings.setSplittabsEnabled(true);
        ComponentSubtabDocumentListener.install(getProject());
    }

    public void testDisablingTabzReleasesSplittabChrome() throws Exception {
        VirtualFile html = createSourceFile("product-list.component.html");
        VirtualFile spec = createSourceFile("product-list.component.spec.ts");
        openAndSettle(html);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), html, spec);
        drainDeferredEditorEvents();

        FileEditor leftEditor = editorFor(html);
        FileEditor rightEditor = editorFor(spec);
        assertNotNull(leftEditor);
        assertNotNull(rightEditor);
        assertNotNull(leftEditor.getUserData(ComponentSubtabSplittabUi.SPLITTAB_SWITCH_BAR_KEY));
        assertNotNull(rightEditor.getUserData(ComponentSubtabSplittabUi.SPLITTAB_HEADER_KEY));
        assertTrue(ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(getProject()));

        TabzSettings.getInstance().setTabzEnabled(false);
        TabzPresentation.applySettingsChange();
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertNull(leftEditor.getUserData(ComponentSubtabSplittabUi.SPLITTAB_SWITCH_BAR_KEY));
        assertNull(rightEditor.getUserData(ComponentSubtabSplittabUi.SPLITTAB_HEADER_KEY));
        assertFalse(ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(getProject()));
        assertFalse(ComponentSubtabEditorSplitPresentation.isSplittabsMainTabFile(getProject(), html));
        assertFalse(ComponentSubtabEditorSplitPresentation.isSplittabsMainTabFile(getProject(), spec));
        assertTrue(manager.isFileOpen(html));
        assertTrue(manager.isFileOpen(spec));
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject());
        assertTrue(registry.hasSavedSplittabs());
        assertNotNull(registry.findPairUnordered(html, spec));
        assertNull(registry.activePair());

        TabzSettings.getInstance().setTabzEnabled(true);
        TabzPresentation.applySettingsChange();
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertTrue(registry.hasSavedSplittabs());
        assertNotNull(registry.findPairUnordered(html, spec));
        ComponentSubtabEditorSplitNavigation.activatePair(getProject(), registry.findPairUnordered(html, spec).id());
        drainDeferredEditorEvents();
        assertTrue(ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(getProject()));
    }

    public void testDisablingTabzRemovesAllAttachedUi() throws Exception {
        VirtualFile html = createSourceFile("header.component.html");
        WriteAction.run(() -> html.setBinaryContent("""
                <header>Title</header>
                <main>Content</main>
                """.getBytes(StandardCharsets.UTF_8)));
        createSourceFile("header.component.ts");
        openAndSettle(html);
        ComponentSubtabsManager.attachIfNeeded(getProject(), html);
        SidetabsManager.attachIfNeeded(getProject(), html);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        FileEditor editor = FileEditorManager.getInstance(getProject()).getSelectedEditor();
        assertNotNull(editor);
        assertNotNull(editor.getUserData(ComponentSubtabsManager.TABZ_BAR_KEY));
        assertNotNull(editor.getUserData(SidetabsManager.SIDETAB_BAR_KEY));
        assertTrue(SubtabProjectViewGrouping.isEnabled());

        TabzSettings.getInstance().setTabzEnabled(false);
        TabzPresentation.applySettingsChange();
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertNull(editor.getUserData(ComponentSubtabsManager.TABZ_BAR_KEY));
        assertNull(editor.getUserData(SidetabsManager.SIDETAB_BAR_KEY));
        assertFalse(SubtabProjectViewGrouping.isEnabled());
        assertNull(SubtabsExpandOverlay.visibleButton(editor));
        assertNull(SidetabsToggleOverlay.visibleButton(editor));
        assertNull(SubtabsCollapseOverlay.visibleButton(editor));
    }

    public void testBackgroundRefreshStaysIdleWhileTabzIsDisabled() throws Exception {
        VirtualFile html = createSourceFile("page.html");
        WriteAction.run(() -> html.setBinaryContent("""
                <html><body><p>Hello</p></body></html>
                """.getBytes(StandardCharsets.UTF_8)));
        openAndSettle(html);
        ComponentSubtabsManager.attachIfNeeded(getProject(), html);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        TabzSettings.getInstance().setTabzEnabled(false);
        TabzPresentation.applySettingsChange();
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        com.intellij.openapi.editor.Document document =
                com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(html);
        assertNotNull(document);
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(
                getProject(),
                () -> document.insertString(document.getTextLength(), "\n<!-- edit -->")
        );
        PlatformTestUtil.waitWithEventsDispatching(
                "deferred refresh must settle",
                () -> true,
                5
        );
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        FileEditor editor = FileEditorManager.getInstance(getProject()).getSelectedEditor();
        assertNotNull(editor);
        assertNull(editor.getUserData(ComponentSubtabsManager.TABZ_BAR_KEY));
    }

    private FileEditor editorFor(VirtualFile file) {
        FileEditor[] editors = FileEditorManager.getInstance(getProject()).getEditors(file);
        return editors.length == 0 ? null : editors[0];
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            TabzSettings.getInstance().setTabzEnabled(true);
        } finally {
            super.tearDown();
        }
    }

    public void testReEnablingTabzRestoresTabzAndSidetabs() throws Exception {
        VirtualFile html = createSourceFile("header.component.html");
        WriteAction.run(() -> html.setBinaryContent("""
                <header>Title</header>
                <main>Content</main>
                """.getBytes(StandardCharsets.UTF_8)));
        createSourceFile("header.component.ts");
        openAndSettle(html);

        TabzSettings settings = TabzSettings.getInstance();
        settings.setTabzEnabled(false);
        TabzPresentation.applySettingsChange();
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        settings.setTabzEnabled(true);
        TabzPresentation.applySettingsChange();
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        FileEditor editor = FileEditorManager.getInstance(getProject()).getSelectedEditor();
        assertNotNull(editor);
        assertNotNull(editor.getUserData(ComponentSubtabsManager.TABZ_BAR_KEY));
        assertNotNull(editor.getUserData(SidetabsManager.SIDETAB_BAR_KEY));
    }
}
