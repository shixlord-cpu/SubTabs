package de.sasbe.subtabs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;

import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.Set;

public class ComponentSubtabProjectViewHoverMainTabTest extends HeavyPlatformTestCase {
    public void testDirectoryModeMarksEveryRelatedFile() throws Exception {
        SubtabsSettings.getInstance().setGroupRelatedFilesInProjectView(false);

        VirtualFile rootDir = getVirtualFile(createTempDir("project"));
        WriteAction.computeAndWait(() -> rootDir.createChildData(this, "header.component.html"));
        WriteAction.computeAndWait(() -> rootDir.createChildData(this, "header.component.scss"));
        WriteAction.computeAndWait(() -> rootDir.createChildData(this, "header.component.spec.ts"));
        VirtualFile ts = WriteAction.computeAndWait(() -> rootDir.createChildData(this, "header.component.ts"));

        JTree tree = flatTree(
                rootDir,
                rootDir.findChild("header.component.html"),
                rootDir.findChild("header.component.scss"),
                rootDir.findChild("header.component.spec.ts"),
                ts
        );
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(ts);
        assertNotNull(match);

        Set<Integer> rows = ComponentSubtabProjectViewHover.collectMainTabHoverRows(tree, match, ts);
        assertEquals(4, rows.size());
    }

    public void testDirectoryModeExternalHoverMarksEveryRelatedFile() throws Exception {
        SubtabsSettings.getInstance().setGroupRelatedFilesInProjectView(false);

        VirtualFile rootDir = getVirtualFile(createTempDir("project"));
        VirtualFile html = WriteAction.computeAndWait(() -> rootDir.createChildData(this, "header.component.html"));
        VirtualFile scss = WriteAction.computeAndWait(() -> rootDir.createChildData(this, "header.component.scss"));
        VirtualFile spec = WriteAction.computeAndWait(() -> rootDir.createChildData(this, "header.component.spec.ts"));
        VirtualFile ts = WriteAction.computeAndWait(() -> rootDir.createChildData(this, "header.component.ts"));

        JTree tree = flatTree(rootDir, html, scss, spec, ts);
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(ts);
        assertNotNull(match);

        JPanel tabLabel = new JPanel();
        Set<Integer> rows = ComponentSubtabProjectViewHover.collectMainTabHoverRows(tree, match, ts);
        ComponentSubtabProjectViewHover.activateMainTabHoverForTest(tabLabel, tree, rows);

        Set<Integer> activeRows = ComponentSubtabProjectViewHover.externalHoverRows(tree);
        assertNotNull(activeRows);
        assertEquals(4, activeRows.size());
        assertSame(tabLabel, ComponentSubtabProjectViewHover.hoverOwner(tree));
        for (VirtualFile file : new VirtualFile[]{html, scss, spec, ts}) {
            assertTrue(isExternalHoverRow(tree, file));
        }
    }

    public void testGroupingModeMarksGroupAndActiveTabFile() throws Exception {
        SubtabsSettings.getInstance().setGroupRelatedFilesInProjectView(true);

        VirtualFile rootDir = getVirtualFile(createTempDir("project"));
        VirtualFile html = WriteAction.computeAndWait(() -> rootDir.createChildData(this, "header.component.html"));
        VirtualFile scss = WriteAction.computeAndWait(() -> rootDir.createChildData(this, "header.component.scss"));
        VirtualFile spec = WriteAction.computeAndWait(() -> rootDir.createChildData(this, "header.component.spec.ts"));
        VirtualFile ts = WriteAction.computeAndWait(() -> rootDir.createChildData(this, "header.component.ts"));

        JTree tree = groupedTree(rootDir, html, scss, spec, ts);
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(ts);
        assertNotNull(match);

        Set<Integer> rows = ComponentSubtabProjectViewHover.collectMainTabHoverRows(tree, match, ts);
        assertEquals(2, rows.size());
        assertTrue(rows.contains(rowForFile(tree, ts)));
        assertFalse(rows.contains(rowForFile(tree, html)));
        assertFalse(rows.contains(rowForFile(tree, scss)));
        assertFalse(rows.contains(rowForFile(tree, spec)));
        assertTrue("group row has to be marked together with the tab file", rows.contains(groupRow(tree)));

        JPanel tabLabel = new JPanel();
        ComponentSubtabProjectViewHover.activateMainTabHoverForTest(tabLabel, tree, rows, ts);
        assertEquals(ts, ComponentSubtabProjectViewHover.primaryHoverFile(tree));
    }

