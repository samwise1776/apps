import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * TestBench — visual test runner for the Datacenter company.
 * Single-file Java Swing application. No external dependencies.
 *
 * Discovers registered applications from config/apps.json, finds test files
 * for each, executes them as subprocesses, and reports results in a rich UI.
 */
public class App {

    // =====================================================================
    // CONSTANTS
    // =====================================================================

    private static final String APP_NAME = "TestBench";
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
    private static final Color ACCENT = new Color(15, 52, 96);
    private static final Color DANGER = new Color(200, 60, 60);
    private static final Color SUCCESS = new Color(60, 180, 90);
    private static final Color WARNING = new Color(220, 180, 50);
    private static final Color SKIP_COLOR = new Color(140, 140, 160);
    private static final Color TABLE_HEADER_BG = new Color(15, 15, 30);
    private static final Color TABLE_ROW_ALT = new Color(20, 20, 38);
    private static final Color TABLE_SELECTION = new Color(15, 52, 96, 140);
    private static final Color PROGRESS_BG = new Color(30, 30, 50);
    private static final Color CONSOLE_BG = new Color(12, 12, 24);
    private static final Color CONSOLE_TEXT = new Color(0, 200, 120);

    private static final String[] STATUS_ICONS = {"\u2714", "\u2716", "\u25CB"};
    private static final Color[] STATUS_COLORS = {SUCCESS, DANGER, SKIP_COLOR};

    // =====================================================================
    // DATA MODEL
    // =====================================================================

    enum TestStatus {
        PASS, FAIL, SKIP, PENDING, RUNNING;

        String icon() { return STATUS_ICONS[ordinal()]; }
        Color color() { return STATUS_COLORS[ordinal()]; }
    }

    static class TestResult {
        String name;
        String source;
        String file;
        TestStatus status;
        long elapsedMs;
        String output;
        String errorOutput;
        int exitCode;

        TestResult(String name, String file) {
            this.name = name;
            this.file = file;
            this.status = TestStatus.PENDING;
            this.elapsedMs = 0;
            this.output = "";
            this.errorOutput = "";
            this.exitCode = -1;
        }
    }

    static class AppInfo {
        String id;
        String slug;
        String name;
        String version;
        String status;
        String language;
        String source;
        String mainClass;
        List<TestResult> tests;
        TestStatus overallStatus;

        AppInfo() {
            this.tests = new ArrayList<>();
            this.overallStatus = TestStatus.PENDING;
        }
    }

    // =====================================================================
    // FIELDS
    // =====================================================================

    private JFrame frame;
    private JList<AppInfo> appList;
    private DefaultListModel<AppInfo> appListModel;
    private JTable testTable;
    private DefaultTableModel testTableModel;
    private JTextArea consoleOutput;
    private JTextArea detailArea;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JLabel timeLabel;
    private JSpinner timeoutSpinner;

    private List<AppInfo> discoveredApps;
    private ExecutorService executor;
    private volatile boolean running = false;
    private long totalStartTime;
    private int testTimeoutSeconds = 30;

