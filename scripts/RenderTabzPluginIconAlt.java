import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/** Alternative TABZ icon: Z-shaped split tab ribbon + sub-tab dots. */
final class RenderTabzPluginIconAlt {
    public static void main(String[] args) throws Exception {
        Path out = args.length > 0 ? Path.of(args[0]) : Path.of("assets/tabz-plugin-icon-alt.png");
        int size = args.length > 1 ? Integer.parseInt(args[1]) : 512;
        Files.createDirectories(out.getParent());
        write(out, size);
    }

    private static void write(Path path, int size) throws Exception {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float s = size / 512f;
        g.scale(s, s);

        Path2D z = new Path2D.Float();
        z.moveTo(96, 128);
        z.lineTo(320, 128);
        z.lineTo(320, 168);
        z.lineTo(168, 168);
        z.lineTo(416, 344);
        z.lineTo(416, 384);
        z.lineTo(192, 384);
        z.lineTo(192, 344);
        z.lineTo(344, 344);
        z.closePath();

        g.setPaint(new GradientPaint(96, 128, new Color(0x6BA8FF), 320, 168, new Color(0x3A7FE8)));
        g.fill(z);

        g.setColor(new Color(0, 0, 0, 30));
        g.setStroke(new BasicStroke(6f));
        g.draw(z);

        g.setColor(new Color(0x22D3EE));
        g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(168, 168, 344, 344);

        int[] chipX = {112, 132, 152};
        for (int i = 0; i < chipX.length; i++) {
            int a = i == 0 ? 230 : (i == 1 ? 160 : 100);
            g.setColor(new Color(255, 255, 255, a));
            g.fill(new RoundRectangle2D.Float(chipX[i], 142, 14, 8, 4, 4));
        }

        g.dispose();
        ImageIO.write(image, "png", path.toFile());
        System.out.println("Wrote " + path.toAbsolutePath());
    }
}
