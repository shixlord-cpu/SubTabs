package de.sasbe.subtabs;

import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.Container;

/**
 * Guards Familia settings open time. Before 2026-08: eager rules tables + repeated migration
 * made opening Settings &gt; Familia noticeably slow on the EDT.
 */
public class SubtabsConfigurablePerformanceTest extends LightPlatformTestCase {
    private static final int WARMUP_RUNS = 2;
    private static final int APPEARANCE_OPEN_BUDGET_MS = 2_000;
    private static final int RULES_TAB_BUDGET_MS = 4_000;
    private static final int MIGRATION_RELOAD_BUDGET_MS = 250;

    public void testAppearanceTabBuildsWithinBudget() {
        warmUpSettings();
        warmUpConfigurable();

        long elapsedMs = measureAppearanceOpenMs();
        assertTrue(
                "Familia settings appearance tab should open quickly, took " + elapsedMs + "ms",
                elapsedMs <= APPEARANCE_OPEN_BUDGET_MS
        );
    }

    public void testRulesTabBuildsOnlyAfterSelection() {
        warmUpSettings();
        SubtabsConfigurable configurable = new SubtabsConfigurable();
        JComponent root = configurable.createComponent();
        JTabbedPane mainTabs = findTabbedPane(root);
        assertNotNull(mainTabs);
        assertEquals(5, mainTabs.getTabCount());
        assertEquals("Ansicht", mainTabs.getTitleAt(0));
        assertEquals("H-Tabs", mainTabs.getTitleAt(1));
        assertEquals("V-Tabs", mainTabs.getTitleAt(2));
        assertEquals("SplitTabs", mainTabs.getTitleAt(3));
        assertEquals("AI", mainTabs.getTitleAt(4));
        assertEquals(0, mountedRulesPanelCount(mainTabs));

        long elapsedMs = measureRulesTabOpenMs(mainTabs);
        assertTrue(
                "Familia H-/V-Tabs rules should build lazily within budget, took " + elapsedMs + "ms",
                elapsedMs <= RULES_TAB_BUDGET_MS
        );
        assertTrue(mountedRulesPanelCount(mainTabs) >= 2);
        configurable.disposeUIResources();
    }

    public void testSidetabRulesMigrationSkipsWorkWhenAlreadyCurrent() {
        warmUpSettings();
        SubtabsSettings settings = SubtabsSettings.getInstance();
        SubtabsSettings.State snapshot = settings.getState();

        long elapsedMs = measureReloadMigrationMs(snapshot);
        assertTrue(
                "Reloading current sidetab rules should stay fast, took " + elapsedMs + "ms",
                elapsedMs <= MIGRATION_RELOAD_BUDGET_MS
        );
    }

    private static void warmUpSettings() {
        SubtabsSettings.getInstance().getSidetabRules();
        SubtabsSettings.getInstance().getRules();
    }

    private static void warmUpConfigurable() {
        for (int index = 0; index < WARMUP_RUNS; index++) {
            SubtabsConfigurable configurable = new SubtabsConfigurable();
            configurable.createComponent();
            configurable.disposeUIResources();
        }
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    }

    private static long measureAppearanceOpenMs() {
        long startNs = System.nanoTime();
        SubtabsConfigurable configurable = new SubtabsConfigurable();
        configurable.createComponent();
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        configurable.disposeUIResources();
        return (System.nanoTime() - startNs) / 1_000_000L;
    }

    private static long measureRulesTabOpenMs(JTabbedPane mainTabs) {
        long startNs = System.nanoTime();
        mainTabs.setSelectedIndex(1);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        return (System.nanoTime() - startNs) / 1_000_000L;
    }

    private static long measureReloadMigrationMs(SubtabsSettings.State snapshot) {
        long startNs = System.nanoTime();
        SubtabsSettings settings = new SubtabsSettings();
        settings.loadState(snapshot);
        settings.getSidetabRules();
        return (System.nanoTime() - startNs) / 1_000_000L;
    }

    private static JTabbedPane findTabbedPane(Container container) {
        for (var child : container.getComponents()) {
            if (child instanceof JTabbedPane tabbedPane) {
                return tabbedPane;
            }
            if (child instanceof Container nested) {
                JTabbedPane found = findTabbedPane(nested);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static int mountedRulesPanelCount(JTabbedPane mainTabs) {
        int count = 0;
        if (mainTabs.getTabCount() > 1 && mainTabs.getComponentAt(1) instanceof JPanel hTabs) {
            if (hTabs.getComponentCount() > 1) {
                count++;
            }
        }
        if (mainTabs.getTabCount() > 2 && mainTabs.getComponentAt(2) instanceof JPanel vTabs) {
            if (vTabs.getComponentCount() > 1) {
                count++;
            }
        }
        return count;
    }
}
