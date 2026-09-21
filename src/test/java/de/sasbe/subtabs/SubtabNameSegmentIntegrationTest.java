package de.sasbe.subtabs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;

import java.util.ArrayList;
import java.util.List;
public class SubtabNameSegmentIntegrationTest extends HeavyPlatformTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings.getInstance().resetToDefaults();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            SubtabsSettings.getInstance().resetToDefaults();
        } finally {
            super.tearDown();
        }
    }

    public void testStateSubtabLabelsFollowNameSegments() throws Exception {
        SubtabsSettings.getInstance().setSubtabsActive(true);

        VirtualFile dir = getVirtualFile(createTempDir("state"));
        WriteAction.runAndWait(() -> {
            dir.createChildData(this, "cart.actions.ts");
            dir.createChildData(this, "cart.reducer.ts");
            dir.createChildData(this, "cart.effects.ts");
        });
        VirtualFile actions = WriteAction.computeAndWait(() -> dir.findChild("cart.actions.ts"));
        assertNotNull(actions);

        ComponentRelatedFiles.Match match = ComponentRelatedFiles.findUncached(actions);
        assertNotNull(match);
        assertEquals("actions", labelFor(match, "cart.actions.ts"));
        assertEquals("reducer", labelFor(match, "cart.reducer.ts"));
        assertEquals("effects", labelFor(match, "cart.effects.ts"));
    }

    public void testStateGroupsAcrossNeighborFoldersByGroupName() throws Exception {
        SubtabsSettings.getInstance().setSubtabsActive(true);

        VirtualFile root = getVirtualFile(createTempDir("state-neighbors"));
        WriteAction.runAndWait(() -> {
            VirtualFile products = root.createChildDirectory(this, "products");
            VirtualFile productsState = root.createChildDirectory(this, "products-state");
            products.createChildData(this, "products.actions.ts");
            products.createChildData(this, "products.selectors.ts");
            productsState.createChildData(this, "products.reducer.ts");
            productsState.createChildData(this, "products.effects.ts");
        });
        VirtualFile actions = WriteAction.computeAndWait(() -> {
            VirtualFile products = root.findChild("products");
            return products == null ? null : products.findChild("products.actions.ts");
        });
        assertNotNull(actions);

        ComponentRelatedFiles.Match match = ComponentRelatedFiles.findUncached(actions);
        assertNotNull(match);
        assertEquals("rule:3:products", match.baseName());
        assertEquals(4, match.relatedFiles().size());
    }

    public void testFeatureBasedDemoStructureGroupsStateAcrossNeighborFolders() throws Exception {
        SubtabsSettings.getInstance().setSubtabsActive(true);

        VirtualFile root = getVirtualFile(createTempDir("feature-based"));
        WriteAction.runAndWait(() -> {
            VirtualFile products = root.createChildDirectory(this, "products");
            VirtualFile productsState = root.createChildDirectory(this, "products-state");
            products.createChildData(this, "products.actions.ts");
            products.createChildData(this, "products.selectors.ts");
            products.createChildData(this, "products.component.ts");
            productsState.createChildData(this, "products.reducer.ts");
            productsState.createChildData(this, "products.effects.ts");
            productsState.createChildData(this, "products.state.ts");
        });
        VirtualFile actions = WriteAction.computeAndWait(() -> {
            VirtualFile products = root.findChild("products");
            return products == null ? null : products.findChild("products.actions.ts");
        });
        assertNotNull(actions);

        ComponentRelatedFiles.Match match = ComponentRelatedFiles.findUncached(actions);
        assertNotNull(match);
        assertEquals("rule:3:products", match.baseName());
        assertEquals(5, match.relatedFiles().size());
    }

    public void testStateFolderRuleKeepsFeatureGroupInsideSingleFolder() throws Exception {
        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setSubtabsActive(true);
        List<CustomSubtabRule> rules = new ArrayList<>(settings.getRules());
        CustomSubtabRule state = rules.stream()
                .filter(rule -> "State".equals(rule.name))
                .findFirst()
                .orElseThrow();
        CustomSubtabRule stateFolder = rules.stream()
                .filter(rule -> "State Folder".equals(rule.name))
                .findFirst()
                .orElseThrow();
        rules.remove(state);
        rules.remove(stateFolder);
        rules.add(0, stateFolder);
        settings.setRules(rules);
        ComponentFileNaming.invalidateRulesCache();

        VirtualFile root = getVirtualFile(createTempDir("feature-based-folder-only"));
        WriteAction.runAndWait(() -> {
            VirtualFile products = root.createChildDirectory(this, "products");
            VirtualFile productsState = root.createChildDirectory(this, "products-state");
            products.createChildData(this, "products.actions.ts");
            products.createChildData(this, "products.selectors.ts");
            productsState.createChildData(this, "products.reducer.ts");
        });
        VirtualFile actions = WriteAction.computeAndWait(() -> {
            VirtualFile products = root.findChild("products");
            return products == null ? null : products.findChild("products.actions.ts");
        });
        assertNotNull(actions);

        ComponentRelatedFiles.Match match = ComponentRelatedFiles.findUncached(actions);
        assertNotNull(match);
        assertEquals("rule:0:products", match.baseName());
        assertEquals(2, match.relatedFiles().size());
    }

    public void testChangingNameSegmentsUpdatesSubtabLabels() throws Exception {
        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setSubtabsActive(true);

        VirtualFile dir = getVirtualFile(createTempDir("state-segments"));
        WriteAction.runAndWait(() -> {
            dir.createChildData(this, "cart.actions.ts");
            dir.createChildData(this, "cart.reducer.ts");
        });
        VirtualFile actions = WriteAction.computeAndWait(() -> dir.findChild("cart.actions.ts"));
        assertNotNull(actions);

        List<CustomSubtabRule> rules = new ArrayList<>(settings.getRules());
        CustomSubtabRule state = rules.stream()
                .filter(rule -> "State".equals(rule.name))
                .findFirst()
                .orElseThrow();
        state.nameSegments = "1, 1, 1, 1, 1, 1, 1, 1";
        settings.setRules(rules);
        ComponentFileNaming.invalidateRulesCache();
        ComponentRelatedFilesCache.getInstance(getProject()).clear();

        ComponentRelatedFiles.Match match = ComponentRelatedFiles.findUncached(actions);
        assertNotNull(match);
        assertEquals("cart", labelFor(match, "cart.actions.ts"));
        assertEquals("cart", labelFor(match, "cart.reducer.ts"));
    }

    public void testZeroSegmentShowsQuestionMark() throws Exception {
        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setSubtabsActive(true);

        VirtualFile dir = getVirtualFile(createTempDir("state-zero"));
        WriteAction.runAndWait(() -> {
            dir.createChildData(this, "cart.actions.ts");
            dir.createChildData(this, "cart.reducer.ts");
        });
        VirtualFile actions = WriteAction.computeAndWait(() -> dir.findChild("cart.actions.ts"));
        assertNotNull(actions);

        List<CustomSubtabRule> rules = new ArrayList<>(settings.getRules());
        CustomSubtabRule state = rules.stream()
                .filter(rule -> "State".equals(rule.name))
                .findFirst()
                .orElseThrow();
        state.nameSegments = "0, 0, 0, 0, 0, 0, 0, 0";
        settings.setRules(rules);
        ComponentFileNaming.invalidateRulesCache();

        ComponentRelatedFiles.Match match = ComponentRelatedFiles.findUncached(actions);
        assertNotNull(match);
        assertEquals("?", labelFor(match, "cart.actions.ts"));
        assertEquals("?", labelFor(match, "cart.reducer.ts"));
    }

    private static String labelFor(
            ComponentRelatedFiles.Match match,
            String fileName
    ) {
        return match.relatedFiles().stream()
                .filter(entry -> fileName.equals(entry.file().getName()))
                .map(ComponentRelatedFiles.Entry::label)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing subtab for " + fileName));
    }
}
