package de.sasbe.subtabs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.JFrame;
import javax.swing.JTree;
import java.awt.Component;

public class SubtabsProjectViewGroupingOverlayTest extends LightPlatformTestCase {
    public void testGroupingButtonIsVisibleInsideProjectTreeScrollPane() {
        SubtabsSettings.getInstance().setShowCollapseButton(true);
        SubtabsSettings.getInstance().setGroupRelatedFilesInProjectView(true);

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
        SubtabsSettings.getInstance().setShowCollapseButton(true);

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

    public void testFamiliaToggleRemovesAndRestoresGroupingButton() {
        SubtabsSettings.getInstance().setFamiliaEnabled(true);
        SubtabsSettings.getInstance().setShowCollapseButton(true);

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

            SubtabsSettings.getInstance().setFamiliaEnabled(false);
            SubtabsProjectViewGroupingOverlay.disposeAll(getProject());
            scrollPane.validate();

            assertNull(SubtabsProjectViewGroupingOverlay.installedButtonForTree(tree));
            assertFalse(scrollPane.getViewport().getView() instanceof ProjectViewTreeOverlayPanel);

            SubtabsSettings.getInstance().setFamiliaEnabled(true);
            SubtabsProjectViewGroupingOverlay.attachForTest(getProject(), scrollPane);
            scrollPane.validate();

            Component restored = SubtabsProjectViewGroupingOverlay.installedButtonForTree(tree);
            assertNotNull("grouping button must return after Familia re-enable", restored);
            assertTrue("restored grouping button must be visible", restored.isVisible());

            frame.dispose();
        });
    }

    public void testGroupingButtonStaysVisibleWhenGroupingIsDisabled() {
        SubtabsSettings.getInstance().setFamiliaEnabled(true);
        SubtabsSettings.getInstance().setGroupRelatedFilesInProjectView(false);

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
            assertNotNull("disabled grouping must still show the control", button);
            assertTrue("disabled grouping control must stay visible", button.isVisible());
            assertEquals(SubtabsIcons.GROUPING_COLLAPSED, button.getIcon());

            frame.dispose();
        });
    }

    public void testGroupingButtonShowsLoadingSpinnerWhileProjectViewRebuilds() {
        SubtabsSettings.getInstance().setShowCollapseButton(true);

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

            SubtabsProjectViewGroupingBusyState.getInstance(getProject()).begin();
            assertTrue(button.isLoading());
            assertFalse(button.isEnabled());

            SubtabsProjectViewGroupingBusyState.getInstance(getProject()).end();
            assertFalse(button.isLoading());
            assertTrue(button.isEnabled());

            frame.dispose();
        });
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            com.intellij.testFramework.PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            SubtabsProjectViewGroupingBusyState.getInstance(getProject()).reset();
            SubtabsSettings.getInstance().setFamiliaEnabled(true);
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
