import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * DevPulse — Datacenter company health dashboard.
 *
 * Reads real data from config/apps.json and the Datacenter file system to
 * display a comprehensive overview of every application in the workspace:
 * lifecycle counts, build results, test outcomes, repository size, source
 * metrics, recent releases, and error logs.
 */
public final class App extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String APP_NAME = "DevPulse";
    private static final String APP_VERSION = "1.0.0";

    private static final Color BG       = new Color(0x1A, 0x1A, 0x2E);
    private static final Color ACCENT   = new Color(0x0F, 0x34, 0x60);
    private static final Color TEXT     = new Color(0xE0, 0xE0, 0xE0);
    private static final Color TEXT_DIM = new Color(0xA0, 0xA0, 0xB0);
    private static final Color CARD_BG  = new Color(0x16, 0x21, 0x3E);
    private static final Color GREEN    = new Color(0x00, 0xC8, 0x53);
    private static final Color YELLOW   = new Color(0xFF, 0xD6, 0x00);
    private static final Color RED      = new Color(0xFF, 0x17, 0x44);
    private static final Color ORANGE   = new Color(0xFF, 0x91, 0x00);

    private final Path root;

    private List<Map<String, String>> applications = new ArrayList<>();
    private Map<String, String> lifecycleStates = new HashMap<>();

    private int totalApps;
    private int activeCount;
    private int developmentCount;
    private int unfinishedCount;
    private int retiredCount;
    private int archivedCount;

    private int buildTotal;
    private int buildPass;
    private int buildFail;
    private int buildSkip;
    private String buildRate = "\u2014";

    private int testTotal;
    private int testPass;
    private int testFail;
    private String testStatusSummary = "\u2014";

    private long repoSizeBytes;
    private String repoSizeFormatted = "\u2014";

    private int totalJavaFiles;
    private int totalPyFiles;
    private int totalJsFiles;
    private int totalHtmlFiles;
    private int totalCsFiles;
    private int totalOtherFiles;

    private int totalLinesOfCode;

    private final List<String> languages = new ArrayList<>();
    private final List<Map<String, String>> releases = new ArrayList<>();
    private final List<Map<String, String>> attentionApps = new ArrayList<>();
    private final List<String> errorLogEntries = new ArrayList<>();

    private JPanel dashboardPanel;
    private JScrollPane scrollPane;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JLabel lastRefreshLabel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            App app = new App();
            app.setVisible(true);
        });
    }

    public App() {
        super(APP_NAME + " v" + APP_VERSION);

        root = resolveRoot();
        if (root == null) {
            JOptionPane.showMessageDialog(null,
                    "Could not locate Datacenter root directory.",
                    APP_NAME, JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1200, 820);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                promptQuit();
            }
        });

        buildMenuBar();
        buildUI();
        bindKeyboardShortcuts();
    }

    private Path resolveRoot() {
        try {
            String classPath = App.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            Path p = Paths.get(classPath);
            Path candidate = p.getParent().getParent().getParent();
            if (candidate != null && Files.isDirectory(candidate)) {
                return candidate;
            }
        } catch (Exception ignored) {
        }

        Path home = Paths.get(System.getProperty("user.home"), "Data");
        if (Files.isDirectory(home)) {
            return home;
        }

        Path cwd = Paths.get(System.getProperty("user.dir"));
        if (cwd.resolve("config").resolve("apps.json").toFile().exists()) {
            return cwd;
        }
        return null;
    }

    private void buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        bar.setBackground(ACCENT);
        bar.setForeground(TEXT);

        JMenu fileMenu = new JMenu("File");
        fileMenu.setForeground(TEXT);
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem refreshItem = new JMenuItem("Refresh (F5)");
        refreshItem.setForeground(TEXT);
        refreshItem.setAccelerator(KeyStroke.getKeyStroke("F5"));
        refreshItem.addActionListener(e -> refreshDashboard());

        JMenuItem quitItem = new JMenuItem("Quit (Ctrl+Q)");
        quitItem.setForeground(TEXT);
        quitItem.setAccelerator(KeyStroke.getKeyStroke("control Q"));
        quitItem.addActionListener(e -> promptQuit());

        fileMenu.add(refreshItem);
        fileMenu.addSeparator();
        fileMenu.add(quitItem);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setForeground(TEXT);
        helpMenu.setMnemonic(KeyEvent.VK_H);

        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.setForeground(TEXT);
        aboutItem.addActionListener(e -> showAboutDialog());

        helpMenu.add(aboutItem);
        bar.add(fileMenu);
        bar.add(helpMenu);
        setJMenuBar(bar);
    }

    private void bindKeyboardShortcuts() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(e -> {
                    if (e.getID() == KeyEvent.KEY_PRESSED) {
                        if (e.getKeyCode() == KeyEvent.VK_F5) {
                            refreshDashboard();
                            return true;
                        }
                        if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Q) {
                            promptQuit();
                            return true;
                        }
                    }
                    return false;
                });
    }

    private void buildUI() {
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ACCENT);
        header.setBorder(new EmptyBorder(12, 18, 12, 18));
        JLabel title = new JLabel(APP_NAME + "  \u2014  Datacenter Health Dashboard");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        title.setForeground(TEXT);
        header.add(title, BorderLayout.WEST);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerRight.setOpaque(false);
        lastRefreshLabel = new JLabel("Not yet refreshed");
        lastRefreshLabel.setForeground(TEXT_DIM);
        headerRight.add(lastRefreshLabel);
        header.add(headerRight, BorderLayout.EAST);
        rootPanel.add(header, BorderLayout.NORTH);

        dashboardPanel = new JPanel();
        dashboardPanel.setBackground(BG);
        dashboardPanel.setLayout(new BoxLayout(dashboardPanel, BoxLayout.Y_AXIS));
        dashboardPanel.setBorder(new EmptyBorder(14, 14, 14, 14));

        scrollPane = new JScrollPane(dashboardPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        scrollPane.setBackground(BG);
        scrollPane.getViewport().setBackground(BG);
        rootPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(ACCENT);
        bottom.setBorder(new EmptyBorder(6, 14, 6, 14));
        statusLabel = new JLabel("Ready  \u2014  Press F5 to scan");
        statusLabel.setForeground(TEXT_DIM);
        bottom.add(statusLabel, BorderLayout.WEST);

        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(220, 16));
        progressBar.setStringPainted(true);
        progressBar.setForeground(GREEN);
        progressBar.setBackground(BG);
        bottom.add(progressBar, BorderLayout.EAST);
        rootPanel.add(bottom, BorderLayout.SOUTH);

        setContentPane(rootPanel);
    }

    /* ================================================================
     * Data collection
     * ================================================================ */
    private void refreshDashboard() {
        progressBar.setValue(0);
        progressBar.setString("Scanning\u2026");
        statusLabel.setText("Scanning file system and config\u2026");

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                collectAllData();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                }
                renderDashboard();
                progressBar.setValue(100);
                progressBar.setString("Done");
                statusLabel.setText("Scan complete  \u2014  F5 to refresh  |  Ctrl+Q to quit");
                String now = new SimpleDateFormat("HH:mm:ss").format(new Date());
                lastRefreshLabel.setText("Last refresh: " + now);
            }
        };
        worker.execute();
    }

    private void collectAllData() {
        applications.clear();
        lifecycleStates.clear();
        languages.clear();
        releases.clear();
        attentionApps.clear();
        errorLogEntries.clear();

        totalApps = 0;
        activeCount = 0;
        developmentCount = 0;
        unfinishedCount = 0;
        retiredCount = 0;
        archivedCount = 0;

        buildTotal = 0;
        buildPass = 0;
        buildFail = 0;
        buildSkip = 0;

        testTotal = 0;
        testPass = 0;
        testFail = 0;

        repoSizeBytes = 0;
        totalJavaFiles = 0;
        totalPyFiles = 0;
        totalJsFiles = 0;
        totalHtmlFiles = 0;
        totalCsFiles = 0;
        totalOtherFiles = 0;
        totalLinesOfCode = 0;

        swingSetProgress(5);
        parseAppsJson();
        swingSetProgress(20);
        scanBuildStatus();
        swingSetProgress(40);
        scanTestStatus();
        swingSetProgress(50);
        scanReleases();
        swingSetProgress(60);
        scanFileSystem();
        swingSetProgress(80);
        computeLanguages();
        scanErrorLogs();
        findAttentionApps();
        swingSetProgress(95);

        repoSizeFormatted = formatBytes(repoSizeBytes);
        buildRate = buildTotal == 0
                ? "N/A"
                : String.format("%.0f%%", (buildPass * 100.0 / buildTotal));
        testStatusSummary = testTotal == 0
                ? "No tests"
                : testPass + " passed / " + testFail + " failed / " + testTotal + " total";
    }

    private void swingSetProgress(int pct) {
        SwingUtilities.invokeLater(() -> progressBar.setValue(pct));
    }

    /* ================================================================
     * 1. Parse config/apps.json (manual string parsing, no external libs)
     * ================================================================ */
    private void parseAppsJson() {
        Path appsJson = root.resolve("config").resolve("apps.json");
        if (!appsJson.toFile().exists()) {
            return;
        }
        try {
            String raw = new String(Files.readAllBytes(appsJson), "UTF-8");

            int lsIdx = raw.indexOf("\"lifecycle_states\"");
            if (lsIdx >= 0) {
                int braceOpen = raw.indexOf('{', lsIdx);
                if (braceOpen >= 0) {
                    int braceClose = findMatchingBrace(raw, braceOpen);
                    if (braceClose > 0) {
                        String block = raw.substring(braceOpen, braceClose + 1);
                        extractStringPairs(block, lifecycleStates);
                    }
                }
            }

            int appsIdx = raw.indexOf("\"applications\"");
            if (appsIdx < 0) {
                return;
            }
            int arrOpen = raw.indexOf('[', appsIdx);
            if (arrOpen < 0) {
                return;
            }
            int arrClose = findMatchingBracket(raw, arrOpen);
            if (arrClose < 0) {
                return;
            }
            String arrayContent = raw.substring(arrOpen + 1, arrClose);

            int depth = 0;
            int start = -1;
            for (int i = 0; i < arrayContent.length(); i++) {
                char c = arrayContent.charAt(i);
                if (c == '{' && depth == 0) {
                    start = i;
                    depth = 1;
                } else if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && start >= 0) {
                        String obj = arrayContent.substring(start + 1, i);
                        Map<String, String> app = extractStringPairs(obj, null);
                        if (app != null) {
                            applications.add(app);
                        }
                        start = -1;
                    }
                }
            }

            totalApps = applications.size();
            for (Map<String, String> app : applications) {
                String st = app.getOrDefault("status", "");
                if ("ACTIVE".equals(st)) {
                    activeCount++;
                } else if ("DEVELOPMENT".equals(st)) {
                    developmentCount++;
                } else if ("UNFINISHED".equals(st)) {
                    unfinishedCount++;
                } else if ("RETIRED".equals(st)) {
                    retiredCount++;
                } else if ("ARCHIVED".equals(st)) {
                    archivedCount++;
                }
            }
        } catch (Exception e) {
            statusLabel.setText("Error reading apps.json: " + e.getMessage());
        }
    }

    private static int findMatchingBrace(String s, int pos) {
        int depth = 0;
        for (int i = pos; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int findMatchingBracket(String s, int pos) {
        int depth = 0;
        for (int i = pos; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static Map<String, String> extractStringPairs(String fragment, Map<String, String> map) {
        if (map == null) {
            map = new LinkedHashMap<String, String>();
        }
        Pattern p = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher m = p.matcher(fragment);
        while (m.find()) {
            map.put(m.group(1), unescapeJson(m.group(2)));
        }
        return map;
    }

    private static String unescapeJson(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(++i);
                switch (next) {
                    case '"':  sb.append('"');  break;
                    case 'n':  sb.append('\n'); break;
                    case 't':  sb.append('\t'); break;
                    case '\\': sb.append('\\'); break;
                    default:   sb.append('\\').append(next); break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /* ================================================================
     * 2. Build status
     * ================================================================ */
    private void scanBuildStatus() {
        for (Map<String, String> app : applications) {
            String slug = app.getOrDefault("slug", "");
            if (slug.isEmpty()) {
                continue;
            }
            String buildCmd = app.getOrDefault("build_command", "");
            String buildScript = app.getOrDefault("build_script", "");

            if (buildCmd.isEmpty() && buildScript.isEmpty()) {
                buildSkip++;
                continue;
            }

            buildTotal++;
            boolean passed = false;
            boolean srcMissing = false;

            Path buildDir = root.resolve("build").resolve("apps").resolve(slug);
            if (buildDir.toFile().isDirectory()) {
                try {
                    long classCount = countFilesRecursive(buildDir, ".class");
                    if (classCount > 0) {
                        passed = true;
                    }
                } catch (Exception ignored) {
                }
            }

            if (!passed) {
                String source = app.getOrDefault("source", "");
                if (!source.isEmpty()) {
                    Path srcDir = root.resolve(source);
                    if (!srcDir.toFile().isDirectory()) {
                        srcMissing = true;
                    } else {
                        try {
                            long srcFiles = countFilesRecursive(srcDir, ".java");
                            if (srcFiles == 0) {
                                srcMissing = true;
                            }
                        } catch (Exception e) {
                            srcMissing = true;
                        }
                    }
                }
            }

            if (passed) {
                buildPass++;
                app.put("_buildStatus", "PASS");
            } else if (srcMissing) {
                buildFail++;
                app.put("_buildStatus", "FAIL");
            } else {
                buildFail++;
                app.put("_buildStatus", "WARN");
            }
        }
    }

    private static long countFilesRecursive(Path dir, String suffix) throws IOException {
        final long[] count = {0};
        Files.walk(dir).filter(Files::isRegularFile).forEach(p -> {
            if (p.toString().endsWith(suffix)) {
                count[0]++;
            }
        });
        return count[0];
    }

    /* ================================================================
     * 3. Test status
     * ================================================================ */
    private void scanTestStatus() {
        Path testsDir = root.resolve("build").resolve("tests");
        if (!testsDir.toFile().isDirectory()) {
            return;
        }
        try {
            File[] dirs = testsDir.toFile().listFiles();
            if (dirs == null) {
                return;
            }
            for (File d : dirs) {
                if (!d.isDirectory()) {
                    continue;
                }
                testTotal++;
                try {
                    long classes = countFilesRecursive(d.toPath(), ".class");
                    if (classes > 0) {
                        testPass++;
                    } else {
                        testFail++;
                    }
                } catch (Exception ignored) {
                    testFail++;
                }
            }
        } catch (Exception ignored) {
        }
    }

    /* ================================================================
     * 4. Releases
     * ================================================================ */
    private void scanReleases() {
        File relDir = root.resolve("releases").toFile();
        if (!relDir.isDirectory()) {
            return;
        }
        File[] appDirs = relDir.listFiles();
        if (appDirs == null) {
            return;
        }
        for (File appDir : appDirs) {
            if (!appDir.isDirectory()) {
                continue;
            }
            File[] versions = appDir.listFiles();
            if (versions == null) {
                continue;
            }
            for (File verDir : versions) {
                if (!verDir.isDirectory()) {
                    continue;
                }
                Map<String, String> rel = new LinkedHashMap<String, String>();
                rel.put("app", appDir.getName());
                rel.put("version", verDir.getName());

                List<String> artifacts = new ArrayList<String>();
                File[] files = verDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        artifacts.add(f.getName());
                    }
                }
                rel.put("artifacts", join(", ", artifacts));
                releases.add(rel);
            }
        }
    }

    /* ================================================================
     * 5. File system scan
     * ================================================================ */
    private void scanFileSystem() {
        Set<String> scannedDirs = new HashSet<String>();
        for (Map<String, String> app : applications) {
            String source = app.getOrDefault("source", "");
            if (source.isEmpty()) {
                continue;
            }
            Path srcDir = root.resolve(source);
            if (!srcDir.toFile().isDirectory() || scannedDirs.contains(srcDir.toString())) {
                continue;
            }
            scannedDirs.add(srcDir.toString());
            scanDirectory(srcDir);
        }

        String[] extraPaths = {"main", "memory", "installer", "velice", "info", "languages"};
        for (String ep : extraPaths) {
            Path p = root.resolve(ep);
            if (p.toFile().isDirectory() && !scannedDirs.contains(p.toString())) {
                scanDirectory(p);
            }
        }
    }

    private void scanDirectory(Path dir) {
        try {
            Files.walk(dir).filter(Files::isRegularFile).forEach(file -> {
                String name = file.toString();
                repoSizeBytes += fileSizeSafe(file);

                if (name.endsWith(".java")) {
                    totalJavaFiles++;
                } else if (name.endsWith(".py")) {
                    totalPyFiles++;
                } else if (name.endsWith(".js")) {
                    totalJsFiles++;
                } else if (name.endsWith(".html")) {
                    totalHtmlFiles++;
                } else if (name.endsWith(".cs")) {
                    totalCsFiles++;
                } else {
                    totalOtherFiles++;
                }

                if (isCountableSource(name)) {
                    totalLinesOfCode += countLinesSafe(file);
                }
            });
        } catch (IOException ignored) {
        }
    }

    private static long fileSizeSafe(Path p) {
        try {
            return Files.size(p);
        } catch (IOException e) {
            return 0;
        }
    }

    private static boolean isCountableSource(String name) {
        return name.endsWith(".java") || name.endsWith(".py")
                || name.endsWith(".js")  || name.endsWith(".ts")
                || name.endsWith(".html")|| name.endsWith(".css")
                || name.endsWith(".cs")  || name.endsWith(".rs")
                || name.endsWith(".go")  || name.endsWith(".c")
                || name.endsWith(".cpp") || name.endsWith(".h")
                || name.endsWith(".hpp") || name.endsWith(".sh")
                || name.endsWith(".bash")|| name.endsWith(".xml")
                || name.endsWith(".json")|| name.endsWith(".yml")
                || name.endsWith(".yaml")|| name.endsWith(".toml");
    }

    private static int countLinesSafe(Path file) {
        try {
            if (Files.size(file) > 2 * 1024 * 1024) {
                return 0;
            }
            byte[] bytes = Files.readAllBytes(file);
            int lines = 1;
            for (byte b : bytes) {
                if (b == '\n') {
                    lines++;
                }
            }
            return lines;
        } catch (Exception e) {
            return 0;
        }
    }

    /* ================================================================
     * 6. Languages
     * ================================================================ */
    private void computeLanguages() {
        Map<String, Integer> counts = new TreeMap<String, Integer>();
        for (Map<String, String> app : applications) {
            String lang = app.getOrDefault("language", "Unknown");
            Integer cur = counts.get(lang);
            counts.put(lang, cur == null ? 1 : cur + 1);
        }
        languages.clear();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            languages.add(e.getValue() + "\u00D7 " + e.getKey());
        }
    }

    /* ================================================================
     * 7. Error logs
     * ================================================================ */
    private void scanErrorLogs() {
        File errDir = root.resolve("logs").resolve("errors").toFile();
        if (!errDir.isDirectory()) {
            return;
        }
        File[] files = errDir.listFiles();
        if (files == null) {
            return;
        }

        Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        int count = 0;
        for (File f : files) {
            if (!f.isFile() || count >= 5) {
                continue;
            }
            errorLogEntries.add("=== " + f.getName() + " ===");
            try {
                BufferedReader br = new BufferedReader(new FileReader(f));
                int linesRead = 0;
                String line;
                while ((line = br.readLine()) != null && linesRead < 3) {
                    errorLogEntries.add("  " + line);
                    linesRead++;
                }
                br.close();
            } catch (IOException e) {
                errorLogEntries.add("  (unreadable)");
            }
            count++;
        }
    }

    /* ================================================================
     * 8. Attention apps
     * ================================================================ */
    private void findAttentionApps() {
        attentionApps.clear();
        for (Map<String, String> app : applications) {
            String bs = app.getOrDefault("_buildStatus", "");
            if ("WARN".equals(bs) || "FAIL".equals(bs)) {
                attentionApps.add(app);
            }
        }
    }

    /* ================================================================
     * Formatting helpers
     * ================================================================ */
    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024L * 1024 * 1024) {
            return String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format(java.util.Locale.US, "%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    private static String statusEmoji(String status) {
        if ("ACTIVE".equals(status))      return "\u2705";
        if ("DEVELOPMENT".equals(status)) return "\u2699\uFE0F";
        if ("UNFINISHED".equals(status))  return "\u26A0\uFE0F";
        if ("RETIRED".equals(status))     return "\u274C";
        if ("ARCHIVED".equals(status))    return "\uD83D\uDCD6";
        return "\u2753";
    }

    private static Color statusColor(String status) {
        if ("ACTIVE".equals(status))      return GREEN;
        if ("DEVELOPMENT".equals(status)) return YELLOW;
        if ("UNFINISHED".equals(status))  return ORANGE;
        if ("RETIRED".equals(status))     return RED;
        if ("ARCHIVED".equals(status))    return RED;
        return TEXT_DIM;
    }

    private static Color buildStatusColor(String st) {
        if ("PASS".equals(st)) return GREEN;
        if ("WARN".equals(st)) return YELLOW;
        if ("FAIL".equals(st)) return RED;
        return TEXT_DIM;
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 3) + "\u2026";
    }

    private static String join(String sep, List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(sep);
            }
            sb.append(items.get(i));
        }
        return sb.toString();
    }

    /* ================================================================
     * Rendering
     * ================================================================ */
    private void renderDashboard() {
        dashboardPanel.removeAll();
        dashboardPanel.setLayout(new BoxLayout(dashboardPanel, BoxLayout.Y_AXIS));

        /* Row 1: lifecycle overview */
        JPanel row1 = makeRow();
        row1.add(makeMetricCard("Apps Total", String.valueOf(totalApps), "\uD83C\uDFE2", GREEN));
        row1.add(makeMetricCard("Active", String.valueOf(activeCount), "\u2705", GREEN));
        row1.add(makeMetricCard("In Development", String.valueOf(developmentCount), "\u2699\uFE0F", YELLOW));
        row1.add(makeMetricCard("Unfinished", String.valueOf(unfinishedCount), "\u26A0\uFE0F", ORANGE));
        row1.add(makeMetricCard("Retired", String.valueOf(retiredCount), "\u274C", RED));
        dashboardPanel.add(row1);
        dashboardPanel.add(Box.createVerticalStrut(10));

        /* Row 2: build / tests / releases */
        JPanel row2 = makeRow();
        row2.add(makeMetricCard("Build Success Rate", buildRate, "\uD83D\uDD27",
                buildFail > 0 ? RED : GREEN));
        row2.add(makeMetricCard("Builds", buildPass + "/" + buildTotal,
                buildFail > 0 ? "\u274C" : "\u2705", buildFail > 0 ? RED : GREEN));
        row2.add(makeMetricCard("Tests", testStatusSummary, "\uD83E\uDDEA",
                testFail > 0 ? RED : GREEN));
        row2.add(makeMetricCard("Releases", String.valueOf(releases.size()), "\uD83C\uDF81", GREEN));
        row2.add(makeMetricCard("Attention", String.valueOf(attentionApps.size()),
                attentionApps.isEmpty() ? "\uD83D\uDE0A" : "\uD83D\uDEA8",
                attentionApps.isEmpty() ? GREEN : RED));
        dashboardPanel.add(row2);
        dashboardPanel.add(Box.createVerticalStrut(10));

        /* Row 3: file metrics */
        JPanel row3 = makeRow();
        row3.add(makeMetricCard("Repo Size", repoSizeFormatted, "\uD83D\uDCC2", ACCENT));
        row3.add(makeMetricCard("Java Files", String.valueOf(totalJavaFiles), "\uD83D\uDCCA", ACCENT));
        row3.add(makeMetricCard("Python Files", String.valueOf(totalPyFiles), "\uD83D\uDC0D", ACCENT));
        row3.add(makeMetricCard("JS Files", String.valueOf(totalJsFiles), "\u26A1", ACCENT));
        row3.add(makeMetricCard("HTML Files", String.valueOf(totalHtmlFiles), "\uD83C\uDF10", ACCENT));
        dashboardPanel.add(row3);
        dashboardPanel.add(Box.createVerticalStrut(10));

        /* Row 4: more metrics */
        JPanel row4 = makeRow();
        int totalAllFiles = totalJavaFiles + totalPyFiles + totalJsFiles
                + totalHtmlFiles + totalCsFiles + totalOtherFiles;
        row4.add(makeMetricCard("C# Files", String.valueOf(totalCsFiles), "\uD83D\uDD35", ACCENT));
        row4.add(makeMetricCard("Total Files", String.valueOf(totalAllFiles), "\uD83D\uDCC1", ACCENT));
        row4.add(makeMetricCard("Lines of Code", String.valueOf(totalLinesOfCode), "\u270D\uFE0F", ACCENT));
        dashboardPanel.add(row4);
        dashboardPanel.add(Box.createVerticalStrut(14));

        /* Languages */
        dashboardPanel.add(makeSectionHeader("Languages Used"));
        JPanel langPanel = makeCardPanel();
        if (languages.isEmpty()) {
            langPanel.add(makeInfoLabel("No languages detected"));
        } else {
            for (String lang : languages) {
                langPanel.add(makeInfoLabel("\u2022  " + lang));
            }
        }
        dashboardPanel.add(langPanel);
        dashboardPanel.add(Box.createVerticalStrut(14));

        /* Releases */
        dashboardPanel.add(makeSectionHeader("Latest Releases"));
        JPanel relPanel = makeCardPanel();
        if (releases.isEmpty()) {
            relPanel.add(makeInfoLabel("No releases found"));
        } else {
            for (Map<String, String> rel : releases) {
                String line = rel.getOrDefault("app", "?")
                        + "  v" + rel.getOrDefault("version", "?");
                String artifacts = rel.getOrDefault("artifacts", "");
                if (!artifacts.isEmpty()) {
                    line += "  \u2014  " + artifacts;
                }
                relPanel.add(makeInfoLabel("\uD83C\uDF81  " + line));
            }
        }
        dashboardPanel.add(relPanel);
        dashboardPanel.add(Box.createVerticalStrut(14));

        /* Attention */
        dashboardPanel.add(makeSectionHeader("Applications Needing Attention"));
        JPanel attPanel = makeCardPanel();
        if (attentionApps.isEmpty()) {
            attPanel.add(makeInfoLabel("No apps need attention \uD83D\uDE0A"));
        } else {
            for (Map<String, String> app : attentionApps) {
                String name = app.getOrDefault("name", app.getOrDefault("slug", "?"));
                String bs = app.getOrDefault("_buildStatus", "??");
                String desc = app.getOrDefault("description", "");
                JPanel arow = new JPanel(new BorderLayout());
                arow.setOpaque(false);
                arow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                JLabel nameLabel = new JLabel(name + "  [" + bs + "]");
                nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
                nameLabel.setForeground(buildStatusColor(bs));
                arow.add(nameLabel, BorderLayout.WEST);
                if (!desc.isEmpty()) {
                    JLabel descLabel = new JLabel(truncate(desc, 70));
                    descLabel.setForeground(TEXT_DIM);
                    arow.add(descLabel, BorderLayout.EAST);
                }
                attPanel.add(arow);
            }
        }
        dashboardPanel.add(attPanel);
        dashboardPanel.add(Box.createVerticalStrut(14));

        /* Error logs */
        dashboardPanel.add(makeSectionHeader("Recent Log Entries (logs/errors/)"));
        JPanel logPanel = makeCardPanel();
        if (errorLogEntries.isEmpty()) {
            logPanel.add(makeInfoLabel("No error log files found \uD83D\uDE0E"));
        } else {
            for (String entry : errorLogEntries) {
                JLabel lbl = makeInfoLabel(entry);
                if (entry.startsWith("===")) {
                    lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
                    lbl.setForeground(YELLOW);
                }
                logPanel.add(lbl);
            }
        }
        dashboardPanel.add(logPanel);
        dashboardPanel.add(Box.createVerticalStrut(14));

        /* Application table */
        dashboardPanel.add(makeSectionHeader("All Applications (" + totalApps + ")"));
        JPanel appTablePanel = makeCardPanel();
        for (Map<String, String> app : applications) {
            appTablePanel.add(makeAppRow(app));
        }
        dashboardPanel.add(appTablePanel);

        Box.Filler filler = new Box.Filler(
                new Dimension(0, 40), new Dimension(0, 40), new Dimension(0, 40));
        dashboardPanel.add(filler);

        dashboardPanel.revalidate();
        dashboardPanel.repaint();
    }

    /* ================================================================
     * UI builders
     * ================================================================ */
    private JPanel makeRow() {
        JPanel row = new JPanel();
        row.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        return row;
    }

    private JPanel makeMetricCard(String title, String value, String icon, Color accentColor) {
        JPanel card = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(accentColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(accentColor);
                g2.fillRoundRect(0, 0, getWidth(), 4, 12, 12);
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(200, 120));
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(12, 14, 10, 14));

        JLabel iconLabel = new JLabel(icon + "  " + title);
        iconLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        iconLabel.setForeground(TEXT_DIM);
        card.add(iconLabel, BorderLayout.NORTH);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        valueLabel.setForeground(TEXT);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private JPanel makeSectionHeader(String text) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        label.setForeground(TEXT);
        panel.add(label);
        return panel;
    }

    private JPanel makeCardPanel() {
        JPanel panel = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(ACCENT);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 14, 10, 14));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 600));
        return panel;
    }

    private JLabel makeInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        label.setForeground(TEXT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        return label;
    }

    private JPanel makeAppRow(Map<String, String> app) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setBorder(new EmptyBorder(2, 0, 2, 0));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.insets = new Insets(0, 4, 0, 12);
        gc.anchor = GridBagConstraints.WEST;

        String emoji = statusEmoji(app.getOrDefault("status", ""));
        JLabel emojiLabel = new JLabel(emoji);
        emojiLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        gc.gridx = 0;
        gc.weightx = 0;
        row.add(emojiLabel, gc);

        String name = app.getOrDefault("name", app.getOrDefault("slug", "?"));
        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        nameLabel.setForeground(TEXT);
        gc.gridx = 1;
        gc.weightx = 0;
        row.add(nameLabel, gc);

        String status = app.getOrDefault("status", "?");
        JLabel statusLabel2 = new JLabel("[" + status + "]");
        statusLabel2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        statusLabel2.setForeground(statusColor(status));
        gc.gridx = 2;
        gc.weightx = 0;
        row.add(statusLabel2, gc);

        String lang = app.getOrDefault("language", "");
        if (!lang.isEmpty()) {
            JLabel langLabel = new JLabel(lang);
            langLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            langLabel.setForeground(TEXT_DIM);
            gc.gridx = 3;
            gc.weightx = 0;
            row.add(langLabel, gc);
        }

        String version = app.getOrDefault("version", "");
        if (!version.isEmpty()) {
            JLabel verLabel = new JLabel("v" + version);
            verLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            verLabel.setForeground(TEXT_DIM);
            gc.gridx = 4;
            gc.weightx = 0;
            row.add(verLabel, gc);
        }

        String bs = app.getOrDefault("_buildStatus", "");
        if (!bs.isEmpty()) {
            JLabel buildLabel = new JLabel("[" + bs + "]");
            buildLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            buildLabel.setForeground(buildStatusColor(bs));
            gc.gridx = 5;
            gc.weightx = 0;
            row.add(buildLabel, gc);
        }

        String desc = app.getOrDefault("description", "");
        if (!desc.isEmpty()) {
            JLabel descLabel = new JLabel("  " + truncate(desc, 60));
            descLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            descLabel.setForeground(TEXT_DIM);
            gc.gridx = 6;
            gc.weightx = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            row.add(descLabel, gc);
        }

        return row;
    }

    /* ================================================================
     * Dialogs
     * ================================================================ */
    private void promptQuit() {
        int result = JOptionPane.showConfirmDialog(
                this,
                "Quit " + APP_NAME + "?",
                APP_NAME,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            dispose();
            System.exit(0);
        }
    }

    private void showAboutDialog() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));

        String[] lines = {
            APP_NAME + " v" + APP_VERSION,
            "",
            "Datacenter company health dashboard.",
            "Reads real data from config/apps.json and the file system.",
            "",
            "Java Version: " + System.getProperty("java.version"),
            "Java Vendor:  " + System.getProperty("java.vendor"),
            "OS:           " + System.getProperty("os.name") + " "
                    + System.getProperty("os.version") + " "
                    + System.getProperty("os.arch"),
            "User:         " + System.getProperty("user.name"),
            "Root:         " + root,
            "",
            "Keyboard shortcuts:",
            "  F5      \u2014 Refresh dashboard",
            "  Ctrl+Q  \u2014 Quit application",
        };
        for (String line : lines) {
            JLabel lbl = new JLabel(line);
            lbl.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            lbl.setForeground(TEXT);
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(lbl);
        }

        JOptionPane.showMessageDialog(this, panel, "About " + APP_NAME,
                JOptionPane.PLAIN_MESSAGE);
    }
}
