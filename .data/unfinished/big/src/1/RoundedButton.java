import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JButton;

/**
 * A small reusable button with a rounded background.
 */
public class RoundedButton extends JButton {
    private static final int ARC_SIZE = 24;

    public RoundedButton(String text) {
        super(text);

        // The component paints its own rounded background.
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setForeground(Color.WHITE);
        setBackground(new Color(62, 104, 220));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D copy = (Graphics2D) graphics.create();
        copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Use a lighter shade while the button is pressed.
        Color fill = getModel().isPressed() ? getBackground().brighter() : getBackground();
        copy.setColor(fill);
        copy.fillRoundRect(0, 0, getWidth(), getHeight(), ARC_SIZE, ARC_SIZE);
        copy.dispose();

        super.paintComponent(graphics);
    }
}
