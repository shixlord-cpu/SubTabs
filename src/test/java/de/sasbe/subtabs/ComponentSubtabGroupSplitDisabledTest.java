package de.sasbe.subtabs;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Pins down that the group split stays switched off: no entry offers it, asking for one changes
 * nothing, and no pane keeps split chrome such as the close-side button or the alignment spacer.
 */
public class ComponentSubtabGroupSplitDisabledTest extends RealEditorWindowTestCase {
    private VirtualFile htmlFile;
    private VirtualFile specFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        htmlFile = createSourceFile("product-list.component.html");
        specFile = createSourceFile("product-list.component.spec.ts");
    }

    public void testTheFeatureFlagStaysOff() {
        assertFalse(
                "the group split has to stay off until it is reworked",
                ComponentSubtabGroupSplitNavigation.ENABLED
        );
    }

    public void testTheContextMenuOffersNoSplit() {
        openAndSettle(htmlFile);

        List<String> texts = menuTexts();

        assertFalse(
                "no menu entry may offer a split: " + texts,
                texts.stream().anyMatch(text -> text.contains("Split"))
        );
    }

    public void testTheContextMenuKeepsItsOtherEntries() {
        openAndSettle(htmlFile);

        List<String> texts = menuTexts();

        assertTrue("opening in a new tab has to stay: " + texts,
                texts.stream().anyMatch(text -> text.contains("neuen Tab")));
        assertTrue("opening in a new window has to stay: " + texts,
                texts.stream().anyMatch(text -> text.contains("neuen Fenster")));
    }

    public void testAskingForASplitChangesNothing() {
        openAndSettle(htmlFile);

        requestSplit();

        assertEquals("no second pane may appear", 1, manager.getWindows().length);
        assertFalse("the partner file must not be opened", manager.isFileOpen(specFile));
        assertTrue(
                "nothing may be registered as a split",
                ComponentSubtabGroupSplitRegistry.getInstance(getProject()).all().isEmpty()
        );
    }

    public void testTheSubtabBarCarriesNoSplitChrome() {
        openAndSettle(htmlFile);
        requestSplit();

        ComponentSubtabBarPanel bar = barFor(htmlFile);
        assertNotNull("the subtab bar has to exist", bar);
        assertFalse("no button may offer closing a split side", bar.isCloseSideButtonVisible());
        assertEquals("the bar needs no spacer without a split", 0, bar.topSpacerHeight());
    }

    public void testTheMainTabStripStaysVisible() {
        openAndSettle(htmlFile);
        requestSplit();

        for (EditorWindow window : manager.getWindows()) {
            assertTrue(
                    "no pane may lose its main tab strip",
                    ComponentSubtabGroupSplitNavigation.isTabStripVisible(window)
            );
        }
    }

    private void requestSplit() {
        ComponentSubtabGroupSplitNavigation.openInGroupSplit(
                getProject(), htmlFile, specFile, ComponentSubtabGroupSplitNavigation.SplitSide.RIGHT);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    }

    private List<String> menuTexts() {
        DefaultActionGroup group = ComponentSubtabBarPopup.buildMenu(getProject(), htmlFile, specFile);
        List<String> texts = new ArrayList<>();
        for (AnAction action : group.getChildActionsOrStubs()) {
            String text = action.getTemplatePresentation().getText();
            if (text != null) {
                texts.add(text);
            }
        }
        return texts;
    }

    private ComponentSubtabBarPanel barFor(VirtualFile file) {
        PlatformTestUtil.waitWithEventsDispatching(
                "no subtab bar appeared for " + file.getName(),
                () -> attachedBarFor(file) != null,
                30
        );
        return attachedBarFor(file);
    }

    private ComponentSubtabBarPanel attachedBarFor(VirtualFile file) {
        for (FileEditor editor : ComponentSubtabsManager.editorsFor(
                FileEditorManager.getInstance(getProject()), file)) {
            ComponentSubtabBarPanel panel = editor.getUserData(ComponentSubtabsManager.SUBTAB_BAR_KEY);
            if (panel != null) {
                return panel;
            }
        }
        return null;
    }
}
