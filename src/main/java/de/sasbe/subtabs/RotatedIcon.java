package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

final class RotatedIcon implements Icon {
    private final Icon delegate;
    private final int degrees;

    RotatedIcon(@NotNull Icon delegate, int degrees) {
        this.delegate = delegate;
        this.degrees = degrees;
    }

    @Override
    public void paintIcon(@Nullable Component component, @NotNull Graphics graphics, int x, int y) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            double centerX = x + getIconWidth() / 2.0;
            double centerY = y + getIconHeight() / 2.0;
            AffineTransform transform = g2.getTransform();
            transform.rotate(Math.toRadians(degrees), centerX, centerY);
            g2.setTransform(transform);
            int paintX = x + (getIconWidth() - delegate.getIconWidth()) / 2;
            int paintY = y + (getIconHeight() - delegate.getIconHeight()) / 2;
            delegate.paintIcon(component, g2, paintX, paintY);
        } finally {
            g2.dispose();
        }
    }

    @Override
    public int getIconWidth() {
        return swapsAxes() ? delegate.getIconHeight() : delegate.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return swapsAxes() ? delegate.getIconWidth() : delegate.getIconHeight();
    }

    private boolean swapsAxes() {
        int normalized = Math.floorMod(degrees, 360);
        return normalized == 90 || normalized == 270;
    }
}
