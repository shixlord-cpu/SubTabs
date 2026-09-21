package de.sasbe.subtabs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SubtabRuleSwitchIntegrationTest extends RealEditorWindowTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setFamiliaEnabled(true);
        settings.setSubtabsActive(true);
        settings.setShowCollapseButton(false);
        settings.setRules(twoOverlappingStateRules());
        ComponentSubtabsDocumentListener.install(getProject());
    }

    public void testRuleSwitchButtonVisibleForOverlappingStateRules() throws Exception {
        VirtualFile dir = WriteAction.computeAndWait(() -> sourceDir.createChildDirectory(this, "state-management"));
        VirtualFile actions = WriteAction.computeAndWait(() -> dir.createChildData(this, "cart.actions.ts"));
        WriteAction.run(() -> actions.setBinaryContent("export const x = 1;".getBytes(StandardCharsets.UTF_8)));
        VirtualFile reducer = WriteAction.computeAndWait(() -> dir.createChildData(this, "cart.reducer.ts"));
        WriteAction.run(() -> reducer.setBinaryContent("export const y = 1;".getBytes(StandardCharsets.UTF_8)));
        WriteAction.computeAndWait(() -> dir.createChildData(this, "catalog.actions.ts"));

        openAndSettle(actions);
        ComponentSubtabsManager.attachIfNeeded(getProject(), actions);

        ComponentSubtabBarPanel panel = barFor(actions);
        assertNotNull(panel);
        assertTrue("rule switch must appear when two state rules match", panel.isRuleSwitchVisible());

        assertEquals("rule:0:cart", ComponentFileNaming.componentBaseName(actions.getName()));
        assertEquals("State Central", SubtabsSettings.getInstance().getRules().get(0).name);

        panel.switchRuleForDisplayedFile();
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        panel = barFor(actions);
        assertNotNull(panel);
        assertEquals("rule:0:actions#cart", ComponentFileNaming.componentBaseName(actions.getName()));
        assertEquals("State Feature", SubtabsSettings.getInstance().getRules().get(0).name);
        assertEquals("State Central", SubtabsSettings.getInstance().getRules().get(1).name);
        assertTrue(panel.isRuleSwitchVisible());
    }

    public void testRuleSwitchVisibleWithoutSubtabsForSingleStateFile() throws Exception {
        SubtabsSettings.getInstance().setRules(SubtabRulesDefaults.createDefaults());
        VirtualFile dir = WriteAction.computeAndWait(() -> sourceDir.createChildDirectory(this, "single-state"));
        VirtualFile state = WriteAction.computeAndWait(() -> dir.createChildData(this, "cart.state.ts"));
        WriteAction.run(() -> state.setBinaryContent("export const state = {};".getBytes(StandardCharsets.UTF_8)));

        openAndSettle(state);
        ComponentSubtabsManager.attachIfNeeded(getProject(), state);

        ComponentSubtabBarPanel panel = barFor(state);
        assertNotNull(panel);
        assertTrue("rule switch must appear even without subtabs", panel.isRuleSwitchVisible());
    }

    public void testRuleSwitchKeepsBarHeight() throws Exception {
        VirtualFile dir = WriteAction.computeAndWait(() -> sourceDir.createChildDirectory(this, "stable-height"));
        VirtualFile actions = WriteAction.computeAndWait(() -> dir.createChildData(this, "cart.actions.ts"));
        WriteAction.run(() -> actions.setBinaryContent("export const x = 1;".getBytes(StandardCharsets.UTF_8)));
        VirtualFile reducer = WriteAction.computeAndWait(() -> dir.createChildData(this, "cart.reducer.ts"));
        WriteAction.run(() -> reducer.setBinaryContent("export const y = 1;".getBytes(StandardCharsets.UTF_8)));
        VirtualFile effects = WriteAction.computeAndWait(() -> dir.createChildData(this, "cart.effects.ts"));
        WriteAction.run(() -> effects.setBinaryContent("export const z = 1;".getBytes(StandardCharsets.UTF_8)));

        openAndSettle(actions);
        ComponentSubtabsManager.attachIfNeeded(getProject(), actions);

        ComponentSubtabBarPanel panel = barFor(actions);
        assertNotNull(panel);
        int before = panel.getPreferredSize().height;

        panel.switchRuleForDisplayedFile();
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        panel = barFor(actions);
        assertNotNull(panel);
        assertEquals("bar height must stay stable when switching rules", before, panel.getPreferredSize().height);
    }

    public void testRuleSwitchUpdatesSubtabLabelsImmediately() throws Exception {
        CustomSubtabRule stemTabs = SubtabRulesDefaults.createDefaults().stream()
                .filter(rule -> "State Central".equals(rule.name))
                .findFirst()
                .orElseThrow()
                .copy();
        stemTabs.nameSegments = "2, 2, 2, 2, 2, 2, 2, 2";

        CustomSubtabRule prefixTabs = stemTabs.copy();
        prefixTabs.name = "State Prefix";
        prefixTabs.nameSegments = "1";
        prefixTabs.searchNeighbors = false;

        SubtabsSettings.getInstance().setRules(new ArrayList<>(List.of(stemTabs, prefixTabs)));

        VirtualFile dir = WriteAction.computeAndWait(() -> sourceDir.createChildDirectory(this, "label-switch"));
        VirtualFile actions = WriteAction.computeAndWait(() -> dir.createChildData(this, "cart.actions.ts"));
        WriteAction.run(() -> actions.setBinaryContent("export const x = 1;".getBytes(StandardCharsets.UTF_8)));
        VirtualFile reducer = WriteAction.computeAndWait(() -> dir.createChildData(this, "cart.reducer.ts"));
        WriteAction.run(() -> reducer.setBinaryContent("export const y = 1;".getBytes(StandardCharsets.UTF_8)));

        openAndSettle(actions);
        ComponentSubtabsManager.attachIfNeeded(getProject(), actions);

        ComponentSubtabBarPanel panel = barFor(actions);
        assertNotNull(panel);
        assertEquals("actions", panel.buttonFor(actions).getText());

        panel.switchRuleForDisplayedFile();
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        panel = barFor(actions);
        assertNotNull(panel);
        assertEquals("cart", panel.buttonFor(actions).getText());
    }

    public void testRuleSwitchVisibleWhenSubtabsInactive() throws Exception {
        SubtabsSettings.getInstance().setRules(SubtabRulesDefaults.createDefaults());
        SubtabsSettings.getInstance().setSubtabsActive(false);
        SubtabsSettings.getInstance().setShowCollapseButton(true);

        VirtualFile dir = WriteAction.computeAndWait(() -> sourceDir.createChildDirectory(this, "inactive-state"));
        VirtualFile actions = WriteAction.computeAndWait(() -> dir.createChildData(this, "cart.actions.ts"));
        WriteAction.run(() -> actions.setBinaryContent("export const x = 1;".getBytes(StandardCharsets.UTF_8)));

        openAndSettle(actions);
        ComponentSubtabsManager.attachIfNeeded(getProject(), actions);

        ComponentSubtabBarPanel panel = barFor(actions);
        assertNotNull(panel);
        assertTrue("rule switch must stay visible when SubTabs are inactive", panel.isRuleSwitchVisible());
    }

    public void testRuleSwitchChangesNeighborGroupingWithDefaultRules() throws Exception {
        SubtabsSettings.getInstance().setRules(SubtabRulesDefaults.createDefaults());

        VirtualFile root = WriteAction.computeAndWait(() -> sourceDir.createChildDirectory(this, "feature-neighbors"));
        WriteAction.runAndWait(() -> {
            VirtualFile products = root.createChildDirectory(this, "products");
            VirtualFile productsState = root.createChildDirectory(this, "products-state");
            products.createChildData(this, "products.actions.ts");
            products.createChildData(this, "products.selectors.ts");
            products.createChildData(this, "demo.actions.ts");
            productsState.createChildData(this, "products.reducer.ts");
            productsState.createChildData(this, "products.effects.ts");
        });
        VirtualFile actions = WriteAction.computeAndWait(() -> {
            VirtualFile products = root.findChild("products");
            return products == null ? null : products.findChild("products.actions.ts");
        });
        assertNotNull(actions);

        openAndSettle(actions);
        ComponentSubtabsManager.attachIfNeeded(getProject(), actions);

        ComponentSubtabBarPanel panel = barFor(actions);
        assertNotNull(panel);
        assertEquals("rule:3:products", ComponentFileNaming.componentBaseName(actions.getName()));
        assertEquals("State Central", SubtabsSettings.getInstance().getRules().get(3).name);
        ComponentRelatedFiles.Match neighborMatch = ComponentRelatedFiles.find(actions);
        assertNotNull(neighborMatch);
        assertEquals(4, neighborMatch.relatedFiles().size());

        panel.switchRuleForDisplayedFile();
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertEquals("rule:3:actions#products", ComponentFileNaming.componentBaseName(actions.getName()));
        assertEquals("State Feature", SubtabsSettings.getInstance().getRules().get(3).name);
        ComponentRelatedFiles.Match folderMatch = ComponentRelatedFiles.find(actions);
        assertNotNull(folderMatch);
        assertEquals(2, folderMatch.relatedFiles().size());
    }

    public void testRuleSwitchIsOutsideScrollHost() throws Exception {
        VirtualFile dir = WriteAction.computeAndWait(() -> sourceDir.createChildDirectory(this, "feature-state"));
        VirtualFile actions = WriteAction.computeAndWait(() -> dir.createChildData(this, "products.actions.ts"));
        WriteAction.run(() -> actions.setBinaryContent("export const x = 1;".getBytes(StandardCharsets.UTF_8)));
        VirtualFile reducer = WriteAction.computeAndWait(() -> dir.createChildData(this, "products.reducer.ts"));
        WriteAction.run(() -> reducer.setBinaryContent("export const y = 1;".getBytes(StandardCharsets.UTF_8)));

        openAndSettle(actions);
        ComponentSubtabsManager.attachIfNeeded(getProject(), actions);

        ComponentSubtabBarPanel panel = barFor(actions);
        assertNotNull(panel);
        assertNotNull(panel.ruleSwitchButton());
        assertNull(panel.ruleSwitchButton().getParent() == null ? null : findScrollPaneAncestor(panel.ruleSwitchButton()));
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
        for (var editor : ComponentSubtabsManager.editorsFor(manager, file)) {
            ComponentSubtabBarPanel panel = editor.getUserData(ComponentSubtabsManager.SUBTAB_BAR_KEY);
            if (panel != null) {
                return panel;
            }
        }
        return null;
    }

    private static List<CustomSubtabRule> twoOverlappingStateRules() {
        CustomSubtabRule central = SubtabRulesDefaults.createDefaults().stream()
                .filter(rule -> "State Central".equals(rule.name))
                .findFirst()
                .orElseThrow();
        return new ArrayList<>(List.of(central, SubtabRulesDefaults.stateFeatureRule()));
    }

    private static Object findScrollPaneAncestor(javax.swing.JComponent component) {
        java.awt.Container parent = component.getParent();
        while (parent != null) {
            if (parent instanceof com.intellij.ui.components.JBScrollPane) {
                return parent;
            }
            parent = parent.getParent();
        }
        return null;
    }
}
