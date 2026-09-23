package de.sasbe.subtabs;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.JBUI.CurrentTheme;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JToggleButton;
import javax.swing.JWindow;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

final class ComponentSubtabBarPanel extends JPanel {
    private final Project project;
    private final TabStrip tabsHost;
    private final JBScrollPane scrollPane;
    private final SubtabOverflowStrip overflowStrip;
    private final ComponentSubtabIconButton collapseButton;
    private final ComponentSubtabIconButton closeSideButton;
    private final ComponentSubtabIconButton ruleSwitchButton;
    private final JPanel ruleSwitchPanel;
    private final JPanel trailingPanel;
    private final Map<VirtualFile, JToggleButton> buttonsByFile = new HashMap<>();
    private final Set<VirtualFile> watchedDocuments = new HashSet<>();
    private final AtomicBoolean ignoreNextClick = new AtomicBoolean();
    private boolean dragInstalled;
    private int reorderDropIndex = -1;
    private @Nullable VirtualFile reorderDraggedFile;
    private @Nullable Point reorderDragPointer;
    private @Nullable BufferedImage reorderDragImage;
    private @Nullable JComponent reorderDragGhost;
    private @Nullable JWindow reorderGhostWindow;
    private int reorderGapX = -1;
    private int reorderGapWidth;
    private int reorderDraggedTabWidth;
    private @Nullable List<Integer> reorderLayoutTabWidths;
    private @Nullable List<JToggleButton> reorderVisualOrder;

    private ComponentSubtabGroup group;
    private VirtualFile displayedFile;
    private SubtabFitScale.Result fit = SubtabFitScale.Result.FULL;
    private int naturalStripWidth;
    private int topSpacerHeight;
    private @Nullable String boundActiveRuleName;

