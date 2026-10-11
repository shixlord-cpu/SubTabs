package com.zayax.tabz;

import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.Container;

/**
 * Guards Tabz settings open time. Before 2026-08: eager rules tables + repeated migration
 * made opening Settings &gt; Tabz noticeably slow on the EDT.
 */
public class TabzConfigurablePerformanceTest extends LightPlatformTestCase {
    private static final int WARMUP_RUNS = 2;
    private static final int APPEARANCE_OPEN_BUDGET_MS = 2_000;
    private static final int RULES_TAB_BUDGET_MS = 4_000;
    private static final int MIGRATION_RELOAD_BUDGET_MS = 250;

    public void testAppearanceTabBuildsWithinBudget() {
        warmUpSettings();
        warmUpConfigurable();

        long elapsedMs = measureAppearanceOpenMs();
        assertTrue(
                "Tabz settings appearance tab should open quickly, took " + elapsedMs + "ms",
                elapsedMs <= APPEARANCE_OPEN_BUDGET_MS
        );
    }

    public void testRulesTabBuildsOnlyAfterSelection() {
        warmUpSettings();
        TabzConfigurable configurable = new TabzConfigurable();
        JComponent root = configurable.createComponent();
        JTabbedPane mainTabs = findTabbedPane(root);
        assertNotNull(mainTabs);
        assertEquals(6, mainTabs.getTabCount());
        assertEquals("Appearance", mainTabs.getTitleAt(0));
        assertEquals("Hover Sync", mainTabs.getTitleAt(1));
        assertEquals("Horizontal Tabs", mainTabs.getTitleAt(2));
        assertEquals("Vertical Tabs", mainTabs.getTitleAt(3));
        assertEquals("Split Pairs", mainTabs.getTitleAt(4));
        assertEquals("AI", mainTabs.getTitleAt(5));
        assertEquals(0, mountedRulesPanelCount(mainTabs));

        long elapsedMs = measureRulesTabOpenMs(mainTabs);
        assertTrue(
                "Tabz H-/V-Tabs rules should build lazily within budget, took " + elapsedMs + "ms",
                elapsedMs <= RULES_TAB_BUDGET_MS
        );
        assertTrue(mountedRulesPanelCount(mainTabs) >= 2);
        configurable.disposeUIResources();
    }

    public void testSidetabRulesMigrationSkipsWorkWhenAlreadyCurrent() {
        warmUpSettings();
        TabzSettings settings = TabzSettings.getInstance();
        TabzSettings.State snapshot = settings.getState();

        long elapsedMs = measureReloadMigrationMs(snapshot);
        assertTrue(
                "Reloading current sidetab rules should stay fast, took " + elapsedMs + "ms",
                elapsedMs <= MIGRATION_RELOAD_BUDGET_MS
        );
    }

    private static void warmUpSettings() {
        TabzSettings.getInstance().getSidetabRules();
        TabzSettings.getInstance().getRules();
    }

    private static void warmUpConfigurable() {
        for (int index = 0; index < WARMUP_RUNS; index++) {
            TabzConfigurable configurable = new TabzConfigurable();
            configurable.createComponent();
            configurable.disposeUIResources();
        }
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    }

    private static long measureAppearanceOpenMs() {
        long startNs = System.nanoTime();
        TabzConfigurable configurable = new TabzConfigurable();
        configurable.createComponent();
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        configurable.disposeUIResources();
        return (System.nanoTime() - startNs) / 1_000_000L;
    }

    private static long measureRulesTabOpenMs(JTabbedPane mainTabs) {
        long startNs = System.nanoTime();
        mainTabs.setSelectedIndex(2);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        return (System.nanoTime() - startNs) / 1_000_000L;
    }

    private static long measureReloadMigrationMs(TabzSettings.State snapshot) {
        long startNs = System.nanoTime();
        TabzSettings settings = new TabzSettings();
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
        if (mainTabs.getTabCount() > 2 && mainTabs.getComponentAt(2) instanceof JPanel hTabs) {
            if (hTabs.getComponentCount() > 1) {
                count++;
            }
        }
        if (mainTabs.getTabCount() > 3 && mainTabs.getComponentAt(3) instanceof JPanel vTabs) {
            if (vTabs.getComponentCount() > 1) {
                count++;
            }
        }
        return count;
    }
}
