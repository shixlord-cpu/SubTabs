package de.sasbe.subtabs;

import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntConsumer;

final class RulesTableDragSupport {
    private static final Color DROP_LINE_COLOR = JBUI.CurrentTheme.TabbedPane.ENABLED_SELECTED_COLOR;

    private final JBTable table;
    private final int dragColumn;
    private final IntConsumer onDropAtRow;
    private Point dragOrigin;
    private boolean dragArmed;
    private boolean dragging;
    private int dropIndicatorRow = -1;

    RulesTableDragSupport(
            @NotNull JBTable table,
            int dragColumn,
            @NotNull IntConsumer onDropAtRow
    ) {
        this.table = table;
        this.dragColumn = dragColumn;
        this.onDropAtRow = onDropAtRow;
        table.setDragEnabled(false);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        table.setCellSelectionEnabled(false);
        table.getTableHeader().setReorderingAllowed(false);
    }

    boolean processMouseEvent(@NotNull MouseEvent event) {
        return switch (event.getID()) {
            case MouseEvent.MOUSE_PRESSED -> onPressed(event);
            case MouseEvent.MOUSE_RELEASED -> onReleased(event);
            default -> false;
        };
    }

    boolean processMouseMotionEvent(@NotNull MouseEvent event) {
        return switch (event.getID()) {
            case MouseEvent.MOUSE_DRAGGED -> onDragged(event);
            case MouseEvent.MOUSE_MOVED -> onMoved(event);
            default -> false;
        };
    }

    void paintDropIndicator(@NotNull Graphics graphics) {
        if (!dragging || dropIndicatorRow < 0) {
            return;
        }

        int y = dropRowY(dropIndicatorRow);
        graphics.setColor(DROP_LINE_COLOR);
        graphics.fillRect(0, y, table.getWidth(), JBUI.scale(2));
    }

    private boolean onPressed(@NotNull MouseEvent event) {
        if (!SwingUtilities.isLeftMouseButton(event) || !isDragHandle(event)) {
            dragArmed = false;
            return false;
        }

        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }

        int row = table.rowAtPoint(event.getPoint());
        if (row < 0) {
            dragArmed = false;
            return false;
        }

        if (!table.isRowSelected(row)) {
            if (event.isControlDown() || event.isMetaDown()) {
                table.addRowSelectionInterval(row, row);
            } else if (event.isShiftDown()) {
                int anchor = Math.max(0, table.getSelectionModel().getAnchorSelectionIndex());
                table.setRowSelectionInterval(Math.min(anchor, row), Math.max(anchor, row));
            } else {
                table.setRowSelectionInterval(row, row);
            }
        }

