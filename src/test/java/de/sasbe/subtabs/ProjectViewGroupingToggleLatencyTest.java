package de.sasbe.subtabs;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.ide.projectView.impl.ProjectViewImpl;
import com.intellij.ide.projectView.impl.ProjectViewPane;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTree;
import javax.swing.tree.TreePath;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

/**
 * Clicks the real project-view grouping button on a real {@link ProjectViewPane} (async tree model plus
 * {@link SubtabGroupTreeStructureProvider}) built from a copy of the demo project including its excluded
 * {@code node_modules}, and measures until the tree shows the new grouping and the spinner is gone.
 */
public class ProjectViewGroupingToggleLatencyTest extends HeavyPlatformTestCase {
    private static final long TOGGLE_BUDGET_MS = 2_000L;
    private static final int TOOL_WINDOW_EVENTS = 12;
    private static final Path FIXTURE = Path.of(System.getProperty("user.dir"), "build", "grouping-latency-fixture");

    private JTree tree;
    private int groupedRowCount;
    private final AtomicInteger nodeModulesChildComputations = new AtomicInteger();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setFamiliaEnabled(true);
        settings.setProjectViewGroupingEnabled(true);
        settings.setProjectViewGroupingActive(true);

        VirtualFile root = demoProjectCopy();
        PsiTestUtil.addContentRoot(getModule(), root);
        VirtualFile nodeModules = root.findChild("node_modules");
        assertNotNull("demo node_modules copy missing", nodeModules);
        PsiTestUtil.addExcludedRoot(getModule(), nodeModules);
        String nodeModulesPath = nodeModules.getPath();
        TreeStructureProvider.EP.getPoint(getProject()).registerExtension(
                new ChildComputationCounter(nodeModulesPath, nodeModulesChildComputations),
                getTestRootDisposable()
        );

        ProjectViewImpl projectView = (ProjectViewImpl) ProjectView.getInstance(getProject());
        projectView.setupImpl(new ToolWindowHeadlessManagerImpl.MockToolWindow(getProject()), true);
        projectView.changeView(ProjectViewPane.ID);
        AbstractProjectViewPane pane = projectView.getCurrentProjectViewPane();
        assertNotNull("project view pane must exist", pane);
        tree = pane.getTree();
        assertNotNull(tree);

