package components;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

public final class Ui {
    public static final Color NAVY=new Color(28,27,72), INK=new Color(31,37,54), MUTED=new Color(105,112,130), BG=new Color(246,247,251), CARD=Color.WHITE, ACCENT=new Color(83,78,196), DANGER=new Color(205,67,73);
    private Ui() {}
    public static JLabel title(String text) { JLabel l=new JLabel(text); l.setFont(new Font("SansSerif",Font.BOLD,28)); l.setForeground(INK); return l; }
    public static JLabel muted(String text) { JLabel l=new JLabel(text); l.setForeground(MUTED); l.setFont(new Font("SansSerif",Font.PLAIN,14)); return l; }
    public static JPanel header(String title,String subtitle) { JPanel p=new JPanel(); p.setOpaque(false); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS)); p.add(title(title)); p.add(Box.createVerticalStrut(4)); p.add(muted(subtitle)); return p; }
    public static JButton button(String text) { JButton b=new RoundedButton(text,14); b.setFont(new Font("SansSerif",Font.BOLD,13)); b.setPreferredSize(new Dimension(120,38)); return b; }
    public static JButton danger(String text) { JButton b=button(text); ((RoundedButton)b).setCustomColors(DANGER,DANGER.brighter(),DANGER.darker()); return b; }
    public static void styleTable(JTable t) { t.setRowHeight(38); t.setShowVerticalLines(false); t.setGridColor(new Color(231,233,240)); t.setSelectionBackground(new Color(230,228,252)); t.setSelectionForeground(INK); t.setFont(new Font("SansSerif",Font.PLAIN,13)); JTableHeader h=t.getTableHeader(); h.setFont(new Font("SansSerif",Font.BOLD,12)); h.setForeground(MUTED); h.setBackground(new Color(239,240,246)); ((DefaultTableCellRenderer)h.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT); }
    public static JPanel page() { JPanel p=new JPanel(new BorderLayout(0,20)); p.setBackground(BG); p.setBorder(BorderFactory.createEmptyBorder(28,30,28,30)); return p; }
    public static String ask(Component parent,String label) { String s=JOptionPane.showInputDialog(parent,label); return s==null?null:s.trim(); }
}
