package com.zayax.tabz;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Verifies collapse icons, SideTabs, and TabZ stay aligned through real editor attach and toggles.
 */
public class CollapseIconLayoutIntegrationTest extends RealEditorWindowTestCase {
    private static final int TOLERANCE = 2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TabzSettings settings = TabzSettings.getInstance();
        settings.setSubtabsActive(true);
        settings.setSidetabsActive(true);
        settings.setSidetabsExpanded(true);
        settings.setSidetabLayoutMode(SidetabLayoutMode.BESIDE);
        settings.setSidetabsOnRight(true);
        settings.setShowCollapseButton(true);
        settings.setSidetabRules(SidetabRulesDefaults.createDefaults());
        ComponentSubtabDocumentListener.install(getProject());
    }

    public void testThreeIconRowAlignsAfterStartupWithCollapsedSidetabs() throws Exception {
        TabzSettings.getInstance().setRules(twoOverlappingStateRules());
        TabzSettings.getInstance().setSidetabsExpanded(false);

        VirtualFile dir = WriteAction.computeAndWait(() -> sourceDir.createChildDirectory(this, "startup-icons"));
        VirtualFile actions = WriteAction.computeAndWait(() -> dir.createChildData(this, "cart.actions.ts"));
        WriteAction.run(() -> actions.setBinaryContent("export const x = 1;".getBytes(StandardCharsets.UTF_8)));
        VirtualFile reducer = WriteAction.computeAndWait(() -> dir.createChildData(this, "cart.reducer.ts"));
        WriteAction.run(() -> reducer.setBinaryContent("export const y = 1;".getBytes(StandardCharsets.UTF_8)));

        openAndSettle(actions);
        ComponentSubtabFileEditorListener.attachToAlreadyOpenFiles(getProject());
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        FileEditor editor = selectedEditor(actions);
        assertTrue("rule switch must use the overlay row on startup", RuleSwitchOverlay.isInstalled(editor));
        assertTrue("SideTabs toggle must be installed on startup", SidetabsToggleOverlay.isInstalled(editor));
        assertTrue("TabZ collapse must be installed on startup", SubtabsCollapseOverlay.isInstalled(editor));

        EditorLayoutMirror mirror = EditorLayoutMirror.forEditor(editor);
        assertThreeIconRowAligned(editor, mirror);
    }

    public void testTabzIconReturnsToOverlayRowAfterBothCollapsedThenTabzExpanded() throws Exception {
        VirtualFile html = createSourceFile("header.component.html");
        WriteAction.run(() -> html.setBinaryContent("""
                <header>Title</header>
                <main>Content</main>
                """.getBytes(StandardCharsets.UTF_8)));
        createSourceFile("header.component.ts");
        openAndSettle(html);
        SidetabsManager.attachIfNeeded(getProject(), html);
        ComponentSubtabsManager.attachIfNeeded(getProject(), html);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        FileEditor editor = selectedEditor(html);
        assertNotNull(editor.getUserData(ComponentSubtabsManager.TABZ_BAR_KEY));
        EditorLayoutMirror mirror = EditorLayoutMirror.forEditor(editor);
        LayoutSnapshot baseline = captureLayout(editor, mirror);
        baseline.assertTabzIconLeftOfSidetabsIcon();
        baseline.assertSidetabsBelowIcons();

        SubtabsCollapseState.getInstance(getProject()).setCollapsed(getProject(), true);
        SidetabsCollapseState.getInstance(getProject()).setCollapsed(getProject(), true);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        SubtabsCollapseState.getInstance(getProject()).setCollapsed(getProject(), false);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        editor = selectedEditor(html);
        mirror.refresh(editor);
        assertNull("TabZ collapse must not return to the bar after re-expanding",
                ComponentSubtabsManager.visibleCollapseButton(editor));
        assertTrue("TabZ collapse must stay in the top-right overlay row",
                SubtabsCollapseOverlay.isInstalled(editor));
        assertTrue("SideTabs toggle must stay in the overlay row",
                SidetabsToggleOverlay.isInstalled(editor));

        LayoutSnapshot restored = captureLayout(editor, mirror);
        restored.assertTabzIconLeftOfSidetabsIcon();
        restored.assertSidetabsBelowIcons();
        assertNear("TabZ icon X after restore", baseline.tabzIcon.x, restored.tabzIcon.x);
        assertNear("TabZ icon Y after restore", baseline.tabzIcon.y, restored.tabzIcon.y);
        assertNear("SideTabs icon X after restore", baseline.sidetabsIcon.x, restored.sidetabsIcon.x);
        assertNear("SideTabs icon Y after restore", baseline.sidetabsIcon.y, restored.sidetabsIcon.y);
    }

    public void testCollapseIconsAndSidetabsStayAlignedThroughToggleSequences() throws Exception {
        VirtualFile html = createSourceFile("catalog-page.html");
        WriteAction.run(() -> html.setBinaryContent("""
                <!DOCTYPE html>
                <html><head><title>Catalog</title></head>
                <body><main><p>Hello</p></main></body></html>
                """.getBytes(StandardCharsets.UTF_8)));
        createSourceFile("catalog-page.ts");
        openAndSettle(html);
        SidetabsManager.attachIfNeeded(getProject(), html);
        ComponentSubtabsManager.attachIfNeeded(getProject(), html);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        FileEditor editor = selectedEditor(html);
        EditorLayoutMirror mirror = EditorLayoutMirror.forEditor(editor);
        LayoutSnapshot baseline = captureLayout(editor, mirror);
        baseline.assertSidetabsBelowIcons();
        baseline.assertTabzIconLeftOfSidetabsIcon();
        baseline.assertPanelReserveMatchesLayout();

        List<Runnable> toggles = List.of(
                () -> SubtabsCollapseState.getInstance(getProject()).toggle(getProject()),
                () -> SidetabsCollapseState.getInstance(getProject()).toggle(getProject())
        );
        runTogglePermutations(editor, mirror, baseline, toggles);
    }

    public void testSingleIconPositionsMatchDualLayoutSlots() throws Exception {
        VirtualFile html = createSourceFile("header.component.html");
        WriteAction.run(() -> html.setBinaryContent("""
                <header>Title</header>
                <main>Content</main>
                """.getBytes(StandardCharsets.UTF_8)));
        createSourceFile("header.component.ts");
        openAndSettle(html);

        TabzSettings settings = TabzSettings.getInstance();
        settings.setSubtabsActive(true);
        settings.setSidetabsActive(true);
        settings.setSidetabsExpanded(true);
        SidetabsManager.attachIfNeeded(getProject(), html);
        ComponentSubtabsManager.attachIfNeeded(getProject(), html);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        FileEditor editor = selectedEditor(html);
        EditorLayoutMirror mirror = EditorLayoutMirror.forEditor(editor);
        LayoutSnapshot dual = captureLayout(editor, mirror);

        settings.setSidetabsActive(false);
        ComponentSubtabsManager.applySettingsChange(getProject());
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        mirror.refresh(editor);
        assertNotNull(editor.getUserData(ComponentSubtabsManager.TABZ_BAR_KEY));
        LayoutSnapshot tabzOnly = captureLayout(editor, mirror);
        assertNull("TabZ-only must not keep collapse button on the bar",
                ComponentSubtabsManager.visibleCollapseButton(editor));
        assertTrue("TabZ-only must use the top-right collapse overlay",
                SubtabsCollapseOverlay.isInstalled(editor));
        assertNear("TabZ-only X", dual.tabzIcon.x, tabzOnly.tabzIcon.x);
        assertNear("TabZ-only Y", dual.tabzIcon.y, tabzOnly.tabzIcon.y);

        settings.setSidetabsActive(true);
        settings.setSubtabsActive(false);
        settings.setSidetabsExpanded(true);
        ComponentSubtabsManager.applyPresentationState(getProject());
        SidetabsManager.applyPresentationState(getProject());
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        mirror.refresh(editor);
        LayoutSnapshot sidetabsOnly = captureLayout(editor, mirror);
        assertNear("SideTabs-only X", dual.sidetabsIcon.x, sidetabsOnly.sidetabsIcon.x);
        assertNear("SideTabs-only Y", dual.sidetabsIcon.y, sidetabsOnly.sidetabsIcon.y);
    }

    public void testProductListHtmlTabzOnlyUsesOverlaySlotWhenSidetabsGloballyCollapsed() throws Exception {
        VirtualFile html = createSourceFile("product-list.component.html");
        WriteAction.run(() -> html.setBinaryContent("""
                <ul class="product-list">
                  <li>Product A</li>
                  <li>Product B</li>
                </ul>
                """.getBytes(StandardCharsets.UTF_8)));
        createSourceFile("product-list.component.ts");
        createSourceFile("product-list.component.scss");

        VirtualFile dualHtml = createSourceFile("header.component.html");
        WriteAction.run(() -> dualHtml.setBinaryContent("""
                <header>Title</header>
                <main>Content</main>
                """.getBytes(StandardCharsets.UTF_8)));
        createSourceFile("header.component.ts");
        openAndSettle(dualHtml);

        TabzSettings settings = TabzSettings.getInstance();
        settings.setSubtabsActive(true);
        settings.setSidetabsActive(true);
        settings.setSidetabsExpanded(true);
        SidetabsManager.attachIfNeeded(getProject(), dualHtml);
        ComponentSubtabsManager.attachIfNeeded(getProject(), dualHtml);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        FileEditor dualEditor = selectedEditor(dualHtml);
        EditorLayoutMirror mirror = EditorLayoutMirror.forEditor(dualEditor);
        LayoutSnapshot dual = captureLayout(dualEditor, mirror);

        openAndSettle(html);
        settings.setSidetabsExpanded(false);
        SidetabsManager.attachIfNeeded(getProject(), html);
        ComponentSubtabsManager.attachIfNeeded(getProject(), html);
        ComponentSubtabsManager.applyPresentationState(getProject());
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        FileEditor editor = selectedEditor(html);
        mirror.refresh(editor);
        assertFalse("product-list must not show a SideTabs icon", SidetabsToggleOverlay.isInstalled(editor));
        assertNull("TabZ collapse must not stay on the bar",
                ComponentSubtabsManager.visibleCollapseButton(editor));
        assertTrue("TabZ-only product-list must use the top-right collapse overlay",
                SubtabsCollapseOverlay.isInstalled(editor));

        LayoutSnapshot tabzOnly = captureLayout(editor, mirror);
        assertNear("product-list TabZ X", dual.tabzIcon.x, tabzOnly.tabzIcon.x);
        assertNear("product-list TabZ Y", dual.tabzIcon.y, tabzOnly.tabzIcon.y);
    }

    public void testCollapsedTabzOnlyUsesExpandOverlaySlot() throws Exception {
        VirtualFile html = createSourceFile("header.component.html");
        WriteAction.run(() -> html.setBinaryContent("""
                <header>Title</header>
                """.getBytes(StandardCharsets.UTF_8)));
        createSourceFile("header.component.ts");
        openAndSettle(html);

        TabzSettings settings = TabzSettings.getInstance();
        settings.setSubtabsActive(true);
        settings.setSidetabsActive(true);
        settings.setSidetabsExpanded(true);
        SidetabsManager.attachIfNeeded(getProject(), html);
        ComponentSubtabsManager.attachIfNeeded(getProject(), html);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        FileEditor editor = selectedEditor(html);
        EditorLayoutMirror mirror = EditorLayoutMirror.forEditor(editor);
        LayoutSnapshot dual = captureLayout(editor, mirror);

        settings.setSidetabsActive(false);
        SubtabsCollapseState.getInstance(getProject()).toggle(getProject());
        ComponentSubtabsManager.attachIfNeeded(getProject(), html);
        ComponentSubtabsManager.applyPresentationState(getProject());
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        mirror.refresh(editor);

        LayoutSnapshot collapsedTabzOnly = captureLayout(editor, mirror);
        assertTrue("Collapsed TabZ-only must use the expand overlay",
                SubtabsExpandOverlay.isInstalled(editor));
        assertNear("Collapsed TabZ-only X", dual.tabzIcon.x, collapsedTabzOnly.tabzIcon.x);
        assertNear("Collapsed TabZ-only Y", dual.tabzIcon.y, collapsedTabzOnly.tabzIcon.y);
    }

    public void testOverlaySidetabsStayBelowIconsThroughToggleSequences() throws Exception {
        TabzSettings.getInstance().setSidetabLayoutMode(SidetabLayoutMode.OVERLAY);
        VirtualFile file = createSourceFile("page.html");
        WriteAction.run(() -> file.setBinaryContent("""
                <html><head><title>Hi</title></head><body><p>Hello</p></body></html>
                """.getBytes(StandardCharsets.UTF_8)));
        openAndSettle(file);
        SidetabsManager.attachIfNeeded(getProject(), file);
        ComponentSubtabsManager.attachIfNeeded(getProject(), file);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        FileEditor editor = selectedEditor(file);
        EditorLayoutMirror mirror = EditorLayoutMirror.forEditor(editor);
        LayoutSnapshot baseline = captureLayout(editor, mirror);
        baseline.assertSidetabsBelowIcons();

        List<Runnable> toggles = List.of(
                () -> SubtabsCollapseState.getInstance(getProject()).toggle(getProject()),
                () -> SidetabsCollapseState.getInstance(getProject()).toggle(getProject())
        );
        runTogglePermutations(editor, mirror, baseline, toggles);
    }

    private void runTogglePermutations(
            FileEditor editor,
            EditorLayoutMirror mirror,
            LayoutSnapshot baseline,
            List<Runnable> toggles
    ) throws Exception {
        List<List<Integer>> sequences = new ArrayList<>();
        sequences.add(List.of());
        for (int length = 1; length <= toggles.size(); length++) {
            permutations(toggles.size(), length, sequences);
        }

        for (List<Integer> sequence : sequences) {
            resetBarsExpanded();
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            mirror.refresh(editor);
            LayoutSnapshot reset = captureLayout(editor, mirror);
            reset.assertSidetabsBelowIcons();
            reset.assertPanelReserveMatchesLayout();
            assertIconsStable(baseline, reset, "after reset");

            for (int toggleIndex : sequence) {
                toggles.get(toggleIndex).run();
                PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
                mirror.refresh(editor);
                LayoutSnapshot current = captureLayout(editor, mirror);
                current.assertSidetabsBelowIcons();
                current.assertPanelReserveMatchesLayout();
                assertIconsStable(baseline, current, "after toggle sequence " + sequence);
                if (TabzSettings.getInstance().isSubtabsActive()
                        && TabzSettings.getInstance().isSidetabsExpanded()) {
                    current.assertTabzIconLeftOfSidetabsIcon();
                }
            }
        }
    }

    private static void permutations(int toggleCount, int length, List<List<Integer>> out) {
        buildSequences(toggleCount, length, new ArrayList<>(), out);
    }

    private static void buildSequences(
            int toggleCount,
            int length,
            List<Integer> current,
            List<List<Integer>> out
    ) {
        if (current.size() == length) {
            out.add(List.copyOf(current));
            return;
        }
        for (int index = 0; index < toggleCount; index++) {
            current.add(index);
            buildSequences(toggleCount, length, current, out);
            current.remove(current.size() - 1);
        }
    }

    private void resetBarsExpanded() {
        TabzSettings settings = TabzSettings.getInstance();
        if (!settings.isSubtabsActive()) {
            SubtabsCollapseState.getInstance(getProject()).toggle(getProject());
        }
        if (!settings.isSidetabsExpanded()) {
            SidetabsCollapseState.getInstance(getProject()).toggle(getProject());
        }
    }

    private static void assertIconsStable(LayoutSnapshot baseline, LayoutSnapshot current, String context) {
        assertNear(context + ": SideTabs icon X", baseline.sidetabsIcon.x, current.sidetabsIcon.x);
        assertNear(context + ": SideTabs icon Y", baseline.sidetabsIcon.y, current.sidetabsIcon.y);
        if (baseline.tabzIcon != null && current.tabzIcon != null) {
            assertNear(context + ": TabZ icon X", baseline.tabzIcon.x, current.tabzIcon.x);
            assertNear(context + ": TabZ icon Y", baseline.tabzIcon.y, current.tabzIcon.y);
        }
    }

    private static void assertNear(String message, int expected, int actual) {
        assertTrue(
                message + " (expected=" + expected + ", actual=" + actual + ", tolerance=" + TOLERANCE + ")",
                Math.abs(expected - actual) <= TOLERANCE
        );
    }

    private static void assertThreeIconRowAligned(FileEditor editor, EditorLayoutMirror mirror) {
        Dimension iconSize = new Dimension(SidetabIconLayout.iconSize(), SidetabIconLayout.iconSize());
        Rectangle sidetabsIcon = SidetabIconLayout.layoutSidetabsIcon(
                editor,
                mirror.layoutComponent(),
                mirror.layeredPane(),
                iconSize
        );
        Rectangle tabzIcon = SidetabIconLayout.layoutTabzIcon(
                editor,
                mirror.layoutComponent(),
                mirror.layeredPane(),
                iconSize
        );
        Rectangle ruleSwitchIcon = SidetabIconLayout.layoutRuleSwitchIcon(
                editor,
                mirror.layoutComponent(),
                mirror.layeredPane(),
                iconSize
        );

        int gap = SidetabIconLayout.iconGap();
        assertNear("rule switch must sit left of TabZ icon", ruleSwitchIcon.x + ruleSwitchIcon.width + gap, tabzIcon.x);
        assertNear("TabZ icon must sit left of SideTabs icon", tabzIcon.x + tabzIcon.width + gap, sidetabsIcon.x);
        assertNear("rule switch must share the icon row", ruleSwitchIcon.y, tabzIcon.y);
        assertNear("SideTabs icon must share the icon row", tabzIcon.y, sidetabsIcon.y);
    }

    private static List<CustomSubtabRule> twoOverlappingStateRules() {
        CustomSubtabRule central = SubtabRulesDefaults.createDefaults().stream()
                .filter(rule -> "State Central".equals(rule.name))
                .findFirst()
                .orElseThrow();
        return new ArrayList<>(List.of(central, SubtabRulesDefaults.stateFeatureRule()));
    }

    private static LayoutSnapshot captureLayout(FileEditor editor, EditorLayoutMirror mirror) {
        Dimension iconSize = new Dimension(SidetabIconLayout.iconSize(), SidetabIconLayout.iconSize());
        Rectangle sidetabsIcon = SidetabIconLayout.layoutSidetabsIcon(
                editor,
                mirror.layoutComponent(),
                mirror.layeredPane(),
                iconSize
        );
        Rectangle tabzIcon = null;
        if (TabzSettings.getInstance().isShowCollapseButton()) {
            tabzIcon = SidetabIconLayout.layoutTabzIcon(
                    editor,
                    mirror.layoutComponent(),
                    mirror.layeredPane(),
                    iconSize
            );
        }
        int sidetabContentTopY = SidetabIconLayout.sidetabContentTopY(
                editor,
                mirror.layoutComponent(),
                mirror.layeredPane()
        );
        int topIconReserve = 0;
        SidetabBarPanel panel = editor.getUserData(SidetabsManager.SIDETAB_BAR_KEY);
        if (panel != null && !panel.sections().isEmpty()) {
            topIconReserve = panel.topIconReserve();
        }
        return new LayoutSnapshot(
                sidetabsIcon,
                tabzIcon,
                sidetabContentTopY,
                topIconReserve
        );
    }

    private FileEditor selectedEditor(VirtualFile file) {
        FileEditor[] editors = ComponentSubtabsManager.editorsFor(manager, file);
        assertTrue(editors.length > 0);
        return editors[0];
    }

    private static final class EditorLayoutMirror {
        private final JLayeredPane layeredPane;
        private final SidetabEditorHost host;
        private final JComponent layoutComponent;

        private EditorLayoutMirror(
                JLayeredPane layeredPane,
                SidetabEditorHost host,
                JComponent layoutComponent
        ) {
            this.layeredPane = layeredPane;
            this.host = host;
            this.layoutComponent = layoutComponent;
        }

        static EditorLayoutMirror forEditor(FileEditor editor) {
            return build(editor, 800, 600, 80);
        }

        void refresh(FileEditor editor) {
            int width = Math.max(800, host.getWidth());
            int height = Math.max(600, host.getHeight());
            int sidetabColumnWidth = sidetabColumnWidth(editor, width);
            applySize(width, height, sidetabColumnWidth);
        }

        JLayeredPane layeredPane() {
            return layeredPane;
        }

        JComponent layoutComponent() {
            return layoutComponent;
        }

        private static EditorLayoutMirror build(
                FileEditor editor,
                int hostWidth,
                int hostHeight,
                int sidetabColumnWidth
        ) {
            JLayeredPane layeredPane = new JLayeredPane();
            layeredPane.setBounds(0, 0, hostWidth, hostHeight);
            SidetabEditorHost host = new SidetabEditorHost();
            host.setBounds(0, 0, hostWidth, hostHeight);
            layeredPane.add(host);

            JComponent code = new JPanel();
            host.add(code, BorderLayout.CENTER);
            JPanel side = new JPanel();
            host.add(side, BorderLayout.EAST);
            EditorLayoutMirror mirror = new EditorLayoutMirror(layeredPane, host, code);
            mirror.applySize(hostWidth, hostHeight, sidetabColumnWidth(editor, sidetabColumnWidth));
            return mirror;
        }

        private static int sidetabColumnWidth(FileEditor editor, int fallback) {
            SidetabBarPanel panel = editor.getUserData(SidetabsManager.SIDETAB_BAR_KEY);
            if (panel != null && panel.getParent() instanceof SidetabEditorHost host) {
                JComponent side = (JComponent) ((BorderLayout) host.getLayout()).getLayoutComponent(
                        TabzSettings.getInstance().isSidetabsOnRight() ? BorderLayout.EAST : BorderLayout.WEST
                );
                if (side != null && side.getWidth() > 0) {
                    return side.getWidth();
                }
            }
            return fallback;
        }

        private void applySize(int hostWidth, int hostHeight, int sidetabColumnWidth) {
            JComponent code = layoutComponent;
            JPanel side = (JPanel) ((BorderLayout) host.getLayout()).getLayoutComponent(BorderLayout.EAST);
            side.setPreferredSize(new Dimension(sidetabColumnWidth, hostHeight));
            code.setPreferredSize(new Dimension(Math.max(0, hostWidth - sidetabColumnWidth), hostHeight));
            host.setSize(hostWidth, hostHeight);
            host.doLayout();
        }
    }

    private static final class LayoutSnapshot {
        private final Rectangle sidetabsIcon;
        private final Rectangle tabzIcon;
        private final int sidetabContentTopY;
        private final int topIconReserve;

        private LayoutSnapshot(
                Rectangle sidetabsIcon,
                Rectangle tabzIcon,
                int sidetabContentTopY,
                int topIconReserve
        ) {
            this.sidetabsIcon = sidetabsIcon;
            this.tabzIcon = tabzIcon;
            this.sidetabContentTopY = sidetabContentTopY;
            this.topIconReserve = topIconReserve;
        }

        private void assertSidetabsBelowIcons() {
            int gap = SidetabIconLayout.sidetabGapBelowIcons();
            int iconBottom = sidetabsIcon.y + sidetabsIcon.height;
            if (tabzIcon != null) {
                iconBottom = Math.max(iconBottom, tabzIcon.y + tabzIcon.height);
            }
            assertTrue(
                    "SideTab content must start below the collapse icons (contentTop="
                            + sidetabContentTopY + ", iconBottom=" + iconBottom + ", gap=" + gap + ")",
                    sidetabContentTopY >= iconBottom + gap - TOLERANCE
            );
        }

        private void assertPanelReserveMatchesLayout() {
            if (TabzSettings.getInstance().getSidetabLayoutMode() != SidetabLayoutMode.BESIDE) {
                return;
            }
            if (!TabzSettings.getInstance().isSidetabsExpanded()) {
                return;
            }
            assertEquals(
                    "SideTab column reserve must follow collapse layout",
                    SidetabIconLayout.besideColumnTopReserve(),
                    topIconReserve
            );
        }

        private void assertTabzIconLeftOfSidetabsIcon() {
            assertNotNull(tabzIcon);
            assertNear(
                    "TabZ icon must sit left of the SideTabs icon",
                    sidetabsIcon.x - SidetabIconLayout.iconGap() - tabzIcon.width,
                    tabzIcon.x
            );
            assertNear("Collapse icons must share one row", sidetabsIcon.y, tabzIcon.y);
        }

        private static void assertNear(String message, int expected, int actual) {
            assertTrue(
                    message + " (expected=" + expected + ", actual=" + actual + ", tolerance=" + TOLERANCE + ")",
                    Math.abs(expected - actual) <= TOLERANCE
            );
        }
    }
}
