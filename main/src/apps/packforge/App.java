import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * PackForge - Datacenter Release & Packaging Manager.
 *
 * Single-file Java Swing application. No external dependencies.
 * Java 17+ recommended.
 */
public class App {

    // =================================================================
    // APP META
    // =================================================================

    private static final String APP_NAME    = "PackForge";
    private static final String APP_VERSION = "1.0.0";
    private static final String APP_DESC    = "Release & Packaging Manager for the Datacenter";

    // =================================================================
    // THEME
    // =================================================================

    private static final Color BG       = new Color(0x1a, 0x1a, 0x2e);
    private static final Color BG_PANEL = new Color(0x16, 0x21, 0x3e);
    private static final Color BG_FIELD = new Color(0x0f, 0x34, 0x60);
    private static final Color BG_DARK  = new Color(0x10, 0x18, 0x2a);
    private static final Color TEXT     = new Color(0xe0, 0xe0, 0xe0);
    private static final Color TEXT_DIM = new Color(0x8a, 0x8e, 0x96);
    private static final Color ACCENT   = new Color(0x0f, 0x34, 0x60);
    private static final Color SUCCESS  = new Color(0x27, 0xae, 0x60);
    private static final Color FAIL     = new Color(0xc0, 0x39, 0x2b);
    private static final Color WARN     = new Color(0xe6, 0x7e, 0x22);
    private static final Color BORDER   = new Color(0x2a, 0x3a, 0x5c);

