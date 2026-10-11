package de.sasbe.subtabs;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.JBUI.CurrentTheme;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JWindow;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.IllegalComponentStateException;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Shared reorder-drag visuals (gap slot, ghost, single-row layout) for horizontal tab strips.
 */
final class ComponentSubtabReorderStripSupport {
    @FunctionalInterface
    interface ButtonOrder {
        @NotNull List<JToggleButton> orderedButtons(@NotNull Map<VirtualFile, JToggleButton> buttonsByFile);
    }

    private final @NotNull Map<VirtualFile, JToggleButton> buttonsByFile;
    private final @NotNull ButtonOrder buttonOrder;
    private final int hgap;
    private final int vgap;
    private final @NotNull TabStrip tabsHost;

    private int reorderDropIndex = -1;
    private @Nullable VirtualFile reorderDraggedFile;
    private @Nullable Point reorderDragPointer;
    private @Nullable BufferedImage reorderDragImage;
    private @Nullable JComponent reorderDragGhost;
    private @Nullable JWindow reorderGhostWindow;
    private int reorderDraggedTabWidth;
    private @Nullable List<Integer> reorderLayoutTabWidths;
    private @Nullable List<JToggleButton> reorderVisualOrder;
    private @Nullable MouseWheelListener wheelForwarder;

    ComponentSubtabReorderStripSupport(
            @NotNull Map<VirtualFile, JToggleButton> buttonsByFile,
            @NotNull ButtonOrder buttonOrder,
            int hgap,
            int vgap
    ) {
        this.buttonsByFile = buttonsByFile;
        this.buttonOrder = buttonOrder;
        this.hgap = hgap;
        this.vgap = vgap;
        this.tabsHost = new TabStrip();
        tabsHost.setBackground(UIUtil.getPanelBackground());
        tabsHost.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        tabsHost.setLayout(new SingleRowLayout());
    }

    void setWheelForwarder(@Nullable MouseWheelListener wheelForwarder) {
        this.wheelForwarder = wheelForwarder;
    }

    @NotNull JPanel tabsHost() {
        return tabsHost;
    }

    void syncTabsHostChildren() {
        List<JToggleButton> orderedButtons = orderedVisibleTabButtons();
        if (orderedButtons.isEmpty()) {
            return;
        }
        JComponent ghost = reorderDragGhost;
        if (ghost != null) {
            tabsHost.remove(ghost);
        }
        for (JToggleButton button : orderedButtons) {
            tabsHost.remove(button);
        }
        for (JToggleButton button : orderedButtons) {
            tabsHost.add(button);
        }
        if (ghost != null) {
            tabsHost.add(ghost);
            tabsHost.setComponentZOrder(ghost, 0);
        }
    }

    void clearReorderPreview() {
        updateReorderDragState(-1, null, null);
    }

    void updateReorderDragState(
            int index,
            @Nullable VirtualFile draggedFile,
            @Nullable Point pointerInTabsHost
    ) {
        if (reorderDropIndex == index
                && reorderDraggedFile == draggedFile
                && pointsEqual(reorderDragPointer, pointerInTabsHost)) {
            return;
        }

        reorderDropIndex = index;
        reorderDraggedFile = draggedFile;
        reorderDragPointer = pointerInTabsHost;
        if (draggedFile == null || pointerInTabsHost == null) {
            reorderDragImage = null;
            reorderDraggedTabWidth = 0;
            reorderLayoutTabWidths = null;
            reorderVisualOrder = null;
            reorderGapReset();
            hideReorderDragGhost();
        } else {
            JToggleButton source = buttonsByFile.get(draggedFile);
            if (source != null) {
                reorderDraggedTabWidth = Math.max(source.getPreferredSize().width, source.getWidth());
                if (reorderDragImage == null) {
                    reorderDragImage = snapshotButton(source);
                    reorderLayoutTabWidths = List.copyOf(tabWidthsForDrag(draggedFile));
                }
            }
            ensureReorderDragGhost();
        }
        captureVisualOrderIfNeeded();
        syncDraggedTabBarVisibility();
        tabsHost.revalidate();
        tabsHost.doLayout();
        tabsHost.repaint();
        if (pointerInTabsHost != null) {
            SwingUtilities.invokeLater(this::updateReorderDragGhostBounds);
        }
    }

