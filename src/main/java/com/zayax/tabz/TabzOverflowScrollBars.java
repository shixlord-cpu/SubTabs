package com.zayax.tabz;

import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.BorderFactory;
import javax.swing.JScrollBar;
import java.awt.Adjustable;
import java.awt.Dimension;

/** Thin, low-contrast overflow scrollbars that sit beside content instead of covering tabs. */
final class TabzOverflowScrollBars {
    private TabzOverflowScrollBars() {
    }

    static int thickness() {
        return JBUI.scale(5);
    }

    static void configure(@NotNull JScrollBar bar) {
        int thickness = thickness();
        if (bar.getOrientation() == Adjustable.HORIZONTAL) {
            bar.setPreferredSize(new Dimension(Integer.MAX_VALUE / 4, thickness));
            bar.setMinimumSize(new Dimension(0, thickness));
            bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, thickness));
        } else {
            bar.setPreferredSize(new Dimension(thickness, Integer.MAX_VALUE / 4));
            bar.setMinimumSize(new Dimension(thickness, 0));
            bar.setMaximumSize(new Dimension(thickness, Integer.MAX_VALUE));
        }
        bar.setOpaque(false);
        bar.setBackground(UIUtil.getPanelBackground());
        bar.setBorder(BorderFactory.createEmptyBorder());
        bar.putClientProperty("JScrollBar.showButtons", Boolean.FALSE);
    }

    static void configureHorizontal(@NotNull JScrollBar bar) {
        configure(bar);
    }

    static void configureVertical(@NotNull JScrollBar bar) {
        configure(bar);
    }

    static int horizontalReserve(@NotNull SubtabOverflowMode mode) {
        return mode == SubtabOverflowMode.SCROLLBAR ? thickness() : 0;
    }

    static int verticalReserve(@NotNull SubtabOverflowMode mode) {
        return mode == SubtabOverflowMode.SCROLLBAR ? thickness() : 0;
    }
}
