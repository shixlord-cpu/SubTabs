package com.zayax.tabz;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.JFrame;
import javax.swing.JTree;
import java.awt.Component;

public class SubtabsProjectViewGroupingOverlayTest extends LightPlatformTestCase {
    public void testGroupingButtonIsVisibleInsideProjectTreeScrollPane() {
        TabzSettings.getInstance().setProjectViewGroupingEnabled(true);
        TabzSettings.getInstance().setProjectViewGroupingActive(true);

        ApplicationManager.getApplication().invokeAndWait(() -> {
            JTree tree = new JTree();
            JBScrollPane scrollPane = new JBScrollPane(tree);
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setContentPane(scrollPane);
            frame.setSize(420, 320);
            frame.setVisible(true);
            frame.validate();

            SubtabsProjectViewGroupingOverlay.attachForTest(getProject(), scrollPane);
            scrollPane.validate();

            Component button = SubtabsProjectViewGroupingOverlay.installedButtonForTree(tree);
            assertNotNull("grouping button must be installed", button);
            int width = Math.max(button.getWidth(), button.getPreferredSize().width);
            int height = Math.max(button.getHeight(), button.getPreferredSize().height);
            assertTrue("grouping button must be visible", button.isVisible());
            assertTrue("grouping button must have width: " + width, width > 0);
            assertTrue("grouping button must have height: " + height, height > 0);
            assertTrue(
                    "grouping button must live inside the project tree viewport overlay",
                    isDescendantOf(scrollPane, button)
            );
            assertTrue("grouping button must sit on the first tree row", button.getY() == 0);

            scrollPane.setSize(180, scrollPane.getHeight());
            scrollPane.validate();
            int viewportWidth = scrollPane.getViewport().getExtentSize().width;
            assertTrue(
                    "grouping button must stay visible beside the scrollbar in narrow project views",
                    button.getX() + button.getWidth() <= viewportWidth
            );

            frame.dispose();
        });
    }

    public void testProjectTreeRemainsScrollableWithGroupingOverlay() {
        TabzSettings.getInstance().setProjectViewGroupingEnabled(true);

        ApplicationManager.getApplication().invokeAndWait(() -> {
            JTree tree = new JTree();
            JBScrollPane scrollPane = new JBScrollPane(tree);
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setContentPane(scrollPane);
            frame.setSize(420, 320);
            frame.setVisible(true);
            frame.validate();

            SubtabsProjectViewGroupingOverlay.attachForTest(getProject(), scrollPane);
            scrollPane.validate();

            assertTrue(
                    "project tree must stay scrollable with overlay installed",
                    scrollPane.getVerticalScrollBar().getMaximum() > 0
            );

            frame.dispose();
        });
    }

    public void testTabzToggleRemovesAndRestoresGroupingButton() {
        TabzSettings.getInstance().setTabzEnabled(true);
        TabzSettings.getInstance().setProjectViewGroupingEnabled(true);

        ApplicationManager.getApplication().invokeAndWait(() -> {
            JTree tree = new JTree();
            JBScrollPane scrollPane = new JBScrollPane(tree);
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setContentPane(scrollPane);
            frame.setSize(420, 320);
            frame.setVisible(true);
            frame.validate();

            SubtabsProjectViewGroupingOverlay.attachForTest(getProject(), scrollPane);
            scrollPane.validate();
            assertNotNull(SubtabsProjectViewGroupingOverlay.installedButtonForTree(tree));

            TabzSettings.getInstance().setTabzEnabled(false);
            SubtabsProjectViewGroupingOverlay.disposeAll(getProject());
            scrollPane.validate();

            assertNull(SubtabsProjectViewGroupingOverlay.installedButtonForTree(tree));
            assertFalse(scrollPane.getViewport().getView() instanceof ProjectViewTreeOverlayPanel);

            TabzSettings.getInstance().setTabzEnabled(true);
            SubtabsProjectViewGroupingOverlay.attachForTest(getProject(), scrollPane);
            scrollPane.validate();

            Component restored = SubtabsProjectViewGroupingOverlay.installedButtonForTree(tree);
            assertNotNull("grouping button must return after Tabz re-enable", restored);
            assertTrue("restored grouping button must be visible", restored.isVisible());

            frame.dispose();
        });
    }

