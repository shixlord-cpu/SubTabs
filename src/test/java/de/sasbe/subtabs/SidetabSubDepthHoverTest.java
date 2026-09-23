package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * TESTE: production SidetabBarPanel hover path must keep sub-depth dots fixed.
 */
public class SidetabSubDepthHoverTest extends HeavyPlatformTestCase {
    public void testSubDepthDotsAndButtonOriginStayFixedOnHover() {
        SidetabBarPanel panel = createPanelWithNestedSections(getProject());
        layoutPanel(panel);

        for (int index = 1; index <= 2; index++) {
            JToggleButton button = panel.buttonAt(index);
            assertNotNull("section button " + index, button);

            int dotXBefore = dotColumnInPanel(panel, button);
            int buttonXBefore = button.getLocation().x;

            hoverButton(button);
            ComponentSubtabUi.refreshButton(button);
            layoutPanel(panel);

            assertEquals("dot column must not move on hover for section " + index, dotXBefore, dotColumnInPanel(panel, button));
            assertEquals("button origin must not move on hover for section " + index, buttonXBefore, button.getLocation().x);
        }
    }

    public void testRefreshDuringHoverKeepsManualHoverBackground() {
        SidetabBarPanel panel = createPanelWithNestedSections(getProject());
        layoutPanel(panel);
        JToggleButton button = panel.buttonAt(1);
        assertNotNull(button);

        hoverButton(button);
        ComponentSubtabUi.refreshButton(button);

        assertTrue(Boolean.TRUE.equals(button.getClientProperty(ComponentSubtabUi.MANUAL_HOVER_KEY)));
    }

    private static @NotNull SidetabBarPanel createPanelWithNestedSections(@NotNull com.intellij.openapi.project.Project project) {
        SidetabBarPanel panel = new SidetabBarPanel(project, new TestFileEditor(new JPanel()));
        panel.bind(
                List.of(
                        new SidetabSection("Main", 0, 10, 0, true),
                        new SidetabSection("Navigation", 10, 20, 1, true),
                        new SidetabSection("Detail", 20, 30, 2, true)
                ),
                SidetabLayoutMode.BESIDE,
                true,
                null,
                0,
                30
        );
        return panel;
    }

    private static void layoutPanel(@NotNull SidetabBarPanel panel) {
        panel.setSize(panel.getPreferredSize());
        panel.validate();
        panel.doLayout();
    }

    private static void hoverButton(@NotNull JToggleButton button) {
        button.dispatchEvent(new MouseEvent(
                button,
                MouseEvent.MOUSE_ENTERED,
                System.currentTimeMillis(),
                0,
                Math.max(1, button.getWidth() / 2),
                Math.max(1, button.getHeight() / 2),
                0,
                false
        ));
    }

    private static int dotColumnInPanel(@NotNull SidetabBarPanel panel, @NotNull JToggleButton button) {
        Point dotInPanel = SwingUtilities.convertPoint(
                button,
                new Point(SidetabBarPanel.subDepthDotBaseX(), button.getHeight() / 2),
                panel
        );
        return dotInPanel.x;
    }

    private static final class TestFileEditor implements FileEditor {
        private final JComponent component;

        private TestFileEditor(JComponent component) {
            this.component = component;
        }

        @Override
        public @NotNull JComponent getComponent() {
            return component;
        }

        @Override
        public @Nullable JComponent getPreferredFocusedComponent() {
            return component;
        }

        @Override
        public @NotNull String getName() {
            return "test";
        }

        @Override
        public void setState(@NotNull FileEditorState state) {
        }

        @Override
        public boolean isModified() {
            return false;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public void selectNotify() {
        }

        @Override
        public void deselectNotify() {
        }

        @Override
        public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
        }

        @Override
        public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
        }

        @Override
        public @Nullable <T> T getUserData(@NotNull Key<T> key) {
            return null;
        }

        @Override
        public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
        }

        @Override
        public @Nullable VirtualFile getFile() {
            return null;
        }

        @Override
        public @NotNull FileEditorLocation getCurrentLocation() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void dispose() {
        }
    }
}
