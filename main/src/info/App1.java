import com.sun.management.OperatingSystemMXBean;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Datapro — live analytics for the local Datacenter workspace. */
public final class App1 {
    private static final Path DATA_ROOT = Path.of(System.getProperty("user.home"), "Data");
    private static final Color BG = new Color(10, 14, 24);
    private static final Color PANEL = new Color(19, 25, 39);
    private static final Color PANEL_2 = new Color(25, 33, 49);
    private static final Color TEXT = new Color(231, 236, 243);
    private static final Color MUTED = new Color(141, 154, 176);
    private static final Color CYAN = new Color(75, 201, 221);
    private static final Color GREEN = new Color(91, 211, 145);
    private static final Color AMBER = new Color(242, 184, 78);
    private static final Color RED = new Color(238, 101, 110);
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("MMM d, HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final JFrame frame = new JFrame("Datapro — Datacenter Analytics");
    private final JLabel status = new JLabel("Starting scanner…");
    private final JLabel lastUpdated = new JLabel("—");
    private final Map<String, MetricCard> cards = new LinkedHashMap<>();
    private final TrackerTableModel trackerModel = new TrackerTableModel();
    private final JTable trackerTable = new JTable(trackerModel);
    private final JTextField search = new JTextField();
    private final UsagePanel usagePanel = new UsagePanel();
    private final PiePanel fileTypesPie = new PiePanel("Files by category");
    private final PiePanel storagePie = new PiePanel("Storage by category");
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "datapro-scanner");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean scanning = new AtomicBoolean();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new App1().start());
    }

    private App1() {
        installLookAndFeel();
        buildWindow();
    }

    private void start() {
        frame.setVisible(true);
        scheduler.scheduleWithFixedDelay(this::scanSafely, 0, 2, TimeUnit.SECONDS);
    }

    private void installLookAndFeel() {
        UIManager.put("Panel.background", BG);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Table.background", PANEL);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.gridColor", new Color(42, 53, 72));
        UIManager.put("TableHeader.background", PANEL_2);
        UIManager.put("TableHeader.foreground", TEXT);
        UIManager.put("TextField.background", PANEL_2);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", CYAN);
        UIManager.put("ScrollPane.background", BG);
        UIManager.put("ProgressBar.background", new Color(38, 47, 64));
        UIManager.put("ProgressBar.foreground", CYAN);
    }

    private void buildWindow() {
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1050, 700));
        frame.setSize(1440, 900);
        frame.setLocationRelativeTo(null);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) { scheduler.shutdownNow(); }
        });

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);
        root.add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(BG);
        tabs.setForeground(TEXT);
        tabs.addTab("Overview", buildOverview());
        tabs.addTab("100+ Trackers", buildTrackers());
        tabs.addTab("Storage", buildStorage());
        tabs.addTab("About", buildAbout());
        root.add(tabs, BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        frame.setContentPane(root);
    }

    private JComponent buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(13, 18, 30));
        panel.setBorder(new EmptyBorder(18, 24, 16, 24));
        JLabel title = new JLabel("DATAPRO");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        title.setForeground(CYAN);
        JLabel subtitle = new JLabel("Datacenter telemetry  •  " + DATA_ROOT);
        subtitle.setForeground(MUTED);
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        JPanel names = new JPanel();
        names.setOpaque(false);
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.add(title);
        names.add(subtitle);
        panel.add(names, BorderLayout.WEST);
        lastUpdated.setForeground(MUTED);
        lastUpdated.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(lastUpdated, BorderLayout.EAST);
        return panel;
    }

    private JComponent buildOverview() {
        JPanel content = new JPanel();
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(18, 20, 20, 20));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JPanel cardGrid = new JPanel(new GridLayout(2, 5, 12, 12));
        cardGrid.setBackground(BG);
        addCard(cardGrid, "Files", "files");
        addCard(cardGrid, "Applications", "apps");
        addCard(cardGrid, "Lines of code", "loc");
        addCard(cardGrid, "Data size", "size");
        addCard(cardGrid, "Icons & images", "icons");
        addCard(cardGrid, "Directories", "directories");
        addCard(cardGrid, "Source files", "source");
        addCard(cardGrid, "Datapro RAM", "processRam");
        addCard(cardGrid, "Datapro CPU", "processCpu");
        addCard(cardGrid, "Scan time", "scanTime");
        cardGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        content.add(cardGrid);
        content.add(Box.createVerticalStrut(14));

        JPanel charts = new JPanel(new GridLayout(1, 3, 12, 12));
        charts.setBackground(BG);
        charts.add(usagePanel);
        charts.add(fileTypesPie);
        charts.add(storagePie);
        charts.setPreferredSize(new Dimension(1200, 440));
        content.add(charts);
        return new JScrollPane(content);
    }

    private void addCard(JPanel parent, String title, String key) {
        MetricCard card = new MetricCard(title);
        cards.put(key, card);
        parent.add(card);
    }

    private JComponent buildTrackers() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(16, 18, 18, 18));
        search.putClientProperty("JTextField.placeholderText", "Filter trackers…");
        search.setToolTipText("Search metric, category, value, or detail");
        search.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(50, 64, 83)), new EmptyBorder(9, 12, 9, 12)));
        panel.add(search, BorderLayout.NORTH);

        trackerTable.setRowHeight(27);
        trackerTable.setFillsViewportHeight(true);
        trackerTable.setAutoCreateRowSorter(true);
        trackerTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        trackerTable.getColumnModel().getColumn(1).setPreferredWidth(230);
        trackerTable.getColumnModel().getColumn(2).setPreferredWidth(140);
        trackerTable.getColumnModel().getColumn(3).setPreferredWidth(520);
        TableRowSorter<TrackerTableModel> sorter = new TableRowSorter<>(trackerModel);
        trackerTable.setRowSorter(sorter);
        SimpleDocumentListener filterListener = () -> {
            String query = search.getText().trim();
            sorter.setRowFilter(query.isEmpty() ? null : RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query)));
        };
        search.getDocument().addDocumentListener(filterListener);
        panel.add(new JScrollPane(trackerTable), BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildStorage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        JTextArea guide = new JTextArea();
        guide.setEditable(false);
        guide.setBackground(PANEL);
        guide.setForeground(TEXT);
        guide.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        guide.setBorder(new EmptyBorder(22, 25, 22, 25));
        guide.setText("STORAGE SCALE\n\n" + storageScale());
        panel.add(new JScrollPane(guide), BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildAbout() {
        JTextArea about = new JTextArea("""
                DATAPRO — DATACENTER ANALYTICS

                A dependency-free Java/Swing dashboard that measures the local Datacenter workspace.

                Live scans run on a background thread every two seconds. The UI remains responsive,
                symbolic links are not followed, unreadable files are reported instead of crashing,
                and large/binary files are not opened for line counting.

                Tracked areas include source code, applications, file types, icons, storage, disk,
                system CPU, process CPU, system RAM, JVM RAM, permissions, file ages, sizes, directory
                depth, naming, build artifacts, archives, media, documentation, and data quality.
                """);
        about.setEditable(false);
        about.setLineWrap(true);
        about.setWrapStyleWord(true);
        about.setBackground(BG);
        about.setForeground(TEXT);
        about.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        about.setBorder(new EmptyBorder(35, 45, 35, 45));
        return about;
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(13, 18, 30));
        footer.setBorder(new EmptyBorder(8, 18, 8, 18));
        status.setForeground(MUTED);
        footer.add(status, BorderLayout.WEST);
        JLabel live = new JLabel("● LIVE  •  refresh 2s");
        live.setForeground(GREEN);
        footer.add(live, BorderLayout.EAST);
        return footer;
    }

    private void scanSafely() {
        if (!scanning.compareAndSet(false, true)) return;
        long started = System.nanoTime();
        try {
            Snapshot snapshot = Scanner.scan(DATA_ROOT);
            snapshot.scanMillis = (System.nanoTime() - started) / 1_000_000.0;
            SwingUtilities.invokeLater(() -> apply(snapshot));
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> status.setText("Scan error: " + ex.getMessage()));
        } finally {
            scanning.set(false);
        }
    }

    private void apply(Snapshot s) {
        cards.get("files").setValue(formatCount(s.files));
        cards.get("apps").setValue(formatCount(s.appFiles));
        cards.get("loc").setValue(formatCount(s.lines));
        cards.get("size").setValue(humanBytes(s.totalBytes));
        cards.get("icons").setValue(formatCount(s.icons));
        cards.get("directories").setValue(formatCount(s.directories));
        cards.get("source").setValue(formatCount(s.sourceFiles));
        cards.get("processRam").setValue(humanBytes(s.jvmUsed));
        cards.get("processCpu").setValue(percent(s.processCpu));
        cards.get("scanTime").setValue(String.format(Locale.US, "%.1f ms", s.scanMillis));
        usagePanel.setSnapshot(s);
        fileTypesPie.setData(s.categoryCounts);
        storagePie.setData(s.categoryBytes);
        trackerModel.setRows(s.trackers);
        status.setText(String.format("Healthy • %,d trackers • %,d unreadable • %,d symlinks skipped", s.trackers.size(), s.unreadable, s.symlinks));
        lastUpdated.setText("Updated " + CLOCK.format(Instant.now()));
    }

    private static String formatCount(long value) { return String.format(Locale.US, "%,d", value); }
    private static String percent(double value) { return value < 0 ? "N/A" : String.format(Locale.US, "%.1f%%", value * 100); }

    private static String humanBytes(long bytes) {
        String[] units = {"B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
        double value = Math.max(0, bytes);
        int unit = 0;
        while (value >= 1024 && unit < units.length - 1) { value /= 1024; unit++; }
        return unit == 0 ? bytes + " B" : String.format(Locale.US, "%.2f %s", value, units[unit]);
    }

    private static String storageScale() {
        String[] units = {"Byte", "Kilobyte (KB)", "Megabyte (MB)", "Gigabyte (GB)", "Terabyte (TB)",
                "Petabyte (PB)", "Exabyte (EB / XB)", "Zettabyte (ZB)", "Yottabyte (YB)"};
        StringBuilder text = new StringBuilder("1 Byte = 8 bits\n");
        for (int i = 1; i < units.length; i++) text.append(String.format("1 %-20s = 1,024 %s%n", units[i], units[i - 1]));
        text.append("\nDatapro uses binary multiples (1 KB = 1,024 bytes).\n");
        text.append("An exabyte—sometimes written XB informally—is 1,152,921,504,606,846,976 bytes.\n");
        return text.toString();
    }

    @SuppressWarnings("serial")
    private static final class MetricCard extends JPanel {
        private final JLabel value = new JLabel("—");
        MetricCard(String title) {
            setLayout(new BorderLayout());
            setBackground(PANEL);
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(42, 53, 72)), new EmptyBorder(12, 14, 12, 14)));
            JLabel label = new JLabel(title.toUpperCase(Locale.ROOT));
            label.setForeground(MUTED);
            label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            value.setForeground(TEXT);
            value.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
            add(label, BorderLayout.NORTH);
            add(value, BorderLayout.CENTER);
        }
        void setValue(String text) { value.setText(text); }
    }

    @SuppressWarnings("serial")
    private static final class UsagePanel extends JPanel {
        private final Meter cpu = new Meter("SYSTEM CPU", CYAN);
        private final Meter ram = new Meter("SYSTEM RAM", GREEN);
        private final Meter disk = new Meter("DISK USED", AMBER);
        private final Meter jvm = new Meter("DATAPRO HEAP", RED);
        UsagePanel() {
            setLayout(new GridLayout(4, 1, 5, 10));
            setBackground(PANEL);
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(42, 53, 72)), new EmptyBorder(18, 18, 18, 18)));
            add(cpu); add(ram); add(disk); add(jvm);
        }
        void setSnapshot(Snapshot s) {
            cpu.set(s.systemCpu, percent(s.systemCpu));
            ram.set(s.totalRam == 0 ? 0 : (double) s.usedRam / s.totalRam, humanBytes(s.usedRam) + " / " + humanBytes(s.totalRam));
            disk.set(s.diskTotal == 0 ? 0 : (double) (s.diskTotal - s.diskUsable) / s.diskTotal, humanBytes(s.diskUsable) + " free");
            jvm.set(s.jvmMax == 0 ? 0 : (double) s.jvmUsed / s.jvmMax, humanBytes(s.jvmUsed) + " / " + humanBytes(s.jvmMax));
        }
    }

    @SuppressWarnings("serial")
    private static final class Meter extends JPanel {
        private final JLabel name = new JLabel();
        private final JLabel value = new JLabel();
        private final JProgressBar bar = new JProgressBar(0, 1000);
        Meter(String title, Color color) {
            setOpaque(false);
            setLayout(new BorderLayout(5, 4));
            name.setText(title); name.setForeground(MUTED); name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            value.setForeground(TEXT); value.setHorizontalAlignment(SwingConstants.RIGHT); value.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            bar.setForeground(color); bar.setBorderPainted(false); bar.setStringPainted(false);
            add(name, BorderLayout.WEST); add(value, BorderLayout.EAST); add(bar, BorderLayout.SOUTH);
        }
        void set(double ratio, String text) { bar.setValue((int) (Math.max(0, Math.min(1, ratio)) * 1000)); value.setText(text); }
    }

    @SuppressWarnings("serial")
    private static final class PiePanel extends JPanel {
        private static final Color[] COLORS = {CYAN, GREEN, AMBER, RED, new Color(148, 116, 225), new Color(80, 132, 220), new Color(210, 107, 175), MUTED};
        private final String title;
        private Map<String, Long> data = Map.of();
        PiePanel(String title) { this.title = title; setBackground(PANEL); setBorder(BorderFactory.createLineBorder(new Color(42, 53, 72))); }
        void setData(Map<String, Long> values) { data = new LinkedHashMap<>(values); repaint(); }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(TEXT); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14)); g.drawString(title, 18, 28);
            long total = data.values().stream().mapToLong(Long::longValue).sum();
            int size = Math.min(getWidth() - 40, getHeight() - 150); size = Math.max(80, size);
            int x = (getWidth() - size) / 2, y = 45, start = 90, index = 0;
            if (total == 0) { g.setColor(new Color(49, 58, 73)); g.fillOval(x, y, size, size); }
            else for (Map.Entry<String, Long> entry : data.entrySet()) {
                int arc = index == data.size() - 1 ? 90 + 360 - start : (int) Math.round(360.0 * entry.getValue() / total);
                g.setColor(COLORS[index % COLORS.length]); g.fillArc(x, y, size, size, start, -arc); start -= arc; index++;
            }
            int ly = y + size + 23; index = 0; g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            for (Map.Entry<String, Long> entry : data.entrySet()) {
                if (index >= 8) break;
                int col = index % 2, row = index / 2, lx = 18 + col * (getWidth() / 2), yy = ly + row * 19;
                g.setColor(COLORS[index % COLORS.length]); g.fillRect(lx, yy - 9, 9, 9);
                g.setColor(MUTED); g.drawString(entry.getKey() + "  " + formatCount(entry.getValue()), lx + 14, yy); index++;
            }
            g.dispose();
        }
    }

    private record Tracker(String category, String metric, String value, String detail) {}

    @SuppressWarnings("serial")
    private static final class TrackerTableModel extends AbstractTableModel {
        private final String[] columns = {"Category", "Metric", "Value", "Detail"};
        private List<Tracker> rows = List.of();
        void setRows(List<Tracker> value) { rows = List.copyOf(value); fireTableDataChanged(); }
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }
        @Override public Object getValueAt(int row, int column) {
            Tracker t = rows.get(row);
            return switch (column) { case 0 -> t.category; case 1 -> t.metric; case 2 -> t.value; default -> t.detail; };
        }
    }

    @FunctionalInterface private interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
        void update();
        @Override default void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
        @Override default void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
        @Override default void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
    }

    private static final class Snapshot {
        long files, directories, totalBytes, lines, codeBytes, sourceFiles, appFiles, icons, unreadable, symlinks;
        long diskTotal, diskUsable, totalRam, usedRam, jvmUsed, jvmMax;
        double systemCpu, processCpu, scanMillis;
        int maxDepth;
        Instant oldest, newest;
        final Map<String, Long> extensions = new TreeMap<>();
        final Map<String, Long> extensionBytes = new TreeMap<>();
        final Map<String, Long> categoryCounts = new LinkedHashMap<>();
        final Map<String, Long> categoryBytes = new LinkedHashMap<>();
        final Map<Integer, Long> depthCounts = new TreeMap<>();
        final long[] sizeBuckets = new long[8];
        final long[] ageBuckets = new long[7];
        final long[] lineBuckets = new long[7];
        long hidden, empty, executable, readable, writable, namesWithSpaces, duplicateNames, longNames;
        final Map<String, Integer> names = new HashMap<>();
        List<Tracker> trackers = List.of();
    }

    private static final class Scanner {
        private static final Set<String> SOURCE = Set.of("java", "cs", "js", "ts", "tsx", "jsx", "py", "c", "cc", "cpp", "h", "hpp", "rs", "go", "rb", "php", "swift", "kt", "kts", "scala", "sh", "ps1", "sql", "html", "css", "scss", "xml", "json", "yaml", "yml", "toml", "md");
        private static final Set<String> ICONS = Set.of("ico", "icns", "png", "svg", "webp", "jpg", "jpeg", "gif", "bmp");
        private static final Set<String> ARCHIVES = Set.of("zip", "tar", "gz", "bz2", "xz", "7z", "rar", "jar", "war");
        private static final Set<String> AUDIO = Set.of("wav", "mp3", "ogg", "flac", "aac", "m4a");
        private static final Set<String> VIDEO = Set.of("mp4", "mkv", "mov", "webm", "avi");

        static Snapshot scan(Path root) throws IOException {
            Snapshot s = new Snapshot();
            Instant now = Instant.now();
            Files.createDirectories(root);
            Files.walkFileTree(root, EnumSet.noneOf(FileVisitOption.class), 64, new SimpleFileVisitor<>() {
                @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (!dir.equals(root)) s.directories++;
                    int depth = root.relativize(dir).getNameCount();
                    s.maxDepth = Math.max(s.maxDepth, depth);
                    s.depthCounts.merge(depth, 1L, Long::sum);
                    return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isSymbolicLink()) { s.symlinks++; return FileVisitResult.CONTINUE; }
                    if (!attrs.isRegularFile()) return FileVisitResult.CONTINUE;
                    s.files++;
                    long size = attrs.size(); s.totalBytes += size;
                    String name = file.getFileName().toString();
                    String lower = name.toLowerCase(Locale.ROOT);
                    String ext = extension(lower);
                    s.extensions.merge(ext, 1L, Long::sum);
                    s.extensionBytes.merge(ext, size, Long::sum);
                    s.names.merge(lower, 1, Integer::sum);
                    if (name.startsWith(".")) s.hidden++;
                    if (size == 0) s.empty++;
                    if (Files.isExecutable(file)) s.executable++;
                    if (Files.isReadable(file)) s.readable++;
                    if (Files.isWritable(file)) s.writable++;
                    if (name.contains(" ")) s.namesWithSpaces++;
                    if (name.length() > 80) s.longNames++;
                    if (ICONS.contains(ext)) s.icons++;
                    if (root.relativize(file).getNameCount() > 1 && root.relativize(file).getName(0).toString().equals("apps")) s.appFiles++;
                    int depth = root.relativize(file).getNameCount(); s.maxDepth = Math.max(s.maxDepth, depth); s.depthCounts.merge(depth, 1L, Long::sum);
                    s.sizeBuckets[sizeBucket(size)]++;
                    Instant modified = attrs.lastModifiedTime().toInstant();
                    s.oldest = s.oldest == null || modified.isBefore(s.oldest) ? modified : s.oldest;
                    s.newest = s.newest == null || modified.isAfter(s.newest) ? modified : s.newest;
                    s.ageBuckets[ageBucket(Duration.between(modified, now))]++;
                    String category = category(ext);
                    s.categoryCounts.merge(category, 1L, Long::sum);
                    s.categoryBytes.merge(category, size, Long::sum);
                    if (SOURCE.contains(ext) && size <= 16 * 1024 * 1024) {
                        s.sourceFiles++; s.codeBytes += size;
                        try {
                            long loc = countLines(file); s.lines += loc; s.lineBuckets[lineBucket(loc)]++;
                        } catch (IOException | SecurityException ex) { s.unreadable++; }
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult visitFileFailed(Path file, IOException exc) { s.unreadable++; return FileVisitResult.CONTINUE; }
            });
            s.duplicateNames = s.names.values().stream().filter(count -> count > 1).mapToLong(count -> count - 1L).sum();
            system(s, root);
            s.trackers = buildTrackers(s, root);
            return s;
        }

        private static void system(Snapshot s, Path root) {
            Runtime runtime = Runtime.getRuntime();
            s.jvmUsed = runtime.totalMemory() - runtime.freeMemory(); s.jvmMax = runtime.maxMemory();
            java.lang.management.OperatingSystemMXBean base = ManagementFactory.getOperatingSystemMXBean();
            if (base instanceof OperatingSystemMXBean os) {
                s.systemCpu = os.getCpuLoad(); s.processCpu = os.getProcessCpuLoad();
                s.totalRam = os.getTotalMemorySize(); s.usedRam = s.totalRam - os.getFreeMemorySize();
            }
            try { FileStore store = Files.getFileStore(root); s.diskTotal = store.getTotalSpace(); s.diskUsable = store.getUsableSpace(); }
            catch (IOException ignored) {}
        }

        private static long countLines(Path file) throws IOException {
            long count = 0; boolean any = false; byte last = '\n';
            try (var in = Files.newInputStream(file)) {
                byte[] buffer = new byte[64 * 1024]; int read;
                while ((read = in.read(buffer)) >= 0) for (int i = 0; i < read; i++) { any = true; last = buffer[i]; if (last == '\n') count++; }
            }
            return count + (any && last != '\n' ? 1 : 0);
        }

        private static List<Tracker> buildTrackers(Snapshot s, Path root) {
            List<Tracker> t = new ArrayList<>();
            add(t, "Overview", "Root", root.toString(), "Directory currently being monitored");
            add(t, "Overview", "Total files", formatCount(s.files), "All regular files, recursively");
            add(t, "Overview", "Directories", formatCount(s.directories), "Subdirectories below the root");
            add(t, "Overview", "Applications", formatCount(s.appFiles), "Files inside Data/apps");
            add(t, "Code", "Lines of code", formatCount(s.lines), "Newline-aware count across recognized text/source files");
            add(t, "Code", "Source files", formatCount(s.sourceFiles), "Recognized code, markup, configuration, and docs");
            add(t, "Code", "Source bytes", humanBytes(s.codeBytes), "Storage occupied by source files");
            add(t, "Storage", "Folder size", humanBytes(s.totalBytes), formatCount(s.totalBytes) + " exact bytes");
            add(t, "Storage", "Disk total", humanBytes(s.diskTotal), "Filesystem containing Datacenter");
            add(t, "Storage", "Disk usable", humanBytes(s.diskUsable), "Space available to the current user");
            add(t, "Storage", "Disk used", humanBytes(s.diskTotal - s.diskUsable), percent(s.diskTotal == 0 ? 0 : (double)(s.diskTotal-s.diskUsable)/s.diskTotal));
            add(t, "System", "System CPU", percent(s.systemCpu), "Whole-machine recent CPU load");
            add(t, "System", "Datapro CPU", percent(s.processCpu), "Current Java process CPU load");
            add(t, "System", "System RAM", humanBytes(s.usedRam), humanBytes(s.totalRam) + " physical total");
            add(t, "System", "System RAM free", humanBytes(s.totalRam - s.usedRam), "Physical memory currently available");
            add(t, "System", "Datapro heap used", humanBytes(s.jvmUsed), "Live JVM heap cost");
            add(t, "System", "Datapro heap limit", humanBytes(s.jvmMax), "Maximum JVM heap");
            add(t, "Quality", "Unreadable files", formatCount(s.unreadable), "Failures handled without stopping the scan");
            add(t, "Quality", "Symlinks skipped", formatCount(s.symlinks), "Links are not followed to prevent cycles");
            add(t, "Quality", "Empty files", formatCount(s.empty), "Regular files with zero bytes");
            add(t, "Quality", "Hidden files", formatCount(s.hidden), "Names beginning with a dot");
            add(t, "Quality", "Duplicate names", formatCount(s.duplicateNames), "Repeated basenames in different directories");
            add(t, "Quality", "Names with spaces", formatCount(s.namesWithSpaces), "May need quoting in shell scripts");
            add(t, "Quality", "Names over 80 chars", formatCount(s.longNames), "Potential portability concern");
            add(t, "Permissions", "Readable files", formatCount(s.readable), "Readable by Datapro");
            add(t, "Permissions", "Writable files", formatCount(s.writable), "Writable by Datapro");
            add(t, "Permissions", "Executable files", formatCount(s.executable), "Executable bit or platform equivalent");
            add(t, "Structure", "Maximum depth", String.valueOf(s.maxDepth), "Deepest relative path component count");
            add(t, "Files", "Icons and images", formatCount(s.icons), "ICO, ICNS, PNG, SVG, WebP, JPEG, GIF, BMP");
            add(t, "Time", "Oldest modified", s.oldest == null ? "—" : CLOCK.format(s.oldest), "Oldest file modification time");
            add(t, "Time", "Newest modified", s.newest == null ? "—" : CLOCK.format(s.newest), "Newest file modification time");

            String[] sizeNames = {"0 B", "1 B–1 KB", "1–10 KB", "10–100 KB", "100 KB–1 MB", "1–10 MB", "10–100 MB", "100 MB+"};
            for (int i = 0; i < sizeNames.length; i++) add(t, "Size buckets", sizeNames[i], formatCount(s.sizeBuckets[i]), "Files in this size range");
            String[] ageNames = {"Modified <1 hour", "1–24 hours", "1–7 days", "7–30 days", "30–365 days", "1–5 years", "5+ years"};
            for (int i = 0; i < ageNames.length; i++) add(t, "Age buckets", ageNames[i], formatCount(s.ageBuckets[i]), "Files in this age range");
            String[] lineNames = {"0 lines", "1–25 lines", "26–100 lines", "101–250 lines", "251–500 lines", "501–1,000 lines", "1,001+ lines"};
            for (int i = 0; i < lineNames.length; i++) add(t, "Code size", lineNames[i], formatCount(s.lineBuckets[i]), "Source files in this LOC range");
            for (int depth = 0; depth <= 16; depth++) add(t, "Directory depth", "Depth " + depth, formatCount(s.depthCounts.getOrDefault(depth, 0L)), "Files and directories at this level");

            // Stable extension trackers make the dashboard useful even before a type appears.
            String[] known = {"java","class","jar","cs","csproj","js","jsx","ts","tsx","py","c","cpp","h","hpp","go","rs","rb","php","swift","kt","scala","sh","ps1","sql","html","css","scss","xml","json","yaml","yml","toml","ini","cfg","md","txt","log","csv","tsv","db","sqlite","png","jpg","jpeg","gif","svg","webp","ico","icns","bmp","pdf","doc","docx","xls","xlsx","ppt","pptx","zip","tar","gz","7z","rar","wav","mp3","ogg","flac","mp4","mkv","mov","webm","exe","dll","so","dylib","bin","dat","lock","tmp","bak","no extension"};
            for (String ext : known) {
                long count = s.extensions.getOrDefault(ext, 0L), bytes = s.extensionBytes.getOrDefault(ext, 0L);
                add(t, "Extensions", "." + ext, formatCount(count), humanBytes(bytes) + " combined");
            }
            for (Map.Entry<String, Long> entry : s.extensions.entrySet())
                if (!Arrays.asList(known).contains(entry.getKey())) add(t, "Other extensions", "." + entry.getKey(), formatCount(entry.getValue()), humanBytes(s.extensionBytes.getOrDefault(entry.getKey(), 0L)) + " combined");
            add(t, "Performance", "Tracker count", formatCount(t.size() + 2L), "Live measurements exposed by this dashboard");
            add(t, "Performance", "Refresh interval", "2 seconds", "Scanning occurs off the Swing event thread");
            return t;
        }

        private static void add(List<Tracker> list, String category, String metric, String value, String detail) { list.add(new Tracker(category, metric, value, detail)); }
        private static String extension(String name) { int dot = name.lastIndexOf('.'); return dot <= 0 || dot == name.length()-1 ? "no extension" : name.substring(dot+1); }
        private static String category(String ext) {
            if (SOURCE.contains(ext)) return "Source";
            if (ICONS.contains(ext)) return "Images";
            if (ARCHIVES.contains(ext)) return "Archives";
            if (AUDIO.contains(ext)) return "Audio";
            if (VIDEO.contains(ext)) return "Video";
            if (Set.of("class","dll","so","dylib","exe","bin").contains(ext)) return "Binaries";
            if (Set.of("pdf","doc","docx","xls","xlsx","ppt","pptx","txt").contains(ext)) return "Documents";
            return "Other";
        }
        private static int sizeBucket(long size) { if(size==0)return 0;if(size<1024)return 1;if(size<10_240)return 2;if(size<102_400)return 3;if(size<1_048_576)return 4;if(size<10_485_760)return 5;if(size<104_857_600)return 6;return 7; }
        private static int ageBucket(Duration age) { long h=Math.max(0,age.toHours());if(h<1)return 0;if(h<24)return 1;if(h<168)return 2;if(h<720)return 3;if(h<8760)return 4;if(h<43800)return 5;return 6; }
        private static int lineBucket(long lines) { if(lines==0)return 0;if(lines<=25)return 1;if(lines<=100)return 2;if(lines<=250)return 3;if(lines<=500)return 4;if(lines<=1000)return 5;return 6; }
    }
    public static void log() {
        Path dataRoot = Path.of(System.getProperty("user.home"), "Data");
        Path logDir = dataRoot.resolve("logs").resolve("apps");
        Path logFile = logDir.resolve("info.log");
        Path propsRoot = dataRoot.resolve("properties");
        Path appsJson = dataRoot.resolve("config").resolve("apps.json");
        try {
            Files.createDirectories(logDir);
            StringBuilder sb = new StringBuilder();
            String line = "─".repeat(60) + "\n";
            sb.append(line);
            sb.append("Datapro log — ").append(CLOCK.format(Instant.now())).append("\n");
            sb.append(line);

            Map<String, String[]> appMeta = loadAppMeta(appsJson);
            Path appsRoot = dataRoot.resolve("apps");
            if (Files.isDirectory(appsRoot)) {
                try (var listing = Files.list(appsRoot)) {
                    List<Path> apps = listing
                            .filter(Files::isDirectory)
                            .sorted()
                            .toList();
                    sb.append("Apps found: ").append(apps.size()).append("\n\n");
                    for (Path app : apps) {
                        String name = app.getFileName().toString();
                        String[] meta = appMeta.getOrDefault(name, new String[]{"", "", "", "", ""});
                        long fileCount = 0, dirCount = 0, totalSize = 0;
                        try (var walk = Files.walk(app, 2)) {
                            var entries = walk.toList();
                            fileCount = entries.stream().filter(Files::isRegularFile).count();
                            dirCount = entries.stream().filter(Files::isDirectory).count() - 1;
                            totalSize = entries.stream().filter(Files::isRegularFile)
                                    .filter(p -> { try { return Files.isReadable(p); } catch (Exception e) { return false; } })
                                    .mapToLong(p -> { try { return Files.size(p); } catch (IOException e) { return 0; } })
                                    .sum();
                        } catch (IOException e) {
                            sb.append("┌─ ").append(name).append("  [scan error: ").append(e.getMessage()).append("]\n└─\n");
                            continue;
                        }
                        sb.append("┌─ ").append(name).append("\n");
                        sb.append("│  Name: ").append(meta[0].isEmpty() ? name : meta[0]).append("\n");
                        sb.append("│  Language: ").append(meta[1].isEmpty() ? "unknown" : meta[1]).append("\n");
                        sb.append("│  Status: ").append(meta[2].isEmpty() ? "unknown" : meta[2]).append("\n");
                        sb.append("│  Files: ").append(fileCount).append("\n");
                        sb.append("│  Directories: ").append(dirCount).append("\n");
                        sb.append("│  Size: ").append(humanBytes(totalSize)).append("\n");
                        if (!meta[3].isEmpty()) sb.append("│  Main: ").append(meta[3]).append("\n");
                        if (!meta[4].isEmpty()) sb.append("│  Description: ").append(meta[4]).append("\n");
                        sb.append("└─\n");
                        generateProps(propsRoot, name, meta, fileCount, dirCount, totalSize);
                    }
                }
            } else {
                sb.append("Apps directory not found at ").append(appsRoot).append("\n");
            }
            sb.append(line).append("\n");
            Files.writeString(logFile, sb.toString(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Failed to write log: " + e.getMessage());
        }
    }

    private static Map<String, String[]> loadAppMeta(Path appsJson) {
        Map<String, String[]> meta = new HashMap<>();
        try {
            String json = Files.readString(appsJson);
            int idx = 0;
            while ((idx = json.indexOf("\"slug\":", idx)) != -1) {
                int sStart = json.indexOf('"', idx + 7) + 1;
                int sEnd = json.indexOf('"', sStart);
                String slug = json.substring(sStart, sEnd);

                String[] fields = new String[5];
                String source = "";
                String[] keys = {"name", "language", "status", "main", "description"};
                for (int k = 0; k < keys.length; k++) {
                    int kIdx = json.indexOf("\"" + keys[k] + "\":", idx);
                    if (kIdx != -1 && kIdx < sEnd + 400) {
                        int vStart = json.indexOf('"', kIdx + keys[k].length() + 3) + 1;
                        int vEnd = json.indexOf('"', vStart);
                        fields[k] = json.substring(vStart, vEnd).replace("\\\"", "\"");
                    } else {
                        fields[k] = "";
                    }
                }
                int srcIdx = json.indexOf("\"source\":", idx);
                if (srcIdx != -1 && srcIdx < sEnd + 400) {
                    int vStart = json.indexOf('"', srcIdx + 9) + 1;
                    int vEnd = json.indexOf('"', vStart);
                    source = json.substring(vStart, vEnd);
                }
                meta.put(slug, fields);
                if (source.startsWith("apps/")) {
                    meta.put(source.substring(5), fields);
                }
                idx = sEnd;
            }
        } catch (IOException ignored) {}
        return meta;
    }

    private static void generateProps(Path propsRoot, String appName, String[] meta,
                                      long files, long dirs, long size) throws IOException {
        Path dir = propsRoot.resolve(appName);
        Files.createDirectories(dir);
        Path propsFile = dir.resolve(appName + ".properties");
        StringBuilder sb = new StringBuilder();
        sb.append("name=").append(meta[0].isEmpty() ? appName : meta[0]).append("\n");
        sb.append("slug=").append(appName).append("\n");
        sb.append("language=").append(meta[1].isEmpty() ? "unknown" : meta[1]).append("\n");
        sb.append("status=").append(meta[2].isEmpty() ? "unknown" : meta[2]).append("\n");
        sb.append("main=").append(meta[3].isEmpty() ? "App" : meta[3]).append("\n");
        sb.append("description=").append(meta[4].isEmpty() ? "No description" : meta[4]).append("\n");
        sb.append("source=apps/").append(appName).append("\n");
        sb.append("type=data\n");
        sb.append("json=").append(appName).append(".json\n");
        sb.append("logs=logs/").append(appName).append(".log\n");
        sb.append("files=").append(files).append("\n");
        sb.append("directories=").append(dirs).append("\n");
        sb.append("size=").append(size).append("\n");
        sb.append("generated=").append(CLOCK.format(Instant.now())).append("\n");
        Files.writeString(propsFile, sb.toString(),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}