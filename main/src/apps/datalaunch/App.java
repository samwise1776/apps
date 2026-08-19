import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

public class App extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String APP_NAME = "DataLaunch";
    private static final String APP_VERSION = "1.0.0";
    private static final String DATACENTER_VERSION = "5.0.0";

    private static final Color BG_DARK = new Color(26, 26, 46);
    private static final Color ACCENT = new Color(15, 52, 96);
    private static final Color TEXT = new Color(224, 224, 224);
    private static final Color CARD_BG = new Color(22, 33, 62);
    private static final Color CARD_HOVER = new Color(30, 45, 80);
    private static final Color GREEN = new Color(46, 204, 113);
    private static final Color YELLOW = new Color(241, 196, 15);
    private static final Color RED = new Color(231, 76, 60);
    private static final Color GRAY = new Color(127, 127, 127);
    private static final Color BORDER_COLOR = new Color(50, 60, 90);
    private static final Color INPUT_BG = new Color(15, 20, 40);
    private static final Color BUTTON_BG = new Color(15, 52, 96);
    private static final Color BUTTON_HOVER = new Color(25, 72, 126);

    private final transient Path rootPath;
    private final transient Path configPath;
    private final transient Path buildPath;
    private final transient Path releasesPath;
    private final transient Path logsPath;

    private final transient List<AppInfo> applications = new ArrayList<>();
    private final transient List<String> buildLog = new ArrayList<>();
    private final transient List<String> logEntries = new ArrayList<>();

    private transient JTabbedPane tabbedPane;
    private transient JPanel appsPanel;
    private transient JPanel languagesPanel;
    private transient JPanel devPanel;
    private transient JPanel buildsPanel;
    private transient JPanel releasesPanel;
    private transient JPanel logsPanel;
    private transient JPanel healthPanel;
    private transient JPanel settingsPanel;
    private transient JPanel outputPanel;
    private transient JTextArea outputArea;
    private transient JProgressBar progressBar;
    private transient JLabel statusAppCount;
    private transient JLabel statusBuildSummary;
    private transient JLabel statusTime;
    private transient JTextField searchField;
    private String filterStatus = "ALL";
    private String searchQuery = "";
    private boolean darkTheme = true;

    private transient AppInfo selectedApp = null;
    private transient Thread buildThread;

    public App() {
        super(APP_NAME + " - Datacenter Control Center");
        rootPath = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        configPath = rootPath.resolve("config/apps.json");
        buildPath = rootPath.resolve("build/apps");
        releasesPath = rootPath.resolve("releases");
        logsPath = rootPath.resolve("logs");
        loadApplications();
        loadBuildLog();
        loadLogEntries();
        initUI();
        loadTheme();
    }

    // ===================== JSON PARSING =====================

    private static String readEntireFile(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static List<String> splitTopLevel(String s, char delimiter) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        boolean inStr = false;
        boolean escaped = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }
            if (c == '\\' && inStr) {
                current.append(c);
                escaped = true;
                continue;
            }
            if (c == '"') {
                inStr = !inStr;
                current.append(c);
                continue;
            }
            if (inStr) {
                current.append(c);
                continue;
            }
            if (c == '{' || c == '[' || c == '(') {
                depth++;
                current.append(c);
            } else if (c == '}' || c == ']' || c == ')') {
                depth--;
                current.append(c);
            } else if (c == delimiter && depth == 0) {
                parts.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        String tail = current.toString().trim();
        if (!tail.isEmpty()) parts.add(tail);
        return parts;
    }

    private static String extractStringValue(String obj, String key) {
        String pattern = "\"" + Pattern.quote(key) + "\"\\s*:\\s*\"";
        Matcher m = Pattern.compile(pattern).matcher(obj);
        if (!m.find()) return "";
        int start = m.end();
        StringBuilder val = new StringBuilder();
        for (int i = start; i < obj.length(); i++) {
            char c = obj.charAt(i);
            if (c == '\\') {
                if (i + 1 < obj.length()) {
                    char next = obj.charAt(i + 1);
                    switch (next) {
                        case 'n': val.append('\n'); break;
                        case 't': val.append('\t'); break;
                        case '"': val.append('"'); break;
                        case '\\': val.append('\\'); break;
                        default: val.append('\\').append(next);
                    }
                    i++;
                }
            } else if (c == '"') {
                break;
            } else {
                val.append(c);
            }
        }
        return val.toString();
    }

    private static String extractBoolValue(String obj, String key) {
        String pattern = "\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)";
        Matcher m = Pattern.compile(pattern).matcher(obj);
        return m.find() ? m.group(1) : "false";
    }

    private void loadApplications() {
        applications.clear();
        String json = readEntireFile(configPath);
        if (json.isEmpty()) return;
        int appArrayStart = json.indexOf("\"applications\"");
        if (appArrayStart < 0) return;
        int bracketStart = json.indexOf('[', appArrayStart);
        if (bracketStart < 0) return;
        int bracketEnd = findMatchingBracket(json, bracketStart);
        if (bracketEnd < 0) return;
        String arrContent = json.substring(bracketStart + 1, bracketEnd);
        List<String> objects = splitTopLevel(arrContent, ',');
        for (String obj : objects) {
            obj = obj.trim();
            if (obj.isEmpty()) continue;
            AppInfo info = new AppInfo();
            info.id = extractStringValue(obj, "id");
            info.slug = extractStringValue(obj, "slug");
            info.name = extractStringValue(obj, "name");
            info.version = extractStringValue(obj, "version");
            info.status = extractStringValue(obj, "status");
            info.language = extractStringValue(obj, "language");
            info.source = extractStringValue(obj, "source");
            info.buildScript = extractStringValue(obj, "build_script");
            info.buildCommand = extractStringValue(obj, "build_command");
            info.mainClass = extractStringValue(obj, "main");
            info.visibility = extractStringValue(obj, "visibility");
            info.distributable = "true".equals(extractBoolValue(obj, "distributable"));
            info.description = extractStringValue(obj, "description");
            info.built = checkBuildStatus(info.slug);
            info.released = checkReleaseStatus(info.name);
            applications.add(info);
        }
    }

    private static int findMatchingBracket(String s, int openPos) {
        if (openPos >= s.length() || s.charAt(openPos) != '[') return -1;
        int depth = 0;
        boolean inStr = false;
        boolean escaped = false;
        for (int i = openPos; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escaped) { escaped = false; continue; }
            if (c == '\\' && inStr) { escaped = true; continue; }
            if (c == '"') { inStr = !inStr; continue; }
            if (inStr) continue;
            if (c == '[') depth++;
            else if (c == ']') { depth--; if (depth == 0) return i; }
        }
        return -1;
    }

    private boolean checkBuildStatus(String slug) {
        Path buildDir = buildPath.resolve(slug);
        if (!Files.isDirectory(buildDir)) return false;
        try {
            return Files.walk(buildDir)
                .anyMatch(p -> p.toString().endsWith(".class"));
        } catch (IOException e) {
            return false;
        }
    }

    private boolean checkReleaseStatus(String name) {
        Path releaseDir = releasesPath.resolve(name);
        return Files.isDirectory(releaseDir);
    }

    private void loadBuildLog() {
        buildLog.clear();
        Path logFile = logsPath.resolve("build/build.log");
        if (Files.exists(logFile)) {
            String content = readEntireFile(logFile);
            for (String line : content.split("\n")) {
                if (!line.trim().isEmpty()) buildLog.add(line);
            }
        }
        buildLog.add(0, "[" + now() + "] DataLaunch initialized");
    }

    private void loadLogEntries() {
        logEntries.clear();
        logEntries.add("[" + now() + "] Application started");
        logEntries.add("[" + now() + "] Loaded " + applications.size() + " applications from config/apps.json");
        Path appsDir = logsPath.resolve("apps");
        if (Files.isDirectory(appsDir)) {
            try {
                Files.list(appsDir).limit(20).forEach(p -> {
                    logEntries.add("[" + now() + "] Log: " + p.getFileName());
                });
            } catch (IOException ignored) {}
        }
    }

    // ===================== UI INIT =====================

    private void initUI() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                promptQuit();
            }
        });
        setSize(1280, 820);
        setMinimumSize(new Dimension(960, 600));
        setLayout(new BorderLayout());

        JPanel topBar = createTopBar();
        add(topBar, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 13));
        tabbedPane.setBackground(ACCENT);
        tabbedPane.setForeground(TEXT);

        appsPanel = new JPanel(new BorderLayout());
        languagesPanel = new JPanel(new BorderLayout());
        devPanel = new JPanel(new BorderLayout());
        buildsPanel = new JPanel(new BorderLayout());
        releasesPanel = new JPanel(new BorderLayout());
        logsPanel = new JPanel(new BorderLayout());
        healthPanel = new JPanel(new BorderLayout());
        settingsPanel = new JPanel(new BorderLayout());

        tabbedPane.addTab("Apps", createAppsIcon(), appsPanel);
        tabbedPane.addTab("Languages", createLangIcon(), languagesPanel);
        tabbedPane.addTab("Development", createDevIcon(), devPanel);
        tabbedPane.addTab("Builds", createBuildIcon(), buildsPanel);
        tabbedPane.addTab("Releases", createReleaseIcon(), releasesPanel);
        tabbedPane.addTab("Logs", createLogIcon(), logsPanel);
        tabbedPane.addTab("Company Health", createHealthIcon(), healthPanel);
        tabbedPane.addTab("Settings", createSettingsIcon(), settingsPanel);

        add(tabbedPane, BorderLayout.CENTER);

        outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR), " Output Console ",
            TitledBorder.LEFT, TitledBorder.TOP, new Font("Monospaced", Font.BOLD, 11), TEXT));
        outputArea = new JTextArea(6, 40);
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputArea.setBackground(INPUT_BG);
        outputArea.setForeground(GREEN);
        outputArea.setCaretColor(GREEN);
        outputPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER);
        JButton clearBtn = styledButton("Clear");
        clearBtn.addActionListener(e -> outputArea.setText(""));
        JPanel clearPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        clearPanel.setBackground(BG_DARK);
        clearPanel.add(clearBtn);
        outputPanel.add(clearPanel, BorderLayout.EAST);
        outputPanel.setPreferredSize(new Dimension(0, 140));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setForeground(GREEN);
        progressBar.setBackground(INPUT_BG);
        progressBar.setString("Ready");
        bottomPanel.add(progressBar, BorderLayout.CENTER);
        bottomPanel.add(outputPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);

        JPanel statusBar = createStatusBar();
        add(statusBar, BorderLayout.PAGE_END);

        buildAllTabs();
        bindKeyboardShortcuts();
        pack();
        setLocationRelativeTo(null);
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private String nowFull() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // ===================== TOP BAR =====================

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(ACCENT);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_COLOR),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)));

        JLabel title = new JLabel(APP_NAME);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(TEXT);
        title.setIcon(new TextIcon(APP_NAME.substring(0, 1), 28));
        title.setIconTextGap(10);

        JLabel subtitle = new JLabel("Datacenter Control Center  v" + APP_VERSION);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(GRAY);

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.add(title, BorderLayout.NORTH);
        titlePanel.add(subtitle, BorderLayout.SOUTH);

        bar.add(titlePanel, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchPanel.setOpaque(false);

        searchField = new JTextField(18);
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        searchField.setBackground(INPUT_BG);
        searchField.setForeground(TEXT);
        searchField.setCaretColor(TEXT);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        searchField.setToolTipText("Search applications...");
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                searchQuery = searchField.getText().toLowerCase();
                refreshAppsGrid();
            }
        });

        JComboBox<String> statusFilter = new JComboBox<>(new String[]{
            "ALL", "ACTIVE", "DEVELOPMENT", "UNFINISHED"
        });
        statusFilter.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusFilter.setBackground(INPUT_BG);
        statusFilter.setForeground(TEXT);
        statusFilter.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        statusFilter.addActionListener(e -> {
            filterStatus = (String) statusFilter.getSelectedItem();
            refreshAppsGrid();
        });

        JButton refreshBtn = styledButton("Refresh");
        refreshBtn.addActionListener(e -> refreshAll());

        JButton aboutBtn = styledButton("About");
        aboutBtn.addActionListener(e -> showAboutDialog());

        searchPanel.add(searchField);
        searchPanel.add(statusFilter);
        searchPanel.add(refreshBtn);
        searchPanel.add(aboutBtn);

        bar.add(searchPanel, BorderLayout.EAST);
        return bar;
    }

    // ===================== STATUS BAR =====================

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(ACCENT);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
            BorderFactory.createEmptyBorder(4, 12, 4, 12)));

        statusAppCount = new JLabel("Apps: " + applications.size());
        statusAppCount.setForeground(TEXT);
        statusAppCount.setFont(new Font("SansSerif", Font.PLAIN, 11));

        int built = (int) applications.stream().filter(a -> a.built).count();
        int total = applications.size();
        statusBuildSummary = new JLabel("Built: " + built + "/" + total);
        statusBuildSummary.setForeground(TEXT);
        statusBuildSummary.setFont(new Font("SansSerif", Font.PLAIN, 11));

        statusTime = new JLabel(nowFull());
        statusTime.setForeground(GRAY);
        statusTime.setFont(new Font("SansSerif", Font.PLAIN, 11));

        JLabel shortcuts = new JLabel("F5 Refresh  F6 Build All  Ctrl+L Launch  Ctrl+Q Quit");
        shortcuts.setForeground(GRAY);
        shortcuts.setFont(new Font("SansSerif", Font.PLAIN, 10));

        bar.add(statusAppCount, BorderLayout.WEST);
        bar.add(statusBuildSummary, BorderLayout.CENTER);
        JPanel rightStatus = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightStatus.setOpaque(false);
        rightStatus.add(shortcuts);
        rightStatus.add(statusTime);
        bar.add(rightStatus, BorderLayout.EAST);

        javax.swing.Timer clock = new javax.swing.Timer(1000, e -> statusTime.setText(nowFull()));
        clock.start();

        return bar;
    }

    // ===================== TAB ICONS =====================

    private Icon createAppsIcon() { return new TextIcon("A", 14); }
    private Icon createLangIcon() { return new TextIcon("L", 14); }
    private Icon createDevIcon() { return new TextIcon("D", 14); }
    private Icon createBuildIcon() { return new TextIcon("B", 14); }
    private Icon createReleaseIcon() { return new TextIcon("R", 14); }
    private Icon createLogIcon() { return new TextIcon("O", 14); }
    private Icon createHealthIcon() { return new TextIcon("H", 14); }
    private Icon createSettingsIcon() { return new TextIcon("S", 14); }

    // ===================== APPS TAB =====================

    private void buildAppsTab() {
        appsPanel.removeAll();
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        filterBar.setBackground(BG_DARK);
        String[] filters = {"All", "Active", "Development", "Unfinished"};
        for (String f : filters) {
            JButton b = styledButton(f);
            b.addActionListener(e -> {
                filterStatus = f.equals("All") ? "ALL" : f.toUpperCase();
                refreshAppsGrid();
            });
            filterBar.add(b);
        }
        appsPanel.add(filterBar, BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(0, 3, 12, 12));
        grid.setBackground(BG_DARK);
        grid.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        List<AppInfo> filtered = getFilteredApps();
        for (AppInfo app : filtered) {
            grid.add(createAppCard(app));
        }
        if (filtered.isEmpty()) {
            JLabel empty = new JLabel("No applications match the current filter.", SwingConstants.CENTER);
            empty.setForeground(GRAY);
            empty.setFont(new Font("SansSerif", Font.ITALIC, 14));
            grid.setLayout(new BorderLayout());
            grid.add(empty, BorderLayout.CENTER);
        }
        JScrollPane scroll = new JScrollPane(grid);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_DARK);
        appsPanel.add(scroll, BorderLayout.CENTER);
        appsPanel.revalidate();
        appsPanel.repaint();
    }

    private List<AppInfo> getFilteredApps() {
        List<AppInfo> result = new ArrayList<>();
        for (AppInfo app : applications) {
            if (!filterStatus.equals("ALL") && !app.status.equals(filterStatus)) continue;
            if (!searchQuery.isEmpty()) {
                String haystack = (app.name + " " + app.id + " " + app.description + " " + app.language).toLowerCase();
                if (!haystack.contains(searchQuery)) continue;
            }
            result.add(app);
        }
        return result;
    }

    private void refreshAppsGrid() {
        buildAppsTab();
    }

    private JPanel createAppCard(AppInfo app) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        JLabel nameLabel = new JLabel(app.name);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        nameLabel.setForeground(TEXT);
        headerRow.add(nameLabel, BorderLayout.WEST);

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        badges.setOpaque(false);
        badges.add(statusBadge(app.status));
        badges.add(buildBadge(app.built));
        if (app.released) {
            badges.add(smallBadge("RELEASED", GREEN));
        }
        headerRow.add(badges, BorderLayout.EAST);
        card.add(headerRow);
        card.add(Box.createVerticalStrut(4));

        JLabel idLabel = new JLabel("ID: " + app.id + "  |  v" + app.version);
        idLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        idLabel.setForeground(GRAY);
        card.add(idLabel);
        card.add(Box.createVerticalStrut(2));

        JLabel langLabel = new JLabel("Language: " + app.language);
        langLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        langLabel.setForeground(new Color(100, 180, 255));
        card.add(langLabel);
        card.add(Box.createVerticalStrut(6));

        JLabel descLabel = new JLabel("<html><div style='width:280px'>" + escapeHtml(app.description) + "</div></html>");
        descLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        descLabel.setForeground(new Color(180, 180, 180));
        card.add(descLabel);
        card.add(Box.createVerticalStrut(10));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        buttonRow.setOpaque(false);

        JButton launchBtn = miniButton("Launch");
        launchBtn.addActionListener(e -> launchApp(app));
        buttonRow.add(launchBtn);

        JButton buildBtn = miniButton("Build");
        buildBtn.addActionListener(e -> buildApp(app));
        buttonRow.add(buildBtn);

        JButton testBtn = miniButton("Test");
        testBtn.addActionListener(e -> testApp(app));
        buttonRow.add(testBtn);

        JButton pkgBtn = miniButton("Package");
        pkgBtn.addActionListener(e -> packageApp(app));
        buttonRow.add(pkgBtn);

        card.add(buttonRow);

        JPanel buttonRow2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        buttonRow2.setOpaque(false);

        JButton srcBtn = miniButton("Source");
        srcBtn.addActionListener(e -> openInFileManager(rootPath.resolve(app.source)));
        buttonRow2.add(srcBtn);

        JButton logsBtn = miniButton("Logs");
        logsBtn.addActionListener(e -> openInFileManager(logsPath));
        buttonRow2.add(logsBtn);

        JButton docsBtn = miniButton("Docs");
        docsBtn.addActionListener(e -> openInFileManager(rootPath.resolve("docs")));
        buttonRow2.add(docsBtn);

        JButton detailsBtn = miniButton("Details");
        detailsBtn.addActionListener(e -> showAppDetails(app));
        buttonRow2.add(detailsBtn);

        card.add(buttonRow2);

        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                card.setBackground(CARD_HOVER);
            }
            public void mouseExited(MouseEvent e) {
                card.setBackground(CARD_BG);
            }
            public void mouseClicked(MouseEvent e) {
                selectedApp = app;
            }
        });

        return card;
    }

    // ===================== BADGES =====================

    private JLabel statusBadge(String status) {
        Color c;
        switch (status) {
            case "ACTIVE": c = GREEN; break;
            case "DEVELOPMENT": c = YELLOW; break;
            case "UNFINISHED": c = GRAY; break;
            default: c = GRAY;
        }
        return smallBadge(status, c);
    }

    private JLabel buildBadge(boolean built) {
        return smallBadge(built ? "BUILT" : "NOT BUILT", built ? GREEN : RED);
    }

    private JLabel smallBadge(String text, Color bg) {
        JLabel label = new JLabel(" " + text + " ");
        label.setFont(new Font("SansSerif", Font.BOLD, 9));
        label.setForeground(Color.BLACK);
        label.setOpaque(true);
        label.setBackground(bg);
        label.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        return label;
    }

    // ===================== LANGUAGES TAB =====================

    private void buildLanguagesTab() {
        languagesPanel.removeAll();
        Map<String, Integer> langCount = new LinkedHashMap<>();
        Map<String, List<String>> langApps = new LinkedHashMap<>();
        for (AppInfo app : applications) {
            String lang = app.language;
            langCount.merge(lang, 1, Integer::sum);
            langApps.computeIfAbsent(lang, k -> new ArrayList<>()).add(app.name);
        }
        JPanel grid = new JPanel(new GridLayout(0, 2, 16, 16));
        grid.setBackground(BG_DARK);
        grid.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        for (Map.Entry<String, Integer> entry : langCount.entrySet()) {
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(CARD_BG);
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));

            JLabel langLabel = new JLabel(entry.getKey());
            langLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
            langLabel.setForeground(TEXT);
            card.add(langLabel);
            card.add(Box.createVerticalStrut(4));

            JLabel countLabel = new JLabel(entry.getValue() + " application(s)");
            countLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
            countLabel.setForeground(GREEN);
            card.add(countLabel);
            card.add(Box.createVerticalStrut(8));

            List<String> apps = langApps.get(entry.getKey());
            if (apps != null) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < apps.size(); i++) {
                    sb.append(apps.get(i));
                    if (i < apps.size() - 1) sb.append(", ");
                }
                JLabel appsLabel = new JLabel("<html><div style='width:300px'>" + escapeHtml(sb.toString()) + "</div></html>");
                appsLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
                appsLabel.setForeground(GRAY);
                card.add(appsLabel);
            }

            grid.add(card);
        }
        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_DARK);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        languagesPanel.add(scroll, BorderLayout.CENTER);
        languagesPanel.revalidate();
        languagesPanel.repaint();
    }

    // ===================== DEVELOPMENT TAB =====================

    private void buildDevTab() {
        devPanel.removeAll();
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel heading = new JLabel("Recent Development Activity");
        heading.setFont(new Font("SansSerif", Font.BOLD, 18));
        heading.setForeground(TEXT);
        heading.setAlignmentX(LEFT_ALIGNMENT);
        content.add(heading);
        content.add(Box.createVerticalStrut(12));

        int active = (int) applications.stream().filter(a -> "ACTIVE".equals(a.status)).count();
        int dev = (int) applications.stream().filter(a -> "DEVELOPMENT".equals(a.status)).count();
        int unfinished = (int) applications.stream().filter(a -> "UNFINISHED".equals(a.status)).count();
        int built = (int) applications.stream().filter(a -> a.built).count();
        int released = (int) applications.stream().filter(a -> a.released).count();

        JPanel summaryGrid = new JPanel(new GridLayout(1, 5, 12, 0));
        summaryGrid.setOpaque(false);
        summaryGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        summaryGrid.setAlignmentX(LEFT_ALIGNMENT);
        summaryGrid.add(statCard("Active", String.valueOf(active), GREEN));
        summaryGrid.add(statCard("Development", String.valueOf(dev), YELLOW));
        summaryGrid.add(statCard("Unfinished", String.valueOf(unfinished), GRAY));
        summaryGrid.add(statCard("Built", String.valueOf(built), GREEN));
        summaryGrid.add(statCard("Released", String.valueOf(released), new Color(100, 180, 255)));
        content.add(summaryGrid);
        content.add(Box.createVerticalStrut(16));

        JLabel recentLabel = new JLabel("Recently Built");
        recentLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        recentLabel.setForeground(TEXT);
        recentLabel.setAlignmentX(LEFT_ALIGNMENT);
        content.add(recentLabel);
        content.add(Box.createVerticalStrut(6));

        List<AppInfo> recentlyBuilt = applications.stream()
            .filter(a -> a.built)
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        if (recentlyBuilt.isEmpty()) {
            JLabel noBuilds = new JLabel("No built applications found.");
            noBuilds.setForeground(GRAY);
            noBuilds.setAlignmentX(LEFT_ALIGNMENT);
            content.add(noBuilds);
        } else {
            for (AppInfo app : recentlyBuilt) {
                JPanel row = new JPanel(new BorderLayout());
                row.setOpaque(false);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                row.setAlignmentX(LEFT_ALIGNMENT);
                JLabel nameLabel = new JLabel(app.name + "  v" + app.version);
                nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
                nameLabel.setForeground(TEXT);
                row.add(nameLabel, BorderLayout.WEST);
                row.add(statusBadge(app.status), BorderLayout.EAST);
                content.add(row);
                content.add(Box.createVerticalStrut(4));
            }
        }

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_DARK);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        devPanel.add(scroll, BorderLayout.CENTER);
        devPanel.revalidate();
        devPanel.repaint();
    }

    private JPanel statCard(String label, String value, Color color) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)));

        JLabel val = new JLabel(value);
        val.setFont(new Font("SansSerif", Font.BOLD, 28));
        val.setForeground(color);
        val.setAlignmentX(LEFT_ALIGNMENT);
        card.add(val);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lbl.setForeground(GRAY);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        card.add(lbl);
        return card;
    }

    // ===================== BUILDS TAB =====================

    private void buildBuildsTab() {
        buildsPanel.removeAll();
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel heading = new JLabel("Build History");
        heading.setFont(new Font("SansSerif", Font.BOLD, 18));
        heading.setForeground(TEXT);
        topRow.add(heading, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.setOpaque(false);
        JButton buildAllBtn = styledButton("Build All");
        buildAllBtn.addActionListener(e -> buildAll());
        JButton clearLogBtn = styledButton("Clear Log");
        clearLogBtn.addActionListener(e -> { buildLog.clear(); buildBuildsTab(); });
        btns.add(buildAllBtn);
        btns.add(clearLogBtn);
        topRow.add(btns, BorderLayout.EAST);
        content.add(topRow, BorderLayout.NORTH);

        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(INPUT_BG);
        logArea.setForeground(TEXT);
        for (String line : buildLog) {
            logArea.append(line + "\n");
        }
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        content.add(scroll, BorderLayout.CENTER);

        buildsPanel.add(content, BorderLayout.CENTER);
        buildsPanel.revalidate();
        buildsPanel.repaint();
    }

    // ===================== RELEASES TAB =====================

    private void buildReleasesTab() {
        releasesPanel.removeAll();
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel heading = new JLabel("Releases");
        heading.setFont(new Font("SansSerif", Font.BOLD, 18));
        heading.setForeground(TEXT);
        content.add(heading, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 3, 12, 12));
        grid.setBackground(BG_DARK);
        grid.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        boolean anyRelease = false;
        for (AppInfo app : applications) {
            if (app.released) {
                anyRelease = true;
                JPanel card = new JPanel();
                card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
                card.setBackground(CARD_BG);
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR),
                    BorderFactory.createEmptyBorder(14, 14, 14, 14)));
                JLabel name = new JLabel(app.name);
                name.setFont(new Font("SansSerif", Font.BOLD, 15));
                name.setForeground(TEXT);
                card.add(name);
                card.add(Box.createVerticalStrut(4));
                JLabel ver = new JLabel("Version: " + app.version);
                ver.setFont(new Font("SansSerif", Font.PLAIN, 12));
                ver.setForeground(GREEN);
                card.add(ver);
                card.add(Box.createVerticalStrut(4));
                Path dir = releasesPath.resolve(app.name);
                long fileCount = 0;
                try {
                    fileCount = Files.walk(dir).filter(Files::isRegularFile).count();
                } catch (IOException ignored) {}
                JLabel files = new JLabel(fileCount + " file(s) in release");
                files.setFont(new Font("SansSerif", Font.PLAIN, 11));
                files.setForeground(GRAY);
                card.add(files);
                card.add(Box.createVerticalStrut(8));
                JButton openBtn = miniButton("Open");
                openBtn.addActionListener(e -> openInFileManager(dir));
                card.add(openBtn);
                grid.add(card);
            }
        }

        if (!anyRelease) {
            JLabel empty = new JLabel("No releases found. Use Package to create releases.", SwingConstants.CENTER);
            empty.setForeground(GRAY);
            content.add(empty, BorderLayout.CENTER);
        } else {
            JScrollPane scroll = new JScrollPane(grid);
            scroll.setBorder(null);
            scroll.getViewport().setBackground(BG_DARK);
            content.add(scroll, BorderLayout.CENTER);
        }

        releasesPanel.add(content, BorderLayout.CENTER);
        releasesPanel.revalidate();
        releasesPanel.repaint();
    }

    // ===================== LOGS TAB =====================

    private void buildLogsTab() {
        logsPanel.removeAll();
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel heading = new JLabel("Application Logs");
        heading.setFont(new Font("SansSerif", Font.BOLD, 18));
        heading.setForeground(TEXT);
        topRow.add(heading, BorderLayout.WEST);

        JButton refreshBtn = styledButton("Refresh");
        refreshBtn.addActionListener(e -> {
            loadLogEntries();
            buildLogsTab();
        });
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        btnPanel.add(refreshBtn);
        topRow.add(btnPanel, BorderLayout.EAST);
        content.add(topRow, BorderLayout.NORTH);

        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(INPUT_BG);
        logArea.setForeground(TEXT);
        for (String entry : logEntries) {
            logArea.append(entry + "\n");
        }

        Path appsDir = logsPath.resolve("apps");
        if (Files.isDirectory(appsDir)) {
            try {
                Files.list(appsDir).sorted().forEach(p -> {
                    if (Files.isRegularFile(p)) {
                        String content2 = readEntireFile(p);
                        logArea.append("\n--- " + p.getFileName() + " ---\n");
                        logArea.append(content2);
                    }
                });
            } catch (IOException ignored) {}
        }

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        content.add(scroll, BorderLayout.CENTER);

        logsPanel.add(content, BorderLayout.CENTER);
        logsPanel.revalidate();
        logsPanel.repaint();
    }

    // ===================== HEALTH TAB =====================

    private void buildHealthTab() {
        healthPanel.removeAll();
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel heading = new JLabel("Company Health Dashboard");
        heading.setFont(new Font("SansSerif", Font.BOLD, 20));
        heading.setForeground(TEXT);
        heading.setAlignmentX(LEFT_ALIGNMENT);
        content.add(heading);
        content.add(Box.createVerticalStrut(20));

        int total = applications.size();
        int active = (int) applications.stream().filter(a -> "ACTIVE".equals(a.status)).count();
        int dev = (int) applications.stream().filter(a -> "DEVELOPMENT".equals(a.status)).count();
        int unfinished = (int) applications.stream().filter(a -> "UNFINISHED".equals(a.status)).count();
        int built = (int) applications.stream().filter(a -> a.built).count();
        int released = (int) applications.stream().filter(a -> a.released).count();
        int withBuildScript = (int) applications.stream().filter(a -> !a.buildScript.isEmpty()).count();
        int javaApps = (int) applications.stream().filter(a -> a.language.contains("Java")).count();
        int distributable = (int) applications.stream().filter(a -> a.distributable).count();

        JPanel metricsGrid = new JPanel(new GridLayout(0, 4, 12, 12));
        metricsGrid.setOpaque(false);
        metricsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        metricsGrid.setAlignmentX(LEFT_ALIGNMENT);

        metricsGrid.add(statCard("Total Apps", String.valueOf(total), TEXT));
        metricsGrid.add(statCard("Active", String.valueOf(active), GREEN));
        metricsGrid.add(statCard("In Development", String.valueOf(dev), YELLOW));
        metricsGrid.add(statCard("Unfinished", String.valueOf(unfinished), GRAY));
        metricsGrid.add(statCard("Built", String.valueOf(built), GREEN));
        metricsGrid.add(statCard("Released", String.valueOf(released), new Color(100, 180, 255)));
        metricsGrid.add(statCard("Java Apps", String.valueOf(javaApps), new Color(255, 165, 0)));
        metricsGrid.add(statCard("Distributable", String.valueOf(distributable), new Color(180, 130, 255)));

        content.add(metricsGrid);
        content.add(Box.createVerticalStrut(20));

        JPanel progressBarPanel = new JPanel();
        progressBarPanel.setLayout(new BoxLayout(progressBarPanel, BoxLayout.Y_AXIS));
        progressBarPanel.setOpaque(false);
        progressBarPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        progressBarPanel.setAlignmentX(LEFT_ALIGNMENT);

        double buildPercent = total > 0 ? (double) built / total * 100 : 0;
        double releasePercent = total > 0 ? (double) released / total * 100 : 0;
        double activePercent = total > 0 ? (double) active / total * 100 : 0;

        progressBarPanel.add(createLabeledProgress("Build Coverage", buildPercent, GREEN));
        progressBarPanel.add(Box.createVerticalStrut(6));
        progressBarPanel.add(createLabeledProgress("Release Coverage", releasePercent, new Color(100, 180, 255)));
        progressBarPanel.add(Box.createVerticalStrut(6));
        progressBarPanel.add(createLabeledProgress("Active Ratio", activePercent, YELLOW));

        content.add(progressBarPanel);
        content.add(Box.createVerticalStrut(20));

        JLabel appsHeading = new JLabel("Application Status Detail");
        appsHeading.setFont(new Font("SansSerif", Font.BOLD, 14));
        appsHeading.setForeground(TEXT);
        appsHeading.setAlignmentX(LEFT_ALIGNMENT);
        content.add(appsHeading);
        content.add(Box.createVerticalStrut(8));

        String[] columnNames = {"Name", "Status", "Language", "Version", "Built", "Released", "Distributable"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        for (AppInfo app : applications) {
            model.addRow(new Object[]{
                app.name, app.status, app.language, app.version,
                app.built ? "Yes" : "No", app.released ? "Yes" : "No",
                app.distributable ? "Yes" : "No"
            });
        }
        JTable table = new JTable(model);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setBackground(CARD_BG);
        table.setForeground(TEXT);
        table.setGridColor(BORDER_COLOR);
        table.setRowHeight(26);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(ACCENT);
        table.getTableHeader().setForeground(TEXT);
        table.setSelectionBackground(ACCENT);
        table.setSelectionForeground(TEXT);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                c.setBackground(sel ? ACCENT : (row % 2 == 0 ? CARD_BG : BG_DARK));
                c.setForeground(TEXT);
                if (col == 1) {
                    String status = String.valueOf(val);
                    switch (status) {
                        case "ACTIVE": setForeground(GREEN); break;
                        case "DEVELOPMENT": setForeground(YELLOW); break;
                        case "UNFINISHED": setForeground(GRAY); break;
                    }
                }
                return c;
            }
        });
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        tableScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        tableScroll.setAlignmentX(LEFT_ALIGNMENT);
        content.add(tableScroll);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_DARK);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        healthPanel.add(scroll, BorderLayout.CENTER);
        healthPanel.revalidate();
        healthPanel.repaint();
    }

    private JPanel createLabeledProgress(String label, double percent, Color color) {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lbl.setForeground(GRAY);
        lbl.setPreferredSize(new Dimension(140, 20));
        panel.add(lbl, BorderLayout.WEST);
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue((int) percent);
        bar.setString(String.format("%.0f%%", percent));
        bar.setStringPainted(true);
        bar.setForeground(color);
        bar.setBackground(INPUT_BG);
        bar.setFont(new Font("SansSerif", Font.BOLD, 10));
        panel.add(bar, BorderLayout.CENTER);
        return panel;
    }

    // ===================== SETTINGS TAB =====================

    private void buildSettingsTab() {
        settingsPanel.removeAll();
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel heading = new JLabel("Settings");
        heading.setFont(new Font("SansSerif", Font.BOLD, 20));
        heading.setForeground(TEXT);
        heading.setAlignmentX(LEFT_ALIGNMENT);
        content.add(heading);
        content.add(Box.createVerticalStrut(20));

        JPanel settingsCard = new JPanel();
        settingsCard.setLayout(new BoxLayout(settingsCard, BoxLayout.Y_AXIS));
        settingsCard.setBackground(CARD_BG);
        settingsCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        settingsCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));
        settingsCard.setAlignmentX(LEFT_ALIGNMENT);

        JLabel generalHeading = new JLabel("General");
        generalHeading.setFont(new Font("SansSerif", Font.BOLD, 14));
        generalHeading.setForeground(TEXT);
        settingsCard.add(generalHeading);
        settingsCard.add(Box.createVerticalStrut(10));

        JPanel rootPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        rootPanel.setOpaque(false);
        rootPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JLabel rootLabel = new JLabel("Root Directory:");
        rootLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        rootLabel.setForeground(TEXT);
        rootLabel.setPreferredSize(new Dimension(120, 20));
        rootPanel.add(rootLabel);
        JLabel rootPathLabel = new JLabel(rootPath.toString());
        rootPathLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        rootPathLabel.setForeground(GRAY);
        rootPanel.add(rootPathLabel);
        settingsCard.add(rootPanel);
        settingsCard.add(Box.createVerticalStrut(8));

        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        configPanel.setOpaque(false);
        configPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JLabel configLabel = new JLabel("Config File:");
        configLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        configLabel.setForeground(TEXT);
        configLabel.setPreferredSize(new Dimension(120, 20));
        configPanel.add(configLabel);
        JLabel configPathLabel = new JLabel(configPath.toString());
        configPathLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        configPathLabel.setForeground(GRAY);
        configPanel.add(configPathLabel);
        settingsCard.add(configPanel);
        settingsCard.add(Box.createVerticalStrut(16));

        JLabel appearanceHeading = new JLabel("Appearance");
        appearanceHeading.setFont(new Font("SansSerif", Font.BOLD, 14));
        appearanceHeading.setForeground(TEXT);
        settingsCard.add(appearanceHeading);
        settingsCard.add(Box.createVerticalStrut(10));

        JCheckBox darkToggle = new JCheckBox("Dark Theme (restart to apply)");
        darkToggle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        darkToggle.setForeground(TEXT);
        darkToggle.setOpaque(false);
        darkToggle.setSelected(darkTheme);
        darkToggle.addActionListener(e -> darkTheme = darkToggle.isSelected());
        settingsCard.add(darkToggle);
        settingsCard.add(Box.createVerticalStrut(16));

        JLabel actionsHeading = new JLabel("Default Actions");
        actionsHeading.setFont(new Font("SansSerif", Font.BOLD, 14));
        actionsHeading.setForeground(TEXT);
        settingsCard.add(actionsHeading);
        settingsCard.add(Box.createVerticalStrut(10));

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actionPanel.setOpaque(false);
        actionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JLabel defaultActionLabel = new JLabel("Default Launch Action:");
        defaultActionLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        defaultActionLabel.setForeground(TEXT);
        defaultActionLabel.setPreferredSize(new Dimension(160, 20));
        actionPanel.add(defaultActionLabel);
        String[] actions = {"Launch", "Build", "Test", "Package"};
        JComboBox<String> actionCombo = new JComboBox<>(actions);
        actionCombo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        actionCombo.setBackground(INPUT_BG);
        actionCombo.setForeground(TEXT);
        actionPanel.add(actionCombo);
        settingsCard.add(actionPanel);
        settingsCard.add(Box.createVerticalStrut(16));

        JLabel pathsHeading = new JLabel("Keyboard Shortcuts");
        pathsHeading.setFont(new Font("SansSerif", Font.BOLD, 14));
        pathsHeading.setForeground(TEXT);
        settingsCard.add(pathsHeading);
        settingsCard.add(Box.createVerticalStrut(8));

        String[][] shortcuts = {
            {"F5", "Refresh all data"},
            {"F6", "Build all applications"},
            {"Ctrl+L", "Launch selected application"},
            {"Ctrl+Q", "Quit application"},
        };
        for (String[] sc : shortcuts) {
            JPanel scRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
            scRow.setOpaque(false);
            scRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            JLabel key = new JLabel(sc[0]);
            key.setFont(new Font("Monospaced", Font.BOLD, 12));
            key.setForeground(new Color(100, 180, 255));
            key.setPreferredSize(new Dimension(100, 20));
            scRow.add(key);
            JLabel desc = new JLabel(sc[1]);
            desc.setFont(new Font("SansSerif", Font.PLAIN, 12));
            desc.setForeground(GRAY);
            scRow.add(desc);
            settingsCard.add(scRow);
        }

        content.add(settingsCard);
        content.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_DARK);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        settingsPanel.add(scroll, BorderLayout.CENTER);
        settingsPanel.revalidate();
        settingsPanel.repaint();
    }

    // ===================== BUILD ALL TABS =====================

    private void buildAllTabs() {
        loadApplications();
        loadBuildLog();
        loadLogEntries();
        buildAppsTab();
        buildLanguagesTab();
        buildDevTab();
        buildBuildsTab();
        buildReleasesTab();
        buildLogsTab();
        buildHealthTab();
        buildSettingsTab();
        updateStatusBar();
    }

    // ===================== ACTIONS =====================

    private void refreshAll() {
        appendOutput("Refreshing all data...");
        progressBar.setIndeterminate(true);
        progressBar.setString("Refreshing...");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            protected Void doInBackground() {
                loadApplications();
                loadBuildLog();
                loadLogEntries();
                return null;
            }
            protected void done() {
                buildAllTabs();
                progressBar.setIndeterminate(false);
                progressBar.setString("Refresh complete");
                appendOutput("Refresh complete. " + applications.size() + " applications loaded.");
            }
        };
        worker.execute();
    }

    private void buildApp(AppInfo app) {
        if (app.buildScript.isEmpty()) {
            appendOutput("[" + app.name + "] No build script configured.");
            JOptionPane.showMessageDialog(this,
                app.name + " has no build script configured.",
                "Build", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String cmd = "bash " + app.buildScript;
        appendOutput("[" + app.name + "] Starting build: " + cmd);
        progressBar.setIndeterminate(true);
        progressBar.setString("Building " + app.name + "...");
        runProcessAsync(cmd, app.name + " build", () -> {
            app.built = checkBuildStatus(app.slug);
            appendOutput("[" + app.name + "] Build complete. Built=" + app.built);
            buildLog.add("[" + now() + "] Build: " + app.name + " v" + app.version + " - " + (app.built ? "SUCCESS" : "FAILED"));
            buildAllTabs();
        });
    }

    private void testApp(AppInfo app) {
        appendOutput("[" + app.name + "] Running tests...");
        progressBar.setIndeterminate(true);
        progressBar.setString("Testing " + app.name + "...");
        String cmd = "bash " + app.buildScript;
        runProcessAsync(cmd, app.name + " test", () -> {
            appendOutput("[" + app.name + "] Test run complete.");
            buildLog.add("[" + now() + "] Test: " + app.name + " - completed");
            progressBar.setIndeterminate(false);
            progressBar.setString("Test complete");
        });
    }

    private void launchApp(AppInfo app) {
        if (!app.built) {
            appendOutput("[" + app.name + "] Application not built. Building first...");
            buildApp(app);
            return;
        }
        Path buildDir = buildPath.resolve(app.slug);
        String mainClass = app.mainClass;
        if (mainClass.isEmpty()) {
            appendOutput("[" + app.name + "] No main class configured.");
            return;
        }
        if (mainClass.endsWith(".java")) {
            mainClass = mainClass.replace(".java", "");
        }
        if (!mainClass.contains(".") && !mainClass.contains("/")) {
            List<String> candidates = findMainClass(buildDir, mainClass);
            if (!candidates.isEmpty()) {
                mainClass = candidates.get(0);
            }
        }
        String cmd = "java -cp \"" + buildDir + "\" " + mainClass;
        appendOutput("[" + app.name + "] Launching: " + cmd);
        progressBar.setString("Launching " + app.name + "...");
        try {
            ProcessBuilder pb = new ProcessBuilder("java", "-cp", buildDir.toString(), mainClass);
            pb.directory(rootPath.toFile());
            pb.redirectErrorStream(true);
            pb.start();
            appendOutput("[" + app.name + "] Process started.");
        } catch (IOException e) {
            appendOutput("[" + app.name + "] Launch failed: " + e.getMessage());
        }
    }

    private void packageApp(AppInfo app) {
        appendOutput("[" + app.name + "] Creating release package...");
        progressBar.setIndeterminate(true);
        progressBar.setString("Packaging " + app.name + "...");
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            protected Boolean doInBackground() {
                try {
                    Path targetDir = releasesPath.resolve(app.name);
                    Files.createDirectories(targetDir);
                    Path buildDir = buildPath.resolve(app.slug);
                    if (Files.isDirectory(buildDir)) {
                        Files.walk(buildDir)
                            .filter(Files::isRegularFile)
                            .forEach(src -> {
                                try {
                                    Path dest = targetDir.resolve(src.getFileName());
                                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                                } catch (IOException ignored) {}
                            });
                    }
                    Path versionFile = targetDir.resolve("VERSION");
                    Files.write(versionFile, app.version.getBytes(StandardCharsets.UTF_8));
                    return true;
                } catch (IOException e) {
                    return false;
                }
            }
            protected void done() {
                try {
                    boolean success = get();
                    app.released = checkReleaseStatus(app.name);
                    appendOutput("[" + app.name + "] Package " + (success ? "created" : "failed") + " in " + releasesPath.resolve(app.name));
                    progressBar.setIndeterminate(false);
                    progressBar.setString(success ? "Package complete" : "Package failed");
                    buildAllTabs();
                } catch (Exception e) {
                    appendOutput("[" + app.name + "] Package error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void buildAll() {
        appendOutput("Building all applications with build scripts...");
        progressBar.setIndeterminate(true);
        progressBar.setString("Building all...");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            protected Void doInBackground() {
                for (AppInfo app : applications) {
                    if (!app.buildScript.isEmpty()) {
                        appendOutput("[" + app.name + "] Building...");
                        try {
                            String cmd = "bash " + app.buildScript;
                            ProcessBuilder pb = new ProcessBuilder("bash", app.buildScript);
                            pb.directory(rootPath.toFile());
                            pb.redirectErrorStream(true);
                            pb.environment().put("ROOT", rootPath.toString());
                            Process p = pb.start();
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    appendOutput("[" + app.name + "] " + line);
                                }
                            }
                            p.waitFor();
                        } catch (Exception e) {
                            appendOutput("[" + app.name + "] Error: " + e.getMessage());
                        }
                    }
                }
                return null;
            }
            protected void done() {
                loadApplications();
                buildLog.add("[" + now() + "] Bulk build completed");
                buildAllTabs();
                progressBar.setIndeterminate(false);
                progressBar.setString("All builds complete");
                appendOutput("All builds completed.");
            }
        };
        worker.execute();
    }

    // ===================== DIALOGS =====================

    private void showAppDetails(AppInfo app) {
        JDialog dialog = new JDialog(this, app.name + " - Details", true);
        dialog.setSize(520, 520);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(BG_DARK);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel nameLabel = new JLabel(app.name);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        nameLabel.setForeground(TEXT);
        nameLabel.setAlignmentX(LEFT_ALIGNMENT);
        content.add(nameLabel);
        content.add(Box.createVerticalStrut(12));

        String[][] fields = {
            {"ID", app.id},
            {"Slug", app.slug},
            {"Version", app.version},
            {"Status", app.status},
            {"Language", app.language},
            {"Source", app.source},
            {"Build Script", app.buildScript.isEmpty() ? "(none)" : app.buildScript},
            {"Main Class", app.mainClass},
            {"Visibility", app.visibility},
            {"Distributable", String.valueOf(app.distributable)},
            {"Built", String.valueOf(app.built)},
            {"Released", String.valueOf(app.released)},
        };
        for (String[] f : fields) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            row.setAlignmentX(LEFT_ALIGNMENT);
            JLabel key = new JLabel(f[0] + ":");
            key.setFont(new Font("SansSerif", Font.BOLD, 12));
            key.setForeground(GRAY);
            key.setPreferredSize(new Dimension(120, 20));
            row.add(key);
            JLabel val = new JLabel(f[1]);
            val.setFont(new Font("Monospaced", Font.PLAIN, 12));
            val.setForeground(TEXT);
            row.add(val);
            content.add(row);
        }

        content.add(Box.createVerticalStrut(12));
        JLabel descHeader = new JLabel("Description:");
        descHeader.setFont(new Font("SansSerif", Font.BOLD, 12));
        descHeader.setForeground(GRAY);
        descHeader.setAlignmentX(LEFT_ALIGNMENT);
        content.add(descHeader);
        content.add(Box.createVerticalStrut(4));
        JLabel descLabel = new JLabel("<html><div style='width:450px'>" + escapeHtml(app.description) + "</div></html>");
        descLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        descLabel.setForeground(TEXT);
        descLabel.setAlignmentX(LEFT_ALIGNMENT);
        content.add(descLabel);

        content.add(Box.createVerticalGlue());

        JButton closeBtn = styledButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        btnPanel.add(closeBtn);
        content.add(btnPanel);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_DARK);
        dialog.setContentPane(scroll);
        dialog.setVisible(true);
    }

    private void showAboutDialog() {
        JDialog dialog = new JDialog(this, "About " + APP_NAME, true);
        dialog.setSize(440, 360);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(BG_DARK);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel nameLabel = new JLabel(APP_NAME);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        nameLabel.setForeground(TEXT);
        nameLabel.setAlignmentX(LEFT_ALIGNMENT);
        content.add(nameLabel);

        JLabel versionLabel = new JLabel("Version " + APP_VERSION);
        versionLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        versionLabel.setForeground(GREEN);
        versionLabel.setAlignmentX(LEFT_ALIGNMENT);
        content.add(versionLabel);
        content.add(Box.createVerticalStrut(16));

        String[][] info = {
            {"Datacenter", DATACENTER_VERSION},
            {"Java", System.getProperty("java.version", "unknown")},
            {"Java Vendor", System.getProperty("java.vendor", "unknown")},
            {"OS", System.getProperty("os.name") + " " + System.getProperty("os.version")},
            {"Architecture", System.getProperty("os.arch")},
            {"Total Applications", String.valueOf(applications.size())},
            {"Active", String.valueOf(applications.stream().filter(a -> "ACTIVE".equals(a.status)).count())},
            {"Built", String.valueOf(applications.stream().filter(a -> a.built).count())},
        };
        for (String[] row : info) {
            JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
            r.setOpaque(false);
            r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
            r.setAlignmentX(LEFT_ALIGNMENT);
            JLabel k = new JLabel(row[0] + ":");
            k.setFont(new Font("SansSerif", Font.BOLD, 12));
            k.setForeground(GRAY);
            k.setPreferredSize(new Dimension(140, 20));
            r.add(k);
            JLabel v = new JLabel(row[1]);
            v.setFont(new Font("Monospaced", Font.PLAIN, 12));
            v.setForeground(TEXT);
            r.add(v);
            content.add(r);
        }

        content.add(Box.createVerticalStrut(16));
        JLabel copyLabel = new JLabel("Datacenter " + DATACENTER_VERSION + " - Internal Use Only");
        copyLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        copyLabel.setForeground(GRAY);
        copyLabel.setAlignmentX(LEFT_ALIGNMENT);
        content.add(copyLabel);

        content.add(Box.createVerticalGlue());

        JButton closeBtn = styledButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        btnPanel.add(closeBtn);
        content.add(btnPanel);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private void promptQuit() {
        int result = JOptionPane.showConfirmDialog(this,
            "Quit " + APP_NAME + "?",
            "Confirm Exit",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            dispose();
            System.exit(0);
        }
    }

    // ===================== PROCESS EXECUTION =====================

    private void runProcessAsync(String command, String label, Runnable onComplete) {
        if (buildThread != null && buildThread.isAlive()) {
            appendOutput("A build is already in progress. Please wait.");
            return;
        }
        buildThread = new Thread(() -> {
            try {
                String[] parts = command.split("\\s+");
                ProcessBuilder pb = new ProcessBuilder(parts);
                pb.directory(rootPath.toFile());
                pb.redirectErrorStream(true);
                pb.environment().put("ROOT", rootPath.toString());
                Process p = pb.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String outputLine = line;
                        SwingUtilities.invokeLater(() -> appendOutput("[" + label + "] " + outputLine));
                    }
                }
                p.waitFor();
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> appendOutput("[" + label + "] Error: " + e.getMessage()));
            }
            SwingUtilities.invokeLater(() -> {
                progressBar.setIndeterminate(false);
                progressBar.setString("Complete");
                if (onComplete != null) onComplete.run();
            });
        });
        buildThread.start();
    }

    private List<String> findMainClass(Path dir, String hint) {
        List<String> result = new ArrayList<>();
        try {
            Files.walk(dir)
                .filter(p -> p.toString().endsWith(".class"))
                .forEach(p -> {
                    String relative = dir.relativize(p).toString();
                    String className = relative.replace(File.separatorChar, '.')
                        .replace(".class", "");
                    if (className.toLowerCase().contains(hint.toLowerCase())) {
                        result.add(className);
                    }
                });
        } catch (IOException ignored) {}
        return result;
    }

    // ===================== UTILITIES =====================

    private void openInFileManager(Path path) {
        try {
            if (Files.exists(path)) {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("mac")) {
                    new ProcessBuilder("open", path.toString()).start();
                } else if (os.contains("linux")) {
                    new ProcessBuilder("xdg-open", path.toString()).start();
                } else {
                    new ProcessBuilder("explorer", path.toString()).start();
                }
                appendOutput("Opened: " + path);
            } else {
                appendOutput("Path does not exist: " + path);
                JOptionPane.showMessageDialog(this,
                    "Directory not found:\n" + path,
                    "Open Directory", JOptionPane.WARNING_MESSAGE);
            }
        } catch (IOException e) {
            appendOutput("Failed to open: " + e.getMessage());
        }
    }

    private void appendOutput(String text) {
        SwingUtilities.invokeLater(() -> {
            outputArea.append(text + "\n");
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
        });
    }

    private void updateStatusBar() {
        if (statusAppCount != null) statusAppCount.setText("Apps: " + applications.size());
        if (statusBuildSummary != null) {
            int built = (int) applications.stream().filter(a -> a.built).count();
            statusBuildSummary.setText("Built: " + built + "/" + applications.size());
        }
    }

    private void loadTheme() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    // ===================== STYLED COMPONENTS =====================

    private JButton styledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setBackground(BUTTON_BG);
        btn.setForeground(TEXT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(BUTTON_HOVER);
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(BUTTON_BG);
            }
        });
        return btn;
    }

    private JButton miniButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 10));
        btn.setBackground(BUTTON_BG);
        btn.setForeground(TEXT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(BUTTON_HOVER);
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(BUTTON_BG);
            }
        });
        return btn;
    }

    // ===================== KEYBOARD SHORTCUTS =====================

    private void bindKeyboardShortcuts() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh");
        am.put("refresh", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { refreshAll(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), "buildAll");
        am.put("buildAll", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { buildAll(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK), "quit");
        am.put("quit", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { promptQuit(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK), "launchSelected");
        am.put("launchSelected", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (selectedApp != null) {
                    launchApp(selectedApp);
                } else {
                    appendOutput("No application selected. Click an app card first.");
                }
            }
        });
    }

    // ===================== TEXT ICON =====================

    static class TextIcon implements Icon {
        private final String text;
        private final int size;
        private final Color color;

        TextIcon(String text, int size) {
            this.text = text;
            this.size = size;
            this.color = new Color(100, 180, 255);
        }

        public int getIconWidth() { return size + 4; }
        public int getIconHeight() { return size + 4; }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setFont(new Font("SansSerif", Font.BOLD, size));
            g2.setColor(color);
            FontMetrics fm = g2.getFontMetrics();
            int textX = x + (getIconWidth() - fm.stringWidth(text)) / 2;
            int textY = y + (getIconHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, textX, textY);
            g2.dispose();
        }
    }

    // ===================== INNER CLASS =====================

    static class AppInfo {
        String id = "";
        String slug = "";
        String name = "";
        String version = "";
        String status = "";
        String language = "";
        String source = "";
        String buildScript = "";
        String buildCommand = "";
        String mainClass = "";
        String visibility = "";
        boolean distributable = false;
        String description = "";
        boolean built = false;
        boolean released = false;
    }

    // ===================== MAIN =====================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            App launcher = new App();
            launcher.setVisible(true);
        });
    }
}