    ComponentSubtabBarPanel(
            @NotNull Project project,
            @NotNull ComponentSubtabGroup group,
            @NotNull VirtualFile displayedFile
    ) {
        super(new BorderLayout(0, 0));
        this.project = project;

        tabsHost = new TabStrip();
        tabsHost.setBackground(UIUtil.getPanelBackground());
        tabsHost.setBorder(BorderFactory.createEmptyBorder());
        tabsHost.setLayout(new SingleRowLayout(
                ComponentSubtabUi.horizontalGap(fit),
                ComponentSubtabUi.verticalGap()
        ));

        scrollPane = new JBScrollPane(
                tabsHost,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(UIUtil.getPanelBackground());
        scrollPane.getViewport().setBackground(UIUtil.getPanelBackground());
        scrollPane.setOverlappingScrollBar(true);
        JScrollBar horizontalBar = scrollPane.getHorizontalScrollBar();
        horizontalBar.setOpaque(true);
        horizontalBar.putClientProperty(JBScrollPane.IGNORE_SCROLLBAR_IN_INSETS, Boolean.FALSE);

        overflowStrip = new SubtabOverflowStrip(scrollPane, () -> tabsHost.getPreferredSize().width);

        collapseButton = createCollapseButton();
        closeSideButton = createCloseSideButton();
        closeSideButton.setVisible(false);
        ruleSwitchButton = createRuleSwitchButton();
        ruleSwitchPanel = new JPanel();
        ruleSwitchPanel.setLayout(new BoxLayout(ruleSwitchPanel, BoxLayout.X_AXIS));
        ruleSwitchPanel.setOpaque(false);
        ruleSwitchPanel.setBorder(JBUI.Borders.emptyLeft(4));
        ruleSwitchPanel.add(ruleSwitchButton);
        ruleSwitchPanel.setVisible(false);

        trailingPanel = new JPanel();
        trailingPanel.setLayout(new BoxLayout(trailingPanel, BoxLayout.X_AXIS));
        trailingPanel.setOpaque(false);
        trailingPanel.setBorder(JBUI.Borders.emptyLeft(2));
        trailingPanel.add(collapseButton);
        trailingPanel.add(closeSideButton);

        JPanel eastPanel = new JPanel();
        eastPanel.setLayout(new BoxLayout(eastPanel, BoxLayout.X_AXIS));
        eastPanel.setOpaque(false);
        eastPanel.add(ruleSwitchPanel);
        eastPanel.add(trailingPanel);

        applyChrome();
        add(overflowStrip, BorderLayout.CENTER);
        add(eastPanel, BorderLayout.EAST);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                updateFitToEditorWidth();
            }
        });
        scrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                updateFitToEditorWidth();
            }
        });

        bind(group, displayedFile);
        updateScrollReserve();
    }

    void refreshAppearance() {
        naturalStripWidth = 0;
        fit = SubtabFitScale.Result.FULL;
        applyFitToButtons(fit);
        tabsHost.setLayout(new SingleRowLayout(
                ComponentSubtabUi.horizontalGap(fit),
                ComponentSubtabUi.verticalGap()
        ));
        collapseButton.updateSize();
        closeSideButton.updateSize();
        ruleSwitchButton.updateSize();
        applyChrome();
        revalidate();
        updateFitToEditorWidth();
        repaint();
    }

    /**
     * Aligns this bar with the neighbouring split pane and shows the button that closes this side.
     * The spacer compensates the main tab strip, which only exists above the primary pane.
     */
    void applySplitLayout(int topSpacerHeight, boolean splitActive) {
        boolean spacerChanged = this.topSpacerHeight != topSpacerHeight;
        this.topSpacerHeight = topSpacerHeight;
        closeSideButton.setVisible(splitActive);
        if (spacerChanged) {
            applyChrome();
        }
        updateTrailingPanelVisibility();
        revalidate();
        updateFitToEditorWidth();
        repaint();
    }

    void setCollapseButtonVisible(boolean visible) {
        collapseButton.setVisible(visible);
        updateTrailingPanelVisibility();
        revalidate();
        updateFitToEditorWidth();
        repaint();
    }

    private void updateTrailingPanelVisibility() {
        trailingPanel.setVisible(collapseButton.isVisible() || closeSideButton.isVisible());
        updateScrollReserve();
    }

    void refreshRuleSwitchButton() {
        if (displayedFile == null) {
            ruleSwitchPanel.setVisible(false);
            return;
        }
        boolean visible = SubtabRuleRotation.hasMultipleMatches(
                displayedFile.getName(),
                ComponentFileNaming.rules()
        );
        ruleSwitchPanel.setVisible(visible);
        if (visible) {
            List<CustomSubtabRule> rules = ComponentFileNaming.rules();
            List<Integer> indices = SubtabRuleRotation.matchingRuleIndices(
                    displayedFile.getName(),
                    rules
            );
            if (!indices.isEmpty()) {
                int activeIndex = indices.get(0);
                String ruleName = activeIndex >= 0 && activeIndex < rules.size()
                        ? rules.get(activeIndex).name
                        : "Regel";
                ruleSwitchButton.setToolTipText(
                        "Regel wechseln (aktiv: " + ruleName + ", " + indices.size() + " Treffer)"
                );
                ruleSwitchButton.getAccessibleContext().setAccessibleName("Regel wechseln");
            }
        }
        updateScrollReserve();
        revalidate();
        repaint();
    }

    private void updateScrollReserve() {
        overflowStrip.setRightReserve(reservedEastWidth());
    }

    private int reservedEastWidth() {
        int width = 0;
        if (ruleSwitchPanel.isVisible()) {
            width += Math.max(
                    ruleSwitchPanel.getPreferredSize().width,
                    ruleSwitchButton.getPreferredSize().width
            );
        }
        if (trailingPanel.isVisible()) {
            width += trailingPanel.getPreferredSize().width;
        }
        if (width > 0) {
            width += JBUI.scale(4);
        }
        return width;
    }

    private static @Nullable String activeRuleName(
            @NotNull VirtualFile file,
            @NotNull List<CustomSubtabRule> rules
    ) {
        List<Integer> indices = SubtabRuleRotation.matchingRuleIndices(file.getName(), rules);
        if (indices.isEmpty()) {
            return null;
        }
        int activeIndex = indices.get(0);
        return activeIndex >= 0 && activeIndex < rules.size() ? rules.get(activeIndex).name : null;
    }

    private static boolean sameRelatedFiles(
            @NotNull ComponentSubtabGroup left,
            @NotNull ComponentSubtabGroup right
    ) {
        List<ComponentRelatedFiles.Entry> leftFiles = left.relatedFiles();
        List<ComponentRelatedFiles.Entry> rightFiles = right.relatedFiles();
        if (leftFiles.size() != rightFiles.size()) {
            return false;
        }
        for (int index = 0; index < leftFiles.size(); index++) {
            if (!leftFiles.get(index).file().getPath().equals(rightFiles.get(index).file().getPath())) {
                return false;
            }
        }
        return true;
    }

    private void clearButtons() {
        tabsHost.removeAll();
        buttonsByFile.clear();
        naturalStripWidth = 0;
    }

    @Nullable JComponent collapseButtonIfShowing() {
        return collapseButton.isVisible() && collapseButton.isShowing() ? collapseButton : null;
    }

    void bind(@NotNull ComponentSubtabGroup group, @NotNull VirtualFile displayedFile) {
        List<CustomSubtabRule> rules = ComponentFileNaming.rules();
        String activeRuleName = activeRuleName(displayedFile, rules);
        boolean groupChanged = this.group != null && !sameRelatedFiles(this.group, group);
        boolean ruleChanged = boundActiveRuleName != null
                && activeRuleName != null
                && !boundActiveRuleName.equals(activeRuleName);
        this.group = group;
        this.displayedFile = displayedFile;
        if (groupChanged || ruleChanged) {
            clearButtons();
        }
        boundActiveRuleName = activeRuleName;
        rebuildButtonsIfNeeded();
        updateSelection(displayedFile);
        refreshOpenStates();
        refreshRuleSwitchButton();
        watchAllDocuments();
    }

    void setDisplayedFile(@NotNull VirtualFile displayedFile) {
        this.displayedFile = displayedFile;
        updateSelection(displayedFile);
        refreshOpenStates();
        refreshRuleSwitchButton();
    }

    void refreshRelatedFiles(@NotNull ComponentSubtabGroup group, @NotNull VirtualFile displayedFile) {
        clearReorderPreview();
        this.group = group;
        this.displayedFile = displayedFile;
        boundActiveRuleName = activeRuleName(displayedFile, ComponentFileNaming.rules());
        clearButtons();
        rebuildButtonsIfNeeded();
        updateSelection(displayedFile);
        refreshOpenStates();
        refreshRuleSwitchButton();
        watchAllDocuments();
        revalidate();
        repaint();
    }

    @NotNull VirtualFile displayedFile() {
        return displayedFile;
    }

    int topSpacerHeight() {
        return topSpacerHeight;
    }

    boolean isCloseSideButtonVisible() {
        return closeSideButton.isVisible();
    }

    boolean isRuleSwitchVisible() {
        return ruleSwitchPanel.isVisible();
    }

    void hideRuleSwitchOnBar() {
        if (!ruleSwitchPanel.isVisible()) {
            return;
        }
        ruleSwitchPanel.setVisible(false);
        updateScrollReserve();
        revalidate();
        repaint();
    }

    void setReorderPreview(int index, @Nullable VirtualFile draggedFile) {
        updateReorderDragState(index, draggedFile, null);
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
            reorderGapX = -1;
            reorderGapWidth = 0;
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

    void clearReorderPreview() {
        updateReorderDragState(-1, null, null);
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

    @NotNull Point pointerInTabsHostFromEvent(@NotNull MouseEvent event) {
        Point inPanel = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), this);
        Point inTabs = SwingUtilities.convertPoint(this, inPanel, tabsHost);
        if (tabsHost.getHeight() > 0) {
            inTabs.y = Math.max(0, Math.min(tabsHost.getHeight() - 1, inTabs.y));
        }
        return inTabs;
    }

    int reorderGapXForTests() {
        return reorderGapX;
    }

    int reorderGapWidthForTests() {
        return reorderGapWidth;
    }

    void layOutTabsForTests(int width) {
        int height = stablePanelHeight();
        setSize(width, height);
        overflowStrip.setSize(Math.max(0, width - getInsets().left - getInsets().right), height);
        scrollPane.setSize(overflowStrip.getWidth(), height);
        tabsHost.setSize(
                Math.max(0, scrollPane.getViewport().getWidth()),
                ComponentSubtabUi.barRowHeight()
        );
        validate();
        doLayout();
        tabsHost.validate();
        tabsHost.doLayout();
    }

    int tabButtonCountForTests() {
        int count = 0;
        for (Component child : tabsHost.getComponents()) {
            if (child instanceof JToggleButton) {
                count++;
            }
        }
        return count;
    }

    void applyReorderLayoutForTests(@NotNull VirtualFile draggedFile, int dropIndex) {
        layOutTabsForTests(900);
        reorderDraggedFile = draggedFile;
        reorderDropIndex = dropIndex;
        reorderDragPointer = new Point(0, 0);
        reorderDraggedTabWidth = tabWidthForLayout(buttonsByFile.get(draggedFile));
        reorderLayoutTabWidths = List.copyOf(tabWidthsForDrag(draggedFile));

        List<JToggleButton> orderedButtons = orderedVisibleTabButtons();
        JToggleButton dragged = buttonsByFile.get(draggedFile);
        int draggedIndex = orderedButtons.indexOf(dragged);
        int hgap = ComponentSubtabUi.horizontalGap(fit);
        ComponentSubtabReorderLayout.Result layout = ComponentSubtabReorderLayout.layout(
                orderedTabWidths(),
                draggedIndex,
                dropIndex,
                reorderDraggedTabWidth,
                hgap,
                tabsHost.getInsets().left
        );
        reorderGapX = layout.gapX();
        reorderGapWidth = layout.gapWidth();

        Insets insets = tabsHost.getInsets();
        int y = insets.top + ComponentSubtabUi.verticalGap();
        int height = ComponentSubtabUi.barRowHeight();
        for (ComponentSubtabReorderLayout.TabPlacement placement : layout.placements()) {
            JToggleButton button = orderedButtons.get(placement.tabIndex());
            button.setBounds(placement.x(), y, placement.width(), height);
        }
        syncDraggedTabBarVisibility();
        if (dragged != null) {
            dragged.setBounds(-100000, y, 0, height);
        }
        tabsHost.repaint();
    }

    private static int tabWidthForLayout(@Nullable JToggleButton button) {
        if (button == null) {
            return JBUI.scale(80);
        }
        return Math.max(JBUI.scale(80), Math.max(button.getPreferredSize().width, button.getWidth()));
    }

    private boolean isActiveReorderDragLayout() {
        return isReorderDragGhostActive() && reorderDropIndex >= 0;
    }

    boolean isReorderHiddenForTests(@NotNull VirtualFile file) {
        JToggleButton button = buttonsByFile.get(file);
        return button != null && ComponentSubtabUi.isReorderHidden(button);
    }

    void scrambleTabsHostOrderForTests() {
        List<JToggleButton> orderedButtons = orderedVisibleTabButtons();
        if (orderedButtons.size() < 2) {
            return;
        }
        JComponent ghost = reorderDragGhost;
        if (ghost != null) {
            tabsHost.remove(ghost);
        }
        for (JToggleButton button : orderedButtons) {
            tabsHost.remove(button);
        }
        for (int index = orderedButtons.size() - 1; index >= 0; index--) {
            tabsHost.add(orderedButtons.get(index));
        }
        if (ghost != null) {
            tabsHost.add(ghost);
        }
    }

    @NotNull List<VirtualFile> visualFilesForTests() {
        List<VirtualFile> files = new ArrayList<>();
        buttonsByFile.entrySet().stream()
                .sorted((left, right) -> Integer.compare(left.getValue().getX(), right.getValue().getX()))
                .forEach(entry -> files.add(entry.getKey()));
        return files;
    }

    @Nullable VirtualFile leftmostVisibleTabFileForTests() {
        int leftmostX = Integer.MAX_VALUE;
        VirtualFile leftmostFile = null;
        for (var entry : buttonsByFile.entrySet()) {
            JToggleButton button = entry.getValue();
            if (ComponentSubtabUi.isReorderHidden(button) || button.getX() < -1000 || button.getWidth() <= 0) {
                continue;
            }
            if (button.getX() < leftmostX) {
                leftmostX = button.getX();
                leftmostFile = entry.getKey();
            }
        }
        return leftmostFile;
    }

    @NotNull BufferedImage paintTabsHostForTests() {
        int width = Math.max(900, tabsHost.getWidth());
        int rowHeight = ComponentSubtabUi.barRowHeight();
        tabsHost.setSize(width, rowHeight);
        tabsHost.doLayout();
        updateReorderDragGhostBounds();
        tabsHost.setDoubleBuffered(false);
        boolean ghostWasVisible = reorderDragGhost != null && reorderDragGhost.isVisible();
        if (reorderDragGhost != null) {
            reorderDragGhost.setVisible(false);
        }
        int ghostHeight = reorderDragImage == null ? 0 : reorderDragImage.getHeight() + JBUI.scale(6);
        BufferedImage image = UIUtil.createImage(tabsHost, width, rowHeight + ghostHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(tabsHost.getBackground());
            graphics.fillRect(0, 0, width, rowHeight + ghostHeight);
            graphics.setClip(0, 0, width, rowHeight);
            tabsHost.print(graphics);
            if (reorderDragImage != null && reorderDragPointer != null) {
                graphics.setClip(0, 0, width, rowHeight + ghostHeight);
                graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.95f));
                int ghostX = reorderDragPointer.x - reorderDragImage.getWidth() / 2;
                int ghostY = reorderDragPointer.y - reorderDragImage.getHeight() / 2;
                graphics.drawImage(reorderDragImage, ghostX, ghostY, null);
            }
            if (reorderDragGhost != null && ghostWasVisible) {
                reorderDragGhost.setVisible(true);
            }
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private boolean isReorderDragGhostActive() {
        return reorderDragPointer != null && reorderDraggedFile != null;
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
                ComponentSubtabUi.horizontalGap(fit),
                tabsHost.getInsets().left
        );
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

    int tabIndexForFile(@NotNull VirtualFile file) {
        JToggleButton button = buttonsByFile.get(file);
        return button == null ? -1 : orderedVisibleTabButtons().indexOf(button);
    }

    int draggedTabIndex() {
        return reorderDraggedFile == null ? -1 : tabIndexForFile(reorderDraggedFile);
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

    private @NotNull List<JToggleButton> orderedVisibleTabButtons() {
        if (reorderVisualOrder != null && isReorderDragGhostActive()) {
            return reorderVisualOrder;
        }
        List<JToggleButton> buttons = new ArrayList<>();
        for (ComponentRelatedFiles.Entry entry : group.relatedFiles()) {
            JToggleButton button = buttonsByFile.get(entry.file());
            if (button != null) {
                buttons.add(button);
            }
        }
        return buttons;
    }

    private void alignTabsHostToGroupOrder() {
        if (buttonsByFile.isEmpty()) {
            return;
        }
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
                int dragWidth = draggedFile != null && draggedFile.equals(reorderDraggedFile) && reorderDraggedTabWidth > 0
                        ? reorderDraggedTabWidth
                        : tabWidthForLayout(button);
                widths.add(dragWidth);
            } else {
                widths.add(tabWidthForLayout(button));
            }
        }
        return widths;
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

    @NotNull ComponentSubtabIconButton ruleSwitchButton() {
        return ruleSwitchButton;
    }

    void switchRuleForDisplayedFile() {
        ComponentSubtabsManager.rotateSubtabRuleForFile(project, displayedFile);
    }

    void refreshTabHighlights() {
        for (JToggleButton button : buttonsByFile.values()) {
            ComponentSubtabUi.refreshButton(button);
        }
    }

    void refreshOpenStates() {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        ComponentSubtabGroupSplitRegistry.SplitState splitState = findActiveSplitState();
        SubtabGroupPopupPresentation.Context mainTabPresentation =
                SubtabGroupPopupPresentation.forMainTab(project, displayedFile);

        for (var entry : buttonsByFile.entrySet()) {
            VirtualFile file = entry.getKey();
            JToggleButton button = entry.getValue();

            boolean own = file.equals(displayedFile);
            boolean partner = splitState != null && !own && splitState.covers(file);

            ComponentSubtabUi.setSplitPartner(button, partner);
            ComponentSubtabUi.setOpenElsewhere(button, manager.isFileOpen(file) && !own && !partner);
            refreshModifiedStateForFile(file);

            if (button.isSelected() != own) {
                button.setSelected(own);
            }
            if (ComponentSubtabUi.isMainTabSyncHighlight(button)
                    && !mainTabPresentation.highlightedFiles().contains(file)) {
                ComponentSubtabUi.setMainTabSyncHighlight(button, false);
            }
        }

        // Panels are recycled between editors, so a bar that no longer belongs to a split has to
        // drop the split chrome it may still carry.
        if (splitState == null && (topSpacerHeight != 0 || closeSideButton.isVisible())) {
            applySplitLayout(0, false);
        }
    }

    void refreshModifiedStateForFile(@NotNull VirtualFile file) {
        JToggleButton button = buttonsByFile.get(file);
        if (button == null) {
            return;
        }
        ComponentSubtabFilePresentation presentation = ComponentSubtabFilePresentation.compute(project, file);
        ComponentSubtabUi.setPresentation(button, presentation.modified(), presentation.hasErrors());
    }

    /**
     * A bar only takes part in a split when its own pane is one of the two split panes. Falling back to
     * the group key would make every other main tab of the same group look like a split participant.
     */
    private @Nullable ComponentSubtabGroupSplitRegistry.SplitState findActiveSplitState() {
        return ComponentSubtabGroupSplitRegistry.getInstance(project).findByFile(displayedFile);
    }

    @Nullable JToggleButton buttonFor(@NotNull VirtualFile file) {
        JToggleButton button = buttonsByFile.get(file);
        if (button != null) {
            return button;
        }
        String path = file.getPath();
        for (var entry : buttonsByFile.entrySet()) {
            if (path.equals(entry.getKey().getPath())) {
                return entry.getValue();
            }
        }
        return null;
    }

    void refreshModifiedStateForDocument(@NotNull com.intellij.openapi.editor.Document document) {
        com.intellij.openapi.fileEditor.FileDocumentManager manager =
                com.intellij.openapi.fileEditor.FileDocumentManager.getInstance();
        for (var entry : buttonsByFile.entrySet()) {
            if (document.equals(manager.getCachedDocument(entry.getKey()))) {
                ComponentSubtabFilePresentation presentation = ComponentSubtabFilePresentation.computeForDocument(
                        project,
                        document,
                        entry.getKey()
                );
                ComponentSubtabUi.setPresentation(
                        entry.getValue(),
                        presentation.modified(),
                        presentation.hasErrors()
                );
            }
        }
    }

    private void watchAllDocuments() {
        for (VirtualFile file : buttonsByFile.keySet()) {
            watchDocument(file);
        }
    }

    private void watchDocument(@NotNull VirtualFile file) {
        if (watchedDocuments.contains(file)) {
            return;
        }
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) {
            return;
        }
        watchedDocuments.add(file);
        document.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                ComponentSubtabsManager.refreshModifiedStateForDocument(project, event.getDocument());
            }
        }, project);
    }

    private void applyChrome() {
        setBorder(JBUI.Borders.empty(
                ComponentSubtabUi.compactVertical(1) + topSpacerHeight,
                6,
                ComponentSubtabUi.compactVertical(1),
                0
        ));
        SubtabOverflowMode mode = SubtabsSettings.getInstance().getOverflowMode();
        overflowStrip.setMode(mode);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(super.getPreferredSize().width, stablePanelHeight());
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0, stablePanelHeight());
    }

    private int stablePanelHeight() {
        Insets insets = getInsets();
        return insets.top + ComponentSubtabUi.barRowHeight() + insets.bottom;
    }

    private void rebuildButtonsIfNeeded() {
        if (!buttonsByFile.isEmpty()) {
            updateSelection(displayedFile);
            refreshOpenStates();
            return;
        }

        ButtonGroup buttonGroup = new ButtonGroup();
        overflowStrip.attachWheel(tabsHost);

        for (ComponentRelatedFiles.Entry relatedFile : group.relatedFiles()) {
            JToggleButton button = ComponentSubtabUi.createSubtabButton(
                    relatedFile.label(),
                    relatedFile.file().equals(displayedFile)
            );
            button.putClientProperty(ComponentSubtabUi.FILE_KEY, relatedFile.file());
            button.setToolTipText(relatedFile.file().getPath());
            button.getAccessibleContext().setAccessibleName(
                    relatedFile.label() + " öffnen: " + relatedFile.file().getName()
            );
            button.addActionListener(event -> {
                if (ignoreNextClick.getAndSet(false)) {
                    updateSelection(displayedFile);
                    return;
                }
                VirtualFile target = relatedFile.file();
                if (displayedFile.equals(target)) {
                    return;
                }
                ComponentSubtabGroupSplitRegistry.SplitState splitState = findActiveSplitState();
                if (splitState != null) {
                    // A split holds exactly two subtabs: the partner swaps sides, anything else
                    // replaces this pane's file so the split never grows beyond two panes.
                    if (splitState.covers(target)) {
                        ComponentSubtabGroupSplitNavigation.swapSides(project, splitState);
                    } else {
                        ComponentSubtabGroupSplitNavigation.replacePaneFile(
                                project, splitState, displayedFile, target);
                    }
                    return;
                }
                ComponentSubtabNavigation.switchToRelatedFile(project, displayedFile, target);
                updateSelection(displayedFile);
            });
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    VirtualFile target = relatedFile.file();
                    if (SubtabHoverView.isEnabled()) {
                        ComponentSubtabProjectViewHover.onEnter(project, target, button);
                        FileEditorManager manager = FileEditorManager.getInstance(project);
                        if (manager.isFileOpen(target) && !displayedFile.equals(target)) {
                            ComponentSubtabMainTabHover.onEnter(project, target, button);
                        }
                    }
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    if (SubtabHoverView.isEnabled()) {
                        ComponentSubtabProjectViewHover.onExit(button);
                        ComponentSubtabMainTabHover.onExit(button);
                    }
                }
            });
            ComponentSubtabBarPopup.install(
                    project,
                    button,
                    relatedFile.file(),
                    () -> displayedFile,
                    this
            );
            overflowStrip.attachWheel(button);

            buttonGroup.add(button);
            tabsHost.add(button);
            buttonsByFile.put(relatedFile.file(), button);
            watchDocument(relatedFile.file());
        }

        refreshOpenStates();
        alignTabsHostToGroupOrder();

        if (!dragInstalled) {
            ComponentSubtabEditorDragSupport.install(
                    project,
                    this,
                    this,
                    buttonsByFile,
                    ignoreNextClick
            );
            dragInstalled = true;
        }
        naturalStripWidth = 0;
        updateFitToEditorWidth();
    }

    private void updateSelection(@NotNull VirtualFile currentFile) {
        for (var entry : buttonsByFile.entrySet()) {
            JToggleButton button = entry.getValue();
            boolean selected = entry.getKey().equals(currentFile);
            if (button.isSelected() != selected) {
                button.setSelected(selected);
            }
        }
    }

    private void updateFitToEditorWidth() {
        if (buttonsByFile.isEmpty() || isReorderDragGhostActive()) {
            return;
        }

        boolean enabled = SubtabsSettings.getInstance().isFitTabsToEditorWidth();
        int available = availableEditorWidth();
        int natural = measureNaturalStripWidth();
        SubtabFitScale.Result next = SubtabFitScale.compute(available, natural, enabled);
        if (!SubtabFitScale.differs(fit, next)) {
            return;
        }

        fit = next;
        applyFitToButtons(fit);
        tabsHost.setLayout(new SingleRowLayout(
                ComponentSubtabUi.horizontalGap(fit),
                ComponentSubtabUi.verticalGap()
        ));
        revalidate();
        repaint();
    }

    private int availableEditorWidth() {
        int width = scrollPane.getViewport().getWidth();
        if (width <= 0) {
            width = overflowStrip.getWidth();
        }
        if (width <= 0) {
            Container parent = getParent();
            Component east = ((BorderLayout) getLayout()).getLayoutComponent(BorderLayout.EAST);
            int eastWidth = east != null ? east.getPreferredSize().width : 0;
            width = Math.max(0, (parent != null ? parent.getWidth() : getWidth())
                    - eastWidth
                    - getInsets().left
                    - getInsets().right);
        }
        return Math.max(0, width - overflowStripRightGap());
    }

    private int overflowStripRightGap() {
        return reservedEastWidth();
    }

    private int measureNaturalStripWidth() {
        if (naturalStripWidth > 0) {
            return naturalStripWidth;
        }
        applyFitToButtons(SubtabFitScale.Result.FULL);
        LayoutManager fullLayout = new SingleRowLayout(
                ComponentSubtabUi.horizontalGap(SubtabFitScale.Result.FULL),
                ComponentSubtabUi.verticalGap()
        );
        naturalStripWidth = fullLayout.preferredLayoutSize(tabsHost).width;
        return naturalStripWidth;
    }

    private void applyFitToButtons(@NotNull SubtabFitScale.Result result) {
        for (JToggleButton button : buttonsByFile.values()) {
            ComponentSubtabUi.applyFit(button, result);
        }
    }

    private @NotNull ComponentSubtabIconButton createCollapseButton() {
        ComponentSubtabIconButton button = new ComponentSubtabIconButton(SubtabsIcons.ACTIVE);
        button.setToolTipText("SubTabs einklappen");
        button.getAccessibleContext().setAccessibleName("SubTabs einklappen");
        button.addActionListener(event -> SubtabsCollapseState.getInstance(project).toggle(project));
        return button;
    }

    private @NotNull ComponentSubtabIconButton createRuleSwitchButton() {
        ComponentSubtabIconButton button = new ComponentSubtabIconButton(AllIcons.Actions.SwapPanels);
        button.setToolTipText("Regel wechseln");
        button.getAccessibleContext().setAccessibleName("Regel wechseln");
        button.addActionListener(event -> ComponentSubtabsManager.rotateSubtabRuleForFile(project, displayedFile));
        return button;
    }

    private @NotNull ComponentSubtabIconButton createCloseSideButton() {
        ComponentSubtabIconButton button = new ComponentSubtabIconButton(AllIcons.Actions.Close);
        button.setToolTipText("Diese Split-Seite schließen");
        button.getAccessibleContext().setAccessibleName("Diese Split-Seite schließen");
        button.addActionListener(event -> {
            ComponentSubtabGroupSplitRegistry.SplitState splitState = findActiveSplitState();
            if (splitState != null) {
                ComponentSubtabGroupSplitNavigation.closeSide(project, splitState, displayedFile);
            }
        });
        return button;
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
                } else if (reorderDropIndex >= 0 && reorderDragPointer == null) {
                    paintMenuReorderPreview(g);
                }
            } finally {
                g.dispose();
            }
        }

        private void paintMenuReorderPreview(@NotNull Graphics2D graphics) {
            JToggleButton source = reorderDraggedFile == null ? null : buttonsByFile.get(reorderDraggedFile);
            if (source == null || !source.isVisible()) {
                return;
            }
            paintDropTarget(
                    graphics,
                    dropLineX(reorderDropIndex),
                    Math.max(source.getWidth(), source.getPreferredSize().width)
            );
        }

        private void paintDropTarget(@NotNull Graphics2D graphics, int dropX, int slotWidth) {
            int referenceHeight = ComponentSubtabUi.tabHeight();
            int slotY = getInsets().top + ComponentSubtabUi.verticalGap();
            int arc = JBUI.scale(4);
            Color accent = CurrentTheme.TabbedPane.ENABLED_SELECTED_COLOR;

            graphics.setColor(getBackground());
            graphics.fillRoundRect(dropX, slotY, slotWidth, referenceHeight, arc, arc);
            graphics.setColor(accent);
            graphics.drawRoundRect(dropX, slotY, Math.max(0, slotWidth - 1), Math.max(0, referenceHeight - 1), arc, arc);
        }

        private int dropLineX(int index) {
            int visibleIndex = 0;
            for (Component child : getComponents()) {
                if (!child.isVisible()) {
                    continue;
                }
                if (visibleIndex == index) {
                    return child.getX();
                }
                visibleIndex++;
            }

            Component lastVisible = null;
            for (Component child : getComponents()) {
                if (child.isVisible()) {
                    lastVisible = child;
                }
            }
            if (lastVisible != null) {
                return lastVisible.getX() + lastVisible.getWidth();
            }
            return getInsets().left;
        }

        @Override
        protected void processMouseWheelEvent(MouseWheelEvent event) {
            overflowStrip.wheelListener().mouseWheelMoved(event);
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
        private final int hgap;
        private final int vgap;

        private SingleRowLayout(int hgap, int vgap) {
            this.hgap = hgap;
            this.vgap = vgap;
        }

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
                    reorderGapX = -1;
                    reorderGapWidth = 0;
                    return;
                }

                if (dragged == null) {
                    layoutNormalRow(parent, insets.left, y, availableHeight, null);
                    reorderGapX = -1;
                    reorderGapWidth = 0;
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
                java.util.HashSet<JToggleButton> placed = new java.util.HashSet<>();
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