    public void testStaleExitFromPreviousTabDoesNotClearCurrentHover() throws Exception {
        SubtabsSettings.getInstance().setGroupRelatedFilesInProjectView(false);

        VirtualFile rootDir = getVirtualFile(createTempDir("project"));
        VirtualFile headerHtml = WriteAction.computeAndWait(() -> rootDir.createChildData(this, "header.component.html"));
        VirtualFile headerTs = WriteAction.computeAndWait(() -> rootDir.createChildData(this, "header.component.ts"));
        VirtualFile productHtml = WriteAction.computeAndWait(() -> rootDir.createChildData(this, "product-list.component.html"));
        VirtualFile productTs = WriteAction.computeAndWait(() -> rootDir.createChildData(this, "product-list.component.ts"));

        JTree tree = flatTree(rootDir, headerHtml, headerTs, productHtml, productTs);
        ComponentRelatedFiles.Match headerMatch = ComponentRelatedFiles.find(headerTs);
        ComponentRelatedFiles.Match productMatch = ComponentRelatedFiles.find(productTs);
        assertNotNull(headerMatch);
        assertNotNull(productMatch);

        JPanel headerTab = new JPanel();
        JPanel productTab = new JPanel();

        ComponentSubtabProjectViewHover.activateMainTabHoverForTest(
                headerTab,
                tree,
                ComponentSubtabProjectViewHover.collectMainTabHoverRows(tree, headerMatch, headerTs)
        );
        ComponentSubtabProjectViewHover.activateMainTabHoverForTest(
                productTab,
                tree,
                ComponentSubtabProjectViewHover.collectMainTabHoverRows(tree, productMatch, productTs)
        );

        assertSame(productTab, ComponentSubtabProjectViewHover.hoverOwner(tree));
        assertTrue(isExternalHoverRow(tree, productHtml));
        assertTrue(isExternalHoverRow(tree, productTs));

        ComponentSubtabProjectViewHover.onExit(headerTab);

        assertSame("stale exit from the previous tab must not clear the active hover",
                productTab, ComponentSubtabProjectViewHover.hoverOwner(tree));
        assertTrue(isExternalHoverRow(tree, productHtml));
        assertTrue(isExternalHoverRow(tree, productTs));
    }

    public void testSingleFileMainTabHoverMarksOnlyThatFile() throws Exception {
        VirtualFile rootDir = getVirtualFile(createTempDir("project"));
        VirtualFile readme = WriteAction.computeAndWait(() -> rootDir.createChildData(this, "readme.md"));

        JTree tree = flatTree(rootDir, readme);
        JPanel tabLabel = new JPanel();

        assertNull(ComponentRelatedFiles.find(readme));
        ComponentSubtabProjectViewHover.activateSubtabHoverForTest(tabLabel, tree, readme);

        assertSame(tabLabel, ComponentSubtabProjectViewHover.hoverOwner(tree));
        assertNull(ComponentSubtabProjectViewHover.externalHoverRows(tree));
    }

    public void testSingleFileHoverReplacesGroupHoverFromOtherOwner() throws Exception {
        SubtabsSettings.getInstance().setGroupRelatedFilesInProjectView(true);

        VirtualFile rootDir = getVirtualFile(createTempDir("project"));
        VirtualFile html = WriteAction.computeAndWait(() -> rootDir.createChildData(this, "header.component.html"));
        VirtualFile scss = WriteAction.computeAndWait(() -> rootDir.createChildData(this, "header.component.scss"));
        VirtualFile ts = WriteAction.computeAndWait(() -> rootDir.createChildData(this, "header.component.ts"));

        JTree tree = groupedTree(rootDir, html, scss, ts);
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(ts);
        assertNotNull(match);

        JPanel tabLabel = new JPanel();
        JPanel popupItem = new JPanel();

        ComponentSubtabProjectViewHover.activateMainTabHoverForTest(
                tabLabel,
                tree,
                ComponentSubtabProjectViewHover.collectMainTabHoverRows(tree, match, ts)
        );
        assertTrue(isExternalHoverRow(tree, ts));

        ComponentSubtabProjectViewHover.activateSubtabHoverForTest(popupItem, tree, html);

        assertSame(popupItem, ComponentSubtabProjectViewHover.hoverOwner(tree));
        assertFalse(isExternalHoverRow(tree, ts));
        assertNull(ComponentSubtabProjectViewHover.externalHoverRows(tree));
    }

