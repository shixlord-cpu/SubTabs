package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.mock.MockVirtualFile;
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
    public void testBesideColumnUsesConfiguredDefaultWidth() {
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

        int expected = SubtabsSettings.getInstance().getSidetabBesideColumnWidth();
        assertEquals(expected, panel.getPreferredSize().width);
    }

    public void testBesideColumnWidthFollowsUserResizeSetting() {
        SubtabsSettings settings = SubtabsSettings.getInstance();
        int originalWidth = settings.getSidetabBesideColumnWidth();
        try {
            settings.setSidetabBesideColumnWidth(originalWidth + com.intellij.util.ui.JBUI.scale(40));
            SidetabBarPanel panel = new SidetabBarPanel(getProject(), new TestFileEditor(new JPanel()));
            panel.bind(
                    List.of(new SidetabSection("Configuration", 0, 20)),
                    SidetabLayoutMode.BESIDE,
                    true,
                    null,
                    0,
                    20
            );
            assertEquals(settings.getSidetabBesideColumnWidth(), panel.getPreferredSize().width);
        } finally {
            settings.setSidetabBesideColumnWidth(originalWidth);
        }
    }

    public void testPerFileBesideColumnWidthOverridesGlobalOnlyForThatFile() {
        VirtualFile fileA = new MockVirtualFile("per-file-width-a.html");
        VirtualFile fileB = new MockVirtualFile("per-file-width-b.html");
        int globalWidth = SubtabsSettings.getInstance().getSidetabBesideColumnWidth();
        int fileAWidth = globalWidth + com.intellij.util.ui.JBUI.scale(55);

        ComponentSubtabsScopedVisibility.setSidetabBesideColumnWidthForFile(getProject(), fileA, fileAWidth);

        SidetabBarPanel panelA = new SidetabBarPanel(getProject(), new TestFileEditor(new JPanel(), fileA));
        SidetabBarPanel panelB = new SidetabBarPanel(getProject(), new TestFileEditor(new JPanel(), fileB));
        List<SidetabSection> sections = List.of(new SidetabSection("Configuration", 0, 20));
        panelA.bind(sections, SidetabLayoutMode.BESIDE, true, null, 0, 20);
        panelB.bind(sections, SidetabLayoutMode.BESIDE, true, null, 0, 20);

        assertEquals(fileAWidth, panelA.getPreferredSize().width);
        assertEquals(globalWidth, panelB.getPreferredSize().width);

        ComponentSubtabsScopedVisibility.clearSidetabBesideColumnWidthForFile(getProject(), fileA);
        panelA.applyLiveBesideColumnWidth(globalWidth);
        assertEquals(globalWidth, panelA.getPreferredSize().width);
        assertEquals(
                globalWidth,
                ComponentSubtabsScopedVisibility.sidetabBesideColumnWidthForFile(getProject(), fileA)
        );
    }

    public void testLiveBesideColumnWidthUpdatesPanelWidthImmediately() {
        SidetabBarPanel panel = new SidetabBarPanel(getProject(), new TestFileEditor(new JPanel()));
        panel.bind(
                List.of(new SidetabSection("Configuration", 0, 20)),
                SidetabLayoutMode.BESIDE,
                true,
                null,
                0,
                20
        );
        int base = panel.getPreferredSize().width;
        int wider = base + com.intellij.util.ui.JBUI.scale(40);
        panel.applyLiveBesideColumnWidth(wider);
        assertEquals(wider, panel.getPreferredSize().width);
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

            assertEquals("SideTab column width stays user-controlled when font style changes", standardWidth, monospaceWidth);
        } finally {
            settings.setTabFontStyle(original);
        }
    }

    private static final class TestFileEditor implements FileEditor {
        private final JComponent component;
        private final @Nullable VirtualFile file;

        private TestFileEditor(JComponent component) {
            this(component, null);
        }

        private TestFileEditor(JComponent component, @Nullable VirtualFile file) {
            this.component = component;
            this.file = file;
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
            return file;
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
