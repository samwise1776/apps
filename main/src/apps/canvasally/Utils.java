import java.awt.Graphics2D;
import javax.swing.JFrame;

public class Utils {

    Graphics2D g2d;
    JFrame frame;
    int x;
    int y;
    int size;

    public Utils(
            Graphics2D g2d,
            JFrame frame,
            int x,
            int y,
            int size
    ) {
        this.g2d = g2d;
        this.frame = frame;
        this.x = x;
        this.y = y;
        this.size = size;
    }

    public void squareButton() {
        g2d.fillRect(
                x,
                y,
                size,
                size
        );
    }
}