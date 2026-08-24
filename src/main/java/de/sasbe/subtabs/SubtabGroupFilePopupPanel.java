package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.PopupHandler;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

final class SubtabGroupFilePopupPanel extends JPanel {
    private static final String FILE_KEY = "componentSubtabs.popupFile";

    SubtabGroupFilePopupPanel(
            @NotNull Project project,
            @NotNull List<VirtualFile> files,
            int fixedWidth,
            @NotNull Consumer<VirtualFile> onSelect,
            @NotNull Runnable onMouseLeave
    ) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(JBUI.Borders.empty(2));
        setBackground(UIUtil.getPanelBackground());

        for (VirtualFile file : files) {
            add(createItem(project, file, fixedWidth, onSelect));
        }

        if (fixedWidth > 0) {
            Dimension preferred = getPreferredSize();
            setPreferredSize(new Dimension(fixedWidth, preferred.height));
            setMinimumSize(new Dimension(fixedWidth, preferred.height));
            setMaximumSize(new Dimension(fixedWidth, preferred.height));
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                SwingUtilities.invokeLater(() -> {
                    Point pointer = new Point(event.getLocationOnScreen());
                    Point local = new Point(pointer);
                    SwingUtilities.convertPointFromScreen(local, SubtabGroupFilePopupPanel.this);
                    if (!SubtabGroupFilePopupPanel.this.contains(local)) {
                        onMouseLeave.run();
                    }
                });
            }
        });
    }

    void refreshPresentation(@NotNull Project project) {
        for (Component component : getComponents()) {
            if (!(component instanceof ComponentSubtabModifiedLabel label)) {
                continue;
            }
            Object value = label.getClientProperty(FILE_KEY);
            if (value instanceof VirtualFile file) {
                applyPresentationState(project, label, file);
            }
        }
    }

    private static @NotNull JComponent createItem(
            @NotNull Project project,
            @NotNull VirtualFile file,
            int fixedWidth,
            @NotNull Consumer<VirtualFile> onSelect
    ) {
        String plainLabel = labelFor(file);
        ComponentSubtabModifiedLabel label = new ComponentSubtabModifiedLabel();
        label.putClientProperty(FILE_KEY, file);
        label.setBorder(JBUI.Borders.empty(4, 8));
        label.setOpaque(true);
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setToolTipText(file.getPath());
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.getAccessibleContext().setAccessibleName(plainLabel + " öffnen: " + file.getName());
        applyPresentationState(project, label, file);
        applyItemWidth(label, fixedWidth);

        Color hoverBackground = JBUI.CurrentTheme.TabbedPane.HOVER_COLOR;

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                label.setBackground(hoverBackground);
                ComponentSubtabBarHover.onEnter(project, file, label);
                if (isOpen(project, file)) {
                    ComponentSubtabMainTabHover.onEnter(project, file, label);
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                applyPresentationState(project, label, file);
                ComponentSubtabBarHover.onExit(label);
                ComponentSubtabMainTabHover.onExit(label);
            }

            @Override
            public void mousePressed(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event)) {
                    onSelect.accept(file);
                }
            }
        });

        if (!isOpen(project, file)) {
            label.addMouseListener(new PopupHandler() {
                @Override
                public void invokePopup(@NotNull Component component, int x, int y) {
                    ComponentSubtabBarPopup.showContextMenu(project, file, component, x, y);
                }
            });
        }

        return label;
    }

    private static void applyItemWidth(@NotNull ComponentSubtabModifiedLabel label, int fixedWidth) {
        if (fixedWidth <= 0) {
            return;
        }
        Dimension preferred = label.getPreferredSize();
        Dimension size = new Dimension(fixedWidth, preferred.height);
        label.setPreferredSize(size);
        label.setMinimumSize(size);
        label.setMaximumSize(size);
    }

    private static boolean isOpen(@NotNull Project project, @NotNull VirtualFile file) {
        return FileEditorManager.getInstance(project).isFileOpen(file);
    }

    private static void applyPresentationState(
            @NotNull Project project,
            @NotNull ComponentSubtabModifiedLabel label,
            @NotNull VirtualFile file
    ) {
        boolean open = isOpen(project, file);
        boolean modified = ComponentSubtabModifiedUi.isModified(project, file);
        String plainLabel = labelFor(file);
        ComponentSubtabModifiedUi.applyToLabel(
                label,
                plainLabel,
                modified,
                open
        );
        label.setBackground(UIUtil.getPanelBackground());
    }

    private static @NotNull String labelFor(@NotNull VirtualFile file) {
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(file);
        if (match != null) {
            for (ComponentRelatedFiles.Entry entry : match.relatedFiles()) {
                if (entry.file().equals(file)) {
                    return entry.label();
                }
            }
        }
        return file.getName();
    }
}
