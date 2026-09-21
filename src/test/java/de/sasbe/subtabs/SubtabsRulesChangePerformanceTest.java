package de.sasbe.subtabs;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Benchmarks settings/rule changes that rebuild directory groups in the project view.
 */
public class SubtabsRulesChangePerformanceTest extends RealEditorWindowTestCase {
    private static final int FILE_NAME_COUNT = 500;
    private static final int RULE_LOOKUP_ITERATIONS = 100;

    private VirtualFile htmlFile;
    private VirtualFile tsFile;
    private List<String> manyComponentFileNames;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setSubtabsActive(true);
        settings.setGroupRelatedFilesInProjectView(true);

        htmlFile = createSourceFile("product-list.component.html");
        tsFile = createSourceFile("product-list.component.ts");
        createSourceFile("product-list.component.scss");

        manyComponentFileNames = new ArrayList<>(FILE_NAME_COUNT);
        for (int index = 0; index < FILE_NAME_COUNT; index++) {
            manyComponentFileNames.add("widget-" + index + ".component.ts");
        }
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            SubtabsProjectViewGroupingBusyState.getInstance(getProject()).reset();
        } finally {
            super.tearDown();
        }
    }

    public void testRulesSnapshotCacheSpeedsUpDirectoryGroupKeyResolution() {
        long uncachedNs = measureRuleLookupsViaSettingsSnapshot();
        long cachedNs = measureRuleLookupsViaComponentFileNaming();

        assertTrue(
                "cached rules snapshot should speed up group-key resolution (cached="
                        + cachedNs / 1_000_000 + "ms, uncached=" + uncachedNs / 1_000_000 + "ms)",
                cachedNs * 5 < uncachedNs * 4
        );
    }

    public void testRulesChangeUsesSingleConsolidatedRefreshPass() {
        openAndSettle(htmlFile);
        openAndSettle(tsFile);

        List<CustomSubtabRule> toggledRules = toggleKomponenteRule();
        long legacyNs = measureEditorSettingsChange(toggledRules, true);
        long optimizedNs = measureEditorSettingsChange(toggledRules, false);

        assertTrue(
                "consolidated editor refresh should beat the legacy multi-pass path (optimized="
                        + optimizedNs / 1_000_000 + "ms, legacy=" + legacyNs / 1_000_000 + "ms)",
                optimizedNs * 5 < legacyNs * 4
        );
    }

    public void testRulesChangeSkipsProjectViewRebuildWhenGroupingIsDisabled() {
        SubtabsSettings.getInstance().setGroupRelatedFilesInProjectView(false);
        openAndSettle(htmlFile);

        List<CustomSubtabRule> toggledRules = toggleKomponenteRule();
        long legacyNs = measureFullSettingsChange(toggledRules, true);
        long optimizedNs = measureFullSettingsChange(toggledRules, false);

        assertTrue(
                "without project-view grouping the optimized path should skip ProjectView.refresh (optimized="
                        + optimizedNs / 1_000_000 + "ms, legacy=" + legacyNs / 1_000_000 + "ms)",
                optimizedNs * 5 < legacyNs * 4
        );
    }

    public void testRulesChangeRefreshesProjectViewWhenGroupingIsEnabled() {
        SubtabsSettings.getInstance().setGroupRelatedFilesInProjectView(true);
        assertTrue(SubtabsPresentation.refreshesProjectViewOnSettingsChange());
    }

    private long measureRuleLookupsViaSettingsSnapshot() {
        long start = System.nanoTime();
        for (int iteration = 0; iteration < RULE_LOOKUP_ITERATIONS; iteration++) {
            List<CustomSubtabRule> rules = SubtabsSettings.getInstance().getRules();
            if (rules.isEmpty()) {
                rules = SubtabRulesDefaults.createDefaults();
            }
            for (String fileName : manyComponentFileNames) {
                CustomSubtabRuleMatcher.match(fileName, rules);
            }
        }
        return System.nanoTime() - start;
    }

    private long measureRuleLookupsViaComponentFileNaming() {
        long start = System.nanoTime();
        for (int iteration = 0; iteration < RULE_LOOKUP_ITERATIONS; iteration++) {
            for (String fileName : manyComponentFileNames) {
                ComponentFileNaming.componentBaseName(fileName);
            }
        }
        return System.nanoTime() - start;
    }

    private long measureEditorSettingsChange(List<CustomSubtabRule> rules, boolean legacy) {
        SubtabsSettings.getInstance().setRules(rules);
        ComponentFileNaming.invalidateRulesCache();
        long start = System.nanoTime();
        if (legacy) {
            SubtabsLegacyRefreshSupport.applyEditorSettingsChangeLegacy(getProject());
        } else {
            ComponentSubtabsManager.applySettingsChange(getProject());
        }
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        return System.nanoTime() - start;
    }

    private long measureFullSettingsChange(List<CustomSubtabRule> rules, boolean legacy) {
        SubtabsSettings.getInstance().setRules(rules);
        ComponentFileNaming.invalidateRulesCache();
        long start = System.nanoTime();
        if (legacy) {
            SubtabsLegacyRefreshSupport.applyFullSettingsChangeLegacy();
        } else {
            SubtabsPresentation.applySettingsChange();
        }
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        return System.nanoTime() - start;
    }

    private static List<CustomSubtabRule> toggleKomponenteRule() {
        List<CustomSubtabRule> rules = new ArrayList<>();
        for (CustomSubtabRule rule : SubtabsSettings.getInstance().getRules()) {
            CustomSubtabRule copy = rule.copy();
            if ("Komponente".equals(copy.name)) {
                copy.enabled = !copy.enabled;
            }
            rules.add(copy);
        }
        return rules;
    }
}
