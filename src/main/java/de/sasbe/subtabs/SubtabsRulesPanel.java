package de.sasbe.subtabs;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.TableUtil;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.ui.JBColor;
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
import javax.swing.table.JTableHeader;
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

final class SubtabsRulesPanel {
    private static final int EDIT_COLUMN = 8;
    private static final int DRAG_COLUMN = 9;
    private static final JBColor BUILTIN_RULE_BACKGROUND =
            new JBColor(new Color(0xFFF3CD), new Color(0x4A422E));
    private static final JBColor SHADOWED_RULE_BACKGROUND =
            new JBColor(new Color(0xF2F2F2), new Color(0x3A3A3A));

    private final List<CustomSubtabRule> rules = new ArrayList<>();
    private final TextCellRenderer textCellRenderer = new TextCellRenderer();
    private final BooleanCellRenderer booleanCellRenderer = new BooleanCellRenderer();
    private final DragHandleCellRenderer dragHandleCellRenderer = new DragHandleCellRenderer();
    private final EditHandleCellRenderer editHandleCellRenderer = new EditHandleCellRenderer();
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
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getColumnModel().getColumn(0).setPreferredWidth(JBUI.scale(36));
        table.getColumnModel().getColumn(1).setPreferredWidth(JBUI.scale(72));
        table.getColumnModel().getColumn(2).setPreferredWidth(JBUI.scale(56));
        table.getColumnModel().getColumn(3).setPreferredWidth(JBUI.scale(120));
        table.getColumnModel().getColumn(4).setPreferredWidth(JBUI.scale(40));
        table.getColumnModel().getColumn(5).setPreferredWidth(JBUI.scale(48));
        table.getColumnModel().getColumn(6).setPreferredWidth(JBUI.scale(56));
        table.getColumnModel().getColumn(7).setPreferredWidth(JBUI.scale(88));

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(true);

        table.getColumnModel().getColumn(0).setCellRenderer(booleanCellRenderer);
        table.getColumnModel().getColumn(0).setCellEditor(new BooleanCellEditor());

        for (int column = 1; column <= 7; column++) {
            table.getColumnModel().getColumn(column).setCellRenderer(textCellRenderer);
        }

        table.getColumnModel().getColumn(6).setCellEditor(
                new DefaultCellEditor(new ComboBox<>(new String[]{"Ordner", "Nachbarn"}))
        );

        var dragColumn = table.getColumnModel().getColumn(DRAG_COLUMN);
        dragColumn.setPreferredWidth(JBUI.scale(28));
        dragColumn.setMaxWidth(JBUI.scale(28));
        dragColumn.setMinWidth(JBUI.scale(28));
        dragColumn.setCellRenderer(dragHandleCellRenderer);

        var editColumn = table.getColumnModel().getColumn(EDIT_COLUMN);
        editColumn.setPreferredWidth(JBUI.scale(28));
        editColumn.setMaxWidth(JBUI.scale(28));
        editColumn.setMinWidth(JBUI.scale(28));
        editColumn.setCellRenderer(editHandleCellRenderer);

        dragSupport = new RulesTableDragSupport(table, DRAG_COLUMN, this::dropSelectedRulesAt);
        installPersistentRowSelection(table);
        installRuleEditorOnEditClick(table);

        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(table)
                .setToolbarPosition(ActionToolbarPosition.TOP)
                .setAddAction(button -> addRule())
                .setRemoveAction(button -> removeSelectedRules())
                .setRemoveActionUpdater(button -> canRemoveSelectedRules())
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

    void reset(@NotNull List<CustomSubtabRule> source) {
        rules.clear();
        for (CustomSubtabRule rule : source) {
            rules.add(rule.copy());
        }
        tableModel.fireTableDataChanged();
    }

    @NotNull List<CustomSubtabRule> getRules() {
        List<CustomSubtabRule> copy = new ArrayList<>(rules.size());
        for (CustomSubtabRule rule : rules) {
            copy.add(rule.copy());
        }
        return copy;
    }

    boolean isSameAs(@NotNull List<CustomSubtabRule> other) {
        if (rules.size() != other.size()) {
            return false;
        }
        for (int index = 0; index < rules.size(); index++) {
            CustomSubtabRule left = rules.get(index);
            CustomSubtabRule right = other.get(index);
            if (!left.name.equals(right.name)
                    || left.type != right.type
                    || left.isSpecial() != right.isSpecial()
                    || !left.patterns.equals(right.patterns)
                    || !left.nameSegments.equals(right.nameSegments)
                    || !left.groupNameSegments.equals(right.groupNameSegments)
                    || !left.slotKeys.equals(right.slotKeys)
                    || !left.groupSuffix.equals(right.groupSuffix)
                    || !left.excludePatterns.equals(right.excludePatterns)
                    || left.searchNeighbors != right.searchNeighbors
                    || left.enabled != right.enabled
                    || left.builtin != right.builtin) {
                return false;
            }
        }
        return true;
    }

