package de.sasbe.subtabs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.Point;

public class ComponentSubtabBarReorderIntegrationTest extends HeavyPlatformTestCase {
    public void testDragFirstTabBetweenSecondAndThirdShiftsTailTabs() throws Exception {
        VirtualFile dir = getVirtualFile(createTempDir("reorder-bar-first"));
        WriteAction.computeAndWait(() -> dir.createChildData(this, "product-list.component.css"));
        WriteAction.computeAndWait(() -> dir.createChildData(this, "product-list.component.html"));
        WriteAction.computeAndWait(() -> dir.createChildData(this, "product-list.component.scss"));
        VirtualFile first = WriteAction.computeAndWait(() -> dir.createChildData(this, "product-list.component.ts"));

        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(first);
        assertNotNull(match);
        assertTrue(match.relatedFiles().size() >= 4);

        VirtualFile dragged = match.relatedFiles().get(0).file();
        VirtualFile second = match.relatedFiles().get(1).file();
        VirtualFile third = match.relatedFiles().get(2).file();
        VirtualFile fourth = match.relatedFiles().get(3).file();

        ComponentSubtabBarPanel panel = new ComponentSubtabBarPanel(
                getProject(),
                new ComponentSubtabGroup(match.relatedFiles()),
                first
        );
        panel.applyReorderLayoutForTests(dragged, 2);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertTrue(panel.isReorderHiddenForTests(dragged));
        assertTrue(tabX(panel, second) < tabX(panel, third));
        assertTrue(tabX(panel, third) < tabX(panel, fourth));
        assertTrue(panel.reorderGapXForTests() > tabX(panel, second));
        assertTrue(panel.reorderGapXForTests() < tabX(panel, third));
    }

    public void testContextMenuMoveAfterPreviewDoesNotDuplicateTabs() throws Exception {
        VirtualFile dir = getVirtualFile(createTempDir("reorder-menu-move"));
        VirtualFile tsFile = WriteAction.computeAndWait(() -> dir.createChildData(this, "product-list.component.ts"));
        VirtualFile scssFile = WriteAction.computeAndWait(() -> dir.createChildData(this, "product-list.component.scss"));
        VirtualFile htmlFile = WriteAction.computeAndWait(() -> dir.createChildData(this, "product-list.component.html"));

        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(htmlFile);
        assertNotNull(match);
        assertEquals(3, match.relatedFiles().size());

        ComponentSubtabBarPanel panel = new ComponentSubtabBarPanel(
                getProject(),
                new ComponentSubtabGroup(match.relatedFiles()),
                htmlFile
        );
        panel.layOutTabsForTests(900);
        int tabCount = match.relatedFiles().size();

        panel.setReorderPreview(ComponentSubtabOrder.previewDropIndexForMoveRight(htmlFile), htmlFile);
        ComponentSubtabOrder.moveRight(getProject(), htmlFile);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        panel.layOutTabsForTests(900);

        assertEquals(tabCount, panel.tabButtonCountForTests());
        ComponentRelatedFilesCache.getInstance(getProject()).clear();
        ComponentRelatedFiles.Match after = ComponentRelatedFiles.find(htmlFile);
        assertNotNull(after);
        assertEquals(scssFile, after.relatedFiles().get(0).file());
        assertEquals(htmlFile, after.relatedFiles().get(1).file());
        assertEquals(tsFile, after.relatedFiles().get(2).file());
    }

    public void testDragShiftsVisibleTabsAndOpensGapAtDropIndex() throws Exception {
        VirtualFile dir = getVirtualFile(createTempDir("reorder-bar"));
        VirtualFile tsFile = WriteAction.computeAndWait(() -> dir.createChildData(this, "product-list.component.ts"));
        VirtualFile scssFile = WriteAction.computeAndWait(() -> dir.createChildData(this, "product-list.component.scss"));
        VirtualFile htmlFile = WriteAction.computeAndWait(() -> dir.createChildData(this, "product-list.component.html"));

        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(htmlFile);
        assertNotNull(match);

        ComponentSubtabBarPanel panel = new ComponentSubtabBarPanel(
                getProject(),
                new ComponentSubtabGroup(match.relatedFiles()),
                htmlFile
        );
        panel.applyReorderLayoutForTests(scssFile, 2);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        int htmlX = tabX(panel, htmlFile);
        int tsX = tabX(panel, tsFile);
        assertTrue("dragged tab is hidden while reordering", panel.isReorderHiddenForTests(scssFile));
        assertTrue("dragged tab is removed from the row", tabX(panel, scssFile) < -1000);
        assertTrue("visible tabs keep their order at the ends", htmlX >= 0 && tsX > htmlX);
        assertTrue("last tab shifts right to make room", tsX > htmlX + panel.reorderGapWidthForTests());
        assertTrue(
                "drop gap sits between the first and last tab",
                panel.reorderGapXForTests() > htmlX && panel.reorderGapXForTests() < tsX
        );
        assertTrue(panel.reorderGapWidthForTests() > 0);
    }

    private static int tabX(@NotNull ComponentSubtabBarPanel panel, @NotNull VirtualFile file) {
        var button = panel.buttonFor(file);
        return button == null ? Integer.MIN_VALUE : button.getX();
    }
}
