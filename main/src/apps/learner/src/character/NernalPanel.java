package character;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;
import javax.swing.Timer;
import ui.Theme;

public final class NernalPanel extends JPanel {
  private static final long serialVersionUID = 1L;
  private NernalMood mood = NernalMood.NORMAL;
  private boolean animation = true;
  private double phase;
  private final Timer timer;

  public NernalPanel() {
    setOpaque(false);
    timer =
        new Timer(
            40,
            e -> {
              if (animation) {
                phase += .08;
                repaint();
              }
            });
    timer.start();
  }

  public void setMood(NernalMood mood) {
    this.mood = mood;
    repaint();
  }

  public void setAnimationEnabled(boolean enabled) {
    animation = enabled;
    if (!enabled) repaint();
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    super.paintComponent(graphics);
    Graphics2D g = (Graphics2D) graphics.create();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    int cx = getWidth() / 2;
    int cy = getHeight() / 2 + (animation ? (int) (Math.sin(phase) * 6) : 0);
    double tilt =
        switch (mood) {
          case HAPPY -> -.08;
          case THINKING -> .1;
          case CELEBRATING -> Math.sin(phase) * .12;
          case ENCOURAGING -> -.04;
          case SURPRISED -> .05;
          default -> 0;
        };
    g.translate(cx, cy);
    g.rotate(tilt);
    g.setColor(new Color(50, 58, 115));
    g.fillRoundRect(-66, -8, 42, 23, 16, 16);
    g.fillRoundRect(24, -8, 42, 23, 16, 16);
    g.fillRoundRect(-45, -78, 90, 156, 90, 90);
    g.setColor(new Color(68, 81, 158));
    g.fillRoundRect(-41, -74, 82, 148, 82, 82);
    int blink = animation && ((int) phase % 55 == 0) ? 3 : 23;
    g.setColor(Color.WHITE);
    g.fillOval(-26, -38, 20, blink);
    g.fillOval(6, -38, 20, blink);
    if (blink > 3) {
      g.setColor(new Color(30, 35, 70));
      g.fillOval(-21, -32, 11, 15);
      g.fillOval(11, -32, 11, 15);
    }
    g.setColor(new Color(30, 35, 70));
    g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    if (mood == NernalMood.SURPRISED) g.drawOval(-8, -5, 16, 18);
    else if (mood == NernalMood.THINKING) g.drawLine(-9, 7, 10, 4);
    else g.drawArc(-14, -12, 28, 22, 180, 180);
    int orbY = -125 + (animation ? (int) (Math.sin(phase * 1.4) * 5) : 0);
    g.setColor(new Color(255, 225, 120, 65));
    g.fillOval(-27, orbY - 7, 54, 54);
    g.setColor(Theme.ACCENT);
    g.fillOval(-19, orbY, 38, 38);
    g.setColor(Color.WHITE);
    g.fillOval(-9, orbY + 4, 8, 8);
    if (mood == NernalMood.CELEBRATING) {
      g.setColor(Theme.ACCENT);
      for (int i = -2; i <= 2; i++) g.fillOval(i * 34 - 3, -95 - Math.abs(i) * 8, 7, 7);
    }
    g.dispose();
  }
}
