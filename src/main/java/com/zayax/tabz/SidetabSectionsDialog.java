package com.zayax.tabz;

import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class SidetabSectionsDialog extends DialogWrapper {
    private final List<SidetabSectionSpec> specs;
    private final JBCheckBox respectOrderCheckBox = new JBCheckBox("Respect section order", true);
    private final SpecsTableModel tableModel = new SpecsTableModel();
    private final JBTable table = new JBTable(tableModel);

    SidetabSectionsDialog(
            @Nullable JComponent parent,
            @NotNull List<SidetabSectionSpec> specs,
            boolean respectOrder
    ) {
        super(parent, true);
        this.specs = new ArrayList<>();
        for (SidetabSectionSpec spec : specs) {
            this.specs.add(spec.copy());
        }
        respectOrderCheckBox.setSelected(respectOrder);
        setTitle("Vertical tab sections");
        init();
    }

    @NotNull List<SidetabSectionSpec> specs() {
        List<SidetabSectionSpec> copy = new ArrayList<>(specs.size());
        for (SidetabSectionSpec spec : specs) {
            copy.add(spec.copy());
        }
        return copy;
    }

    boolean respectOrder() {
        return respectOrderCheckBox.isSelected();
    }

    @Override
    protected @NotNull JComponent createCenterPanel() {
        table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        table.setRowHeight(JBUI.scale(22));
        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);
        table.getColumnModel().getColumn(2).setPreferredWidth(220);
        table.getColumnModel().getColumn(3).setPreferredWidth(60);

        JBLabel help = new JBLabel("""
                <html>Each section has a <b>name</b>, <b>start logic</b>, and optional <b>end logic</b>.\
                 Without end logic, the section ends where the next one starts.<br>\
                Start/end logic: code snippets (<code>&lt;body</code>) or commands such as\
                 <code>@start</code>, <code>@tag body</code>, <code>@close-tag body</code>,\
                 <code>@after-tag head</code>, <code>@regex</code>, <code>@after-class-open</code>,\
                 <code>@first-method</code>, <code>@media</code>, <code>@eof</code>. Alternatives with <code>||</code>.<br>\
                <b>End incl.</b>: include the end-marker line in the section instead of ending before it.<br>\
                <b>Respect section order</b>: find sections in table order; otherwise sort by file position.<br>\
                TOP comments: <code>TOP-Name</code>, <code>TOPEND</code>/<code>TOP-END</code>,\
                 <code>SUB-{{Name}}</code>, <code>SUBEND</code>/<code>SUB-END</code>,\
                 <code>+SUB-…</code>, <code>+SUBEND</code> for deeper levels.</html>
                """);
        help.setBorder(JBUI.Borders.emptyBottom(8));

        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(table)
                .setToolbarPosition(ActionToolbarPosition.TOP)
                .setAddAction(button -> addSpec())
                .setRemoveAction(button -> removeSelected())
                .setMoveUpAction(button -> moveSelected(-1))
                .setMoveDownAction(button -> moveSelected(1));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(help, BorderLayout.NORTH);
        panel.add(decorator.createPanel(), BorderLayout.CENTER);
        respectOrderCheckBox.setBorder(JBUI.Borders.emptyTop(8));
        respectOrderCheckBox.setToolTipText(
                "On: sections are matched in table order. "
                        + "Off: section order is ignored; vertical tabs are sorted by file position."
        );
        panel.add(respectOrderCheckBox, BorderLayout.SOUTH);
        panel.setPreferredSize(new Dimension(JBUI.scale(820), JBUI.scale(340)));
        return panel;
    }

    private void addSpec() {
        specs.add(new SidetabSectionSpec("", "@start"));
        int row = specs.size() - 1;
        tableModel.fireTableRowsInserted(row, row);
        table.setRowSelectionInterval(row, row);
    }

    private void removeSelected() {
        int[] selected = table.getSelectedRows();
        if (selected.length == 0) {
            return;
        }
        Arrays.sort(selected);
        for (int index = selected.length - 1; index >= 0; index--) {
            specs.remove(selected[index]);
        }
        tableModel.fireTableDataChanged();
        if (!specs.isEmpty()) {
            int next = Math.min(selected[0], specs.size() - 1);
            table.setRowSelectionInterval(next, next);
        }
    }

    private void moveSelected(int direction) {
        int[] selected = table.getSelectedRows();
        if (!RulesTableDragSupport.canMoveSelectedBlock(selected, specs.size(), direction)) {
            return;
        }
        RulesTableDragSupport.moveSelectedBlock(specs, selected, direction);
        tableModel.fireTableDataChanged();
        int first = Arrays.stream(selected).min().orElse(0);
        RulesTableDragSupport.restoreSelection(
                table,
                direction < 0 ? first - 1 : first + 1,
                selected.length
        );
    }

    private final class SpecsTableModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return specs.size();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case 0 -> "Name";
                case 1 -> "Start logic";
                case 2 -> "End logic";
                case 3 -> "End incl.";
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 3 ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            SidetabSectionSpec spec = specs.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> spec.name;
                case 1 -> spec.start;
                case 2 -> spec.end;
                case 3 -> spec.endIncludesMarker;
                default -> "";
            };
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            SidetabSectionSpec spec = specs.get(rowIndex);
            switch (columnIndex) {
                case 0 -> spec.name = String.valueOf(value).trim();
                case 1 -> spec.start = String.valueOf(value).trim();
                case 2 -> spec.end = String.valueOf(value).trim();
                case 3 -> spec.endIncludesMarker = Boolean.TRUE.equals(value);
                default -> {
                }
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }
}