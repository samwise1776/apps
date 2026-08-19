import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * App
 */
public class App {
    public static void main(String[] args) {
        JFrame frame = new JFrame("AppForge");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 900);
        frame.setLocationRelativeTo(null);

        // Main background panel with layout padding
        JPanel bg = new JPanel(new GridBagLayout());
        bg.setBackground(new Color(34, 35, 100));
        frame.setContentPane(bg);

        // Stylish Custom Card Panel (Rounded corners via paintComponent)
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(500, 450));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(40, 40, 40, 40));

        // Card Header
        JLabel header = new JLabel("AppForge");
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 36));
        header.setForeground(new Color(34, 35, 100));
        header.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Card Subtitle/Description
        JLabel desc = new JLabel("\"Nothing impossible it's just hard\"");
        desc.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 16));
        desc.setForeground(Color.GRAY);
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Action Button - Made Rounded
        JButton button = new JButton("Get Started") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Change color if hovered/pressed (optional UI polish)
                if (getModel().isArmed()) {
                    g2.setColor(new Color(24, 25, 80));
                } else {
                    g2.setColor(getBackground());
                }
                
                // Draw rounded rectangle background (arc diameter 20)
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                
                // Super call handles rendering the centered text labels
                super.paintComponent(g);
            }
        };
        
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        button.setBackground(new Color(34, 35, 100));
        button.setForeground(Color.WHITE);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Critical configuration rules for custom shaped buttons
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
        
        // Match alignment with layout padding constraints
        button.setMaximumSize(new Dimension(200, 45));

        // Assemble the UI components
        card.add(header);
        card.add(Box.createRigidArea(new Dimension(0, 15)));
        card.add(desc);
        card.add(Box.createVerticalGlue());
        card.add(button);

        button.addActionListener(e -> {
            start(frame, bg);
        });

        bg.add(card);
        frame.setVisible(true);
    }
    public static void start(JFrame frame, JPanel bg) {
        bg.removeAll();

        

        bg.revalidate();
        bg.repaint();
    }
}