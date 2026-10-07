package de.sasbe.subtabs;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

final class SplittabViewIcons {
    private static final Color COLOR_ACTIVE = new Color(0x3B82F6);
    private static final Color COLOR_IDLE = new Color(0x9CA3AF);

    private SplittabViewIcons() {
    }

    static @NotNull Icon forProject(@NotNull Project project) {
        boolean dedicated = SplittabDedicatedViewService.usesDedicatedBehavior(project);
        boolean splitViewActive = dedicated
                && SplittabDedicatedViewService.getInstance(project).isDedicatedViewActive();
        Color color = dedicated && !splitViewActive ? COLOR_IDLE : COLOR_ACTIVE;
        if (!dedicated) {
            color = COLOR_ACTIVE;
        }
        return splitViewActive ? filledSplitIcon(color) : outlineSplitIcon(color);
    }

    static @NotNull Icon outlineSplitIcon(@NotNull Color color) {
        return IconUtil.colorize(AllIcons.Actions.SplitVertically, color);
    }

    static @NotNull Icon filledSplitIcon(@NotNull Color color) {
        return new FilledSplitIcon(color);
    }

    private static final class FilledSplitIcon implements Icon {
        private final Color fillColor;

        private FilledSplitIcon(@NotNull Color fillColor) {
            this.fillColor = fillColor;
        }

        @Override
        public void paintIcon(@NotNull Component component, @NotNull Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);
                int size = getIconWidth();
                float unit = size / 16f;
                float inset = 2.2f * unit;
                float gap = 1.1f * unit;
                float arc = 1.1f * unit;
                float paneWidth = (size - 2f * inset - gap) / 2f;
                float paneHeight = size - 2f * inset;
                g2.setColor(fillColor);
                g2.fill(new RoundRectangle2D.Float(inset, inset, paneWidth, paneHeight, arc, arc));
                g2.fill(new RoundRectangle2D.Float(inset + paneWidth + gap, inset, paneWidth, paneHeight, arc, arc));
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return JBUI.scale(16);
        }

        @Override
        public int getIconHeight() {
            return JBUI.scale(16);
        }
    }
}
