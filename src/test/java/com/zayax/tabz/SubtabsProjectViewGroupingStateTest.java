package com.zayax.tabz;

import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;

public class SubtabsProjectViewGroupingStateTest extends LightPlatformTestCase {
    public void testGroupingStaysEnabledWhenTabzAreCollapsed() {
        TabzSettings settings = TabzSettings.getInstance();
        settings.setTabzEnabled(true);
        settings.setProjectViewGroupingEnabled(true);
        settings.setProjectViewGroupingActive(true);
        settings.setSubtabsActive(false);

        assertTrue(SubtabProjectViewGrouping.isEnabled());
        assertFalse(SubtabsProjectViewGroupingState.getInstance(getProject()).isCollapsed());
    }

    public void testToggleUpdatesActiveStateNotEnabledFlag() {
        TabzSettings settings = TabzSettings.getInstance();
        settings.setProjectViewGroupingEnabled(true);
        settings.setProjectViewGroupingActive(true);

        SubtabsProjectViewGroupingState state = SubtabsProjectViewGroupingState.getInstance(getProject());
        state.toggle(getProject());
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertTrue(settings.isProjectViewGroupingEnabled());
        assertFalse(settings.isProjectViewGroupingActive());
        assertTrue(state.isCollapsed());

        state.toggle(getProject());
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertTrue(settings.isProjectViewGroupingEnabled());
        assertTrue(settings.isProjectViewGroupingActive());
        assertFalse(state.isCollapsed());
    }

    public void testCollapsingTabzDoesNotChangeProjectViewGrouping() {
        TabzSettings settings = TabzSettings.getInstance();
        settings.setTabzEnabled(true);
        settings.setProjectViewGroupingEnabled(true);
        settings.setProjectViewGroupingActive(true);
        settings.setSubtabsActive(true);

        SubtabsCollapseState.getInstance(getProject()).toggle(getProject());

        assertFalse(settings.isSubtabsActive());
        assertTrue(settings.isProjectViewGroupingActive());
        assertTrue(SubtabProjectViewGrouping.isEnabled());
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            TabzSettings.getInstance().setTabzEnabled(true);
            TabzSettings.getInstance().setProjectViewGroupingEnabled(true);
            TabzSettings.getInstance().setProjectViewGroupingActive(true);
            com.intellij.testFramework.PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            SubtabProjectViewGroupingBusyState.getInstance(getProject()).reset();
        } finally {
            super.tearDown();
        }
    }
}