    private void addRule() {
        TableUtil.stopEditing(table);
        int insertIndex = firstSpecialRuleIndex();
        rules.add(insertIndex, new CustomSubtabRule());
        tableModel.fireTableRowsInserted(insertIndex, insertIndex);
        table.setRowSelectionInterval(insertIndex, insertIndex);
    }

    private int firstSpecialRuleIndex() {
        for (int index = 0; index < rules.size(); index++) {
            if (rules.get(index).isSpecial()) {
                return index;
            }
        }
        return rules.size();
    }

    private boolean canRemoveSelectedRules() {
        int[] selected = table.getSelectedRows();
        if (selected.length == 0) {
            return false;
        }
        for (int row : selected) {
            if (rules.get(row).builtin) {
                return false;
            }
        }
        return true;
    }

    private void removeSelectedRules() {
        if (!canRemoveSelectedRules()) {
            return;
        }

        int[] selected = table.getSelectedRows();
        TableUtil.stopEditing(table);
        Arrays.sort(selected);
        for (int index = selected.length - 1; index >= 0; index--) {
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
        if (row >= 0 && row < rules.size() && rules.get(row).builtin) {
            component.setBackground(BUILTIN_RULE_BACKGROUND);
            component.setForeground(UIUtil.getLabelForeground());
            return;
        }
        if (row >= 0 && row < rules.size() && isShadowed(row)) {
            component.setBackground(SHADOWED_RULE_BACKGROUND);
            component.setForeground(UIUtil.getInactiveTextColor());
            return;
        }
        component.setBackground(table.getBackground());
        component.setForeground(UIUtil.getLabelForeground());
    }

    private boolean isShadowed(int row) {
        return SubtabRulePrecedence.isShadowed(rules, row);
    }

    private void installRuleEditorOnEditClick(@NotNull JBTable table) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() != 1 || event.getButton() != MouseEvent.BUTTON1) {
                    return;
                }
                int row = table.rowAtPoint(event.getPoint());
                int column = table.columnAtPoint(event.getPoint());
                if (row < 0 || row >= rules.size() || column != EDIT_COLUMN) {
                    return;
                }
                openRuleEditor(row);
            }
        });
    }

    private void openRuleEditor(int rowIndex) {
        TableUtil.stopEditing(table);
        SubtabRuleEditDialog dialog = new SubtabRuleEditDialog(table, rules.get(rowIndex));
        if (!dialog.showAndGet()) {
            return;
        }
        rules.set(rowIndex, dialog.rule());
        tableModel.fireTableRowsUpdated(rowIndex, rowIndex);
        tableModel.fireTableDataChanged();
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

    private final class EditHandleCellRenderer extends DefaultTableCellRenderer {
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
            label.setIcon(AllIcons.Actions.Edit);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setToolTipText("Regel bearbeiten");
            applyRowAppearance(table, label, row);
            return label;
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
            return 10;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case 0 -> "An";
                case 1 -> "Name";
                case 2 -> "Suffix";
                case 3 -> "Muster";
                case 4 -> "Subtab Label";
                case 5 -> "Name Rule";
                case 6 -> "Suche";
                case 7 -> "Ausnahme";
                case 8 -> "";
                case 9 -> "";
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            CustomSubtabRule rule = rules.get(rowIndex);
            if (columnIndex == 0) {
                return true;
            }
            if (rule.isSpecial()) {
                return false;
            }
            return columnIndex > 0 && columnIndex < 8;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            CustomSubtabRule rule = rules.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> rule.enabled;
                case 1 -> rule.name;
                case 2 -> rule.groupSuffix;
                case 3 -> rule.patterns;
                case 4 -> rule.nameSegments;
                case 5 -> rule.isSpecial() ? "" : rule.groupNameSegments;
                case 6 -> rule.isSpecial() ? "" : (rule.searchNeighbors ? "Nachbarn" : "Ordner");
                case 7 -> rule.isSpecial() ? "" : rule.excludePatterns;
                case 8 -> "";
                case 9 -> "";
                default -> "";
            };
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            CustomSubtabRule rule = rules.get(rowIndex);
            switch (columnIndex) {
                case 0 -> rule.enabled = Boolean.TRUE.equals(value);
                case 1 -> rule.name = String.valueOf(value).trim();
                case 2 -> rule.groupSuffix = String.valueOf(value).trim();
                case 3 -> rule.patterns = String.valueOf(value).trim();
                case 4 -> rule.nameSegments = String.valueOf(value).trim();
                case 5 -> rule.groupNameSegments = String.valueOf(value).trim();
                case 6 -> rule.searchNeighbors = "Nachbarn".equals(String.valueOf(value));
                case 7 -> rule.excludePatterns = String.valueOf(value).trim();
                default -> {
                }
            }
            fireTableCellUpdated(rowIndex, columnIndex);
            if (columnIndex == 0 || columnIndex == 3 || columnIndex == 5 || columnIndex == 6 || columnIndex == 7) {
                fireTableDataChanged();
            }
        }
    }
}
