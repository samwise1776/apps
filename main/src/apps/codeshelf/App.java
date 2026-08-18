import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import javax.swing.tree.*;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import java.util.stream.*;

/**
 * CodeShelf — local code snippet manager for Datacenter.
 * Single-file Java Swing application. No external dependencies.
 *
 * Java 11+ recommended.
 */
public class App {

    // =====================================================================
    // CONSTANTS
    // =====================================================================

    private static final String APP_NAME = "CodeShelf";
    private static final String VERSION = "1.0.0";

    private static final Color BG_DARK = new Color(26, 26, 46);
    private static final Color BG_ACCENT = new Color(15, 52, 96);
    private static final Color TEXT_PRIMARY = new Color(224, 224, 224);
    private static final Color TEXT_MUTED = new Color(140, 140, 160);
    private static final Color BTN_DARK = new Color(22, 33, 62);
    private static final Color BTN_HOVER = new Color(30, 50, 90);
    private static final Color PANEL_BG = new Color(22, 22, 40);
    private static final Color PANEL_BORDER = new Color(40, 40, 70);
    private static final Color EDITOR_BG = new Color(18, 18, 34);
    private static final Color SELECTION_BG = new Color(15, 52, 96, 120);
    private static final Color STAR_COLOR = new Color(255, 200, 50);
    private static final Color ACCENT = new Color(15, 52, 96);
    private static final Color DANGER = new Color(180, 50, 50);
    private static final Color SUCCESS = new Color(50, 180, 80);

    private static final String[] SUPPORTED_LANGUAGES = {
        "Java", "C#", "Python", "Bash", "JavaScript", "HTML", "CSS", "Velice", "Vexa"
    };

    private static final String[] BUILTIN_TAGS = {
        "utility", "api", "algorithm", "ui", "database", "network", "security",
        "testing", "config", "template", "snippet", "example"
    };

    // =====================================================================
    // DATA MODEL
    // =====================================================================

    static class Snippet implements Comparable<Snippet> {
        String id;
        String title;
        String content;
        String language;
        String folder;
        String tags;
        boolean favorite;
        String createdAt;
        String modifiedAt;

        Snippet() {
            this.id = UUID.randomUUID().toString();
            this.title = "Untitled";
            this.content = "";
            this.language = "Java";
            this.folder = "General";
            this.tags = "";
            this.favorite = false;
            String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            this.createdAt = now;
            this.modifiedAt = now;
        }

        Snippet copy() {
            Snippet s = new Snippet();
            s.id = this.id;
            s.title = this.title;
            s.content = this.content;
            s.language = this.language;
            s.folder = this.folder;
            s.tags = this.tags;
            s.favorite = this.favorite;
            s.createdAt = this.createdAt;
            s.modifiedAt = this.modifiedAt;
            return s;
        }

        void touch() {
            this.modifiedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        List<String> tagList() {
            if (tags == null || tags.isBlank()) return Collections.emptyList();
            return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toList());
        }

        @Override
        public int compareTo(Snippet o) {
            return this.title.compareToIgnoreCase(o.title);
        }
    }

    static class AppSettings {
        int fontSize = 14;
        int tabSize = 4;
        String themeVariant = "Deep Navy";

        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("fontSize", fontSize);
            m.put("tabSize", tabSize);
            m.put("themeVariant", themeVariant);
            return m;
        }

