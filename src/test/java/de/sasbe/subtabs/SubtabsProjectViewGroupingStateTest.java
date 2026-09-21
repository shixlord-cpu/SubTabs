package de.sasbe.subtabs;

import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;

public class SubtabsProjectViewGroupingStateTest extends LightPlatformTestCase {
    public void testGroupingStaysEnabledWhenSubtabsAreCollapsed() {
        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setFamiliaEnabled(true);
        settings.setGroupRelatedFilesInProjectView(true);
        settings.setSubtabsActive(false);

        assertTrue(SubtabProjectViewGrouping.isEnabled());
        assertTrue(SubtabsProjectViewGroupingState.getInstance(getProject()).isCollapsed() == false);
    }

    public void testToggleUpdatesGroupingSetting() {
        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setGroupRelatedFilesInProjectView(true);

        SubtabsProjectViewGroupingState state = SubtabsProjectViewGroupingState.getInstance(getProject());
        state.toggle(getProject());
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertFalse(settings.isGroupRelatedFilesInProjectView());
        assertTrue(state.isCollapsed());

        state.toggle(getProject());
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertTrue(settings.isGroupRelatedFilesInProjectView());
        assertFalse(state.isCollapsed());
    }

    public void testCollapsingSubtabsDoesNotChangeProjectViewGrouping() {
        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setFamiliaEnabled(true);
        settings.setGroupRelatedFilesInProjectView(true);
        settings.setSubtabsActive(true);

        SubtabsCollapseState.getInstance(getProject()).toggle(getProject());

        assertFalse(settings.isSubtabsActive());
        assertTrue(settings.isGroupRelatedFilesInProjectView());
        assertTrue(SubtabProjectViewGrouping.isEnabled());
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            SubtabsSettings.getInstance().setFamiliaEnabled(true);
            com.intellij.testFramework.PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            SubtabsProjectViewGroupingBusyState.getInstance(getProject()).reset();
        } finally {
            super.tearDown();
        }
    }
}
