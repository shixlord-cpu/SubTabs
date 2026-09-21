package de.sasbe.subtabs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ComponentSubtabProjectViewHoverFolderTest extends HeavyPlatformTestCase {
    public void testFindHoverTargetPathReturnsFileWhenVisible() throws Exception {
        VirtualFile rootDir = getVirtualFile(createTempDir("project"));
        VirtualFile file = WriteAction.computeAndWait(() -> rootDir.createChildData(this, "file.ts"));

        JTree tree = treeWithNodes(rootDir, file);
        TreePath path = ComponentSubtabProjectViewHover.findHoverTargetPath(tree, file);
        assertNotNull(path);
        assertEquals(file, ComponentSubtabProjectViewHover.virtualFileOf(path));
    }

    public void testFindHoverTargetPathReturnsClosedParentFolder() throws Exception {
        VirtualFile rootDir = getVirtualFile(createTempDir("project"));
        VirtualFile srcDir = WriteAction.computeAndWait(() -> rootDir.createChildDirectory(this, "src"));
        VirtualFile file = WriteAction.computeAndWait(() -> srcDir.createChildData(this, "file.ts"));

        JTree tree = treeWithNodes(rootDir, srcDir, file);
        tree.collapsePath(new TreePath(new Object[]{tree.getModel().getRoot(), srcDir}));

        TreePath path = ComponentSubtabProjectViewHover.findHoverTargetPath(tree, file);
        assertNotNull(path);
        assertEquals(srcDir, ComponentSubtabProjectViewHover.virtualFileOf(path));
    }

    public void testFindHoverTargetPathReturnsOutermostVisibleClosedAncestor() throws Exception {
        VirtualFile rootDir = getVirtualFile(createTempDir("project"));
        VirtualFile srcDir = WriteAction.computeAndWait(() -> rootDir.createChildDirectory(this, "src"));
        VirtualFile nestedDir = WriteAction.computeAndWait(() -> srcDir.createChildDirectory(this, "nested"));
        VirtualFile file = WriteAction.computeAndWait(() -> nestedDir.createChildData(this, "file.ts"));

        JTree tree = treeWithNodes(rootDir, srcDir, nestedDir, file);
        tree.collapsePath(new TreePath(new Object[]{tree.getModel().getRoot(), srcDir}));

        TreePath path = ComponentSubtabProjectViewHover.findHoverTargetPath(tree, file);
        assertNotNull(path);
        assertEquals(srcDir, ComponentSubtabProjectViewHover.virtualFileOf(path));
    }

    public void testDirectoryMainTabHoverMarksEveryRelatedFile() throws Exception {
        HeaderTree header = createHeaderTree();

        List<TreePath> paths = ComponentSubtabProjectViewHover.findMainTabHoverPaths(
                header.directoryTree(),
                header.ts,
                header.relatedFiles(),
                false
        );

        assertEquals(Set.of(header.html, header.scss, header.spec, header.ts), filesOf(paths));
    }

    public void testDirectoryMainTabHoverMarksClosedFolderInsteadOfHiddenFiles() throws Exception {
        HeaderTree header = createHeaderTree();
        JTree tree = header.directoryTree();
        tree.collapsePath(pathTo(tree, header.appDir));

        List<TreePath> paths = ComponentSubtabProjectViewHover.findMainTabHoverPaths(
                tree,
                header.ts,
                header.relatedFiles(),
                false
        );

        assertEquals(List.of(header.appDir), filesOf(paths).stream().toList());
    }

    public void testGroupedMainTabHoverMarksGroupAndTabFile() throws Exception {
        HeaderTree header = createHeaderTree();

        List<TreePath> paths = ComponentSubtabProjectViewHover.findMainTabHoverPaths(
                header.groupedTree(true),
                header.ts,
                header.relatedFiles(),
                true
        );

        assertEquals(2, paths.size());
        assertTrue(isGroupPath(paths.get(0), header.ts));
        assertEquals(header.ts, ComponentSubtabProjectViewHover.virtualFileOf(paths.get(1)));
        assertFalse(filesOf(paths).contains(header.html));
        assertFalse(filesOf(paths).contains(header.scss));
        assertFalse(filesOf(paths).contains(header.spec));
    }

    public void testGroupedMainTabHoverMarksOnlyGroupWhenCollapsed() throws Exception {
        HeaderTree header = createHeaderTree();

        List<TreePath> paths = ComponentSubtabProjectViewHover.findMainTabHoverPaths(
                header.groupedTree(false),
                header.ts,
                header.relatedFiles(),
                true
        );

        assertEquals(1, paths.size());
        assertTrue(isGroupPath(paths.get(0), header.ts));
    }

    public void testGroupedMainTabHoverMarksClosedFolderWhenGroupIsHidden() throws Exception {
        HeaderTree header = createHeaderTree();
        JTree tree = header.groupedTree(true);
        tree.collapsePath(pathTo(tree, header.appDir));

        List<TreePath> paths = ComponentSubtabProjectViewHover.findMainTabHoverPaths(
                tree,
                header.ts,
                header.relatedFiles(),
                true
        );

        assertEquals(List.of(header.appDir), filesOf(paths).stream().toList());
    }

    private HeaderTree createHeaderTree() throws Exception {
        VirtualFile rootDir = getVirtualFile(createTempDir("project"));
        VirtualFile srcDir = WriteAction.computeAndWait(() -> rootDir.createChildDirectory(this, "src"));
        VirtualFile appDir = WriteAction.computeAndWait(() -> srcDir.createChildDirectory(this, "app"));
        VirtualFile html = WriteAction.computeAndWait(
                () -> appDir.createChildData(this, "header.component.html"));
        VirtualFile scss = WriteAction.computeAndWait(
                () -> appDir.createChildData(this, "header.component.scss"));
        VirtualFile spec = WriteAction.computeAndWait(
                () -> appDir.createChildData(this, "header.component.spec.ts"));
        VirtualFile ts = WriteAction.computeAndWait(
                () -> appDir.createChildData(this, "header.component.ts"));
        return new HeaderTree(appDir, html, scss, spec, ts);
    }

    private static boolean isGroupPath(@NotNull TreePath path, @NotNull VirtualFile file) {
        Object userObject = path.getLastPathComponent() instanceof DefaultMutableTreeNode node
                ? node.getUserObject()
                : path.getLastPathComponent();
        return userObject instanceof ComponentSubtabProjectViewHover.GroupNode groupNode
                && groupNode.contains(file);
    }

    private static Set<VirtualFile> filesOf(@NotNull List<TreePath> paths) {
        return paths.stream()
                .map(ComponentSubtabProjectViewHover::virtualFileOf)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private static TreePath pathTo(@NotNull JTree tree, @NotNull VirtualFile file) {
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath path = tree.getPathForRow(row);
            if (path != null && file.equals(ComponentSubtabProjectViewHover.virtualFileOf(path))) {
                return path;
            }
        }
        throw new AssertionError("no visible row for " + file.getName());
    }

    private static JTree treeWithNodes(VirtualFile rootDir, VirtualFile... descendants) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootDir);
        DefaultMutableTreeNode current = root;
        for (VirtualFile descendant : descendants) {
            DefaultMutableTreeNode child = new DefaultMutableTreeNode(descendant);
            current.add(child);
            if (descendant.isDirectory()) {
                current = child;
            }
        }
        JTree tree = new JTree(root);
        tree.expandPath(new TreePath(root));
        return tree;
    }

    private record TestGroupNode(@NotNull Set<VirtualFile> members)
            implements ComponentSubtabProjectViewHover.GroupNode {
        @Override
        public boolean contains(@NotNull VirtualFile file) {
            return members.contains(file);
        }
    }

    private static final class HeaderTree {
        private final VirtualFile appDir;
        private final VirtualFile html;
        private final VirtualFile scss;
        private final VirtualFile spec;
        private final VirtualFile ts;

        private HeaderTree(
                VirtualFile appDir,
                VirtualFile html,
                VirtualFile scss,
                VirtualFile spec,
                VirtualFile ts
        ) {
            this.appDir = appDir;
            this.html = html;
            this.scss = scss;
            this.spec = spec;
            this.ts = ts;
        }

        private List<VirtualFile> relatedFiles() {
            return List.of(html, scss, spec, ts);
        }

        private JTree directoryTree() {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
            DefaultMutableTreeNode app = new DefaultMutableTreeNode(appDir);
            app.add(new DefaultMutableTreeNode(html));
            app.add(new DefaultMutableTreeNode(scss));
            app.add(new DefaultMutableTreeNode(spec));
            app.add(new DefaultMutableTreeNode(ts));
            root.add(app);
            return expanded(root, new TreePath(new Object[]{root, app}));
        }

        private JTree groupedTree(boolean expandGroup) {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
            DefaultMutableTreeNode app = new DefaultMutableTreeNode(appDir);
            DefaultMutableTreeNode group = new DefaultMutableTreeNode(
                    new TestGroupNode(Set.of(html, scss, spec, ts))
            );
            group.add(new DefaultMutableTreeNode(html));
            group.add(new DefaultMutableTreeNode(scss));
            group.add(new DefaultMutableTreeNode(spec));
            group.add(new DefaultMutableTreeNode(ts));
            app.add(group);
            root.add(app);
            JTree tree = expanded(root, new TreePath(new Object[]{root, app}));
            if (expandGroup) {
                tree.expandPath(new TreePath(new Object[]{root, app, group}));
            }
            return tree;
        }

        private static JTree expanded(DefaultMutableTreeNode root, TreePath path) {
            JTree tree = new JTree(root);
            tree.expandPath(new TreePath(root));
            tree.expandPath(path);
            return tree;
        }
    }
}
