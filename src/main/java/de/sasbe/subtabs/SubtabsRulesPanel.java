package de.sasbe.subtabs;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.TableUtil;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class SubtabsRulesPanel {
    private static final int DRAG_COLUMN = 6;

    private final List<CustomSubtabRule> rules = new ArrayList<>();
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
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(3).setPreferredWidth(220);
        table.getColumnModel().getColumn(4).setPreferredWidth(70);
        table.getColumnModel().getColumn(5).setPreferredWidth(80);

        table.getColumnModel().getColumn(0).setCellRenderer(new BooleanCellRenderer());
        table.getColumnModel().getColumn(0).setCellEditor(new BooleanCellEditor());

        table.getColumnModel().getColumn(4).setCellEditor(
                new DefaultCellEditor(new ComboBox<>(new String[]{"Stamm", "Dateien"}))
        );
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer());
        table.getColumnModel().getColumn(5).setCellEditor(
                new DefaultCellEditor(new ComboBox<>(new String[]{"Ordner", "Nachbarn"}))
        );
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer());

        var dragColumn = table.getColumnModel().getColumn(DRAG_COLUMN);
        dragColumn.setPreferredWidth(JBUI.scale(28));
        dragColumn.setMaxWidth(JBUI.scale(28));
        dragColumn.setMinWidth(JBUI.scale(28));
        dragColumn.setCellRenderer(new DragHandleCellRenderer());

        dragSupport = new RulesTableDragSupport(table, DRAG_COLUMN, this::dropSelectedRulesAt);

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
                    || !left.patterns.equals(right.patterns)
                    || !left.labels.equals(right.labels)
                    || !left.slotKeys.equals(right.slotKeys)
                    || !left.groupSuffix.equals(right.groupSuffix)
                    || left.searchNeighbors != right.searchNeighbors
                    || left.stripComponentSuffix != right.stripComponentSuffix
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

    private static @NotNull String typeLabel(@NotNull CustomSubtabRule rule) {
        return switch (rule.type) {
            case STEM -> "Stamm";
            case FILES -> "Dateien";
            case CUSTOM_GROUPS -> "Eigene Gruppen";
            case FOLDER -> "Ordner";
        };
    }

    private static final class BooleanCellRenderer extends JCheckBox implements TableCellRenderer {
        BooleanCellRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorderPaintedFlat(true);
        }

        @Override
        public Component getTableCellRendererComponent(
                javax.swing.JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            setSelected(Boolean.TRUE.equals(value));
            if (isSelected) {
                setBackground(table.getSelectionBackground());
            } else {
                setBackground(table.getBackground());
            }
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

    private static final class DragHandleCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                javax.swing.JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column
            );
            label.setText("");
            label.setIcon(AllIcons.General.Drag);
            label.setHorizontalAlignment(SwingConstants.CENTER);
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
            return 7;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case 0 -> "Aktiv";
                case 1 -> "Name";
                case 2 -> "Zusatz";
                case 3 -> "Dateien";
                case 4 -> "Art";
                case 5 -> "Suche";
                case 6 -> "";
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
            return columnIndex < 6;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            CustomSubtabRule rule = rules.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> rule.enabled;
                case 1 -> rule.name;
                case 2 -> rule.groupSuffix;
                case 3 -> rule.patterns;
                case 4 -> typeLabel(rule);
                case 5 -> rule.searchNeighbors ? "Nachbarn" : "Ordner";
                case 6 -> "";
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
                case 4 -> rule.type = "Dateien".equals(String.valueOf(value))
                        ? CustomSubtabRule.Type.FILES
                        : CustomSubtabRule.Type.STEM;
                case 5 -> rule.searchNeighbors = "Nachbarn".equals(String.valueOf(value));
                default -> {
                }
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }
}
