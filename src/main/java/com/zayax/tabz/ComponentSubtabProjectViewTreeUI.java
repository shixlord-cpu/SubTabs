package com.zayax.tabz;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.tree.ui.DefaultTreeUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Set;

/**
 * Paints external main-tab hover rows with the same native tree hover color as tabz hover
 * before the default tree UI draws row content on top.
 */
final class ComponentSubtabProjectViewTreeUI extends DefaultTreeUI {
    private static final Key<Boolean> INSTALLED = Key.create("componentTabz.projectViewTreeUiInstalled");

    private ComponentSubtabProjectViewTreeUI() {
    }

    static void install(@NotNull JTree tree) {
        if (!(tree.getUI() instanceof DefaultTreeUI) || tree.getUI() instanceof ComponentSubtabProjectViewTreeUI) {
            return;
        }
        tree.setUI(new ComponentSubtabProjectViewTreeUI());
        if (!Boolean.TRUE.equals(tree.getClientProperty(INSTALLED))) {
            tree.putClientProperty(INSTALLED, Boolean.TRUE);
            tree.addPropertyChangeListener("UI", event -> {
                if (!(tree.getUI() instanceof ComponentSubtabProjectViewTreeUI)) {
                    SwingUtilities.invokeLater(() -> install(tree));
                }
            });
        }
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        if (c instanceof JTree tree) {
            paintExternalHoverRowBackgrounds(g, tree);
        }
        super.paint(g, c);
    }

    private static void paintExternalHoverRowBackgrounds(@NotNull Graphics g, @NotNull JTree tree) {
        Set<Integer> rows = ComponentSubtabProjectViewHover.paintedHoverRows(tree);
        if (rows.isEmpty() || InternalPlatformBridge.isTreeHoverPaintingDisabled(tree)) {
            return;
        }

        Color hover = InternalPlatformBridge.treeHoverBackground(tree);
        VirtualFile primaryHighlightFile = ComponentSubtabProjectViewHover.primaryHoverFile(tree);

        int x = InternalPlatformBridge.treeRenderingX(tree);
        int width = InternalPlatformBridge.treeRenderingWidth(tree);
        if (width <= 0) {
            return;
        }

        int nativeHoverRow = InternalPlatformBridge.treeNativeHoveredRow(tree);
        for (int row : rows) {
            if (row == nativeHoverRow || tree.isRowSelected(row)) {
                continue;
            }
            Rectangle bounds = tree.getRowBounds(row);
            if (bounds == null) {
                continue;
            }
            Color fill = externalHoverFill(tree, row, primaryHighlightFile, hover);
            if (fill == null) {
                continue;
            }
            g.setColor(fill);
            g.fillRect(x, bounds.y, width, bounds.height);
        }
    }

    private static @Nullable Color externalHoverFill(
            @NotNull JTree tree,
            int row,
            @Nullable VirtualFile primaryHighlightFile,
            @NotNull Color hover
    ) {
        if (primaryHighlightFile != null) {
            TreePath path = tree.getPathForRow(row);
            VirtualFile rowFile = path == null ? null : ComponentSubtabProjectViewHover.virtualFileOf(path);
            if (rowFile != null && !primaryHighlightFile.equals(rowFile)) {
                return null;
            }
        }
        return hover;
    }
}
