package com.zayax.tabz;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

final class SplittabSwitchBarPanel extends JPanel implements ComponentSubtabReorderStripHost {
    static final String PAIR_ID_KEY = "componentTabz.splittabPairId";

    private final Project project;
    private final ComponentSubtabReorderStripSupport reorderStrip;
    private final JPanel tabsHost;
    private final JBScrollPane scrollPane;
    private final SubtabOverflowStrip overflowStrip;
    /** Unique per pair tab (right file when several pairs share the same left anchor). */
    private final Map<VirtualFile, JToggleButton> buttonsByDragKey = new HashMap<>();
    private final AtomicBoolean ignoreNextClick = new AtomicBoolean();
    private final SubtabFitScale.Result fit = SubtabFitScale.Result.FULL;
    private boolean dragInstalled;
    private int overlayIconRightReserve;

    SplittabSwitchBarPanel(@NotNull Project project) {
        super(new BorderLayout(0, 0));
        this.project = project;
        reorderStrip = new ComponentSubtabReorderStripSupport(
                buttonsByDragKey,
                (buttonsByFile) -> {
                    List<JToggleButton> ordered = new ArrayList<>();
                    for (ComponentSubtabEditorSplitRegistry.SplittabPair pair
                            : ComponentSubtabEditorSplitRegistry.getInstance(project).all()) {
                        JToggleButton button = buttonsByFile.get(pairDragKey(pair));
                        if (button != null) {
                            ordered.add(button);
                        }
                    }
                    return ordered;
                },
                ComponentSubtabUi.horizontalGap(fit),
                ComponentSubtabUi.verticalGap()
        );
        tabsHost = reorderStrip.tabsHost();
        scrollPane = new JBScrollPane(
                tabsHost,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        JScrollBar horizontalBar = scrollPane.getHorizontalScrollBar();
        TabzOverflowScrollBars.configureHorizontal(horizontalBar);
        horizontalBar.putClientProperty(JBScrollPane.IGNORE_SCROLLBAR_IN_INSETS, Boolean.FALSE);
        scrollPane.setOverlappingScrollBar(false);

        overflowStrip = new SubtabOverflowStrip(scrollPane, () -> tabsHost.getPreferredSize().width);
        overflowStrip.attachWheel(tabsHost);
        reorderStrip.setWheelForwarder(overflowStrip.wheelListener());
        add(overflowStrip, BorderLayout.CENTER);
        applyChrome();
    }

    void applyOverflowSettings() {
        applyChrome();
    }

    void setOverlayIconRightReserve(int pixels) {
        int next = Math.max(0, pixels);
        if (overlayIconRightReserve == next) {
            return;
        }
        overlayIconRightReserve = next;
        overflowStrip.setRightReserve(next);
        revalidate();
        repaint();
    }

    @TestOnly
    int pairTabButtonCount() {
        int count = 0;
        for (Component child : tabsHost.getComponents()) {
            if (child instanceof JToggleButton) {
                count++;
            }
        }
        return count;
    }

    @org.jetbrains.annotations.Nullable JToggleButton buttonForPairId(@NotNull String pairId) {
        for (Component child : tabsHost.getComponents()) {
            if (child instanceof JToggleButton button && pairId.equals(button.getClientProperty(PAIR_ID_KEY))) {
                return button;
            }
        }
        return null;
    }

    @TestOnly
    @NotNull JToggleButton pairButtonForTests(@NotNull String pairId) {
        JToggleButton button = buttonForPairId(pairId);
        if (button == null) {
            throw new IllegalStateException("No switch-bar button for pair " + pairId);
        }
        return button;
    }

    @TestOnly
    void clickPairForTests(@NotNull String pairId) {
        for (Component child : tabsHost.getComponents()) {
            if (child instanceof JToggleButton button && pairId.equals(button.getClientProperty(PAIR_ID_KEY))) {
                button.doClick(0);
                return;
            }
        }
        throw new IllegalStateException("No switch-bar button for pair " + pairId);
    }

    @TestOnly
    int reorderGapXForTests() {
        return reorderStrip.reorderGapXForTests();
    }

    @TestOnly
    int reorderGapWidthForTests() {
        return reorderStrip.reorderGapWidthForTests();
    }

    @TestOnly
    void layOutTabsForTests(int width) {
        int rowHeight = ComponentSubtabUi.barRowHeight()
                + tabsHost.getInsets().top
                + tabsHost.getInsets().bottom
                + 2 * ComponentSubtabUi.verticalGap();
        setSize(width, stablePanelHeight());
        validate();
        doLayout();
        tabsHost.setSize(width, rowHeight);
        tabsHost.validate();
        tabsHost.doLayout();
    }

    void refreshActiveSelection() {
        ComponentSubtabEditorSplitRegistry.SplittabPair active =
                ComponentSubtabEditorSplitRegistry.getInstance(project).activePair();
        String activeId = active != null ? active.id() : null;
        for (Component child : tabsHost.getComponents()) {
            if (!(child instanceof JToggleButton button)) {
                continue;
            }
            Object pairId = button.getClientProperty(PAIR_ID_KEY);
            button.setSelected(pairId instanceof String id && id.equals(activeId));
        }
    }

    void refresh() {
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair active = registry.activePair();

        tabsHost.removeAll();
        buttonsByDragKey.clear();
        clearReorderPreview();

        int index = 0;
        for (ComponentSubtabEditorSplitRegistry.SplittabPair pair : registry.all()) {
            String label = ComponentSubtabEditorSplitPresentation.linkBarText(pair, index);
            boolean selected = active != null && active.id().equals(pair.id());
            JToggleButton button = ComponentSubtabUi.createTabzButton(label, selected);
            button.putClientProperty(PAIR_ID_KEY, pair.id());
            VirtualFile dragKey = pairDragKey(pair);
            button.putClientProperty(ComponentSubtabUi.FILE_KEY, dragKey);
            button.setToolTipText(ComponentSubtabEditorSplitPresentation.paneHeaderText(pair));
            button.getAccessibleContext().setAccessibleName(
                    label + " split pair: " + pair.leftFile().getName() + " and " + pair.rightFile().getName()
            );
            button.addActionListener(event -> {
                if (ignoreNextClick.getAndSet(false)) {
                    return;
                }
                Object pairId = button.getClientProperty(PAIR_ID_KEY);
                if (pairId instanceof String id) {
                    ComponentSubtabEditorSplitNavigation.activatePair(project, id);
                }
            });
            installPairHoverSync(button, pair);
            ComponentSubtabBarPopup.installSplittabPopup(project, button, pair.id(), this);
            buttonsByDragKey.put(dragKey, button);
            tabsHost.add(button);
            overflowStrip.attachWheel(button);
            index++;
        }

        reorderStrip.syncTabsHostChildren();

        if (!dragInstalled) {
            ComponentSubtabEditorDragSupport.install(
                    project,
                    this,
                    this,
                    buttonsByDragKey,
                    ignoreNextClick,
                    ComponentSubtabEditorSplitOrder::reorderByPairDragKey
            );
            dragInstalled = true;
        }

        revalidate();
        repaint();
    }

    private void installPairHoverSync(
            @NotNull JToggleButton button,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        if (Boolean.TRUE.equals(button.getClientProperty(SPLITTAB_SWITCH_HOVER_INSTALLED))) {
            return;
        }
        button.putClientProperty(SPLITTAB_SWITCH_HOVER_INSTALLED, Boolean.TRUE);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                if (!SubtabHoverView.isEnabled()) {
                    return;
                }
                if (!ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(project)) {
                    return;
                }
                ComponentSubtabProjectViewHover.onEnterSplittabPair(
                        project,
                        pair,
                        pair.leftFile(),
                        button
                );
            }

            @Override
            public void mouseExited(MouseEvent event) {
                if (SubtabHoverView.isEnabled()) {
                    ComponentSubtabProjectViewHover.onExit(button);
                }
            }
        });
    }

    private static final String SPLITTAB_SWITCH_HOVER_INSTALLED =
            "componentTabz.splittabSwitchHoverInstalled";

    @TestOnly
    boolean isPairHoverSyncInstalledForTests(@NotNull JToggleButton button) {
        return Boolean.TRUE.equals(button.getClientProperty(SPLITTAB_SWITCH_HOVER_INSTALLED));
    }

    private void applyChrome() {
        setBorder(JBUI.Borders.empty(
                ComponentSubtabUi.compactVertical(1),
                6,
                ComponentSubtabUi.compactVertical(1),
                0
        ));
        SubtabOverflowMode mode = TabzSettings.getInstance().getOverflowMode();
        overflowStrip.setMode(mode);
        scrollPane.setOverlappingScrollBar(mode == SubtabOverflowMode.ARROWS);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(super.getPreferredSize().width, stablePanelHeight());
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0, stablePanelHeight());
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension preferred = super.getPreferredSize();
        return new Dimension(preferred.width, stablePanelHeight());
    }

    private int stablePanelHeight() {
        Insets insets = getInsets();
        int rowHeight = ComponentSubtabUi.barRowHeight();
        if (TabzSettings.getInstance().getOverflowMode() == SubtabOverflowMode.SCROLLBAR) {
            rowHeight += TabzOverflowScrollBars.horizontalReserve(SubtabOverflowMode.SCROLLBAR);
        }
        return insets.top + rowHeight + insets.bottom;
    }

    @Override
    public void clearReorderPreview() {
        reorderStrip.clearReorderPreview();
    }

    @Override
    public void updateReorderDragState(
            int index,
            @Nullable VirtualFile draggedFile,
            @Nullable Point pointerInTabsHost
    ) {
        reorderStrip.updateReorderDragState(index, draggedFile, pointerInTabsHost);
    }

    @Override
    public @NotNull Point pointerInTabsHostFromEvent(@NotNull MouseEvent event) {
        Point inPanel = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), this);
        Point inTabs = SwingUtilities.convertPoint(this, inPanel, tabsHost);
        if (tabsHost.getHeight() > 0) {
            inTabs.y = Math.max(0, Math.min(tabsHost.getHeight() - 1, inTabs.y));
        }
        return inTabs;
    }

    @Override
    public int resolveReorderDropIndex(@NotNull Point pointerInTabsHost, @NotNull VirtualFile draggedFile) {
        return reorderStrip.resolveReorderDropIndex(pointerInTabsHost, draggedFile);
    }

    @Override
    public int tabIndexForFile(@NotNull VirtualFile file) {
        return reorderStrip.tabIndexForFile(file);
    }

    private static @NotNull VirtualFile pairDragKey(
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        return pair.rightFile();
    }
}
