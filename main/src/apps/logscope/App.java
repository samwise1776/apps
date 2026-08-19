import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.Timer;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class App extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final Color BG_DARK =
            new Color(26, 26, 46);

    private static final Color BG_ACCENT =
            new Color(15, 52, 96);

    private static final Color BG_BUTTON =
            new Color(22, 33, 62);

    private static final Color FG_TEXT =
            new Color(224, 224, 224);

    private static final Color FG_DIM =
            new Color(140, 140, 160);

    private static final Color CLR_ERROR =
            new Color(255, 80, 80);

    private static final Color CLR_WARN =
            new Color(255, 180, 50);

    private static final Color CLR_INFO =
            new Color(224, 224, 224);

    private static final Color CLR_DEBUG =
            new Color(120, 120, 140);

    private static final Color CLR_HIGHLIGHT =
            new Color(60, 80, 140);

    private static final String[] LOG_DIRS = {
        "logs", ".data/logs",
        "logs/apps", "logs/errors",
        "logs/system", "logs/build"
    };

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern(
                "yyyy-MM-dd HH:mm:ss"
            );

    private final LogTableModel tableModel;
    private final JTable logTable;
    private final TableRowSorter<LogTableModel> sorter;

    private final JTextField searchField;
    private final JComboBox<String> levelFilter;
    private final JComboBox<String> appFilter;
    private final JTextField dateFromField;
    private final JTextField dateToField;
    private final JLabel statusLabel;
    private final JLabel refreshLabel;

    private final Timer refreshTimer;
    private final List<LogEntry> allEntries;
    private final Set<String> knownApps;

    private long refreshIntervalMs = 5000;
    private boolean autoRefresh = true;


    /*
     * =========================================
     * MAIN
     * =========================================
     */

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            App app = new App();
            app.setVisible(true);
        });
    }


    /*
     * =========================================
     * CONSTRUCTOR
     * =========================================
     */

    public App() {
        super("LogScope - Datacenter Log Viewer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 800);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        allEntries = Collections.synchronizedList(
            new ArrayList<>()
        );
        knownApps = new TreeSet<>();

        tableModel = new LogTableModel();
        logTable = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        logTable.setRowSorter(sorter);

        searchField = new JTextField(20);
        levelFilter = new JComboBox<>(
            new String[]{
                "ALL", "ERROR", "WARN", "INFO", "DEBUG"
            }
        );
        appFilter = new JComboBox<>();
        appFilter.addItem("ALL");
        dateFromField = new JTextField(10);
        dateToField = new JTextField(10);
        statusLabel = new JLabel("Logs: 0");
        refreshLabel = new JLabel("Last refresh: never");

        refreshTimer = new Timer(
            (int) refreshIntervalMs,
            e -> performRefresh()
        );
        refreshTimer.setCoalesce(true);

        initUI();
        loadAllLogs();
        applyFilters();
        refreshTimer.start();
    }


    /*
     * =========================================
     * UI INITIALIZATION
     * =========================================
     */

    private void initUI() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(
            new Font("SansSerif", Font.BOLD, 14)
        );
        tabs.setBackground(BG_ACCENT);
        tabs.setForeground(FG_TEXT);

        tabs.addTab("Live View", createLiveViewTab());
        tabs.addTab("Statistics", createStatsTab());
        tabs.addTab("Settings", createSettingsTab());

        setContentPane(tabs);
        setupMenuBar();
        setupKeyBindings();
    }


    /*
     * =========================================
     * LIVE VIEW TAB
     * =========================================
     */

    private JPanel createLiveViewTab() {
        JPanel panel = new JPanel(
            new BorderLayout(0, 0)
        );
        panel.setBackground(BG_DARK);

        panel.add(
            createFilterPanel(),
            BorderLayout.NORTH
        );

        setupTable();
        JScrollPane tableScroll = new JScrollPane(
            logTable
        );
        tableScroll.setBorder(
            BorderFactory.createEmptyBorder()
        );
        tableScroll.getViewport().setBackground(
            BG_DARK
        );
        panel.add(tableScroll, BorderLayout.CENTER);

        panel.add(
            createStatusBar(),
            BorderLayout.SOUTH
        );

        return panel;
    }


    /*
     * =========================================
     * FILTER PANEL
     * =========================================
     */

    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 8, 6)
        );
        panel.setBackground(BG_ACCENT);
        panel.setBorder(
            new EmptyBorder(4, 8, 4, 8)
        );

        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setForeground(FG_TEXT);
        searchField.setBackground(BG_BUTTON);
        searchField.setForeground(FG_TEXT);
        searchField.setCaretColor(Color.WHITE);
        searchField.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    new Color(60, 80, 120)
                ),
                BorderFactory.createEmptyBorder(
                    4, 6, 4, 6
                )
            )
        );
        searchField.setToolTipText(
            "Text or regex search (Ctrl+F)"
        );
        searchField.getDocument()
            .addDocumentListener(
                new SimpleDocListener(
                    () -> applyFilters()
                )
            );

        JLabel levelLabel = new JLabel("Level:");
        levelLabel.setForeground(FG_TEXT);
        styleCombo(levelFilter);
        levelFilter.addActionListener(
            e -> applyFilters()
        );

        JLabel appLabel = new JLabel("App:");
        appLabel.setForeground(FG_TEXT);
        styleCombo(appFilter);
        appFilter.addActionListener(
            e -> applyFilters()
        );

        JLabel fromLabel = new JLabel("From:");
        fromLabel.setForeground(FG_TEXT);
        dateFromField.setBackground(BG_BUTTON);
        dateFromField.setForeground(FG_TEXT);
        dateFromField.setColumns(10);
        dateFromField.setToolTipText(
            "yyyy-MM-dd HH:mm:ss"
        );

        JLabel toLabel = new JLabel("To:");
        toLabel.setForeground(FG_TEXT);
        dateToField.setBackground(BG_BUTTON);
        dateToField.setForeground(FG_TEXT);
        dateToField.setColumns(10);
        dateToField.setToolTipText(
            "yyyy-MM-dd HH:mm:ss"
        );

        JButton applyDateBtn = createButton(
            "Apply Dates"
        );
        applyDateBtn.addActionListener(
            e -> applyFilters()
        );

        JButton resetBtn = createButton("Reset");
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            levelFilter.setSelectedIndex(0);
            appFilter.setSelectedIndex(0);
            dateFromField.setText("");
            dateToField.setText("");
            applyFilters();
        });

        panel.add(searchLabel);
        panel.add(searchField);
        panel.add(Box.createHorizontalStrut(12));
        panel.add(levelLabel);
        panel.add(levelFilter);
        panel.add(Box.createHorizontalStrut(12));
        panel.add(appLabel);
        panel.add(appFilter);
        panel.add(Box.createHorizontalStrut(12));
        panel.add(fromLabel);
        panel.add(dateFromField);
        panel.add(toLabel);
        panel.add(dateToField);
        panel.add(applyDateBtn);
        panel.add(Box.createHorizontalStrut(6));
        panel.add(resetBtn);

        return panel;
    }


    /*
     * =========================================
     * TABLE SETUP
     * =========================================
     */

    private void setupTable() {
        logTable.setModel(tableModel);
        logTable.setRowHeight(24);
        logTable.setBackground(BG_DARK);
        logTable.setForeground(FG_TEXT);
        logTable.setGridColor(
            new Color(40, 40, 60)
        );
        logTable.setSelectionBackground(CLR_HIGHLIGHT);
        logTable.setSelectionForeground(Color.WHITE);
        logTable.setFont(
            new Font("Monospaced", Font.PLAIN, 13)
        );
        logTable.setShowGrid(true);
        logTable.setIntercellSpacing(
            new Dimension(1, 1)
        );
        logTable.getTableHeader().setBackground(
            BG_ACCENT
        );
        logTable.getTableHeader().setForeground(
            FG_TEXT
        );
        logTable.getTableHeader().setFont(
            new Font("SansSerif", Font.BOLD, 13)
        );
        logTable.setAutoResizeMode(
            JTable.AUTO_RESIZE_LAST_COLUMN
        );

        logTable.getColumnModel()
            .getColumn(0).setPreferredWidth(60);
        logTable.getColumnModel()
            .getColumn(1).setPreferredWidth(160);
        logTable.getColumnModel()
            .getColumn(2).setPreferredWidth(100);
        logTable.getColumnModel()
            .getColumn(3).setPreferredWidth(600);

        logTable.setDefaultRenderer(
            Object.class,
            new LogCellRenderer()
        );

        logTable.addMouseListener(
            new MouseAdapter() {
                @Override
                public void mouseClicked(
                    MouseEvent e
                ) {
                    if (
                        e.getClickCount() == 2
                    ) {
                        openSelectedFile();
                    }
                }
            }
        );
    }


    /*
     * =========================================
     * STATISTICS TAB
     * =========================================
     */

    private JPanel createStatsTab() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_DARK);
        panel.setLayout(
            new BoxLayout(
                panel, BoxLayout.Y_AXIS
            )
        );
        panel.setBorder(
            new EmptyBorder(20, 20, 20, 20)
        );

        JLabel title = new JLabel("Log Statistics");
        title.setForeground(FG_TEXT);
        title.setFont(
            new Font(
                "SansSerif", Font.BOLD, 20
            )
        );
        title.setAlignmentX(
            Component.LEFT_ALIGNMENT
        );
        panel.add(title);
        panel.add(Box.createVerticalStrut(16));

        JPanel statsContent = new JPanel();
        statsContent.setBackground(BG_DARK);
        statsContent.setLayout(
            new BoxLayout(
                statsContent, BoxLayout.Y_AXIS
            )
        );
        statsContent.setAlignmentX(
            Component.LEFT_ALIGNMENT
        );

        JLabel levelTitle = new JLabel(
            "--- By Level ---"
        );
        levelTitle.setForeground(FG_DIM);
        levelTitle.setFont(
            new Font(
                "SansSerif", Font.BOLD, 15
            )
        );
        statsContent.add(levelTitle);
        statsContent.add(Box.createVerticalStrut(8));

        JPanel levelPanel = new JPanel(
            new GridLayout(4, 2, 8, 4)
        );
        levelPanel.setBackground(BG_DARK);
        levelPanel.setMaximumSize(
            new Dimension(500, 120)
        );
        levelPanel.setAlignmentX(
            Component.LEFT_ALIGNMENT
        );

        String[] levels = {
            "ERROR", "WARN", "INFO", "DEBUG"
        };
        Color[] levelColors = {
            CLR_ERROR, CLR_WARN, CLR_INFO, CLR_DEBUG
        };
        for (int i = 0; i < levels.length; i++) {
            JLabel lbl = new JLabel(
                "  " + levels[i] + ":"
            );
            lbl.setForeground(levelColors[i]);
            lbl.setFont(
                new Font(
                    "Monospaced", Font.BOLD, 14
                )
            );
            JLabel cnt = new JLabel("0");
            cnt.setName("count_" + levels[i]);
            cnt.setForeground(FG_TEXT);
            cnt.setFont(
                new Font(
                    "Monospaced", Font.PLAIN, 14
                )
            );
            levelPanel.add(lbl);
            levelPanel.add(cnt);
        }

        statsContent.add(levelPanel);
        statsContent.add(
            Box.createVerticalStrut(20)
        );

        JLabel appTitle = new JLabel(
            "--- By Application ---"
        );
        appTitle.setForeground(FG_DIM);
        appTitle.setFont(
            new Font(
                "SansSerif", Font.BOLD, 15
            )
        );
        statsContent.add(appTitle);
        statsContent.add(Box.createVerticalStrut(8));

        JPanel appPanel = new JPanel();
        appPanel.setBackground(BG_DARK);
        appPanel.setLayout(
            new BoxLayout(
                appPanel, BoxLayout.Y_AXIS
            )
        );
        appPanel.setAlignmentX(
            Component.LEFT_ALIGNMENT
        );
        appPanel.setName("appStatsPanel");
        statsContent.add(appPanel);

        JScrollPane scroll = new JScrollPane(
            statsContent
        );
        scroll.setBorder(
            BorderFactory.createEmptyBorder()
        );
        scroll.getViewport().setBackground(
            BG_DARK
        );
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }


    /*
     * =========================================
     * SETTINGS TAB
     * =========================================
     */

    private JPanel createSettingsTab() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_DARK);
        panel.setLayout(
            new BoxLayout(
                panel, BoxLayout.Y_AXIS
            )
        );
        panel.setBorder(
            new EmptyBorder(20, 40, 20, 40)
        );

        JLabel title = new JLabel("Settings");
        title.setForeground(FG_TEXT);
        title.setFont(
            new Font(
                "SansSerif", Font.BOLD, 20
            )
        );
        title.setAlignmentX(
            Component.LEFT_ALIGNMENT
        );
        panel.add(title);
        panel.add(Box.createVerticalStrut(20));

        JCheckBox autoRefreshBox = new JCheckBox(
            "Enable auto-refresh"
        );
        autoRefreshBox.setSelected(true);
        autoRefreshBox.setForeground(FG_TEXT);
        autoRefreshBox.setBackground(BG_DARK);
        autoRefreshBox.setFont(
            new Font(
                "SansSerif", Font.PLAIN, 14
            )
        );
        autoRefreshBox.setAlignmentX(
            Component.LEFT_ALIGNMENT
        );
        autoRefreshBox.addActionListener(e -> {
            autoRefresh = autoRefreshBox.isSelected();
            if (autoRefresh) {
                refreshTimer.start();
            } else {
                refreshTimer.stop();
            }
        });
        panel.add(autoRefreshBox);
        panel.add(Box.createVerticalStrut(12));

        JPanel intervalPanel = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 8, 0)
        );
        intervalPanel.setBackground(BG_DARK);
        intervalPanel.setMaximumSize(
            new Dimension(600, 40)
        );
        intervalPanel.setAlignmentX(
            Component.LEFT_ALIGNMENT
        );
        JLabel intervalLabel = new JLabel(
            "Refresh interval (ms):"
        );
        intervalLabel.setForeground(FG_TEXT);
        intervalLabel.setFont(
            new Font(
                "SansSerif", Font.PLAIN, 14
            )
        );
        SpinnerNumberModel spinnerModel =
            new SpinnerNumberModel(
                5000, 1000, 60000, 500
            );
        JSpinner intervalSpinner = new JSpinner(
            spinnerModel
        );
        intervalSpinner.setBackground(BG_BUTTON);
        intervalSpinner.setForeground(FG_TEXT);
        intervalSpinner.setFont(
            new Font(
                "SansSerif", Font.PLAIN, 14
            )
        );
        intervalSpinner.addChangeListener(e -> {
            refreshIntervalMs =
                (int) intervalSpinner.getValue();
            refreshTimer.setDelay(
                (int) refreshIntervalMs
            );
        });
        intervalPanel.add(intervalLabel);
        intervalPanel.add(intervalSpinner);
        panel.add(intervalPanel);
        panel.add(Box.createVerticalStrut(16));

        JLabel keysTitle = new JLabel(
            "Keyboard Shortcuts"
        );
        keysTitle.setForeground(FG_DIM);
        keysTitle.setFont(
            new Font(
                "SansSerif", Font.BOLD, 15
            )
        );
        keysTitle.setAlignmentX(
            Component.LEFT_ALIGNMENT
        );
        panel.add(keysTitle);
        panel.add(Box.createVerticalStrut(8));

        String[][] shortcuts = {
            {"Ctrl+F", "Focus search field"},
            {"Ctrl+R / F5", "Refresh logs"},
            {"Ctrl+E", "Export logs to file"},
            {"Ctrl+L", "Clear all logs"},
            {"Ctrl+A", "Select all log entries"},
            {"Double-click", "Open log file"},
            {"Ctrl+Q", "Quit application"}
        };

        for (String[] sc : shortcuts) {
            JPanel row = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 12, 2)
            );
            row.setBackground(BG_DARK);
            row.setMaximumSize(
                new Dimension(600, 30)
            );
            row.setAlignmentX(
                Component.LEFT_ALIGNMENT
            );
            JLabel key = new JLabel(sc[0]);
            key.setForeground(FG_TEXT);
            key.setFont(
                new Font(
                    "Monospaced", Font.BOLD, 13
                )
            );
            key.setPreferredSize(
                new Dimension(140, 24)
            );
            JLabel desc = new JLabel(sc[1]);
            desc.setForeground(FG_DIM);
            desc.setFont(
                new Font(
                    "SansSerif", Font.PLAIN, 13
                )
            );
            row.add(key);
            row.add(desc);
            panel.add(row);
        }

        panel.add(
            Box.createVerticalStrut(20)
        );

        JLabel aboutBtn = new JLabel(
            "LogScope v1.0 - Datacenter Log Viewer"
        );
        aboutBtn.setForeground(FG_DIM);
        aboutBtn.setFont(
            new Font(
                "SansSerif", Font.ITALIC, 12
            )
        );
        aboutBtn.setAlignmentX(
            Component.LEFT_ALIGNMENT
        );
        panel.add(aboutBtn);

        return panel;
    }


    /*
     * =========================================
     * STATUS BAR
     * =========================================
     */

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 12, 4)
        );
        bar.setBackground(BG_ACCENT);
        bar.setBorder(
            BorderFactory.createMatteBorder(
                1, 0, 0, 0,
                new Color(40, 50, 80)
            )
        );

        statusLabel.setForeground(FG_TEXT);
        statusLabel.setFont(
            new Font(
                "SansSerif", Font.PLAIN, 12
            )
        );

        refreshLabel.setForeground(FG_DIM);
        refreshLabel.setFont(
            new Font(
                "SansSerif", Font.PLAIN, 12
            )
        );

        JLabel hint = new JLabel(
            "  Double-click row to open file"
        );
        hint.setForeground(FG_DIM);
        hint.setFont(
            new Font(
                "SansSerif", Font.ITALIC, 11
            )
        );

        bar.add(statusLabel);
        bar.add(
            Box.createHorizontalStrut(20)
        );
        bar.add(refreshLabel);
        bar.add(
            Box.createHorizontalGlue()
        );
        bar.add(hint);

        return bar;
    }


    /*
     * =========================================
     * MENU BAR
     * =========================================
     */

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(BG_ACCENT);
        menuBar.setForeground(FG_TEXT);

        JMenu fileMenu = new JMenu("File");
        fileMenu.setForeground(FG_TEXT);

        fileMenu.add(createMenuItem(
            "Refresh", "Ctrl+R",
            e -> performRefresh()
        ));
        fileMenu.add(createMenuItem(
            "Export...", "Ctrl+E",
            e -> exportLogs()
        ));
        fileMenu.addSeparator();
        fileMenu.add(createMenuItem(
            "Open Selected File", "Double-click",
            e -> openSelectedFile()
        ));
        fileMenu.addSeparator();
        fileMenu.add(createMenuItem(
            "Clear Logs", "Ctrl+L",
            e -> clearLogs()
        ));
        fileMenu.addSeparator();
        fileMenu.add(createMenuItem(
            "Quit", "Ctrl+Q",
            e -> {
                refreshTimer.stop();
                dispose();
                System.exit(0);
            }
        ));

        JMenu viewMenu = new JMenu("View");
        viewMenu.setForeground(FG_TEXT);
        viewMenu.add(createMenuItem(
            "Find", "Ctrl+F",
            e -> searchField.requestFocusInWindow()
        ));
        viewMenu.add(createMenuItem(
            "Select All", "Ctrl+A",
            e -> logTable.selectAll()
        ));

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setForeground(FG_TEXT);
        helpMenu.add(createMenuItem(
            "About LogScope", null,
            e -> showAbout()
        ));

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }


    /*
     * =========================================
     * KEY BINDINGS
     * =========================================
     */

    private void setupKeyBindings() {
        InputMap im = rootPane.getInputMap(
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        ActionMap am = rootPane.getActionMap();

        im.put(
            KeyStroke.getKeyStroke(
                KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK
            ),
            "find"
        );
        am.put("find", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.requestFocusInWindow();
                searchField.selectAll();
            }
        });

        im.put(
            KeyStroke.getKeyStroke(
                KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK
            ),
            "refresh"
        );
        im.put(
            KeyStroke.getKeyStroke(
                KeyEvent.VK_F5, 0
            ),
            "refresh_f5"
        );
        AbstractAction refreshAction =
            new AbstractAction() {
                @Override
                public void actionPerformed(
                    ActionEvent e
                ) {
                    performRefresh();
                }
            };
        am.put("refresh", refreshAction);
        am.put("refresh_f5", refreshAction);

        im.put(
            KeyStroke.getKeyStroke(
                KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK
            ),
            "export"
        );
        am.put("export", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportLogs();
            }
        });

        im.put(
            KeyStroke.getKeyStroke(
                KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK
            ),
            "clear"
        );
        am.put("clear", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearLogs();
            }
        });

        im.put(
            KeyStroke.getKeyStroke(
                KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK
            ),
            "quit"
        );
        am.put("quit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshTimer.stop();
                dispose();
                System.exit(0);
            }
        });

        im.put(
            KeyStroke.getKeyStroke(
                KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK
            ),
            "selectall"
        );
        am.put("selectall", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logTable.selectAll();
            }
        });
    }


    /*
     * =========================================
     * LOG LOADING
     * =========================================
     */

    private void loadAllLogs() {
        SwingWorker<List<LogEntry>, Void> worker =
            new SwingWorker<>() {
                @Override
                protected List<LogEntry> doInBackground() {
                    List<LogEntry> entries =
                        new ArrayList<>();

                    String root = findProjectRoot();

                    scanDirectory(
                        new File(root, "logs"),
                        entries
                    );

                    for (String dir : LOG_DIRS) {
                        scanDirectory(
                            new File(root, dir),
                            entries
                        );
                    }

                    scanDirectory(
                        new File(root,
                            ".data/logs"),
                        entries
                    );

                    scanDirectory(
                        new File(root, "logs/apps"),
                        entries
                    );

                    scanDirectory(
                        new File(root, "logs/errors"),
                        entries
                    );

                    scanDirectory(
                        new File(root,
                            "logs/system"),
                        entries
                    );

                    scanDirectory(
                        new File(root, "logs/build"),
                        entries
                    );

                    scanUserHomeLogs(entries);

                    entries.sort(
                        (a, b) -> b.timestamp.compareTo(
                            a.timestamp
                        )
                    );

                    return entries;
                }

                @Override
                protected void done() {
                    try {
                        List<LogEntry> loaded =
                            get();
                        allEntries.clear();
                        allEntries.addAll(loaded);

                        knownApps.clear();
                        for (LogEntry le : loaded) {
                            if (
                                le.appName != null
                                    && !le.appName
                                        .isEmpty()
                            ) {
                                knownApps.add(
                                    le.appName
                                );
                            }
                        }

                        refreshAppFilter();
                        applyFilters();
                        updateStats();
                        updateStatus(
                            "Logs loaded: "
                                + loaded.size()
                        );
                    } catch (Exception ex) {
                        updateStatus(
                            "Error loading logs: "
                                + ex.getMessage()
                        );
                    }
                }
            };

        worker.execute();
    }


    /*
     * =========================================
     * DIRECTORY SCANNING
     * =========================================
     */

    private void scanDirectory(
        File dir,
        List<LogEntry> entries
    ) {
        if (
            dir == null || !dir.exists()
                || !dir.isDirectory()
        ) {
            return;
        }

        File[] files = dir.listFiles(
            (d, name) ->
                name.endsWith(".log")
                    || name.endsWith(".txt")
                    || name.endsWith(".out")
        );

        if (files == null) {
            return;
        }

        for (File f : files) {
            readFile(f, entries);
        }
    }


    private void scanUserHomeLogs(
        List<LogEntry> entries
    ) {
        String home = System.getProperty(
            "user.home"
        );
        if (home == null) {
            return;
        }

        File logDir = new File(home, ".data/logs");
        scanDirectory(logDir, entries);
    }


    /*
     * =========================================
     * FILE READING
     * =========================================
     */

    private void readFile(
        File file,
        List<LogEntry> entries
    ) {
        try (BufferedReader br = Files.newBufferedReader(
            file.toPath()
        )) {
            String sourceName = file.getName();
            String line;
            int lineNum = 0;

            while ((line = br.readLine()) != null) {
                lineNum++;
                LogEntry entry = parseLogLine(
                    line, sourceName, file, lineNum
                );
                if (entry != null) {
                    entries.add(entry);
                }
            }
        } catch (IOException e) {
            LogEntry errEntry = new LogEntry();
            errEntry.level = "ERROR";
            errEntry.message =
                "Failed to read "
                    + file.getName()
                    + ": " + e.getMessage();
            errEntry.sourceFile = file.getAbsolutePath();
            errEntry.lineNumber = 0;
            errEntry.timestamp = LocalDateTime.now();
            errEntry.appName = "LogScope";
            entries.add(errEntry);
        }
    }


    /*
     * =========================================
     * LOG PARSING
     * =========================================
     */

    private LogEntry parseLogLine(
        String line,
        String sourceName,
        File file,
        int lineNum
    ) {
        if (line.trim().isEmpty()) {
            return null;
        }

        LogEntry entry = new LogEntry();
        entry.rawLine = line;
        entry.sourceFile = file.getAbsolutePath();
        entry.lineNumber = lineNum;
        entry.appName = extractAppName(sourceName);

        String trimmed = line.trim();

        if (
            trimmed.startsWith("[")
                && trimmed.length() > 20
        ) {
            int bracketEnd = trimmed.indexOf(']');
            if (bracketEnd > 0 && bracketEnd < 25) {
                String tsPart = trimmed.substring(
                    1, bracketEnd
                );
                try {
                    entry.timestamp =
                        LocalDateTime.parse(
                            tsPart, TS_FMT
                        );
                } catch (
                    DateTimeParseException ex
                ) {
                    entry.timestamp =
                        LocalDateTime.now();
                }

                String rest = trimmed.substring(
                    bracketEnd + 1
                ).trim();
                entry.level = detectLevel(rest);
                entry.message = rest;
                return entry;
            }
        }

        entry.timestamp = LocalDateTime.now();
        entry.level = detectLevel(trimmed);
        entry.message = trimmed;

        return entry;
    }


    /*
     * =========================================
     * LEVEL DETECTION
     * =========================================
     */

    private String detectLevel(String text) {
        String upper = text.toUpperCase(Locale.ROOT);

        if (
            upper.contains("ERROR")
                || upper.contains("EXCEPTION")
                || upper.contains("FATAL")
                || upper.contains("CRITICAL")
                || upper.contains("FAIL")
        ) {
            return "ERROR";
        }

        if (
            upper.contains("WARN")
                || upper.contains("WARNING")
        ) {
            return "WARN";
        }

        if (
            upper.contains("DEBUG")
                || upper.contains("TRACE")
        ) {
            return "DEBUG";
        }

        if (
            upper.contains("INFO")
                || upper.contains("STARTED")
                || upper.contains("REPAIR OK")
                || upper.contains("LAUNCH")
                || upper.contains("EXIT")
                || upper.contains("VERSION")
        ) {
            return "INFO";
        }

        return "INFO";
    }


    /*
     * =========================================
     * APP NAME EXTRACTION
     * =========================================
     */

    private String extractAppName(String fileName) {
        if (fileName == null) {
            return "unknown";
        }

        String name = fileName;
        int dotIdx = name.lastIndexOf('.');
        if (dotIdx > 0) {
            name = name.substring(0, dotIdx);
        }
        return name;
    }


    /*
     * =========================================
     * PROJECT ROOT
     * =========================================
     */

    private String findProjectRoot() {
        String dir = System.getProperty(
            "user.dir"
        );
        if (dir != null) {
            File d = new File(dir);
            while (d != null) {
                if (
                    new File(d, "logs").exists()
                        || new File(
                            d, ".data"
                        ).exists()
                ) {
                    return d.getAbsolutePath();
                }
                d = d.getParentFile();
            }
        }

        String home = System.getProperty(
            "user.home"
        );
        if (home != null) {
            File candidate = new File(
                home, "Data"
            );
            if (candidate.exists()) {
                return candidate.getAbsolutePath();
            }
        }

        return dir != null ? dir : ".";
    }


    /*
     * =========================================
     * FILTERING
     * =========================================
     */

    private void applyFilters() {
        SwingWorker<List<LogEntry>, Void> worker =
            new SwingWorker<>() {
                @Override
                protected List<LogEntry> doInBackground() {
                    return computeFiltered();
                }

                @Override
                protected void done() {
                    try {
                        List<LogEntry> filtered =
                            get();
                        tableModel.setData(filtered);
                        updateStatus(
                            "Showing "
                                + filtered.size()
                                + " of "
                                + allEntries.size()
                                + " entries"
                        );
                    } catch (Exception ex) {
                        updateStatus(
                            "Filter error: "
                                + ex.getMessage()
                        );
                    }
                }
            };

        worker.execute();
    }


    private List<LogEntry> computeFiltered() {
        String searchText =
            searchField.getText().trim()
                .toLowerCase(Locale.ROOT);
        String selectedLevel =
            (String) levelFilter.getSelectedItem();
        String selectedApp =
            (String) appFilter.getSelectedItem();

        String fromStr =
            dateFromField.getText().trim();
        String toStr =
            dateToField.getText().trim();

        LocalDateTime fromDate = null;
        LocalDateTime toDate = null;

        if (!fromStr.isEmpty()) {
            try {
                fromDate = LocalDateTime.parse(
                    fromStr, TS_FMT
                );
            } catch (
                DateTimeParseException e
            ) {
                fromDate = null;
            }
        }

        if (!toStr.isEmpty()) {
            try {
                toDate = LocalDateTime.parse(
                    toStr, TS_FMT
                );
            } catch (
                DateTimeParseException e
            ) {
                toDate = null;
            }
        }

        Pattern searchPattern = null;
        if (!searchText.isEmpty()) {
            try {
                searchPattern = Pattern.compile(
                    searchText,
                    Pattern.CASE_INSENSITIVE
                );
            } catch (
                PatternSyntaxException e
            ) {
                searchPattern = Pattern.compile(
                    Pattern.quote(searchText),
                    Pattern.CASE_INSENSITIVE
                );
            }
        }

        List<LogEntry> result = new ArrayList<>();

        synchronized (allEntries) {
            for (LogEntry entry : allEntries) {
                if (
                    !"ALL".equals(selectedLevel)
                        && !entry.level.equals(
                            selectedLevel
                        )
                ) {
                    continue;
                }

                if (
                    !"ALL".equals(selectedApp)
                        && !selectedApp.equals(
                            entry.appName
                        )
                ) {
                    continue;
                }

                if (
                    fromDate != null
                        && entry.timestamp.isBefore(
                            fromDate
                        )
                ) {
                    continue;
                }

                if (
                    toDate != null
                        && entry.timestamp.isAfter(
                            toDate
                        )
                ) {
                    continue;
                }

                if (searchPattern != null) {
                    if (
                        !searchPattern
                            .matcher(entry.message)
                            .find()
                            && !searchPattern
                                .matcher(
                                    entry.sourceFile
                                )
                                .find()
                    ) {
                        continue;
                    }
                }

                result.add(entry);
            }
        }

        return result;
    }


    /*
     * =========================================
     * APP FILTER REFRESH
     * =========================================
     */

    private void refreshAppFilter() {
        String prev =
            (String) appFilter.getSelectedItem();
        appFilter.removeAllItems();
        appFilter.addItem("ALL");
        for (String app : knownApps) {
            appFilter.addItem(app);
        }
        if (prev != null) {
            appFilter.setSelectedItem(prev);
        }
    }


    /*
     * =========================================
     * STATISTICS UPDATE
     * =========================================
     */

    private void updateStats() {
        Map<String, Integer> levelCounts =
            new LinkedHashMap<>();
        Map<String, Integer> appCounts =
            new TreeMap<>();

        levelCounts.put("ERROR", 0);
        levelCounts.put("WARN", 0);
        levelCounts.put("INFO", 0);
        levelCounts.put("DEBUG", 0);

        synchronized (allEntries) {
            for (LogEntry entry : allEntries) {
                levelCounts.merge(
                    entry.level, 1, Integer::sum
                );
                if (
                    entry.appName != null
                        && !entry.appName.isEmpty()
                ) {
                    appCounts.merge(
                        entry.appName,
                        1,
                        Integer::sum
                    );
                }
            }
        }

        SwingUtilities.invokeLater(
            this::updateStatsPanel
        );
    }


    private void updateStatsPanel() {
        Map<String, Integer> levelCounts =
            new LinkedHashMap<>();
        Map<String, Integer> appCounts =
            new TreeMap<>();

        levelCounts.put("ERROR", 0);
        levelCounts.put("WARN", 0);
        levelCounts.put("INFO", 0);
        levelCounts.put("DEBUG", 0);

        synchronized (allEntries) {
            for (LogEntry entry : allEntries) {
                levelCounts.merge(
                    entry.level, 1, Integer::sum
                );
                if (
                    entry.appName != null
                        && !entry.appName.isEmpty()
                ) {
                    appCounts.merge(
                        entry.appName,
                        1,
                        Integer::sum
                    );
                }
            }
        }

        JTabbedPane tabs =
            (JTabbedPane) getContentPane();
        if (tabs.getTabCount() < 2) {
            return;
        }

        Component statsTab =
            tabs.getComponentAt(1);
        if (!(statsTab instanceof JPanel)) {
            return;
        }

        JPanel statsPanel = (JPanel) statsTab;
        updateLevelCounts(
            statsPanel, levelCounts
        );
        updateAppCounts(
            statsPanel, appCounts
        );
    }


    private void updateLevelCounts(
        JPanel statsPanel,
        Map<String, Integer> counts
    ) {
        for (Map.Entry<String, Integer> e :
            counts.entrySet()
        ) {
            findCountLabel(
                statsPanel, "count_" + e.getKey()
            );
        }

        JScrollPane scrollPane =
            (JScrollPane) statsPanel.getComponent(
                statsPanel.getComponentCount() - 1
            );

        if (
            scrollPane instanceof JScrollPane
        ) {
            Component view =
                scrollPane.getViewport()
                    .getView();
            if (view instanceof JPanel) {
                JPanel content = (JPanel) view;
                for (Component c :
                    content.getComponents()
                ) {
                    if (
                        c instanceof JPanel
                            && c.getName() == null
                    ) {
                        JPanel lp = (JPanel) c;
                        Component[] children =
                            lp.getComponents();
                        for (int i = 0;
                            i < children.length - 1;
                            i += 2
                        ) {
                            if (
                                children[i]
                                    instanceof JLabel
                            ) {
                                JLabel keyLbl =
                                    (JLabel)
                                        children[i];
                                String key = keyLbl
                                    .getText()
                                    .trim()
                                    .replace(
                                        "  ", ""
                                    )
                                    .replace(
                                        ":", ""
                                    );
                                if (
                                    children[i + 1]
                                        instanceof JLabel
                                ) {
                                    JLabel valLbl =
                                        (JLabel)
                                            children[
                                                i + 1
                                            ];
                                    Integer count =
                                        counts
                                            .get(key);
                                    if (count != null) {
                                        valLbl.setText(
                                            String
                                                .valueOf(
                                                    count
                                                )
                                        );
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    private void updateAppCounts(
        JPanel statsPanel,
        Map<String, Integer> counts
    ) {
        JScrollPane scrollPane =
            (JScrollPane) statsPanel.getComponent(
                statsPanel.getComponentCount() - 1
            );

        if (
            !(scrollPane instanceof JScrollPane)
        ) {
            return;
        }

        Component view =
            scrollPane.getViewport().getView();
        if (!(view instanceof JPanel)) {
            return;
        }

        JPanel content = (JPanel) view;
        JPanel appPanel = null;

        for (Component c : content.getComponents()) {
            if (
                "appStatsPanel".equals(c.getName())
            ) {
                appPanel = (JPanel) c;
                break;
            }
        }

        if (appPanel == null) {
            return;
        }

        appPanel.removeAll();

        if (counts.isEmpty()) {
            JLabel empty = new JLabel(
                "  No application data"
            );
            empty.setForeground(FG_DIM);
            appPanel.add(empty);
        } else {
            for (Map.Entry<String, Integer> e :
                counts.entrySet()
            ) {
                JPanel row = new JPanel(
                    new FlowLayout(
                        FlowLayout.LEFT, 8, 2
                    )
                );
                row.setBackground(BG_DARK);
                row.setMaximumSize(
                    new Dimension(500, 28)
                );

                JLabel name = new JLabel(
                    "  " + e.getKey() + ":"
                );
                name.setForeground(FG_TEXT);
                name.setFont(
                    new Font(
                        "Monospaced",
                        Font.PLAIN,
                        13
                    )
                );
                name.setPreferredSize(
                    new Dimension(180, 24)
                );

                JLabel count = new JLabel(
                    String.valueOf(e.getValue())
                );
                count.setForeground(FG_DIM);
                count.setFont(
                    new Font(
                        "Monospaced",
                        Font.BOLD,
                        13
                    )
                );

                row.add(name);
                row.add(count);
                appPanel.add(row);
            }
        }

        appPanel.revalidate();
        appPanel.repaint();
    }


    private void findCountLabel(
        JPanel panel,
        String name
    ) {
        for (Component c : panel.getComponents()) {
            if (
                c instanceof JPanel
            ) {
                for (Component inner :
                    ((JPanel) c).getComponents()
                ) {
                    if (
                        name.equals(inner.getName())
                    ) {
                        return;
                    }
                }
            }
        }
    }


    /*
     * =========================================
     * REFRESH
     * =========================================
     */

    private void performRefresh() {
        SwingWorker<List<LogEntry>, Void> worker =
            new SwingWorker<>() {
                @Override
                protected List<LogEntry> doInBackground() {
                    List<LogEntry> fresh =
                        new ArrayList<>();

                    String root = findProjectRoot();

                    scanDirectory(
                        new File(root, "logs"),
                        fresh
                    );
                    scanDirectory(
                        new File(root, ".data/logs"),
                        fresh
                    );
                    scanDirectory(
                        new File(root, "logs/apps"),
                        fresh
                    );
                    scanDirectory(
                        new File(root, "logs/errors"),
                        fresh
                    );
                    scanDirectory(
                        new File(root, "logs/system"),
                        fresh
                    );
                    scanDirectory(
                        new File(root, "logs/build"),
                        fresh
                    );
                    scanUserHomeLogs(fresh);

                    return fresh;
                }

                @Override
                protected void done() {
                    try {
                        List<LogEntry> fresh =
                            get();

                        allEntries.clear();
                        allEntries.addAll(fresh);

                        knownApps.clear();
                        for (LogEntry le : fresh) {
                            if (
                                le.appName != null
                                    && !le.appName
                                        .isEmpty()
                            ) {
                                knownApps.add(
                                    le.appName
                                );
                            }
                        }

                        refreshAppFilter();
                        applyFilters();
                        updateStats();

                        DateTimeFormatter shortFmt =
                            DateTimeFormatter
                                .ofPattern(
                                    "HH:mm:ss"
                                );
                        refreshLabel.setText(
                            "Last refresh: "
                                + LocalDateTime.now()
                                    .format(shortFmt)
                        );
                    } catch (Exception ex) {
                        updateStatus(
                            "Refresh error: "
                                + ex.getMessage()
                        );
                    }
                }
            };

        worker.execute();
    }


    /*
     * =========================================
     * EXPORT
     * =========================================
     */

    private void exportLogs() {
        SwingUtilities.invokeLater(() -> {
            JFileChooser chooser =
                new JFileChooser();
            chooser.setDialogTitle(
                "Export Logs"
            );
            chooser.setSelectedFile(
                new File("exported_logs.txt")
            );
            chooser.setFileFilter(
                new FileNameExtensionFilter(
                    "Text files", "txt"
                )
            );
            chooser.setCurrentDirectory(
                new File(findProjectRoot())
            );

            int result =
                chooser.showSaveDialog(this);

            if (
                result
                    == JFileChooser.APPROVE_OPTION
            ) {
                File target =
                    chooser.getSelectedFile();
                if (
                    !target.getName().endsWith(
                        ".txt"
                    )
                ) {
                    target = new File(
                        target.getAbsolutePath()
                            + ".txt"
                    );
                }

                final File exportFile = target;

                SwingWorker<Void, Void> worker =
                    new SwingWorker<>() {
                        @Override
                        protected Void doInBackground()
                            throws IOException
                        {
                            List<LogEntry> data =
                                tableModel.getData();

                            try (BufferedWriter bw =
                                Files.newBufferedWriter(
                                    exportFile
                                        .toPath()
                                )
                            ) {
                                for (LogEntry e :
                                    data
                                ) {
                                    bw.write(
                                        e.toString()
                                    );
                                    bw.newLine();
                                }
                            }

                            return null;
                        }

                        @Override
                        protected void done() {
                            try {
                                get();
                                updateStatus(
                                    "Exported "
                                        + tableModel
                                            .getRowCount()
                                        + " logs to "
                                        + exportFile
                                            .getName()
                                );
                            } catch (Exception ex) {
                                updateStatus(
                                    "Export failed: "
                                        + ex.getMessage()
                                );
                            }
                        }
                    };

                worker.execute();
            }
        });
    }


    /*
     * =========================================
     * OPEN FILE
     * =========================================
     */

    private void openSelectedFile() {
        int row = logTable.getSelectedRow();
        if (row < 0) {
            return;
        }

        int modelRow =
            logTable.convertRowIndexToModel(row);
        LogEntry entry =
            tableModel.getEntryAt(modelRow);

        if (
            entry.sourceFile != null
                && !entry.sourceFile.isEmpty()
        ) {
            try {
                File f = new File(entry.sourceFile);
                if (f.exists()) {
                    Desktop.getDesktop().open(f);
                    updateStatus(
                        "Opened: " + f.getName()
                    );
                } else {
                    updateStatus(
                        "File not found: "
                            + entry.sourceFile
                    );
                }
            } catch (Exception ex) {
                updateStatus(
                    "Cannot open file: "
                        + ex.getMessage()
                );
            }
        }
    }


    /*
     * =========================================
     * CLEAR LOGS
     * =========================================
     */

    private void clearLogs() {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to clear all "
                + "loaded log entries from view?\n"
                + "This does not delete files on disk.",
            "Confirm Clear",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            allEntries.clear();
            tableModel.clear();
            knownApps.clear();
            refreshAppFilter();
            updateStats();
            updateStatus("Logs cleared");
        }
    }


    /*
     * =========================================
     * ABOUT DIALOG
     * =========================================
     */

    private void showAbout() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_DARK);
        panel.setLayout(
            new BoxLayout(
                panel, BoxLayout.Y_AXIS
            )
        );
        panel.setBorder(
            new EmptyBorder(16, 24, 16, 24)
        );

        JLabel name = new JLabel("LogScope v1.0");
        name.setForeground(FG_TEXT);
        name.setFont(
            new Font(
                "SansSerif", Font.BOLD, 18
            )
        );
        name.setAlignmentX(
            Component.CENTER_ALIGNMENT
        );
        panel.add(name);

        panel.add(Box.createVerticalStrut(8));

        JLabel desc = new JLabel(
            "Company-wide log viewer for the"
                + " Datacenter workspace"
        );
        desc.setForeground(FG_DIM);
        desc.setFont(
            new Font(
                "SansSerif", Font.PLAIN, 13
            )
        );
        desc.setAlignmentX(
            Component.CENTER_ALIGNMENT
        );
        panel.add(desc);

        panel.add(Box.createVerticalStrut(12));

        String[] features = {
            "Read logs from multiple sources",
            "Search by text or regex",
            "Filter by level, app, date range",
            "Live auto-refresh",
            "Statistics by level and app",
            "Export filtered logs",
            "Open log files in default editor",
            "Repeated error detection",
            "Dark theme with keyboard shortcuts"
        };

        for (String feat : features) {
            JLabel f = new JLabel("  * " + feat);
            f.setForeground(FG_TEXT);
            f.setFont(
                new Font(
                    "SansSerif", Font.PLAIN, 12
                )
            );
            f.setAlignmentX(
                Component.CENTER_ALIGNMENT
            );
            panel.add(f);
        }

        panel.add(Box.createVerticalStrut(12));

        JLabel footer = new JLabel(
            "Pure Java Swing - No external libraries"
        );
        footer.setForeground(FG_DIM);
        footer.setFont(
            new Font(
                "SansSerif", Font.ITALIC, 11
            )
        );
        footer.setAlignmentX(
            Component.CENTER_ALIGNMENT
        );
        panel.add(footer);

        JOptionPane.showMessageDialog(
            this,
            panel,
            "About LogScope",
            JOptionPane.INFORMATION_MESSAGE
        );
    }


    /*
     * =========================================
     * REPEATED ERROR DETECTION
     * =========================================
     */

    private Map<String, Integer> detectRepeatedErrors() {
        Map<String, Integer> errorCounts =
            new LinkedHashMap<>();

        synchronized (allEntries) {
            for (LogEntry entry : allEntries) {
                if ("ERROR".equals(entry.level)) {
                    String key =
                        entry.message.trim();
                    errorCounts.merge(
                        key, 1, Integer::sum
                    );
                }
            }
        }

        Map<String, Integer> repeated =
            new LinkedHashMap<>();

        for (Map.Entry<String, Integer> e :
            errorCounts.entrySet()
        ) {
            if (e.getValue() > 1) {
                repeated.put(
                    e.getKey(), e.getValue()
                );
            }
        }

        return repeated;
    }


    /*
     * =========================================
     * HELPER: STATUS UPDATE
     * =========================================
     */

    private void updateStatus(String msg) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(msg);
        });
    }


    /*
     * =========================================
     * HELPER: STYLE BUTTON
     * =========================================
     */

    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(BG_BUTTON);
        btn.setForeground(FG_TEXT);
        btn.setFocusPainted(false);
        btn.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    new Color(50, 60, 100)
                ),
                BorderFactory.createEmptyBorder(
                    4, 12, 4, 12
                )
            )
        );
        btn.setFont(
            new Font(
                "SansSerif", Font.PLAIN, 12
            )
        );
        btn.setCursor(
            Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR
            )
        );
        return btn;
    }


    /*
     * =========================================
     * HELPER: STYLE COMBO
     * =========================================
     */

    private void styleCombo(JComboBox<String> combo) {
        combo.setBackground(BG_BUTTON);
        combo.setForeground(FG_TEXT);
        combo.setFont(
            new Font(
                "SansSerif", Font.PLAIN, 12
            )
        );
        combo.setBorder(
            BorderFactory.createLineBorder(
                new Color(50, 60, 100)
            )
        );
    }


    /*
     * =========================================
     * HELPER: MENU ITEM
     * =========================================
     */

    private JMenuItem createMenuItem(
        String text,
        String shortcut,
        ActionListener listener
    ) {
        JMenuItem item = new JMenuItem(text);
        item.setForeground(FG_TEXT);
        item.setBackground(BG_DARK);
        item.addActionListener(listener);

        if (shortcut != null) {
            item.setToolTipText(shortcut);
        }

        return item;
    }


    /*
     * =========================================
     * TABLE MODEL
     * =========================================
     */

    private static class LogTableModel
        extends AbstractTableModel
    {
        private static final long serialVersionUID =
            1L;

        private static final String[] COLUMNS = {
            "Level", "Timestamp", "Source", "Message"
        };

        private List<LogEntry> data;

        LogTableModel() {
            this.data = new ArrayList<>();
        }

        void setData(List<LogEntry> newData) {
            this.data = new ArrayList<>(newData);
            fireTableDataChanged();
        }

        void clear() {
            this.data.clear();
            fireTableDataChanged();
        }

        List<LogEntry> getData() {
            return new ArrayList<>(data);
        }

        LogEntry getEntryAt(int row) {
            if (row >= 0 && row < data.size()) {
                return data.get(row);
            }
            return null;
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int col) {
            return COLUMNS[col];
        }

        @Override
        public Object getValueAt(
            int row, int col
        ) {
            LogEntry e = data.get(row);
            switch (col) {
                case 0: return e.level;
                case 1: return e.timestamp != null
                    ? e.timestamp.format(TS_FMT)
                    : "";
                case 2: return e.appName != null
                    ? e.appName : "";
                case 3: return e.message;
                default: return "";
            }
        }
    }


    /*
     * =========================================
     * CELL RENDERER
     * =========================================
     */

    private static class LogCellRenderer
        extends DefaultTableCellRenderer
    {
        private static final long serialVersionUID =
            1L;

        @Override
        public Component
            getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
            )
        {
            Component c =
                super.getTableCellRendererComponent(
                    table, value,
                    isSelected, hasFocus,
                    row, column
                );

            if (!isSelected) {
                c.setBackground(BG_DARK);
            }

            if (column == 0 && !isSelected) {
                String level =
                    value != null
                        ? value.toString() : "";
                switch (level) {
                    case "ERROR":
                        c.setForeground(CLR_ERROR);
                        break;
                    case "WARN":
                        c.setForeground(CLR_WARN);
                        break;
                    case "DEBUG":
                        c.setForeground(CLR_DEBUG);
                        break;
                    default:
                        c.setForeground(CLR_INFO);
                        break;
                }
            } else if (!isSelected) {
                c.setForeground(FG_TEXT);
            }

            c.setFont(
                new Font(
                    "Monospaced", Font.PLAIN, 13
                )
            );

            return c;
        }
    }


    /*
     * =========================================
     * LOG ENTRY
     * =========================================
     */

    private static class LogEntry {
        String level = "INFO";
        LocalDateTime timestamp;
        String appName = "";
        String message = "";
        String sourceFile = "";
        int lineNumber = 0;
        String rawLine = "";

        @Override
        public String toString() {
            String ts = timestamp != null
                ? "[" + timestamp.format(TS_FMT) + "]"
                : "[no-timestamp]";
            return ts
                + " [" + level + "] "
                + (appName != null
                    ? appName + ": " : "")
                + message;
        }
    }


    /*
     * =========================================
     * DOCUMENT LISTENER HELPER
     * =========================================
     */

    private static class SimpleDocListener
        implements javax.swing.event.DocumentListener
    {
        private final Runnable callback;

        SimpleDocListener(Runnable callback) {
            this.callback = callback;
        }

        @Override
        public void insertUpdate(
            javax.swing.event.DocumentEvent e
        ) {
            callback.run();
        }

        @Override
        public void removeUpdate(
            javax.swing.event.DocumentEvent e
        ) {
            callback.run();
        }

        @Override
        public void changedUpdate(
            javax.swing.event.DocumentEvent e
        ) {
            callback.run();
        }
    }
}
