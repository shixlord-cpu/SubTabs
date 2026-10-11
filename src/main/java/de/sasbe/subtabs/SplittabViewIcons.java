package de.sasbe.subtabs;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

final class SplittabViewIcons {
    private static final Color COLOR_ACTIVE = new Color(0x3B82F6);
    private static final Color COLOR_IDLE = new Color(0x9CA3AF);

    private SplittabViewIcons() {
    }

    static @NotNull Icon forProject(@NotNull Project project) {
        Icon icon;
        if (!ComponentSubtabEditorSplitRegistry.getInstance(project).hasSavedSplittabs()
                && ComponentSubtabEditorSplitNavigation.hasNativeTwoPaneSplitCandidate(project)) {
            icon = unsavedNativeTwoPaneIcon(project);
        } else {
            boolean dedicated = SplittabDedicatedViewService.usesDedicatedBehavior(project);
            boolean splitViewActive = dedicated
                    && SplittabDedicatedViewService.getInstance(project).isDedicatedViewActive();
            Color color = dedicated && !splitViewActive ? COLOR_IDLE : COLOR_ACTIVE;
            if (!dedicated) {
                color = COLOR_ACTIVE;
            }
            icon = splitViewActive ? filledSplitIcon(color) : outlineSplitIcon(color);
        }
        if (ComponentSubtabEditorSplitNavigation.isTwoPaneStackedVertically(project)) {
            return rotatedQuarterTurn(icon);
        }
        return icon;
    }

    private static @NotNull Icon rotatedQuarterTurn(@NotNull Icon icon) {
        return new RotatedIcon(icon, Math.PI / 2);
    }

    private static @NotNull Icon unsavedNativeTwoPaneIcon(@NotNull Project project) {
        boolean dedicated = SplittabDedicatedViewService.usesDedicatedBehavior(project);
        Color ringColor = dedicated ? COLOR_IDLE : COLOR_ACTIVE;
        return new UnsavedSplitPairIcon(ringColor);
    }

    static @NotNull Icon outlineSplitIcon(@NotNull Color color) {
        return IconUtil.colorize(AllIcons.Actions.SplitVertically, color);
    }

    static @NotNull Icon filledSplitIcon(@NotNull Color color) {
        return new FilledSplitIcon(color);
    }

    private static final class RotatedIcon implements Icon {
        private final Icon delegate;
        private final double radians;

        private RotatedIcon(@NotNull Icon delegate, double radians) {
            this.delegate = delegate;
            this.radians = radians;
        }

        @Override
        public void paintIcon(@NotNull Component component, @NotNull Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int width = getIconWidth();
                int height = getIconHeight();
                g2.translate(x + width / 2.0, y + height / 2.0);
                g2.rotate(radians);
                delegate.paintIcon(component, g2, -delegate.getIconWidth() / 2, -delegate.getIconHeight() / 2);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return delegate.getIconHeight();
        }

        @Override
        public int getIconHeight() {
            return delegate.getIconWidth();
        }
    }

    /** Two empty circles side by side — unsaved split-pair affordance. */
    private static final class UnsavedSplitPairIcon implements Icon {
        private final Color ringColor;

        private UnsavedSplitPairIcon(@NotNull Color ringColor) {
            this.ringColor = ringColor;
        }

        @Override
        public void paintIcon(@NotNull Component component, @NotNull Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);
                int size = getIconWidth();
                float unit = size / 16f;
                float ringStroke = Math.max(1f, 1.05f * unit);
                float ringDiameter = 5f * unit;
                float ringRadius = ringDiameter / 2f;
                float centerY = size / 2f;
                float gap = JBUI.scale(3f);
                float pairWidth = ringDiameter * 2f + gap;
                float leftX = (size - pairWidth) / 2f;
                float rightX = leftX + ringDiameter + gap;
                float topY = centerY - ringRadius;

                g2.setColor(ringColor);
                g2.setStroke(new BasicStroke(ringStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Ellipse2D.Float(leftX, topY, ringDiameter, ringDiameter));
                g2.draw(new Ellipse2D.Float(rightX, topY, ringDiameter, ringDiameter));
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
