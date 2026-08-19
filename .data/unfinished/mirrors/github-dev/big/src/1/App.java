
import java.awt.*;
import javax.swing.*;

/**
 * Starts a midnight-themed text statistics utility.
 */
public class App {
    private static final Color MIDNIGHT_BLUE = new Color(8, 18, 38);
    private static final Color SOFT_WHITE = new Color(235, 241, 255);

    public static void main(String[] args) {
        // Swing windows should be created on the event dispatch thread.
        SwingUtilities.invokeLater(App::createWindow);
    }

    private static void createWindow() {
        JFrame frame = new JFrame("Midnight Text Utility");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(640, 420));

        // This panel supplies the midnight-dark-blue background and spacing.
        JPanel content = new JPanel(new BorderLayout(20, 20));
        content.setBackground(MIDNIGHT_BLUE);
        content.setBorder(BorderFactory.createEmptyBorder(48, 48, 48, 48));

        JLabel heading = new JLabel("Text Statistics", SwingConstants.CENTER);
        heading.setForeground(SOFT_WHITE);
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));

        JTextArea textArea = new JTextArea(10, 36);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        textArea.setForeground(SOFT_WHITE);
        textArea.setBackground(new Color(17, 32, 60));
        textArea.setCaretColor(SOFT_WHITE);
        textArea.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(45, 67, 105)));

        JLabel result = new JLabel("Words: 0   Characters: 0   Lines: 0", SwingConstants.CENTER);
        result.setForeground(new Color(160, 180, 215));
        result.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));

        JButton button = new JButton("Analyze Text");
        button.setPreferredSize(new Dimension(170, 48));
        button.addActionListener(event -> {
            // Count words separated by whitespace, along with characters and lines.
            String text = textArea.getText();
            int words = text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
            int characters = text.length();
            int lines = text.isEmpty() ? 0 : text.split("\\R", -1).length;
            result.setText("Words: " + words + "   Characters: " + characters + "   Lines: " + lines);
        });

        JPanel buttonRow = new JPanel();
        buttonRow.setOpaque(false);
        buttonRow.add(result);
        buttonRow.add(button);

        content.add(heading, BorderLayout.NORTH);
        content.add(scrollPane, BorderLayout.CENTER);
        content.add(buttonRow, BorderLayout.SOUTH);

        frame.setContentPane(content);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
