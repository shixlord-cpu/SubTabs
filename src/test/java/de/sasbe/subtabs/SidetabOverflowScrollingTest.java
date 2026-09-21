package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import java.awt.Dimension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SidetabOverflowScrollingTest {
    @Test
    void verticalSnapshotDetectsOverflow() {
        JPanel view = new JPanel();
        view.setPreferredSize(new Dimension(80, 400));
        JScrollPane scrollPane = new JScrollPane(view);
        scrollPane.setSize(80, 120);
        scrollPane.doLayout();
        JViewport viewport = scrollPane.getViewport();
        viewport.setViewPosition(new java.awt.Point(0, 0));
        SubtabBarScrolling.syncViewHeight(viewport, view.getPreferredSize().height, viewport.getHeight());

        SubtabBarScrolling.VerticalViewportSnapshot top =
                SubtabBarScrolling.snapshotVertical(viewport, view.getPreferredSize().height);
        assertFalse(top.canScrollUp());
        assertTrue(top.canScrollDown());

        SubtabBarScrolling.applyVerticalScroll(
                viewport,
                scrollPane.getVerticalScrollBar(),
                view.getPreferredSize().height,
                500
        );
        SubtabBarScrolling.VerticalViewportSnapshot bottom =
                SubtabBarScrolling.snapshotVertical(viewport, view.getPreferredSize().height);
        assertTrue(bottom.canScrollUp());
        assertFalse(bottom.canScrollDown());
    }
}
