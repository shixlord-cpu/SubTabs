package com.zayax.tabz;

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

    public void testInactiveTabzOfferFlatMenuWithoutTabz() throws Exception {
        VirtualFile specFile = siblingFile("product-list.component.spec.ts");
        List<String> topLevel = topLevelTexts(specFile, false);
        assertFalse("tabz menus must not contain TabZ: " + topLevel, topLevel.contains("TabZ"));
        assertTrue(topLevel.contains("Show in Project view"));
        assertTrue(topLevel.contains("Create split pair"));
        assertTrue(topLevel.contains("Open tabz in new tab"));
        assertTrue(topLevel.contains("Open tabz in new window"));
        assertTrue(topLevel.contains("Move left"));
        assertTrue(topLevel.contains("Move right"));
    }

    public void testFocusedOrGrayedTabzOfferRevealOnly() {
        List<String> topLevel = topLevelTexts(htmlFile, true);
        assertTrue(topLevel.contains("Show in Project view"));
        assertTrue(topLevel.contains("Move left"));
        assertTrue(topLevel.contains("Move right"));
        assertFalse(topLevel.contains("Open tabz in new tab"));
        assertFalse(topLevel.contains("Open tabz in new window"));
        assertFalse(topLevel.contains("TabZ"));
        assertFalse("the active tabz must not offer split", topLevel.contains("Create split pair"));
    }

    public void testGrayedTabzStillOfferSplitten() throws Exception {
        VirtualFile specFile = siblingFile("product-list.component.spec.ts");
        List<String> topLevel = topLevelTexts(specFile, true);
        assertTrue("grayed tabz must still offer split: " + topLevel, topLevel.contains("Create split pair"));
    }

    public void testEveryTabzMenuOffersProjectTreeReveal() {
        List<String> texts = menuTexts(htmlFile, false);

        assertTrue(
                "every tabz menu must offer project-tree navigation: " + texts,
                texts.contains("Show in Project view")
        );
    }

    public void testTabzMenuNeverOffersGroupColors() {
        SubtabGroupColors.setEnabled(true);
        try {
            List<String> texts = menuTexts(htmlFile, false);
            assertFalse("tabz menus must not offer group colors: " + texts, texts.contains("TabZ"));
            assertFalse(texts.contains("Change group color…"));
            assertFalse(texts.contains("Reassign group color"));
        } finally {
            SubtabGroupColors.setEnabled(false);
        }
    }

    public void testHoverViewDefaultsToEnabled() {
        assertTrue(SubtabHoverView.isEnabled());
    }

    private VirtualFile siblingFile(String name) throws Exception {
        return WriteAction.computeAndWait(() -> htmlFile.getParent().createChildData(this, name));
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
