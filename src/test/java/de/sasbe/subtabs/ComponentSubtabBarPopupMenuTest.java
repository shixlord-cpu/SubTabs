package de.sasbe.subtabs;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;

import java.util.ArrayList;
import java.util.List;

public class ComponentSubtabBarPopupMenuTest extends HeavyPlatformTestCase {
    private VirtualFile htmlFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        VirtualFile dir = getVirtualFile(createTempDir("app"));
        WriteAction.runAndWait(() -> {
            dir.createChildData(this, "product-list.component.ts");
            dir.createChildData(this, "product-list.component.scss");
        });
        htmlFile = WriteAction.computeAndWait(() -> dir.createChildData(this, "product-list.component.html"));
    }

    public void testInactiveSubtabsOfferFlatMenuWithoutFamilia() {
        List<String> topLevel = topLevelTexts(htmlFile, false);
        assertFalse("subtab menus must not contain Familia: " + topLevel, topLevel.contains("Familia"));
        assertTrue(topLevel.contains("Im Projektbaum anzeigen"));
        assertTrue(topLevel.contains("Sub-Tab im neuen Tab öffnen"));
        assertTrue(topLevel.contains("Sub-Tab im neuen Fenster öffnen"));
    }

    public void testFocusedOrGrayedSubtabsOfferRevealOnly() {
        List<String> topLevel = topLevelTexts(htmlFile, true);
        assertEquals("focused and grayed subtabs keep one entry only: " + topLevel, 1, topLevel.size());
        assertEquals("Im Projektbaum anzeigen", topLevel.get(0));
        assertFalse(topLevel.contains("Familia"));
    }

    public void testEverySubtabMenuOffersProjectTreeReveal() {
        List<String> texts = menuTexts(htmlFile, false);

        assertTrue(
                "every subtab menu must offer project-tree navigation: " + texts,
                texts.contains("Im Projektbaum anzeigen")
        );
    }

    public void testSubtabMenuNeverOffersGroupColors() {
        SubtabGroupColors.setEnabled(true);
        try {
            List<String> texts = menuTexts(htmlFile, false);
            assertFalse("subtab menus must not offer group colors: " + texts, texts.contains("Familia"));
            assertFalse(texts.contains("Gruppenfarbe ändern…"));
            assertFalse(texts.contains("Gruppenfarbe neu zuweisen"));
        } finally {
            SubtabGroupColors.setEnabled(false);
        }
    }

    public void testHoverViewDefaultsToEnabled() {
        assertTrue(SubtabHoverView.isEnabled());
    }

    private List<String> topLevelTexts(VirtualFile targetFile, boolean revealOnly) {
        DefaultActionGroup group = ComponentSubtabBarPopup.buildMenu(
                getProject(), htmlFile, targetFile, revealOnly);
        List<String> texts = new ArrayList<>();
        for (AnAction action : group.getChildActionsOrStubs()) {
            String text = action.getTemplatePresentation().getText();
            if (text != null) {
                texts.add(text);
            }
        }
        return texts;
    }

    private List<String> menuTexts(VirtualFile targetFile, boolean revealOnly) {
        DefaultActionGroup group = ComponentSubtabBarPopup.buildMenu(
                getProject(), htmlFile, targetFile, revealOnly);
        List<String> texts = new ArrayList<>();
        collectTexts(group, texts);
        return texts;
    }

    private static void collectTexts(DefaultActionGroup group, List<String> texts) {
        for (AnAction action : group.getChildActionsOrStubs()) {
            if (action instanceof DefaultActionGroup nested) {
                String text = nested.getTemplatePresentation().getText();
                if (text != null) {
                    texts.add(text);
                }
                collectTexts(nested, texts);
                continue;
            }
            String text = action.getTemplatePresentation().getText();
            if (text != null) {
                texts.add(text);
            }
        }
    }
}
