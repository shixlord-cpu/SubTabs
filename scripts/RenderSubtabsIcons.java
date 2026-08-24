import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

final class RenderSubtabsIcons {
    private static final Color ACTIVE = new Color(0x3B82F6);
    private static final Color INACTIVE = new Color(0x8A8A8A);

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        Path icons = root.resolve("src/main/resources/icons");
        Path meta = root.resolve("src/main/resources/META-INF");
        Files.createDirectories(icons);
        Files.createDirectories(meta);

        write(icons.resolve("subtabs.png"), 16, true);
        write(icons.resolve("subtabs@2x.png"), 32, true);
        write(icons.resolve("subtabsInactive.png"), 16, false);
        write(icons.resolve("subtabsInactive@2x.png"), 32, false);
        write(meta.resolve("pluginIcon.png"), 40, true);
        write(meta.resolve("pluginIcon@2x.png"), 80, true);
        write(meta.resolve("pluginIcon_dark.png"), 40, true);
        write(meta.resolve("pluginIcon_dark@2x.png"), 80, true);
    }

    private static void write(Path path, int size, boolean active) throws Exception {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);

        Color color = active ? ACTIVE : INACTIVE;
        float unit = size / 16f;
        float stroke = Math.max(1.5f, unit * 1.35f);
        g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        drawTab(g, 1.2f * unit, 1.8f * unit, 6.4f * unit, 5.0f * unit, 1.5f * unit, color, active);
        drawTab(g, 8.4f * unit, 1.8f * unit, 6.4f * unit, 5.0f * unit, 1.5f * unit, color, false);
        drawTab(g, 1.2f * unit, 8.6f * unit, 4.2f * unit, 5.2f * unit, 1.3f * unit, color, active);
        drawTab(g, 5.9f * unit, 8.6f * unit, 4.2f * unit, 5.2f * unit, 1.3f * unit, color, false);
        drawTab(g, 10.6f * unit, 8.6f * unit, 4.2f * unit, 5.2f * unit, 1.3f * unit, color, false);

        g.dispose();
        ImageIO.write(image, "png", path.toFile());
        System.out.println("Wrote " + path);
    }

    private static void drawTab(
            Graphics2D g,
            float x,
            float y,
            float width,
            float height,
            float arc,
            Color color,
            boolean filled
    ) {
        RoundRectangle2D.Float shape = new RoundRectangle2D.Float(x, y, width, height, arc, arc);
        if (filled) {
            g.setColor(color);
            g.fill(shape);
            return;
        }

        Color wash = new Color(color.getRed(), color.getGreen(), color.getBlue(), 48);
        g.setColor(wash);
        g.fill(shape);
        g.setColor(color);
        g.draw(shape);
    }
}
