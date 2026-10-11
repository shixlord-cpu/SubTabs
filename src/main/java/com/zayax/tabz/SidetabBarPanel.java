package com.zayax.tabz;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.FoldingListener;
import com.intellij.openapi.editor.ex.FoldingModelEx;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

final class SidetabBarPanel extends JPanel {
    static final String FOLDED_KEY = "componentTabz.sidetabFolded";
    static final String BLANK_KEY = "componentTabz.sidetabBlank";
    static final String SEGMENT_HOVER_KEY = "componentTabz.segmentHover";
    static final String SECTION_INDEX_KEY = "componentTabz.sidetabSectionIndex";
    private static final int OVERLAY_HIT_WIDTH = 40;
    private static final int OVERLAY_LINE_WIDTH = 12;
    private static final int OVERLAY_BLANK_LINE_WIDTH = 6;
    private static final int OVERLAY_SEGMENT_HEIGHT = 8;
    private static final int OVERLAY_SEGMENT_GAP = 5;
    private static final int OVERLAY_DOT_SIZE = 6;
    private static final int OVERLAY_EDGE_INSET = 3;
    private static final int OVERLAY_BAR_WIDTH = 32;
    private static final int DEPTH_INDENT = 8;
    static final String DEPTH_KEY = "componentTabz.sidetabDepth";
    private static final String OVERLAY_TABS_ON_RIGHT_KEY = "componentTabz.overlayTabsOnRight";

    static int defaultBesideColumnWidth() {
        return JBUI.scale(120);
    }

    static int subDepthIndentStep() {
        return JBUI.scale(DEPTH_INDENT);
    }

    static int subDepthTextIndent(int depth) {
        if (depth <= 0) {
            return 0;
        }
        return JBUI.scale(4) + depth * subDepthIndentStep();
    }

    /** @deprecated tests only — use {@link #subDepthTextIndent(int)} */
    @Deprecated
    static int subDepthDotBaseX() {
        return JBUI.scale(4);
    }

    static int subDepthDotOffset(int depth) {
        return subDepthTextIndent(Math.max(0, depth));
    }

