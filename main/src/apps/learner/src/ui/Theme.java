package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.UIManager;

public final class Theme {
  public static final Color BACKGROUND = new Color(35, 34, 90);
  public static final Color PANEL = new Color(48, 47, 110);
  public static final Color PANEL_LIGHT = new Color(65, 64, 135);
  public static final Color TEXT = new Color(240, 240, 245);
  public static final Color MUTED = new Color(190, 192, 215);
  public static final Color ACCENT = new Color(255, 203, 71);
  public static final Color SUCCESS = new Color(70, 180, 110);
  public static final Color ERROR = new Color(225, 100, 100);

  private Theme() {}

  public static void install(int fontSize) {
    Font normal = new Font("SansSerif", Font.PLAIN, fontSize);
    for (Object key : UIManager.getDefaults().keySet())
      if (key.toString().endsWith(".font")) UIManager.put(key, normal);
    UIManager.put("Label.foreground", TEXT);
    UIManager.put("Panel.background", BACKGROUND);
    UIManager.put("Button.foreground", TEXT);
    UIManager.put("Button.background", PANEL_LIGHT);
    UIManager.put("ProgressBar.foreground", ACCENT);
    UIManager.put("ProgressBar.background", PANEL);
  }

  public static JPanel page() {
    JPanel panel = new JPanel(new BorderLayout(14, 14));
    panel.setBackground(BACKGROUND);
    panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    return panel;
  }

  public static JButton button(String text) {
    JButton button = new JButton(text);
    button.setFocusPainted(false);
    button.setForeground(TEXT);
    button.setBackground(PANEL_LIGHT);
    button.setFont(button.getFont().deriveFont(Font.BOLD));
    button.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(95, 93, 170), 2),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));
    return button;
  }
}
