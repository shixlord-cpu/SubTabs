package de.sasbe.subtabs;

import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;

public class SubtabsProjectViewGroupingStateTest extends LightPlatformTestCase {
    public void testGroupingStaysEnabledWhenSubtabsAreCollapsed() {
        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setFamiliaEnabled(true);
        settings.setProjectViewGroupingEnabled(true);
        settings.setProjectViewGroupingActive(true);
        settings.setSubtabsActive(false);

        assertTrue(SubtabProjectViewGrouping.isEnabled());
        assertFalse(SubtabsProjectViewGroupingState.getInstance(getProject()).isCollapsed());
    }

    public void testToggleUpdatesActiveStateNotEnabledFlag() {
        SubtabsSettings settings = SubtabsSettings.getInstance();
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

    public void testCollapsingSubtabsDoesNotChangeProjectViewGrouping() {
        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setFamiliaEnabled(true);
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
            SubtabsSettings.getInstance().setFamiliaEnabled(true);
            SubtabsSettings.getInstance().setProjectViewGroupingEnabled(true);
            SubtabsSettings.getInstance().setProjectViewGroupingActive(true);
            com.intellij.testFramework.PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            SubtabsProjectViewGroupingBusyState.getInstance(getProject()).reset();
        } finally {
            super.tearDown();
        }
    }
}
