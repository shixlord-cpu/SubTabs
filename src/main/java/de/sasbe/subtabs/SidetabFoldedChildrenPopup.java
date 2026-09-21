package de.sasbe.subtabs;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.IllegalComponentStateException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

final class SidetabFoldedChildrenPopup {
    private static final String POPUP_KEY = "componentSubtabs.sidetabFoldedChildrenPopup";
    private static final String HIDE_TIMER_KEY = "componentSubtabs.sidetabFoldedChildrenHideTimer";
    private static final String SECTION_INDEX_KEY = "componentSubtabs.sidetabFoldedChildIndex";
    private static final int HIDE_DELAY_MS = 180;

    private SidetabFoldedChildrenPopup() {
    }

    static void show(
            @NotNull Project project,
            @NotNull Editor editor,
            @NotNull VirtualFile file,
            @NotNull JComponent anchor,
            @NotNull List<SidetabSection> sections,
            @NotNull List<Integer> childIndices,
            @NotNull IntConsumer onSelect
    ) {
        cancelHideTimer(anchor);
        JBPopup existing = getPopup(anchor);
        if (existing != null && existing.isVisible()) {
            return;
        }

        Document document = editor.getDocument();
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(JBUI.Borders.empty(4, 6));
        panel.setOpaque(true);
        panel.setBackground(UIUtil.getPanelBackground());
        Color hoverBackground = JBUI.CurrentTheme.TabbedPane.HOVER_COLOR;

        for (int childIndex : childIndices) {
            if (childIndex < 0 || childIndex >= sections.size()) {
                continue;
            }
            SidetabSection section = sections.get(childIndex);
            ComponentSubtabModifiedLabel row = new ComponentSubtabModifiedLabel();
            row.putClientProperty(SECTION_INDEX_KEY, childIndex);
            row.setBorder(JBUI.Borders.empty(3, 6));
            row.setOpaque(true);
            row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            int indent = SidetabBarPanel.subDepthDotOffset(Math.max(1, section.depth()));
            if (indent > 0) {
                row.setBorder(JBUI.Borders.emptyLeft(3 + indent));
            }
            applyRowPresentation(project, document, file, section, row, false);
            row.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(@NotNull MouseEvent event) {
                    hide(anchor);
                    onSelect.accept(childIndex);
                }

                @Override
                public void mouseEntered(@NotNull MouseEvent event) {
                    cancelHideTimer(anchor);
                    row.setBackground(hoverBackground);
                    SidetabEditorHover.show(editor, section);
                }

                @Override
                public void mouseExited(@NotNull MouseEvent event) {
                    applyRowPresentation(project, document, file, section, row, false);
                    if (!isPointerOverPopupOrAnchor(anchor)) {
                        SidetabEditorHover.hide(editor);
                    }
                }
            });
            panel.add(row);
        }

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, null)
                .setRequestFocus(false)
                .setCancelOnClickOutside(true)
                .setCancelOnWindowDeactivation(true)
                .createPopup();
        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(@NotNull LightweightWindowEvent event) {
                anchor.putClientProperty(POPUP_KEY, null);
                SidetabEditorHover.hide(editor);
            }
        });
        anchor.putClientProperty(POPUP_KEY, popup);
        popup.show(new RelativePoint(anchor, popupPoint(anchor)));
    }

    static void refreshPresentation(
            @NotNull JComponent anchor,
            @NotNull Project project,
            @NotNull Editor editor,
            @NotNull Document document,
            @NotNull VirtualFile file,
            @NotNull List<SidetabSection> sections
    ) {
        JBPopup popup = getPopup(anchor);
        if (popup == null || !popup.isVisible()) {
            return;
        }
        Component content = popup.getContent();
        if (!(content instanceof JPanel panel)) {
            return;
        }
        for (Component component : panel.getComponents()) {
            if (!(component instanceof ComponentSubtabModifiedLabel row)) {
                continue;
            }
            Object indexValue = row.getClientProperty(SECTION_INDEX_KEY);
            if (!(indexValue instanceof Integer index) || index < 0 || index >= sections.size()) {
                continue;
            }
            boolean hovered = isPointerOver(row, MouseInfo.getPointerInfo().getLocation());
            applyRowPresentation(project, document, file, sections.get(index), row, hovered);
        }
    }

    static void refreshForDocument(@NotNull Project project, @NotNull Document document) {
        com.intellij.openapi.fileEditor.FileDocumentManager manager =
                com.intellij.openapi.fileEditor.FileDocumentManager.getInstance();
        VirtualFile file = manager.getFile(document);
        if (file == null) {
            return;
        }
        com.intellij.openapi.fileEditor.FileEditorManager editorManager =
                com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project);
        for (com.intellij.openapi.fileEditor.FileEditor fileEditor
                : ComponentSubtabsManager.editorsFor(editorManager, file)) {
            SidetabBarPanel panel = fileEditor.getUserData(SidetabsManager.SIDETAB_BAR_KEY);
            if (panel != null) {
                panel.refreshVisibleFoldedChildrenPopup();
            }
        }
    }

    static void scheduleHide(@NotNull JComponent anchor) {
        cancelHideTimer(anchor);
        Timer timer = new Timer(HIDE_DELAY_MS, event -> {
            if (isPointerOverPopupOrAnchor(anchor)) {
                return;
            }
            hide(anchor);
        });
        timer.setRepeats(false);
        anchor.putClientProperty(HIDE_TIMER_KEY, timer);
        timer.start();
    }

    static void hide(@NotNull JComponent anchor) {
        cancelHideTimer(anchor);
        JBPopup popup = getPopup(anchor);
        if (popup != null) {
            popup.cancel();
        }
        anchor.putClientProperty(POPUP_KEY, null);
    }

    private static void applyRowPresentation(
            @NotNull Project project,
            @NotNull Document document,
            @NotNull VirtualFile file,
            @NotNull SidetabSection section,
            @NotNull ComponentSubtabModifiedLabel row,
            boolean hovered
    ) {
        SidetabSectionPresentation presentation = SidetabSectionPresentation.compute(
                project,
                document,
                file,
                section
        );
        ComponentSubtabModifiedUi.applyToLabel(
                row,
                section.name(),
                presentation.modified(),
                false,
                presentation.hasErrors()
        );
        row.setBackground(hovered
                ? JBUI.CurrentTheme.TabbedPane.HOVER_COLOR
                : UIUtil.getPanelBackground());
    }

    private static @NotNull Point popupPoint(@NotNull JComponent anchor) {
        return new Point(0, anchor.getHeight());
    }

    private static void cancelHideTimer(@NotNull JComponent anchor) {
        Object value = anchor.getClientProperty(HIDE_TIMER_KEY);
        if (value instanceof Timer timer) {
            timer.stop();
        }
        anchor.putClientProperty(HIDE_TIMER_KEY, null);
    }

    private static boolean isPointerOverPopupOrAnchor(@NotNull JComponent anchor) {
        Point pointer = MouseInfo.getPointerInfo().getLocation();
        if (isPointerOver(anchor, pointer)) {
            return true;
        }
        JBPopup popup = getPopup(anchor);
        if (popup == null || !popup.isVisible()) {
            return false;
        }
        Component content = popup.getContent();
        return content != null && isPointerOver(content, pointer);
    }

    private static boolean isPointerOver(@NotNull Component component, @NotNull Point pointerOnScreen) {
        if (!component.isShowing()) {
            return false;
        }
        try {
            Point origin = component.getLocationOnScreen();
            return new Rectangle(origin, component.getSize()).contains(pointerOnScreen);
        } catch (IllegalComponentStateException ignored) {
            return false;
        }
    }

    private static @Nullable JBPopup getPopup(@NotNull JComponent anchor) {
        Object value = anchor.getClientProperty(POPUP_KEY);
        return value instanceof JBPopup popup ? popup : null;
    }
}