    public void testGroupingButtonStaysVisibleWhenGroupingIsCollapsed() {
        TabzSettings.getInstance().setTabzEnabled(true);
        TabzSettings.getInstance().setProjectViewGroupingEnabled(true);
        TabzSettings.getInstance().setProjectViewGroupingActive(false);

        ApplicationManager.getApplication().invokeAndWait(() -> {
            JTree tree = new JTree();
            JBScrollPane scrollPane = new JBScrollPane(tree);
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setContentPane(scrollPane);
            frame.setSize(420, 320);
            frame.setVisible(true);
            frame.validate();

            SubtabsProjectViewGroupingOverlay.attachForTest(getProject(), scrollPane);
            scrollPane.validate();

            ComponentSubtabIconButton button = SubtabsProjectViewGroupingOverlay.installedGroupingButtonForTree(tree);
            assertNotNull("collapsed grouping must still show the control", button);
            assertTrue("collapsed grouping control must stay visible", button.isVisible());
            assertEquals(SubtabsIcons.GROUPING_COLLAPSED, button.getIcon());

            frame.dispose();
        });
    }

    public void testGroupingButtonHiddenWhenGruppierungDisabled() {
        TabzSettings.getInstance().setTabzEnabled(true);
        TabzSettings.getInstance().setProjectViewGroupingEnabled(false);
        TabzSettings.getInstance().setProjectViewGroupingActive(true);

        ApplicationManager.getApplication().invokeAndWait(() -> {
            JTree tree = new JTree();
            JBScrollPane scrollPane = new JBScrollPane(tree);
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setContentPane(scrollPane);
            frame.setSize(420, 320);
            frame.setVisible(true);
            frame.validate();

            SubtabsProjectViewGroupingOverlay.attachForTest(getProject(), scrollPane);
            scrollPane.validate();

            assertNull(
                    "Gruppierung off must hide the project-view control",
                    SubtabsProjectViewGroupingOverlay.installedButtonForTree(tree)
            );

            frame.dispose();
        });
    }

    public void testGroupingButtonShowsLoadingSpinnerWhileProjectViewRebuilds() {
        TabzSettings.getInstance().setProjectViewGroupingEnabled(true);

        ApplicationManager.getApplication().invokeAndWait(() -> {
            JTree tree = new JTree();
            JBScrollPane scrollPane = new JBScrollPane(tree);
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setContentPane(scrollPane);
            frame.setSize(420, 320);
            frame.setVisible(true);
            frame.validate();

            SubtabsProjectViewGroupingOverlay.attachForTest(getProject(), scrollPane);
            scrollPane.validate();

            ComponentSubtabIconButton button = SubtabsProjectViewGroupingOverlay.installedGroupingButtonForTree(tree);
            assertNotNull(button);

            SubtabProjectViewGroupingBusyState.getInstance(getProject()).begin();
            assertTrue(button.isLoading());
            assertFalse(button.isEnabled());

            SubtabProjectViewGroupingBusyState.getInstance(getProject()).end();
            assertFalse(button.isLoading());
            assertTrue(button.isEnabled());

            frame.dispose();
        });
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            com.intellij.testFramework.PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            SubtabProjectViewGroupingBusyState.getInstance(getProject()).reset();
            TabzSettings.getInstance().setTabzEnabled(true);
        } finally {
            super.tearDown();
        }
    }

    private static boolean isDescendantOf(Component ancestor, Component component) {
        Component current = component;
        while (current != null) {
            if (current == ancestor) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
}
