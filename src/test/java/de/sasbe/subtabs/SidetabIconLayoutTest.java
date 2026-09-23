package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Point;
import java.beans.PropertyChangeListener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SidetabIconLayoutTest {
    @Test
    void reservedTopHeightStaysSingleRowForMultipleIcons() {
        assertEquals(SidetabIconLayout.reservedTopHeight(2), SidetabIconLayout.reservedTopHeight(1));
        assertTrue(SidetabIconLayout.reservedHorizontalWidth(2) > SidetabIconLayout.reservedHorizontalWidth(1));
        assertEquals(0, SidetabIconLayout.reservedTopHeight(0));
    }

    @Test
    void sidetabLayoutModeLabelsMatchPresentation() {
        assertEquals("Als Label", SidetabLayoutMode.BESIDE.label());
        assertEquals("Als leere Balken", SidetabLayoutMode.OVERLAY.label());
    }

    @Test
    void reservedTopHeightUsesCollapseSettings() {
        int both = SidetabIconLayout.reservedTopHeight(true, true, true);
        int sidetabsOnly = SidetabIconLayout.reservedTopHeight(true, false, true);
        assertEquals(both, sidetabsOnly);
        assertTrue(SidetabIconLayout.reservedHorizontalWidth(true, true, true)
                > SidetabIconLayout.reservedHorizontalWidth(true, false, true));
        assertEquals(0, SidetabIconLayout.reservedTopHeight(false, true, true));
    }

    @Test
    void sidetabsIconSitsToTheRightOfSubtabsIcon() {
        java.awt.Dimension iconSize = new java.awt.Dimension(SidetabIconLayout.iconSize(), SidetabIconLayout.iconSize());
        java.awt.Rectangle sidetabs = new java.awt.Rectangle(548, 8, iconSize.width, iconSize.height);
        java.awt.Rectangle subtabs = new java.awt.Rectangle(
                sidetabs.x - SidetabIconLayout.iconGap() - iconSize.width,
                sidetabs.y,
                iconSize.width,
                iconSize.height
        );

        assertEquals(subtabs.x + subtabs.width + SidetabIconLayout.iconGap(), sidetabs.x);
    }

    @Test
    void iconsStayAtEditorTopRightWhenSidetabsAreOnTheLeft() {
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setBounds(0, 0, 600, 400);

        SidetabEditorHost host = new SidetabEditorHost();
        host.setBounds(0, 0, 600, 400);
        layeredPane.add(host);

        JPanel code = new JPanel();
        code.setPreferredSize(new java.awt.Dimension(520, 400));
        host.add(code, BorderLayout.CENTER);

        JPanel side = new JPanel();
        side.setPreferredSize(new java.awt.Dimension(80, 400));
        host.add(side, BorderLayout.WEST);
        host.setSize(600, 400);
        host.doLayout();

        TestFileEditor editor = new TestFileEditor(host);
        java.awt.Dimension iconSize = new java.awt.Dimension(SidetabIconLayout.iconSize(), SidetabIconLayout.iconSize());
        java.awt.Rectangle icon = SidetabIconLayout.layoutSidetabsIcon(
                editor,
                code,
                layeredPane,
                iconSize
        );

        Point hostOrigin = SwingUtilities.convertPoint(host, 0, 0, layeredPane);
        int hostRight = hostOrigin.x + host.getWidth();
        assertEquals(hostRight - SidetabIconLayout.iconGap(), icon.x + icon.width, 1,
                "Icons must stay at the editor top-right even when SideTabs are on the left");
        assertTrue(icon.x > side.getWidth(),
                "Icons must not follow the left SideTabs column");
    }

    @Test
    void iconsStayAtEditorTopRightWhenSidetabsAreOnTheRight() {
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setBounds(0, 0, 600, 400);

        SidetabEditorHost host = new SidetabEditorHost();
        host.setBounds(0, 0, 600, 400);
        layeredPane.add(host);

        JPanel code = new JPanel();
        code.setPreferredSize(new java.awt.Dimension(520, 400));
        host.add(code, BorderLayout.CENTER);

        JPanel side = new JPanel();
        side.setPreferredSize(new java.awt.Dimension(80, 400));
        host.add(side, BorderLayout.EAST);
        host.setSize(600, 400);
        host.doLayout();

        TestFileEditor editor = new TestFileEditor(host);
        java.awt.Dimension iconSize = new java.awt.Dimension(SidetabIconLayout.iconSize(), SidetabIconLayout.iconSize());
        java.awt.Rectangle sidetabs = SidetabIconLayout.layoutSidetabsIcon(
                editor,
                code,
                layeredPane,
                iconSize
        );

        Point hostOrigin = SwingUtilities.convertPoint(host, 0, 0, layeredPane);
        int hostRight = hostOrigin.x + host.getWidth();
        assertEquals(hostRight - SidetabIconLayout.iconGap(), sidetabs.x + sidetabs.width, 1,
                "SideTabs icon must stay at the fixed editor top-right even when SideTabs are on the right");
    }

    @Test
    void iconsStayInsideEditorCompositeWhenContentWrapperIsInset() {
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setBounds(0, 0, 600, 400);

        EditorCompositePanel composite = new EditorCompositePanel();
        composite.setLayout(new BorderLayout());
        composite.setBounds(0, 0, 600, 400);
        layeredPane.add(composite);

        JPanel codeWrapper = new JPanel(new BorderLayout());
        codeWrapper.setBounds(40, 0, 560, 400);
        composite.add(codeWrapper, BorderLayout.CENTER);

        JPanel code = new JPanel();
        code.setPreferredSize(new java.awt.Dimension(520, 400));
        codeWrapper.add(code, BorderLayout.CENTER);
        composite.setSize(600, 400);
        composite.doLayout();
        codeWrapper.setSize(560, 400);

        TestFileEditor editor = new TestFileEditor(code);
        java.awt.Dimension iconSize = new java.awt.Dimension(SidetabIconLayout.iconSize(), SidetabIconLayout.iconSize());
        java.awt.Rectangle sidetabs = SidetabIconLayout.layoutSidetabsIcon(
                editor,
                code,
                layeredPane,
                iconSize
        );

        Point compositeRight = SwingUtilities.convertPoint(composite, composite.getWidth(), 0, layeredPane);
        assertEquals(
                compositeRight.x - SidetabIconLayout.iconGap(),
                sidetabs.x + sidetabs.width,
                1,
                "Icons must anchor to the editor composite right edge, not wrapper offset + composite width"
        );
    }

    @Test
    void iconPositionDoesNotShiftWhenSidetabColumnWidthChanges() {
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setBounds(0, 0, 600, 400);

        SidetabEditorHost host = new SidetabEditorHost();
        host.setBounds(0, 0, 600, 400);
        layeredPane.add(host);

        JPanel code = new JPanel();
        code.setPreferredSize(new java.awt.Dimension(520, 400));
        host.add(code, BorderLayout.CENTER);

        JPanel side = new JPanel();
        side.setPreferredSize(new java.awt.Dimension(80, 400));
        host.add(side, BorderLayout.EAST);
        host.setSize(600, 400);
        host.doLayout();

        TestFileEditor editor = new TestFileEditor(host);
        java.awt.Dimension iconSize = new java.awt.Dimension(SidetabIconLayout.iconSize(), SidetabIconLayout.iconSize());
        java.awt.Rectangle narrowColumnIcon = SidetabIconLayout.layoutSidetabsIcon(
                editor,
                code,
                layeredPane,
                iconSize
        );

        side.setPreferredSize(new java.awt.Dimension(160, 400));
        host.setSize(600, 400);
        host.doLayout();

        java.awt.Rectangle wideColumnIcon = SidetabIconLayout.layoutSidetabsIcon(
                editor,
                code,
                layeredPane,
                iconSize
        );

        assertEquals(narrowColumnIcon, wideColumnIcon,
                "Icon position must not move when the SideTabs column width changes");
    }

    private static final class EditorCompositePanel extends JPanel {
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