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
import java.beans.PropertyChangeListener;
import java.util.List;

public class SidetabBesideLayoutTest extends HeavyPlatformTestCase {
    public void testBesideColumnWidthMatchesLongestSectionLabel() {
        SidetabBarPanel panel = new SidetabBarPanel(getProject(), new TestFileEditor(new JPanel()));
        panel.bind(
                List.of(
                        new SidetabSection("Head", 0, 10),
                        new SidetabSection("Configuration", 10, 20)
                ),
                SidetabLayoutMode.BESIDE,
                true,
                null,
                0,
                20
        );

        int expected = ComponentSubtabUi.preferredLabelWidth("Configuration");
        assertEquals(expected, panel.getPreferredSize().width);
        assertTrue(expected > ComponentSubtabUi.preferredLabelWidth("Head"));
    }

    public void testBesideColumnWidthUpdatesImmediatelyWhenTextSizeChanges() {
        SubtabsSettings settings = SubtabsSettings.getInstance();
        int originalTextSize = settings.getTextSizePercent();
        try {
            settings.setTextSizePercent(60);
            SidetabBarPanel panel = new SidetabBarPanel(getProject(), new TestFileEditor(new JPanel()));
            panel.bind(
                    List.of(new SidetabSection("Configuration", 0, 20)),
                    SidetabLayoutMode.BESIDE,
                    true,
                    null,
                    0,
                    20
            );
            int narrow = panel.getPreferredSize().width;

            settings.setTextSizePercent(100);
            panel.refreshAppearance();
            int wide = panel.getPreferredSize().width;

            assertTrue("SideTab column must grow with text size", wide > narrow);
            assertEquals(ComponentSubtabUi.preferredLabelWidth("Configuration"), wide);
        } finally {
            settings.setTextSizePercent(originalTextSize);
        }
    }

    public void testBesideColumnWidthUpdatesImmediatelyWhenFontStyleChanges() {
        SubtabsSettings settings = SubtabsSettings.getInstance();
        TabFontStyle original = settings.getTabFontStyle();
        try {
            settings.setTabFontStyle(TabFontStyle.IDE_STANDARD);
            SidetabBarPanel panel = new SidetabBarPanel(getProject(), new TestFileEditor(new JPanel()));
            panel.bind(
                    List.of(new SidetabSection("Configuration", 0, 20)),
                    SidetabLayoutMode.BESIDE,
                    true,
                    null,
                    0,
                    20
            );
            int standardWidth = panel.getPreferredSize().width;

            settings.setTabFontStyle(TabFontStyle.MONOSPACED);
            panel.refreshAppearance();
            int monospaceWidth = panel.getPreferredSize().width;

            assertTrue("SideTab column must reflow when font style changes", monospaceWidth != standardWidth);
        } finally {
            settings.setTabFontStyle(original);
        }
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