    int resolveReorderDropIndex(@NotNull Point pointerInTabsHost, @NotNull VirtualFile draggedFile) {
        captureVisualOrderIfNeeded();
        int draggedIndex = tabIndexForFile(draggedFile);
        int fromBounds = dropIndexFromCurrentBounds(pointerInTabsHost.x, draggedIndex);
        if (fromBounds >= 0) {
            return fromBounds;
        }
        return ComponentSubtabReorderLayout.dropIndexForPointer(
                pointerInTabsHost.x,
                tabWidthsForDrag(draggedFile),
                draggedIndex,
                hgap,
                tabsHost.getInsets().left
        );
    }

    int tabIndexForFile(@NotNull VirtualFile file) {
        JToggleButton button = buttonsByFile.get(file);
        return button == null ? -1 : orderedVisibleTabButtons().indexOf(button);
    }

    @TestOnly
    int reorderGapXForTests() {
        return reorderGapX;
    }

    @TestOnly
    int reorderGapWidthForTests() {
        return reorderGapWidth;
    }

    @TestOnly
    void applyReorderLayoutForTests(@NotNull VirtualFile draggedFile, int dropIndex, int hostWidth) {
        layOutTabsForTests(hostWidth);
        reorderDraggedFile = draggedFile;
        reorderDropIndex = dropIndex;
        reorderDragPointer = new Point(0, 0);
        JToggleButton source = buttonsByFile.get(draggedFile);
        reorderDraggedTabWidth = tabWidthForLayout(source);
        reorderLayoutTabWidths = List.copyOf(tabWidthsForDrag(draggedFile));
        List<JToggleButton> orderedButtons = orderedVisibleTabButtons();
        JToggleButton dragged = buttonsByFile.get(draggedFile);
        int draggedIndex = orderedButtons.indexOf(dragged);
        ComponentSubtabReorderLayout.Result layout = ComponentSubtabReorderLayout.layout(
                orderedTabWidths(),
                draggedIndex,
                reorderDropIndex,
                reorderDraggedTabWidth,
                hgap,
                tabsHost.getInsets().left
        );
        reorderGapX = layout.gapX();
        reorderGapWidth = layout.gapWidth();
        Insets insets = tabsHost.getInsets();
        int y = insets.top + vgap;
        int height = ComponentSubtabUi.barRowHeight();
        for (ComponentSubtabReorderLayout.TabPlacement placement : layout.placements()) {
            JToggleButton button = orderedButtons.get(placement.tabIndex());
            button.setBounds(placement.x(), y, placement.width(), height);
        }
        if (dragged != null) {
            dragged.setBounds(-100000, y, 0, height);
        }
        syncDraggedTabBarVisibility();
        tabsHost.repaint();
    }

    private int reorderGapX = -1;
    private int reorderGapWidth;

    private void reorderGapReset() {
        reorderGapX = -1;
        reorderGapWidth = 0;
    }

    private void layOutTabsForTests(int width) {
        int height = ComponentSubtabUi.barRowHeight() + tabsHost.getInsets().top + tabsHost.getInsets().bottom + 2 * vgap;
        tabsHost.setSize(width, height);
        tabsHost.validate();
        tabsHost.doLayout();
    }

    private void syncDraggedTabBarVisibility() {
        boolean hideDraggedInBar = reorderDraggedFile != null && reorderDragPointer != null;
        for (var entry : buttonsByFile.entrySet()) {
            JToggleButton button = entry.getValue();
            boolean hide = hideDraggedInBar && entry.getKey().equals(reorderDraggedFile);
            if (hide) {
                button.putClientProperty(ComponentSubtabUi.REORDER_HIDDEN_KEY, Boolean.TRUE);
            } else {
                button.putClientProperty(ComponentSubtabUi.REORDER_HIDDEN_KEY, null);
            }
        }
    }

    private int dropIndexFromCurrentBounds(int pointerX, int draggedIndex) {
        if (reorderGapX >= 0
                && reorderGapWidth > 0
                && pointerX >= reorderGapX
                && pointerX < reorderGapX + reorderGapWidth) {
            return reorderDropIndex;
        }

        List<ComponentSubtabReorderLayout.BoundsSlot> slots = new ArrayList<>();
        List<JToggleButton> orderedButtons = orderedVisibleTabButtons();
        for (int tabIndex = 0; tabIndex < orderedButtons.size(); tabIndex++) {
            JToggleButton button = orderedButtons.get(tabIndex);
            if (ComponentSubtabUi.isReorderHidden(button)) {
                continue;
            }
            int width = Math.max(button.getWidth(), tabWidthForLayout(button));
            if (button.getX() < -1000 || width <= 0) {
                continue;
            }
            slots.add(new ComponentSubtabReorderLayout.BoundsSlot(tabIndex, button.getX(), width));
        }
        if (slots.isEmpty()) {
            return -1;
        }
        return ComponentSubtabReorderLayout.dropIndexForCurrentBounds(
                pointerX,
                slots,
                draggedIndex,
                orderedButtons.size(),
                reorderGapX,
                reorderGapWidth
        );
    }

