package components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

@SuppressWarnings({"serial", "this-escape"})
public class RoundedButton extends JButton {
    private int cornerRadius;
    private Color normalBackground;
    private Color hoverBackground;
    private Color activeBackground;

    // Public constructor allowing custom text and corner roundness adjustment
    public RoundedButton(String text, int cornerRadius) {
        super(text);
        this.cornerRadius = cornerRadius;
        
        // Critical: Tell Swing to let us draw the custom shape background manually
        setOpaque(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);

        // Define default button state themes to match your layout colors
        normalBackground = new Color(68, 81, 158);  // Clean Slate Blue
        hoverBackground = new Color(85, 99, 185);   // Lighter Highlight Blue
        activeBackground = new Color(50, 60, 125);  // Deeper Clicked Blue
        setForeground(new Color(240, 240, 245));    // Bright Off-White text

        // Add standard interactive hover/click response event triggers
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(hoverBackground);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(normalBackground);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                setBackground(activeBackground);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (getMousePosition() != null) {
                    setBackground(hoverBackground);
                } else {
                    setBackground(normalBackground);
                }
            }
        });

        // Set the initialization base color
        setBackground(normalBackground);
    }

    // Secondary public constructor with a default safe roundness setting
    public RoundedButton(String text) {
        this(text, 20); // Defaults to a 20px arc configuration
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        
        // Enable high-fidelity anti-aliasing so edges don't appear jagged or pixelated
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Render the rounded background canvas bounding box
        g2d.setColor(getBackground());
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);

        g2d.dispose();

        // Let the master superclass place the label text and graphics squarely on top
        super.paintComponent(g);
    }

    // Public setter methods to allow on-the-fly skin color adjustments
    public void setCustomColors(Color normal, Color hover, Color active) {
        this.normalBackground = normal;
        this.hoverBackground = hover;
        this.activeBackground = active;
        setBackground(normal);
    }
}
