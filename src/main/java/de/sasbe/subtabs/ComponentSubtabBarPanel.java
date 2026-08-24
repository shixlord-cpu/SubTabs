package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
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
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

final class ComponentSubtabBarPanel extends JPanel {
    private final Project project;
    private final TabStrip tabsHost;
    private final JBScrollPane scrollPane;
    private final SubtabOverflowStrip overflowStrip;
    private final ComponentSubtabIconButton collapseButton;
    private final Map<VirtualFile, JToggleButton> buttonsByFile = new HashMap<>();
    private final AtomicBoolean ignoreNextClick = new AtomicBoolean();
    private boolean dragInstalled;

    private ComponentSubtabGroup group;
    private VirtualFile displayedFile;
    private SubtabFitScale.Result fit = SubtabFitScale.Result.FULL;
    private int naturalStripWidth;

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
        scrollPane.setOverlappingScrollBar(false);
        JScrollBar horizontalBar = scrollPane.getHorizontalScrollBar();
        horizontalBar.setOpaque(true);
        horizontalBar.putClientProperty(JBScrollPane.IGNORE_SCROLLBAR_IN_INSETS, Boolean.FALSE);

        overflowStrip = new SubtabOverflowStrip(scrollPane, () -> tabsHost.getPreferredSize().width);

        collapseButton = createCollapseButton();
        applyChrome();
        add(overflowStrip, BorderLayout.CENTER);
        add(collapseButton, BorderLayout.EAST);

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
        applyChrome();
        revalidate();
        updateFitToEditorWidth();
        repaint();
    }

    void setCollapseButtonVisible(boolean visible) {
        collapseButton.setVisible(visible);
        revalidate();
        updateFitToEditorWidth();
        repaint();
    }

    void bind(@NotNull ComponentSubtabGroup group, @NotNull VirtualFile displayedFile) {
        this.group = group;
        this.displayedFile = displayedFile;
        rebuildButtonsIfNeeded();
        updateSelection(displayedFile);
        refreshOpenStates();
    }

    void setDisplayedFile(@NotNull VirtualFile displayedFile) {
        this.displayedFile = displayedFile;
        updateSelection(displayedFile);
        refreshOpenStates();
    }

    void refreshOpenStates() {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (var entry : buttonsByFile.entrySet()) {
            VirtualFile file = entry.getKey();
            boolean openElsewhere = manager.isFileOpen(file) && !file.equals(displayedFile);
            ComponentSubtabUi.setOpenElsewhere(entry.getValue(), openElsewhere);
            ComponentSubtabUi.setModified(entry.getValue(), ComponentSubtabModifiedUi.isModified(project, file));
        }
    }

    @Nullable JToggleButton buttonFor(@NotNull VirtualFile file) {
        return buttonsByFile.get(file);
    }

    private void applyChrome() {
        setBorder(JBUI.Borders.empty(
                ComponentSubtabUi.compactVertical(1),
                6,
                ComponentSubtabUi.compactVertical(1),
                0
        ));
        SubtabOverflowMode mode = SubtabsSettings.getInstance().getOverflowMode();
        overflowStrip.setMode(mode);
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
                ComponentSubtabNavigation.switchToRelatedFile(project, displayedFile, target);
            });
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    VirtualFile target = relatedFile.file();
                    ComponentSubtabProjectViewHover.onEnter(project, target, button);
                    FileEditorManager manager = FileEditorManager.getInstance(project);
                    if (manager.isFileOpen(target) && !displayedFile.equals(target)) {
                        ComponentSubtabMainTabHover.onEnter(project, target, button);
                    }
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    ComponentSubtabProjectViewHover.onExit(button);
                    ComponentSubtabMainTabHover.onExit(button);
                }
            });
            ComponentSubtabBarPopup.install(project, button, relatedFile.file(), () -> displayedFile);
            overflowStrip.attachWheel(button);

            buttonGroup.add(button);
            tabsHost.add(button);
            buttonsByFile.put(relatedFile.file(), button);
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
            int collapseWidth = collapseButton.isVisible() ? collapseButton.getPreferredSize().width : 0;
            width = Math.max(0, getWidth() - collapseWidth - getInsets().left - getInsets().right);
        }
        return width;
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
