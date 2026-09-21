package de.sasbe.subtabs;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.tabs.TabInfo;

import javax.swing.JPanel;

/**
 * Covers the hover marking of main tabs. The platform only tracks a single hovered tab per strip, so
 * these tests pin down that every background tab of a group really gets marked.
 */
public class ComponentSubtabMainTabHoverTest extends RealEditorWindowTestCase {
    private final JPanel hoverSource = new JPanel();

    private VirtualFile htmlFile;
    private VirtualFile specFile;
    private VirtualFile styleFile;
    private VirtualFile otherGroupFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        htmlFile = createSourceFile("product-list.component.html");
        specFile = createSourceFile("product-list.component.spec.ts");
        styleFile = createSourceFile("product-list.component.scss");
        otherGroupFile = createSourceFile("user-card.component.html");
        createSourceFile("user-card.component.ts");
    }

    public void testHoveringTheGroupMarksEveryBackgroundTab() {
        openAndSettle(htmlFile);
        openAndSettle(specFile);
        openAndSettle(styleFile);

        enterGroup();

        assertTrue("a background tab of the group has to be marked", isMarked(htmlFile));
        assertTrue("every background tab has to be marked, not just one", isMarked(specFile));
    }

    public void testHoveringTheGroupLeavesTheVisibleTabAlone() {
        openAndSettle(htmlFile);
        openAndSettle(specFile);
        openAndSettle(styleFile);
        assertEquals("the last opened file is the visible one", styleFile, selectedFile());

        enterGroup();

        assertFalse("the tab that is already on screen must stay unmarked", isMarked(styleFile));
    }

    public void testHoveringTheGroupIgnoresOtherGroups() {
        openAndSettle(htmlFile);
        openAndSettle(otherGroupFile);
        openAndSettle(specFile);

        enterGroup();

        assertTrue(isMarked(htmlFile));
        assertFalse("a tab of another group is none of the hover's business", isMarked(otherGroupFile));
    }

    public void testLeavingRestoresEveryTab() {
        openAndSettle(htmlFile);
        openAndSettle(specFile);
        openAndSettle(styleFile);

        enterGroup();
        ComponentSubtabMainTabHover.onExit(hoverSource);

        assertFalse(isMarked(htmlFile));
        assertFalse(isMarked(specFile));
        assertFalse(isMarked(styleFile));
    }

    public void testHoveringASingleFileMarksOnlyThatTab() {
        openAndSettle(htmlFile);
        openAndSettle(specFile);
        openAndSettle(styleFile);

        ComponentSubtabMainTabHover.onEnter(getProject(), htmlFile, hoverSource);

        assertTrue(isMarked(htmlFile));
        assertFalse(isMarked(specFile));
    }

    private void enterGroup() {
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(htmlFile);
        assertNotNull("the test files have to form a subtab group", match);
        ComponentSubtabMainTabHover.onEnterGroup(getProject(), match.baseName(), hoverSource);
    }

    private boolean isMarked(VirtualFile file) {
        TabInfo tabInfo = tabInfoOf(file);
        assertNotNull("no main tab for " + file.getName(), tabInfo);
        return tabInfo.getTabColor() != null;
    }
}