    public void testSwitchingPopupEntryUpdatesGroupingHoverOwner() throws Exception {
        SubtabsSettings.getInstance().setGroupRelatedFilesInProjectView(true);

        VirtualFile rootDir = getVirtualFile(createTempDir("project"));
        VirtualFile html = WriteAction.computeAndWait(() -> rootDir.createChildData(this, "header.component.html"));
        VirtualFile ts = WriteAction.computeAndWait(() -> rootDir.createChildData(this, "header.component.ts"));

        JTree tree = flatTree(rootDir, html, ts);
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(ts);
        assertNotNull(match);

        JPanel htmlItem = new JPanel();
        JPanel tsItem = new JPanel();

        ComponentSubtabProjectViewHover.activateMainTabHoverForTest(
                htmlItem,
                tree,
                ComponentSubtabProjectViewHover.collectMainTabHoverRows(tree, match, html)
        );
        assertSame(htmlItem, ComponentSubtabProjectViewHover.hoverOwner(tree));
        assertTrue(isExternalHoverRow(tree, html));

        ComponentSubtabProjectViewHover.activateMainTabHoverForTest(
                tsItem,
                tree,
                ComponentSubtabProjectViewHover.collectMainTabHoverRows(tree, match, ts)
        );
        assertSame(tsItem, ComponentSubtabProjectViewHover.hoverOwner(tree));
        assertFalse(isExternalHoverRow(tree, html));
        assertTrue(isExternalHoverRow(tree, ts));

        ComponentSubtabProjectViewHover.onExit(htmlItem);
        assertSame(tsItem, ComponentSubtabProjectViewHover.hoverOwner(tree));
        assertTrue(isExternalHoverRow(tree, ts));
    }

    public void testDirectoryHoverIsVisibleOnScreenshot() throws Exception {
        SubtabsSettings.getInstance().setGroupRelatedFilesInProjectView(false);
        HeaderFiles files = createHeaderFiles();
        JTree tree = flatTree(files.root, files.html, files.scss, files.spec, files.ts, files.other);
        ProjectViewTreeOverlayPanel overlay = overlayOf(tree);

        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(files.ts);
        assertNotNull(match);
        ComponentSubtabProjectViewHover.activateMainTabHoverForTest(
                new JPanel(),
                tree,
                ComponentSubtabProjectViewHover.collectMainTabHoverRows(tree, match, files.ts)
        );

        java.awt.image.BufferedImage image = paintOverlay(overlay, "directory-main-tab-hover.png");
        assertHoveredRowsLookDifferent(tree, overlay, image, Set.of(
                rowForFile(tree, files.html),
                rowForFile(tree, files.scss),
                rowForFile(tree, files.spec),
                rowForFile(tree, files.ts)
        ), rowForFile(tree, files.other));
    }

    public void testGroupedHoverIsVisibleOnScreenshot() throws Exception {
        SubtabsSettings.getInstance().setGroupRelatedFilesInProjectView(true);
        HeaderFiles files = createHeaderFiles();
        JTree tree = groupedTree(files.root, files.html, files.scss, files.spec, files.ts);
        ProjectViewTreeOverlayPanel overlay = overlayOf(tree);

        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(files.ts);
        assertNotNull(match);
        Set<Integer> rows = ComponentSubtabProjectViewHover.collectMainTabHoverRows(tree, match, files.ts);
        ComponentSubtabProjectViewHover.activateMainTabHoverForTest(new JPanel(), tree, rows, files.ts);

        java.awt.image.BufferedImage image = paintOverlay(overlay, "grouped-main-tab-hover.png");
        assertTrue("group row must be marked", rows.contains(groupRow(tree)));
        assertTrue("tab file must be marked", rows.contains(rowForFile(tree, files.ts)));
        assertFalse("other group files must stay unmarked", rows.contains(rowForFile(tree, files.html)));
        int plain = sampleRow(tree, overlay, image, rowForFile(tree, files.html));
        int highlighted = sampleRow(tree, overlay, image, rowForFile(tree, files.ts));
        assertTrue(
                "tab file row must paint differently from sibling files ("
                        + Integer.toHexString(highlighted) + " vs " + Integer.toHexString(plain) + ")",
                highlighted != plain
        );
    }

    private static ProjectViewTreeOverlayPanel overlayOf(JTree tree) {
        JPanel button = new JPanel();
        button.setPreferredSize(new java.awt.Dimension(20, 20));
        ComponentSubtabProjectViewTreeUI.install(tree);
        ProjectViewTreeOverlayPanel overlay = new ProjectViewTreeOverlayPanel(tree, button);
        overlay.setSize(520, 360);
        overlay.relayout();
        overlay.doLayout();
        overlay.validate();
        return overlay;
    }

    private static java.awt.image.BufferedImage paintOverlay(
            ProjectViewTreeOverlayPanel overlay,
            String fileName
    ) throws Exception {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
                overlay.getWidth(),
                overlay.getHeight(),
                java.awt.image.BufferedImage.TYPE_INT_RGB
        );
        java.awt.Graphics2D graphics = image.createGraphics();
        graphics.setColor(java.awt.Color.WHITE);
        graphics.fillRect(0, 0, overlay.getWidth(), overlay.getHeight());
        overlay.paint(graphics);
        graphics.dispose();