        SubtabGroupTreeControl.installOn(getProject());
        expandSourceFolders();
        waitUntil("initial grouped tree",
                () -> treeShowsGroups() && autoExpandGroupsExpanded() && !groupingBusy(), 15_000);
        PlatformTestUtil.waitWhileBusy(tree);
        groupedRowCount = tree.getRowCount();
        nodeModulesChildComputations.set(0);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            SubtabsProjectViewGroupingBusyState.getInstance(getProject()).reset();
            SubtabsSettings.getInstance().setProjectViewGroupingEnabled(true);
            SubtabsSettings.getInstance().setProjectViewGroupingActive(true);
            tree = null;
        } finally {
            super.tearDown();
        }
    }

    public void testUngroupClickFinishesWithinTwoSeconds() {
        long elapsedMs = clickGroupingButtonAndAwait(false);
        assertTrue("ungrouping took " + elapsedMs + "ms (budget " + TOGGLE_BUDGET_MS + "ms)",
                elapsedMs < TOGGLE_BUDGET_MS);
    }

    public void testRegroupClickFinishesWithinTwoSeconds() {
        clickGroupingButtonAndAwait(false);
        long elapsedMs = clickGroupingButtonAndAwait(true);
        assertTrue("regrouping took " + elapsedMs + "ms (budget " + TOGGLE_BUDGET_MS + "ms)",
                elapsedMs < TOGGLE_BUDGET_MS);
    }

    /** In the IDE every tool-window focus/resize fires {@code stateChanged}, which reaches {@link SubtabGroupTreeControlListener}. */
    public void testToggleAfterToolWindowActivityFinishesWithinTwoSeconds() {
        simulateToolWindowStateChanges();
        long ungroupMs = clickGroupingButtonAndAwait(false);
        simulateToolWindowStateChanges();
        long regroupMs = clickGroupingButtonAndAwait(true);
        assertTrue("ungrouping after tool-window activity took " + ungroupMs + "ms", ungroupMs < TOGGLE_BUDGET_MS);
        assertTrue("regrouping after tool-window activity took " + regroupMs + "ms", regroupMs < TOGGLE_BUDGET_MS);
    }

    public void testToolWindowActivityDoesNotLoadCollapsedNodeModules() {
        simulateToolWindowStateChanges();
        System.out.println("[grouping-toggle] node_modules child computations after tool-window activity: "
                + nodeModulesChildComputations.get());
        assertEquals("tool-window events must not load the collapsed node_modules folder",
                0, nodeModulesChildComputations.get());
    }

    public void testToggleDoesNotLoadCollapsedNodeModules() {
        clickGroupingButtonAndAwait(false);
        clickGroupingButtonAndAwait(true);
        System.out.println("[grouping-toggle] node_modules child computations after two toggles: "
                + nodeModulesChildComputations.get());
        assertEquals("toggling must not load the collapsed node_modules folder",
                0, nodeModulesChildComputations.get());
    }

    private long clickGroupingButtonAndAwait(boolean expectGrouped) {
        ComponentSubtabIconButton button = SubtabsProjectViewGroupingOverlay.installedGroupingButtonForTree(tree);
        assertNotNull("grouping button must be installed on the project tree", button);
        long start = System.nanoTime();
        button.doClick();
        long treeDoneMs = waitUntil(
                "tree " + (expectGrouped ? "grouped" : "ungrouped"),
                () -> treeShowsGroups() == expectGrouped,
                20_000
        );
        if (expectGrouped) {
            waitUntil("auto-expanding groups expanded", this::autoExpandGroupsExpanded, 20_000);
            waitUntil("grouped tree back to " + groupedRowCount + " rows",
                    () -> tree.getRowCount() == groupedRowCount, 20_000);
        }
        waitUntil("grouping spinner finished", () -> !groupingBusy(), 20_000);
        long totalMs = (System.nanoTime() - start) / 1_000_000L;
        System.out.println("[grouping-toggle] expectGrouped=" + expectGrouped
                + " treeUpdatedAfter=" + treeDoneMs + "ms spinnerGoneAfter=" + totalMs
                + "ms rows=" + tree.getRowCount() + "/" + groupedRowCount
                + " nodeModulesComputations=" + nodeModulesChildComputations.get());
        return totalMs;
    }

    private void simulateToolWindowStateChanges() {
        SubtabGroupTreeControlListener listener = new SubtabGroupTreeControlListener(getProject());
        for (int index = 0; index < TOOL_WINDOW_EVENTS; index++) {
            listener.stateChanged(ToolWindowManager.getInstance(getProject()));
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            PlatformTestUtil.waitWhileBusy(tree);
        }
    }

    private void expandSourceFolders() {
        for (int pass = 0; pass < 20; pass++) {
            PlatformTestUtil.waitWhileBusy(tree);
            boolean expandedAny = false;
            for (int row = 0; row < tree.getRowCount(); row++) {
                TreePath path = tree.getPathForRow(row);
                if (path == null || tree.isExpanded(path) || tree.getModel().isLeaf(path.getLastPathComponent())) {
                    continue;
                }
                if (isNodeModules(path) || !(TreeUtil.getLastUserObject(path) instanceof PsiDirectoryNode)) {
                    continue;
                }
                tree.expandPath(path);
                expandedAny = true;
            }
            if (!expandedAny) {
                break;
            }
        }
        PlatformTestUtil.waitWhileBusy(tree);
    }

    private static boolean isNodeModules(TreePath path) {
        Object userObject = TreeUtil.getLastUserObject(path);
        return userObject instanceof PsiDirectoryNode directoryNode
                && directoryNode.getVirtualFile() != null
                && "node_modules".equals(directoryNode.getVirtualFile().getName());
    }

    private boolean autoExpandGroupsExpanded() {
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath path = tree.getPathForRow(row);
            if (SubtabGroupTreeControl.isAutoExpandedGroupPath(path) && !tree.isExpanded(path)) {
                return false;
            }
        }
        return true;
    }

    private boolean groupingBusy() {
        ComponentSubtabIconButton button = SubtabsProjectViewGroupingOverlay.installedGroupingButtonForTree(tree);
        return SubtabsProjectViewGroupingBusyState.getInstance(getProject()).isBusy()
                || (button != null && button.isLoading());
    }

    private boolean treeShowsGroups() {
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath path = tree.getPathForRow(row);
            if (path != null && TreeUtil.getLastUserObject(path) instanceof SubtabGroupProjectViewNode) {
                return true;
            }
        }
        return false;
    }

    private static long waitUntil(String what, BooleanSupplier condition, long timeoutMs) {
        long start = System.nanoTime();
        long deadline = start + timeoutMs * 1_000_000L;
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                fail("timed out after " + timeoutMs + "ms waiting for " + what);
            }
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            try {
                Thread.sleep(5);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                fail("interrupted");
            }
        }
        return (System.nanoTime() - start) / 1_000_000L;
    }

    private static VirtualFile demoProjectCopy() throws IOException {
        Path demo = Path.of(System.getProperty("user.dir"), "demo-project");
        assertTrue("demo project missing at " + demo, Files.isDirectory(demo));
        if (!Files.isDirectory(FIXTURE.resolve("node_modules"))) {
            for (String part : new String[]{"src", "sidetabs-examples", "node_modules"}) {
                copyTree(demo.resolve(part), FIXTURE.resolve(part));
            }
        }
        VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(FIXTURE);
        assertNotNull(root);
        VfsUtil.markDirtyAndRefresh(false, true, true, root);
        return root;
    }

    private static void copyTree(Path source, Path target) throws IOException {
        if (!Files.isDirectory(source)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                Path destination = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static final class ChildComputationCounter implements TreeStructureProvider {
        private final String nodeModulesPath;
        private final AtomicInteger counter;

        private ChildComputationCounter(String nodeModulesPath, AtomicInteger counter) {
            this.nodeModulesPath = nodeModulesPath;
            this.counter = counter;
        }

        @Override
        public @NotNull Collection<AbstractTreeNode<?>> modify(
                @NotNull AbstractTreeNode<?> parent,
                @NotNull Collection<AbstractTreeNode<?>> children,
                ViewSettings settings
        ) {
            if (parent.getValue() instanceof PsiDirectory directory
                    && directory.getVirtualFile().getPath().startsWith(nodeModulesPath)) {
                counter.incrementAndGet();
            }
            return children;
        }
    }
}
