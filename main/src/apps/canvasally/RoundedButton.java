import javax.swing.*;
import java.awt.*;

public class RoundedButton extends JButton {

    private int radius = 18;

    public RoundedButton(String text) {

        super(text);

        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);

        setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );
    }

    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2d =
                (Graphics2D) g.create();

        g2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        Color color =
                getBackground();

        if (getModel().isPressed()) {

            color =
                    color.darker();

        } else if (getModel().isRollover()) {

            color =
                    color.brighter();
        }

        g2d.setColor(
                color
        );

        g2d.fillRoundRect(
                0,
                0,
                getWidth(),
                getHeight(),
                radius,
                radius
        );

        g2d.dispose();

        super.paintComponent(g);
    }

    public void setRadius(int radius) {

        this.radius =
                radius;

        repaint();
    }

    public int getRadius() {

        return radius;
    }
}