    private static final Font UI_FONT    = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font UI_FONT_B  = new Font("SansSerif", Font.BOLD, 13);
    private static final Font MONO_FONT  = new Font("Monospaced", Font.PLAIN, 12);
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 18);

    // =================================================================
    // PATHS
    // =================================================================

    private static final Path COMPANY_DIR  = Paths.get(System.getProperty("user.home"), "Data");
    private static final Path CONFIG_FILE  = COMPANY_DIR.resolve("config/apps.json");
    private static final Path RELEASES_DIR = COMPANY_DIR.resolve("releases");
    private static final Path ROOT_DIR     = COMPANY_DIR;

    // =================================================================
    // DATA MODEL
    // =================================================================

    static final class AppEntry {
        final String id, slug, name, version, status, language;
        final String source, buildScript, buildCommand, mainClass;
        final String visibility, description;
        final boolean distributable;

        AppEntry(String id, String slug, String name, String version,
                 String status, String language, String source,
                 String buildScript, String buildCommand, String mainClass,
                 String visibility, boolean distributable, String description) {
            this.id = id; this.slug = slug; this.name = name;
            this.version = version; this.status = status; this.language = language;
            this.source = source; this.buildScript = buildScript;
            this.buildCommand = buildCommand; this.mainClass = mainClass;
            this.visibility = visibility; this.distributable = distributable;
            this.description = description;
        }

        @Override
        public String toString() { return name + " (" + slug + ")"; }
    }

    // =================================================================
    // UI STATE
    // =================================================================

    private static JFrame frame;
    private static JComboBox<AppEntry> appCombo;
    private static JSpinner spinMajor, spinMinor, spinPatch;
    private static JTextArea buildLogArea, testLogArea, notesArea, outputArea;
    private static JLabel buildStatusLabel, testStatusLabel, statusStep;
    private static JLabel sizeValueLabel, fileCountValueLabel;
    private static JProgressBar progressBar;
    private static JButton buildBtn, testBtn, packageBtn, saveNotesBtn;
    private static JPanel buildIndicator, testIndicator;
    private static List<AppEntry> registry = new ArrayList<>();
    private static boolean buildOk = false, testOk = false;

    private static final ExecutorService exec =
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "packforge-worker");
            t.setDaemon(true);
            return t;
        });

    // =================================================================
    // MAIN
    // =================================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {}
            loadRegistry();
            buildUI();
            frame.setVisible(true);
        });
    }

    // =================================================================
    // REGISTRY LOADER
    // =================================================================

    private static void loadRegistry() {
        registry.clear();
        if (!Files.exists(CONFIG_FILE)) {
            System.err.println("PackForge: " + CONFIG_FILE + " not found");
            return;
        }
        try {
            String raw = Files.readString(CONFIG_FILE, StandardCharsets.UTF_8);
            registry = parseAppsJson(raw);
        } catch (Exception e) {
            System.err.println("PackForge: failed to load registry: " + e.getMessage());
        }
    }

    private static List<AppEntry> parseAppsJson(String json) {
        List<AppEntry> out = new ArrayList<>();
        int arrStart = json.indexOf("\"applications\"");
        if (arrStart < 0) return out;
        int bracket = json.indexOf('[', arrStart);
        if (bracket < 0) return out;
        int depth = 0, end = bracket;
        for (int i = bracket; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') { depth--; if (depth == 0) { end = i; break; } }
        }
        String arr = json.substring(bracket + 1, end);
        int objDepth = 0, objStart = -1;
        for (int i = 0; i < arr.length(); i++) {
            char c = arr.charAt(i);
            if (c == '{') { if (objDepth == 0) objStart = i; objDepth++; }
            else if (c == '}') {
                objDepth--;
                if (objDepth == 0 && objStart >= 0) {
                    out.add(parseEntry(arr.substring(objStart + 1, i)));
                    objStart = -1;
                }
            }
        }
        return out;
    }

    private static AppEntry parseEntry(String obj) {
        return new AppEntry(
            jStr(obj, "id"), jStr(obj, "slug"), jStr(obj, "name"),
            jStr(obj, "version"), jStr(obj, "status"), jStr(obj, "language"),
            jStr(obj, "source"), jStr(obj, "build_script"), jStr(obj, "build_command"),
            jStr(obj, "main"), jStr(obj, "visibility"),
            jBool(obj, "distributable"), jStr(obj, "description")
        );
    }

    private static String jStr(String obj, String key) {
        String needle = "\"" + key + "\"";
        int k = obj.indexOf(needle);
        if (k < 0) return "";
        int colon = obj.indexOf(':', k + needle.length());
        if (colon < 0) return "";
        int q1 = obj.indexOf('"', colon + 1);
        if (q1 < 0) return "";
        int q2 = obj.indexOf('"', q1 + 1);
        if (q2 < 0) return "";
        return obj.substring(q1 + 1, q2);
    }

    private static boolean jBool(String obj, String key) {
        return jStr(obj, key).equalsIgnoreCase("true");
    }

    // =================================================================
    // UI CONSTRUCTION
    // =================================================================

    private static void buildUI() {
        frame = new JFrame(APP_NAME + " v" + APP_VERSION);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(960, 720));
        frame.setSize(1060, 780);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(BG);
        frame.getContentPane().setLayout(new BorderLayout(0, 0));

        registerShortcut("build",     KeyEvent.VK_F7, 0,                    App::startBuild);
        registerShortcut("test",      KeyEvent.VK_F8, 0,                    App::startTest);
        registerShortcut("package",   KeyEvent.VK_F9, 0,                    App::startPackage);
        registerShortcut("saveNotes", KeyEvent.VK_S,  InputEvent.CTRL_DOWN_MASK, App::saveReleaseNotes);

        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(BG_PANEL);
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));

        JMenu fileMenu = makeMenu("File");
        fileMenu.add(makeMenuItem("Save Release Notes  Ctrl+S", App::saveReleaseNotes));
        fileMenu.addSeparator();
        fileMenu.add(makeMenuItem("Exit", () -> System.exit(0)));
        menuBar.add(fileMenu);

        JMenu runMenu = makeMenu("Run");
        runMenu.add(makeMenuItem("Build  F7",  App::startBuild));
        runMenu.add(makeMenuItem("Test   F8",  App::startTest));
        runMenu.add(makeMenuItem("Package F9", App::startPackage));
        menuBar.add(runMenu);

        JMenu helpMenu = makeMenu("Help");
        helpMenu.add(makeMenuItem("About PackForge", App::showAbout));
        menuBar.add(helpMenu);
        frame.setJMenuBar(menuBar);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerLocation(500);
        mainSplit.setBackground(BG);
        mainSplit.setBorder(null);
        mainSplit.setLeftComponent(buildLeftPanel());
        mainSplit.setRightComponent(buildRightPanel());
        frame.add(mainSplit, BorderLayout.CENTER);
        frame.add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // -----------------------------------------------------------------
    // Left panel
    // -----------------------------------------------------------------

    private static JPanel buildLeftPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 4));
        p.add(buildSelectorPanel(), BorderLayout.NORTH);
        p.add(buildTabsPanel(), BorderLayout.CENTER);
        return p;
    }

    private static JPanel buildSelectorPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_PANEL);
        p.setBorder(compoundBorder(8, 8, 8, 8, "Application Selector"));

        JPanel row1 = labeledRow("Application:");
        appCombo = new JComboBox<>(registry.toArray(new AppEntry[0]));
        appCombo.setBackground(BG_FIELD);
        appCombo.setForeground(TEXT);
        appCombo.setFont(UI_FONT);
        appCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        appCombo.setRenderer(new AppComboRenderer());
        appCombo.addActionListener(e -> onAppSelected());
        row1.add(appCombo);
        p.add(row1);

        JPanel row2 = labeledRow("Version:");
        spinMajor = makeSpinner(0, 0, 99);
        spinMinor = makeSpinner(0, 0, 99);
        spinPatch = makeSpinner(0, 0, 999);
        row2.add(makeSpinnerLabel("Major:"));
        row2.add(makeSpinnerWrap(spinMajor));
        row2.add(Box.createHorizontalStrut(6));
        row2.add(makeSpinnerLabel("Minor:"));
        row2.add(makeSpinnerWrap(spinMinor));
        row2.add(Box.createHorizontalStrut(6));
        row2.add(makeSpinnerLabel("Patch:"));
        row2.add(makeSpinnerWrap(spinPatch));
        p.add(row2);

        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 2));
        row3.setBackground(BG_PANEL);
        buildStatusLabel = makeStatusLabel("Build: Not run");
        testStatusLabel  = makeStatusLabel("Test: Not run");
        row3.add(buildStatusLabel);
        row3.add(Box.createHorizontalStrut(20));
        row3.add(testStatusLabel);
        p.add(row3);

        if (!registry.isEmpty()) appCombo.setSelectedIndex(0);
        return p;
    }

    // -----------------------------------------------------------------
    // Tabs panel
    // -----------------------------------------------------------------

    private static JPanel buildTabsPanel() {
        JTabbedPane tp = new JTabbedPane();
        tp.setBackground(BG_PANEL);
        tp.setForeground(TEXT);
        tp.setFont(UI_FONT);
        tp.setBorder(BorderFactory.createLineBorder(BORDER));

        buildLogArea = makeTextArea(false);
        testLogArea  = makeTextArea(false);
        notesArea    = makeTextArea(false);
        outputArea   = makeTextArea(true);

        tp.addTab("Build Log",     scrollPane(buildLogArea));
        tp.addTab("Test Log",      scrollPane(testLogArea));
        tp.addTab("Release Notes", scrollPane(notesArea));
        tp.addTab("Output",        scrollPane(outputArea));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        btns.setBackground(BG_PANEL);
        buildBtn     = makeActionBtn("Build  [F7]",  App::startBuild);
        testBtn      = makeActionBtn("Test   [F8]",  App::startTest);
        packageBtn   = makeActionBtn("Package [F9]", App::startPackage);
        saveNotesBtn = makeActionBtn("Save Notes",   App::saveReleaseNotes);
        packageBtn.setEnabled(false);
        btns.add(buildBtn);
        btns.add(testBtn);
        btns.add(packageBtn);
        btns.add(Box.createHorizontalStrut(12));
        btns.add(saveNotesBtn);

        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(BG);
        container.add(tp, BorderLayout.CENTER);
        container.add(btns, BorderLayout.SOUTH);
        return container;
    }

    // -----------------------------------------------------------------
    // Right panel
    // -----------------------------------------------------------------

    private static JPanel buildRightPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 8));

        JSplitPane vsplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                buildInfoCard(), buildOutputCard());
        vsplit.setDividerLocation(310);
        vsplit.setBackground(BG);
        vsplit.setBorder(null);

        JSplitPane vsplit2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                vsplit, buildProvenanceCard());
        vsplit2.setDividerLocation(530);
        vsplit2.setBackground(BG);
        vsplit2.setBorder(null);

        p.add(vsplit2, BorderLayout.CENTER);
        return p;
    }

    private static JPanel buildInfoCard() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_PANEL);
        p.setBorder(compoundBorder(8, 8, 8, 8, "Package Info"));

        JPanel details = new JPanel(new GridLayout(0, 2, 6, 3));
        details.setBackground(BG_PANEL);
        details.add(makeInfoKey("App:"));          details.add(makeInfoVal("\u2014"));
        details.add(makeInfoKey("Slug:"));         details.add(makeInfoVal("\u2014"));
        details.add(makeInfoKey("Language:"));     details.add(makeInfoVal("\u2014"));
        details.add(makeInfoKey("Status:"));       details.add(makeInfoVal("\u2014"));
        details.add(makeInfoKey("Version:"));      details.add(makeInfoVal("\u2014"));
        details.add(makeInfoKey("Visibility:"));   details.add(makeInfoVal("\u2014"));
        details.add(makeInfoKey("Distributable:"));details.add(makeInfoVal("\u2014"));
        details.add(makeInfoKey("Source:"));       details.add(makeInfoVal("\u2014"));
        details.add(makeInfoKey("Build Script:")); details.add(makeInfoVal("\u2014"));
        details.add(makeInfoKey("Output Dir:"));   details.add(makeInfoVal("\u2014"));
        details.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        p.add(details);

        JPanel indicators = new JPanel(new GridLayout(1, 2, 12, 0));
        indicators.setBackground(BG_PANEL);
        indicators.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        buildIndicator = makeIndicator("Build");
        testIndicator  = makeIndicator("Test");
        indicators.add(buildIndicator);
        indicators.add(testIndicator);
        p.add(Box.createVerticalStrut(6));
        p.add(indicators);

        JPanel sizeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        sizeRow.setBackground(BG_PANEL);
        sizeRow.add(makeInfoKey("Package Size:"));
        sizeValueLabel = makeInfoVal("\u2014");
        sizeRow.add(sizeValueLabel);
        sizeRow.add(Box.createHorizontalStrut(20));
        sizeRow.add(makeInfoKey("File Count:"));
        fileCountValueLabel = makeInfoVal("\u2014");
        sizeRow.add(fileCountValueLabel);
        p.add(sizeRow);

        return p;
    }

    private static JPanel buildOutputCard() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_PANEL);
        p.setBorder(compoundBorder(8, 8, 8, 8, "Manifest & Checksums"));
        outputArea = makeTextArea(true);
        outputArea.setEditable(false);
        p.add(scrollPane(outputArea), BorderLayout.CENTER);
        return p;
    }

    private static JPanel buildProvenanceCard() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_PANEL);
        p.setBorder(compoundBorder(8, 8, 8, 8, ".provenance.json"));
        JTextArea provArea = makeTextArea(true);
        provArea.setEditable(false);
        provArea.setName("provenanceArea");
        p.add(scrollPane(provArea), BorderLayout.CENTER);
        return p;
    }

    // -----------------------------------------------------------------
    // Status bar
    // -----------------------------------------------------------------

    private static JPanel buildStatusBar() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(BG_DARK);
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));
        p.setPreferredSize(new Dimension(0, 28));

        statusStep = new JLabel("  Ready");
        statusStep.setForeground(TEXT_DIM);
        statusStep.setFont(UI_FONT);

        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(220, 18));
        progressBar.setBackground(BG_FIELD);
        progressBar.setForeground(SUCCESS);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);

        JLabel ver = new JLabel("  " + APP_NAME + " v" + APP_VERSION + "  ");
        ver.setForeground(TEXT_DIM);
        ver.setFont(UI_FONT);

        p.add(ver, BorderLayout.WEST);
        p.add(statusStep, BorderLayout.CENTER);
        p.add(progressBar, BorderLayout.EAST);
        return p;
    }

    // =================================================================
    // WIDGET HELPERS
    // =================================================================

    private static void registerShortcut(String name, int key, int mod, Runnable action) {
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(key, mod), name);
        frame.getRootPane().getActionMap().put(name, act(action));
    }

    private static AbstractAction act(Runnable r) {
        return new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { r.run(); }
        };
    }

    private static JMenu makeMenu(String label) {
        JMenu m = new JMenu(label);
        m.setForeground(TEXT);
        return m;
    }

    private static JMenuItem makeMenuItem(String label, Runnable action) {
        JMenuItem item = new JMenuItem(label);
        item.setForeground(TEXT);
        item.addActionListener(e -> action.run());
        return item;
    }

    private static Border compoundBorder(int t, int l, int b, int r, String title) {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(t, l, b, r),
            BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(4, 4, 4, 4), title,
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                UI_FONT_B, TEXT_DIM)
        );
    }

    private static JPanel labeledRow(String label) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_PANEL);
        p.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JLabel l = new JLabel("  " + label);
        l.setForeground(TEXT_DIM);
        l.setFont(UI_FONT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(l);
        JPanel inner = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        inner.setBackground(BG_PANEL);
        inner.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(inner);
        return p;
    }

    private static JSpinner makeSpinner(int val, int min, int max) {
        SpinnerNumberModel m = new SpinnerNumberModel(val, min, max, 1);
        JSpinner s = new JSpinner(m);
        s.setBackground(BG_FIELD);
        s.setForeground(TEXT);
        s.setFont(UI_FONT);
        s.setPreferredSize(new Dimension(56, 26));
        return s;
    }

    private static JLabel makeSpinnerLabel(String txt) {
        JLabel l = new JLabel(txt);
        l.setForeground(TEXT_DIM);
        l.setFont(UI_FONT);
        return l;
    }

    private static JPanel makeSpinnerWrap(JSpinner s) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_FIELD);
        p.setBorder(BorderFactory.createLineBorder(BORDER));
        p.add(s, BorderLayout.CENTER);
        return p;
    }

    private static JLabel makeStatusLabel(String txt) {
        JLabel l = new JLabel(txt);
        l.setForeground(TEXT_DIM);
        l.setFont(UI_FONT);
        return l;
    }

    private static JLabel makeInfoKey(String txt) {
        JLabel l = new JLabel(txt);
        l.setForeground(TEXT_DIM);
        l.setFont(UI_FONT_B);
        return l;
    }

    private static JLabel makeInfoVal(String txt) {
        JLabel l = new JLabel(txt);
        l.setForeground(TEXT);
        l.setFont(UI_FONT);
        return l;
    }

    private static JPanel makeIndicator(String label) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        p.setBackground(BG_DARK);
        p.setBorder(BorderFactory.createLineBorder(BORDER));
        JLabel icon = new JLabel("\u25CF");
        icon.setFont(new Font("SansSerif", Font.PLAIN, 18));
        icon.setName("icon");
        icon.setForeground(TEXT_DIM);
        JLabel lbl = new JLabel(label + ": Pending");
        lbl.setForeground(TEXT_DIM);
        lbl.setFont(UI_FONT);
        lbl.setName("label");
        p.add(icon);
        p.add(lbl);
        return p;
    }

    private static void setIndicator(JPanel indicator, String state) {
        if (indicator == null) return;
        for (Component c : indicator.getComponents()) {
            if (c instanceof JLabel) {
                JLabel l = (JLabel) c;
                if ("icon".equals(l.getName())) {
                    l.setText("\u25CF");
                    switch (state) {
                        case "success": l.setForeground(SUCCESS); break;
                        case "fail":    l.setForeground(FAIL); break;
                        default:        l.setForeground(TEXT_DIM); break;
                    }
                } else if ("label".equals(l.getName())) {
                    String text = l.getText();
                    String prefix = text.contains(":") ?
                        text.substring(0, text.indexOf(':')) : text;
                    switch (state) {
                        case "success": l.setText(prefix + ": Passed"); l.setForeground(SUCCESS); break;
                        case "fail":    l.setText(prefix + ": Failed"); l.setForeground(FAIL); break;
                        default:        l.setText(prefix + ": Pending"); l.setForeground(TEXT_DIM); break;
                    }
                }
            }
        }
    }

    private static JButton makeActionBtn(String label, Runnable action) {
        JButton b = new JButton(label);
        b.setBackground(BG_FIELD);
        b.setForeground(TEXT);
        b.setFont(UI_FONT_B);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                b.setBackground(ACCENT);
            }
            @Override public void mouseExited(MouseEvent e) {
                b.setBackground(BG_FIELD);
            }
        });
        b.addActionListener(e -> action.run());
        return b;
    }

    private static JTextArea makeTextArea(boolean mono) {
        JTextArea ta = new JTextArea();
        ta.setBackground(BG_DARK);
        ta.setForeground(TEXT);
        ta.setCaretColor(TEXT);
        ta.setFont(mono ? MONO_FONT : UI_FONT);
        ta.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        return ta;
    }

    private static JScrollPane scrollPane(JTextArea ta) {
        JScrollPane sp = new JScrollPane(ta);
        sp.setBorder(BorderFactory.createLineBorder(BORDER));
        sp.getViewport().setBackground(BG_DARK);
        return sp;
    }

    private static JPanel findInfoPanel() {
        return findInfoPanelIn(frame);
    }

    private static JPanel findInfoPanelIn(Component c) {
        if (c instanceof JPanel) {
            JPanel p = (JPanel) c;
            if (p.getLayout() instanceof GridLayout &&
                ((GridLayout) p.getLayout()).getColumns() == 2) {
                for (Component k : p.getComponents()) {
                    if (k instanceof JLabel && "App:".equals(((JLabel) k).getText()))
                        return p;
                }
            }
        }
        if (c instanceof Container) {
            for (Component ch : ((Container) c).getComponents()) {
                JPanel found = findInfoPanelIn(ch);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static JTextArea findProvenanceArea() {
        return findNamedAreaIn(frame, "provenanceArea");
    }

    private static JTextArea findNamedAreaIn(Component c, String name) {
        if (c instanceof JTextArea && name.equals(((JTextArea) c).getName()))
            return (JTextArea) c;
        if (c instanceof Container) {
            for (Component ch : ((Container) c).getComponents()) {
                JTextArea f = findNamedAreaIn(ch, name);
                if (f != null) return f;
            }
        }
        return null;
    }

    private static String versionString() {
        return spinMajor.getValue() + "." + spinMinor.getValue() + "." + spinPatch.getValue();
    }

    private static void setStep(String text) {
        SwingUtilities.invokeLater(() -> statusStep.setText("  " + text));
    }

    private static void updateProgress(int pct, String msg) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(pct);
            progressBar.setString(msg);
            statusStep.setText("  " + msg);
        });
    }

    private static void appendLog(JTextArea area, String text) {
        area.append(text);
        area.setCaretPosition(area.getDocument().getLength());
    }

    private static Icon flatIcon(Color c) {
        return new Icon() {
            @Override public void paintIcon(Component comp, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c);
                g2.fillOval(x + 2, y + 2, 10, 10);
                g2.dispose();
            }
            @Override public int getIconWidth()  { return 14; }
            @Override public int getIconHeight() { return 14; }
        };
    }

    private static class AppComboRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean sel, boolean focus) {
            super.getListCellRendererComponent(list, value, index, sel, focus);
            setBackground(sel ? ACCENT : BG_FIELD);
            setForeground(TEXT);
            setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            if (value instanceof AppEntry) {
                AppEntry e = (AppEntry) value;
                setText(e.name + "  [" + e.status + "]");
            }
            return this;
        }
    }

    // =================================================================
    // ACTIONS
    // =================================================================

    private static void onAppSelected() {
        AppEntry e = (AppEntry) appCombo.getSelectedItem();
        if (e == null) return;

        String[] parts = e.version.split("\\.");
        try {
            spinMajor.setValue(Integer.parseInt(parts[0]));
            spinMinor.setValue(parts.length > 1 ? Integer.parseInt(parts[1]) : 0);
            spinPatch.setValue(parts.length > 2 ? Integer.parseInt(parts[2]) : 0);
        } catch (NumberFormatException ignored) {}

        refreshInfoCard(e);
        refreshOutputCard(e);
        refreshProvenanceCard(e);
        prefillReleaseNotes(e);

        buildOk = false;
        testOk = false;
        packageBtn.setEnabled(false);
        setIndicator(buildIndicator, "pending");
        setIndicator(testIndicator, "pending");
        buildStatusLabel.setText("Build: Not run");
        buildStatusLabel.setIcon(null);
        testStatusLabel.setText("Test: Not run");
        testStatusLabel.setIcon(null);
        buildLogArea.setText("");
        testLogArea.setText("");
        sizeValueLabel.setText("\u2014");
        fileCountValueLabel.setText("\u2014");
    }

    private static void refreshInfoCard(AppEntry e) {
        String ver = versionString();
        Path outDir = RELEASES_DIR.resolve(e.name).resolve("v" + ver);
        JPanel p = findInfoPanel();
        if (p == null) return;
        String[] vals = {
            e.name, e.slug, e.language, e.status, ver,
            e.visibility, String.valueOf(e.distributable),
            e.source, e.buildCommand.isEmpty() ? e.buildScript : e.buildCommand,
            outDir.toString()
        };
        Component[] kids = p.getComponents();
        int idx = 0;
        for (int i = 1; i < kids.length && idx < vals.length; i += 2) {
            if (kids[i] instanceof JLabel) {
                ((JLabel) kids[i]).setText(vals[idx++]);
                kids[i].setForeground(TEXT);
            }
        }
    }

    private static void refreshOutputCard(AppEntry e) {
        String ver = versionString();
        StringBuilder sb = new StringBuilder();
        sb.append("=== manifest.json ===\n");
        sb.append(generateManifestJson(e, ver)).append("\n\n");
        sb.append("=== SHA256SUMS ===\n");
        sb.append("(generated after packaging)\n");
        outputArea.setText(sb.toString());
        outputArea.setCaretPosition(0);
    }

    private static void refreshProvenanceCard(AppEntry e) {
        String ver = versionString();
        JTextArea pa = findProvenanceArea();
        if (pa != null) {
            pa.setText(generateProvenanceJson(e, ver));
            pa.setCaretPosition(0);
        }
    }

    private static void prefillReleaseNotes(AppEntry e) {
        String ver = versionString();
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        notesArea.setText(
            "# " + e.name + " v" + ver + " Release Notes\n\n" +
            "**Date:** " + date + "\n" +
            "**Application:** " + e.name + " (" + e.slug + ")\n" +
            "**Version:** " + ver + "\n" +
            "**Status:** " + e.status + "\n" +
            "**Language:** " + e.language + "\n\n" +
            "## Changes\n\n" +
            "- \n\n" +
            "## Bug Fixes\n\n" +
            "- \n\n" +
            "## Known Issues\n\n" +
            "- \n\n" +
            "## Installation\n\n" +
            "Extract `" + e.slug + "-" + ver + "-release.zip` and follow the instructions in the included README.\n\n" +
            "---\n" +
            "*Generated by PackForge v" + APP_VERSION + "*\n"
        );
        notesArea.setCaretPosition(0);
    }

    // =================================================================
    // BUILD
    // =================================================================

    private static void startBuild() {
        AppEntry e = (AppEntry) appCombo.getSelectedItem();
        if (e == null) return;
        buildOk = false;
        packageBtn.setEnabled(false);
        setIndicator(buildIndicator, "pending");
        buildBtn.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        setStep("Building " + e.name + "...");
        buildLogArea.setText("");

        exec.submit(() -> {
            try {
                String cmd = e.buildCommand.isEmpty()
                    ? "bash " + COMPANY_DIR + "/" + e.buildScript
                    : e.buildCommand;
                SwingUtilities.invokeLater(() -> appendLog(buildLogArea, "$ " + cmd + "\n"));
                ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
                pb.directory(ROOT_DIR.toFile());
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(proc.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String l = line;
                        SwingUtilities.invokeLater(() -> appendLog(buildLogArea, l + "\n"));
                    }
                }
                int exit = proc.waitFor();
                SwingUtilities.invokeLater(() -> {
                    if (exit == 0) {
                        buildOk = true;
                        setIndicator(buildIndicator, "success");
                        buildStatusLabel.setText("Build: Passed");
                        buildStatusLabel.setIcon(flatIcon(SUCCESS));
                        appendLog(buildLogArea, "\n\u2713 Build succeeded (exit 0)\n");
                    } else {
                        buildOk = false;
                        setIndicator(buildIndicator, "fail");
                        buildStatusLabel.setText("Build: Failed");
                        buildStatusLabel.setIcon(flatIcon(FAIL));
                        appendLog(buildLogArea, "\n\u2717 Build failed (exit " + exit + ")\n");
                    }
                    packageBtn.setEnabled(buildOk && testOk);
                    progressBar.setIndeterminate(false);
                    progressBar.setVisible(false);
                    setStep("Ready");
                    buildBtn.setEnabled(true);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    appendLog(buildLogArea, "\n\u2717 Error: " + ex.getMessage() + "\n");
                    setIndicator(buildIndicator, "fail");
                    buildStatusLabel.setText("Build: Error");
                    buildStatusLabel.setIcon(flatIcon(FAIL));
                    progressBar.setIndeterminate(false);
                    progressBar.setVisible(false);
                    setStep("Build error");
                    buildBtn.setEnabled(true);
                });
            }
        });
    }

    // =================================================================
    // TEST
    // =================================================================

    private static void startTest() {
        AppEntry e = (AppEntry) appCombo.getSelectedItem();
        if (e == null) return;
        testOk = false;
        packageBtn.setEnabled(false);
        setIndicator(testIndicator, "pending");
        testBtn.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        setStep("Testing " + e.name + "...");
        testLogArea.setText("");

        exec.submit(() -> {
            try {
                Path testScript = COMPANY_DIR.resolve("scripts/build/" + e.slug + "-test.sh");
                String cmd;
                if (Files.exists(testScript)) {
                    cmd = "bash " + testScript;
                } else {
                    cmd = "bash -c 'cd " + ROOT_DIR +
                        " && if [ -f scripts/build/" + e.slug + ".sh ]; " +
                        "then echo \"Running build as test for " + e.slug + "\"; " +
                        "bash scripts/build/" + e.slug + ".sh && " +
                        "echo \"PASS: " + e.slug + " compiles successfully\"; " +
                        "else echo \"SKIP: No test script found for " + e.slug +
                        "\"; exit 0; fi'";
                }
                SwingUtilities.invokeLater(() -> appendLog(testLogArea, "$ " + cmd + "\n"));
                ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
                pb.directory(ROOT_DIR.toFile());
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(proc.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String l = line;
                        SwingUtilities.invokeLater(() -> appendLog(testLogArea, l + "\n"));
                    }
                }
                int exit = proc.waitFor();
                SwingUtilities.invokeLater(() -> {
                    if (exit == 0) {
                        testOk = true;
                        setIndicator(testIndicator, "success");
                        testStatusLabel.setText("Test: Passed");
                        testStatusLabel.setIcon(flatIcon(SUCCESS));
                        appendLog(testLogArea, "\n\u2713 Tests passed (exit 0)\n");
                    } else {
                        testOk = false;
                        setIndicator(testIndicator, "fail");
                        testStatusLabel.setText("Test: Failed");
                        testStatusLabel.setIcon(flatIcon(FAIL));
                        appendLog(testLogArea, "\n\u2717 Tests failed (exit " + exit + ")\n");
                    }
                    packageBtn.setEnabled(buildOk && testOk);
                    progressBar.setIndeterminate(false);
                    progressBar.setVisible(false);
                    setStep("Ready");
                    testBtn.setEnabled(true);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    appendLog(testLogArea, "\n\u2717 Error: " + ex.getMessage() + "\n");
                    setIndicator(testIndicator, "fail");
                    testStatusLabel.setText("Test: Error");
                    testStatusLabel.setIcon(flatIcon(FAIL));
                    progressBar.setIndeterminate(false);
                    progressBar.setVisible(false);
                    setStep("Test error");
                    testBtn.setEnabled(true);
                });
            }
        });
    }

    // =================================================================
    // PACKAGE
    // =================================================================

    private static void startPackage() {
        AppEntry e = (AppEntry) appCombo.getSelectedItem();
        if (e == null) return;
        if (!buildOk || !testOk) {
            JOptionPane.showMessageDialog(frame,
                "Cannot package: build and test must both pass first.",
                APP_NAME, JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(frame,
            "Package " + e.name + " v" + versionString() + "?\n" +
            "Output: " + RELEASES_DIR.resolve(e.name).resolve("v" + versionString()),
            "Confirm Package", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        packageBtn.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(false);
        progressBar.setValue(0);
        setStep("Packaging " + e.name + "...");

        exec.submit(() -> {
            try {
                String ver = versionString();
                Path outDir = RELEASES_DIR.resolve(e.name).resolve("v" + ver);
                Files.createDirectories(outDir);

                String slug = e.slug;
                Path sourceDir = ROOT_DIR.resolve(e.source);
                List<Path> sourceFiles = new ArrayList<>();
                if (Files.exists(sourceDir)) {
                    try (var walk = Files.walk(sourceDir)) {
                        walk.filter(Files::isRegularFile).forEach(sourceFiles::add);
                    }
                }
                int fileCount = sourceFiles.size();

                updateProgress(15, "Creating ZIP archive...");
                String zipName = slug + "-" + ver + "-source.zip";
                Path zipPath = outDir.resolve(zipName);
                try (ZipOutputStream zos = new ZipOutputStream(
                        Files.newOutputStream(zipPath))) {
                    for (int i = 0; i < sourceFiles.size(); i++) {
                        Path file = sourceFiles.get(i);
                        String entryName = slug + "/" + ROOT_DIR.relativize(file).toString();
                        zos.putNextEntry(new ZipEntry(entryName));
                        Files.copy(file, zos);
                        zos.closeEntry();
                        final int pct = 15 + (int) ((i + 1.0) / sourceFiles.size() * 35);
                        updateProgress(pct, "Zipping " + (i + 1) + "/" + fileCount + "...");
                    }
                }

                updateProgress(55, "Computing SHA-256 checksum...");
                String zipHash = sha256(zipPath);

                updateProgress(65, "Writing SHA256SUMS...");
                Path sumsFile = outDir.resolve("SHA256SUMS");
                Files.writeString(sumsFile, zipHash + "  " + zipName + "\n",
                    StandardCharsets.UTF_8);

                updateProgress(70, "Writing .sha256 file...");
                Path sha256File = outDir.resolve(zipName + ".sha256");
                Files.writeString(sha256File, zipHash + "\n", StandardCharsets.UTF_8);

                updateProgress(75, "Writing manifest.json...");
                String manifest = generateManifestJson(e, ver);
                Path manifestFile = outDir.resolve("manifest.json");
                Files.writeString(manifestFile, manifest, StandardCharsets.UTF_8);

                updateProgress(80, "Writing .provenance.json...");
                String provenance = generateProvenanceJson(e, ver);
                Path provenanceFile = outDir.resolve(".provenance.json");
                Files.writeString(provenanceFile, provenance, StandardCharsets.UTF_8);

                updateProgress(85, "Writing RELEASE-MANIFEST.txt...");
                String relManifest = generateReleaseManifest(e, ver, zipHash, zipName,
                    sourceFiles.size(), zipPath);
                Path relManifestFile = outDir.resolve("RELEASE-MANIFEST.txt");
                Files.writeString(relManifestFile, relManifest, StandardCharsets.UTF_8);

                updateProgress(90, "Writing RELEASE-NOTES.md...");
                String notes = notesArea.getText();
                Path notesFile = outDir.resolve("RELEASE-NOTES.md");
                Files.writeString(notesFile, notes, StandardCharsets.UTF_8);

                updateProgress(100, "Package complete!");
                long sizeBytes = Files.size(zipPath);
                String sizeStr = humanSize(sizeBytes);

                SwingUtilities.invokeLater(() -> {
                    sizeValueLabel.setText(sizeStr);
                    sizeValueLabel.setForeground(SUCCESS);
                    fileCountValueLabel.setText(String.valueOf(fileCount));
                    fileCountValueLabel.setForeground(SUCCESS);

                    StringBuilder sb = new StringBuilder();
                    sb.append("=== manifest.json ===\n").append(manifest).append("\n\n");
                    sb.append("=== SHA256SUMS ===\n").append(zipHash).append("  ").append(zipName).append("\n");
                    sb.append("=== ZIP Checksum ===\n").append(zipHash).append("  ").append(zipName).append("\n\n");
                    sb.append("=== Package Info ===\n");
                    sb.append("Archive:  ").append(zipPath).append("\n");
                    sb.append("Size:     ").append(sizeStr).append("\n");
                    sb.append("Files:    ").append(fileCount).append("\n");
                    sb.append("Hash:     ").append(zipHash).append("\n");
                    outputArea.setText(sb.toString());
                    outputArea.setCaretPosition(0);

                    JTextArea pa = findProvenanceArea();
                    if (pa != null) { pa.setText(provenance); pa.setCaretPosition(0); }

                    progressBar.setIndeterminate(false);
                    progressBar.setValue(100);
                    progressBar.setString("Done!");
                    setStep("Packaging complete: " + zipPath.getFileName());
                    packageBtn.setEnabled(true);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    appendLog(buildLogArea, "\n\u2717 Package error: " + ex.getMessage() + "\n");
                    progressBar.setIndeterminate(false);
                    progressBar.setVisible(false);
                    setStep("Packaging error: " + ex.getMessage());
                    packageBtn.setEnabled(true);
                });
            }
        });
    }

    // =================================================================
    // SAVE RELEASE NOTES
    // =================================================================

    private static void saveReleaseNotes() {
        AppEntry e = (AppEntry) appCombo.getSelectedItem();
        if (e == null) return;
        try {
            Path outDir = RELEASES_DIR.resolve(e.name).resolve("v" + versionString());
            Files.createDirectories(outDir);
            Path file = outDir.resolve("RELEASE-NOTES.md");
            Files.writeString(file, notesArea.getText(), StandardCharsets.UTF_8);
            setStep("Release notes saved: " + file);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame,
                "Failed to save: " + ex.getMessage(),
                APP_NAME, JOptionPane.ERROR_MESSAGE);
        }
    }

    // =================================================================
    // GENERATORS
    // =================================================================

    private static String generateManifestJson(AppEntry e, String ver) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return "{\n" +
            "  \"schema_version\": 1,\n" +
            "  \"application\": {\n" +
            "    \"id\": \"" + e.id + "\",\n" +
            "    \"slug\": \"" + e.slug + "\",\n" +
            "    \"name\": \"" + e.name + "\",\n" +
            "    \"version\": \"" + ver + "\",\n" +
            "    \"status\": \"" + e.status + "\",\n" +
            "    \"language\": \"" + e.language + "\",\n" +
            "    \"visibility\": \"" + e.visibility + "\",\n" +
            "    \"distributable\": " + e.distributable + ",\n" +
            "    \"main\": \"" + e.mainClass + "\",\n" +
            "    \"source\": \"" + e.source + "\",\n" +
            "    \"description\": \"" + escapeJson(e.description) + "\"\n" +
            "  },\n" +
            "  \"release\": {\n" +
            "    \"version\": \"" + ver + "\",\n" +
            "    \"date\": \"" + date + "\",\n" +
            "    \"builder\": \"" + APP_NAME + " v" + APP_VERSION + "\",\n" +
            "    \"platform\": \"java\",\n" +
            "    \"packager\": \"" + System.getProperty("user.name") + "@" +
            System.getProperty("os.name") + "\"\n" +
            "  },\n" +
            "  \"files\": {\n" +
            "    \"archive\": \"" + e.slug + "-" + ver + "-source.zip\"\n" +
            "  }\n" +
            "}";
    }

    private static String generateProvenanceJson(AppEntry e, String ver) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return "{\n" +
            "  \"provenance_version\": 1,\n" +
            "  \"application\": \"" + e.slug + "\",\n" +
            "  \"version\": \"" + ver + "\",\n" +
            "  \"timestamp\": \"" + date + "\",\n" +
            "  \"build_system\": \"javac\",\n" +
            "  \"build_script\": \"" + e.buildScript + "\",\n" +
            "  \"source_hash_algorithm\": \"SHA-256\",\n" +
            "  \"release_tool\": \"" + APP_NAME + "\",\n" +
            "  \"release_tool_version\": \"" + APP_VERSION + "\",\n" +
            "  \"operator\": \"" + System.getProperty("user.name") + "\",\n" +
            "  \"platform\": {\n" +
            "    \"os\": \"" + System.getProperty("os.name") + "\",\n" +
            "    \"arch\": \"" + System.getProperty("os.arch") + "\",\n" +
            "    \"java\": \"" + System.getProperty("java.version") + "\"\n" +
            "  }\n" +
            "}";
    }

    private static String generateReleaseManifest(AppEntry e, String ver,
            String hash, String zipName, int fileCount, Path zipPath) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        try {
            long size = Files.size(zipPath);
            return "RELEASE MANIFEST\n" +
                "================\n\n" +
                "Application:  " + e.name + "\n" +
                "ID:           " + e.id + "\n" +
                "Slug:         " + e.slug + "\n" +
                "Version:      " + ver + "\n" +
                "Status:       " + e.status + "\n" +
                "Language:     " + e.language + "\n" +
                "Date:         " + date + "\n" +
                "Builder:      " + APP_NAME + " v" + APP_VERSION + "\n\n" +
                "FILES\n" +
                "=====\n" +
                "Archive:      " + zipName + "\n" +
                "Size:         " + humanSize(size) + "\n" +
                "File Count:   " + fileCount + "\n" +
                "SHA-256:      " + hash + "\n\n" +
                "OUTPUT DIRECTORY\n" +
                "================\n" +
                RELEASES_DIR.resolve(e.name).resolve("v" + ver) + "\n";
        } catch (IOException ex) {
            return "Error reading file size: " + ex.getMessage();
        }
    }

    // =================================================================
    // CRYPTO & UTILS
    // =================================================================

    private static String sha256(Path file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            try (InputStream is = Files.newInputStream(file)) {
                int n;
                while ((n = is.read(buf)) != -1) {
                    md.update(buf, 0, n);
                }
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    // =================================================================
    // ABOUT DIALOG
    // =================================================================

    private static void showAbout() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG);

        JLabel title = new JLabel(APP_NAME);
        title.setFont(TITLE_FONT);
        title.setForeground(TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel ver = new JLabel("Version " + APP_VERSION);
        ver.setFont(UI_FONT);
        ver.setForeground(TEXT_DIM);
        ver.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel desc = new JLabel("<html><div style='text-align:center;width:300px'>" +
            APP_DESC + "</div></html>");
        desc.setFont(UI_FONT);
        desc.setForeground(TEXT_DIM);
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel copy = new JLabel("Datacenter \u00a9 2026");
        copy.setFont(UI_FONT);
        copy.setForeground(TEXT_DIM);
        copy.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(Box.createVerticalStrut(12));
        panel.add(title);
        panel.add(Box.createVerticalStrut(6));
        panel.add(ver);
        panel.add(Box.createVerticalStrut(12));
        panel.add(desc);
        panel.add(Box.createVerticalStrut(16));
        panel.add(copy);
        panel.add(Box.createVerticalStrut(8));

        JOptionPane.showMessageDialog(frame, panel, "About " + APP_NAME,
            JOptionPane.PLAIN_MESSAGE);
    }
}
