package de.sasbe.subtabs;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AWTEvent;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.function.IntSupplier;

final class SidetabOverflowStrip extends JPanel {
    private static final int ARROW_HEIGHT = 22;

    private final JBScrollPane scrollPane;
    private final IntSupplier contentHeight;
    private final ArrowButton upArrow = new ArrowButton(true);
    private final ArrowButton downArrow = new ArrowButton(false);
    private final MouseWheelListener wheelListener = this::onWheel;
    private final AWTEventListener globalWheel = event -> {
        if (event instanceof MouseWheelEvent wheel) {
            onWheel(wheel);
        }
    };
    private SubtabOverflowMode mode = SubtabOverflowMode.SCROLLBAR;
    private boolean enabled = true;

    SidetabOverflowStrip(
            @NotNull JBScrollPane scrollPane,
            @NotNull IntSupplier contentHeight
    ) {
        super(null);
        this.scrollPane = scrollPane;
        this.contentHeight = contentHeight;
        setOpaque(false);
        enableEvents(AWTEvent.MOUSE_WHEEL_EVENT_MASK);
        add(scrollPane);
        add(upArrow);
        add(downArrow);
        setComponentZOrder(upArrow, 0);
        setComponentZOrder(downArrow, 1);
        setComponentZOrder(scrollPane, 2);

        scrollPane.setWheelScrollingEnabled(false);
        attachWheel(this);
        attachWheel(scrollPane);
        attachWheel(scrollPane.getViewport());
        attachWheel(upArrow);
        attachWheel(downArrow);

        scrollPane.getViewport().addChangeListener(event -> updateArrows());
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                layoutChildren();
                updateArrows();
            }
        });
        updateArrows();
    }

    @NotNull MouseWheelListener wheelListener() {
        return wheelListener;
    }

    void attachWheel(@NotNull JComponent component) {
        component.removeMouseWheelListener(wheelListener);
        component.addMouseWheelListener(wheelListener);
    }

    void setMode(@NotNull SubtabOverflowMode mode) {
        this.mode = mode;
        applyScrollPolicy();
    }

    void setOverflowEnabled(boolean enabled) {
        this.enabled = enabled;
        applyScrollPolicy();
    }

    private void applyScrollPolicy() {
        JScrollBar bar = scrollPane.getVerticalScrollBar();
        boolean arrows = enabled && mode == SubtabOverflowMode.ARROWS;
        bar.setPreferredSize(arrows ? new Dimension(0, 0) : null);
        bar.setMinimumSize(arrows ? new Dimension(0, 0) : null);
        bar.setMaximumSize(arrows ? new Dimension(0, 0) : null);
        scrollPane.setVerticalScrollBarPolicy(
                enabled
                        ? (arrows
                        ? ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
                        : ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED)
                        : ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
        );
        layoutChildren();
        updateArrows();
        revalidate();
        repaint();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        Toolkit.getDefaultToolkit().addAWTEventListener(globalWheel, AWTEvent.MOUSE_WHEEL_EVENT_MASK);
    }

    @Override
    public void removeNotify() {
        Toolkit.getDefaultToolkit().removeAWTEventListener(globalWheel);
        super.removeNotify();
    }

    @Override
    protected void processMouseWheelEvent(MouseWheelEvent event) {
        onWheel(event);
    }

    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    @Override
    public Dimension getPreferredSize() {
        return scrollPane.getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(scrollPane.getPreferredSize().width, 0);
    }

    @Override
    public void doLayout() {
        layoutChildren();
        updateArrows();
    }

    private void layoutChildren() {
        int width = getWidth();
        int height = getHeight();
        scrollPane.setBounds(0, 0, width, height);
        int arrowHeight = JBUI.scale(ARROW_HEIGHT);
        upArrow.setBounds(0, 0, width, arrowHeight);
        downArrow.setBounds(0, Math.max(0, height - arrowHeight), width, arrowHeight);
    }

    private void onWheel(@NotNull MouseWheelEvent event) {
        if (!enabled || event.isConsumed()) {
            return;
        }
        Component source = event.getComponent();
        if (source != null && !SwingUtilities.isDescendingFrom(source, this) && source != this) {
            return;
        }
        int delta = SubtabBarScrolling.wheelPixelDelta(
                event.getPreciseWheelRotation(),
                event.getWheelRotation(),
                JBUI.scale(SubtabBarScrolling.DEFAULT_PIXEL_STEP)
        );
        if (delta == 0) {
            return;
        }
        scrollByPixels(delta);
        event.consume();
    }

    private void scrollBy(int direction) {
        JViewport viewport = scrollPane.getViewport();
        int step = SubtabBarScrolling.step(Math.max(viewport.getHeight(), 1), JBUI.scale(80));
        scrollByPixels(direction * step);
    }

    private void scrollByPixels(int delta) {
        SubtabBarScrolling.applyVerticalScroll(
                scrollPane.getViewport(),
                scrollPane.getVerticalScrollBar(),
                contentHeight.getAsInt(),
                delta
        );
        updateArrows();
    }

    private void updateArrows() {
        JViewport viewport = scrollPane.getViewport();
        int height = contentHeight.getAsInt();
        if (enabled) {
            SubtabBarScrolling.syncViewHeight(
                    viewport,
                    height,
                    Math.max(viewport.getExtentSize().height, viewport.getHeight())
            );
        }
        boolean arrows = enabled && mode == SubtabOverflowMode.ARROWS;
        SubtabBarScrolling.VerticalViewportSnapshot snapshot =
                SubtabBarScrolling.snapshotVertical(viewport, height);
        upArrow.setVisible(arrows && snapshot.canScrollUp());
        downArrow.setVisible(arrows && snapshot.canScrollDown());
    }

    private final class ArrowButton extends JButton {
        private final boolean up;
        private final Timer repeatTimer;

        private ArrowButton(boolean up) {
            this.up = up;
            this.repeatTimer = new Timer(70, event -> scrollBy(up ? -1 : 1));
            repeatTimer.setInitialDelay(280);
            setFocusable(false);
            setBorder(JBUI.Borders.empty());
            setContentAreaFilled(false);
            setOpaque(false);
            enableEvents(AWTEvent.MOUSE_WHEEL_EVENT_MASK);
            setIcon(up ? AllIcons.General.ChevronUp : AllIcons.General.ChevronDown);
            setToolTipText(up ? "Nach oben scrollen" : "Nach unten scrollen");
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    scrollBy(up ? -1 : 1);
                    repeatTimer.restart();
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    repeatTimer.stop();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    repeatTimer.stop();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color opaque = UIUtil.getPanelBackground();
                Color fade = new Color(opaque.getRed(), opaque.getGreen(), opaque.getBlue(), 0);
                int height = getHeight();
                GradientPaint paint = up
                        ? new GradientPaint(0, 0, opaque, 0, height, fade)
                        : new GradientPaint(0, 0, fade, 0, height, opaque);
                g2.setPaint(paint);
                g2.setComposite(AlphaComposite.SrcOver);
                g2.fillRect(0, 0, getWidth(), height);
            } finally {
                g2.dispose();
            }
            super.paintComponent(graphics);
        }

        @Override
        public boolean contains(int x, int y) {
            return isVisible() && super.contains(x, y);
        }
    }
}
