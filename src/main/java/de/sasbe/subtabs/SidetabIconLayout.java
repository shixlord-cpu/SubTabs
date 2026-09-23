package de.sasbe.subtabs;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

final class SidetabIconLayout {
    private SidetabIconLayout() {
    }

    static int iconSize() {
        return ComponentSubtabUi.tabHeight();
    }

    static int iconGap() {
        return JBUI.scale(2);
    }

    static int sidetabGapBelowIcons() {
        return iconGap() * 3;
    }

    static int reservedTopHeight(int visibleIconCount) {
        if (visibleIconCount <= 0) {
            return 0;
        }
        return iconSize() + iconGap() * 2;
    }

    static int reservedHorizontalWidth(int visibleIconCount) {
        if (visibleIconCount <= 0) {
            return 0;
        }
        return iconSize() * visibleIconCount + iconGap() * visibleIconCount;
    }

    static int reservedTopHeight(
            boolean showCollapseButton,
            boolean subtabsActive,
            boolean sidetabsExpanded
    ) {
        if (!showCollapseButton) {
            return 0;
        }
        if (sidetabsExpanded && subtabsActive) {
            return reservedTopHeight(2);
        }
        return reservedTopHeight(1);
    }

    static int reservedHorizontalWidth(
            boolean showCollapseButton,
            boolean subtabsActive,
            boolean sidetabsExpanded
    ) {
        if (!showCollapseButton) {
            return 0;
        }
        if (sidetabsExpanded && subtabsActive) {
            return reservedHorizontalWidth(2);
        }
        return reservedHorizontalWidth(1);
    }

    static int visibleCollapseIconCount() {
        SubtabsSettings settings = SubtabsSettings.getInstance();
        if (!settings.isShowCollapseButton()) {
            return 0;
        }
        if (settings.isSidetabsActive() && settings.isSubtabsActive() && !settings.isSidetabsExpanded()) {
            return 3;
        }
        if (settings.isSidetabsActive() && settings.isSubtabsActive() && settings.isSidetabsExpanded()) {
            return 2;
        }
        return 1;
    }

    static int besideColumnTopReserve() {
        SubtabsSettings settings = SubtabsSettings.getInstance();
        if (!settings.isShowCollapseButton()) {
            return 0;
        }
        return reservedTopHeight(visibleCollapseIconCount()) + sidetabGapBelowIcons();
    }

    static @NotNull Rectangle layoutSubtabsIcon(
            @NotNull FileEditor editor,
            @NotNull JComponent editorComponent,
            @NotNull JLayeredPane layeredPane,
            @NotNull Dimension size
    ) {
        return layoutAtEditorTopRight(editor, editorComponent, layeredPane, size, subtabsSlotFromRight());
    }

    static @NotNull Rectangle layoutSidetabsIcon(
            @NotNull FileEditor editor,
            @NotNull JComponent editorComponent,
            @NotNull JLayeredPane layeredPane,
            @NotNull Dimension size
    ) {
        return layoutAtEditorTopRight(editor, editorComponent, layeredPane, size, sidetabsSlotFromRight());
    }

    static @NotNull Rectangle layoutRuleSwitchIcon(
            @NotNull FileEditor editor,
            @NotNull JComponent editorComponent,
            @NotNull JLayeredPane layeredPane,
            @NotNull Dimension size
    ) {
        return layoutAtEditorTopRight(editor, editorComponent, layeredPane, size, subtabsSlotFromRight() + 1);
    }

    static int sidetabContentTopY(
            @NotNull FileEditor editor,
            @NotNull JComponent editorComponent,
            @NotNull JLayeredPane layeredPane
    ) {
        Point hostOrigin = hostOrigin(editor, editorComponent, layeredPane);
        return hostOrigin.y + besideColumnTopReserve();
    }

    static int iconRowBottomY(
            @NotNull FileEditor editor,
            @NotNull JComponent editorComponent,
            @NotNull JLayeredPane layeredPane
    ) {
        return sidetabContentTopY(editor, editorComponent, layeredPane) - sidetabGapBelowIcons();
    }

    private static int subtabsSlotFromRight() {
        SubtabsSettings settings = SubtabsSettings.getInstance();
        if (!settings.isShowCollapseButton()) {
            return 0;
        }
        return 1;
    }

    private static int sidetabsSlotFromRight() {
        return 0;
    }

