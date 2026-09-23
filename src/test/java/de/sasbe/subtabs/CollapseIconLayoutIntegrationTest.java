package de.sasbe.subtabs;

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
 * Verifies collapse icons, SideTabs, and SubTabs stay aligned through real editor attach and toggles.
 */
public class CollapseIconLayoutIntegrationTest extends RealEditorWindowTestCase {
    private static final int TOLERANCE = 2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setSubtabsActive(true);
        settings.setSidetabsActive(true);
        settings.setSidetabsExpanded(true);
        settings.setSidetabLayoutMode(SidetabLayoutMode.BESIDE);
        settings.setSidetabsOnRight(true);
        settings.setShowCollapseButton(true);
        settings.setSidetabRules(SidetabRulesDefaults.createDefaults());
        ComponentSubtabsDocumentListener.install(getProject());
    }

    public void testThreeIconRowAlignsAfterStartupWithCollapsedSidetabs() throws Exception {
        SubtabsSettings.getInstance().setRules(twoOverlappingStateRules());
        SubtabsSettings.getInstance().setSidetabsExpanded(false);

        VirtualFile dir = WriteAction.computeAndWait(() -> sourceDir.createChildDirectory(this, "startup-icons"));
        VirtualFile actions = WriteAction.computeAndWait(() -> dir.createChildData(this, "cart.actions.ts"));
        WriteAction.run(() -> actions.setBinaryContent("export const x = 1;".getBytes(StandardCharsets.UTF_8)));
        VirtualFile reducer = WriteAction.computeAndWait(() -> dir.createChildData(this, "cart.reducer.ts"));
        WriteAction.run(() -> reducer.setBinaryContent("export const y = 1;".getBytes(StandardCharsets.UTF_8)));

        openAndSettle(actions);
        ComponentSubtabsFileEditorListener.attachToAlreadyOpenFiles(getProject());
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        FileEditor editor = selectedEditor(actions);
        assertTrue("rule switch must use the overlay row on startup", RuleSwitchOverlay.isInstalled(editor));
        assertTrue("SideTabs toggle must be installed on startup", SidetabsToggleOverlay.isInstalled(editor));
        assertTrue("SubTabs collapse must be installed on startup", SubtabsCollapseOverlay.isInstalled(editor));

        EditorLayoutMirror mirror = EditorLayoutMirror.forEditor(editor);
        assertThreeIconRowAligned(editor, mirror);
    }

    public void testSubtabsIconReturnsToOverlayRowAfterBothCollapsedThenSubtabsExpanded() throws Exception {
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
        assertNotNull(editor.getUserData(ComponentSubtabsManager.SUBTAB_BAR_KEY));
        EditorLayoutMirror mirror = EditorLayoutMirror.forEditor(editor);
        LayoutSnapshot baseline = captureLayout(editor, mirror);
        baseline.assertSubtabsIconLeftOfSidetabsIcon();
        baseline.assertSidetabsBelowIcons();

        SubtabsCollapseState.getInstance(getProject()).setCollapsed(getProject(), true);
        SidetabsCollapseState.getInstance(getProject()).setCollapsed(getProject(), true);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        SubtabsCollapseState.getInstance(getProject()).setCollapsed(getProject(), false);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        editor = selectedEditor(html);
        mirror.refresh(editor);
        assertNull("SubTabs collapse must not return to the bar after re-expanding",
                ComponentSubtabsManager.visibleCollapseButton(editor));
        assertTrue("SubTabs collapse must stay in the top-right overlay row",
                SubtabsCollapseOverlay.isInstalled(editor));
        assertTrue("SideTabs toggle must stay in the overlay row",
                SidetabsToggleOverlay.isInstalled(editor));

        LayoutSnapshot restored = captureLayout(editor, mirror);
        restored.assertSubtabsIconLeftOfSidetabsIcon();
        restored.assertSidetabsBelowIcons();
        assertNear("SubTabs icon X after restore", baseline.subtabsIcon.x, restored.subtabsIcon.x);
        assertNear("SubTabs icon Y after restore", baseline.subtabsIcon.y, restored.subtabsIcon.y);
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
        baseline.assertSubtabsIconLeftOfSidetabsIcon();
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

        SubtabsSettings settings = SubtabsSettings.getInstance();
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
        assertNotNull(editor.getUserData(ComponentSubtabsManager.SUBTAB_BAR_KEY));
        LayoutSnapshot subtabsOnly = captureLayout(editor, mirror);
        assertNull("SubTabs-only must not keep collapse button on the bar",
                ComponentSubtabsManager.visibleCollapseButton(editor));
        assertTrue("SubTabs-only must use the top-right collapse overlay",
                SubtabsCollapseOverlay.isInstalled(editor));
        assertNear("SubTabs-only X", dual.subtabsIcon.x, subtabsOnly.subtabsIcon.x);
        assertNear("SubTabs-only Y", dual.subtabsIcon.y, subtabsOnly.subtabsIcon.y);

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

    public void testProductListHtmlSubtabsOnlyUsesOverlaySlotWhenSidetabsGloballyCollapsed() throws Exception {
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

        SubtabsSettings settings = SubtabsSettings.getInstance();
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
        assertNull("SubTabs collapse must not stay on the bar",
                ComponentSubtabsManager.visibleCollapseButton(editor));
        assertTrue("SubTabs-only product-list must use the top-right collapse overlay",
                SubtabsCollapseOverlay.isInstalled(editor));

        LayoutSnapshot subtabsOnly = captureLayout(editor, mirror);
        assertNear("product-list SubTabs X", dual.subtabsIcon.x, subtabsOnly.subtabsIcon.x);
        assertNear("product-list SubTabs Y", dual.subtabsIcon.y, subtabsOnly.subtabsIcon.y);
    }

    public void testCollapsedSubtabsOnlyUsesExpandOverlaySlot() throws Exception {
        VirtualFile html = createSourceFile("header.component.html");
        WriteAction.run(() -> html.setBinaryContent("""
                <header>Title</header>
                """.getBytes(StandardCharsets.UTF_8)));
        createSourceFile("header.component.ts");
        openAndSettle(html);

        SubtabsSettings settings = SubtabsSettings.getInstance();
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

        LayoutSnapshot collapsedSubtabsOnly = captureLayout(editor, mirror);
        assertTrue("Collapsed SubTabs-only must use the expand overlay",
                SubtabsExpandOverlay.isInstalled(editor));
        assertNear("Collapsed SubTabs-only X", dual.subtabsIcon.x, collapsedSubtabsOnly.subtabsIcon.x);
        assertNear("Collapsed SubTabs-only Y", dual.subtabsIcon.y, collapsedSubtabsOnly.subtabsIcon.y);
    }

    public void testOverlaySidetabsStayBelowIconsThroughToggleSequences() throws Exception {
        SubtabsSettings.getInstance().setSidetabLayoutMode(SidetabLayoutMode.OVERLAY);
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
                if (SubtabsSettings.getInstance().isSubtabsActive()
                        && SubtabsSettings.getInstance().isSidetabsExpanded()) {
                    current.assertSubtabsIconLeftOfSidetabsIcon();
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
        SubtabsSettings settings = SubtabsSettings.getInstance();
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
        if (baseline.subtabsIcon != null && current.subtabsIcon != null) {
            assertNear(context + ": SubTabs icon X", baseline.subtabsIcon.x, current.subtabsIcon.x);
            assertNear(context + ": SubTabs icon Y", baseline.subtabsIcon.y, current.subtabsIcon.y);
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
        Rectangle subtabsIcon = SidetabIconLayout.layoutSubtabsIcon(
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
        assertNear("rule switch must sit left of SubTabs icon", ruleSwitchIcon.x + ruleSwitchIcon.width + gap, subtabsIcon.x);
        assertNear("SubTabs icon must sit left of SideTabs icon", subtabsIcon.x + subtabsIcon.width + gap, sidetabsIcon.x);
        assertNear("rule switch must share the icon row", ruleSwitchIcon.y, subtabsIcon.y);
        assertNear("SideTabs icon must share the icon row", subtabsIcon.y, sidetabsIcon.y);
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
        Rectangle subtabsIcon = null;
        if (SubtabsSettings.getInstance().isShowCollapseButton()) {
            subtabsIcon = SidetabIconLayout.layoutSubtabsIcon(
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
                subtabsIcon,
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
                        SubtabsSettings.getInstance().isSidetabsOnRight() ? BorderLayout.EAST : BorderLayout.WEST
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
        private final Rectangle subtabsIcon;
        private final int sidetabContentTopY;
        private final int topIconReserve;

        private LayoutSnapshot(
                Rectangle sidetabsIcon,
                Rectangle subtabsIcon,
                int sidetabContentTopY,
                int topIconReserve
        ) {
            this.sidetabsIcon = sidetabsIcon;
            this.subtabsIcon = subtabsIcon;
            this.sidetabContentTopY = sidetabContentTopY;
            this.topIconReserve = topIconReserve;
        }

        private void assertSidetabsBelowIcons() {
            int gap = SidetabIconLayout.sidetabGapBelowIcons();
            int iconBottom = sidetabsIcon.y + sidetabsIcon.height;
            if (subtabsIcon != null) {
                iconBottom = Math.max(iconBottom, subtabsIcon.y + subtabsIcon.height);
            }
            assertTrue(
                    "SideTab content must start below the collapse icons (contentTop="
                            + sidetabContentTopY + ", iconBottom=" + iconBottom + ", gap=" + gap + ")",
                    sidetabContentTopY >= iconBottom + gap - TOLERANCE
            );
        }

        private void assertPanelReserveMatchesLayout() {
            if (SubtabsSettings.getInstance().getSidetabLayoutMode() != SidetabLayoutMode.BESIDE) {
                return;
            }
            if (!SubtabsSettings.getInstance().isSidetabsExpanded()) {
                return;
            }
            assertEquals(
                    "SideTab column reserve must follow collapse layout",
                    SidetabIconLayout.besideColumnTopReserve(),
                    topIconReserve
            );
        }

        private void assertSubtabsIconLeftOfSidetabsIcon() {
            assertNotNull(subtabsIcon);
            assertNear(
                    "SubTabs icon must sit left of the SideTabs icon",
                    sidetabsIcon.x - SidetabIconLayout.iconGap() - subtabsIcon.width,
                    subtabsIcon.x
            );
            assertNear("Collapse icons must share one row", sidetabsIcon.y, subtabsIcon.y);
        }

        private static void assertNear(String message, int expected, int actual) {
            assertTrue(
                    message + " (expected=" + expected + ", actual=" + actual + ", tolerance=" + TOLERANCE + ")",
                    Math.abs(expected - actual) <= TOLERANCE
            );
        }
    }
}
