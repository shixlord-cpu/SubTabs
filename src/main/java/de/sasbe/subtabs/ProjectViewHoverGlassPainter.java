package de.sasbe.subtabs;

import com.intellij.openapi.ui.AbstractPainter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTree;
import java.awt.Component;
import java.awt.Graphics2D;

/**
 * Kept so the glass pane still invalidates when hover rows change. The row itself is painted by
 * the native tree hover ({@code TreeHoverListener}), so this painter does not fill over the text.
 */
final class ProjectViewHoverGlassPainter extends AbstractPainter {
    private final JTree tree;

    ProjectViewHoverGlassPainter(@NotNull JTree tree) {
        this.tree = tree;
    }

    @Override
    public boolean needsRepaint() {
        return false;
    }

    @Override
    public void executePaint(@NotNull Component component, @NotNull Graphics2D graphics) {
    }
}
