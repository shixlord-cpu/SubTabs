package de.sasbe.subtabs;

import com.intellij.ui.JBColor;
import com.intellij.ui.paint.EffectPainter;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

final class ComponentSubtabTextPainter {
    private static final JBColor ERROR_WAVE_COLOR = new JBColor(0xFF0000, 0xFF6B68);

    private ComponentSubtabTextPainter() {
    }

    static @NotNull Color errorWaveColor() {
        return ERROR_WAVE_COLOR;
    }

    static void paint(
            @NotNull Graphics2D graphics,
            @NotNull Font font,
            @NotNull String text,
            int x,
            int baseline,
            @NotNull Color textColor,
            boolean hasErrors
    ) {
        paint(graphics, font, text, x, baseline, textColor, hasErrors, false);
    }

    static void paint(
            @NotNull Graphics2D graphics,
            @NotNull Font font,
            @NotNull String text,
            int x,
            int baseline,
            @NotNull Color textColor,
            boolean hasErrors,
            boolean strikethrough
    ) {
        FontMetrics metrics = graphics.getFontMetrics(font);
        graphics.setFont(font);
        graphics.setColor(textColor);
        graphics.drawString(text, x, baseline);
        if (strikethrough) {
            int width = metrics.stringWidth(text);
            int lineY = baseline - Math.max(1, metrics.getAscent() / 2);
            graphics.drawLine(x, lineY, x + width, lineY);
        }
        if (hasErrors) {
            int width = metrics.stringWidth(text);
            graphics.setColor(ERROR_WAVE_COLOR);
            EffectPainter.WAVE_UNDERSCORE.paint(
                    graphics,
                    x,
                    baseline + 1,
                    width,
                    Math.max(2, metrics.getDescent()),
                    font
            );
        }
    }
}
