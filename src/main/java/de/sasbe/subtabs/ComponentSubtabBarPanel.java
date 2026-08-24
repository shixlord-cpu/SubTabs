package de.sasbe.subtabs;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

final class ComponentSubtabBarPanel extends JPanel {
    private final Project project;
    private final JPanel tabsHost;
    private final JBScrollPane scrollPane;
    private final JButton collapseButton;
    private final Map<VirtualFile, JToggleButton> buttonsByFile = new HashMap<>();
    private final AtomicBoolean ignoreNextClick = new AtomicBoolean();
    private boolean dragInstalled;

    private ComponentSubtabGroup group;
    private VirtualFile displayedFile;

    ComponentSubtabBarPanel(
            @NotNull Project project,
            @NotNull ComponentSubtabGroup group,
            @NotNull VirtualFile displayedFile
    ) {
        super(new BorderLayout(0, 0));
        this.project = project;

        tabsHost = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(2), JBUI.scale(2)));
        tabsHost.setBackground(UIUtil.getPanelBackground());
        tabsHost.setBorder(BorderFactory.createEmptyBorder());

        scrollPane = new JBScrollPane(
                tabsHost,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(UIUtil.getPanelBackground());
        scrollPane.getViewport().setBackground(UIUtil.getPanelBackground());

        MouseWheelListener wheelListener = createHorizontalWheelListener(scrollPane);
        scrollPane.addMouseWheelListener(wheelListener);
        scrollPane.getViewport().addMouseWheelListener(wheelListener);
        tabsHost.addMouseWheelListener(wheelListener);

        collapseButton = createCollapseButton();

        setBorder(JBUI.Borders.empty(1, 6, 1, 0));
        add(scrollPane, BorderLayout.CENTER);
        add(collapseButton, BorderLayout.EAST);

        bind(group, displayedFile);
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
        }
    }

    private void rebuildButtonsIfNeeded() {
        if (!buttonsByFile.isEmpty()) {
            updateSelection(displayedFile);
            refreshOpenStates();
            return;
        }

        ButtonGroup buttonGroup = new ButtonGroup();
        MouseWheelListener wheelListener = createHorizontalWheelListener(scrollPane);

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
                    FileEditorManager manager = FileEditorManager.getInstance(project);
                    if (manager.isFileOpen(target) && !displayedFile.equals(target)) {
                        ComponentSubtabMainTabHover.onEnter(project, target, button);
                    }
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    ComponentSubtabMainTabHover.onExit(button);
                }
            });
            button.addMouseWheelListener(wheelListener);

            buttonGroup.add(button);
            tabsHost.add(button);
            buttonsByFile.put(relatedFile.file(), button);
        }

        refreshOpenStates();

        if (!dragInstalled) {
            ComponentSubtabEditorDragSupport.install(project, this, buttonsByFile, ignoreNextClick);
            dragInstalled = true;
        }
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

    private static @NotNull MouseWheelListener createHorizontalWheelListener(@NotNull JBScrollPane scrollPane) {
        return (MouseWheelEvent event) -> {
            JScrollBar horizontalBar = scrollPane.getHorizontalScrollBar();
            if (!horizontalBar.isVisible()) {
                return;
            }

            int direction = event.getWheelRotation();
            int amount = event.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL
                    ? event.getUnitsToScroll() * horizontalBar.getUnitIncrement()
                    : direction * horizontalBar.getBlockIncrement();
            horizontalBar.setValue(horizontalBar.getValue() + amount);
            event.consume();
        };
    }

    private @NotNull JButton createCollapseButton() {
        JButton button = new JButton(AllIcons.General.ChevronUp);
        button.setFocusable(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setToolTipText("Subtabs einklappen");
        button.getAccessibleContext().setAccessibleName("Subtabs einklappen");
        button.addActionListener(event -> SubtabsCollapseState.getInstance(project).toggle(project));
        return button;
    }
}
