package de.sasbe.subtabs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JToggleButton;
import java.nio.charset.StandardCharsets;

public class SidetabSectionPresentationUpdateTest extends RealEditorWindowTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setSubtabsActive(false);
        settings.setSidetabsActive(true);
        settings.setSidetabsExpanded(true);
        settings.setSidetabLayoutMode(SidetabLayoutMode.BESIDE);
        settings.setSidetabsOnRight(true);
        settings.setSidetabRules(SidetabRulesDefaults.createDefaults());
        ComponentSubtabsDocumentListener.install(getProject());
    }

    public void testEditedSectionTurnsBlueWithoutReopen() throws Exception {
        VirtualFile file = createSourceFile("page.html");
        WriteAction.run(() -> file.setBinaryContent("""
                <html>
                <head><title>Hi</title></head>
                <body><p>Hello</p></body>
                </html>
                """.getBytes(StandardCharsets.UTF_8)));
        openAndSettle(file);
        SidetabsManager.attachIfNeeded(getProject(), file);

        SidetabBarPanel panel = panelFor(file);
        JToggleButton headButton = buttonForSection(panel, "Head");
        JToggleButton bodyButton = buttonForSection(panel, "Body");
        assertNotNull(headButton);
        assertNotNull(bodyButton);
        assertFalse(ComponentSubtabUi.isModified(headButton));
        assertFalse(ComponentSubtabUi.isModified(bodyButton));

        Document document = FileDocumentManager.getInstance().getDocument(file);
        assertNotNull(document);
        int bodyStart = document.getText().indexOf("<body>");
        assertTrue(bodyStart >= 0);
        WriteCommandAction.runWriteCommandAction(getProject(), () ->
                document.insertString(bodyStart + "<body>".length(), "<!-- edit -->"));
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        SidetabBarPanel attachedPanel = panel;
        PlatformTestUtil.waitWithEventsDispatching(
                "sidetab presentation must refresh after document change",
                () -> {
                    SidetabSection bodySection = attachedPanel.sections().stream()
                            .filter(section -> "Body".equals(section.name()))
                            .findFirst()
                            .orElse(null);
                    return bodySection != null
                            && SidetabSectionPresentation.compute(getProject(), document, file, bodySection).modified();
                },
                30
        );

        SidetabSection headSection = panel.sections().stream()
                .filter(section -> "Head".equals(section.name()))
                .findFirst()
                .orElseThrow();
        SidetabSection bodySection = panel.sections().stream()
                .filter(section -> "Body".equals(section.name()))
                .findFirst()
                .orElseThrow();
        assertFalse(
                "unchanged head section must stay unmodified",
                SidetabSectionPresentation.compute(getProject(), document, file, headSection).modified()
        );
        assertTrue(
                "edited body section must turn modified from the document event",
                SidetabSectionPresentation.compute(getProject(), document, file, bodySection).modified()
        );

        SidetabsManager.refreshPresentationForDocument(getProject(), document);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        panel = panelFor(file);
        headButton = buttonForSection(panel, "Head");
        bodyButton = buttonForSection(panel, "Body");
        assertNotNull(headButton);
        assertNotNull(bodyButton);
        assertFalse(
                "unchanged head sidetab must stay unmodified",
                ComponentSubtabUi.isModified(headButton)
        );
        assertTrue(
                "edited body sidetab must turn modified from the document event",
                ComponentSubtabUi.isModified(bodyButton)
        );
    }

    public void testSectionShowsErrorWaveWhenMarkupErrorIsInsideRange() throws Exception {
        VirtualFile file = createSourceFile("page.html");
        WriteAction.run(() -> file.setBinaryContent("""
                <html>
                <head><title>Hi</title></head>
                <body><p>Hello</p></body>
                </html>
                """.getBytes(StandardCharsets.UTF_8)));
        openAndSettle(file);
        SidetabsManager.attachIfNeeded(getProject(), file);

        Document document = FileDocumentManager.getInstance().getDocument(file);
        assertNotNull(document);
        int bodyStart = document.getText().indexOf("<body>");
        int bodyEnd = document.getText().indexOf("</body>") + "</body>".length();
        assertTrue(bodyStart >= 0 && bodyEnd > bodyStart);

        var model = com.intellij.openapi.editor.impl.DocumentMarkupModel.forDocument(document, getProject(), true);
        var highlighter = model.addRangeHighlighter(
                bodyStart,
                bodyEnd,
                com.intellij.openapi.editor.markup.HighlighterLayer.ERROR,
                null,
                com.intellij.openapi.editor.markup.HighlighterTargetArea.EXACT_RANGE
        );
        highlighter.setErrorStripeTooltip(
                com.intellij.codeInsight.daemon.impl.HighlightInfo.newHighlightInfo(
                                com.intellij.codeInsight.daemon.impl.HighlightInfoType.ERROR)
                        .range(bodyStart, bodyEnd)
                        .descriptionAndTooltip("invalid body")
                        .createUnconditionally()
        );

        SidetabBarPanel panel = panelFor(file);
        JToggleButton headButton = buttonForSection(panel, "Head");
        JToggleButton bodyButton = buttonForSection(panel, "Body");
        assertNotNull(headButton);
        assertNotNull(bodyButton);

        panel.refreshSectionPresentation();
        assertFalse("head section must stay clean", ComponentSubtabUi.hasErrors(headButton));
        assertTrue("body section must show errors", ComponentSubtabUi.hasErrors(bodyButton));
    }

    private SidetabBarPanel panelFor(VirtualFile file) {
        PlatformTestUtil.waitWithEventsDispatching(
                "no sidetab bar appeared for " + file.getName(),
                () -> attachedPanelFor(file) != null,
                30
        );
        return attachedPanelFor(file);
    }

    private SidetabBarPanel attachedPanelFor(VirtualFile file) {
        for (FileEditor editor : ComponentSubtabsManager.editorsFor(
                FileEditorManager.getInstance(getProject()), file)) {
            SidetabBarPanel panel = editor.getUserData(SidetabsManager.SIDETAB_BAR_KEY);
            if (panel != null) {
                return panel;
            }
        }
        return null;
    }

    private static JToggleButton buttonForSection(@NotNull SidetabBarPanel panel, @NotNull String name) {
        for (int index = 0; index < panel.sections().size(); index++) {
            if (name.equals(panel.sections().get(index).name())) {
                return panel.buttonAt(index);
            }
        }
        return null;
    }
}