    // =====================================================================
    // MAIN
    // =====================================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new App().start());
    }

    // =====================================================================
    // INITIALIZATION
    // =====================================================================

    private void start() {
        executor = Executors.newSingleThreadExecutor();
        discoveredApps = new ArrayList<>();
        discoverAppsFromConfig();
        buildUI();
        refreshAppList();
        frame.setVisible(true);
    }

    // =====================================================================
    // JSON PARSING (manual, no external libs)
    // =====================================================================

    private void discoverAppsFromConfig() {
        String root = findRoot();
        Path configPath = Paths.get(root, "config", "apps.json");
        if (!Files.exists(configPath)) {
            log("Config not found: " + configPath);
            return;
        }
        try {
            String json = new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8);
            parseAppsJson(json);
        } catch (IOException e) {
            log("Error reading config: " + e.getMessage());
        }
    }

    private void parseAppsJson(String json) {
        int idx = json.indexOf("\"applications\"");
        if (idx < 0) return;
        idx = json.indexOf('[', idx);
        if (idx < 0) return;
        int depth = 0;
        int start = -1;
        for (int i = idx; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    String obj = json.substring(start, i + 1);
                    AppInfo app = parseAppObject(obj);
                    if (app != null) {
                        discoveredApps.add(app);
                        discoverTests(app);
                    }
                    start = -1;
                }
            }
        }
    }

    private AppInfo parseAppObject(String obj) {
        AppInfo app = new AppInfo();
        app.id = extractString(obj, "id");
        app.slug = extractString(obj, "slug");
        app.name = extractString(obj, "name");
        app.version = extractString(obj, "version");
        app.status = extractString(obj, "status");
        app.language = extractString(obj, "language");
        app.source = extractString(obj, "source");
        app.mainClass = extractString(obj, "main");
        return app;
    }

    private String extractString(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return "";
        idx = json.indexOf(':', idx + pattern.length());
        if (idx < 0) return "";
        idx++;
        while (idx < json.length() && json.charAt(idx) == ' ') idx++;
        if (idx >= json.length()) return "";
        if (json.charAt(idx) == '"') {
            int end = findStringEnd(json, idx + 1);
            return unescape(json.substring(idx + 1, end));
        }
        int end = idx;
        while (end < json.length() && ",}".indexOf(json.charAt(end)) < 0) end++;
        return json.substring(idx, end).trim();
    }

    private int findStringEnd(String json, int start) {
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\') { i++; continue; }
            if (c == '"') return i;
        }
        return json.length() - 1;
    }

    private String unescape(String s) {
        return s.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n").replace("\\t", "\t");
    }

    // =====================================================================
    // TEST DISCOVERY
    // =====================================================================

    private void discoverTests(AppInfo app) {
        String root = findRoot();
        Path sourceDir = Paths.get(root, app.source);
        if (!Files.exists(sourceDir)) return;

        String lang = app.language != null ? app.language.toLowerCase() : "";

        if (lang.contains("java")) {
            discoverJavaTests(app, sourceDir);
            discoverJavaTests(app, sourceDir.resolve("tests"));
            discoverJavaTests(app, sourceDir.resolve("src"));
        } else if (lang.contains("python")) {
            discoverPythonTests(app, sourceDir);
        } else if (lang.contains("node") || lang.contains("javascript")) {
            discoverNodeTests(app, sourceDir);
        } else {
            discoverJavaTests(app, sourceDir);
            discoverPythonTests(app, sourceDir);
        }
    }

    private void discoverJavaTests(AppInfo app, Path dir) {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) return;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*Test*.java")) {
            for (Path p : stream) {
                String name = p.getFileName().toString().replace(".java", "");
                app.tests.add(new TestResult(name, p.toString()));
            }
        } catch (IOException ignored) {}
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*Tests*.java")) {
            for (Path p : stream) {
                String name = p.getFileName().toString().replace(".java", "");
                boolean dup = app.tests.stream().anyMatch(t -> t.name.equals(name));
                if (!dup) app.tests.add(new TestResult(name, p.toString()));
            }
        } catch (IOException ignored) {}
    }

    private void discoverPythonTests(AppInfo app, Path dir) {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) return;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "test_*.py")) {
            for (Path p : stream) {
                String name = p.getFileName().toString().replace(".py", "");
                app.tests.add(new TestResult(name, p.toString()));
            }
        } catch (IOException ignored) {}
    }

    private void discoverNodeTests(AppInfo app, Path dir) {
        Path pkg = dir.resolve("package.json");
        if (!Files.exists(pkg)) return;
        try {
            String json = new String(Files.readAllBytes(pkg), StandardCharsets.UTF_8);
            if (json.contains("\"test\"")) {
                app.tests.add(new TestResult(app.slug + " (npm test)", pkg.toString()));
            }
        } catch (IOException ignored) {}
    }

    private String findRoot() {
        String prop = System.getProperty("datacenter.root");
        if (prop != null) return prop;
        Path cwd = Paths.get("").toAbsolutePath();
        if (Files.exists(cwd.resolve("config").resolve("apps.json"))) return cwd.toString();
        Path parent = cwd.getParent();
        if (parent != null && Files.exists(parent.resolve("config").resolve("apps.json")))
            return parent.toString();
        if (Files.exists(cwd.resolve("main"))) return cwd.toString();
        return cwd.toString();
    }

    // =====================================================================
    // UI CONSTRUCTION
    // =====================================================================

    private void buildUI() {
        frame = new JFrame(APP_NAME + " v" + VERSION);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 750);
        frame.setMinimumSize(new Dimension(900, 550));
        frame.setLocationRelativeTo(null);

        UIManager.put("Button.background", BTN_DARK);
        UIManager.put("Button.foreground", TEXT_PRIMARY);
        UIManager.put("Panel.background", BG_DARK);
        UIManager.put("Table.background", EDITOR_BG);
        UIManager.put("Table.foreground", TEXT_PRIMARY);
        UIManager.put("TableHeader.background", TABLE_HEADER_BG);
        UIManager.put("TableHeader.foreground", TEXT_PRIMARY);
        UIManager.put("List.background", PANEL_BG);
        UIManager.put("List.foreground", TEXT_PRIMARY);
        UIManager.put("TextArea.background", CONSOLE_BG);
        UIManager.put("TextArea.foreground", CONSOLE_TEXT);
        UIManager.put("ScrollPane.background", BG_DARK);
        UIManager.put("SplitPane.background", BG_DARK);
        UIManager.put("Label.foreground", TEXT_PRIMARY);

        JPanel content = new JPanel(new BorderLayout(0, 0));
        content.setBackground(BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        content.add(createToolbar(), BorderLayout.NORTH);
        content.add(createMainPanel(), BorderLayout.CENTER);
        content.add(createStatusBar(), BorderLayout.SOUTH);

        frame.setContentPane(content);
        frame.setJMenuBar(createMenuBar());

        registerKeyboardShortcuts();
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();
        bar.setBackground(PANEL_BG);
        bar.setBorder(BorderFactory.createLineBorder(PANEL_BORDER));

        JMenu fileMenu = new JMenu("File");
        fileMenu.setForeground(TEXT_PRIMARY);
        fileMenu.add(createMenuItem("Save Report", "Ctrl+S", e -> saveReport()));
        fileMenu.addSeparator();
        fileMenu.add(createMenuItem("Exit", null, e -> System.exit(0)));
        bar.add(fileMenu);

        JMenu runMenu = new JMenu("Run");
        runMenu.setForeground(TEXT_PRIMARY);
        runMenu.add(createMenuItem("Run All", "F5", e -> runAllTests()));
        runMenu.add(createMenuItem("Run Selected", null, e -> runSelectedAppTests()));
        runMenu.add(createMenuItem("Rerun Failed", "Ctrl+R", e -> rerunFailed()));
        runMenu.addSeparator();
        runMenu.add(createMenuItem("Clear Results", null, e -> clearResults()));
        bar.add(runMenu);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setForeground(TEXT_PRIMARY);
        helpMenu.add(createMenuItem("Settings", null, e -> showSettings()));
        helpMenu.add(createMenuItem("About", null, e -> showAbout()));
        bar.add(helpMenu);

        return bar;
    }

    private JMenuItem createMenuItem(String label, String shortcut, ActionListener action) {
        JMenuItem item = new JMenuItem(label);
        item.setForeground(TEXT_PRIMARY);
        item.setBackground(PANEL_BG);
        item.addActionListener(action);
        if (shortcut != null) {
            KeyStroke ks = KeyStroke.getKeyStroke(shortcut.replace("Ctrl", "control"));
            if (ks != null) item.setAccelerator(ks);
        }
        return item;
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        toolbar.setBackground(PANEL_BG);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, PANEL_BORDER),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        JButton btnRunAll = createStyledButton("Run All");
        btnRunAll.addActionListener(e -> runAllTests());
        btnRunAll.setToolTipText("Run all tests (F5)");

        JButton btnRunSelected = createStyledButton("Run Selected");
        btnRunSelected.addActionListener(e -> runSelectedAppTests());

        JButton btnRerunFailed = createStyledButton("Rerun Failed");
        btnRerunFailed.addActionListener(e -> rerunFailed());
        btnRerunFailed.setToolTipText("Rerun failed tests (Ctrl+R)");

        JButton btnSave = createStyledButton("Save Report");
        btnSave.addActionListener(e -> saveReport());
        btnSave.setToolTipText("Save HTML report (Ctrl+S)");

        JButton btnClear = createStyledButton("Clear");
        btnClear.addActionListener(e -> clearResults());

        JLabel timeoutLabel = new JLabel(" Timeout(s):");
        timeoutLabel.setForeground(TEXT_MUTED);
        timeoutSpinner = new JSpinner(new SpinnerNumberModel(30, 5, 600, 5));
        timeoutSpinner.setPreferredSize(new Dimension(60, 26));
        timeoutSpinner.addChangeListener(e -> testTimeoutSeconds = (int) timeoutSpinner.getValue());

        JButton btnLogs = createStyledButton("Open Logs");
        btnLogs.addActionListener(e -> openBuildLogs());

        JButton btnOpenFolder = createStyledButton("Open Folder");
        btnOpenFolder.addActionListener(e -> openTestSourceFolder());

        toolbar.add(btnRunAll);
        toolbar.add(btnRunSelected);
        toolbar.add(btnRerunFailed);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(btnSave);
        toolbar.add(btnClear);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(timeoutLabel);
        toolbar.add(timeoutSpinner);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(btnLogs);
        toolbar.add(btnOpenFolder);

        return toolbar;
    }

    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(BTN_DARK);
        btn.setForeground(TEXT_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorderPainted(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PANEL_BORDER),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(BTN_HOVER); }
            public void mouseExited(MouseEvent e) { btn.setBackground(BTN_DARK); }
        });
        return btn;
    }

    private JSplitPane createMainPanel() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setBackground(BG_DARK);
        split.setDividerLocation(280);
        split.setLeftComponent(createAppListPanel());
        split.setRightComponent(createRightPanel());
        return split;
    }

    private JPanel createAppListPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, PANEL_BORDER),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));

        JLabel header = new JLabel("  Applications");
        header.setForeground(TEXT_MUTED);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 11f));
        header.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
        header.setOpaque(true);
        header.setBackground(PANEL_BG);
        panel.add(header, BorderLayout.NORTH);

        appListModel = new DefaultListModel<>();
        appList = new JList<>(appListModel);
        appList.setCellRenderer(new AppListRenderer());
        appList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        appList.setBackground(PANEL_BG);
        appList.setForeground(TEXT_PRIMARY);
        appList.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        appList.addListSelectionListener(e -> onAppSelected());
        appList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) runSelectedAppTests();
            }
        });

        JScrollPane sp = new JScrollPane(appList);
        sp.setBorder(null);
        sp.getViewport().setBackground(PANEL_BG);
        panel.add(sp, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(BG_DARK);

        JSplitPane vsplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        vsplit.setBackground(BG_DARK);
        vsplit.setDividerLocation(320);
        vsplit.setResizeWeight(0.5);

        vsplit.setTopComponent(createTestTablePanel());
        vsplit.setBottomComponent(createBottomPanel());

        panel.add(vsplit, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTestTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(BG_DARK);

        String[] cols = {"", "Test Name", "Status", "Time", "Source"};
        testTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
            public Class<?> getColumnClass(int c) {
                if (c == 0) return String.class;
                return String.class;
            }
        };
        testTable = new JTable(testTableModel);
        testTable.setBackground(EDITOR_BG);
        testTable.setForeground(TEXT_PRIMARY);
        testTable.setSelectionBackground(TABLE_SELECTION);
        testTable.setSelectionForeground(Color.WHITE);
        testTable.setGridColor(PANEL_BORDER);
        testTable.setRowHeight(28);
        testTable.setShowGrid(true);
        testTable.setIntercellSpacing(new Dimension(1, 1));
        testTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        testTable.getTableHeader().setBackground(TABLE_HEADER_BG);
        testTable.getTableHeader().setForeground(TEXT_MUTED);
        testTable.getTableHeader().setFont(testTable.getFont().deriveFont(Font.BOLD, 11f));
        testTable.getColumnModel().getColumn(0).setMaxWidth(36);
        testTable.getColumnModel().getColumn(0).setMinWidth(36);
        testTable.getColumnModel().getColumn(2).setMaxWidth(100);
        testTable.getColumnModel().getColumn(3).setMaxWidth(100);
        testTable.getColumnModel().getColumn(4).setPreferredWidth(180);

        testTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showTestDetail();
            }
        });

        testTable.setDefaultRenderer(String.class, new TestTableCellRenderer());

        JScrollPane sp = new JScrollPane(testTable);
        sp.setBorder(BorderFactory.createLineBorder(PANEL_BORDER));
        sp.getViewport().setBackground(EDITOR_BG);
        panel.add(sp, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 4, 0));
        panel.setBackground(BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        panel.add(createDetailPanel());
        panel.add(createConsolePanel());

        return panel;
    }

    private JPanel createDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createLineBorder(PANEL_BORDER));

        JLabel header = new JLabel("  Test Details");
        header.setForeground(TEXT_MUTED);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 11f));
        header.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
        panel.add(header, BorderLayout.NORTH);

        detailArea = new JTextArea();
        detailArea.setBackground(CONSOLE_BG);
        detailArea.setForeground(TEXT_PRIMARY);
        detailArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        detailArea.setEditable(false);
        detailArea.setMargin(new Insets(8, 8, 8, 8));
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);

        JScrollPane sp = new JScrollPane(detailArea);
        sp.setBorder(null);
        panel.add(sp, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createConsolePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createLineBorder(PANEL_BORDER));

        JLabel header = new JLabel("  Output Console");
        header.setForeground(TEXT_MUTED);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 11f));
        header.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
        panel.add(header, BorderLayout.NORTH);

        consoleOutput = new JTextArea();
        consoleOutput.setBackground(CONSOLE_BG);
        consoleOutput.setForeground(CONSOLE_TEXT);
        consoleOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        consoleOutput.setEditable(false);
        consoleOutput.setMargin(new Insets(8, 8, 8, 8));

        JScrollPane sp = new JScrollPane(consoleOutput);
        sp.setBorder(null);
        panel.add(sp, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(0, 0));
        bar.setBackground(PANEL_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, PANEL_BORDER),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftPanel.setBackground(PANEL_BG);

        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(200, 14));
        progressBar.setBackground(PROGRESS_BG);
        progressBar.setForeground(BG_ACCENT);
        progressBar.setStringPainted(false);
        progressBar.setVisible(false);

        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(TEXT_MUTED);
        statusLabel.setFont(statusLabel.getFont().deriveFont(11f));

        leftPanel.add(progressBar);
        leftPanel.add(statusLabel);

        timeLabel = new JLabel("0 tests | 0 passed | 0 failed | 0 skipped | 0.0s");
        timeLabel.setForeground(TEXT_MUTED);
        timeLabel.setFont(timeLabel.getFont().deriveFont(11f));

        bar.add(leftPanel, BorderLayout.CENTER);
        bar.add(timeLabel, BorderLayout.EAST);

        return bar;
    }

    // =====================================================================
    // RENDERERS
    // =====================================================================

    class AppListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            if (value instanceof AppInfo) {
                AppInfo app = (AppInfo) value;
                String icon = app.overallStatus.icon();
                Color col = app.overallStatus.color();
                label.setText("  " + icon + "  " + app.name + " (" + app.language + ")");
                label.setForeground(isSelected ? Color.WHITE : TEXT_PRIMARY);
                label.setFont(label.getFont().deriveFont(Font.PLAIN, 12f));
            }
            label.setOpaque(true);
            label.setBackground(isSelected ? BG_ACCENT : PANEL_BG);
            label.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
            return label;
        }
    }

    class TestTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            label.setOpaque(true);
            if (column == 0) {
                if (row < testTableModel.getRowCount()) {
                    String status = (String) testTableModel.getValueAt(row, 2);
                    if (status != null) {
                        TestStatus ts = parseStatus(status);
                        label.setText(ts.icon());
                        label.setForeground(ts.color());
                    }
                }
                label.setHorizontalAlignment(SwingConstants.CENTER);
            }
            if (isSelected) {
                label.setBackground(TABLE_SELECTION);
                label.setForeground(Color.WHITE);
            } else {
                label.setBackground(row % 2 == 0 ? EDITOR_BG : TABLE_ROW_ALT);
                label.setForeground(TEXT_PRIMARY);
            }
            label.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            return label;
        }
    }

    private TestStatus parseStatus(String s) {
        if (s == null) return TestStatus.PENDING;
        switch (s) {
            case "PASS": return TestStatus.PASS;
            case "FAIL": return TestStatus.FAIL;
            case "SKIP": return TestStatus.SKIP;
            case "RUNNING": return TestStatus.RUNNING;
            default: return TestStatus.PENDING;
        }
    }

    // =====================================================================
    // EVENT HANDLERS
    // =====================================================================

    private void onAppSelected() {
        AppInfo selected = appList.getSelectedValue();
        if (selected == null) return;
        loadTestResults(selected);
    }

    private void loadTestResults(AppInfo app) {
        testTableModel.setRowCount(0);
        for (TestResult tr : app.tests) {
            testTableModel.addRow(new Object[]{
                    tr.status.icon(),
                    tr.name,
                    tr.status.name(),
                    formatTime(tr.elapsedMs),
                    tr.file
            });
        }
    }

    private void showTestDetail() {
        int row = testTable.getSelectedRow();
        if (row < 0) return;
        AppInfo selected = appList.getSelectedValue();
        if (selected == null || row >= selected.tests.size()) return;
        TestResult tr = selected.tests.get(row);

        StringBuilder sb = new StringBuilder();
        sb.append("Test: ").append(tr.name).append("\n");
        sb.append("File: ").append(tr.file).append("\n");
        sb.append("Status: ").append(tr.status.name()).append("\n");
        sb.append("Time: ").append(formatTime(tr.elapsedMs)).append("\n");
        sb.append("Exit Code: ").append(tr.exitCode).append("\n");
        sb.append("\n--- STDOUT ---\n");
        sb.append(tr.output.isEmpty() ? "(empty)" : tr.output);
        if (tr.errorOutput != null && !tr.errorOutput.isEmpty()) {
            sb.append("\n\n--- STDERR ---\n");
            sb.append(tr.errorOutput);
        }
        detailArea.setText(sb.toString());
        detailArea.setCaretPosition(0);
    }

    // =====================================================================
    // TEST EXECUTION
    // =====================================================================

    private void runAllTests() {
        if (running) return;
        List<AppInfo> toRun = new ArrayList<>(discoveredApps);
        runTests(toRun);
    }

    private void runSelectedAppTests() {
        if (running) return;
        AppInfo selected = appList.getSelectedValue();
        if (selected == null) {
            statusLabel.setText("No application selected");
            return;
        }
        runTests(Collections.singletonList(selected));
    }

    private void rerunFailed() {
        if (running) return;
        List<AppInfo> toRun = new ArrayList<>();
        for (AppInfo app : discoveredApps) {
            boolean hasFailed = app.tests.stream()
                    .anyMatch(t -> t.status == TestStatus.FAIL);
            if (hasFailed) toRun.add(app);
        }
        if (toRun.isEmpty()) {
            statusLabel.setText("No failed tests to rerun");
            return;
        }
        for (AppInfo app : toRun) {
            app.tests.removeIf(t -> t.status == TestStatus.FAIL);
        }
        runTests(toRun);
    }

    private void runTests(List<AppInfo> apps) {
        running = true;
        totalStartTime = System.currentTimeMillis();
        int totalTests = apps.stream().mapToInt(a -> a.tests.size()).sum();
        progressBar.setValue(0);
        progressBar.setMaximum(totalTests);
        progressBar.setVisible(true);
        consoleOutput.setText("");
        statusLabel.setText("Running " + totalTests + " test(s)...");
        log("=== TestBench Run Started " + now() + " ===");

        for (AppInfo app : apps) {
            for (TestResult tr : app.tests) {
                tr.status = TestStatus.PENDING;
            }
        }
        refreshAppList();

        SwingWorker<Integer, Object> worker = new SwingWorker<Integer, Object>() {
            int pass = 0, fail = 0, skip = 0;

            @Override
            protected Integer doInBackground() {
                for (AppInfo app : apps) {
                    publish(app);
                    for (TestResult tr : app.tests) {
                        tr.status = TestStatus.RUNNING;
                        publish(tr);
                        executeTest(tr, app);
                        if (tr.status == TestStatus.PASS) pass++;
                        else if (tr.status == TestStatus.FAIL) fail++;
                        else skip++;
                        publish(tr);
                        progressBar.setValue(progressBar.getValue() + 1);
                    }
                    app.overallStatus = computeOverall(app);
                    publish(app);
                }
                return pass;
            }

            @Override
            protected void process(List<Object> chunks) {
                for (Object obj : chunks) {
                    if (obj instanceof AppInfo) {
                        appList.repaint();
                        AppInfo a = (AppInfo) obj;
                        loadTestResults(a);
                    } else if (obj instanceof TestResult) {
                        refreshTableForCurrentApp();
                    }
                }
            }

            @Override
            protected void done() {
                running = false;
                progressBar.setVisible(false);
                long elapsed = System.currentTimeMillis() - totalStartTime;
                int total = pass + fail + skip;
                timeLabel.setText(total + " tests | " + pass + " passed | "
                        + fail + " failed | " + skip + " skipped | "
                        + formatTime(elapsed));
                statusLabel.setText("Run complete — " + total + " test(s) executed");
                log("=== Run complete: " + pass + " passed, " + fail + " failed, "
                        + skip + " skipped in " + formatTime(elapsed) + " ===");
                appList.repaint();
                refreshAppList();
            }
        };
        worker.execute();
    }

    private void executeTest(TestResult tr, AppInfo app) {
        log("Running: " + tr.name + " (" + app.name + ")");
        String root = findRoot();
        ProcessBuilder pb = null;

        String lang = app.language != null ? app.language.toLowerCase() : "";
        Path file = Paths.get(tr.file);

        try {
            if (lang.contains("java") && tr.file.endsWith(".java")) {
                pb = buildJavaCommand(tr, app, root);
            } else if (lang.contains("python") && tr.file.endsWith(".py")) {
                pb = buildPythonCommand(tr, app, root);
            } else if (tr.file.endsWith("package.json")) {
                pb = buildNodeCommand(tr, app, root);
            } else {
                tr.status = TestStatus.SKIP;
                tr.output = "Unsupported test type or missing runner";
                log("  SKIP: unsupported test type");
                return;
            }

            if (pb == null) {
                tr.status = TestStatus.SKIP;
                tr.output = "Could not build command for test";
                log("  SKIP: command build failed");
                return;
            }

            pb.redirectErrorStream(false);
            pb.directory(new File(root));
            Process proc = pb.start();
            boolean finished = proc.waitFor(testTimeoutSeconds, TimeUnit.SECONDS);

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    stdout.append(line).append("\n");
                }
            }
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(proc.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    stderr.append(line).append("\n");
                }
            }

            tr.output = stdout.toString().trim();
            tr.errorOutput = stderr.toString().trim();
            tr.exitCode = finished ? proc.exitValue() : -1;

            if (!finished) {
                proc.destroyForcibly();
                tr.status = TestStatus.FAIL;
                tr.output = "TIMEOUT after " + testTimeoutSeconds + "s";
                log("  TIMEOUT: " + tr.name);
            } else if (tr.exitCode == 0) {
                tr.status = TestStatus.PASS;
                log("  PASS: " + tr.name);
            } else {
                tr.status = TestStatus.FAIL;
                log("  FAIL: " + tr.name + " (exit=" + tr.exitCode + ")");
            }

            if (tr.output.contains("[SKIP]") || tr.output.contains("SKIP")) {
                tr.status = TestStatus.SKIP;
            }

        } catch (Exception e) {
            tr.status = TestStatus.FAIL;
            tr.output = "Error executing test: " + e.getMessage();
            tr.errorOutput = e.getClass().getName();
            log("  ERROR: " + tr.name + " — " + e.getMessage());
        }
    }

    private ProcessBuilder buildJavaCommand(TestResult tr, AppInfo app, String root) {
        Path srcDir = Paths.get(root, app.source);
        Path buildDir = Paths.get(root, "build", "apps", app.slug);
        Path javaFile = Paths.get(tr.file);

        try { Files.createDirectories(buildDir); } catch (IOException ignored) {}

        ProcessBuilder compile = new ProcessBuilder("javac", "-d", buildDir.toString(),
                javaFile.toString());
        compile.directory(srcDir.toFile());
        try {
            Process cproc = compile.start();
            boolean ok = cproc.waitFor(15, TimeUnit.SECONDS);
            if (!ok || cproc.exitValue() != 0) {
                StringBuilder err = new StringBuilder();
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(cproc.getErrorStream()))) {
                    String l; while ((l = r.readLine()) != null) err.append(l).append("\n");
                }
                tr.status = TestStatus.SKIP;
                tr.output = "Compilation failed:\n" + err;
                return null;
            }
        } catch (Exception e) {
            tr.status = TestStatus.SKIP;
            tr.output = "Compilation error: " + e.getMessage();
            return null;
        }

        String className = javaFile.getFileName().toString().replace(".java", "");
        return new ProcessBuilder("java", "-cp", buildDir.toString(), className);
    }

    private ProcessBuilder buildPythonCommand(TestResult tr, AppInfo app, String root) {
        return new ProcessBuilder("python3", tr.file);
    }

    private ProcessBuilder buildNodeCommand(TestResult tr, AppInfo app, String root) {
        Path pkgDir = Paths.get(root, app.source);
        return new ProcessBuilder("npm", "test");
    }

    private TestStatus computeOverall(AppInfo app) {
        if (app.tests.isEmpty()) return TestStatus.SKIP;
        boolean anyFail = app.tests.stream().anyMatch(t -> t.status == TestStatus.FAIL);
        if (anyFail) return TestStatus.FAIL;
        boolean anyPass = app.tests.stream().anyMatch(t -> t.status == TestStatus.PASS);
        if (anyPass) return TestStatus.PASS;
        return TestStatus.SKIP;
    }

    // =====================================================================
    // SAVE REPORT
    // =====================================================================

    private void saveReport() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Test Report");
        fc.setSelectedFile(new File("testbench-report.html"));
        fc.setFileFilter(new FileNameExtensionFilter("HTML files", "html", "htm"));
        if (fc.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        if (!file.getName().endsWith(".html")) {
            file = new File(file.getAbsolutePath() + ".html");
        }

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">");
        html.append("<title>TestBench Report</title>");
        html.append("<style>");
        html.append("body{background:#1a1a2e;color:#e0e0e0;font-family:monospace;margin:20px}");
        html.append("h1{color:#0f3460}");
        html.append("table{border-collapse:collapse;width:100%}");
        html.append("th{background:#0f3460;color:#fff;padding:8px 12px;text-align:left}");
        html.append("td{border-bottom:1px solid #282846;padding:6px 12px}");
        html.append("tr:nth-child(even){background:#141428}");
        html.append(".pass{color:#3cb55a;font-weight:bold}");
        html.append(".fail{color:#c83c3c;font-weight:bold}");
        html.append(".skip{color:#8c8ca0}");
        html.append(".summary{margin:16px 0;padding:12px;background:#16213e;border-radius:6px}");
        html.append("</style></head><body>");
        html.append("<h1>TestBench Report</h1>");
        html.append("<p>Generated: ").append(now()).append("</p>");

        int totalP = 0, totalF = 0, totalS = 0;
        for (AppInfo app : discoveredApps) {
            for (TestResult tr : app.tests) {
                if (tr.status == TestStatus.PASS) totalP++;
                else if (tr.status == TestStatus.FAIL) totalF++;
                else totalS++;
            }
        }
        html.append("<div class='summary'>");
        html.append("Total: <b>").append(totalP + totalF + totalS).append("</b> | ");
        html.append("Passed: <b class='pass'>").append(totalP).append("</b> | ");
        html.append("Failed: <b class='fail'>").append(totalF).append("</b> | ");
        html.append("Skipped: <b class='skip'>").append(totalS).append("</b>");
        html.append("</div>");

        for (AppInfo app : discoveredApps) {
            if (app.tests.isEmpty()) continue;
            html.append("<h2>").append(escHtml(app.name)).append(" (").append(app.language).append(")</h2>");
            html.append("<table><tr><th>Status</th><th>Test</th><th>Time</th><th>File</th></tr>");
            for (TestResult tr : app.tests) {
                String cls = tr.status.name().toLowerCase();
                html.append("<tr><td class='").append(cls).append("'>").append(tr.status.icon())
                        .append(" ").append(tr.status.name()).append("</td>");
                html.append("<td>").append(escHtml(tr.name)).append("</td>");
                html.append("<td>").append(formatTime(tr.elapsedMs)).append("</td>");
                html.append("<td>").append(escHtml(tr.file)).append("</td></tr>");
            }
            html.append("</table>");
        }

        html.append("</body></html>");
        try {
            Files.write(file.toPath(), html.toString().getBytes(StandardCharsets.UTF_8));
            log("Report saved to: " + file.getAbsolutePath());
            statusLabel.setText("Report saved to " + file.getName());
        } catch (IOException e) {
            log("Error saving report: " + e.getMessage());
        }
    }

    // =====================================================================
    // LOGS & NAVIGATION
    // =====================================================================

    private void openBuildLogs() {
        AppInfo selected = appList.getSelectedValue();
        if (selected == null) {
            statusLabel.setText("Select an application first");
            return;
        }
        String root = findRoot();
        Path logDir = Paths.get(root, "logs");
        if (!Files.exists(logDir)) {
            try { Files.createDirectories(logDir); } catch (IOException ignored) {}
        }
        File logFile = logDir.resolve(selected.slug + ".log").toFile();
        if (logFile.exists()) {
            try {
                Desktop.getDesktop().open(logFile);
            } catch (IOException e) {
                log("Cannot open log file: " + e.getMessage());
                openInTextEditor(logFile);
            }
        } else {
            log("No log file found for " + selected.name);
            statusLabel.setText("No logs found for " + selected.name);
        }
    }

    private void openTestSourceFolder() {
        AppInfo selected = appList.getSelectedValue();
        if (selected == null) return;
        String root = findRoot();
        File dir = new File(root, selected.source);
        if (dir.exists()) {
            try {
                Desktop.getDesktop().open(dir);
            } catch (IOException e) {
                log("Cannot open folder: " + e.getMessage());
            }
        }
    }

    private void openInTextEditor(File file) {
        try {
            String editor = System.getenv().getOrDefault("EDITOR",
                    System.getenv().getOrDefault("VISUAL", "nano"));
            new ProcessBuilder(editor, file.getAbsolutePath()).start();
        } catch (Exception e) {
            log("Could not open editor: " + e.getMessage());
        }
    }

    // =====================================================================
    // CLEAR & REFRESH
    // =====================================================================

    private void clearResults() {
        for (AppInfo app : discoveredApps) {
            for (TestResult tr : app.tests) {
                tr.status = TestStatus.PENDING;
                tr.elapsedMs = 0;
                tr.output = "";
                tr.errorOutput = "";
            }
            app.overallStatus = TestStatus.PENDING;
        }
        testTableModel.setRowCount(0);
        detailArea.setText("");
        consoleOutput.setText("");
        timeLabel.setText("0 tests | 0 passed | 0 failed | 0 skipped | 0.0s");
        statusLabel.setText("Cleared");
        refreshAppList();
    }

    private void refreshAppList() {
        appListModel.clear();
        for (AppInfo app : discoveredApps) {
            appListModel.addElement(app);
        }
        appList.repaint();
    }

    private void refreshTableForCurrentApp() {
        AppInfo selected = appList.getSelectedValue();
        if (selected != null) {
            loadTestResults(selected);
            appList.repaint();
        }
    }

    // =====================================================================
    // DIALOGS
    // =====================================================================

    private void showSettings() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG_DARK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lbl = new JLabel("Test Timeout (seconds):");
        lbl.setForeground(TEXT_PRIMARY);
        panel.add(lbl, gbc);

        JSpinner spin = new JSpinner(new SpinnerNumberModel(testTimeoutSeconds, 5, 600, 5));
        spin.setPreferredSize(new Dimension(80, 26));
        gbc.gridx = 1;
        panel.add(spin, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        JLabel note = new JLabel("Timeout applies to each individual test execution.");
        note.setForeground(TEXT_MUTED);
        panel.add(note, gbc);

        int result = JOptionPane.showConfirmDialog(frame, panel, "Settings",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            testTimeoutSeconds = (int) spin.getValue();
            timeoutSpinner.setValue(testTimeoutSeconds);
            log("Timeout set to " + testTimeoutSeconds + "s");
        }
    }

    private void showAbout() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        JLabel title = new JLabel(APP_NAME);
        title.setForeground(BG_ACCENT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(title, BorderLayout.NORTH);

        JTextArea info = new JTextArea();
        info.setBackground(BG_DARK);
        info.setForeground(TEXT_PRIMARY);
        info.setEditable(false);
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        info.setFont(info.getFont().deriveFont(13f));
        info.setText("Version: " + VERSION + "\n"
                + "Java Swing Application\n"
                + "No external dependencies\n\n"
                + "Visual test runner for the Datacenter company.\n"
                + "Discovers apps from config/apps.json and runs\n"
                + "their tests with pass/fail/skip reporting.\n\n"
                + "Keyboard Shortcuts:\n"
                + "  F5 — Run All Tests\n"
                + "  Ctrl+S — Save Report\n"
                + "  Ctrl+R — Rerun Failed\n\n"
                + "Datacenter \u00A9 2026");
        panel.add(info, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(frame, panel, "About " + APP_NAME,
                JOptionPane.PLAIN_MESSAGE);
    }

    // =====================================================================
    // KEYBOARD SHORTCUTS
    // =====================================================================

    private void registerKeyboardShortcuts() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() != KeyEvent.KEY_PRESSED) return false;
            if (e.getKeyCode() == KeyEvent.VK_F5) {
                SwingUtilities.invokeLater(this::runAllTests);
                return true;
            }
            if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_S) {
                SwingUtilities.invokeLater(this::saveReport);
                return true;
            }
            if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_R) {
                SwingUtilities.invokeLater(this::rerunFailed);
                return true;
            }
            return false;
        });
    }

    // =====================================================================
    // UTILITIES
    // =====================================================================

    private void log(String msg) {
        String line = "[" + LocalTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + msg + "\n";
        SwingUtilities.invokeLater(() -> {
            consoleOutput.append(line);
            consoleOutput.setCaretPosition(consoleOutput.getDocument().getLength());
        });
    }

    private static String formatTime(long ms) {
        if (ms < 1000) return ms + "ms";
        return String.format("%.2fs", ms / 1000.0);
    }

    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