    private final Project project;
    private final FileEditor fileEditor;
    private final JPanel tabsHost;
    private final JBScrollPane scrollPane;
    private final SidetabOverflowStrip overflowStrip;
    private final List<JToggleButton> buttons = new ArrayList<>();
    private Component topIconSpacer;
    private int topIconReserve;
    private List<SidetabSection> sections = List.of();
    private SidetabLayoutMode layoutMode = SidetabLayoutMode.BESIDE;
    private @Nullable JPanel overlaySectionStack;
    private boolean onRight = true;
    private final ComponentAdapter resizeListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent event) {
            updateSegmentSizes();
            if (layoutMode == SidetabLayoutMode.BESIDE) {
                ComponentSubtabsManager.refreshOverlayIconReserve(project, fileEditor);
            }
        }
    };
    private boolean besideResizeInstalled;
    private boolean besideResizeActive;
    private int besideResizeStartX;
    private int besideResizeStartWidth;
    private boolean besideResizePerFile;

    SidetabBarPanel(@NotNull Project project, @NotNull FileEditor fileEditor) {
        super(new BorderLayout(0, 0));
        this.project = project;
        this.fileEditor = fileEditor;
        setOpaque(true);
        setBackground(UIUtil.getPanelBackground());

        tabsHost = new JPanel();
        tabsHost.setOpaque(true);
        tabsHost.setBackground(UIUtil.getPanelBackground());
        tabsHost.setLayout(new BoxLayout(tabsHost, BoxLayout.Y_AXIS));
        tabsHost.setBorder(BorderFactory.createEmptyBorder(
                ComponentSubtabUi.verticalGap(),
                JBUI.scale(2),
                ComponentSubtabUi.verticalGap(),
                JBUI.scale(2)
        ));
        tabsHost.addComponentListener(resizeListener);
        addComponentListener(resizeListener);

        JBScrollPane scrollPane = new JBScrollPane(
                tabsHost,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        this.scrollPane = scrollPane;
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOverlappingScrollBar(false);
        TabzOverflowScrollBars.configureVertical(scrollPane.getVerticalScrollBar());

        overflowStrip = new SidetabOverflowStrip(scrollPane, () -> tabsHost.getPreferredSize().height);
        overflowStrip.attachWheel(tabsHost);
        add(overflowStrip, BorderLayout.CENTER);
        installBesideColumnResizeHandle();
    }

    private void installBesideColumnResizeHandle() {
        if (besideResizeInstalled) {
            return;
        }
        besideResizeInstalled = true;
        MouseAdapter resize = new MouseAdapter() {
            @Override
            public void mousePressed(@NotNull MouseEvent event) {
                if (!isBesideResizeZone(event)) {
                    return;
                }
                if (!SwingUtilities.isLeftMouseButton(event) && !SwingUtilities.isRightMouseButton(event)) {
                    return;
                }
                event.consume();
                besideResizeActive = true;
                besideResizeStartX = event.getXOnScreen();
                VirtualFile file = fileEditor.getFile();
                if (SwingUtilities.isLeftMouseButton(event) && file != null) {
                    besideResizePerFile = true;
                    besideResizeStartWidth = ComponentSubtabScopedVisibility.sidetabBesideColumnWidthForFile(
                            project,
                            file
                    );
                } else if (SwingUtilities.isRightMouseButton(event)) {
                    besideResizePerFile = false;
                    if (file != null) {
                        ComponentSubtabScopedVisibility.clearSidetabBesideColumnWidthForFile(project, file);
                    }
                    besideResizeStartWidth = TabzSettings.getInstance().getSidetabBesideColumnWidth();
                    applyLiveBesideColumnWidth(besideResizeStartWidth);
                } else {
                    besideResizeActive = false;
                }
            }

            @Override
            public void mouseDragged(@NotNull MouseEvent event) {
                if (!besideResizeActive) {
                    return;
                }
                event.consume();
                int delta = event.getXOnScreen() - besideResizeStartX;
                int next = ComponentSubtabScopedVisibility.clampSidetabBesideColumnWidth(
                        onRight ? besideResizeStartWidth - delta : besideResizeStartWidth + delta
                );
                if (besideResizePerFile) {
                    VirtualFile file = fileEditor.getFile();
                    if (file == null) {
                        return;
                    }
                    ComponentSubtabScopedVisibility.storeSidetabBesideColumnWidthForFile(project, file, next);
                    applyLiveBesideColumnWidth(next);
                    ComponentSubtabsManager.refreshOverlayIconReserve(project, fileEditor);
                } else {
                    TabzSettings.getInstance().setSidetabBesideColumnWidth(next);
                    SidetabsManager.applyBesideColumnWidth(project);
                }
            }

            @Override
            public void mouseReleased(@NotNull MouseEvent event) {
                if (!besideResizeActive) {
                    return;
                }
                besideResizeActive = false;
                besideResizeStartWidth = 0;
                besideResizePerFile = false;
                ComponentSubtabsManager.refreshOverlayIconReserve(project, fileEditor);
            }

            @Override
            public void mouseMoved(@NotNull MouseEvent event) {
                setCursor(isBesideResizeZone(event)
                        ? Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)
                        : Cursor.getDefaultCursor());
            }

            @Override
            public void mouseExited(@NotNull MouseEvent event) {
                setCursor(Cursor.getDefaultCursor());
            }
        };
        addMouseListener(resize);
        addMouseMotionListener(resize);
    }

    private boolean isBesideResizeZone(@NotNull MouseEvent event) {
        if (layoutMode != SidetabLayoutMode.BESIDE) {
            return false;
        }
        int zone = JBUI.scale(5);
        return onRight ? event.getX() <= zone : event.getX() >= getWidth() - zone;
    }

    void setTopIconReserve(int height) {
        if (height == topIconReserve) {
            return;
        }
        topIconReserve = height;
        if (topIconSpacer != null) {
            remove(topIconSpacer);
            topIconSpacer = null;
        }
        if (height > 0) {
            topIconSpacer = Box.createVerticalStrut(height);
            add(topIconSpacer, BorderLayout.NORTH);
        }
        revalidate();
        repaint();
    }

    int topIconReserve() {
        return topIconReserve;
    }

    static int overlayHitWidth() {
        return OVERLAY_HIT_WIDTH;
    }

    void bind(
            @NotNull List<SidetabSection> nextSections,
            @NotNull SidetabLayoutMode nextLayoutMode,
            boolean nextOnRight,
            @Nullable String selectedName,
            int caretOffset,
            int textLength
    ) {
        boolean sameSections = sectionsEqual(nextSections);
        boolean sameLayout = layoutMode == nextLayoutMode && onRight == nextOnRight;
        this.sections = List.copyOf(nextSections);
        this.layoutMode = nextLayoutMode;
        this.onRight = nextOnRight;
        if (sameSections && sameLayout && !buttons.isEmpty()) {
            refreshSelectionAndBlankStates(caretOffset, textLength, documentText());
            refreshFoldStates();
            refreshBarVisibility();
            return;
        }
        applyLayoutChrome();
        rebuildButtons(selectedName, caretOffset, textLength);
        refreshFoldStates();
        refreshBarVisibility();
        revalidate();
        repaint();
    }

    void refreshSelectionAndBlankStates(int caretOffset, int textLength, @NotNull String documentText) {
        selectOffset(caretOffset, textLength);
        for (JToggleButton button : buttons) {
            Object indexValue = button.getClientProperty(SECTION_INDEX_KEY);
            int index = indexValue instanceof Integer value ? value : buttons.indexOf(button);
            if (index < 0 || index >= sections.size()) {
                continue;
            }
            boolean blank = sections.get(index).isBlank(documentText);
            button.putClientProperty(BLANK_KEY, blank);
            if (button instanceof SegmentButton segmentButton) {
                segmentButton.repaint();
            } else {
                ComponentSubtabUi.refreshButton(button);
            }
        }
        refreshSectionPresentation();
    }

    void refreshSectionPresentation() {
        Editor editor = editor();
        if (editor == null || editor.isDisposed()) {
            return;
        }
        Document document = editor.getDocument();
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null) {
            return;
        }
        for (JToggleButton button : buttons) {
            Object indexValue = button.getClientProperty(SECTION_INDEX_KEY);
            int index = indexValue instanceof Integer value ? value : -1;
            if (index < 0 || index >= sections.size()) {
                continue;
            }
            SidetabSectionPresentation presentation = SidetabSectionPresentation.compute(
                    project,
                    document,
                    file,
                    sections.get(index)
            );
            applySectionPresentation(button, presentation.modified(), presentation.hasErrors());
        }
        refreshVisibleFoldedChildrenPopup();
    }

    void refreshVisibleFoldedChildrenPopup() {
        Editor editor = editor();
        if (editor == null || editor.isDisposed()) {
            return;
        }
        Document document = editor.getDocument();
        VirtualFile file = fileEditor.getFile();
        if (file == null) {
            return;
        }
        for (JToggleButton button : buttons) {
            SidetabFoldedChildrenPopup.refreshPresentation(
                    button,
                    project,
                    editor,
                    document,
                    file,
                    sections
            );
        }
    }

    private static void applySectionPresentation(
            @NotNull JToggleButton button,
            boolean modified,
            boolean hasErrors
    ) {
        if (button instanceof SegmentButton) {
            button.putClientProperty(ComponentSubtabUi.MODIFIED_KEY, modified);
            button.putClientProperty(ComponentSubtabUi.ERROR_KEY, hasErrors);
            button.repaint();
            return;
        }
        ComponentSubtabUi.setPresentation(button, modified, hasErrors);
    }

    private boolean sectionsEqual(@NotNull List<SidetabSection> nextSections) {
        if (sections.size() != nextSections.size()) {
            return false;
        }
        for (int index = 0; index < sections.size(); index++) {
            if (!sections.get(index).equals(nextSections.get(index))) {
                return false;
            }
        }
        return true;
    }

    boolean overlayMode() {
        return layoutMode == SidetabLayoutMode.OVERLAY;
    }

    void refreshOverlayStackLayout() {
        if (!overlayMode()) {
            return;
        }
        updateSegmentSizes();
    }

    @NotNull List<SidetabSection> sections() {
        return sections;
    }

    @Nullable String selectedName() {
        for (int index = 0; index < buttons.size(); index++) {
            if (buttons.get(index).isSelected() && index < sections.size()) {
                return sections.get(index).name();
            }
        }
        return null;
    }

    @Nullable JToggleButton buttonAt(int index) {
        if (index < 0 || index >= buttons.size()) {
            return null;
        }
        return buttons.get(index);
    }

    void selectOffset(int caretOffset, int textLength) {
        int index = indexForOffset(caretOffset, textLength);
        if (index < 0) {
            return;
        }
        for (int buttonIndex = 0; buttonIndex < buttons.size(); buttonIndex++) {
            buttons.get(buttonIndex).setSelected(buttonIndex == index);
        }
    }

    void refreshFoldStates() {
        Editor editor = editor();
        if (editor == null || editor.isDisposed()) {
            return;
        }
        for (int index = 0; index < buttons.size() && index < sections.size(); index++) {
            boolean folded = SidetabSectionFolding.isFolded(editor, sections.get(index));
            setFoldedAppearance(buttons.get(index), folded);
        }
    }

    void refreshBarVisibility() {
        Editor editor = editor();
        if (editor == null || editor.isDisposed()) {
            return;
        }
        for (int index = 0; index < buttons.size() && index < sections.size(); index++) {
            boolean hidden = SidetabSectionHierarchy.isHiddenByFoldedAncestor(editor, sections, index);
            buttons.get(index).setVisible(!hidden);
        }
        updateSegmentSizes();
        tabsHost.revalidate();
        tabsHost.repaint();
    }

    void refreshTabHighlights() {
        for (JToggleButton button : buttons) {
            if (button instanceof SegmentButton) {
                button.repaint();
            } else {
                ComponentSubtabUi.refreshButton(button);
            }
        }
    }

    void applyLiveBesideColumnWidth(int columnWidth) {
        if (layoutMode != SidetabLayoutMode.BESIDE) {
            return;
        }
        columnWidth = ComponentSubtabScopedVisibility.clampSidetabBesideColumnWidth(columnWidth);
        int height = getPreferredSize().height;
        if (height <= 0) {
            height = 1;
        }
        setPreferredSize(new Dimension(columnWidth, height));
        setMinimumSize(new Dimension(Math.min(columnWidth, JBUI.scale(48)), 0));
        setMaximumSize(new Dimension(columnWidth, Integer.MAX_VALUE));
        layoutBesideButtons();
        revalidate();
        repaint();
        Container parent = getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }

    void refreshAppearance() {
        tabsHost.setBorder(BorderFactory.createEmptyBorder(
                ComponentSubtabUi.verticalGap(),
                JBUI.scale(2),
                ComponentSubtabUi.verticalGap(),
                JBUI.scale(2)
        ));
        for (JToggleButton button : buttons) {
            if (button instanceof SegmentButton) {
                continue;
            }
            ComponentSubtabUi.refreshButton(button);
            if (Boolean.TRUE.equals(button.getClientProperty(FOLDED_KEY))) {
                setFoldedAppearance(button, true);
            }
        }
        applyLayoutChrome();
        updateSegmentSizes();
        refreshBarVisibility();
        revalidate();
        repaint();
        Container parent = getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }

    private void applyLayoutChrome() {
        overflowStrip.setMode(TabzSettings.getInstance().getOverflowMode());
        overflowStrip.setOverflowEnabled(true);
        if (layoutMode == SidetabLayoutMode.OVERLAY) {
            setBorder(BorderFactory.createEmptyBorder());
            setOpaque(false);
            tabsHost.setOpaque(false);
            tabsHost.setBorder(BorderFactory.createEmptyBorder());
            ensureOverlayScrollLayout();
            int barWidth = JBUI.scale(OVERLAY_HIT_WIDTH);
            setPreferredSize(new Dimension(barWidth, overlayStackHeight(Math.max(sections.size(), buttons.size()))));
            setMinimumSize(new Dimension(barWidth, 0));
            setMaximumSize(new Dimension(barWidth, Integer.MAX_VALUE));
            return;
        }

        ensureBesideScrollLayout();
        setOpaque(true);
        tabsHost.setOpaque(true);
        tabsHost.setBackground(UIUtil.getPanelBackground());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        applyBesideSeparatorBorder();
        tabsHost.setLayout(new BoxLayout(tabsHost, BoxLayout.Y_AXIS));
        tabsHost.setBorder(BorderFactory.createEmptyBorder(
                ComponentSubtabUi.verticalGap(),
                JBUI.scale(2),
                ComponentSubtabUi.verticalGap(),
                JBUI.scale(2)
        ));
        int columnWidth = besideColumnWidth();
        setPreferredSize(new Dimension(columnWidth, 1));
        setMinimumSize(new Dimension(Math.min(columnWidth, JBUI.scale(48)), 0));
        setMaximumSize(new Dimension(columnWidth, Integer.MAX_VALUE));
    }

    void refreshSeparatorBorder() {
        if (layoutMode != SidetabLayoutMode.BESIDE) {
            return;
        }
        applyBesideSeparatorBorder();
        repaint();
    }

    private void applyBesideSeparatorBorder() {
        setBorder(JBUI.Borders.customLine(
                SidetabSeparatorColors.forFile(fileEditor.getFile()),
                0,
                onRight ? 1 : 0,
                0,
                onRight ? 0 : 1
        ));
    }

    private void ensureOverlayScrollLayout() {
        if (tabsHost.getParent() == this) {
            remove(tabsHost);
        }
        if (scrollPane.getViewport().getView() != tabsHost) {
            scrollPane.setViewportView(tabsHost);
        }
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        if (overflowStrip.getParent() != this) {
            add(overflowStrip, BorderLayout.CENTER);
        }
    }

    private void ensureBesideScrollLayout() {
        if (tabsHost.getParent() == this) {
            remove(tabsHost);
        }
        if (scrollPane.getViewport().getView() != tabsHost) {
            scrollPane.setViewportView(tabsHost);
        }
        if (overflowStrip.getParent() != this) {
            add(overflowStrip, BorderLayout.CENTER);
        }
    }

    private int besideColumnWidth() {
        VirtualFile file = fileEditor.getFile();
        if (file != null) {
            return ComponentSubtabScopedVisibility.sidetabBesideColumnWidthForFile(project, file);
        }
        return TabzSettings.getInstance().getSidetabBesideColumnWidth();
    }

    private static int depthIndent(int depth) {
        return depth * JBUI.scale(DEPTH_INDENT);
    }

    private void rebuildButtons(@Nullable String selectedName, int caretOffset, int textLength) {
        tabsHost.removeAll();
        buttons.clear();
        ButtonGroup group = new ButtonGroup();
        int selectedIndex = indexForName(selectedName);
        if (selectedIndex < 0) {
            selectedIndex = indexForOffset(caretOffset, textLength);
        }
        if (selectedIndex < 0 && !sections.isEmpty()) {
            selectedIndex = 0;
        }

        String documentText = documentText();
        JPanel stackPanel = new JPanel();
        stackPanel.setOpaque(false);
        stackPanel.setLayout(new BoxLayout(stackPanel, BoxLayout.Y_AXIS));
        for (int index = 0; index < sections.size(); index++) {
            SidetabSection section = sections.get(index);
            JToggleButton button = createSectionButton(section, index, index == selectedIndex, documentText);
            int finalIndex = index;
            button.addActionListener(event -> activateSection(finalIndex));
            installFoldToggle(button, finalIndex);
            installSectionHover(button, finalIndex);
            group.add(button);
            buttons.add(button);
            stackPanel.add(button);
            if (layoutMode == SidetabLayoutMode.OVERLAY && index + 1 < sections.size()) {
                stackPanel.add(Box.createVerticalStrut(JBUI.scale(OVERLAY_SEGMENT_GAP)));
            }
        }
        mountSectionStack(stackPanel);
        updateSegmentSizes();
        refreshSectionPresentation();
    }

    private void mountSectionStack(@NotNull JPanel stackPanel) {
        if (layoutMode == SidetabLayoutMode.OVERLAY) {
            overlaySectionStack = stackPanel;
            tabsHost.removeAll();
            tabsHost.setLayout(null);
            tabsHost.add(stackPanel);
            return;
        }
        overlaySectionStack = null;
        tabsHost.setLayout(new BoxLayout(tabsHost, BoxLayout.Y_AXIS));
        tabsHost.add(stackPanel);
        tabsHost.add(Box.createVerticalGlue());
    }

    private @NotNull JToggleButton createSectionButton(
            @NotNull SidetabSection section,
            int index,
            boolean selected,
            @NotNull String documentText
    ) {
        boolean blank = section.isBlank(documentText);
        VirtualFile editorFile = fileEditor.getFile();
        if (layoutMode == SidetabLayoutMode.OVERLAY) {
            SegmentButton button = new SegmentButton();
            button.setSelected(selected);
            button.setToolTipText(section.name());
            button.putClientProperty(SECTION_INDEX_KEY, index);
            button.putClientProperty(BLANK_KEY, blank);
            button.putClientProperty(DEPTH_KEY, section.depth());
            button.putClientProperty(OVERLAY_TABS_ON_RIGHT_KEY, onRight);
            if (editorFile != null) {
                button.putClientProperty(ComponentSubtabUi.FILE_KEY, editorFile);
            }
            return button;
        }
        JToggleButton button = ComponentSubtabUi.createTabzButton(section.name(), selected);
        if (editorFile != null) {
            button.putClientProperty(ComponentSubtabUi.FILE_KEY, editorFile);
        }
        ComponentSubtabUi.setVerticalPlacement(button, onRight);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setToolTipText(section.name());
        button.putClientProperty(SECTION_INDEX_KEY, index);
        button.putClientProperty(BLANK_KEY, blank);
        button.putClientProperty(DEPTH_KEY, section.depth());
        if (section.depth() > 0) {
            // Sub-depth tabs paint themselves; segmented LAF rollover shifts the dot gutter on hover.
            button.putClientProperty("JButton.buttonType", null);
            button.setRolloverEnabled(false);
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
        }
        return button;
    }

    private void installFoldToggle(@NotNull JToggleButton button, int index) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(@NotNull MouseEvent event) {
                if (SwingUtilities.isRightMouseButton(event)) {
                    toggleFold(index);
                    event.consume();
                }
            }
        });
    }

    private void installSectionHover(@NotNull JToggleButton button, int index) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(@NotNull MouseEvent event) {
                if (index < 0 || index >= sections.size()) {
                    return;
                }
                Editor editor = editor();
                if (editor == null) {
                    return;
                }
                SidetabSection section = sections.get(index);
                if (SidetabSectionFolding.isFolded(editor, section)) {
                    List<Integer> hiddenChildren = hiddenDirectChildren(editor, index);
                    if (!hiddenChildren.isEmpty()) {
                        VirtualFile file = fileEditor.getFile();
                        if (file != null) {
                            SidetabFoldedChildrenPopup.show(
                                    project,
                                    editor,
                                    file,
                                    button,
                                    sections,
                                    hiddenChildren,
                                    childIndex -> expandAncestorsAndActivate(childIndex)
                            );
                        }
                        return;
                    }
                }
                SidetabEditorHover.show(editor, section);
            }

            @Override
            public void mouseExited(@NotNull MouseEvent event) {
                SidetabFoldedChildrenPopup.scheduleHide(button);
                Editor editor = editor();
                if (editor != null) {
                    SidetabEditorHover.hide(editor);
                }
            }
        });
    }

    private @NotNull List<Integer> hiddenDirectChildren(@NotNull Editor editor, int parentIndex) {
        List<Integer> hidden = new ArrayList<>();
        for (int childIndex : SidetabSectionHierarchy.directChildren(sections, parentIndex)) {
            if (SidetabSectionHierarchy.isHiddenByFoldedAncestor(editor, sections, childIndex)) {
                hidden.add(childIndex);
            }
        }
        return hidden;
    }

    private void expandAncestorsAndActivate(int index) {
        if (index < 0 || index >= sections.size()) {
            return;
        }
        Editor editor = editor();
        if (editor == null || editor.isDisposed()) {
            return;
        }
        SidetabSectionHierarchy.expandAncestors(editor, sections, index);
        refreshFoldStates();
        refreshBarVisibility();
        for (int buttonIndex = 0; buttonIndex < buttons.size(); buttonIndex++) {
            buttons.get(buttonIndex).setSelected(buttonIndex == index);
        }
        activateSection(index);
    }

    private void toggleFold(int index) {
        if (index < 0 || index >= sections.size()) {
            return;
        }
        SidetabSection section = sections.get(index);
        if (!section.foldable()) {
            return;
        }
        Editor editor = editor();
        if (editor == null || editor.isDisposed()) {
            return;
        }
        SidetabSectionFolding.toggle(editor, section);
        refreshFoldStates();
        refreshBarVisibility();
    }

    private static void setFoldedAppearance(@NotNull JToggleButton button, boolean folded) {
        button.putClientProperty(FOLDED_KEY, folded);
        if (button instanceof SegmentButton segmentButton) {
            segmentButton.repaint();
            return;
        }
        ComponentSubtabUi.refreshButton(button);
        button.setBorder(createBesideFoldBorder(button, folded, button.isSelected()));
    }

    private static @NotNull Border createBesideFoldBorder(
            @NotNull JToggleButton button,
            boolean folded,
            boolean selected
    ) {
        if (!folded) {
            return button.getBorder();
        }
        Color color = JBUI.CurrentTheme.Label.disabledForeground();
        Object vertical = button.getClientProperty("componentTabz.verticalSide");
        Border dashed;
        if ("right".equals(vertical)) {
            dashed = BorderFactory.createMatteBorder(0, 1, 0, 0, color);
        } else if ("left".equals(vertical)) {
            dashed = BorderFactory.createMatteBorder(0, 0, 0, 1, color);
        } else {
            dashed = BorderFactory.createMatteBorder(0, 0, 1, 0, color);
        }
        if (selected) {
            return BorderFactory.createCompoundBorder(dashed, button.getBorder());
        }
        return dashed;
    }

    static int overlayStackHeight(int sectionCount) {
        if (sectionCount <= 0) {
            return 0;
        }
        int gap = JBUI.scale(OVERLAY_SEGMENT_GAP);
        int segment = JBUI.scale(OVERLAY_SEGMENT_HEIGHT);
        return sectionCount * segment + Math.max(0, sectionCount - 1) * gap;
    }

    private void updateSegmentSizes() {
        if (layoutMode != SidetabLayoutMode.OVERLAY || buttons.isEmpty()) {
            layoutBesideButtons();
            return;
        }

        int segmentHeight = JBUI.scale(OVERLAY_SEGMENT_HEIGHT);
        int hitWidth = getWidth() > 0 ? getWidth() : JBUI.scale(OVERLAY_HIT_WIDTH);
        for (JToggleButton button : buttons) {
            boolean blank = Boolean.TRUE.equals(button.getClientProperty(BLANK_KEY));
            int lineWidth = blank ? OVERLAY_BLANK_LINE_WIDTH : OVERLAY_LINE_WIDTH;
            button.putClientProperty("overlayLineWidth", lineWidth);
            Dimension size = new Dimension(hitWidth, segmentHeight);
            button.setPreferredSize(size);
            button.setMinimumSize(size);
            button.setMaximumSize(size);
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        int stackHeight = overlayStackHeight(buttons.size());
        Dimension stackSize = new Dimension(hitWidth, stackHeight);
        int viewportHeight = scrollPane.getViewport().getHeight();
        if (viewportHeight <= 0) {
            viewportHeight = Math.max(getHeight(), stackHeight);
        }
        updateOverlayTabsHostSize(hitWidth, stackHeight, viewportHeight);
        int panelHeight = getHeight() > 0 ? getHeight() : stackHeight;
        setPreferredSize(new Dimension(hitWidth, panelHeight));
        setMinimumSize(new Dimension(hitWidth, 0));
        setMaximumSize(new Dimension(hitWidth, Integer.MAX_VALUE));
        layoutOverlayTabsHost();
        tabsHost.revalidate();
        tabsHost.repaint();
        overflowStrip.revalidate();
        revalidate();
    }

    @Override
    public void doLayout() {
        super.doLayout();
        layoutOverlayTabsHost();
    }

    private void layoutOverlayTabsHost() {
        if (layoutMode != SidetabLayoutMode.OVERLAY || getHeight() <= 0 || buttons.isEmpty()) {
            return;
        }
        int hitWidth = getWidth() > 0 ? getWidth() : JBUI.scale(OVERLAY_HIT_WIDTH);
        int stackHeight = overlayStackHeight(buttons.size());
        int viewportHeight = scrollPane.getViewport().getHeight();
        if (viewportHeight <= 0) {
            viewportHeight = overflowStrip.getHeight();
        }
        if (viewportHeight <= 0) {
            viewportHeight = getHeight();
        }
        updateOverlayTabsHostSize(hitWidth, stackHeight, viewportHeight);
        layoutOverlaySectionStack(hitWidth, stackHeight);
        scrollPane.getViewport().revalidate();
        tabsHost.revalidate();
    }

    private void layoutOverlaySectionStack(int hitWidth, int stackHeight) {
        JPanel stack = overlaySectionStack;
        if (stack == null) {
            return;
        }
        int hostWidth = Math.max(tabsHost.getWidth(), hitWidth);
        int hostHeight = Math.max(tabsHost.getHeight(), tabsHost.getPreferredSize().height);
        int y = stackHeight <= hostHeight ? (hostHeight - stackHeight) / 2 : 0;
        stack.setBounds(0, y, hostWidth, stackHeight);
        stack.doLayout();
    }

    /**
     * Keeps the overlay marker stack vertically centered in the code window while it fits; once the stack
     * is taller than the viewport, scrolling or overflow arrows take over at the strip edge.
     */
    private void updateOverlayTabsHostSize(int hitWidth, int stackHeight, int viewportHeight) {
        int viewHeight = Math.max(stackHeight, Math.max(viewportHeight, 0));
        Dimension viewSize = new Dimension(hitWidth, viewHeight);
        tabsHost.setPreferredSize(viewSize);
        tabsHost.setMinimumSize(new Dimension(hitWidth, stackHeight));
        tabsHost.setMaximumSize(new Dimension(hitWidth, Integer.MAX_VALUE));
        if (viewHeight > stackHeight) {
            tabsHost.setSize(viewSize);
            scrollPane.getViewport().setViewSize(viewSize);
        }
        layoutOverlaySectionStack(hitWidth, stackHeight);
    }

    private void layoutBesideButtons() {
        int columnWidth = besideColumnWidth();
        for (int index = 0; index < buttons.size(); index++) {
            JToggleButton button = buttons.get(index);
            Dimension size = new Dimension(columnWidth, ComponentSubtabUi.tabHeight());
            button.setPreferredSize(size);
            button.setMinimumSize(size);
            button.setMaximumSize(size);
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        int stackHeight = buttons.size() * ComponentSubtabUi.tabHeight();
        if (getWidth() > 0) {
            tabsHost.setMaximumSize(new Dimension(columnWidth, Integer.MAX_VALUE));
        }
        tabsHost.setPreferredSize(new Dimension(columnWidth, stackHeight));
        tabsHost.setMinimumSize(new Dimension(columnWidth, stackHeight));
    }

    private void activateSection(int index) {
        if (index < 0 || index >= sections.size()) {
            return;
        }
        Editor editor = editor();
        if (editor == null || editor.isDisposed()) {
            return;
        }
        int offset = Math.max(0, Math.min(sections.get(index).startOffset(), editor.getDocument().getTextLength()));
        editor.getCaretModel().moveToOffset(offset);
        editor.getScrollingModel().scrollToCaret(com.intellij.openapi.editor.ScrollType.MAKE_VISIBLE);
        editor.getContentComponent().requestFocusInWindow();
    }

    private int indexForName(@Nullable String name) {
        if (name == null) {
            return -1;
        }
        for (int index = 0; index < sections.size(); index++) {
            if (name.equals(sections.get(index).name())) {
                return index;
            }
        }
        return -1;
    }

    private int indexForOffset(int caretOffset, int textLength) {
        Editor editor = editor();
        int bestIndex = -1;
        int bestDepth = -1;
        for (int index = 0; index < sections.size(); index++) {
            if (editor != null && SidetabSectionHierarchy.isHiddenByFoldedAncestor(editor, sections, index)) {
                continue;
            }
            if (!sections.get(index).contains(caretOffset, textLength)) {
                continue;
            }
            int depth = sections.get(index).depth();
            if (depth > bestDepth) {
                bestDepth = depth;
                bestIndex = index;
            }
        }
        if (bestIndex >= 0 || editor == null) {
            return bestIndex;
        }
        for (int index = 0; index < sections.size(); index++) {
            if (!sections.get(index).contains(caretOffset, textLength)) {
                continue;
            }
            int candidate = index;
            while (candidate >= 0
                    && SidetabSectionHierarchy.isHiddenByFoldedAncestor(editor, sections, candidate)) {
                candidate = SidetabSectionHierarchy.parentIndex(sections, candidate);
            }
            if (candidate >= 0) {
                return candidate;
            }
        }
        return -1;
    }

    private @Nullable Editor editor() {
        return fileEditor instanceof TextEditor textEditor ? textEditor.getEditor() : null;
    }

    private @NotNull String documentText() {
        Editor editor = editor();
        if (editor == null || editor.isDisposed()) {
            return "";
        }
        return editor.getDocument().getText();
    }

    static void installFoldingListener(@NotNull FileEditor fileEditor, @NotNull SidetabBarPanel panel) {
        uninstallFoldingListener(fileEditor);
        if (!(fileEditor instanceof TextEditor textEditor)) {
            return;
        }
        Editor editor = textEditor.getEditor();
        if (!(editor.getFoldingModel() instanceof FoldingModelEx modelEx)) {
            return;
        }
        Disposable disposable = Disposer.newDisposable("SidetabFoldingListener");
        FoldingListener listener = new FoldingListener() {
            @Override
            public void onFoldProcessingEnd() {
                panel.refreshFoldStates();
                panel.refreshBarVisibility();
            }
        };
        modelEx.addListener(listener, disposable);
        fileEditor.putUserData(FOLDING_DISPOSABLE_KEY, disposable);
    }

    static void uninstallFoldingListener(@NotNull FileEditor fileEditor) {
        Disposable disposable = fileEditor.getUserData(FOLDING_DISPOSABLE_KEY);
        if (disposable != null) {
            Disposer.dispose(disposable);
            fileEditor.putUserData(FOLDING_DISPOSABLE_KEY, null);
        }
    }

    private static final Key<Disposable> FOLDING_DISPOSABLE_KEY =
            Key.create("componentTabz.sidetabFoldingDisposable");

    private static final class SegmentButton extends JToggleButton {
        private SegmentButton() {
            setFocusable(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setText("");
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addChangeListener(event -> repaint());
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    putClientProperty(SEGMENT_HOVER_KEY, Boolean.TRUE);
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    putClientProperty(SEGMENT_HOVER_KEY, Boolean.FALSE);
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(@NotNull Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                int width = getWidth();
                int height = getHeight();
                if (width <= 0 || height <= 0) {
                    return;
                }
                boolean folded = Boolean.TRUE.equals(getClientProperty(FOLDED_KEY));
                boolean blank = Boolean.TRUE.equals(getClientProperty(BLANK_KEY));
                boolean modified = Boolean.TRUE.equals(getClientProperty(ComponentSubtabUi.MODIFIED_KEY));
                boolean hasErrors = Boolean.TRUE.equals(getClientProperty(ComponentSubtabUi.ERROR_KEY));
                boolean hovered = segmentHovered();
                boolean selected = isSelected();
                Object lineWidthValue = getClientProperty("overlayLineWidth");
                int barHeight = lineWidthValue instanceof Integer value
                        ? Math.max(1, JBUI.scale(value))
                        : JBUI.scale(OVERLAY_LINE_WIDTH);
                if (!folded && (selected || hovered)) {
                    barHeight = Math.max(barHeight, JBUI.scale(OVERLAY_LINE_WIDTH + (selected ? 1 : 0)));
                }

                g.setColor(overlayMarkerColor(this, selected, hovered, blank, modified, hasErrors));
                int edgeInset = JBUI.scale(OVERLAY_EDGE_INSET);
                int centerY = height / 2;
                Object depthValue = getClientProperty(DEPTH_KEY);
                int depth = depthValue instanceof Integer value ? Math.max(0, value) : 0;
                OverlayMarkerGeometry geometry = overlayMarkerGeometry(
                        width,
                        edgeInset,
                        depth
                );

                if (folded) {
                    int dotSize = JBUI.scale(OVERLAY_DOT_SIZE);
                    int dotX = geometry.centerX - dotSize / 2;
                    g.fillOval(dotX, centerY - dotSize / 2, dotSize, dotSize);
                    return;
                }

                int barY = centerY - barHeight / 2;
                g.fillRect(geometry.barX, barY, geometry.barWidth, barHeight);
            } finally {
                g.dispose();
            }
        }

        private static @NotNull OverlayMarkerGeometry overlayMarkerGeometry(
                int width,
                int edgeInset,
                int depth
        ) {
            int rightEdge = width - edgeInset;
            int maxBarWidth = Math.min(
                    JBUI.scale(OVERLAY_BAR_WIDTH),
                    Math.max(JBUI.scale(4), rightEdge - edgeInset)
            );
            int barWidth = maxBarWidth;
            if (depth > 0) {
                barWidth = Math.max(JBUI.scale(4), maxBarWidth - depthIndent(depth));
            }
            int barX = rightEdge - barWidth;
            return new OverlayMarkerGeometry(barX, barWidth, barX + barWidth / 2);
        }

        private record OverlayMarkerGeometry(int barX, int barWidth, int centerX) {
        }

        private static @NotNull Color overlayMarkerColor(
                @NotNull JToggleButton button,
                boolean selected,
                boolean hovered,
                boolean blank,
                boolean modified,
                boolean hasErrors
        ) {
            if (hasErrors) {
                return overlayOpacity(ComponentSubtabTextPainter.errorWaveColor(), hovered || selected);
            }
            if (modified) {
                return overlayOpacity(ComponentSubtabModifiedUi.foreground(true, !selected && !hovered), hovered || selected);
            }
            Color base;
            if (selected) {
                Color groupColor = null;
                if (ApplicationManager.getApplication() != null) {
                    groupColor = SubtabGroupColors.colorForFile(ComponentSubtabUi.tabFile(button));
                }
                Color foreground = UIUtil.getLabelForeground();
                base = groupColor != null ? groupColor : foreground != null ? foreground : Color.GRAY;
            } else if (hovered) {
                base = JBUI.CurrentTheme.TabbedPane.HOVER_COLOR;
            } else {
                Color muted = UIUtil.getInactiveTextColor();
                if (muted == null) {
                    muted = Color.GRAY;
                }
                base = blank
                        ? new Color(muted.getRed(), muted.getGreen(), muted.getBlue(), 120)
                        : muted;
            }
            return overlayOpacity(base, hovered);
        }

        private static @NotNull Color overlayOpacity(@NotNull Color color, boolean hovered) {
            if (hovered) {
                return color;
            }
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), 128);
        }

        private boolean segmentHovered() {
            return Boolean.TRUE.equals(getClientProperty(SEGMENT_HOVER_KEY)) || getModel().isRollover();
        }
    }
}