    private static @Nullable SidetabEditorHost editorHost(
            @NotNull FileEditor editor,
            @NotNull JComponent editorComponent
    ) {
        if (editorComponent instanceof SidetabEditorHost host) {
            return host;
        }
        SidetabEditorHost host = SidetabEditorHost.findHost(editorComponent);
        if (host != null) {
            return host;
        }
        JComponent root = editor.getComponent();
        return root instanceof SidetabEditorHost rootHost ? rootHost : null;
    }

    private static @NotNull Point hostOrigin(
            @NotNull FileEditor editor,
            @NotNull JComponent editorComponent,
            @NotNull JLayeredPane layeredPane
    ) {
        CollapseIconRowAnchor anchor = collapseIconRowAnchor(editor, editorComponent);
        return SwingUtilities.convertPoint(anchor.originComponent(), 0, 0, layeredPane);
    }

    private static @NotNull Rectangle layoutAtEditorTopRight(
            @NotNull FileEditor editor,
            @NotNull JComponent editorComponent,
            @NotNull JLayeredPane layeredPane,
            @NotNull Dimension size,
            int slotFromRight
    ) {
        CollapseIconRowAnchor anchor = collapseIconRowAnchor(editor, editorComponent);
        Point rowRight = rowRightInLayeredPane(anchor.originComponent(), layeredPane);
        int y = iconY(editor, editorComponent, layeredPane, size.height);
        int slotWidth = size.width + iconGap();
        int x = rowRight.x - iconGap() - size.width - slotFromRight * slotWidth;
        return new Rectangle(x, y, size.width, size.height);
    }

    /**
     * Collapse icons share one row below the SubTab bar. With SideTabs attached the row anchor is
     * {@link SidetabEditorHost}. Without SideTabs the editor composite is the anchor so icons stay
     * inside the visible code pane instead of drifting past its right edge.
     */
    private static @NotNull CollapseIconRowAnchor collapseIconRowAnchor(
            @NotNull FileEditor editor,
            @NotNull JComponent editorComponent
    ) {
        SidetabEditorHost host = editorHost(editor, editorComponent);
        if (host != null) {
            return new CollapseIconRowAnchor(host, componentWidth(host));
        }
        JComponent composite = findEditorComposite(editorComponent);
        if (composite != null) {
            return new CollapseIconRowAnchor(composite, componentWidth(composite));
        }
        JComponent rowOrigin = editorContentWrapper(editorComponent);
        return new CollapseIconRowAnchor(rowOrigin, componentWidth(rowOrigin));
    }

    private static @NotNull Point rowRightInLayeredPane(
            @NotNull JComponent anchorComponent,
            @NotNull JLayeredPane layeredPane
    ) {
        return SwingUtilities.convertPoint(anchorComponent, componentWidth(anchorComponent), 0, layeredPane);
    }

    private static int componentWidth(@NotNull JComponent component) {
        if (component.getWidth() > 0) {
            return component.getWidth();
        }
        return Math.max(component.getPreferredSize().width, 0);
    }

    private static @NotNull JComponent editorContentWrapper(@NotNull JComponent editorComponent) {
        Container parent = editorComponent.getParent();
        return parent instanceof JComponent parentComponent ? parentComponent : editorComponent;
    }

    private static @Nullable JComponent findEditorComposite(@NotNull JComponent editorComponent) {
        Container current = editorComponent.getParent();
        while (current instanceof JComponent component) {
            String name = component.getClass().getName();
            if (name.endsWith("EditorCompositePanel") || name.endsWith("EditorComposite")) {
                return component;
            }
            current = component.getParent();
        }
        return null;
    }

    private static int iconY(
            @NotNull FileEditor editor,
            @NotNull JComponent editorComponent,
            @NotNull JLayeredPane layeredPane,
            int iconHeight
    ) {
        Point hostOrigin = hostOrigin(editor, editorComponent, layeredPane);
        int rowHeight = reservedTopHeight(visibleIconCountSafe());
        return hostOrigin.y + Math.max(0, (rowHeight - iconHeight) / 2);
    }

    private static int visibleIconCountSafe() {
        Application application = ApplicationManager.getApplication();
        if (application == null) {
            return 1;
        }
        return visibleCollapseIconCount();
    }

    private record CollapseIconRowAnchor(@NotNull JComponent originComponent, int width) {
    }
}
