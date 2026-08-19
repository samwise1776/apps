import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

/**
 * A dependency-free visual Swing app builder.
 *
 * <p>Run with: {@code javac AppManager.java && java AppManager}. Components can
 * be added and styled from the editor and the result can be exported as a
 * standalone Java source file.</p>
 */
public final class AppManager {
    private static final Color INK = new Color(25, 29, 43);
    private static final Color MUTED = new Color(100, 108, 130);
    private static final Color ACCENT = new Color(92, 84, 232);
    private static final Color SURFACE = new Color(247, 248, 252);

    private final JFrame window = new JFrame("AppManager — No-code Swing Builder");
    private final JPanel canvas = new JPanel();
    private final DefaultListModel<Node> model = new DefaultListModel<>();
    private final JList<Node> layers = new JList<>(model);
    private final Map<String, JComponent> previewById = new LinkedHashMap<>();
    private final PropertiesEditor properties = new PropertiesEditor();
    private AppSpec app = new AppSpec();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AppManager().show());
    }

    public AppManager() {
        buildEditor();
        seedExample();
    }

    private void show() {
        window.setVisible(true);
    }

    private void buildEditor() {
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setMinimumSize(new Dimension(1080, 700));
        window.setSize(1380, 850);
        window.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(SURFACE);
        root.add(toolbar(), BorderLayout.NORTH);
        root.add(workspace(), BorderLayout.CENTER);
        window.setContentPane(root);
        bindShortcuts();
    }

    private JComponent toolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 9));
        bar.setBackground(Color.WHITE);
        bar.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, new Color(220, 223, 232)),
                new EmptyBorder(0, 8, 0, 8)));
        JLabel brand = new JLabel("AppManager");
        brand.setFont(font(19, Font.BOLD));
        brand.setForeground(INK);
        bar.add(brand);
        bar.add(Box.createHorizontalStrut(12));
        bar.add(action("New", e -> newApp()));
        bar.add(action("Preview", e -> openPreview()));
        bar.add(action("Export Java", e -> exportJava()));
        bar.add(action("Theme", e -> cycleTheme()));
        JLabel tip = new JLabel("  Ctrl+E export  •  Delete removes  •  Alt+↑/↓ reorders");
        tip.setForeground(MUTED);
        bar.add(tip);
        return bar;
    }

    private JComponent workspace() {
        JSplitPane leftCenter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, paletteAndLayers(), canvasHost());
        leftCenter.setDividerLocation(255);
        leftCenter.setResizeWeight(0);
        leftCenter.setBorder(null);
        JSplitPane all = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftCenter, properties);
        all.setDividerLocation(1030);
        all.setResizeWeight(1);
        all.setBorder(null);
        return all;
    }

    private JComponent paletteAndLayers() {
        JPanel side = new JPanel(new BorderLayout(8, 8));
        side.setBackground(Color.WHITE);
        side.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel palette = new JPanel(new GridLayout(0, 2, 6, 6));
        palette.setOpaque(false);
        for (Kind kind : Kind.values()) {
            JButton add = action("+ " + kind.label, e -> addNode(kind));
            add.setToolTipText("Add " + kind.label.toLowerCase(Locale.ROOT));
            palette.add(add);
        }
        JPanel top = section("COMPONENTS", palette);
        side.add(top, BorderLayout.NORTH);

        layers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        layers.setCellRenderer(new LayerRenderer());
        layers.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) select(layers.getSelectedValue());
        });
        side.add(section("LAYERS", new JScrollPane(layers)), BorderLayout.CENTER);

        JPanel moves = new JPanel(new GridLayout(1, 3, 5, 0));
        moves.setOpaque(false);
        moves.add(action("↑", e -> move(-1)));
        moves.add(action("↓", e -> move(1)));
        moves.add(action("Delete", e -> removeSelected()));
        side.add(moves, BorderLayout.SOUTH);
        return side;
    }

    private JComponent canvasHost() {
        canvas.setLayout(new BoxLayout(canvas, BoxLayout.Y_AXIS));
        canvas.setBorder(new EmptyBorder(28, 32, 28, 32));
        JPanel stage = new JPanel(new GridBagLayout());
        stage.setBackground(new Color(226, 229, 238));
        stage.add(canvas);
        JScrollPane scroll = new JScrollPane(stage);
        scroll.setBorder(null);
        return scroll;
    }

    private JPanel section(String title, JComponent content) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        JLabel label = new JLabel(title);
        label.setFont(font(11, Font.BOLD));
        label.setForeground(MUTED);
        p.add(label, BorderLayout.NORTH);
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    private JButton action(String text, ActionListener listener) {
        JButton b = new JButton(text);
        b.setFont(font(12, Font.BOLD));
        b.setFocusPainted(false);
        b.addActionListener(listener);
        return b;
    }

    private void seedExample() {
        app.title = "My Awesome App";
        app.background = "#F7F8FC";
        addNode(new Node(Kind.HEADING, "Build something great"));
        addNode(new Node(Kind.TEXT, "Design your app visually, then export clean Java."));
        addNode(new Node(Kind.INPUT, "Your name"));
        addNode(new Node(Kind.BUTTON, "Get started"));
        layers.setSelectedIndex(0);
    }

    private void newApp() {
        if (JOptionPane.showConfirmDialog(window, "Clear the current design?", "New app",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        app = new AppSpec();
        model.clear();
        properties.edit(null);
        refreshCanvas();
    }

    private void addNode(Kind kind) {
        addNode(new Node(kind, kind.defaultText));
        layers.setSelectedIndex(model.size() - 1);
    }

    private void addNode(Node node) {
        model.addElement(node);
        app.nodes.add(node);
        refreshCanvas();
    }

    private void removeSelected() {
        int i = layers.getSelectedIndex();
        if (i < 0) return;
        app.nodes.remove(i);
        model.remove(i);
        refreshCanvas();
        if (!model.isEmpty()) layers.setSelectedIndex(Math.min(i, model.size() - 1));
        else properties.edit(null);
    }

    private void move(int delta) {
        int from = layers.getSelectedIndex(), to = from + delta;
        if (from < 0 || to < 0 || to >= model.size()) return;
        Node n = model.remove(from);
        model.add(to, n);
        app.nodes.remove(from);
        app.nodes.add(to, n);
        refreshCanvas();
        layers.setSelectedIndex(to);
    }

    private void select(Node node) {
        properties.edit(node);
        previewById.values().forEach(c -> c.setBorder(null));
        if (node != null && previewById.containsKey(node.id)) {
            previewById.get(node.id).setBorder(new LineBorder(ACCENT, 2));
        }
    }

    private void refreshCanvas() {
        canvas.removeAll();
        previewById.clear();
        canvas.setBackground(color(app.background, SURFACE));
        canvas.setPreferredSize(new Dimension(app.width, Math.max(app.height, 120 + app.nodes.size() * 65)));
        for (Node node : app.nodes) {
            JComponent component = ComponentUtils.create(node);
            component.setAlignmentX(alignment(node.align));
            component.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { layers.setSelectedValue(node, true); }
            });
            previewById.put(node.id, component);
            canvas.add(component);
            canvas.add(Box.createRigidArea(new Dimension(0, node.gap)));
        }
        canvas.revalidate();
        canvas.repaint();
        select(layers.getSelectedValue());
    }

    private void cycleTheme() {
        String[] themes = {"Light", "Midnight", "Ocean", "Forest", "Sunset"};
        String picked = (String) JOptionPane.showInputDialog(window, "Choose an app theme", "Theme",
                JOptionPane.PLAIN_MESSAGE, null, themes, themes[0]);
        if (picked == null) return;
        Theme t = Theme.named(picked);
        app.background = t.background;
        for (Node n : app.nodes) {
            n.foreground = n.kind == Kind.BUTTON ? t.buttonText : t.text;
            n.background = n.kind == Kind.BUTTON ? t.accent : "transparent";
        }
        refreshCanvas();
        properties.edit(layers.getSelectedValue());
    }

    private void openPreview() {
        JFrame preview = new JFrame(app.title);
        preview.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(color(app.background, Color.WHITE));
        content.setBorder(new EmptyBorder(30, 30, 30, 30));
        for (Node node : app.nodes) {
            content.add(ComponentUtils.create(node));
            content.add(Box.createRigidArea(new Dimension(0, node.gap)));
        }
        preview.setContentPane(new JScrollPane(content));
        preview.setSize(app.width, app.height);
        preview.setLocationRelativeTo(window);
        preview.setVisible(true);
    }

    private void exportJava() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(safeClassName(app.title) + ".java"));
        if (chooser.showSaveDialog(window) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        try {
            Files.write(file.toPath(), SourceExporter.generate(app).getBytes(StandardCharsets.UTF_8));
            JOptionPane.showMessageDialog(window, "Exported to\n" + file.getAbsolutePath(), "Export complete",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(window, ex.getMessage(), "Export failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void bindShortcuts() {
        JRootPane root = window.getRootPane();
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK), "export", this::exportJava);
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete", this::removeSelected);
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK), "up", () -> move(-1));
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK), "down", () -> move(1));
    }

    private static void bind(JRootPane root, KeyStroke key, String name, Runnable task) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
        root.getActionMap().put(name, new AbstractAction() {
            public void actionPerformed(ActionEvent e) { task.run(); }
        });
    }

    private final class PropertiesEditor extends JPanel {
        private static final long serialVersionUID = 1L;
        private final JTextField text = field();
        private final JTextField foreground = field();
        private final JTextField background = field();
        private final JSpinner size = new JSpinner(new SpinnerNumberModel(16, 8, 96, 1));
        private final JSpinner width = new JSpinner(new SpinnerNumberModel(300, 40, 1000, 10));
        private final JSpinner height = new JSpinner(new SpinnerNumberModel(44, 20, 500, 5));
        private final JSpinner gap = new JSpinner(new SpinnerNumberModel(12, 0, 100, 1));
        private final JCheckBox bold = new JCheckBox("Bold");
        private final JCheckBox enabled = new JCheckBox("Enabled", true);
        private final JComboBox<String> align = new JComboBox<>(new String[]{"Left", "Center", "Right"});
        private final JComboBox<String> border = new JComboBox<>(new String[]{"None", "Line", "Rounded", "Raised", "Lowered"});
        private transient Node editing;
        private boolean loading;

        PropertiesEditor() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Color.WHITE);
            setBorder(new EmptyBorder(16, 16, 16, 16));
            JLabel title = new JLabel("PROPERTIES & STYLE");
            title.setFont(font(11, Font.BOLD));
            title.setForeground(MUTED);
            add(title);
            add(Box.createRigidArea(new Dimension(0, 12)));
            add(row("Text", text));
            add(row("Text color", foreground));
            add(row("Background", background));
            add(row("Font size", size));
            add(row("Width", width));
            add(row("Height", height));
            add(row("Spacing", gap));
            add(row("Alignment", align));
            add(row("Border", border));
            add(bold);
            add(enabled);
            add(Box.createVerticalGlue());
            JLabel help = new JLabel("<html>Colors accept #RRGGBB or<br>transparent.</html>");
            help.setForeground(MUTED);
            add(help);
            watch(text); watch(foreground); watch(background);
            size.addChangeListener(e -> save()); width.addChangeListener(e -> save());
            height.addChangeListener(e -> save()); gap.addChangeListener(e -> save());
            bold.addActionListener(e -> save()); enabled.addActionListener(e -> save());
            align.addActionListener(e -> save()); border.addActionListener(e -> save());
            setFieldsEnabled(false);
        }

        private JPanel row(String label, JComponent input) {
            JPanel p = new JPanel(new BorderLayout(0, 4));
            p.setOpaque(false);
            p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
            JLabel l = new JLabel(label);
            l.setForeground(INK);
            p.add(l, BorderLayout.NORTH);
            p.add(input, BorderLayout.CENTER);
            p.setBorder(new EmptyBorder(0, 0, 8, 0));
            return p;
        }

        private void watch(JTextField f) {
            f.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { save(); }
                public void removeUpdate(DocumentEvent e) { save(); }
                public void changedUpdate(DocumentEvent e) { save(); }
            });
        }

        void edit(Node node) {
            editing = node;
            loading = true;
            setFieldsEnabled(node != null);
            if (node != null) {
                text.setText(node.text); foreground.setText(node.foreground); background.setText(node.background);
                size.setValue(node.fontSize); width.setValue(node.width); height.setValue(node.height);
                gap.setValue(node.gap); bold.setSelected(node.bold); enabled.setSelected(node.enabled);
                align.setSelectedItem(node.align); border.setSelectedItem(node.border);
            }
            loading = false;
        }

        private void save() {
            if (loading || editing == null) return;
            editing.text = text.getText(); editing.foreground = foreground.getText();
            editing.background = background.getText(); editing.fontSize = (Integer) size.getValue();
            editing.width = (Integer) width.getValue(); editing.height = (Integer) height.getValue();
            editing.gap = (Integer) gap.getValue(); editing.bold = bold.isSelected();
            editing.enabled = enabled.isSelected(); editing.align = (String) align.getSelectedItem();
            editing.border = (String) border.getSelectedItem();
            layers.repaint();
            refreshCanvas();
        }

        private void setFieldsEnabled(boolean value) {
            for (Component c : getComponents()) if (c instanceof JPanel) {
                for (Component child : ((JPanel) c).getComponents()) child.setEnabled(value);
            }
            bold.setEnabled(value); enabled.setEnabled(value);
        }
    }

    /** Reusable component, styling, sizing, border, and formatting utilities. */
    public static final class ComponentUtils {
        private ComponentUtils() {}

        public static JComponent create(Node n) {
            JComponent c;
            switch (n.kind) {
                case HEADING: case TEXT: c = new JLabel(n.text); break;
                case BUTTON: c = new JButton(n.text); break;
                case INPUT: JTextField f = new JTextField(); f.setToolTipText(n.text); f.setText(n.value); c = f; break;
                case TEXT_AREA: JTextArea a = new JTextArea(n.value); a.setLineWrap(true); a.setWrapStyleWord(true); c = a; break;
                case CHECKBOX: c = new JCheckBox(n.text); break;
                case RADIO: c = new JRadioButton(n.text); break;
                case COMBO: c = new JComboBox<>(split(n.text)); break;
                case SLIDER: c = new JSlider(0, 100, 50); break;
                case PROGRESS: c = new JProgressBar(0, 100); ((JProgressBar)c).setValue(65); break;
                case SEPARATOR: c = new JSeparator(); break;
                case IMAGE: c = new JLabel("🖼  " + n.text, SwingConstants.CENTER); break;
                case SPACER: c = new JPanel(); c.setPreferredSize(new Dimension(n.width, n.height)); break;
                default: c = new JLabel(n.text);
            }
            style(c, n);
            return c;
        }

        public static <T extends JComponent> T style(T c, Node n) {
            c.setFont(font(n.fontSize, n.bold ? Font.BOLD : Font.PLAIN));
            c.setForeground(color(n.foreground, INK));
            Color bg = color(n.background, null);
            c.setOpaque(bg != null);
            if (bg != null) c.setBackground(bg);
            c.setEnabled(n.enabled);
            c.setPreferredSize(new Dimension(n.width, n.height));
            c.setMaximumSize(new Dimension(n.width, n.height));
            c.setMinimumSize(new Dimension(Math.min(40, n.width), Math.min(20, n.height)));
            if (c instanceof JLabel) ((JLabel)c).setHorizontalAlignment(swingAlignment(n.align));
            if (c instanceof AbstractButton) ((AbstractButton)c).setHorizontalAlignment(swingAlignment(n.align));
            c.setBorder(makeBorder(n.border, color(n.foreground, INK)));
            return c;
        }

        public static Border makeBorder(String name, Color color) {
            if ("Line".equals(name)) return new CompoundBorder(new LineBorder(color), new EmptyBorder(6, 10, 6, 10));
            if ("Rounded".equals(name)) return new CompoundBorder(new RoundedBorder(color, 14), new EmptyBorder(6, 10, 6, 10));
            if ("Raised".equals(name)) return new BevelBorder(BevelBorder.RAISED);
            if ("Lowered".equals(name)) return new BevelBorder(BevelBorder.LOWERED);
            return new EmptyBorder(4, 4, 4, 4);
        }

        public static JPanel row(Component... items) { return panel(new FlowLayout(FlowLayout.LEFT, 8, 8), items); }
        public static JPanel column(Component... items) {
            JPanel p = panel(null); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            for (Component item : items) p.add(item); return p;
        }
        public static JPanel panel(LayoutManager layout, Component... items) {
            JPanel p = new JPanel(layout); for (Component item : items) p.add(item); return p;
        }
        public static JScrollPane scroll(Component c) { return new JScrollPane(c); }
        public static JComponent pad(JComponent c, int all) { c.setBorder(new EmptyBorder(all, all, all, all)); return c; }
        public static JComponent size(JComponent c, int w, int h) { c.setPreferredSize(new Dimension(w, h)); return c; }
        public static String[] split(String text) { return text.split("\\s*,\\s*"); }
    }

    public enum Kind {
        HEADING("Heading", "New heading"), TEXT("Text", "Some helpful text"), BUTTON("Button", "Continue"),
        INPUT("Input", "Enter text"), TEXT_AREA("Text area", "Write something..."),
        CHECKBOX("Checkbox", "I agree"), RADIO("Radio", "Option"), COMBO("Dropdown", "One, Two, Three"),
        SLIDER("Slider", "Slider"), PROGRESS("Progress", "Progress"), IMAGE("Image", "Image placeholder"),
        SEPARATOR("Divider", "Divider"), SPACER("Spacer", "Spacer");
        final String label, defaultText;
        Kind(String label, String defaultText) { this.label = label; this.defaultText = defaultText; }
    }

    public static final class Node {
        final String id = UUID.randomUUID().toString();
        final Kind kind;
        String text, value = "", foreground = "#191D2B", background = "transparent";
        String align = "Left", border = "None";
        int fontSize = 16, width = 360, height = 44, gap = 12;
        boolean bold, enabled = true;
        Node(Kind kind, String text) {
            this.kind = kind; this.text = text;
            bold = kind == Kind.HEADING;
            fontSize = kind == Kind.HEADING ? 30 : 16;
            if (kind == Kind.BUTTON) { background = "#5C54E8"; foreground = "#FFFFFF"; border = "Rounded"; }
            if (kind == Kind.TEXT_AREA || kind == Kind.IMAGE) height = 110;
            if (kind == Kind.SEPARATOR) height = 10;
        }
        public String toString() { return kind.label + "  ·  " + shorten(text, 22); }
    }

    public static final class AppSpec {
        String title = "Untitled App", background = "#F7F8FC";
        int width = 720, height = 620;
        final List<Node> nodes = new ArrayList<>();
    }

    private static final class Theme {
        final String background, text, accent, buttonText;
        Theme(String bg, String text, String accent) { this.background = bg; this.text = text; this.accent = accent; buttonText = "#FFFFFF"; }
        static Theme named(String name) {
            if ("Midnight".equals(name)) return new Theme("#101425", "#F2F4FF", "#7C6FFF");
            if ("Ocean".equals(name)) return new Theme("#EFFAFF", "#123047", "#087EA4");
            if ("Forest".equals(name)) return new Theme("#F1F8F3", "#173B27", "#278653");
            if ("Sunset".equals(name)) return new Theme("#FFF6F0", "#4A2622", "#E85D45");
            return new Theme("#F7F8FC", "#191D2B", "#5C54E8");
        }
    }

    private static final class RoundedBorder extends AbstractBorder {
        private static final long serialVersionUID = 1L;
        private final Color color; private final int radius;
        RoundedBorder(Color color, int radius) { this.color = color; this.radius = radius; }
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color); g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius); g2.dispose();
        }
    }

    private static final class LayerRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
            JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
            l.setBorder(new EmptyBorder(9, 8, 9, 8)); l.setFont(font(12, Font.PLAIN)); return l;
        }
    }

    /** Converts the visual model to a standalone, dependency-free Swing program. */
    public static final class SourceExporter {
        private SourceExporter() {}
        public static String generate(AppSpec app) {
            String cls = safeClassName(app.title);
            StringBuilder s = new StringBuilder();
            s.append("import javax.swing.*;\nimport javax.swing.border.*;\nimport java.awt.*;\n\n")
             .append("public class ").append(cls).append(" {\n")
             .append("  public static void main(String[] args) { SwingUtilities.invokeLater(").append(cls).append("::show); }\n")
             .append("  private static void show() {\n    JFrame frame = new JFrame(\"").append(escape(app.title)).append("\");\n")
             .append("    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);\n")
             .append("    JPanel root = new JPanel(); root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));\n")
             .append("    root.setBackground(Color.decode(\"").append(app.background).append("\")); root.setBorder(new EmptyBorder(30,30,30,30));\n");
            int i = 0;
            for (Node n : app.nodes) {
                String v = "c" + i++;
                appendComponent(s, n, v);
                s.append("    ").append(v).append(".setFont(new Font(Font.SANS_SERIF,")
                 .append(n.bold ? "Font.BOLD" : "Font.PLAIN").append(",").append(n.fontSize).append("));\n")
                 .append("    ").append(v).append(".setForeground(Color.decode(\"").append(n.foreground).append("\"));\n");
                if (!"transparent".equalsIgnoreCase(n.background))
                    s.append("    ").append(v).append(".setBackground(Color.decode(\"").append(n.background).append("\")); ").append(v).append(".setOpaque(true);\n");
                s.append("    ").append(v).append(".setMaximumSize(new Dimension(").append(n.width).append(",").append(n.height).append("));\n")
                 .append("    root.add(").append(v).append("); root.add(Box.createRigidArea(new Dimension(0,").append(n.gap).append(")));\n");
            }
            s.append("    frame.setContentPane(new JScrollPane(root)); frame.setSize(").append(app.width).append(",").append(app.height)
             .append("); frame.setLocationRelativeTo(null); frame.setVisible(true);\n  }\n}\n");
            return s.toString();
        }

        private static void appendComponent(StringBuilder s, Node n, String v) {
            String text = escape(n.text);
            switch (n.kind) {
                case BUTTON: s.append("    JButton ").append(v).append(" = new JButton(\"").append(text).append("\");\n"); break;
                case INPUT: s.append("    JTextField ").append(v).append(" = new JTextField(\"").append(escape(n.value)).append("\");\n"); break;
                case TEXT_AREA: s.append("    JTextArea ").append(v).append(" = new JTextArea(\"").append(escape(n.value)).append("\"); ").append(v).append(".setLineWrap(true);\n"); break;
                case CHECKBOX: s.append("    JCheckBox ").append(v).append(" = new JCheckBox(\"").append(text).append("\");\n"); break;
                case RADIO: s.append("    JRadioButton ").append(v).append(" = new JRadioButton(\"").append(text).append("\");\n"); break;
                case SLIDER: s.append("    JSlider ").append(v).append(" = new JSlider(0,100,50);\n"); break;
                case PROGRESS: s.append("    JProgressBar ").append(v).append(" = new JProgressBar(0,100); ").append(v).append(".setValue(65);\n"); break;
                case SEPARATOR: s.append("    JSeparator ").append(v).append(" = new JSeparator();\n"); break;
                case COMBO: s.append("    JComboBox<String> ").append(v).append(" = new JComboBox<>(new String[]{\"").append(text.replace(", ", "\",\"")).append("\"});\n"); break;
                default: s.append("    JLabel ").append(v).append(" = new JLabel(\"").append(text).append("\");\n");
            }
        }
    }

    private static JTextField field() { return new JTextField(); }
    private static Font font(int size, int style) { return new Font(Font.SANS_SERIF, style, size); }
    private static Color color(String value, Color fallback) {
        if (value == null || value.trim().isEmpty() || "transparent".equalsIgnoreCase(value.trim())) return fallback;
        try { return Color.decode(value.trim()); } catch (NumberFormatException ex) { return fallback; }
    }
    private static float alignment(String a) { return "Center".equals(a) ? .5f : "Right".equals(a) ? 1f : 0f; }
    private static int swingAlignment(String a) { return "Center".equals(a) ? SwingConstants.CENTER : "Right".equals(a) ? SwingConstants.RIGHT : SwingConstants.LEFT; }
    private static String shorten(String s, int n) { return s.length() <= n ? s : s.substring(0, n - 1) + "…"; }
    private static String safeClassName(String s) {
        String cleaned = s.replaceAll("[^A-Za-z0-9_$]", "");
        if (cleaned.isEmpty()) cleaned = "GeneratedApp";
        if (!Character.isJavaIdentifierStart(cleaned.charAt(0))) cleaned = "App" + cleaned;
        return cleaned;
    }
    private static String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", ""); }
}
