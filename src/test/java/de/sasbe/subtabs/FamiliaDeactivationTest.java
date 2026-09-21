package de.sasbe.subtabs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;

import java.nio.charset.StandardCharsets;

public class FamiliaDeactivationTest extends RealEditorWindowTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setFamiliaEnabled(true);
        settings.setSubtabsActive(true);
        settings.setSidetabsActive(true);
        settings.setSidetabsExpanded(true);
        settings.setShowCollapseButton(true);
        settings.setGroupRelatedFilesInProjectView(true);
        settings.setSidetabRules(SidetabRulesDefaults.createDefaults());
        ComponentSubtabsDocumentListener.install(getProject());
    }

    public void testDisablingFamiliaRemovesAllAttachedUi() throws Exception {
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
        assertNotNull(editor.getUserData(ComponentSubtabsManager.SUBTAB_BAR_KEY));
        assertNotNull(editor.getUserData(SidetabsManager.SIDETAB_BAR_KEY));
        assertTrue(SubtabProjectViewGrouping.isEnabled());

        SubtabsSettings.getInstance().setFamiliaEnabled(false);
        SubtabsPresentation.applySettingsChange();
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertNull(editor.getUserData(ComponentSubtabsManager.SUBTAB_BAR_KEY));
        assertNull(editor.getUserData(SidetabsManager.SIDETAB_BAR_KEY));
        assertFalse(SubtabProjectViewGrouping.isEnabled());
        assertNull(SubtabsExpandOverlay.visibleButton(editor));
        assertNull(SidetabsToggleOverlay.visibleButton(editor));
        assertNull(SubtabsCollapseOverlay.visibleButton(editor));
    }

    public void testBackgroundRefreshStaysIdleWhileFamiliaIsDisabled() throws Exception {
        VirtualFile html = createSourceFile("page.html");
        WriteAction.run(() -> html.setBinaryContent("""
                <html><body><p>Hello</p></body></html>
                """.getBytes(StandardCharsets.UTF_8)));
        openAndSettle(html);
        ComponentSubtabsManager.attachIfNeeded(getProject(), html);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        SubtabsSettings.getInstance().setFamiliaEnabled(false);
        SubtabsPresentation.applySettingsChange();
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
        assertNull(editor.getUserData(ComponentSubtabsManager.SUBTAB_BAR_KEY));
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            SubtabsSettings.getInstance().setFamiliaEnabled(true);
        } finally {
            super.tearDown();
        }
    }

    public void testReEnablingFamiliaRestoresSubtabsAndSidetabs() throws Exception {
        VirtualFile html = createSourceFile("header.component.html");
        WriteAction.run(() -> html.setBinaryContent("""
                <header>Title</header>
                <main>Content</main>
                """.getBytes(StandardCharsets.UTF_8)));
        createSourceFile("header.component.ts");
        openAndSettle(html);

        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setFamiliaEnabled(false);
        SubtabsPresentation.applySettingsChange();
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        settings.setFamiliaEnabled(true);
        SubtabsPresentation.applySettingsChange();
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        FileEditor editor = FileEditorManager.getInstance(getProject()).getSelectedEditor();
        assertNotNull(editor);
        assertNotNull(editor.getUserData(ComponentSubtabsManager.SUBTAB_BAR_KEY));
        assertNotNull(editor.getUserData(SidetabsManager.SIDETAB_BAR_KEY));
    }
}
