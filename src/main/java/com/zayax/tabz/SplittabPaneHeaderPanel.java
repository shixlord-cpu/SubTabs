package com.zayax.tabz;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class SplittabPaneHeaderPanel extends JPanel {
    private static final Color DISSOLVE_ICON_COLOR = new Color(0x3B82F6);
    private static final String SPLITTAB_HEADER_HOVER_INSTALLED = "componentTabz.splittabHeaderHoverSync";

    private final Project project;
    private final JBLabel titleLabel = new JBLabel();
    private final ComponentSubtabIconButton dissolveButton;
    private @NotNull String fullTitle = "";
    private boolean titleVisible = true;
    private int layoutLeftReserve;
    private int layoutRightReserve;

    SplittabPaneHeaderPanel(@NotNull Project project) {
        super(null);
        this.project = project;
        setBorder(JBUI.Borders.empty(4, 8));
        setOpaque(false);

        titleLabel.setFont(JBUI.Fonts.label());
        titleLabel.setHorizontalAlignment(SwingConstants.LEFT);
        titleLabel.setVerticalAlignment(SwingConstants.CENTER);
        add(titleLabel);

        dissolveButton = new ComponentSubtabIconButton(
                IconUtil.colorize(AllIcons.Actions.Close, DISSOLVE_ICON_COLOR)
        );
        dissolveButton.setToolTipText("Dissolve split pair");
        dissolveButton.getAccessibleContext().setAccessibleName("Dissolve split pair");
        add(dissolveButton);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                updateTruncatedTitle();
                layoutHeader();
            }
        });
        addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                updateTruncatedTitle();
                layoutHeader();
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
            }
        });
    }

    @NotNull JBLabel titleLabelForHoverSync() {
        return titleLabel;
    }

    void setTitleVisible(boolean visible) {
        if (titleVisible == visible) {
            return;
        }
        titleVisible = visible;
        titleLabel.setVisible(visible);
        if (!visible) {
            titleLabel.setText("");
            titleLabel.setToolTipText(null);
        } else {
            updateTruncatedTitle();
        }
        revalidate();
        repaint();
    }

    boolean isTitleVisible() {
        return titleVisible;
    }

    void setLayoutReserves(int leftPixels, int rightPixels) {
        int left = Math.max(0, leftPixels);
        int right = Math.max(0, rightPixels);
        if (layoutLeftReserve == left && layoutRightReserve == right) {
            return;
        }
        layoutLeftReserve = left;
        layoutRightReserve = right;
        updateTruncatedTitle();
        revalidate();
        repaint();
    }

    void bind(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        fullTitle = ComponentSubtabEditorSplitPresentation.paneHeaderText(pair);
        titleLabel.putClientProperty(ComponentSubtabBarPopup.SPLITTAB_PAIR_ID_KEY, pair.id());
        installTitleHoverSync();
        if (!Boolean.TRUE.equals(titleLabel.getClientProperty(ComponentSubtabBarPopup.SPLITTAB_HEADER_POPUP_INSTALLED))) {
            titleLabel.putClientProperty(ComponentSubtabBarPopup.SPLITTAB_HEADER_POPUP_INSTALLED, Boolean.TRUE);
            titleLabel.addMouseListener(new PopupHandler() {
                @Override
                public void invokePopup(@NotNull Component comp, int x, int y) {
                    Object pairId = titleLabel.getClientProperty(ComponentSubtabBarPopup.SPLITTAB_PAIR_ID_KEY);
                    if (pairId instanceof String id) {
                        ComponentSubtabBarPopup.showSplittabHeaderContextMenu(project, id, comp, x, y);
                    }
                }
            });
        }
        for (var listener : dissolveButton.getActionListeners()) {
            dissolveButton.removeActionListener(listener);
        }
        dissolveButton.addActionListener(event ->
                ComponentSubtabEditorSplitPresentation.dissolvePair(project, pair.id())
        );
        updateTruncatedTitle();
        revalidate();
        repaint();
    }

    private void installTitleHoverSync() {
        if (Boolean.TRUE.equals(titleLabel.getClientProperty(SPLITTAB_HEADER_HOVER_INSTALLED))) {
            return;
        }
        titleLabel.putClientProperty(SPLITTAB_HEADER_HOVER_INSTALLED, Boolean.TRUE);
        titleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                if (!SubtabHoverView.isEnabled()) {
                    return;
                }
                Object pairId = titleLabel.getClientProperty(ComponentSubtabBarPopup.SPLITTAB_PAIR_ID_KEY);
                if (!(pairId instanceof String id)) {
                    return;
                }
                ComponentSubtabEditorSplitRegistry.SplittabPair pair =
                        ComponentSubtabEditorSplitRegistry.getInstance(project).findById(id);
                if (pair == null || !ComponentSubtabEditorSplitMainTab.isForeground(project, pair)) {
                    return;
                }
                ComponentSubtabProjectViewHover.onEnterSplittabPair(
                        project,
                        pair,
                        pair.rightFile(),
                        titleLabel
                );
            }

            @Override
            public void mouseExited(MouseEvent event) {
                if (SubtabHoverView.isEnabled()) {
                    ComponentSubtabProjectViewHover.onExit(titleLabel);
                }
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(0, headerHeight());
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0, headerHeight());
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, headerHeight());
    }

    private int headerHeight() {
        Insets insets = getInsets();
        int content = Math.max(titleLabel.getPreferredSize().height, dissolveButton.getPreferredSize().height);
        return insets.top + content + insets.bottom;
    }

    @Override
    public void doLayout() {
        layoutHeader();
    }

    private void layoutHeader() {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        Insets insets = getInsets();
        int gap = JBUI.scale(6);
        Dimension closeSize = dissolveButton.getPreferredSize();
        int closeX = width - insets.right - layoutRightReserve - closeSize.width;
        int closeY = insets.top + Math.max(0, (height - insets.top - insets.bottom - closeSize.height) / 2);
        dissolveButton.setBounds(closeX, closeY, closeSize.width, closeSize.height);

        if (titleVisible) {
            int titleX = insets.left + layoutLeftReserve;
            int titleWidth = Math.max(0, closeX - gap - titleX);
            titleLabel.setBounds(titleX, insets.top, titleWidth, height - insets.top - insets.bottom);
            updateTruncatedTitle();
        } else {
            titleLabel.setBounds(0, 0, 0, 0);
        }
    }

    private void updateTruncatedTitle() {
        if (!titleVisible) {
            titleLabel.setText("");
            return;
        }
        if (fullTitle.isEmpty()) {
            titleLabel.setText("");
            return;
        }
        int maxWidth = titleLabel.getWidth();
        if (maxWidth <= 0) {
            titleLabel.setText(fullTitle);
            titleLabel.setToolTipText(fullTitle);
            return;
        }
        FontMetrics metrics = titleLabel.getFontMetrics(titleLabel.getFont());
        String fitted = ComponentSubtabUi.fitText(metrics, fullTitle, maxWidth);
        titleLabel.setText(fitted);
        titleLabel.setToolTipText(fullTitle.equals(fitted) ? null : fullTitle);
    }
}
