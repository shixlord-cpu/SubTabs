import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * TABZ wordmark icon (T + stacked tabs A/B + Z) centered in a square PNG with transparent background.
 */
final class RenderTabzPluginIcon {
    private static final Color STROKE = new Color(0x8B939C);
    private static final Color T_FILL = new Color(0x0D0D0D);
    private static final Color TAB_A = new Color(0x2A3F7A);
    private static final Color TAB_B = new Color(0x0D0D0D);
    private static final Color Z_FILL = new Color(0x5E6369);
    private static final Color ACCENT = new Color(0x0099FF);

    public static void main(String[] args) throws Exception {
        Path out = args.length > 0 ? Path.of(args[0]) : Path.of("assets/tabz-plugin-icon.png");
        int size = args.length > 1 ? Integer.parseInt(args[1]) : 512;
        Files.createDirectories(out.getParent());
        write(out, size);
    }

    private static void write(Path path, int size) throws Exception {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        float designW = 440f;
        float contentH = 268f;
        float designH = designW;
        float scale = (size * 0.88f) / designW;
        float tx = (size - designW * scale) / 2f;
        float ty = (size - designH * scale) / 2f;
        g.translate(tx, ty);
        g.scale(scale, scale);
        g.translate(0, (designH - contentH) / 2f);

        float stroke = 3f;
        g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));

        drawT(g, stroke);
        drawTabA(g, stroke);
        drawTabB(g, stroke);
        drawZ(g, stroke);

        g.dispose();
        ImageIO.write(image, "png", path.toFile());
        System.out.println("Wrote " + path.toAbsolutePath());
    }

    private static void fillStrokeShape(Graphics2D g, Path2D shape, Color fill, float stroke) {
        g.setColor(fill);
        g.fill(shape);
        g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        g.setColor(STROKE);
        g.draw(shape);
    }

    private static void fillStrokeRect(Graphics2D g, float x, float y, float w, float h, Color fill, float stroke) {
        Path2D rect = new Path2D.Float();
        rect.moveTo(x, y);
        rect.lineTo(x + w, y);
        rect.lineTo(x + w, y + h);
        rect.lineTo(x, y + h);
        rect.closePath();
        fillStrokeShape(g, rect, fill, stroke);
    }

    private static void drawT(Graphics2D g, float stroke) {
        Path2D t = new Path2D.Float();
        t.moveTo(36, 36);
        t.lineTo(132, 36);
        t.lineTo(132, 88);
        t.lineTo(98, 88);
        t.lineTo(98, 232);
        t.lineTo(70, 232);
        t.lineTo(70, 88);
        t.lineTo(36, 88);
        t.closePath();
        fillStrokeShape(g, t, T_FILL, stroke);
    }

    private static void drawTabA(Graphics2D g, float stroke) {
        float x = 148;
        float y = 36;
        float s = 86;
        fillStrokeRect(g, x, y, s, s, TAB_A, stroke);
        g.setColor(ACCENT);
        g.fill(new RoundRectangle2D.Float(x + 4, y + s - 8, s - 8, 5, 2, 2));
        drawLetter(g, "A", x, y, s);
    }

    private static void drawTabB(Graphics2D g, float stroke) {
        float x = 148;
        float y = 122;
        float s = 86;
        fillStrokeRect(g, x, y, s, s, TAB_B, stroke);
        drawLetter(g, "B", x, y, s);
    }

    private static void drawLetter(Graphics2D g, String letter, float x, float y, float box) {
        int fontSize = Math.round(box * 0.52f);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
        var fm = g.getFontMetrics();
        Rectangle2D bounds = fm.getStringBounds(letter, g);
        float bx = (float) (x + (box - bounds.getWidth()) / 2 - bounds.getX());
        float by = (float) (y + (box - bounds.getHeight()) / 2 - bounds.getY());
        g.setColor(Color.WHITE);
        g.drawString(letter, bx, by);
    }

    private static void drawZ(Graphics2D g, float stroke) {
        float x0 = 244;
        float y0 = 122;
        float topW = 160;
        float barH = 46;
        Path2D z = new Path2D.Float();
        z.moveTo(x0, y0);
        z.lineTo(x0 + topW, y0);
        z.lineTo(x0 + topW, y0 + barH);
        z.lineTo(x0 + 44, y0 + barH);
        z.lineTo(x0 + topW + 24, y0 + barH + 66);
        z.lineTo(x0 + topW + 24, y0 + barH + 66 + barH);
        z.lineTo(x0 - 20, y0 + barH + 66 + barH);
        z.lineTo(x0 - 20, y0 + barH + 66);
        z.lineTo(x0 + topW - 36, y0 + barH + 66);
        z.lineTo(x0 + 44, y0 + barH);
        z.closePath();
        fillStrokeShape(g, z, Z_FILL, stroke);
    }
}
