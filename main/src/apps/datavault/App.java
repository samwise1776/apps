package apps.datavault;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.text.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;

@SuppressWarnings("serial")
public class App extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String APP_NAME = "DataVault";
    private static final String VERSION = "1.0.0";
    private static final Color BG_DARK = new Color(0x1a, 0x1a, 0x2e);
    private static final Color BG_ACCENT = new Color(0x0f, 0x34, 0x60);
    private static final Color BG_CARD = new Color(0x16, 0x21, 0x3e);
    private static final Color BG_INPUT = new Color(0x1f, 0x2b, 0x4d);
    private static final Color TEXT_PRIMARY = new Color(0xe0, 0xe0, 0xe0);
    private static final Color TEXT_SECONDARY = new Color(0x90, 0x90, 0xa0);
    private static final Color ACCENT = new Color(0x0f, 0x34, 0x60);
    private static final Color ACCENT_HOVER = new Color(0x1a, 0x4a, 0x7a);
    private static final Color SUCCESS = new Color(0x2e, 0xcc, 0x71);
    private static final Color WARNING = new Color(0xf3, 0x9c, 0x12);
    private static final Color DANGER = new Color(0xe7, 0x4c, 0x3c);

    private Path backupDir = Paths.get(System.getProperty("user.home"), "DataVault_Backups");
    private final List<BackupEntry> history = new ArrayList<>();
    private JPanel mainContent;
    private CardLayout cardLayout;
    private JLabel statusLabel;
    private JTextField backupPathField;
    private JTable historyTable;
    private DefaultTableModel historyTableModel;
    private JProgressBar progressBar;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new App().setVisible(true));
    }

    @SuppressWarnings("this-escape")
    public App() {
        super(APP_NAME);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(800, 550));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        loadHistory();
        initUI();
        initKeyBindings();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DARK);
        root.add(createSidebar(), BorderLayout.WEST);
        root.add(createMainPanel(), BorderLayout.CENTER);
        root.add(createStatusBar(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(BG_ACCENT);
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 0, 1, new Color(0x0a, 0x25, 0x40)),
            new EmptyBorder(16, 0, 16, 0)
        ));

        JLabel logo = new JLabel("  " + APP_NAME);
        logo.setFont(new Font("SansSerif", Font.BOLD, 20));
        logo.setForeground(TEXT_PRIMARY);
        logo.setAlignmentX(LEFT_ALIGNMENT);
        logo.setBorder(new EmptyBorder(0, 8, 24, 0));
        sidebar.add(logo);

        String[] navItems = {"Backups", "History", "Verify", "Settings"};
        for (String item : navItems) {
            JButton btn = createNavButton(item);
            sidebar.add(btn);
            sidebar.add(Box.createRigidArea(new Dimension(0, 4)));
        }

        sidebar.add(Box.createVerticalGlue());
        JLabel ver = new JLabel("  v" + VERSION);
        ver.setFont(new Font("SansSerif", Font.PLAIN, 11));
        ver.setForeground(TEXT_SECONDARY);
        ver.setAlignmentX(LEFT_ALIGNMENT);
        sidebar.add(ver);
        return sidebar;
    }

    private JButton createNavButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        btn.setForeground(TEXT_PRIMARY);
        btn.setBackground(BG_CARD);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(180, 42));
        btn.setMinimumSize(new Dimension(180, 42));
        btn.setPreferredSize(new Dimension(180, 42));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(8, 20, 8, 12));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(ACCENT_HOVER); }
            public void mouseExited(MouseEvent e) { btn.setBackground(BG_CARD); }
        });
        btn.addActionListener(e -> switchPanel(text));
        return btn;
    }

    private JPanel createMainPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_DARK);
        cardLayout = new CardLayout();
        mainContent = new JPanel(cardLayout);
        mainContent.setBackground(BG_DARK);

        mainContent.add(createBackupsPanel(), "Backups");
        mainContent.add(createHistoryPanel(), "History");
        mainContent.add(createVerifyPanel(), "Verify");
        mainContent.add(createSettingsPanel(), "Settings");

        wrapper.add(mainContent, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createBackupsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel title = new JLabel("Backup Manager");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(TEXT_PRIMARY);
        panel.add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(3, 2, 16, 16));
        center.setBackground(BG_DARK);
        center.add(createActionCard("Create Backup", "Create a new timestamped backup with SHA-256 checksum", e -> createBackup()));
        center.add(createActionCard("Restore Backup", "Browse and extract files from an existing backup", e -> restoreBackup()));
        center.add(createActionCard("Quick Project Backup", "Backup the currently selected project folder", e -> quickProjectBackup()));
        center.add(createActionCard("Full Tree Backup", "Backup the entire source tree at once", e -> fullTreeBackup()));
        center.add(createActionCard("Compare Versions", "Show differences between two backup versions", e -> compareVersions()));
        center.add(createActionCard("Export Checksums", "Export all backup checksums to a manifest file", e -> exportChecksums()));
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createActionCard(String title, String desc, ActionListener action) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(BG_CARD);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(0x2a, 0x3a, 0x5e), 1, true),
            new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.BOLD, 16));
        t.setForeground(TEXT_PRIMARY);
        card.add(t, BorderLayout.NORTH);

        JLabel d = new JLabel("<html><body style='width: 200px'>" + desc + "</body></html>");
        d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        d.setForeground(TEXT_SECONDARY);
        card.add(d, BorderLayout.CENTER);

        JButton btn = new JButton("Open");
        btn.setBackground(ACCENT);
        btn.setForeground(TEXT_PRIMARY);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(100, 32));
        btn.addActionListener(action);
        card.add(btn, BorderLayout.SOUTH);

        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { card.setBackground(new Color(0x1c, 0x29, 0x50)); }
            public void mouseExited(MouseEvent e) { card.setBackground(BG_CARD); }
        });
        return card;
    }

    private void createBackup() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Files/Folders to Back Up");
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setMultiSelectionEnabled(true);
        chooser.setAcceptAllFileFilterUsed(true);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File[] selected = chooser.getSelectedFiles();
        if (selected.length == 0) return;

        String name = JOptionPane.showInputDialog(this, "Backup name:", "my-backup");
        if (name == null || name.isBlank()) name = "backup-" + fmtTs();

        Path outDir = backupDir;
        if (!Files.exists(outDir)) {
            try { Files.createDirectories(outDir); } catch (IOException ex) {
                showError("Cannot create backup directory: " + ex.getMessage());
                return;
            }
        }
        Path zipFile = outDir.resolve(sanitizeFilename(name) + "_" + fmtTs() + ".zip");

        progressBar.setValue(0);
        progressBar.setString("Creating backup...");
        progressBar.setVisible(true);
        setStatus("Creating backup: " + zipFile.getFileName());

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            protected Boolean doInBackground() {
                try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
                    int count = 0;
                    for (File f : selected) {
                        if (f.isDirectory()) {
                            count += zipDirectory(zos, f.toPath(), f.toPath(), selected.length);
                        } else {
                            zipFile(zos, f.toPath(), f.toPath());
                            count++;
                        }
                    }
                    String sha = computeSha256(zipFile);
                    Path meta = outDir.resolve(zipFile.getFileName().toString() + ".sha256");
                    Files.writeString(meta, sha + "  " + zipFile.getFileName() + "\n");
                    history.add(new BackupEntry(zipFile.getFileName().toString(), zipFile, Instant.now(), Files.size(zipFile), count, sha));
                    saveHistory();
                    return true;
                } catch (Exception ex) {
                    showError("Backup failed: " + ex.getMessage());
                    return false;
                }
            }
            protected void done() {
                progressBar.setVisible(false);
                try {
                    if (get()) setStatus("Backup created successfully: " + zipFile.getFileName());
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void restoreBackup() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Backup to Restore");
        chooser.setFileFilter(new FileNameExtensionFilter("ZIP Archives", "zip"));
        chooser.setCurrentDirectory(backupDir.toFile());
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File zip = chooser.getSelectedFile();

        JFileChooser destChooser = new JFileChooser();
        destChooser.setDialogTitle("Select Restore Destination");
        destChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (destChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File dest = destChooser.getSelectedFile();

        if (!confirm("Restore all files from " + zip.getName() + " to " + dest.getName() + "?")) return;

        progressBar.setValue(0);
        progressBar.setString("Restoring...");
        progressBar.setVisible(true);
        setStatus("Restoring from: " + zip.getName());

        SwingWorker<Boolean, Integer> worker = new SwingWorker<>() {
            protected Boolean doInBackground() {
                try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip.toPath()))) {
                    ZipEntry entry;
                    int count = 0;
                    while ((entry = zis.getNextEntry()) != null) {
                        Path target = dest.toPath().resolve(entry.getName()).normalize();
                        if (!target.startsWith(dest.toPath())) {
                            showError("Zip path traversal detected, aborting.");
                            return false;
                        }
                        if (entry.isDirectory()) {
                            Files.createDirectories(target);
                        } else {
                            Files.createDirectories(target.getParent());
                            Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                        }
                        count++;
                        if (count % 10 == 0) publish(count);
                    }
                    return true;
                } catch (Exception ex) {
                    showError("Restore failed: " + ex.getMessage());
                    return false;
                }
            }
            protected void process(List<Integer> chunks) {
                int last = chunks.get(chunks.size() - 1);
                progressBar.setValue(Math.min(last, 99));
            }
            protected void done() {
                progressBar.setVisible(false);
                try { if (get()) setStatus("Restore completed successfully."); } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void quickProjectBackup() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Project Folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File dir = chooser.getSelectedFile();
        String name = dir.getName() + "-project-" + fmtTs();
        performBackup(new File[]{dir}, name);
    }

    private void fullTreeBackup() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Root of Source Tree");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File dir = chooser.getSelectedFile();
        String name = dir.getName() + "-full-" + fmtTs();
        performBackup(new File[]{dir}, name);
    }

    private void compareVersions() {
        if (!Files.exists(backupDir)) { showError("No backup directory found."); return; }
        try (var stream = Files.list(backupDir)) {
            List<Path> zips = stream.filter(p -> p.toString().endsWith(".zip")).sorted(Comparator.reverseOrder()).toList();
            if (zips.size() < 2) { showError("Need at least 2 backups to compare."); return; }

            String[] names = zips.stream().map(p -> p.getFileName().toString()).toArray(String[]::new);
            JComboBox<String> box1 = new JComboBox<>(names);
            JComboBox<String> box2 = new JComboBox<>(names);
            if (names.length > 1) box2.setSelectedIndex(1);

            JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
            panel.setBackground(BG_CARD);
            panel.setBorder(new EmptyBorder(12, 12, 12, 12));
            JLabel l1 = new JLabel("Version A:"); l1.setForeground(TEXT_PRIMARY);
            JLabel l2 = new JLabel("Version B:"); l2.setForeground(TEXT_PRIMARY);
            box1.setBackground(BG_INPUT); box1.setForeground(TEXT_PRIMARY);
            box2.setBackground(BG_INPUT); box2.setForeground(TEXT_PRIMARY);
            panel.add(l1); panel.add(box1);
            panel.add(l2); panel.add(box2);

            int result = JOptionPane.showConfirmDialog(this, panel, "Compare Versions", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;

            Path zipA = zips.get(box1.getSelectedIndex());
            Path zipB = zips.get(box2.getSelectedIndex());

            Set<String> entriesA = listZipEntries(zipA);
            Set<String> entriesB = listZipEntries(zipB);

            StringBuilder sb = new StringBuilder();
            sb.append("Comparison:\n  A: ").append(zipA.getFileName()).append("\n  B: ").append(zipB.getFileName()).append("\n\n");

            Set<String> onlyInA = new TreeSet<>(entriesA);
            onlyInA.removeAll(entriesB);
            Set<String> onlyInB = new TreeSet<>(entriesB);
            onlyInB.removeAll(entriesA);
            Set<String> common = new TreeSet<>(entriesA);
            common.retainAll(entriesB);

            sb.append("Only in A (").append(onlyInA.size()).append("):\n");
            onlyInA.forEach(e -> sb.append("  - ").append(e).append("\n"));
            sb.append("\nOnly in B (").append(onlyInB.size()).append("):\n");
            onlyInB.forEach(e -> sb.append("  + ").append(e).append("\n"));
            sb.append("\nCommon (").append(common.size()).append("):\n");
            common.forEach(e -> sb.append("    ").append(e).append("\n"));
            sb.append("\nTotal: A=").append(entriesA.size()).append(" B=").append(entriesB.size());

            JTextArea ta = new JTextArea(sb.toString());
            ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
            ta.setEditable(false);
            ta.setBackground(BG_CARD);
            ta.setForeground(TEXT_PRIMARY);
            ta.setCaretColor(TEXT_PRIMARY);
            JScrollPane sp = new JScrollPane(ta);
            sp.setPreferredSize(new Dimension(600, 450));
            JOptionPane.showMessageDialog(this, sp, "Version Comparison", JOptionPane.PLAIN_MESSAGE);
        } catch (IOException ex) {
            showError("Error listing backups: " + ex.getMessage());
        }
    }

    private void exportChecksums() {
        if (!Files.exists(backupDir)) { showError("No backup directory found."); return; }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Checksum Manifest");
        chooser.setSelectedFile(new File("checksums.sha256"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File out = chooser.getSelectedFile();

        try (var stream = Files.list(backupDir)) {
            List<Path> zips = stream.filter(p -> p.toString().endsWith(".zip")).toList();
            StringBuilder sb = new StringBuilder();
            sb.append("# DataVault Checksum Manifest - ").append(LocalDateTime.now()).append("\n");
            sb.append("# Format: sha256  filename\n\n");
            for (Path zip : zips) {
                try {
                    String sha = computeSha256(zip);
                    sb.append(sha).append("  ").append(zip.getFileName()).append("\n");
                } catch (Exception ex) {
                    sb.append("# ERROR computing checksum for ").append(zip.getFileName()).append(": ").append(ex.getMessage()).append("\n");
                }
            }
            Files.writeString(out.toPath(), sb.toString());
            setStatus("Checksum manifest exported: " + out.getName());
            JOptionPane.showMessageDialog(this, "Manifest saved to " + out.getAbsolutePath(), "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            showError("Export failed: " + ex.getMessage());
        }
    }

    private Set<String> listZipEntries(Path zip) {
        Set<String> entries = new TreeSet<>();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) entries.add(entry.getName());
        } catch (IOException ignored) {}
        return entries;
    }

    private void performBackup(File[] sources, String name) {
        Path outDir = backupDir;
        if (!Files.exists(outDir)) { try { Files.createDirectories(outDir); } catch (IOException ex) { showError(ex.getMessage()); return; } }
        Path zipFile = outDir.resolve(sanitizeFilename(name) + ".zip");
        progressBar.setValue(0);
        progressBar.setString("Backing up...");
        progressBar.setVisible(true);
        setStatus("Creating backup: " + zipFile.getFileName());

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            protected Boolean doInBackground() {
                try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
                    int count = 0;
                    for (File f : sources) {
                        if (f.isDirectory()) count += zipDirectory(zos, f.toPath(), f.toPath(), sources.length);
                        else { zipFile(zos, f.toPath(), f.toPath()); count++; }
                    }
                    String sha = computeSha256(zipFile);
                    Path meta = outDir.resolve(zipFile.getFileName().toString() + ".sha256");
                    Files.writeString(meta, sha + "  " + zipFile.getFileName() + "\n");
                    history.add(new BackupEntry(zipFile.getFileName().toString(), zipFile, Instant.now(), Files.size(zipFile), count, sha));
                    saveHistory();
                    return true;
                } catch (Exception ex) {
                    showError("Backup failed: " + ex.getMessage());
                    return false;
                }
            }
            protected void done() {
                progressBar.setVisible(false);
                try { if (get()) setStatus("Backup created: " + zipFile.getFileName()); } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_DARK);
        JLabel title = new JLabel("Backup History");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);

        JButton refreshBtn = createStyledButton("Refresh", ACCENT);
        refreshBtn.addActionListener(e -> refreshHistory());
        header.add(refreshBtn, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        String[] cols = {"Name", "Date", "Size", "Files", "Checksum"};
        historyTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        historyTable = new JTable(historyTableModel);
        styleTable(historyTable);

        JScrollPane sp = new JScrollPane(historyTable);
        sp.setBorder(new LineBorder(new Color(0x2a, 0x3a, 0x5e), 1, true));
        sp.getViewport().setBackground(BG_CARD);
        panel.add(sp, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setBackground(BG_DARK);
        JButton viewBtn = createStyledButton("View Contents", ACCENT);
        viewBtn.addActionListener(e -> viewBackupContents());
        JButton deleteBtn = createStyledButton("Delete", DANGER);
        deleteBtn.addActionListener(e -> deleteBackup());
        actions.add(viewBtn);
        actions.add(deleteBtn);
        panel.add(actions, BorderLayout.SOUTH);

        refreshHistory();
        return panel;
    }

    private void refreshHistory() {
        historyTableModel.setRowCount(0);
        if (!Files.exists(backupDir)) return;
        try (var stream = Files.list(backupDir)) {
            stream.filter(p -> p.toString().endsWith(".zip"))
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        long size = Files.size(p);
                        String sha = "";
                        Path meta = backupDir.resolve(p.getFileName() + ".sha256");
                        if (Files.exists(meta)) sha = Files.readString(meta).trim().split("\\s+")[0];
                        historyTableModel.addRow(new Object[]{
                            p.getFileName().toString(),
                            Instant.ofEpochMilli(p.toFile().lastModified()).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                            formatSize(size),
                            countZipEntries(p),
                            sha.length() > 16 ? sha.substring(0, 16) + "..." : sha
                        });
                    } catch (Exception ignored) {}
                });
        } catch (IOException ignored) {}
    }

    private void viewBackupContents() {
        int row = historyTable.getSelectedRow();
        if (row < 0) { showError("Select a backup first."); return; }
        String name = (String) historyTableModel.getValueAt(row, 0);
        Path zip = backupDir.resolve(name);
        if (!Files.exists(zip)) { showError("Backup file not found."); return; }

        StringBuilder sb = new StringBuilder();
        sb.append("Contents of ").append(name).append(":\n\n");
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            int count = 0;
            while ((entry = zis.getNextEntry()) != null) {
                sb.append("  ").append(entry.isDirectory() ? "[DIR] " : "      ").append(entry.getName()).append("\n");
                count++;
                if (count > 200) { sb.append("  ... and more entries\n"); break; }
            }
        } catch (IOException ex) {
            showError("Cannot read backup: " + ex.getMessage());
            return;
        }

        JTextArea ta = new JTextArea(sb.toString());
        ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
        ta.setEditable(false);
        ta.setBackground(BG_CARD);
        ta.setForeground(TEXT_PRIMARY);
        ta.setCaretColor(TEXT_PRIMARY);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(550, 400));
        JOptionPane.showMessageDialog(this, sp, "Backup Contents", JOptionPane.PLAIN_MESSAGE);
    }

    private void deleteBackup() {
        int row = historyTable.getSelectedRow();
        if (row < 0) { showError("Select a backup first."); return; }
        String name = (String) historyTableModel.getValueAt(row, 0);
        if (!confirm("Permanently delete " + name + "? This cannot be undone.")) return;
        Path zip = backupDir.resolve(name);
        Path meta = backupDir.resolve(name + ".sha256");
        try {
            Files.deleteIfExists(zip);
            Files.deleteIfExists(meta);
            refreshHistory();
            setStatus("Deleted: " + name);
        } catch (IOException ex) {
            showError("Delete failed: " + ex.getMessage());
        }
    }

    private JPanel createVerifyPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel title = new JLabel("Verify Integrity");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(TEXT_PRIMARY);
        panel.add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setBackground(BG_DARK);

        JLabel desc = new JLabel("Select a backup to verify its SHA-256 checksum against the stored value.");
        desc.setForeground(TEXT_SECONDARY);
        center.add(desc, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnPanel.setBackground(BG_DARK);
        JButton verifyBtn = createStyledButton("Select & Verify", ACCENT);
        verifyBtn.addActionListener(e -> verifyBackup());
        JButton verifyAllBtn = createStyledButton("Verify All Backups", ACCENT_HOVER);
        verifyAllBtn.addActionListener(e -> verifyAllBackups());
        btnPanel.add(verifyBtn);
        btnPanel.add(verifyAllBtn);
        center.add(btnPanel, BorderLayout.CENTER);

        JTextArea resultArea = new JTextArea();
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        resultArea.setEditable(false);
        resultArea.setBackground(BG_CARD);
        resultArea.setForeground(TEXT_PRIMARY);
        resultArea.setBorder(new EmptyBorder(12, 12, 12, 12));
        JScrollPane sp = new JScrollPane(resultArea);
        sp.setBorder(new LineBorder(new Color(0x2a, 0x3a, 0x5e), 1, true));
        center.add(sp, BorderLayout.SOUTH);
        panel.add(center, BorderLayout.CENTER);

        return panel;
    }

    private void verifyBackup() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Backup to Verify");
        chooser.setFileFilter(new FileNameExtensionFilter("ZIP Archives", "zip"));
        chooser.setCurrentDirectory(backupDir.toFile());
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        verifyFile(chooser.getSelectedFile().toPath());
    }

    private void verifyFile(Path zip) {
        setStatusBar("Verifying: " + zip.getFileName());
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            protected String doInBackground() {
                try {
                    String actual = computeSha256(zip);
                    Path meta = backupDir.resolve(zip.getFileName() + ".sha256");
                    if (Files.exists(meta)) {
                        String expected = Files.readString(meta).trim().split("\\s+")[0];
                        if (actual.equals(expected)) {
                            return "PASS: " + zip.getFileName() + "\n  Expected: " + expected + "\n  Actual:   " + actual;
                        } else {
                            return "FAIL: " + zip.getFileName() + "\n  Expected: " + expected + "\n  Actual:   " + actual;
                        }
                    } else {
                        return "WARN: No checksum file found for " + zip.getFileName() + "\n  Computed: " + actual;
                    }
                } catch (Exception ex) {
                    return "ERROR: " + ex.getMessage();
                }
            }
            protected void done() {
                try {
                    String result = get();
                    JTextArea area = new JTextArea(result);
                    area.setFont(new Font("Monospaced", Font.PLAIN, 12));
                    area.setEditable(false);
                    area.setBackground(BG_CARD);
                    area.setForeground(result.startsWith("PASS") ? SUCCESS : result.startsWith("WARN") ? WARNING : DANGER);
                    area.setBorder(new EmptyBorder(8, 8, 8, 8));
                    JOptionPane.showMessageDialog(App.this, area, "Verification Result", JOptionPane.PLAIN_MESSAGE);
                    setStatus(result.contains("PASS") ? "Verification passed." : "Verification issue detected.");
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void verifyAllBackups() {
        if (!Files.exists(backupDir)) { showError("No backup directory found."); return; }
        try (var stream = Files.list(backupDir)) {
            List<Path> zips = stream.filter(p -> p.toString().endsWith(".zip")).toList();
            if (zips.isEmpty()) { showError("No backups found."); return; }
            StringBuilder sb = new StringBuilder("Verification Report:\n\n");
            int pass = 0, fail = 0;
            for (Path zip : zips) {
                try {
                    String actual = computeSha256(zip);
                    Path meta = backupDir.resolve(zip.getFileName() + ".sha256");
                    if (Files.exists(meta)) {
                        String expected = Files.readString(meta).trim().split("\\s+")[0];
                        if (actual.equals(expected)) { sb.append("[PASS] ").append(zip.getFileName()).append("\n"); pass++; }
                        else { sb.append("[FAIL] ").append(zip.getFileName()).append("\n"); fail++; }
                    } else {
                        sb.append("[WARN] ").append(zip.getFileName()).append(" - no checksum file\n");
                    }
                } catch (Exception ex) {
                    sb.append("[ERR]  ").append(zip.getFileName()).append(" - ").append(ex.getMessage()).append("\n");
                }
            }
            sb.append("\nResult: ").append(pass).append(" passed, ").append(fail).append(" failed, ").append(zips.size()).append(" total.");
            JTextArea area = new JTextArea(sb.toString());
            area.setFont(new Font("Monospaced", Font.PLAIN, 12));
            area.setEditable(false);
            area.setBackground(BG_CARD);
            area.setForeground(TEXT_PRIMARY);
            area.setBorder(new EmptyBorder(8, 8, 8, 8));
            JOptionPane.showMessageDialog(this, area, "Batch Verification", JOptionPane.PLAIN_MESSAGE);
        } catch (IOException ex) {
            showError("Error listing backups: " + ex.getMessage());
        }
    }

    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel title = new JLabel("Settings");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(TEXT_PRIMARY);
        panel.add(title, BorderLayout.NORTH);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(BG_CARD);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(0x2a, 0x3a, 0x5e), 1, true),
            new EmptyBorder(24, 24, 24, 24)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel pathLabel = new JLabel("Default Backup Location:");
        pathLabel.setForeground(TEXT_PRIMARY);
        pathLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        card.add(pathLabel, gbc);

        backupPathField = new JTextField(backupDir.toString(), 35);
        backupPathField.setBackground(BG_INPUT);
        backupPathField.setForeground(TEXT_PRIMARY);
        backupPathField.setCaretColor(TEXT_PRIMARY);
        backupPathField.setBorder(new CompoundBorder(
            new LineBorder(new Color(0x2a, 0x3a, 0x5e), 1, true),
            new EmptyBorder(6, 8, 6, 8)
        ));
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        card.add(backupPathField, gbc);

        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE;
        JButton browseBtn = createStyledButton("Browse", ACCENT);
        browseBtn.addActionListener(e -> {
            JFileChooser c = new JFileChooser();
            c.setDialogTitle("Select Backup Directory");
            c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (c.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                backupPathField.setText(c.getSelectedFile().getAbsolutePath());
            }
        });
        card.add(browseBtn, gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2;
        JButton saveBtn = createStyledButton("Save Settings", SUCCESS);
        saveBtn.addActionListener(e -> {
            backupDir = Paths.get(backupPathField.getText().trim());
            if (!Files.exists(backupDir)) {
                try { Files.createDirectories(backupDir); } catch (IOException ex) {
                    showError("Cannot create directory: " + ex.getMessage());
                    return;
                }
            }
            setStatus("Settings saved. Backup directory: " + backupDir);
            JOptionPane.showMessageDialog(this, "Settings saved.", "Success", JOptionPane.INFORMATION_MESSAGE);
        });
        card.add(saveBtn, gbc);

        panel.add(card, BorderLayout.NORTH);

        JLabel aboutLabel = new JLabel("<html><body><br><br><br><br><hr width='400' noshade color='#2a3a5e'><br>"
            + "<b>" + APP_NAME + " v" + VERSION + "</b><br>"
            + "Backup and Recovery Manager<br>"
            + "For Datacenter Company<br><br>"
            + "Features:<br>"
            + "- SHA-256 checksum verification<br>"
            + "- ZIP-based backups with compression<br>"
            + "- Integrity verification<br>"
            + "- Version comparison<br>"
            + "- Keyboard shortcuts (Ctrl+N, Ctrl+O, Ctrl+V, F5)<br>"
            + "</body></html>");
        aboutLabel.setForeground(TEXT_SECONDARY);
        panel.add(aboutLabel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBackground(new Color(0x0a, 0x14, 0x28));
        bar.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, new Color(0x2a, 0x3a, 0x5e)),
            new EmptyBorder(6, 12, 6, 12)
        ));
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(TEXT_SECONDARY);
        bar.add(statusLabel, BorderLayout.WEST);

        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(200, 16));
        progressBar.setVisible(false);
        bar.add(progressBar, BorderLayout.EAST);
        return bar;
    }

    private void initKeyBindings() {
        JPanel content = (JPanel) getContentPane();
        InputMap im = content.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = content.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "newBackup");
        am.put("newBackup", new AbstractAction() { public void actionPerformed(ActionEvent e) { createBackup(); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), "openBackup");
        am.put("openBackup", new AbstractAction() { public void actionPerformed(ActionEvent e) { restoreBackup(); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK), "verify");
        am.put("verify", new AbstractAction() { public void actionPerformed(ActionEvent e) { verifyBackup(); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh");
        am.put("refresh", new AbstractAction() { public void actionPerformed(ActionEvent e) { refreshHistory(); } });
    }

    private void switchPanel(String name) {
        cardLayout.show(mainContent, name);
        if ("History".equals(name)) refreshHistory();
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg);
    }

    private void setStatusBar(String msg) {
        statusLabel.setText(msg);
    }

    private boolean confirm(String msg) {
        return JOptionPane.showConfirmDialog(this, msg, "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private int zipDirectory(ZipOutputStream zos, Path root, Path base, int totalDirs) throws IOException {
        int count = 0;
        try (var walker = Files.walk(root)) {
            for (Path p : walker.toList()) {
                if (Files.isRegularFile(p)) {
                    zipFile(zos, p, base);
                    count++;
                }
            }
        }
        return count;
    }

    private void zipFile(ZipOutputStream zos, Path file, Path base) throws IOException {
        String entryName = base.relativize(file).toString().replace(File.separatorChar, '/');
        zos.putNextEntry(new ZipEntry(entryName));
        Files.copy(file, zos);
        zos.closeEntry();
    }

    private String computeSha256(Path file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream is = Files.newInputStream(file)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) md.update(buf, 0, n);
        }
        byte[] hash = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private int countZipEntries(Path zip) {
        int count = 0;
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
            while (zis.getNextEntry() != null) count++;
        } catch (IOException ignored) {}
        return count;
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private static String fmtTs() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    private static String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    private void loadHistory() {
        // History is loaded from disk on demand in refreshHistory()
    }

    private void saveHistory() {
        // Metadata stored as .sha256 sidecar files
    }

    private void styleTable(JTable table) {
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_PRIMARY);
        table.setSelectionBackground(ACCENT);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setGridColor(new Color(0x2a, 0x3a, 0x5e));
        table.setRowHeight(32);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.getTableHeader().setBackground(BG_ACCENT);
        table.getTableHeader().setForeground(TEXT_PRIMARY);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        table.getTableHeader().setBorder(new LineBorder(new Color(0x2a, 0x3a, 0x5e)));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(new DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
                    super.getTableCellRendererComponent(t, v, sel, focus, r, c);
                    setBackground(sel ? ACCENT : BG_CARD);
                    setForeground(TEXT_PRIMARY);
                    setBorder(new EmptyBorder(4, 8, 4, 8));
                    return this;
                }
            });
        }
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(TEXT_PRIMARY);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bg.brighter()); }
            public void mouseExited(MouseEvent e) { btn.setBackground(bg); }
        });
        return btn;
    }

    private static class BackupEntry {
        final String name;
        final Path path;
        final Instant date;
        final long size;
        final int fileCount;
        final String checksum;

        BackupEntry(String name, Path path, Instant date, long size, int fileCount, String checksum) {
            this.name = name; this.path = path; this.date = date;
            this.size = size; this.fileCount = fileCount; this.checksum = checksum;
        }
    }
}
