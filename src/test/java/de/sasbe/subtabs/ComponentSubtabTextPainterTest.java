package de.sasbe.subtabs;

import com.intellij.util.ui.JBUI;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ComponentSubtabTextPainterTest {
    @Test
    void strikethroughDrawsAcrossLabelWidth() {
        BufferedImage plain = render("Body", false);
        BufferedImage struck = render("Body", true);
        assertNotEquals(countNonBackground(plain), countNonBackground(struck));
    }

    private static BufferedImage render(String text, boolean strikethrough) {
        BufferedImage image = new BufferedImage(JBUI.scale(80), JBUI.scale(24), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(0x2B2B2B));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            Font font = new Font(Font.SANS_SERIF, Font.PLAIN, JBUI.scale(12));
            ComponentSubtabTextPainter.paint(
                    graphics,
                    font,
                    text,
                    JBUI.scale(8),
                    JBUI.scale(16),
                    Color.WHITE,
                    false,
                    strikethrough
            );
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static int countNonBackground(BufferedImage image) {
        int background = image.getRGB(0, 0);
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != background) {
                    count++;
                }
            }
        }
        return count;
    }
}