        dragArmed = true;
        dragging = false;
        dragOrigin = event.getPoint();
        updateDropIndicator(event.getPoint());
        event.consume();
        return true;
    }

    private boolean onDragged(@NotNull MouseEvent event) {
        if (!dragArmed || dragOrigin == null) {
            return false;
        }
        if (dragOrigin.distance(event.getPoint()) < JBUI.scale(4)) {
            event.consume();
            return true;
        }
        dragging = true;
        table.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        updateDropIndicator(event.getPoint());
        table.repaint();
        event.consume();
        return true;
    }

    private boolean onReleased(@NotNull MouseEvent event) {
        if (!dragArmed) {
            return false;
        }
        if (dragging && dropIndicatorRow >= 0) {
            onDropAtRow.accept(dropIndicatorRow);
        }
        dragArmed = false;
        dragging = false;
        dragOrigin = null;
        dropIndicatorRow = -1;
        table.setCursor(Cursor.getDefaultCursor());
        table.repaint();
        event.consume();
        return true;
    }

    private boolean onMoved(@NotNull MouseEvent event) {
        if (isDragHandle(event) && table.rowAtPoint(event.getPoint()) >= 0) {
            table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else if (!dragging) {
            table.setCursor(Cursor.getDefaultCursor());
        }
        return false;
    }

    private boolean isDragHandle(@NotNull MouseEvent event) {
        int viewColumn = table.columnAtPoint(event.getPoint());
        if (viewColumn < 0) {
            return false;
        }
        return table.convertColumnIndexToModel(viewColumn) == dragColumn;
    }

    private void updateDropIndicator(@NotNull Point point) {
        dropIndicatorRow = resolveDropRow(point);
    }

    private int resolveDropRow(@NotNull Point point) {
        int row = table.rowAtPoint(point);
        if (row < 0) {
            return table.getRowCount();
        }

        Rectangle bounds = table.getCellRect(row, 0, false);
        return point.y < bounds.y + bounds.height / 2 ? row : row + 1;
    }

    private int dropRowY(int dropRow) {
        if (dropRow >= table.getRowCount()) {
            if (table.getRowCount() == 0) {
                return 0;
            }
            Rectangle lastRow = table.getCellRect(table.getRowCount() - 1, 0, false);
            return lastRow.y + lastRow.height;
        }
        return table.getCellRect(dropRow, 0, false).y;
    }

    static void moveRowsTo(@NotNull List<CustomSubtabRule> rules, @NotNull int[] selectedRows, int targetRow) {
        if (selectedRows.length == 0) {
            return;
        }

        int[] sorted = Arrays.copyOf(selectedRows, selectedRows.length);
        Arrays.sort(sorted);

        List<CustomSubtabRule> moving = new ArrayList<>(sorted.length);
        for (int index = sorted.length - 1; index >= 0; index--) {
            moving.add(0, rules.remove(sorted[index]));
        }

        int adjustedTarget = targetRow;
        for (int row : sorted) {
            if (row < targetRow) {
                adjustedTarget--;
            }
        }
        adjustedTarget = Math.max(0, Math.min(adjustedTarget, rules.size()));
        rules.addAll(adjustedTarget, moving);
    }

    static boolean isContiguous(@NotNull int[] selectedRows) {
        if (selectedRows.length <= 1) {
            return true;
        }
        int[] sorted = Arrays.copyOf(selectedRows, selectedRows.length);
        Arrays.sort(sorted);
        for (int index = 1; index < sorted.length; index++) {
            if (sorted[index] != sorted[index - 1] + 1) {
                return false;
            }
        }
        return true;
    }

    static void moveSelectedBlock(@NotNull List<CustomSubtabRule> rules, @NotNull int[] selectedRows, int direction) {
        if (selectedRows.length == 0) {
            return;
        }

        int[] sorted = Arrays.copyOf(selectedRows, selectedRows.length);
        Arrays.sort(sorted);
        int min = sorted[0];
        int max = sorted[sorted.length - 1];

        if (direction < 0) {
            if (min <= 0) {
                return;
            }
            CustomSubtabRule above = rules.remove(min - 1);
            rules.add(max, above);
            return;
        }

        if (max >= rules.size() - 1) {
            return;
        }
        CustomSubtabRule below = rules.remove(max + 1);
        rules.add(min, below);
    }

    static boolean canMoveSelectedBlock(@NotNull int[] selectedRows, int rowCount, int direction) {
        if (selectedRows.length == 0) {
            return false;
        }

        int[] sorted = Arrays.copyOf(selectedRows, selectedRows.length);
        Arrays.sort(sorted);
        if (direction < 0) {
            return sorted[0] > 0;
        }
        return sorted[sorted.length - 1] < rowCount - 1;
    }

    static void restoreSelection(@NotNull JBTable table, int firstRow, int count) {
        if (count <= 0 || firstRow < 0 || firstRow >= table.getRowCount()) {
            return;
        }
        int lastRow = Math.min(firstRow + count - 1, table.getRowCount() - 1);
        table.setRowSelectionInterval(firstRow, lastRow);
        Rectangle visible = table.getCellRect(firstRow, 0, true);
        visible.height = table.getCellRect(lastRow, 0, true).y + table.getRowHeight() - visible.y;
        table.scrollRectToVisible(visible);
        SwingUtilities.invokeLater(() -> table.requestFocusInWindow());
    }
}
