package de.sasbe.subtabs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JPanel;
import java.awt.Component;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * TESTE: active subtab and sidetab highlights must use the file group's color when group colors are on.
 */
public class SubtabGroupTabHighlightTest extends RealEditorWindowTestCase {
    private static final Color GROUP_COLOR = new JBColor(new Color(0x2563EB), new Color(0x3B82F6));

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setFamiliaEnabled(true);
        settings.setGroupColorsEnabled(true);
        settings.setSubtabsActive(true);
        settings.setSidetabsActive(true);
        settings.setSidetabsExpanded(true);
        settings.setSidetabLayoutMode(SidetabLayoutMode.BESIDE);
        settings.setSidetabsOnRight(true);
        settings.setRules(SubtabRulesDefaults.createDefaults());
        settings.setSidetabRules(SidetabRulesDefaults.createDefaults());
        ComponentSubtabsDocumentListener.install(getProject());
    }

    public void testUnselectedMainTabKeepsGroupColoredIcon() throws Exception {
        VirtualFile html = createSourceFile("product-list.component.html");
        WriteAction.run(() -> html.setBinaryContent("<div></div>".getBytes(StandardCharsets.UTF_8)));
        VirtualFile ts = createSourceFile("product-list.component.ts");
        assignGroupColor(html);

        openAndSettle(html);
        openAndSettle(ts);
        assertEquals(ts, selectedFile());
        ComponentSubtabMainTabColors.refresh(getProject());
        ComponentSubtabMainTabIcons.scheduleRefreshAfterPlatformUpdate(getProject());
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        TabInfo htmlTab = tabInfoOf(html);
        assertNotNull(htmlTab);
        Icon base = ComponentSubtabMainTabIcons.standardFileIcon(getProject(), html);
        assertNotNull(base);
        Icon expected = IconUtil.colorize(base, GROUP_COLOR);
        assertEquals(expected.getClass(), htmlTab.getIcon().getClass());

        manager.openFile(html, true);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        manager.openFile(ts, true);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        ComponentSubtabMainTabIcons.scheduleRefreshAfterPlatformUpdate(getProject());
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        htmlTab = tabInfoOf(html);
        assertNotNull(htmlTab);
        assertEquals(expected.getClass(), htmlTab.getIcon().getClass());
    }

    public void testMainTabFileIconUsesGroupColorWithoutReplacingShape() throws Exception {
        VirtualFile html = createSourceFile("product-list.component.html");
        WriteAction.run(() -> html.setBinaryContent("<div></div>".getBytes(StandardCharsets.UTF_8)));
        assignGroupColor(html);

        openAndSettle(html);

        TabInfo tabInfo = tabInfoOf(html);
        Icon base = ComponentSubtabMainTabIcons.standardFileIcon(getProject(), html);
        assertNotNull(base);
        Icon expected = IconUtil.colorize(base, GROUP_COLOR);
        assertEquals(expected.getClass(), tabInfo.getIcon().getClass());
        assertEquals(expected.getIconWidth(), tabInfo.getIcon().getIconWidth());
        assertEquals(expected.getIconHeight(), tabInfo.getIcon().getIconHeight());
    }

    public void testSelectedSubtabUsesGroupColorTint() throws Exception {
        VirtualFile html = createSourceFile("product-list.component.html");
        WriteAction.run(() -> html.setBinaryContent("<div></div>".getBytes(StandardCharsets.UTF_8)));
        createSourceFile("product-list.component.ts");
        assignGroupColor(html);

        openAndSettle(html);
        ComponentSubtabsManager.attachIfNeeded(getProject(), html);

        ComponentSubtabBarPanel panel = barFor(html);
        assertNotNull(panel);
        JToggleButton htmlButton = panel.buttonFor(html);
        assertNotNull(htmlButton);
        assertTrue(htmlButton.isSelected());

        Color expected = expectedTint(html);
        assertEquals(expected, htmlButton.getBackground());
    }

    public void testSelectedSidetabUsesGroupColorTint() throws Exception {
        VirtualFile html = createSourceFile("header.component.html");
        WriteAction.run(() -> html.setBinaryContent("""
                <html>
                <head><title>Hi</title></head>
                <body><p>Hello</p></body>
                </html>
                """.getBytes(StandardCharsets.UTF_8)));
        createSourceFile("header.component.ts");
        assignGroupColor(html);

        openAndSettle(html);
        SidetabsManager.attachIfNeeded(getProject(), html);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        SidetabBarPanel panel = sidetabPanelFor(html);
        assertNotNull(panel);
        JToggleButton headButton = buttonForSection(panel, "Head");
        assertNotNull(headButton);
        assertTrue(headButton.isSelected());

        Color expected = expectedTint(html);
        assertEquals(expected, headButton.getBackground());
    }

    public void testMainTabHoverPopupHighlightsCurrentFileWithGroupTint() throws Exception {
        VirtualFile effects = createSourceFile("checkout.effects.ts");
        WriteAction.run(() -> effects.setBinaryContent("export {}".getBytes(StandardCharsets.UTF_8)));
        VirtualFile reducer = createSourceFile("checkout.reducer.ts");
        VirtualFile state = createSourceFile("checkout.state.ts");
        assignGroupColor(effects);

        SubtabGroupPopupPresentation.Context context =
                SubtabGroupPopupPresentation.forMainTab(getProject(), effects);
        SubtabGroupFilePopupPanel panel = new SubtabGroupFilePopupPanel(
                getProject(),
                List.of(effects, reducer, state),
                240,
                context,
                file -> {},
                () -> {}
        );
        ComponentSubtabModifiedLabel highlighted = popupLabelFor(panel, effects);
        ComponentSubtabModifiedLabel other = popupLabelFor(panel, reducer);
        assertNotNull(highlighted);
        assertNotNull(other);

        Color expected = expectedTint(effects);
        assertEquals(expected, highlighted.getBackground());
        assertEquals(UIUtil.getPanelBackground(), other.getBackground());
    }

    public void testDirectoryModeOnlyHighlightsMainTabFileWithGroupTint() throws Exception {
        SubtabsSettings.getInstance().setGroupRelatedFilesInProjectView(false);

        VirtualFile effects = createSourceFile("checkout.effects.ts");
        WriteAction.run(() -> effects.setBinaryContent("export {}".getBytes(StandardCharsets.UTF_8)));
        VirtualFile reducer = createSourceFile("checkout.reducer.ts");
        VirtualFile state = createSourceFile("checkout.state.ts");
        assignGroupColor(effects);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(sourceDir);
        for (VirtualFile file : new VirtualFile[]{effects, reducer, state}) {
            root.add(new DefaultMutableTreeNode(file));
        }
        JTree tree = new JTree(root);

        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(effects);
        assertNotNull(match);
        Set<Integer> rows = ComponentSubtabProjectViewHover.collectMainTabHoverRows(tree, match, effects);
        assertEquals(3, rows.size());

        JPanel tabLabel = new JPanel();
        ComponentSubtabProjectViewHover.activateMainTabHoverForTest(tabLabel, tree, rows, effects);
        assertEquals(effects, ComponentSubtabProjectViewHover.primaryHoverFile(tree));
    }

    public void testGroupedProjectViewMarksPrimaryFileForGroupTint() throws Exception {
        SubtabsSettings.getInstance().setGroupRelatedFilesInProjectView(true);

        VirtualFile effects = createSourceFile("checkout.effects.ts");
        WriteAction.run(() -> effects.setBinaryContent("export {}".getBytes(StandardCharsets.UTF_8)));
        VirtualFile reducer = createSourceFile("checkout.reducer.ts");
        VirtualFile state = createSourceFile("checkout.state.ts");
        assignGroupColor(effects);

        JTree tree = groupedTree(sourceDir, effects, reducer, state);
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(effects);
        assertNotNull(match);

        Set<Integer> rows = ComponentSubtabProjectViewHover.collectMainTabHoverRows(tree, match, effects);
        JPanel tabLabel = new JPanel();
        ComponentSubtabProjectViewHover.activateMainTabHoverForTest(tabLabel, tree, rows, effects);

        assertEquals(effects, ComponentSubtabProjectViewHover.primaryHoverFile(tree));
        assertTrue(rows.contains(rowForFile(tree, effects)));
        assertFalse(rows.contains(rowForFile(tree, reducer)));
    }

    public void testMainTabHoverSyncUsesGroupColorTint() throws Exception {
        SubtabsSettings.getInstance().setHoverViewEnabled(true);
        VirtualFile html = createSourceFile("product-list.component.html");
        WriteAction.run(() -> html.setBinaryContent("<div></div>".getBytes(StandardCharsets.UTF_8)));
        createSourceFile("product-list.component.ts");
        assignGroupColor(html);

        openAndSettle(html);
        ComponentSubtabsManager.attachIfNeeded(getProject(), html);
        ComponentSubtabMainTabSelectPopup.installOn(getProject());

        JToggleButton htmlButton = barFor(html).buttonFor(html);
        assertNotNull(htmlButton);
        ComponentSubtabBarHover.onEnterMainTab(getProject(), html, htmlButton);

        Color expected = expectedTint(html);
        assertTrue(ComponentSubtabUi.isMainTabSyncHighlight(htmlButton));
        assertEquals(expected, htmlButton.getBackground());
    }

    private void assignGroupColor(@NotNull VirtualFile file) {
        String key = SubtabGroupColors.colorKey(file);
        assertNotNull(key);
        SubtabGroupColors.setColor(key, GROUP_COLOR);
        ComponentSubtabMainTabColors.refresh(getProject());
    }

    private static @NotNull Color expectedTint(@NotNull VirtualFile file) {
        Color groupColor = SubtabGroupColors.colorForFile(file);
        assertNotNull(groupColor);
        return ComponentSubtabMainTabColors.blend(
                groupColor,
                UIUtil.getPanelBackground(),
                ComponentSubtabMainTabColors.FOCUSED_SELECTED_MIX
        );
    }

    private ComponentSubtabBarPanel barFor(@NotNull VirtualFile file) {
        for (FileEditor editor : FileEditorManager.getInstance(getProject()).getAllEditors()) {
            if (!file.equals(editor.getFile())) {
                continue;
            }
            ComponentSubtabBarPanel panel = editor.getUserData(ComponentSubtabsManager.SUBTAB_BAR_KEY);
            if (panel != null) {
                return panel;
            }
        }
        return null;
    }

    private SidetabBarPanel sidetabPanelFor(@NotNull VirtualFile file) {
        for (FileEditor editor : FileEditorManager.getInstance(getProject()).getAllEditors()) {
            if (!file.equals(editor.getFile())) {
                continue;
            }
            SidetabBarPanel panel = editor.getUserData(SidetabsManager.SIDETAB_BAR_KEY);
            if (panel != null) {
                return panel;
            }
        }
        return null;
    }

    private static @NotNull JTree groupedTree(
            @NotNull VirtualFile rootDir,
            @NotNull VirtualFile... files
    ) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootDir);
        TestGroupNode groupNode = new TestGroupNode(Set.of(files));
        DefaultMutableTreeNode groupTreeNode = new DefaultMutableTreeNode(groupNode);
        root.add(groupTreeNode);
        for (VirtualFile file : files) {
            groupTreeNode.add(new DefaultMutableTreeNode(file));
        }
        JTree tree = new JTree(root);
        tree.expandPath(new TreePath(new Object[]{root, groupTreeNode}));
        return tree;
    }

    private static int rowForFile(@NotNull JTree tree, @NotNull VirtualFile file) {
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath path = tree.getPathForRow(row);
            if (path != null && file.equals(ComponentSubtabProjectViewHover.virtualFileOf(path))) {
                return row;
            }
        }
        return -1;
    }

    private static ComponentSubtabModifiedLabel popupLabelFor(
            @NotNull SubtabGroupFilePopupPanel panel,
            @NotNull VirtualFile file
    ) {
        for (Component component : panel.getComponents()) {
            if (component instanceof ComponentSubtabModifiedLabel label
                    && file.equals(label.getClientProperty("componentSubtabs.popupFile"))) {
                return label;
            }
        }
        return null;
    }

    private record TestGroupNode(@NotNull Set<VirtualFile> members)
            implements ComponentSubtabProjectViewHover.GroupNode {
        @Override
        public boolean contains(@NotNull VirtualFile file) {
            return members.contains(file);
        }
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
