package de.sasbe.subtabs;

import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

final class SplittabSavedPairsPopupPanel extends JPanel {
    SplittabSavedPairsPopupPanel(
            @NotNull Project project,
            @NotNull List<ComponentSubtabEditorSplitRegistry.SplittabPair> pairs,
            int fixedWidth,
            @NotNull Consumer<String> onSelectPairId
    ) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(JBUI.Borders.empty(2));
        setBackground(UIUtil.getPanelBackground());

        int index = 0;
        for (ComponentSubtabEditorSplitRegistry.SplittabPair pair : pairs) {
            add(createItem(project, pair, index, onSelectPairId));
            index++;
        }

        if (fixedWidth > 0) {
            Dimension preferred = getPreferredSize();
            setPreferredSize(new Dimension(fixedWidth, preferred.height));
            setMinimumSize(new Dimension(fixedWidth, preferred.height));
            setMaximumSize(new Dimension(fixedWidth, preferred.height));
        }
    }

    private static @NotNull JLabel createItem(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair,
            int index,
            @NotNull Consumer<String> onSelectPairId
    ) {
        String linkText = ComponentSubtabEditorSplitPresentation.linkBarText(pair, index);
        String labelTooltip = ComponentSubtabEditorSplitPresentation.paneHeaderText(pair);
        JLabel label = new JLabel(linkText);
        label.setBorder(JBUI.Borders.empty(4, 8));
        label.setOpaque(true);
        label.setBackground(UIUtil.getPanelBackground());
        label.setForeground(UIUtil.getLabelForeground());
        label.setFont(label.getFont().deriveFont(Font.PLAIN));
        label.setToolTipText(labelTooltip);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                label.setBackground(UIUtil.getListSelectionBackground(true));
                label.setForeground(UIUtil.getListSelectionForeground(true));
            }

            @Override
            public void mouseExited(MouseEvent event) {
                label.setBackground(UIUtil.getPanelBackground());
                label.setForeground(UIUtil.getLabelForeground());
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                onSelectPairId.accept(pair.id());
            }
        });
        return label;
    }
}