    private @NotNull List<JToggleButton> orderedVisibleTabButtons() {
        if (reorderVisualOrder != null && isReorderDragGhostActive()) {
            return reorderVisualOrder;
        }
        return buttonOrder.orderedButtons(buttonsByFile);
    }

    private void captureVisualOrderIfNeeded() {
        if (!isReorderDragGhostActive() || reorderVisualOrder != null) {
            return;
        }
        List<JToggleButton> buttons = new ArrayList<>();
        for (Component child : tabsHost.getComponents()) {
            if (child == reorderDragGhost || !(child instanceof JToggleButton button)) {
                continue;
            }
            if (buttonsByFile.containsValue(button)) {
                buttons.add(button);
            }
        }
        buttons.sort((left, right) -> Integer.compare(left.getX(), right.getX()));
        reorderVisualOrder = List.copyOf(buttons);
    }

    private @NotNull List<Integer> orderedTabWidths() {
        if (reorderLayoutTabWidths != null) {
            return reorderLayoutTabWidths;
        }
        return reorderDraggedFile == null
                ? tabWidthsForDrag(null)
                : tabWidthsForDrag(reorderDraggedFile);
    }

    private @NotNull List<Integer> tabWidthsForDrag(@Nullable VirtualFile draggedFile) {
        List<Integer> widths = new ArrayList<>();
        JToggleButton dragged = draggedFile == null ? null : buttonsByFile.get(draggedFile);
        for (JToggleButton button : orderedVisibleTabButtons()) {
            if (button == dragged) {
                int dragWidth = draggedFile != null
                        && draggedFile.equals(reorderDraggedFile)
                        && reorderDraggedTabWidth > 0
                        ? reorderDraggedTabWidth
                        : tabWidthForLayout(button);
                widths.add(dragWidth);
            } else {
                widths.add(tabWidthForLayout(button));
            }
        }
        return widths;
    }

    private static int tabWidthForLayout(@Nullable JToggleButton button) {
        if (button == null) {
            return JBUI.scale(80);
        }
        return Math.max(JBUI.scale(80), Math.max(button.getPreferredSize().width, button.getWidth()));
    }

    private boolean isReorderDragGhostActive() {
        return reorderDragPointer != null && reorderDraggedFile != null;
    }

    private boolean isActiveReorderDragLayout() {
        return isReorderDragGhostActive() && reorderDropIndex >= 0;
    }

