package de.sasbe.subtabs;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.ui.TableUtil;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class SidetabsRulesPanel {
    private static final int SECTIONS_COLUMN = 3;
    private static final int DRAG_COLUMN = 4;
    private static final JBColor PROTECTED_RULE_BACKGROUND =
            new JBColor(new Color(0xFFF3CD), new Color(0x4A422E));

    private final List<CustomSidetabRule> rules = new ArrayList<>();
    private final TextCellRenderer textCellRenderer = new TextCellRenderer();
    private final BooleanCellRenderer booleanCellRenderer = new BooleanCellRenderer();
    private final SectionsCellRenderer sectionsCellRenderer = new SectionsCellRenderer();
    private final DragHandleCellRenderer dragHandleCellRenderer = new DragHandleCellRenderer();
    private final RulesTableModel tableModel = new RulesTableModel();
    private RulesTableDragSupport dragSupport;
    private final JBTable table = new JBTable(tableModel) {
        @Override
        protected void processMouseEvent(java.awt.event.MouseEvent event) {
            if (dragSupport != null && dragSupport.processMouseEvent(event)) {
                return;
            }
            super.processMouseEvent(event);
        }

        @Override
        protected void processMouseMotionEvent(java.awt.event.MouseEvent event) {
            if (dragSupport != null && dragSupport.processMouseMotionEvent(event)) {
                return;
            }
            super.processMouseMotionEvent(event);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (dragSupport != null) {
                dragSupport.paintDropIndicator(graphics);
            }
        }
    };

    @NotNull JComponent createPanel() {
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setShowGrid(false);
        table.setRowHeight(JBUI.scale(22));
        table.getColumnModel().getColumn(0).setPreferredWidth(48);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(220);
        table.getColumnModel().getColumn(SECTIONS_COLUMN).setPreferredWidth(260);
        table.getColumnModel().getColumn(0).setCellRenderer(booleanCellRenderer);
        table.getColumnModel().getColumn(0).setCellEditor(new BooleanCellEditor());
        table.getColumnModel().getColumn(1).setCellRenderer(textCellRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(textCellRenderer);
        table.getColumnModel().getColumn(SECTIONS_COLUMN).setCellRenderer(sectionsCellRenderer);

        var dragColumn = table.getColumnModel().getColumn(DRAG_COLUMN);
        dragColumn.setPreferredWidth(JBUI.scale(28));
        dragColumn.setMaxWidth(JBUI.scale(28));
        dragColumn.setMinWidth(JBUI.scale(28));
        dragColumn.setCellRenderer(dragHandleCellRenderer);

        dragSupport = new RulesTableDragSupport(table, DRAG_COLUMN, this::dropSelectedRulesAt);
        installPersistentRowSelection(table);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getButton() != MouseEvent.BUTTON1) {
                    return;
                }
                int row = table.rowAtPoint(event.getPoint());
                int column = table.columnAtPoint(event.getPoint());
                if (row >= 0 && column == SECTIONS_COLUMN) {
                    openSectionsDialog(row);
                }
            }
        });

        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(table)
                .setToolbarPosition(ActionToolbarPosition.TOP)
                .setAddAction(button -> addRule())
                .setRemoveAction(button -> removeSelectedRules())
                .setRemoveActionUpdater(button -> table.getSelectedRowCount() > 0)
                .setMoveUpAction(button -> moveSelectedRules(-1))
                .setMoveDownAction(button -> moveSelectedRules(1))
                .setMoveUpActionUpdater(button -> RulesTableDragSupport.canMoveSelectedBlock(
                        table.getSelectedRows(), rules.size(), -1
                ))
                .setMoveDownActionUpdater(button -> RulesTableDragSupport.canMoveSelectedBlock(
                        table.getSelectedRows(), rules.size(), 1
                ));

        return decorator.createPanel();
    }

    void reset(@NotNull List<CustomSidetabRule> source) {
        rules.clear();
        for (CustomSidetabRule rule : source) {
            rules.add(rule.copy());
        }
        tableModel.fireTableDataChanged();
    }

    @NotNull List<CustomSidetabRule> getRules() {
        List<CustomSidetabRule> copy = new ArrayList<>(rules.size());
        for (CustomSidetabRule rule : rules) {
            copy.add(rule.copy());
        }
        return copy;
    }

    boolean isSameAs(@NotNull List<CustomSidetabRule> other) {
        if (rules.size() != other.size()) {
            return false;
        }
        for (int index = 0; index < rules.size(); index++) {
            if (!rules.get(index).sameAs(other.get(index))) {
                return false;
            }
        }
        return true;
    }

    private void openSectionsDialog(int row) {
        TableUtil.stopEditing(table);
        CustomSidetabRule rule = rules.get(row);
        if (rule.isTopRule()) {
            rule.familyMode = rule.familyMode == TopCommentMode.COMBINE
                    ? TopCommentMode.OVERRIDE
                    : TopCommentMode.COMBINE;
            tableModel.fireTableRowsUpdated(row, row);
            return;
        }
        SidetabSectionsDialog dialog = new SidetabSectionsDialog(table, rule.copySpecs(), rule.respectOrder);
        if (dialog.showAndGet()) {
            rule.sectionSpecs = dialog.specs();
            rule.respectOrder = dialog.respectOrder();
            tableModel.fireTableRowsUpdated(row, row);
        }
    }

    private void addRule() {
        TableUtil.stopEditing(table);
        CustomSidetabRule rule = new CustomSidetabRule();
        rule.enabled = true;
        rule.sectionSpecs.add(new SidetabSectionSpec("", "@start"));
        rules.add(rule);
        int insertIndex = rules.size() - 1;
        tableModel.fireTableRowsInserted(insertIndex, insertIndex);
        table.setRowSelectionInterval(insertIndex, insertIndex);
        openSectionsDialog(insertIndex);
    }

    private void removeSelectedRules() {
        int[] selected = table.getSelectedRows();
        if (selected.length == 0) {
            return;
        }

        TableUtil.stopEditing(table);
        Arrays.sort(selected);
        for (int index = selected.length - 1; index >= 0; index--) {
            if (rules.get(selected[index]).isTopRule()) {
                continue;
            }
            rules.remove(selected[index]);
        }
        tableModel.fireTableDataChanged();

        if (!rules.isEmpty()) {
            int nextSelection = Math.min(selected[0], rules.size() - 1);
            table.setRowSelectionInterval(nextSelection, nextSelection);
        }
    }

    private void moveSelectedRules(int direction) {
        int[] selected = table.getSelectedRows();
        if (!RulesTableDragSupport.canMoveSelectedBlock(selected, rules.size(), direction)) {
            return;
        }

        TableUtil.stopEditing(table);
        int firstSelected = Arrays.stream(selected).min().orElse(-1);
        int count = selected.length;

        if (RulesTableDragSupport.isContiguous(selected)) {
            RulesTableDragSupport.moveSelectedBlock(rules, selected, direction);
            tableModel.fireTableDataChanged();
            RulesTableDragSupport.restoreSelection(
                    table,
                    direction < 0 ? firstSelected - 1 : firstSelected + 1,
                    count
            );
            return;
        }

        int[] sorted = Arrays.copyOf(selected, selected.length);
        Arrays.sort(sorted);
        if (direction > 0) {
            for (int index = sorted.length - 1; index >= 0; index--) {
                RulesTableDragSupport.moveSelectedBlock(rules, new int[]{sorted[index]}, direction);
            }
        } else {
            for (int row : sorted) {
                RulesTableDragSupport.moveSelectedBlock(rules, new int[]{row}, direction);
            }
        }
        tableModel.fireTableDataChanged();
        RulesTableDragSupport.restoreSelection(table, direction < 0 ? firstSelected - 1 : firstSelected + 1, count);
    }

    private void dropSelectedRulesAt(int targetRow) {
        int[] selected = table.getSelectedRows();
        if (selected.length == 0) {
            return;
        }

        TableUtil.stopEditing(table);
        int count = selected.length;
        RulesTableDragSupport.moveRowsTo(rules, selected, targetRow);
        tableModel.fireTableDataChanged();

        int[] sorted = Arrays.copyOf(selected, selected.length);
        Arrays.sort(sorted);
        int adjustedTarget = targetRow;
        for (int row : sorted) {
            if (row < targetRow) {
                adjustedTarget--;
            }
        }
        adjustedTarget = Math.max(0, Math.min(adjustedTarget, rules.size() - count));
        RulesTableDragSupport.restoreSelection(table, adjustedTarget, count);
    }

    private void applyRowAppearance(
            @NotNull JTable table,
            @NotNull JComponent component,
            int row
    ) {
        component.setOpaque(true);
        if (table.isRowSelected(row)) {
            boolean focused = table.hasFocus();
            component.setBackground(UIUtil.getTreeSelectionBackground(focused));
            component.setForeground(UIUtil.getTreeSelectionForeground(focused));
            return;
        }
        if (row >= 0 && row < rules.size() && rules.get(row).isTopRule()) {
            component.setBackground(PROTECTED_RULE_BACKGROUND);
            component.setForeground(UIUtil.getLabelForeground());
            return;
        }
        component.setBackground(table.getBackground());
        component.setForeground(UIUtil.getLabelForeground());
    }

    private static void installPersistentRowSelection(@NotNull JBTable table) {
        Runnable repaintSelection = table::repaint;
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                repaintSelection.run();
            }
        });
        table.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                repaintSelection.run();
            }

            @Override
            public void focusLost(FocusEvent event) {
                repaintSelection.run();
            }
        });
    }

    private final class TextCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, table.isRowSelected(row), hasFocus, row, column
            );
            applyRowAppearance(table, label, row);
            return label;
        }
    }

    private final class SectionsCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, table.isRowSelected(row), hasFocus, row, column
            );
            applyRowAppearance(table, label, row);
            label.setIcon(AllIcons.Actions.Edit);
            label.setHorizontalTextPosition(SwingConstants.LEFT);
            label.setToolTipText(row < rules.size() && rules.get(row).isTopRule()
                    ? "Klicken zum Umschalten: Überschreiben / Kombinieren"
                    : "Abschnitte und Startlogik bearbeiten");
            return label;
        }
    }

    private final class BooleanCellRenderer extends JCheckBox implements TableCellRenderer {
        BooleanCellRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorderPaintedFlat(true);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            setSelected(Boolean.TRUE.equals(value));
            applyRowAppearance(table, this, row);
            return this;
        }
    }

    private static final class BooleanCellEditor extends DefaultCellEditor {
        BooleanCellEditor() {
            super(new JCheckBox());
            JCheckBox checkBox = (JCheckBox) getComponent();
            checkBox.setHorizontalAlignment(SwingConstants.CENTER);
            checkBox.setBorderPaintedFlat(true);
        }
    }

    private final class DragHandleCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, table.isRowSelected(row), hasFocus, row, column
            );
            label.setText("");
            label.setIcon(AllIcons.General.Drag);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            applyRowAppearance(table, label, row);
            return label;
        }
    }

    private final class RulesTableModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return rules.size();
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case 0 -> "Aktiv";
                case 1 -> "Name";
                case 2 -> "Dateien";
                case 3 -> "Abschnitte";
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            CustomSidetabRule rule = rules.get(rowIndex);
            if (rule.isTopRule()) {
                return columnIndex == 0;
            }
            return columnIndex == 0 || columnIndex == 1 || columnIndex == 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            CustomSidetabRule rule = rules.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> rule.enabled;
                case 1 -> rule.name;
                case 2 -> rule.filePatterns;
                case 3 -> rule.sectionsSummary();
                default -> "";
            };
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            CustomSidetabRule rule = rules.get(rowIndex);
            switch (columnIndex) {
                case 0 -> rule.enabled = Boolean.TRUE.equals(value);
                case 1 -> rule.name = String.valueOf(value).trim();
                case 2 -> rule.filePatterns = String.valueOf(value).trim();
                default -> {
                }
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }
}
