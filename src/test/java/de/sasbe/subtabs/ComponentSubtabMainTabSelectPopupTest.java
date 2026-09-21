package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;

import javax.swing.SwingConstants;

/**
 * Covers what a click inside the hover select box of a main tab does. The box can belong to a tab that
 * is not focused, and the click has to act on that tab instead of the focused one.
 */
public class ComponentSubtabMainTabSelectPopupTest extends RealEditorWindowTestCase {
    private VirtualFile htmlFile;
    private VirtualFile specFile;
    private VirtualFile styleFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        htmlFile = createSourceFile("product-list.component.html");
        specFile = createSourceFile("product-list.component.spec.ts");
        styleFile = createSourceFile("product-list.component.scss");
    }

    public void testClickingASubtabSwitchesTheBackgroundTabAndFocusesIt() {
        openAndSettle(htmlFile);
        openAndSettle(specFile);
        assertEquals("the second file is the focused tab", specFile, selectedFile());

        clickInPopupOf(htmlFile, styleFile);

        assertFalse("the background tab has to take the new file", manager.isFileOpen(htmlFile));
        assertTrue("the focused tab must keep its file", manager.isFileOpen(specFile));
        assertTrue(manager.isFileOpen(styleFile));
        assertEquals("the switched tab has to end up focused", styleFile, selectedFile());
    }

    public void testClickingASubtabSwitchesTheTabInItsOwnPane() {
        openAndSettle(htmlFile);
        EditorWindow firstPane = manager.getCurrentWindow();
        assertNotNull(firstPane);
        EditorWindow secondPane = firstPane.split(SwingConstants.VERTICAL, true, specFile, true);
        assertNotNull(secondPane);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        manager.setCurrentWindow(secondPane);

        clickInPopupOf(htmlFile, styleFile);

        assertSame("the new file belongs into the pane of the clicked tab", firstPane, windowOf(styleFile));
        assertTrue("the other pane must not be touched", secondPane.isFileOpen(specFile));
        assertSame("the pane of the clicked tab has to become the current one",
                firstPane, manager.getCurrentWindow());
        assertEquals(styleFile, selectedFile());
    }

    public void testClickingTheTabsOwnFileFocusesThatTabWithoutDuplicating() {
        openAndSettle(htmlFile);
        EditorWindow firstPane = manager.getCurrentWindow();
        assertNotNull(firstPane);
        EditorWindow secondPane = firstPane.split(SwingConstants.VERTICAL, true, specFile, true);
        assertNotNull(secondPane);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        manager.setCurrentWindow(secondPane);

        clickInPopupOf(htmlFile, htmlFile);

        assertEquals("the file must not be opened a second time", 1, paneCountOf(htmlFile));
        assertSame(firstPane, manager.getCurrentWindow());
        assertEquals(htmlFile, selectedFile());
    }

    private void clickInPopupOf(VirtualFile tabFile, VirtualFile clickedFile) {
        ComponentSubtabMainTabSelectPopup.openFromPopup(getProject(), tabFile, clickedFile);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    }
}
