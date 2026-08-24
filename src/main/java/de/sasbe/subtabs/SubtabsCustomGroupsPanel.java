package de.sasbe.subtabs;

import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.ui.TableUtil;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class SubtabsCustomGroupsPanel {
    private final List<CustomSubtabGroupDefinition> groups = new ArrayList<>();
    private final GroupsTableModel tableModel = new GroupsTableModel();
    private final JBTable table = new JBTable(tableModel);

    @NotNull JComponent createPanel() {
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setShowGrid(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);

        return ToolbarDecorator.createDecorator(table)
                .setToolbarPosition(ActionToolbarPosition.TOP)
                .setAddAction(button -> addGroup())
                .setRemoveAction(button -> removeSelectedGroups())
                .createPanel();
    }

    void reset(@NotNull List<CustomSubtabGroupDefinition> source) {
        groups.clear();
        for (CustomSubtabGroupDefinition group : source) {
            groups.add(group.copy());
        }
        tableModel.fireTableDataChanged();
    }

    @NotNull List<CustomSubtabGroupDefinition> getGroups() {
        List<CustomSubtabGroupDefinition> copy = new ArrayList<>(groups.size());
        for (CustomSubtabGroupDefinition group : groups) {
            copy.add(group.copy());
        }
        return copy;
    }

    boolean isSameAs(@NotNull List<CustomSubtabGroupDefinition> other) {
        if (groups.size() != other.size()) {
            return false;
        }
        for (int index = 0; index < groups.size(); index++) {
            CustomSubtabGroupDefinition left = groups.get(index);
            CustomSubtabGroupDefinition right = other.get(index);
            if (!left.name.equals(right.name)
                    || !left.patterns.equals(right.patterns)
                    || !left.labels.equals(right.labels)) {
                return false;
            }
        }
        return true;
    }

    private void addGroup() {
        TableUtil.stopEditing(table);
        groups.add(new CustomSubtabGroupDefinition());
        tableModel.fireTableRowsInserted(groups.size() - 1, groups.size() - 1);
        table.setRowSelectionInterval(groups.size() - 1, groups.size() - 1);
    }

    private void removeSelectedGroups() {
        int[] selected = table.getSelectedRows();
        if (selected.length == 0) {
            return;
        }

        TableUtil.stopEditing(table);
        Arrays.sort(selected);
        for (int index = selected.length - 1; index >= 0; index--) {
            groups.remove(selected[index]);
        }
        tableModel.fireTableDataChanged();

        if (!groups.isEmpty()) {
            int nextSelection = Math.min(selected[0], groups.size() - 1);
            table.setRowSelectionInterval(nextSelection, nextSelection);
        }
    }

    private final class GroupsTableModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return groups.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case 0 -> "Name";
                case 1 -> "Dateien";
                case 2 -> "Labels";
                default -> "";
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            CustomSubtabGroupDefinition group = groups.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> group.name;
                case 1 -> group.patterns;
                case 2 -> group.labels;
                default -> "";
            };
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            CustomSubtabGroupDefinition group = groups.get(rowIndex);
            switch (columnIndex) {
                case 0 -> group.name = String.valueOf(value).trim();
                case 1 -> group.patterns = String.valueOf(value).trim();
                case 2 -> group.labels = String.valueOf(value).trim();
                default -> {
                }
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }
}
