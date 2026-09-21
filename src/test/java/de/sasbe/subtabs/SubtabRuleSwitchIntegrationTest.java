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

        openAndSettle(actions);
        ComponentSubtabsManager.attachIfNeeded(getProject(), actions);

        ComponentSubtabBarPanel panel = barFor(actions);
        assertNotNull(panel);
        assertTrue("rule switch must appear when two state rules match", panel.isRuleSwitchVisible());

        String before = ComponentFileNaming.componentBaseName(actions.getName());
        assertEquals("rule:0:cart", before);

        panel.switchRuleForDisplayedFile();
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertEquals("rule:0:cart", ComponentFileNaming.componentBaseName(actions.getName()));
        assertEquals("State Nachbar", SubtabsSettings.getInstance().getRules().get(0).name);
        assertTrue(panel.isRuleSwitchVisible());
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
        CustomSubtabRule primary = SubtabRulesDefaults.createDefaults().stream()
                .filter(rule -> "State".equals(rule.name))
                .findFirst()
                .orElseThrow();
        CustomSubtabRule neighborState = primary.copy();
        neighborState.name = "State Nachbar";
        neighborState.searchNeighbors = true;
        return new ArrayList<>(List.of(primary, neighborState));
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
