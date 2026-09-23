package de.sasbe.subtabs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.PopupHandler;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

final class SubtabGroupFilePopupPanel extends JPanel {
    private static final String FILE_KEY = "componentSubtabs.popupFile";
    private static final String CONTEXT_KEY = "componentSubtabs.popupContext";

    record MainTabProjectViewHover(
            @NotNull Project project,
            @NotNull VirtualFile tabFile,
            @NotNull JComponent tabLabel,
            @NotNull Runnable restoreTabGroupHover
    ) {
    }

    private SubtabGroupPopupPresentation.Context presentationContext = SubtabGroupPopupPresentation.Context.projectView();
    private final @Nullable MainTabProjectViewHover mainTabProjectViewHover;

    SubtabGroupFilePopupPanel(
            @NotNull Project project,
            @NotNull List<VirtualFile> files,
            int fixedWidth,
            @NotNull SubtabGroupPopupPresentation.Context presentationContext,
            @NotNull Consumer<VirtualFile> onSelect,
            @NotNull Runnable onMouseLeave
    ) {
        this(project, files, fixedWidth, presentationContext, onSelect, onMouseLeave, null);
    }

    SubtabGroupFilePopupPanel(
            @NotNull Project project,
            @NotNull List<VirtualFile> files,
            int fixedWidth,
            @NotNull SubtabGroupPopupPresentation.Context presentationContext,
            @NotNull Consumer<VirtualFile> onSelect,
            @NotNull Runnable onMouseLeave,
            @Nullable MainTabProjectViewHover mainTabProjectViewHover
    ) {
        super();
        this.presentationContext = presentationContext;
        this.mainTabProjectViewHover = mainTabProjectViewHover;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(JBUI.Borders.empty(2));
        setBackground(UIUtil.getPanelBackground());
        putClientProperty(CONTEXT_KEY, presentationContext);

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

    void refreshPresentation(@NotNull Project project, @NotNull SubtabGroupPopupPresentation.Context context) {
        presentationContext = context;
        putClientProperty(CONTEXT_KEY, context);
        for (Component component : getComponents()) {
            if (!(component instanceof ComponentSubtabModifiedLabel label)) {
                continue;
            }
            Object value = label.getClientProperty(FILE_KEY);
            if (value instanceof VirtualFile file) {
                applyPresentationState(project, label, file, context);
            }
        }
    }

    void refreshModifiedStateForFile(
            @NotNull Project project,
            @NotNull VirtualFile file,
            boolean modified,
            boolean hasErrors
    ) {
        if (!isShowing()) {
            return;
        }
        SubtabGroupPopupPresentation.Context context = context();
        for (Component component : getComponents()) {
            if (!(component instanceof ComponentSubtabModifiedLabel label)) {
                continue;
            }
            Object value = label.getClientProperty(FILE_KEY);
            if (!file.equals(value)) {
                continue;
            }
            boolean highlighted = SubtabGroupPopupPresentation.isHighlighted(context, file);
            boolean openElsewhere = SubtabGroupPopupPresentation.isOpenElsewhere(project, context, file);
            ComponentSubtabModifiedUi.applyToLabel(
                    label,
                    labelFor(file),
                    modified,
                    openElsewhere,
                    hasErrors
            );
            label.setBackground(highlighted
                    ? ComponentSubtabUi.highlightBackground(file)
                    : UIUtil.getPanelBackground());
            return;
        }
    }

    private @NotNull SubtabGroupPopupPresentation.Context context() {
        Object value = getClientProperty(CONTEXT_KEY);
        return value instanceof SubtabGroupPopupPresentation.Context context
                ? context
                : presentationContext;
    }

    private @NotNull JComponent createItem(
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
        label.getAccessibleContext().setAccessibleName(plainLabel + " öffnen: " + file.getName());
        applyPresentationState(project, label, file, context());
        applyItemWidth(label, fixedWidth);

        Color hoverBackground = JBUI.CurrentTheme.TabbedPane.HOVER_COLOR;

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                label.setBackground(hoverBackground);
                if (SubtabHoverView.isEnabled()) {
                    ComponentSubtabBarHover.onEnter(project, file, label);
                    if (!SubtabGroupPopupPresentation.isOpenElsewhere(project, context(), file)) {
                        ComponentSubtabMainTabHover.onEnter(project, file, label);
                    }
                    if (mainTabProjectViewHover != null) {
                        ComponentSubtabProjectViewHover.onEnter(mainTabProjectViewHover.project(), file, label);
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                applyPresentationState(project, label, file, context());
                ComponentSubtabBarHover.onExit(label);
                ComponentSubtabMainTabHover.onExit(label);
                if (mainTabProjectViewHover != null) {
                    ComponentSubtabProjectViewHover.onExit(label);
                    if (isPointerOver(mainTabProjectViewHover.tabLabel())) {
                        mainTabProjectViewHover.restoreTabGroupHover().run();
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event)) {
                    onSelect.accept(file);
                }
            }
        });

        label.addMouseListener(new PopupHandler() {
            @Override
            public void invokePopup(@NotNull Component component, int x, int y) {
                VirtualFile primaryHighlight = context().primaryHighlight();
                VirtualFile anchor = primaryHighlight != null ? primaryHighlight : file;
                SubtabGroupPopupPresentation.Context menuContext = context();
                boolean revealOnly = SubtabGroupPopupPresentation.isHighlighted(menuContext, file)
                        || SubtabGroupPopupPresentation.isOpenElsewhere(project, menuContext, file);
                ComponentSubtabBarPopup.showContextMenu(
                        project, anchor, file, component, x, y, revealOnly);
            }
        });

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

    private static void applyPresentationState(
            @NotNull Project project,
            @NotNull ComponentSubtabModifiedLabel label,
            @NotNull VirtualFile file,
            @NotNull SubtabGroupPopupPresentation.Context context
    ) {
        boolean highlighted = SubtabGroupPopupPresentation.isHighlighted(context, file);
        boolean openElsewhere = SubtabGroupPopupPresentation.isOpenElsewhere(project, context, file);
        ComponentSubtabFilePresentation presentation = ComponentSubtabFilePresentation.compute(project, file);
        String plainLabel = labelFor(file);
        ComponentSubtabModifiedUi.applyToLabel(
                label,
                plainLabel,
                presentation.modified(),
                openElsewhere,
                presentation.hasErrors()
        );
        label.setBackground(highlighted
                ? ComponentSubtabUi.highlightBackground(file)
                : UIUtil.getPanelBackground());
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

    private static boolean isPointerOver(@NotNull java.awt.Component component) {
        if (!component.isShowing()) {
            return false;
        }
        try {
            Point origin = component.getLocationOnScreen();
            Point pointer = java.awt.MouseInfo.getPointerInfo().getLocation();
            return new java.awt.Rectangle(origin, component.getSize()).contains(pointer);
        } catch (java.awt.IllegalComponentStateException ignored) {
            return false;
        }
    }
}