        void fromMap(Map<String, Object> m) {
            if (m.containsKey("fontSize")) fontSize = ((Number) m.get("fontSize")).intValue();
            if (m.containsKey("tabSize")) tabSize = ((Number) m.get("tabSize")).intValue();
            if (m.containsKey("themeVariant")) themeVariant = (String) m.get("themeVariant");
        }
    }

    // =====================================================================
    // JSON SERIALIZER (minimal, dependency-free)
    // =====================================================================

    static class MiniJSON {

        // ---- Objects / Arrays as maps / lists ----

        static Object parse(String json) {
            return new Parser(json).parseValue();
        }

        static String stringify(Object obj) {
            StringBuilder sb = new StringBuilder();
            writeValue(sb, obj, 0);
            return sb.toString();
        }

        static String prettyPrint(Object obj) {
            StringBuilder sb = new StringBuilder();
            writeValue(sb, obj, 0);
            return sb.toString();
        }

        private static void writeValue(StringBuilder sb, Object v, int depth) {
            if (v == null) {
                sb.append("null");
            } else if (v instanceof Map) {
                writeMap(sb, (Map<?, ?>) v, depth);
            } else if (v instanceof List) {
                writeList(sb, (List<?>) v, depth);
            } else if (v instanceof Boolean || v instanceof Number) {
                sb.append(v);
            } else {
                sb.append('"').append(escape(String.valueOf(v))).append('"');
            }
        }

        private static void writeMap(StringBuilder sb, Map<?, ?> map, int d) {
            if (map.isEmpty()) { sb.append("{}"); return; }
            sb.append("{\n");
            List<?> keys = new ArrayList<>(map.keySet());
            for (int i = 0; i < keys.size(); i++) {
                sb.append("  ".repeat(d + 1));
                sb.append('"').append(escape(String.valueOf(keys.get(i)))).append("\": ");
                writeValue(sb, map.get(keys.get(i)), d + 1);
                if (i < keys.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ".repeat(d)).append("}");
        }

        private static void writeList(StringBuilder sb, List<?> list, int d) {
            if (list.isEmpty()) { sb.append("[]"); return; }
            sb.append("[\n");
            for (int i = 0; i < list.size(); i++) {
                sb.append("  ".repeat(d + 1));
                writeValue(sb, list.get(i), d + 1);
                if (i < list.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ".repeat(d)).append("]");
        }

        static String escape(String s) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '"': sb.append("\\\""); break;
                    case '\\': sb.append("\\\\"); break;
                    case '\n': sb.append("\\n"); break;
                    case '\r': sb.append("\\r"); break;
                    case '\t': sb.append("\\t"); break;
                    default: sb.append(c);
                }
            }
            return sb.toString();
        }

        static String unescape(String s) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '\\' && i + 1 < s.length()) {
                    char next = s.charAt(++i);
                    switch (next) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u': {
                            String hex = s.substring(i + 1, i + 5);
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                            break;
                        }
                        default: sb.append(next);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        // ---- Minimal recursive-descent parser ----

        private static class Parser {
            private final String json;
            private int pos;

            Parser(String json) { this.json = json; this.pos = 0; }

            Object parseValue() { skipWS(); return readValue(); }

            private Object readValue() {
                skipWS();
                if (pos >= json.length()) return null;
                char c = json.charAt(pos);
                if (c == '"') return readString();
                if (c == '{') return readObject();
                if (c == '[') return readArray();
                if (c == 't' || c == 'f') return readBool();
                if (c == 'n') { pos += 4; return null; }
                return readNumber();
            }

            private String readString() {
                pos++; // opening quote
                StringBuilder sb = new StringBuilder();
                while (pos < json.length() && json.charAt(pos) != '"') {
                    if (json.charAt(pos) == '\\') {
                        sb.append(json.charAt(pos));
                        sb.append(json.charAt(pos + 1));
                        pos += 2;
                    } else {
                        sb.append(json.charAt(pos));
                        pos++;
                    }
                }
                pos++; // closing quote
                return unescape(sb.toString());
            }

            private Map<String, Object> readObject() {
                pos++; // '{'
                Map<String, Object> map = new LinkedHashMap<>();
                skipWS();
                if (pos < json.length() && json.charAt(pos) == '}') { pos++; return map; }
                while (pos < json.length()) {
                    skipWS();
                    String key = readString();
                    skipWS();
                    pos++; // ':'
                    Object val = readValue();
                    map.put(key, val);
                    skipWS();
                    if (pos < json.length() && json.charAt(pos) == ',') { pos++; continue; }
                    break;
                }
                skipWS();
                if (pos < json.length() && json.charAt(pos) == '}') pos++;
                return map;
            }

            private List<Object> readArray() {
                pos++; // '['
                List<Object> list = new ArrayList<>();
                skipWS();
                if (pos < json.length() && json.charAt(pos) == ']') { pos++; return list; }
                while (pos < json.length()) {
                    list.add(readValue());
                    skipWS();
                    if (pos < json.length() && json.charAt(pos) == ',') { pos++; continue; }
                    break;
                }
                skipWS();
                if (pos < json.length() && json.charAt(pos) == ']') pos++;
                return list;
            }

            private Number readNumber() {
                int start = pos;
                if (pos < json.length() && (json.charAt(pos) == '-' || json.charAt(pos) == '+')) pos++;
                while (pos < json.length() && (Character.isDigit(json.charAt(pos)) || json.charAt(pos) == '.')) pos++;
                String num = json.substring(start, pos);
                if (num.contains(".")) return Double.parseDouble(num);
                long v = Long.parseLong(num);
                if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE) return (int) v;
                return v;
            }

            private Boolean readBool() {
                if (json.startsWith("true", pos)) { pos += 4; return true; }
                pos += 5; return false;
            }

            private void skipWS() {
                while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;
            }
        }
    }

    // =====================================================================
    // STORAGE
    // =====================================================================

    private static Path dataDir() {
        String home = System.getProperty("user.home");
        return Path.of(home, ".datadocs", "codeshelf");
    }

    private static Path snippetsPath() { return dataDir().resolve("snippets.json"); }
    private static Path settingsPath() { return dataDir().resolve("settings.json"); }

    private static void ensureDataDir() {
        try { Files.createDirectories(dataDir()); }
        catch (IOException e) { e.printStackTrace(); }
    }

    // =====================================================================
    // FIELDS
    // =====================================================================

    private final List<Snippet> snippets = new ArrayList<>();
    private final AppSettings settings = new AppSettings();
    private final List<String> folders = new ArrayList<>();

    private JFrame frame;
    private JTree folderTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    private JList<String> snippetList;
    private DefaultListModel<String> listModel;
    private JTextArea editorArea;
    private JTextArea highlightOverlay;
    private JTextField searchField;
    private JTextField tagField;
    private JTextField titleField;
    private JComboBox<String> langCombo;
    private JLabel statusLabel;
    private JLabel copyFeedbackLabel;
    private JButton starBtn;
    private JPopupMenu snippetPopup;

    private String activeFilter = "All";
    private String activeFolder = "All Snippets";
    private int selectedIndex = -1;
    private boolean suppressEvents = false;
    private javax.swing.Timer saveTimer;
    private javax.swing.Timer copyFeedbackTimer;

    // =====================================================================
    // MAIN
    // =====================================================================

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new App().start());
    }

    // =====================================================================
    // STARTUP
    // =====================================================================

    private void start() {
        ensureDataDir();
        loadSettings();
        loadSnippets();
        if (snippets.isEmpty()) populateExamples();
        buildUI();
        refreshFolders();
        refreshSnippetList();
        if (!snippets.isEmpty()) selectSnippet(0);
        frame.setVisible(true);
    }

    // =====================================================================
    // UI CONSTRUCTION
    // =====================================================================

    private void buildUI() {
        frame = new JFrame(APP_NAME + " v" + VERSION);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(1400, 860);
        frame.setMinimumSize(new Dimension(900, 600));
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(BG_DARK);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { saveSnippets(); saveSettings(); System.exit(0); }
        });

        frame.setJMenuBar(buildMenuBar());

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG_DARK);

        JSplitPane splitOuter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitOuter.setBorder(null);
        splitOuter.setDividerSize(4);
        splitOuter.setContinuousLayout(true);

        splitOuter.setLeftComponent(buildLeftPanel());
        splitOuter.setRightComponent(buildRightSplit());
        splitOuter.setDividerLocation(220);

        root.add(splitOuter, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(new Color(16, 16, 30));
        statusPanel.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, PANEL_BORDER),
            new EmptyBorder(4, 10, 4, 10)));
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        statusLabel.setForeground(TEXT_MUTED);
        statusPanel.add(statusLabel, BorderLayout.WEST);

        copyFeedbackLabel = new JLabel(" ");
        copyFeedbackLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        copyFeedbackLabel.setForeground(SUCCESS);
        statusPanel.add(copyFeedbackLabel, BorderLayout.EAST);

        root.add(statusPanel, BorderLayout.SOUTH);
        frame.setContentPane(root);

        registerKeyboardShortcuts();
    }

    // ---- Menu Bar ----

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        bar.setBackground(PANEL_BG);
        bar.setBorder(new MatteBorder(0, 0, 1, 0, PANEL_BORDER));

        JMenu fileMenu = buildMenu("File");
        fileMenu.add(menuItem("New Snippet", KeyEvent.VK_N, e -> newSnippet()));
        fileMenu.add(menuItem("Delete Snippet", KeyEvent.VK_D, e -> deleteSnippet()));
        fileMenu.addSeparator();
        fileMenu.add(menuItem("Import JSON...", KeyEvent.VK_I, e -> importSnippets()));
        fileMenu.add(menuItem("Export JSON...", KeyEvent.VK_E, e -> exportSnippets()));
        fileMenu.addSeparator();
        fileMenu.add(menuItem("Save", KeyEvent.VK_S, e -> { saveSnippets(); status("Saved."); }));
        fileMenu.addSeparator();
        fileMenu.add(menuItem("Exit", -1, e -> { saveSnippets(); saveSettings(); System.exit(0); }));

        JMenu editMenu = buildMenu("Edit");
        editMenu.add(menuItem("Copy to Clipboard", KeyEvent.VK_C, e -> copyToClipboard()));
        editMenu.add(menuItem("Find...", KeyEvent.VK_F, e -> searchField.requestFocusInWindow()));

        JMenu viewMenu = buildMenu("View");
        ButtonGroup themeGroup = new ButtonGroup();
        for (String v : new String[]{"Deep Navy", "Midnight Purple", "Charcoal"}) {
            JRadioButtonMenuItem m = new JRadioButtonMenuItem(v);
            m.setSelected(v.equals(settings.themeVariant));
            m.addActionListener(e -> { settings.themeVariant = v; saveSettings(); status("Theme: " + v); });
            themeGroup.add(m);
            viewMenu.add(m);
        }
        viewMenu.addSeparator();
        viewMenu.add(menuItem("Increase Font Size", -1, e -> { settings.fontSize = Math.min(32, settings.fontSize + 1); applyFontSize(); }));
        viewMenu.add(menuItem("Decrease Font Size", -1, e -> { settings.fontSize = Math.max(8, settings.fontSize - 1); applyFontSize(); }));

        JMenu helpMenu = buildMenu("Help");
        helpMenu.add(menuItem("About " + APP_NAME, -1, e -> showAbout()));

        bar.add(fileMenu);
        bar.add(editMenu);
        bar.add(viewMenu);
        bar.add(helpMenu);
        return bar;
    }

    private JMenu buildMenu(String label) {
        JMenu m = new JMenu(label);
        m.setForeground(TEXT_PRIMARY);
        m.setFont(new Font("SansSerif", Font.PLAIN, 13));
        return m;
    }

    private JMenuItem menuItem(String label, int mnemonic, ActionListener al) {
        JMenuItem item = new JMenuItem(label);
        item.setForeground(TEXT_PRIMARY);
        item.setBackground(PANEL_BG);
        item.setFont(new Font("SansSerif", Font.PLAIN, 13));
        if (mnemonic >= 0) item.setAccelerator(KeyStroke.getKeyStroke(mnemonic, InputEvent.CTRL_DOWN_MASK));
        item.addActionListener(al);
        return item;
    }

    // ---- Left Panel (folder tree + buttons) ----

    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(PANEL_BG);
        panel.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 0, 1, PANEL_BORDER),
            new EmptyBorder(8, 8, 8, 8)));

        JLabel logo = new JLabel("  " + APP_NAME);
        logo.setFont(new Font("SansSerif", Font.BOLD, 16));
        logo.setForeground(TEXT_PRIMARY);
        logo.setBorder(new EmptyBorder(0, 0, 8, 0));
        panel.add(logo, BorderLayout.NORTH);

        rootNode = new DefaultMutableTreeNode("Root");
        treeModel = new DefaultTreeModel(rootNode);
        folderTree = new JTree(treeModel);
        folderTree.setBackground(PANEL_BG);
        folderTree.setForeground(TEXT_PRIMARY);
        folderTree.setFont(new Font("SansSerif", Font.PLAIN, 13));
        folderTree.setRootVisible(false);
        folderTree.setShowsRootHandles(true);
        folderTree.setCellRenderer(new FolderRenderer());

        folderTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) folderTree.getLastSelectedPathComponent();
            if (node == null) return;
            String name = node.getUserObject().toString();
            activeFolder = name;
            refreshSnippetList();
            status("Folder: " + name);
        });

        JScrollPane scroll = new JScrollPane(folderTree);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(PANEL_BG);
        panel.add(scroll, BorderLayout.CENTER);

        JPanel bottomBtns = new JPanel(new GridLayout(3, 1, 0, 4));
        bottomBtns.setBackground(PANEL_BG);
        bottomBtns.setBorder(new EmptyBorder(8, 0, 0, 0));

        bottomBtns.add(makeSidebarBtn("New Snippet", e -> newSnippet()));
        bottomBtns.add(makeSidebarBtn("Import / Export", e -> showImportExportDialog()));
        bottomBtns.add(makeSidebarBtn("Settings", e -> showSettingsDialog()));

        panel.add(bottomBtns, BorderLayout.SOUTH);
        return panel;
    }

    private JButton makeSidebarBtn(String text, ActionListener al) {
        JButton btn = new JButton(text);
        btn.setBackground(BTN_DARK);
        btn.setForeground(TEXT_PRIMARY);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btn.setBorder(new EmptyBorder(6, 6, 6, 6));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(BTN_HOVER); }
            public void mouseExited(MouseEvent e) { btn.setBackground(BTN_DARK); }
        });
        btn.addActionListener(al);
        return btn;
    }

    // ---- Right split: snippet list (center) + editor (far right) ----

    private JSplitPane buildRightSplit() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setBorder(null);
        split.setDividerSize(4);
        split.setContinuousLayout(true);
        split.setLeftComponent(buildCenterPanel());
        split.setRightComponent(buildEditorPanel());
        split.setDividerLocation(310);
        return split;
    }

    // ---- Center Panel (search + snippet list) ----

    private JPanel buildCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(BG_DARK);

        JPanel searchPanel = new JPanel(new BorderLayout(8, 0));
        searchPanel.setBackground(PANEL_BG);
        searchPanel.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, PANEL_BORDER),
            new EmptyBorder(8, 10, 8, 10)));

        searchField = new JTextField();
        searchField.setFont(new Font("Monospaced", Font.PLAIN, 13));
        searchField.setBackground(EDITOR_BG);
        searchField.setForeground(TEXT_PRIMARY);
        searchField.setCaretColor(TEXT_PRIMARY);
        searchField.setBorder(new CompoundBorder(
            new LineBorder(PANEL_BORDER, 1),
            new EmptyBorder(6, 8, 6, 8)));
        searchField.putClientProperty("JTextField.placeholderText", "Search snippets...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshSnippetList(); }
            public void removeUpdate(DocumentEvent e) { refreshSnippetList(); }
            public void changedUpdate(DocumentEvent e) { refreshSnippetList(); }
        });
        searchPanel.add(searchField, BorderLayout.CENTER);

        langCombo = new JComboBox<>(concat(new String[]{"All Languages"}, SUPPORTED_LANGUAGES));
        langCombo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        langCombo.setBackground(BTN_DARK);
        langCombo.setForeground(TEXT_PRIMARY);
        langCombo.setPreferredSize(new Dimension(140, 30));
        langCombo.addActionListener(e -> refreshSnippetList());
        searchPanel.add(langCombo, BorderLayout.EAST);

        panel.add(searchPanel, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        snippetList = new JList<>(listModel);
        snippetList.setFont(new Font("Monospaced", Font.PLAIN, 13));
        snippetList.setBackground(BG_DARK);
        snippetList.setForeground(TEXT_PRIMARY);
        snippetList.setSelectionBackground(SELECTION_BG);
        snippetList.setSelectionForeground(Color.WHITE);
        snippetList.setCellRenderer(new SnippetCellRenderer());
        snippetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        snippetList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) copyToClipboard();
                int idx = snippetList.locationToIndex(e.getPoint());
                if (idx >= 0) selectSnippet(idx);
            }
        });
        snippetList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int idx = snippetList.getSelectedIndex();
                if (idx >= 0) selectSnippet(idx);
            }
        });

        snippetList.setComponentPopupMenu(buildSnippetPopup());

        JScrollPane scroll = new JScrollPane(snippetList);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_DARK);
        panel.add(scroll, BorderLayout.CENTER);

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        actionBtns.setBackground(PANEL_BG);
        actionBtns.setBorder(new MatteBorder(1, 0, 0, 0, PANEL_BORDER));

        actionBtns.add(makeSmallBtn("Copy", e -> copyToClipboard()));
        actionBtns.add(makeSmallBtn("Star", e -> toggleFavorite()));
        actionBtns.add(makeSmallBtn("Duplicate", e -> duplicateSnippet()));
        actionBtns.add(makeSmallBtn("Delete", e -> deleteSnippet()));

        panel.add(actionBtns, BorderLayout.SOUTH);
        return panel;
    }

    private JPopupMenu buildSnippetPopup() {
        snippetPopup = new JPopupMenu();
        snippetPopup.setBackground(PANEL_BG);
        snippetPopup.add(menuItem("Copy to Clipboard", -1, e -> copyToClipboard()));
        snippetPopup.add(menuItem("Duplicate", -1, e -> duplicateSnippet()));
        snippetPopup.addSeparator();
        snippetPopup.add(menuItem("Delete", -1, e -> deleteSnippet()));
        return snippetPopup;
    }

    private JButton makeSmallBtn(String text, ActionListener al) {
        JButton btn = new JButton(text);
        btn.setBackground(BTN_DARK);
        btn.setForeground(TEXT_PRIMARY);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btn.setBorder(new EmptyBorder(4, 10, 4, 10));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(BTN_HOVER); }
            public void mouseExited(MouseEvent e) { btn.setBackground(BTN_DARK); }
        });
        btn.addActionListener(al);
        return btn;
    }

    // ---- Editor Panel (right) ----

    private JPanel buildEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(BG_DARK);
        panel.setBorder(new MatteBorder(0, 1, 0, 0, PANEL_BORDER));

        JPanel topBar = new JPanel(new BorderLayout(8, 0));
        topBar.setBackground(PANEL_BG);
        topBar.setBorder(new EmptyBorder(8, 10, 8, 10));

        titleField = new JTextField("Select a snippet");
        titleField.setFont(new Font("SansSerif", Font.BOLD, 15));
        titleField.setBackground(EDITOR_BG);
        titleField.setForeground(TEXT_PRIMARY);
        titleField.setCaretColor(TEXT_PRIMARY);
        titleField.setBorder(new CompoundBorder(new LineBorder(PANEL_BORDER, 1), new EmptyBorder(6, 8, 6, 8)));
        titleField.getDocument().addDocumentListener(simpleDocListener(() -> {
            if (!suppressEvents && selectedIndex >= 0) {
                snippets.get(selectedIndex).title = titleField.getText();
                snippets.get(selectedIndex).touch();
                refreshSnippetList();
                scheduleSave();
            }
        }));
        topBar.add(titleField, BorderLayout.CENTER);

        starBtn = new JButton("\u2606");
        starBtn.setFont(new Font("SansSerif", Font.PLAIN, 20));
        starBtn.setBackground(PANEL_BG);
        starBtn.setForeground(TEXT_MUTED);
        starBtn.setBorder(null);
        starBtn.setFocusPainted(false);
        starBtn.setToolTipText("Toggle favorite");
        starBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        starBtn.addActionListener(e -> toggleFavorite());
        topBar.add(starBtn, BorderLayout.EAST);

        panel.add(topBar, BorderLayout.NORTH);

        JPanel metaPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        metaPanel.setBackground(PANEL_BG);
        metaPanel.setBorder(new EmptyBorder(0, 10, 6, 10));

        JPanel langPanel = new JPanel(new BorderLayout(4, 0));
        langPanel.setBackground(PANEL_BG);
        JLabel langLabel = new JLabel("Language:");
        langLabel.setForeground(TEXT_MUTED);
        langLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        langPanel.add(langLabel, BorderLayout.WEST);
        JComboBox<String> editorLang = new JComboBox<>(SUPPORTED_LANGUAGES);
        editorLang.setFont(new Font("SansSerif", Font.PLAIN, 12));
        editorLang.setBackground(BTN_DARK);
        editorLang.setForeground(TEXT_PRIMARY);
        editorLang.addActionListener(e -> {
            if (!suppressEvents && selectedIndex >= 0) {
                snippets.get(selectedIndex).language = (String) editorLang.getSelectedItem();
                snippets.get(selectedIndex).touch();
                scheduleSave();
                refreshSnippetList();
            }
        });
        langCombo = editorLang;
        langPanel.add(editorLang, BorderLayout.CENTER);

        JPanel tagPanel = new JPanel(new BorderLayout(4, 0));
        tagPanel.setBackground(PANEL_BG);
        JLabel tagLabel = new JLabel("Tags:");
        tagLabel.setForeground(TEXT_MUTED);
        tagLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        tagPanel.add(tagLabel, BorderLayout.WEST);
        tagField = new JTextField();
        tagField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tagField.setBackground(EDITOR_BG);
        tagField.setForeground(TEXT_PRIMARY);
        tagField.setCaretColor(TEXT_PRIMARY);
        tagField.setBorder(new CompoundBorder(new LineBorder(PANEL_BORDER, 1), new EmptyBorder(4, 6, 4, 6)));
        tagField.setToolTipText("Comma-separated tags");
        tagField.getDocument().addDocumentListener(simpleDocListener(() -> {
            if (!suppressEvents && selectedIndex >= 0) {
                snippets.get(selectedIndex).tags = tagField.getText();
                snippets.get(selectedIndex).touch();
                scheduleSave();
            }
        }));
        tagPanel.add(tagField, BorderLayout.CENTER);

        metaPanel.add(langPanel);
        metaPanel.add(tagPanel);
        panel.add(metaPanel, BorderLayout.SOUTH);

        editorArea = new JTextArea();
        editorArea.setFont(new Font("Monospaced", Font.PLAIN, settings.fontSize));
        editorArea.setBackground(EDITOR_BG);
        editorArea.setForeground(TEXT_PRIMARY);
        editorArea.setCaretColor(TEXT_PRIMARY);
        editorArea.setSelectionColor(SELECTION_BG);
        editorArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        editorArea.setTabSize(settings.tabSize);
        editorArea.setLineWrap(true);
        editorArea.setWrapStyleWord(false);
        editorArea.getDocument().addDocumentListener(simpleDocListener(() -> {
            if (!suppressEvents && selectedIndex >= 0) {
                snippets.get(selectedIndex).content = editorArea.getText();
                snippets.get(selectedIndex).touch();
                highlightSyntax();
                scheduleSave();
            }
        }));

        highlightOverlay = new JTextArea();
        highlightOverlay.setEditable(false);
        highlightOverlay.setFont(editorArea.getFont());
        highlightOverlay.setBackground(new Color(0, 0, 0, 0));
        highlightOverlay.setBorder(new EmptyBorder(10, 10, 10, 10));

        highlightOverlay.setHighlighter(new javax.swing.text.DefaultHighlighter() {
            @Override
            public void paint(java.awt.Graphics g) {
                // Let editorArea handle painting
            }
        });

        JScrollPane editorScroll = new JScrollPane(editorArea);
        editorScroll.setBorder(null);
        editorScroll.getViewport().setBackground(EDITOR_BG);
        panel.add(editorScroll, BorderLayout.CENTER);

        JPanel copyBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        copyBar.setBackground(PANEL_BG);
        copyBar.setBorder(new MatteBorder(1, 0, 0, 0, PANEL_BORDER));

        JButton copyBtn = makeSmallBtn("Copy to Clipboard", e -> copyToClipboard());
        copyBtn.setBackground(ACCENT);
        copyBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        copyBar.add(copyBtn);

        JLabel folderLabel = new JLabel("Folder: ");
        folderLabel.setForeground(TEXT_MUTED);
        folderLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JComboBox<String> folderCombo = new JComboBox<>();
        folderCombo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        folderCombo.setBackground(BTN_DARK);
        folderCombo.setForeground(TEXT_PRIMARY);
        folderCombo.setPreferredSize(new Dimension(120, 26));
        folderCombo.addActionListener(e -> {
            if (!suppressEvents && selectedIndex >= 0) {
                snippets.get(selectedIndex).folder = (String) folderCombo.getSelectedItem();
                snippets.get(selectedIndex).touch();
                refreshFolders();
                scheduleSave();
            }
        });
        copyBar.add(folderLabel);
        copyBar.add(folderCombo);

        panel.add(copyBar, BorderLayout.SOUTH);

        return panel;
    }

    // =====================================================================
    // SNIPPET OPERATIONS
    // =====================================================================

    private void newSnippet() {
        Snippet s = new Snippet();
        s.title = "New Snippet";
        s.language = "Java";
        s.folder = activeFolder.equals("All Snippets") || activeFolder.equals("Favorites") ? "General" : activeFolder;
        snippets.add(0, s);
        refreshFolders();
        refreshSnippetList();
        selectSnippet(0);
        titleField.requestFocusInWindow();
        titleField.selectAll();
        status("New snippet created.");
        saveSnippets();
    }

    private void deleteSnippet() {
        if (selectedIndex < 0 || selectedIndex >= snippets.size()) return;
        Snippet s = snippets.get(selectedIndex);
        int choice = JOptionPane.showConfirmDialog(frame,
            "Delete \"" + s.title + "\"?", "Confirm Delete",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        snippets.remove(selectedIndex);
        refreshFolders();
        refreshSnippetList();
        if (!snippets.isEmpty()) selectSnippet(Math.min(selectedIndex, snippets.size() - 1));
        else clearEditor();
        status("Snippet deleted.");
        saveSnippets();
    }

    private void duplicateSnippet() {
        if (selectedIndex < 0 || selectedIndex >= snippets.size()) return;
        Snippet dup = snippets.get(selectedIndex).copy();
        dup.id = UUID.randomUUID().toString();
        dup.title = dup.title + " (copy)";
        dup.touch();
        snippets.add(0, dup);
        refreshSnippetList();
        selectSnippet(0);
        status("Snippet duplicated.");
        saveSnippets();
    }

    private void toggleFavorite() {
        if (selectedIndex < 0 || selectedIndex >= snippets.size()) return;
        Snippet s = snippets.get(selectedIndex);
        s.favorite = !s.favorite;
        s.touch();
        updateStarButton();
        refreshFolders();
        refreshSnippetList();
        status(s.favorite ? "Added to favorites." : "Removed from favorites.");
        saveSnippets();
    }

    private void copyToClipboard() {
        if (selectedIndex < 0 || selectedIndex >= snippets.size()) return;
        String content = snippets.get(selectedIndex).content;
        StringSelection sel = new StringSelection(content);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
        showCopyFeedback();
        status("Copied to clipboard.");
    }

    // =====================================================================
    // SELECTION & EDITOR
    // =====================================================================

    private void selectSnippet(int idx) {
        if (idx < 0 || idx >= snippets.size()) return;
        selectedIndex = idx;
        suppressEvents = true;
        Snippet s = snippets.get(idx);

        titleField.setText(s.title);
        editorArea.setText(s.content);
        tagField.setText(s.tags);

        langCombo.setSelectedItem(s.language);
        updateStarButton();

        String indent = "  ".repeat(settings.tabSize);
        editorArea.setTabSize(settings.tabSize);

        highlightSyntax();
        status("Editing: " + s.title + " [" + s.language + "]");
        suppressEvents = false;
    }

    private void clearEditor() {
        selectedIndex = -1;
        suppressEvents = true;
        titleField.setText("Select a snippet");
        editorArea.setText("");
        tagField.setText("");
        starBtn.setText("\u2606");
        starBtn.setForeground(TEXT_MUTED);
        suppressEvents = false;
    }

    private void updateStarButton() {
        if (selectedIndex < 0 || selectedIndex >= snippets.size()) return;
        Snippet s = snippets.get(selectedIndex);
        starBtn.setText(s.favorite ? "\u2605" : "\u2606");
        starBtn.setForeground(s.favorite ? STAR_COLOR : TEXT_MUTED);
    }

    // =====================================================================
    // SYNTAX HIGHLIGHTING (colorized via default highlighter)
    // =====================================================================

    private void highlightSyntax() {
        javax.swing.text.Highlighter hl = editorArea.getHighlighter();
        hl.removeAllHighlights();
        String lang = (selectedIndex >= 0 && selectedIndex < snippets.size())
            ? snippets.get(selectedIndex).language : "";
        String text = editorArea.getText();
        if (text.isEmpty()) return;

        Color kw = new Color(130, 170, 255);
        Color str = new Color(150, 220, 130);
        Color comment = new Color(120, 120, 140);
        Color number = new Color(240, 170, 100);

        try {
            Pattern kwPattern = keywordPattern(lang);
            if (kwPattern != null) {
                Matcher m = kwPattern.matcher(text);
                while (m.find()) hl.addHighlight(m.start(), m.end(),
                    new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(new Color(kw.getRed(), kw.getGreen(), kw.getBlue(), 50)));
            }
            Pattern strP = Pattern.compile("(\"[^\"]*\")|('[^']*')|(`[^`]*`)");
            Matcher sm = strP.matcher(text);
            while (sm.find()) hl.addHighlight(sm.start(), sm.end(),
                new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(new Color(str.getRed(), str.getGreen(), str.getBlue(), 40)));
            Pattern commentP = Pattern.compile("(//.*$|/\\*[\\s\\S]*?\\*/|#.*$|--.*$)", Pattern.MULTILINE);
            Matcher cm = commentP.matcher(text);
            while (cm.find()) hl.addHighlight(cm.start(), cm.end(),
                new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(new Color(comment.getRed(), comment.getGreen(), comment.getBlue(), 50)));
            Pattern numP = Pattern.compile("\\b\\d+\\.?\\d*\\b");
            Matcher nm = numP.matcher(text);
            while (nm.find()) hl.addHighlight(nm.start(), nm.end(),
                new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(new Color(number.getRed(), number.getGreen(), number.getBlue(), 40)));
        } catch (Exception ignored) {}
    }

    private Pattern keywordPattern(String lang) {
        switch (lang) {
            case "Java":
                return Pattern.compile("\\b(public|private|protected|class|interface|extends|implements|new|return|if|else|for|while|do|switch|case|break|continue|try|catch|finally|throw|throws|static|final|void|int|long|double|float|boolean|char|byte|short|String|var|import|package|this|super|null|true|false|abstract|synchronized|volatile|enum|instanceof|assert|yield|record|sealed|permits)\\b");
            case "C#":
                return Pattern.compile("\\b(public|private|protected|internal|class|struct|interface|enum|namespace|using|new|return|if|else|for|while|do|switch|case|break|continue|try|catch|finally|throw|throws|static|readonly|void|int|long|double|float|bool|string|var|async|await|yield|override|virtual|abstract|sealed|partial|base|this|null|true|false|typeof|nameof|when)\\b");
            case "Python":
                return Pattern.compile("\\b(def|class|if|elif|else|for|while|return|import|from|as|try|except|finally|raise|with|yield|lambda|pass|break|continue|and|or|not|in|is|True|False|None|print|self|async|await|global|nonlocal|assert|del)\\b");
            case "Bash":
                return Pattern.compile("\\b(if|then|else|elif|fi|for|do|done|while|until|case|esac|function|return|exit|echo|export|source|alias|local|readonly|declare|set|unset|shift|test)\\b");
            case "JavaScript":
                return Pattern.compile("\\b(const|let|var|function|return|if|else|for|while|do|switch|case|break|continue|try|catch|finally|throw|new|class|extends|import|export|from|default|async|await|yield|null|undefined|true|false|this|typeof|instanceof|of|in|delete|void)\\b");
            case "HTML":
                return Pattern.compile("(&lt;/?[a-zA-Z][a-zA-Z0-9]*|\\b(class|id|href|src|style|type|name|value|rel)\\b)");
            case "CSS":
                return Pattern.compile("\\b(color|background|margin|padding|border|display|font|width|height|position|top|left|right|bottom|flex|grid|animation|transition|opacity|overflow|z-index|content)\\b");
            case "Velice":
                return Pattern.compile("\\b(func|var|let|if|else|for|while|return|struct|enum|module|import|export|pub|priv|class|new|this|null|true|false|match|case|break|continue|defer|go|chan|select|type|interface|const)\\b");
            case "Vexa":
                return Pattern.compile("\\b(fn|let|mut|if|else|match|for|while|loop|return|break|continue|struct|enum|trait|impl|pub|mod|use|crate|self|super|true|false|async|await|move|ref|where|type|const|static|unsafe|extern|yield)\\b");
            default:
                return null;
        }
    }

    // =====================================================================
    // FOLDERS
    // =====================================================================

    private void refreshFolders() {
        rootNode.removeAllChildren();
        Set<String> folderSet = new LinkedHashSet<>();
        folderSet.add("All Snippets");
        folderSet.add("Favorites");
        for (Snippet s : snippets) {
            if (s.folder != null && !s.folder.isEmpty()) folderSet.add(s.folder);
        }
        folders.clear();
        for (String f : folderSet) {
            folders.add(f);
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(f);
            rootNode.add(node);
            if (f.equals("Favorites")) {
                long count = snippets.stream().filter(s -> s.favorite).count();
                node.setUserObject(f + " (" + count + ")");
            } else if (!f.equals("All Snippets")) {
                long count = snippets.stream().filter(s -> f.equals(s.folder)).count();
                node.setUserObject(f + " (" + count + ")");
            } else {
                node.setUserObject(f + " (" + snippets.size() + ")");
            }
        }

        treeModel.reload();
        for (int i = 0; i < folderTree.getRowCount(); i++) folderTree.expandRow(i);

        // Update folder combo in editor panel
        Component[] comps = frame.getContentPane().getComponents();
        updateFolderCombos();
    }

    private void updateFolderCombos() {
        // Find the folder combo by traversing
        Container contentPane = frame.getContentPane();
        findAndUpdateFolderCombo(contentPane);
    }

    @SuppressWarnings("unchecked")
    private void findAndUpdateFolderCombo(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JComboBox) {
                JComboBox<Object> combo = (JComboBox<Object>) c;
                if (combo.getItemCount() > 0 && combo.getItemAt(0) instanceof String) {
                    String first = (String) combo.getItemAt(0);
                    if ("General".equals(first) || "All Snippets".equals(first)) {
                        String current = combo.getSelectedItem() instanceof String ? (String) combo.getSelectedItem() : "";
                        combo.removeAllItems();
                        Set<String> uniqueFolders = new LinkedHashSet<>(folders);
                        uniqueFolders.remove("All Snippets");
                        uniqueFolders.remove("Favorites");
                        if (uniqueFolders.isEmpty()) uniqueFolders.add("General");
                        for (String f : uniqueFolders) combo.addItem(f);
                        if (!current.isEmpty()) combo.setSelectedItem(current);
                    }
                }
            } else if (c instanceof Container) {
                findAndUpdateFolderCombo((Container) c);
            }
        }
    }

    // =====================================================================
    // SNIPPET LIST
    // =====================================================================

    private void refreshSnippetList() {
        listModel.clear();
        String query = searchField.getText().toLowerCase().trim();
        String langFilter = langCombo.getSelectedItem() instanceof String
            ? (String) langCombo.getSelectedItem() : "All Languages";

        for (Snippet s : snippets) {
            if (!matchesFilter(s)) continue;
            if (!langFilter.equals("All Languages") && !s.language.equals(langFilter)) continue;
            if (!query.isEmpty()) {
                String haystack = (s.title + " " + s.content + " " + s.language + " " + s.tags).toLowerCase();
                if (!haystack.contains(query)) continue;
            }
            String icon = s.favorite ? "\u2605 " : "  ";
            listModel.addElement(icon + s.title + "  [" + s.language + "]");
        }

        // re-select if possible
        if (selectedIndex >= 0 && selectedIndex < snippets.size()) {
            snippetList.setSelectedIndex(Math.min(selectedIndex, listModel.size() - 1));
        }
    }

    private boolean matchesFilter(Snippet s) {
        switch (activeFolder) {
            case "All Snippets": return true;
            case "Favorites": return s.favorite;
            default: return activeFolder.equals(s.folder);
        }
    }

    // =====================================================================
    // IMPORT / EXPORT
    // =====================================================================

    private void exportSnippets() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export Snippets");
        fc.setFileFilter(new FileNameExtensionFilter("JSON files", "json"));
        fc.setSelectedFile(new File("codeshelf_export.json"));
        if (fc.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;

        try {
            List<Object> data = new ArrayList<>();
            for (Snippet s : snippets) data.add(snippetToMap(s));
            String json = MiniJSON.stringify(data);
            Files.writeString(fc.getSelectedFile().toPath(), json, StandardCharsets.UTF_8);
            status("Exported " + snippets.size() + " snippets.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Export failed: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importSnippets() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Import Snippets");
        fc.setFileFilter(new FileNameExtensionFilter("JSON files", "json"));
        if (fc.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;

        try {
            String json = Files.readString(fc.getSelectedFile().toPath(), StandardCharsets.UTF_8);
            Object parsed = MiniJSON.parse(json);
            if (parsed instanceof List) {
                List<?> list = (List<?>) parsed;
                int count = 0;
                for (Object item : list) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) item;
                        Snippet s = mapToSnippet(map);
                        s.id = UUID.randomUUID().toString();
                        snippets.add(s);
                        count++;
                    }
                }
                refreshFolders();
                refreshSnippetList();
                saveSnippets();
                status("Imported " + count + " snippets.");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Import failed: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showImportExportDialog() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 8));
        panel.setBackground(PANEL_BG);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton impBtn = makeSidebarBtn("Import from JSON...", e -> { importSnippets(); });
        JButton expBtn = makeSidebarBtn("Export to JSON...", e -> { exportSnippets(); });

        panel.add(impBtn);
        panel.add(expBtn);

        JOptionPane.showMessageDialog(frame, panel, "Import / Export", JOptionPane.PLAIN_MESSAGE);
    }

    // =====================================================================
    // SETTINGS DIALOG
    // =====================================================================

    private void showSettingsDialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(PANEL_BG);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel fsLabel = new JLabel("Font Size:");
        fsLabel.setForeground(TEXT_PRIMARY);
        panel.add(fsLabel, gbc);
        JSpinner fsSpinner = new JSpinner(new SpinnerNumberModel(settings.fontSize, 8, 36, 1));
        fsSpinner.setFont(new Font("SansSerif", Font.PLAIN, 13));
        gbc.gridx = 1;
        panel.add(fsSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        JLabel tabLabel = new JLabel("Tab Size:");
        tabLabel.setForeground(TEXT_PRIMARY);
        panel.add(tabLabel, gbc);
        JSpinner tabSpinner = new JSpinner(new SpinnerNumberModel(settings.tabSize, 2, 8, 1));
        tabSpinner.setFont(new Font("SansSerif", Font.PLAIN, 13));
        gbc.gridx = 1;
        panel.add(tabSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        JLabel themeLabel = new JLabel("Theme Variant:");
        themeLabel.setForeground(TEXT_PRIMARY);
        panel.add(themeLabel, gbc);
        JComboBox<String> themeBox = new JComboBox<>(new String[]{"Deep Navy", "Midnight Purple", "Charcoal"});
        themeBox.setSelectedItem(settings.themeVariant);
        themeBox.setFont(new Font("SansSerif", Font.PLAIN, 13));
        gbc.gridx = 1;
        panel.add(themeBox, gbc);

        int result = JOptionPane.showConfirmDialog(frame, panel, "Settings",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            settings.fontSize = (Integer) fsSpinner.getValue();
            settings.tabSize = (Integer) tabSpinner.getValue();
            settings.themeVariant = (String) themeBox.getSelectedItem();
            applyFontSize();
            saveSettings();
            status("Settings saved.");
        }
    }

    private void applyFontSize() {
        editorArea.setFont(new Font("Monospaced", Font.PLAIN, settings.fontSize));
        editorArea.setTabSize(settings.tabSize);
        highlightSyntax();
    }

    // =====================================================================
    // ABOUT DIALOG
    // =====================================================================

    private void showAbout() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(PANEL_BG);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel(APP_NAME);
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(STAR_COLOR);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(title, BorderLayout.NORTH);

        String info = "<html><center>"
            + "<b>Version " + VERSION + "</b><br><br>"
            + "A local code snippet manager for Datacenter.<br>"
            + "Supports: Java, C#, Python, Bash, JavaScript,<br>"
            + "HTML, CSS, Velice, Vexa<br><br>"
            + "Features: folders, tags, favorites, search,<br>"
            + "syntax highlighting, import/export, templates.<br><br>"
            + "Keyboard Shortcuts:<br>"
            + "Ctrl+N New | Ctrl+D Delete | Ctrl+C Copy<br>"
            + "Ctrl+F Find | Ctrl+S Save<br>"
            + "Ctrl+E Export | Ctrl+I Import"
            + "</center></html>";
        JLabel infoLabel = new JLabel(info);
        infoLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        infoLabel.setForeground(TEXT_PRIMARY);
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(infoLabel, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(frame, panel, "About " + APP_NAME, JOptionPane.PLAIN_MESSAGE);
    }

    // =====================================================================
    // KEYBOARD SHORTCUTS
    // =====================================================================

    private void registerKeyboardShortcuts() {
        InputMap im = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = frame.getRootPane().getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "new");
        am.put("new", new AbstractAction() { public void actionPerformed(ActionEvent e) { newSnippet(); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK), "delete");
        am.put("delete", new AbstractAction() { public void actionPerformed(ActionEvent e) { deleteSnippet(); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "copy");
        am.put("copy", new AbstractAction() { public void actionPerformed(ActionEvent e) { copyToClipboard(); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "find");
        am.put("find", new AbstractAction() { public void actionPerformed(ActionEvent e) { searchField.requestFocusInWindow(); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "save");
        am.put("save", new AbstractAction() { public void actionPerformed(ActionEvent e) { saveSnippets(); status("Saved."); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK), "export");
        am.put("export", new AbstractAction() { public void actionPerformed(ActionEvent e) { exportSnippets(); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK), "import");
        am.put("import", new AbstractAction() { public void actionPerformed(ActionEvent e) { importSnippets(); } });
    }

    // =====================================================================
    // PERSISTENCE
    // =====================================================================

    private void loadSnippets() {
        snippets.clear();
        Path p = snippetsPath();
        if (!Files.exists(p)) return;
        try {
            String json = Files.readString(p, StandardCharsets.UTF_8);
            if (json.isBlank()) return;
            Object parsed = MiniJSON.parse(json);
            if (parsed instanceof List) {
                for (Object item : (List<?>) parsed) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) item;
                        snippets.add(mapToSnippet(map));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load snippets: " + e.getMessage());
        }
    }

    private void saveSnippets() {
        try {
            List<Object> data = new ArrayList<>();
            for (Snippet s : snippets) data.add(snippetToMap(s));
            Files.writeString(snippetsPath(), MiniJSON.stringify(data), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to save snippets: " + e.getMessage());
        }
    }

    private void loadSettings() {
        Path p = settingsPath();
        if (!Files.exists(p)) return;
        try {
            String json = Files.readString(p, StandardCharsets.UTF_8);
            Object parsed = MiniJSON.parse(json);
            if (parsed instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) parsed;
                settings.fromMap(map);
            }
        } catch (Exception e) {
            System.err.println("Failed to load settings: " + e.getMessage());
        }
    }

    private void saveSettings() {
        try {
            Files.writeString(settingsPath(), MiniJSON.stringify(settings.toMap()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to save settings: " + e.getMessage());
        }
    }

    private Map<String, Object> snippetToMap(Snippet s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.id);
        m.put("title", s.title);
        m.put("content", s.content);
        m.put("language", s.language);
        m.put("folder", s.folder);
        m.put("tags", s.tags);
        m.put("favorite", s.favorite);
        m.put("createdAt", s.createdAt);
        m.put("modifiedAt", s.modifiedAt);
        return m;
    }

    private Snippet mapToSnippet(Map<String, Object> m) {
        Snippet s = new Snippet();
        if (m.containsKey("id")) s.id = String.valueOf(m.get("id"));
        if (m.containsKey("title")) s.title = String.valueOf(m.get("title"));
        if (m.containsKey("content")) s.content = String.valueOf(m.get("content"));
        if (m.containsKey("language")) s.language = String.valueOf(m.get("language"));
        if (m.containsKey("folder")) s.folder = String.valueOf(m.get("folder"));
        if (m.containsKey("tags")) s.tags = String.valueOf(m.get("tags"));
        if (m.containsKey("favorite")) s.favorite = Boolean.TRUE.equals(m.get("favorite"));
        if (m.containsKey("createdAt")) s.createdAt = String.valueOf(m.get("createdAt"));
        if (m.containsKey("modifiedAt")) s.modifiedAt = String.valueOf(m.get("modifiedAt"));
        return s;
    }

    // =====================================================================
    // AUTO-SAVE TIMER
    // =====================================================================

    private void scheduleSave() {
        if (saveTimer != null && saveTimer.isRunning()) saveTimer.stop();
        saveTimer = new javax.swing.Timer(800, e -> saveSnippets());
        saveTimer.setRepeats(false);
        saveTimer.start();
    }

    // =====================================================================
    // COPY FEEDBACK
    // =====================================================================

    private void showCopyFeedback() {
        copyFeedbackLabel.setText("Copied!");
        if (copyFeedbackTimer != null && copyFeedbackTimer.isRunning()) copyFeedbackTimer.stop();
        copyFeedbackTimer = new javax.swing.Timer(1500, e -> copyFeedbackLabel.setText(" "));
        copyFeedbackTimer.setRepeats(false);
        copyFeedbackTimer.start();
    }

    // =====================================================================
    // UTILITIES
    // =====================================================================

    private void status(String msg) {
        statusLabel.setText(msg);
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] concat(T[] a, T... b) {
        T[] result = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), a.length + b.length);
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    private DocumentListener simpleDocListener(Runnable action) {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { action.run(); }
            public void removeUpdate(DocumentEvent e) { action.run(); }
            public void changedUpdate(DocumentEvent e) { action.run(); }
        };
    }

    // =====================================================================
    // CELL RENDERERS
    // =====================================================================

    private class SnippetCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setFont(new Font("Monospaced", Font.PLAIN, 13));
            label.setBorder(new EmptyBorder(6, 8, 6, 8));
            if (isSelected) {
                label.setBackground(SELECTION_BG);
                label.setForeground(Color.WHITE);
            } else {
                label.setBackground(BG_DARK);
                label.setForeground(TEXT_PRIMARY);
            }
            String text = label.getText();
            if (text != null && text.startsWith("\u2605")) {
                label.setForeground(STAR_COLOR);
            }
            return label;
        }
    }

    private class FolderRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                      boolean expanded, boolean leaf, int row, boolean hasFocus) {
            JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            label.setFont(new Font("SansSerif", Font.PLAIN, 13));
            label.setBorder(new EmptyBorder(2, 4, 2, 4));
            if (sel) {
                label.setBackground(SELECTION_BG);
                label.setForeground(STAR_COLOR);
            } else {
                label.setBackground(PANEL_BG);
                label.setForeground(TEXT_PRIMARY);
            }
            label.setOpaque(true);
            return label;
        }
    }

    // =====================================================================
    // EXAMPLE SNIPPETS
    // =====================================================================

    private void populateExamples() {
        addExample("Hello World", "Java",
            "public class HelloWorld {\n"
            + "    public static void main(String[] args) {\n"
            + "        System.out.println(\"Hello, World!\");\n"
            + "    }\n"
            + "}",
            "General", "template,example");

        addExample("FizzBuzz", "Python",
            "def fizzbuzz(n):\n"
            + "    for i in range(1, n + 1):\n"
            + "        if i % 15 == 0:\n"
            + "            print(\"FizzBuzz\")\n"
            + "        elif i % 3 == 0:\n"
            + "            print(\"Fizz\")\n"
            + "        elif i % 5 == 0:\n"
            + "            print(\"Buzz\")\n"
            + "        else:\n"
            + "            print(i)\n"
            + "\n"
            + "fizzbuzz(100)",
            "General", "algorithm,example");

        addExample("HTTP Server", "JavaScript",
            "const http = require('http');\n"
            + "\n"
            + "const server = http.createServer((req, res) => {\n"
            + "    res.writeHead(200, { 'Content-Type': 'text/plain' });\n"
            + "    res.end('Hello from CodeShelf!\\n');\n"
            + "});\n"
            + "\n"
            + "server.listen(3000, () => {\n"
            + "    console.log('Server running on port 3000');\n"
            + "});",
            "General", "api,network");

        addExample("Quick Sort", "C#",
            "public static void QuickSort(int[] arr, int low, int high)\n"
            + "{\n"
            + "    if (low < high)\n"
            + "    {\n"
            + "        int pivot = Partition(arr, low, high);\n"
            + "        QuickSort(arr, low, pivot - 1);\n"
            + "        QuickSort(arr, pivot + 1, high);\n"
            + "    }\n"
            + "}\n"
            + "\n"
            + "static int Partition(int[] arr, int low, int high)\n"
            + "{\n"
            + "    int pivot = arr[high];\n"
            + "    int i = low - 1;\n"
            + "    for (int j = low; j < high; j++)\n"
            + "    {\n"
            + "        if (arr[j] < pivot)\n"
            + "        {\n"
            + "            i++;\n"
            + "            (arr[i], arr[j]) = (arr[j], arr[i]);\n"
            + "        }\n"
            + "    }\n"
            + "    (arr[i + 1], arr[high]) = (arr[high], arr[i + 1]);\n"
            + "    return i + 1;\n"
            + "}",
            "Algorithms", "algorithm,utility");

        addExample("Backup Script", "Bash",
            "#!/usr/bin/env bash\n"
            + "set -Eeuo pipefail\n"
            + "\n"
            + "SRC=\"/home/user/projects\"\n"
            + "DST=\"/mnt/backup/$(date +%Y%m%d)\"\n"
            + "\n"
            + "mkdir -p \"$DST\"\n"
            + "rsync -av --delete \"$SRC/\" \"$DST/\"\n"
            + "\n"
            + "echo \"Backup complete: $DST\"",
            "General", "utility,template");

        addExample("Login Page", "HTML",
            "<!DOCTYPE html>\n"
            + "<html lang=\"en\">\n"
            + "<head>\n"
            + "    <meta charset=\"UTF-8\">\n"
            + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "    <title>Login</title>\n"
            + "    <link rel=\"stylesheet\" href=\"style.css\">\n"
            + "</head>\n"
            + "<body>\n"
            + "    <div class=\"login-container\">\n"
            + "        <h1>Login</h1>\n"
            + "        <form>\n"
            + "            <input type=\"email\" placeholder=\"Email\" required>\n"
            + "            <input type=\"password\" placeholder=\"Password\" required>\n"
            + "            <button type=\"submit\">Sign In</button>\n"
            + "        </form>\n"
            + "    </div>\n"
            + "</body>\n"
            + "</html>",
            "Web", "ui,template");

        addExample("Dark Theme", "CSS",
            ":root {\n"
            + "    --bg-primary: #1a1a2e;\n"
            + "    --bg-secondary: #16213e;\n"
            + "    --accent: #0f3460;\n"
            + "    --text-primary: #e0e0e0;\n"
            + "    --text-muted: #8888aa;\n"
            + "}\n"
            + "\n"
            + "body {\n"
            + "    background-color: var(--bg-primary);\n"
            + "    color: var(--text-primary);\n"
            + "    font-family: 'Segoe UI', sans-serif;\n"
            + "    margin: 0;\n"
            + "    padding: 20px;\n"
            + "}\n"
            + "\n"
            + ".card {\n"
            + "    background: var(--bg-secondary);\n"
            + "    border: 1px solid var(--accent);\n"
            + "    border-radius: 8px;\n"
            + "    padding: 16px;\n"
            + "}",
            "Web", "ui,config");

        addExample("Hello World (Velice)", "Velice",
            "module main\n"
            + "\n"
            + "import \"fmt\"\n"
            + "\n"
            + "func main() {\n"
            + "    fmt.Println(\"Hello from Velice!\")\n"
            + "    \n"
            + "    numbers := []int{1, 2, 3, 4, 5}\n"
            + "    sum := 0\n"
            + "    for _, n := range numbers {\n"
            + "        sum += n\n"
            + "    }\n"
            + "    fmt.Println(\"Sum:\", sum)\n"
            + "}",
            "General", "example,template");

        addExample("Async Fetcher", "Vexa",
            "use std::net::TcpStream;\n"
            + "use std::io::{Read, Write};\n"
            + "\n"
            + "fn fetch(url: &str) -> Result<String, Box<dyn std::error::Error>> {\n"
            + "    let mut stream = TcpStream::connect(url)?;\n"
            + "    let mut buffer = String::new();\n"
            + "    stream.read_to_string(&mut buffer)?;\n"
            + "    Ok(buffer)\n"
            + "}\n"
            + "\n"
            + "fn main() {\n"
            + "    match fetch(\"example.com:80\") {\n"
            + "        Ok(data) => println!(\"Received: {}\", data),\n"
            + "        Err(e) => eprintln!(\"Error: {}\", e),\n"
            + "    }\n"
            + "}",
            "General", "network,api");

        addExample("Singleton Pattern", "Java",
            "public class Singleton {\n"
            + "    private static volatile Singleton instance;\n"
            + "    private final String data;\n"
            + "\n"
            + "    private Singleton(String data) {\n"
            + "        this.data = data;\n"
            + "    }\n"
            + "\n"
            + "    public static Singleton getInstance(String data) {\n"
            + "        if (instance == null) {\n"
            + "            synchronized (Singleton.class) {\n"
            + "                if (instance == null) {\n"
            + "                    instance = new Singleton(data);\n"
            + "                }\n"
            + "            }\n"
            + "        }\n"
            + "        return instance;\n"
            + "    }\n"
            + "\n"
            + "    public String getData() { return data; }\n"
            + "}",
            "Patterns", "design-pattern,template");

        addExample("REST API Client", "Python",
            "import requests\n"
            + "import json\n"
            + "\n"
            + "class APIClient:\n"
            + "    def __init__(self, base_url, api_key=None):\n"
            + "        self.base_url = base_url\n"
            + "        self.headers = {}\n"
            + "        if api_key:\n"
            + "            self.headers['Authorization'] = f'Bearer {api_key}'\n"
            + "\n"
            + "    def get(self, endpoint):\n"
            + "        resp = requests.get(\n"
            + "            f'{self.base_url}{endpoint}',\n"
            + "            headers=self.headers\n"
            + "        )\n"
            + "        resp.raise_for_status()\n"
            + "        return resp.json()\n"
            + "\n"
            + "    def post(self, endpoint, data):\n"
            + "        resp = requests.post(\n"
            + "            f'{self.base_url}{endpoint}',\n"
            + "            json=data,\n"
            + "            headers=self.headers\n"
            + "        )\n"
            + "        resp.raise_for_status()\n"
            + "        return resp.json()\n"
            + "\n"
            + "# Usage\n"
            + "client = APIClient('https://api.example.com', 'token123')\n"
            + "users = client.get('/users')\n"
            + "print(json.dumps(users, indent=2))",
            "API", "api,utility");

        addExample("Responsive Grid", "CSS",
            "/* Responsive CSS Grid Layout */\n"
            + ".grid-container {\n"
            + "    display: grid;\n"
            + "    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));\n"
            + "    gap: 16px;\n"
            + "    padding: 16px;\n"
            + "}\n"
            + "\n"
            + ".grid-item {\n"
            + "    background: #16213e;\n"
            + "    border-radius: 8px;\n"
            + "    padding: 20px;\n"
            + "    transition: transform 0.2s;\n"
            + "}\n"
            + "\n"
            + ".grid-item:hover {\n"
            + "    transform: translateY(-2px);\n"
            + "    box-shadow: 0 4px 12px rgba(0,0,0,0.3);\n"
            + "}\n"
            + "\n"
            + "@media (max-width: 768px) {\n"
            + "    .grid-container {\n"
            + "        grid-template-columns: 1fr;\n"
            + "    }\n"
            + "}",
            "Web", "ui,template");

        addExample("Promise.all Pattern", "JavaScript",
            "// Fetch multiple endpoints in parallel\n"
            + "async function fetchAll(urls) {\n"
            + "    try {\n"
            + "        const responses = await Promise.all(\n"
            + "            urls.map(url => fetch(url).then(r => r.json()))\n"
            + "        );\n"
            + "        return responses;\n"
            + "    } catch (error) {\n"
            + "        console.error('Fetch failed:', error);\n"
            + "        throw error;\n"
            + "    }\n"
            + "}\n"
            + "\n"
            + "// Usage\n"
            + "const urls = [\n"
            + "    'https://api.example.com/users',\n"
            + "    'https://api.example.com/posts',\n"
            + "    'https://api.example.com/comments'\n"
            + "];\n"
            + "\n"
            + "fetchAll(urls).then(([users, posts, comments]) => {\n"
            + "    console.log({ users, posts, comments });\n"
            + "});",
            "General", "api,utility");
    }

    private void addExample(String title, String language, String content, String folder, String tags) {
        Snippet s = new Snippet();
        s.title = title;
        s.language = language;
        s.content = content;
        s.folder = folder;
        s.tags = tags;
        s.favorite = title.equals("Hello World") || title.equals("Quick Sort");
        snippets.add(s);
    }
}
