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
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
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

    @NotNull ComponentSubtabIconButton ruleSwitchButton() {
        return ruleSwitchButton;
    }

    void switchRuleForDisplayedFile() {
        ComponentSubtabsManager.rotateSubtabRuleForFile(project, displayedFile);
    }

    void refreshOpenStates() {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        ComponentSubtabGroupSplitRegistry.SplitState splitState = findActiveSplitState();

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
            ComponentSubtabBarPopup.install(project, button, relatedFile.file(), () -> displayedFile);
            overflowStrip.attachWheel(button);

            buttonGroup.add(button);
            tabsHost.add(button);
            buttonsByFile.put(relatedFile.file(), button);
            watchDocument(relatedFile.file());
        }

        refreshOpenStates();

        if (!dragInstalled) {
            ComponentSubtabEditorDragSupport.install(project, this, buttonsByFile, ignoreNextClick);
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
        if (buttonsByFile.isEmpty()) {
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

    private static final class SingleRowLayout implements LayoutManager {
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
                int x = insets.left;
                int y = insets.top + vgap;
                int availableHeight = parent.getHeight() - insets.top - insets.bottom - 2 * vgap;
                for (Component child : parent.getComponents()) {
                    if (!child.isVisible()) {
                        continue;
                    }
                    Dimension size = child.getPreferredSize();
                    int height = availableHeight > 0 ? availableHeight : size.height;
                    child.setBounds(x, y, size.width, height);
                    x += size.width + hgap;
                }
            }
        }

        private @NotNull Dimension measure(@NotNull Container parent, boolean minimumWidth) {
            synchronized (parent.getTreeLock()) {
                int width = 0;
                int height = ComponentSubtabUi.tabHeight();
                int visible = 0;
                for (Component child : parent.getComponents()) {
                    if (!child.isVisible()) {
                        continue;
                    }
                    Dimension size = child.getPreferredSize();
                    width += size.width;
                    height = Math.max(height, size.height);
                    visible++;
                }
                if (visible > 1) {
                    width += hgap * (visible - 1);
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