    private void ensureReorderDragGhost() {
        if (reorderDragGhost != null) {
            return;
        }
        reorderDragGhost = new JComponent() {
            @Override
            protected void paintComponent(@NotNull Graphics graphics) {
                if (reorderDragImage == null) {
                    return;
                }
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.82f));
                    g.drawImage(reorderDragImage, 0, 0, null);
                } finally {
                    g.dispose();
                }
            }
        };
        reorderDragGhost.setOpaque(false);
        tabsHost.add(reorderDragGhost);
    }

    private void hideReorderDragGhost() {
        if (reorderDragGhost != null) {
            reorderDragGhost.setVisible(false);
        }
        if (reorderGhostWindow != null) {
            reorderGhostWindow.setVisible(false);
        }
    }

    private void updateReorderDragGhostBounds() {
        if (reorderDragGhost == null
                || reorderDragImage == null
                || reorderDragPointer == null
                || !isReorderDragGhostActive()) {
            hideReorderDragGhost();
            return;
        }

        int ghostX = reorderDragPointer.x - reorderDragImage.getWidth() / 2;
        int ghostY = reorderDragPointer.y - reorderDragImage.getHeight() / 2;
        reorderDragGhost.setBounds(
                ghostX,
                ghostY,
                reorderDragImage.getWidth(),
                reorderDragImage.getHeight()
        );
        reorderDragGhost.setVisible(false);
        showReorderGhostWindow(ghostX, ghostY);
    }

    private void showReorderGhostWindow(int ghostX, int ghostY) {
        if (reorderDragImage == null || !tabsHost.isShowing()) {
            return;
        }
        Window owner = SwingUtilities.getWindowAncestor(tabsHost);
        if (owner == null) {
            return;
        }
        if (reorderGhostWindow == null) {
            reorderGhostWindow = new JWindow(owner);
            reorderGhostWindow.setBackground(new Color(0, 0, 0, 0));
            reorderGhostWindow.setContentPane(new JComponent() {
                @Override
                protected void paintComponent(Graphics graphics) {
                    if (reorderDragImage != null) {
                        graphics.drawImage(reorderDragImage, 0, 0, null);
                    }
                }
            });
            if (reorderGhostWindow.getContentPane() instanceof JComponent content) {
                content.setOpaque(false);
            }
        }
        try {
            Point screen = tabsHost.getLocationOnScreen();
            reorderGhostWindow.setBounds(
                    screen.x + ghostX,
                    screen.y + ghostY,
                    reorderDragImage.getWidth(),
                    reorderDragImage.getHeight()
            );
            reorderGhostWindow.setVisible(true);
            reorderGhostWindow.repaint();
        } catch (IllegalComponentStateException ignored) {
            reorderGhostWindow.setVisible(false);
        }
    }

    private static boolean pointsEqual(@Nullable Point left, @Nullable Point right) {
        if (left == right) {
            return true;
        }
        return left != null && right != null && left.x == right.x && left.y == right.y;
    }

    private static @NotNull BufferedImage snapshotButton(@NotNull JToggleButton button) {
        int width = Math.max(button.getWidth(), 1);
        int height = Math.max(button.getHeight(), 1);
        BufferedImage image = UIUtil.createImage(button, width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            button.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private final class TabStrip extends JPanel implements Scrollable {
        private TabStrip() {
            enableEvents(AWTEvent.MOUSE_WHEEL_EVENT_MASK);
        }

        @Override
        protected void paintChildren(Graphics graphics) {
            super.paintChildren(graphics);
            paintReorderDropOverlay(graphics);
            if (reorderDragGhost != null && reorderDragGhost.isVisible()) {
                reorderDragGhost.paint(graphics);
            }
        }

        private void paintReorderDropOverlay(@NotNull Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                if (isActiveReorderDragLayout() && reorderGapX >= 0) {
                    paintDropTarget(g, reorderGapX, reorderGapWidth);
                }
            } finally {
                g.dispose();
            }
        }

        private void paintDropTarget(@NotNull Graphics2D graphics, int dropX, int slotWidth) {
            int referenceHeight = ComponentSubtabUi.tabHeight();
            int slotY = getInsets().top + vgap;
            int arc = JBUI.scale(4);
            Color accent = CurrentTheme.TabbedPane.ENABLED_SELECTED_COLOR;

            graphics.setColor(getBackground());
            graphics.fillRoundRect(dropX, slotY, slotWidth, referenceHeight, arc, arc);
            graphics.setColor(accent);
            graphics.drawRoundRect(dropX, slotY, Math.max(0, slotWidth - 1), Math.max(0, referenceHeight - 1), arc, arc);
        }

        @Override
        protected void processMouseWheelEvent(MouseWheelEvent event) {
            if (wheelForwarder != null) {
                wheelForwarder.mouseWheelMoved(event);
            }
            super.processMouseWheelEvent(event);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            Dimension preferred = getPreferredSize();
            return new Dimension(1, preferred.height);
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return JBUI.scale(16);
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(visibleRect.width / 2, JBUI.scale(64));
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return false;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return true;
        }
    }

    private final class SingleRowLayout implements LayoutManager {
        @Override
        public void addLayoutComponent(String name, Component component) {
        }

        @Override
        public void removeLayoutComponent(Component component) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return measure(parent, false);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return measure(parent, true);
        }

        @Override
        public void layoutContainer(Container parent) {
            synchronized (parent.getTreeLock()) {
                Insets insets = parent.getInsets();
                int y = insets.top + vgap;
                int availableHeight = parent.getHeight() - insets.top - insets.bottom - 2 * vgap;

                JToggleButton dragged = isReorderDragGhostActive()
                        ? buttonsByFile.get(reorderDraggedFile)
                        : null;
                if (!isActiveReorderDragLayout()) {
                    layoutNormalRow(parent, insets.left, y, availableHeight, dragged);
                    reorderGapReset();
                    return;
                }

                if (dragged == null) {
                    layoutNormalRow(parent, insets.left, y, availableHeight, null);
                    reorderGapReset();
                    return;
                }

                List<JToggleButton> orderedButtons = orderedVisibleTabButtons();
                int draggedIndex = orderedButtons.indexOf(dragged);
                if (draggedIndex < 0) {
                    draggedIndex = 0;
                }
                int gapWidth = reorderDraggedTabWidth > 0
                        ? reorderDraggedTabWidth
                        : Math.max(dragged.getPreferredSize().width, dragged.getWidth());
                ComponentSubtabReorderLayout.Result layout = ComponentSubtabReorderLayout.layout(
                        orderedTabWidths(),
                        draggedIndex,
                        reorderDropIndex,
                        gapWidth,
                        hgap,
                        insets.left
                );
                reorderGapX = layout.gapX();
                reorderGapWidth = layout.gapWidth();

                int height = availableHeight > 0 ? availableHeight : dragged.getPreferredSize().height;
                HashSet<JToggleButton> placed = new HashSet<>();
                for (ComponentSubtabReorderLayout.TabPlacement placement : layout.placements()) {
                    if (placement.tabIndex() < 0 || placement.tabIndex() >= orderedButtons.size()) {
                        continue;
                    }
                    JToggleButton button = orderedButtons.get(placement.tabIndex());
                    if (button == dragged) {
                        continue;
                    }
                    button.setBounds(placement.x(), y, placement.width(), height);
                    placed.add(button);
                }
                for (JToggleButton button : orderedButtons) {
                    if (button != dragged && !placed.contains(button)) {
                        button.setBounds(-100000, y, 0, height);
                    }
                }
                dragged.setBounds(-100000, y, 0, height);
            }
        }

        private void layoutNormalRow(
                @NotNull Container parent,
                int startX,
                int y,
                int availableHeight,
                @Nullable JToggleButton hideDragged
        ) {
            int x = startX;
            for (Component child : parent.getComponents()) {
                if (child == reorderDragGhost) {
                    continue;
                }
                if (!(child instanceof JToggleButton)) {
                    continue;
                }
                Dimension size = child.getPreferredSize();
                int height = availableHeight > 0 ? availableHeight : size.height;
                if (child == hideDragged || ComponentSubtabUi.isReorderHidden((JToggleButton) child)) {
                    child.setBounds(-100000, y, 0, height);
                    continue;
                }
                child.setBounds(x, y, size.width, height);
                x += size.width + hgap;
            }
        }

        private @NotNull Dimension measure(@NotNull Container parent, boolean minimumWidth) {
            synchronized (parent.getTreeLock()) {
                int width = 0;
                int height = ComponentSubtabUi.tabHeight();
                int visibleTabs = 0;
                JToggleButton dragged = isReorderDragGhostActive()
                        ? buttonsByFile.get(reorderDraggedFile)
                        : null;
                if (isActiveReorderDragLayout() && dragged != null) {
                    int draggedIndex = orderedVisibleTabButtons().indexOf(dragged);
                    int gapWidth = reorderDraggedTabWidth > 0
                            ? reorderDraggedTabWidth
                            : Math.max(dragged.getPreferredSize().width, dragged.getWidth());
                    ComponentSubtabReorderLayout.Result layout = ComponentSubtabReorderLayout.layout(
                            orderedTabWidths(),
                            draggedIndex,
                            reorderDropIndex,
                            gapWidth,
                            hgap,
                            parent.getInsets().left
                    );
                    height = ComponentSubtabUi.tabHeight();
                    Insets insets = parent.getInsets();
                    return new Dimension(
                            minimumWidth ? 0 : layout.totalWidth() + insets.left + insets.right,
                            height + insets.top + insets.bottom + 2 * vgap
                    );
                }

                for (Component child : parent.getComponents()) {
                    if (child == reorderDragGhost || !(child instanceof JToggleButton)) {
                        continue;
                    }
                    if (child == dragged || ComponentSubtabUi.isReorderHidden((JToggleButton) child)) {
                        continue;
                    }
                    Dimension size = child.getPreferredSize();
                    width += size.width;
                    height = Math.max(height, size.height);
                    visibleTabs++;
                }

                if (visibleTabs > 1) {
                    width += hgap * (visibleTabs - 1);
                }

                Insets insets = parent.getInsets();
                return new Dimension(
                        minimumWidth ? 0 : width + insets.left + insets.right,
                        height + insets.top + insets.bottom + 2 * vgap
                );
            }
        }
    }
}