        java.io.File dir = new java.io.File("C:/Users/SasBe/Desktop/SubTabs/build/hover-proof");
        assertTrue(dir.mkdirs() || dir.isDirectory());
        javax.imageio.ImageIO.write(image, "png", new java.io.File(dir, fileName));
        return image;
    }

    private static void assertHoveredRowsLookDifferent(
            JTree tree,
            ProjectViewTreeOverlayPanel overlay,
            java.awt.image.BufferedImage image,
            Set<Integer> hoveredRows,
            int plainRow
    ) {
        int plain = sampleRow(tree, overlay, image, plainRow);
        for (int row : hoveredRows) {
            int hovered = sampleRow(tree, overlay, image, row);
            assertTrue(
                    "hovered row " + row + " must paint differently from a plain row ("
                            + Integer.toHexString(hovered) + " vs " + Integer.toHexString(plain) + ")",
                    hovered != plain
            );
        }
    }

    private static int sampleRow(
            JTree tree,
            ProjectViewTreeOverlayPanel overlay,
            java.awt.image.BufferedImage image,
            int row
    ) {
        java.awt.Rectangle bounds = tree.getRowBounds(row);
        assertNotNull("missing bounds for row " + row, bounds);
        int x = Math.min(image.getWidth() - 1, Math.max(0, tree.getX() + bounds.x + Math.max(8, bounds.width / 3)));
        int y = Math.min(image.getHeight() - 1, Math.max(0, tree.getY() + bounds.y + Math.max(1, bounds.height / 2)));
        return image.getRGB(x, y);
    }

    private HeaderFiles createHeaderFiles() throws Exception {
        VirtualFile root = getVirtualFile(createTempDir("project"));
        VirtualFile html = WriteAction.computeAndWait(() -> root.createChildData(this, "header.component.html"));
        VirtualFile scss = WriteAction.computeAndWait(() -> root.createChildData(this, "header.component.scss"));
        VirtualFile spec = WriteAction.computeAndWait(() -> root.createChildData(this, "header.component.spec.ts"));
        VirtualFile ts = WriteAction.computeAndWait(() -> root.createChildData(this, "header.component.ts"));
        VirtualFile other = WriteAction.computeAndWait(() -> root.createChildData(this, "readme.md"));
        return new HeaderFiles(root, html, scss, spec, ts, other);
    }

    private record HeaderFiles(
            VirtualFile root,
            VirtualFile html,
            VirtualFile scss,
            VirtualFile spec,
            VirtualFile ts,
            VirtualFile other
    ) {
    }

    private static boolean isExternalHoverRow(JTree tree, VirtualFile file) {
        int row = rowForFile(tree, file);
        assertTrue("expected visible row for " + file.getName(), row >= 0);
        return ComponentSubtabProjectViewHover.isExternalHoverRow(tree, row);
    }

    private static int groupRow(JTree tree) {
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath path = tree.getPathForRow(row);
            if (path == null) {
                continue;
            }
            Object userObject = path.getLastPathComponent() instanceof DefaultMutableTreeNode node
                    ? node.getUserObject()
                    : path.getLastPathComponent();
            if (userObject instanceof ComponentSubtabProjectViewHover.GroupNode) {
                return row;
            }
        }
        return -1;
    }

    private static int rowForFile(JTree tree, VirtualFile file) {
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath path = tree.getPathForRow(row);
            if (file.equals(ComponentSubtabProjectViewHover.virtualFileOf(path))) {
                return row;
            }
        }
        return -1;
    }

    private static JTree groupedTree(VirtualFile rootDir, VirtualFile... files) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootDir);
        DefaultMutableTreeNode group = new DefaultMutableTreeNode(new TestGroupNode(Set.of(files)));
        for (VirtualFile file : files) {
            group.add(new DefaultMutableTreeNode(file));
        }
        root.add(group);
        JTree tree = new JTree(root);
        tree.expandPath(new TreePath(root));
        tree.expandPath(new TreePath(new Object[]{root, group}));
        tree.setSize(800, 600);
        return tree;
    }

    private record TestGroupNode(Set<VirtualFile> members) implements ComponentSubtabProjectViewHover.GroupNode {
        @Override
        public boolean contains(@NotNull VirtualFile file) {
            return members.contains(file);
        }
    }

    private static JTree flatTree(VirtualFile rootDir, VirtualFile... files) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootDir);
        for (VirtualFile file : files) {
            root.add(new DefaultMutableTreeNode(file));
        }
        JTree tree = new JTree(root);
        tree.expandPath(new TreePath(root));
        tree.setSize(800, 600);
        return tree;
    }
}
