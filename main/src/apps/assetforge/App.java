package apps.assetforge;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.text.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import java.util.stream.*;

public class App extends JFrame {

    private static final Color BG = new Color(0x1a1a2e);
    private static final Color ACCENT = new Color(0x0f3460);
    private static final Color TEXT = new Color(0xe0e0e0);
    private static final Color BTN_BG = new Color(0x16213e);
    private static final Color CARD_BG = new Color(0x202040);
    private static final Color SELECTION_BG = new Color(0x0f3460);

    private JTree dirTree;
    private JList<String> assetList;
    private JLabel previewLabel;
    private JLabel metaLabel;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JTextField searchField;
    private JPanel detailsPanel;
    private JPanel toolbarPanel;
    private JSplitPane mainSplit;
    private JSplitPane rightSplit;
    private DefaultListModel<String> listModel;
    private DefaultTreeModel treeModel;
    private File currentDir;
    private String rootPath;
    private Map<String, Set<String>> tags;
    private Map<String, String> projectMap;
    private List<AssetEntry> allAssets;
    private List<AssetEntry> filteredAssets;
    private Set<String> selectedTags;
    private String activeMode;
    private JFrame settingsDialog;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new App().setVisible(true));
    }

    public App() {
        super("AssetForge - Project Asset Manager");
        tags = new HashMap<>();
        projectMap = new HashMap<>();
        allAssets = new ArrayList<>();
        filteredAssets = new ArrayList<>();
        selectedTags = new HashSet<>();
        activeMode = "browse";
        rootPath = System.getProperty("user.home");
        loadConfig();
        initUI();
        bindKeys();
        setSize(1400, 800);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void loadConfig() {
        try {
            Path cfg = Path.of(rootPath, ".assetforge", "tags.dat");
            if (Files.exists(cfg)) {
                Files.readAllLines(cfg).forEach(l -> {
                    String[] parts = l.split("\\|");
                    if (parts.length >= 2) {
                        tags.computeIfAbsent(parts[0], k -> new HashSet<>()).addAll(
                            Arrays.asList(parts[1].split(",")));
                    }
                });
            }
        } catch (Exception ignored) {}
    }

    private void saveConfig() {
        try {
            Path dir = Path.of(rootPath, ".assetforge");
            Files.createDirectories(dir);
            List<String> lines = new ArrayList<>();
            tags.forEach((file, tagSet) ->
                lines.add(file + "|" + String.join(",", tagSet)));
            Files.write(dir.resolve("tags.dat"), lines);
        } catch (Exception ignored) {}
    }

    private void initUI() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(4, 4, 4, 4));

        toolbarPanel = buildToolbar();
        content.add(toolbarPanel, BorderLayout.NORTH);

        mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setBackground(BG);
        mainSplit.setDividerLocation(240);

        dirTree = new JTree(buildTreeModel());
        styleTree();
        dirTree.addTreeSelectionListener(e -> onTreeSelect());
        JScrollPane treeScroll = new JScrollPane(dirTree);
        treeScroll.setBorder(BorderFactory.createLineBorder(ACCENT, 1));
        treeScroll.setPreferredSize(new Dimension(240, 0));
        mainSplit.setLeftComponent(treeScroll);

        rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        rightSplit.setBackground(BG);
        rightSplit.setDividerLocation(400);

        listModel = new DefaultListModel<>();
        assetList = new JList<>(listModel);
        styleList();
        assetList.addListSelectionListener(e -> onAssetSelect());
        JScrollPane listScroll = new JScrollPane(assetList);
        listScroll.setBorder(BorderFactory.createLineBorder(ACCENT, 1));
        rightSplit.setLeftComponent(listScroll);

        detailsPanel = buildDetailsPanel();
        rightSplit.setRightComponent(new JScrollPane(detailsPanel));
        mainSplit.setRightComponent(rightSplit);

        content.add(mainSplit, BorderLayout.CENTER);

        JPanel statusPanel = buildStatusBar();
        content.add(statusPanel, BorderLayout.SOUTH);

        setContentPane(content);
    }

    private DefaultTreeModel buildTreeModel() {
        File root = new File(rootPath);
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(root.getName());
        if (root.isDirectory()) {
            File[] dirs = root.listFiles(File::isDirectory);
            if (dirs != null) {
                Arrays.sort(dirs);
                for (File d : dirs) {
                    rootNode.add(new DefaultMutableTreeNode(d.getName()));
                }
            }
        }
        return new DefaultTreeModel(rootNode);
    }

    private void styleTree() {
        dirTree.setBackground(BG);
        dirTree.setForeground(TEXT);
        dirTree.setFont(new Font("Monospaced", Font.PLAIN, 13));
        dirTree.setCellRenderer(new DefaultTreeCellRenderer() {
            {
                setBackgroundNonSelectionColor(BG);
                setBackgroundSelectionColor(SELECTION_BG);
                setTextNonSelectionColor(TEXT);
                setTextSelectionColor(Color.WHITE);
                setBorderSelectionColor(ACCENT);
            }
        });
    }

    private void styleList() {
        assetList.setBackground(BG);
        assetList.setForeground(TEXT);
        assetList.setFont(new Font("Monospaced", Font.PLAIN, 13));
        assetList.setSelectionBackground(SELECTION_BG);
        assetList.setSelectionForeground(Color.WHITE);
        assetList.setCellRenderer(new AssetListRenderer());
    }

    private JPanel buildToolbar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.setBackground(BG);

        String[] buttons = {"Browse", "Search", "Duplicates", "Tags", "Projects", "Unused"};
        for (String b : buttons) {
            JButton btn = makeButton(b);
            btn.addActionListener(e -> switchMode(b.toLowerCase()));
            p.add(btn);
        }

        p.add(Box.createHorizontalStrut(20));
        searchField = new JTextField(20);
        searchField.setBackground(CARD_BG);
        searchField.setForeground(TEXT);
        searchField.setCaretColor(TEXT);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT),
            new EmptyBorder(4, 6, 4, 6)));
        searchField.addActionListener(e -> performSearch());
        p.add(searchField);

        JButton searchBtn = makeButton("Go");
        searchBtn.addActionListener(e -> performSearch());
        p.add(searchBtn);

        return p;
    }

    private JButton makeButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(BTN_BG);
        btn.setForeground(TEXT);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT),
            new EmptyBorder(4, 12, 4, 12)));
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(ACCENT); }
            public void mouseExited(MouseEvent e) { btn.setBackground(BTN_BG); }
        });
        return btn;
    }

    private JPanel buildDetailsPanel() {
        JPanel p = new JPanel();
        p.setBackground(BG);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        previewLabel = new JLabel("Select an asset to preview");
        previewLabel.setAlignmentX(CENTER_ALIGNMENT);
        previewLabel.setForeground(TEXT);
        previewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        previewLabel.setPreferredSize(new Dimension(400, 300));
        previewLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT),
            new EmptyBorder(8, 8, 8, 8)));
        p.add(previewLabel);

        p.add(Box.createVerticalStrut(10));

        metaLabel = new JLabel("<html><body style='color:#e0e0e0;font-family:monospace'>No asset selected</body></html>");
        metaLabel.setAlignmentX(CENTER_ALIGNMENT);
        metaLabel.setForeground(TEXT);
        metaLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        p.add(metaLabel);

        p.add(Box.createVerticalStrut(10));

        JPanel tagPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        tagPanel.setBackground(BG);
        JLabel tagLbl = new JLabel("Tags: ");
        tagLbl.setForeground(TEXT);
        tagLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        tagPanel.add(tagLbl);
        JButton addTagBtn = makeButton("+ Tag");
        addTagBtn.addActionListener(e -> addTagToSelected());
        tagPanel.add(addTagBtn);
        p.add(tagPanel);

        p.add(Box.createVerticalStrut(10));

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        actionPanel.setBackground(BG);
        JButton renameBtn = makeButton("Rename (Ctrl+R)");
        renameBtn.addActionListener(e -> safeRename());
        actionPanel.add(renameBtn);
        JButton moveBtn = makeButton("Move (Ctrl+M)");
        moveBtn.addActionListener(e -> safeMove());
        actionPanel.add(moveBtn);
        JButton deleteBtn = makeButton("Delete");
        deleteBtn.addActionListener(e -> confirmDelete());
        actionPanel.add(deleteBtn);
        p.add(actionPanel);

        return p;
    }

    private JPanel buildStatusBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ACCENT));
        statusLabel = new JLabel(" Ready");
        statusLabel.setForeground(TEXT);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        p.add(statusLabel, BorderLayout.WEST);
        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(200, 16));
        progressBar.setBackground(BG);
        progressBar.setForeground(ACCENT);
        p.add(progressBar, BorderLayout.EAST);
        return p;
    }

    private void bindKeys() {
        KeyboardFocusManager km = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        km.addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED && (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_R -> { safeRename(); return true; }
                    case KeyEvent.VK_M -> { safeMove(); return true; }
                    case KeyEvent.VK_T -> { addTagToSelected(); return true; }
                    case KeyEvent.VK_F -> { searchField.requestFocus(); return true; }
                }
            }
            if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_F5) {
                refreshView();
                return true;
            }
            return false;
        });
    }

    private void switchMode(String mode) {
        activeMode = mode;
        switch (mode) {
            case "browse" -> loadDirectory(currentDir != null ? currentDir : new File(rootPath));
            case "search" -> performSearch();
            case "duplicates" -> findDuplicates();
            case "tags" -> showTaggedAssets();
            case "projects" -> showProjectAssets();
            case "unused" -> findUnusedAssets();
        }
        statusLabel.setText(" Mode: " + mode.substring(0, 1).toUpperCase() + mode.substring(1));
    }

    private void onTreeSelect() {
        TreePath path = dirTree.getSelectionPath();
        if (path == null) return;
        StringBuilder sb = new StringBuilder(rootPath);
        for (Object comp : path.getPath()) {
            String name = comp.toString();
            if (!name.equals(rootPath.substring(rootPath.lastIndexOf('/') + 1))) {
                sb.append('/').append(name);
            }
        }
        File dir = new File(sb.toString());
        if (dir.isDirectory()) {
            loadDirectory(dir);
        }
    }

    private void loadDirectory(File dir) {
        currentDir = dir;
        allAssets.clear();
        listModel.clear();
        previewLabel.setIcon(null);
        previewLabel.setText("Select an asset to preview");
        metaLabel.setText("No asset selected");

        File[] files = dir.listFiles();
        if (files == null) return;

        progressBar.setIndeterminate(true);
        SwingWorker<Void, AssetEntry> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                for (File f : files) {
                    if (f.isFile() && isSupported(f)) {
                        publish(new AssetEntry(f));
                    }
                }
                return null;
            }

            @Override
            protected void process(List<AssetEntry> chunks) {
                for (AssetEntry ae : chunks) {
                    allAssets.add(ae);
                    listModel.addElement(ae.displayName());
                }
                progressBar.setIndeterminate(false);
                statusLabel.setText(" " + allAssets.size() + " assets loaded from " + dir.getName());
            }
        };
        worker.execute();
    }

    private boolean isSupported(File f) {
        String n = f.getName().toLowerCase();
        return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")
            || n.endsWith(".svg") || n.endsWith(".gif") || n.endsWith(".bmp")
            || n.endsWith(".mp3") || n.endsWith(".wav") || n.endsWith(".ogg")
            || n.endsWith(".flac") || n.endsWith(".mp4") || n.endsWith(".webm")
            || n.endsWith(".avi") || n.endsWith(".mkv")
            || n.endsWith(".ttf") || n.endsWith(".otf") || n.endsWith(".woff")
            || n.endsWith(".json") || n.endsWith(".xml") || n.endsWith(".yaml")
            || n.endsWith(".tmx") || n.endsWith(".atlas") || n.endsWith(".pack");
    }

    private void onAssetSelect() {
        int idx = assetList.getSelectedIndex();
        if (idx < 0 || idx >= filteredAssets.size()) return;
        AssetEntry ae = filteredAssets.get(idx);
        showPreview(ae);
        showMetadata(ae);
    }

    private void showPreview(AssetEntry ae) {
        String name = ae.file.getName().toLowerCase();
        if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".gif") || name.endsWith(".bmp")) {
            try {
                ImageIcon icon = new ImageIcon(ae.file.getAbsolutePath());
                Image img = icon.getImage();
                int w = icon.getIconWidth();
                int h = icon.getIconHeight();
                if (w > 400 || h > 300) {
                    double scale = Math.min(400.0 / w, 300.0 / h);
                    img = img.getScaledInstance((int)(w * scale), (int)(h * scale), Image.SCALE_SMOOTH);
                }
                previewLabel.setIcon(new ImageIcon(img));
                previewLabel.setText("");
            } catch (Exception e) {
                previewLabel.setIcon(null);
                previewLabel.setText("Cannot load image preview");
            }
        } else if (name.endsWith(".svg")) {
            previewLabel.setIcon(null);
            try {
                String content = Files.readString(ae.file.toPath());
                String preview = content.length() > 500 ? content.substring(0, 500) + "..." : content;
                previewLabel.setText("<html><pre style='color:#e0e0e0;font-size:10px'>"
                    + escapeHtml(preview) + "</pre></html>");
            } catch (Exception e) {
                previewLabel.setText("Cannot read SVG");
            }
        } else if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".ogg")
                || name.endsWith(".flac")) {
            previewLabel.setIcon(null);
            previewLabel.setText("<html><body style='color:#e0e0e0;text-align:center'>"
                + "<b>Audio File</b><br>" + ae.file.getName() + "</body></html>");
        } else if (name.endsWith(".mp4") || name.endsWith(".webm")
                || name.endsWith(".avi") || name.endsWith(".mkv")) {
            previewLabel.setIcon(null);
            previewLabel.setText("<html><body style='color:#e0e0e0;text-align:center'>"
                + "<b>Video File</b><br>" + ae.file.getName() + "</body></html>");
        } else if (name.endsWith(".ttf") || name.endsWith(".otf") || name.endsWith(".woff")) {
            previewLabel.setIcon(null);
            previewLabel.setText("<html><body style='color:#e0e0e0;text-align:center'>"
                + "<b>Font File</b><br>AaBbCcDd 0123</body></html>");
        } else {
            previewLabel.setIcon(null);
            previewLabel.setText("<html><body style='color:#e0e0e0;text-align:center'>"
                + ae.file.getName() + "</body></html>");
        }
    }

    private void showMetadata(AssetEntry ae) {
        File f = ae.file;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String size = formatSize(f.length());
        String modified = sdf.format(new Date(f.lastModified()));
        String mime = guessMime(f.getName());
        String dims = getDimensions(f);
        Set<String> t = tags.getOrDefault(f.getAbsolutePath(), Collections.emptySet());
        String proj = projectMap.getOrDefault(f.getAbsolutePath(), "None");

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='color:#e0e0e0;font-family:monospace;font-size:12px'>");
        sb.append("<b>").append(escapeHtml(f.getName())).append("</b><br><br>");
        sb.append("Path: ").append(escapeHtml(f.getAbsolutePath())).append("<br>");
        sb.append("Size: ").append(size).append("<br>");
        sb.append("Modified: ").append(modified).append("<br>");
        sb.append("MIME: ").append(mime).append("<br>");
        if (dims != null) sb.append("Dimensions: ").append(dims).append("<br>");
        sb.append("Tags: ").append(t.isEmpty() ? "(none)" : String.join(", ", t)).append("<br>");
        sb.append("Project: ").append(proj).append("<br>");
        sb.append("</body></html>");
        metaLabel.setText(sb.toString());
    }

    private String getDimensions(File f) {
        String name = f.getName().toLowerCase();
        if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".gif") || name.endsWith(".bmp")) {
            try {
                ImageIcon ic = new ImageIcon(f.getAbsolutePath());
                return ic.getIconWidth() + "x" + ic.getIconHeight();
            } catch (Exception e) {
                return "unknown";
            }
        }
        return null;
    }

    private String guessMime(String name) {
        String n = name.toLowerCase();
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".gif")) return "image/gif";
        if (n.endsWith(".svg")) return "image/svg+xml";
        if (n.endsWith(".bmp")) return "image/bmp";
        if (n.endsWith(".mp3")) return "audio/mpeg";
        if (n.endsWith(".wav")) return "audio/wav";
        if (n.endsWith(".ogg")) return "audio/ogg";
        if (n.endsWith(".flac")) return "audio/flac";
        if (n.endsWith(".mp4")) return "video/mp4";
        if (n.endsWith(".webm")) return "video/webm";
        if (n.endsWith(".avi")) return "video/x-msvideo";
        if (n.endsWith(".mkv")) return "video/x-matroska";
        if (n.endsWith(".ttf")) return "font/ttf";
        if (n.endsWith(".otf")) return "font/otf";
        if (n.endsWith(".woff")) return "font/woff";
        if (n.endsWith(".json")) return "application/json";
        if (n.endsWith(".xml")) return "application/xml";
        if (n.endsWith(".yaml")) return "text/yaml";
        return "application/octet-stream";
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private void performSearch() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty() && activeMode.equals("search")) {
            loadDirectory(currentDir != null ? currentDir : new File(rootPath));
            return;
        }
        activeMode = "search";
        filteredAssets.clear();
        listModel.clear();

        for (AssetEntry ae : allAssets) {
            boolean match = ae.file.getName().toLowerCase().contains(query)
                || ae.file.getName().toLowerCase().endsWith("." + query)
                || tags.getOrDefault(ae.file.getAbsolutePath(), Collections.emptySet())
                    .stream().anyMatch(t -> t.toLowerCase().contains(query));
            if (match) {
                filteredAssets.add(ae);
                listModel.addElement(ae.displayName());
            }
        }
        statusLabel.setText(" Search: " + filteredAssets.size() + " results for \"" + query + "\"");
    }

    private void findDuplicates() {
        filteredAssets.clear();
        listModel.clear();
        statusLabel.setText(" Scanning for duplicates...");

        SwingWorker<List<AssetEntry>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<AssetEntry> doInBackground() {
                Map<Long, List<AssetEntry>> sizeMap = new HashMap<>();
                for (AssetEntry ae : allAssets) {
                    sizeMap.computeIfAbsent(ae.file.length(), k -> new ArrayList<>()).add(ae);
                }
                List<AssetEntry> dupes = new ArrayList<>();
                for (List<AssetEntry> group : sizeMap.values()) {
                    if (group.size() < 2) continue;
                    Map<String, List<AssetEntry>> hashMap = new HashMap<>();
                    for (AssetEntry ae : group) {
                        String hash = hashFirstK(ae.file, 1024);
                        hashMap.computeIfAbsent(hash, k -> new ArrayList<>()).add(ae);
                    }
                    for (List<AssetEntry> hg : hashMap.values()) {
                        if (hg.size() >= 2) dupes.addAll(hg);
                    }
                }
                return dupes;
            }

            @Override
            protected void done() {
                try {
                    filteredAssets.addAll(get());
                    for (AssetEntry ae : filteredAssets) {
                        listModel.addElement("[DUP] " + ae.displayName());
                    }
                    statusLabel.setText(" Found " + filteredAssets.size() + " duplicate files");
                } catch (Exception e) {
                    statusLabel.setText(" Error finding duplicates");
                }
            }
        };
        worker.execute();
    }

    private String hashFirstK(File f, int k) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (InputStream is = new FileInputStream(f)) {
                byte[] buf = new byte[k];
                int read = is.read(buf);
                if (read > 0) md.update(buf, 0, read);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "error";
        }
    }

    private void showTaggedAssets() {
        String raw = JOptionPane.showInputDialog(this, "Enter tag to filter:", "Filter by Tag",
            JOptionPane.QUESTION_MESSAGE);
        if (raw == null || raw.trim().isEmpty()) return;
        final String tag = raw.trim().toLowerCase();
        filteredAssets.clear();
        listModel.clear();
        for (AssetEntry ae : allAssets) {
            Set<String> t = tags.getOrDefault(ae.file.getAbsolutePath(), Collections.emptySet());
            if (t.stream().anyMatch(x -> x.toLowerCase().contains(tag))) {
                filteredAssets.add(ae);
                listModel.addElement("[#" + tag + "] " + ae.displayName());
            }
        }
        statusLabel.setText(" Tag filter: " + filteredAssets.size() + " assets tagged with #" + tag);
    }

    private void showProjectAssets() {
        String raw = JOptionPane.showInputDialog(this, "Enter project name:", "Filter by Project",
            JOptionPane.QUESTION_MESSAGE);
        if (raw == null || raw.trim().isEmpty()) return;
        final String proj = raw.trim();
        filteredAssets.clear();
        listModel.clear();
        for (AssetEntry ae : allAssets) {
            String p = projectMap.getOrDefault(ae.file.getAbsolutePath(), "");
            if (p.equalsIgnoreCase(proj)) {
                filteredAssets.add(ae);
                listModel.addElement("[" + proj + "] " + ae.displayName());
            }
        }
        statusLabel.setText(" Project filter: " + filteredAssets.size() + " assets in " + proj);
    }

    private void findUnusedAssets() {
        filteredAssets.clear();
        listModel.clear();
        statusLabel.setText(" Scanning for unused assets...");

        SwingWorker<List<AssetEntry>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<AssetEntry> doInBackground() {
                List<String> sourceFiles = new ArrayList<>();
                collectSourceFiles(new File(rootPath), sourceFiles);
                Set<String> referenced = new HashSet<>();
                for (String sf : sourceFiles) {
                    try {
                        String content = Files.readString(Path.of(sf));
                        for (AssetEntry ae : allAssets) {
                            if (content.contains(ae.file.getName())) {
                                referenced.add(ae.file.getAbsolutePath());
                            }
                        }
                    } catch (Exception ignored) {}
                }
                List<AssetEntry> unused = new ArrayList<>();
                for (AssetEntry ae : allAssets) {
                    if (!referenced.contains(ae.file.getAbsolutePath())) {
                        unused.add(ae);
                    }
                }
                return unused;
            }

            private void collectSourceFiles(File dir, List<String> acc) {
                File[] files = dir.listFiles();
                if (files == null) return;
                for (File f : files) {
                    if (f.isDirectory() && !f.getName().equals("build") && !f.getName().equals(".git")) {
                        collectSourceFiles(f, acc);
                    } else if (f.getName().endsWith(".java") || f.getName().endsWith(".kt")
                            || f.getName().endsWith(".xml") || f.getName().endsWith(".gradle")) {
                        acc.add(f.getAbsolutePath());
                    }
                }
            }

            @Override
            protected void done() {
                try {
                    filteredAssets.addAll(get());
                    for (AssetEntry ae : filteredAssets) {
                        listModel.addElement("[UNUSED] " + ae.displayName());
                    }
                    statusLabel.setText(" Found " + filteredAssets.size() + " potentially unused assets");
                } catch (Exception e) {
                    statusLabel.setText(" Error scanning for unused assets");
                }
            }
        };
        worker.execute();
    }

    private void addTagToSelected() {
        int idx = assetList.getSelectedIndex();
        if (idx < 0 || idx >= filteredAssets.size()) {
            JOptionPane.showMessageDialog(this, "Select an asset first.", "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        AssetEntry ae = filteredAssets.get(idx);
        String input = JOptionPane.showInputDialog(this, "Add tag(s) (comma-separated):",
            "Tag Asset", JOptionPane.PLAIN_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;
        String[] newTags = input.split(",");
        Set<String> existing = tags.computeIfAbsent(ae.file.getAbsolutePath(), k -> new HashSet<>());
        for (String t : newTags) {
            existing.add(t.trim().toLowerCase());
        }
        saveConfig();
        showMetadata(ae);
        statusLabel.setText(" Added tags to " + ae.file.getName());
    }

    private void safeRename() {
        int idx = assetList.getSelectedIndex();
        if (idx < 0 || idx >= filteredAssets.size()) {
            JOptionPane.showMessageDialog(this, "Select an asset first.", "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        AssetEntry ae = filteredAssets.get(idx);
        String currentName = ae.file.getName();
        String newName = (String) JOptionPane.showInputDialog(this,
            "Rename asset:", "Safe Rename",
            JOptionPane.PLAIN_MESSAGE, null, null, currentName);
        if (newName == null || newName.equals(currentName)) return;

        File newFile = new File(ae.file.getParent(), newName);
        if (newFile.exists()) {
            JOptionPane.showMessageDialog(this, "A file with that name already exists!",
                "Rename Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String msg = "Preview of rename:\n\n"
            + "FROM: " + ae.file.getAbsolutePath() + "\n"
            + "  TO: " + newFile.getAbsolutePath() + "\n\n"
            + "Tags will be transferred.\n"
            + "This operation cannot be undone automatically.\n\n"
            + "Proceed?";
        int confirm = JOptionPane.showConfirmDialog(this, msg, "Confirm Rename",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        Set<String> t = tags.remove(ae.file.getAbsolutePath());
        if (ae.file.renameTo(newFile)) {
            if (t != null) tags.put(newFile.getAbsolutePath(), t);
            saveConfig();
            refreshView();
            statusLabel.setText(" Renamed: " + currentName + " -> " + newName);
        } else {
            JOptionPane.showMessageDialog(this, "Rename failed. Check permissions.",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void safeMove() {
        int idx = assetList.getSelectedIndex();
        if (idx < 0 || idx >= filteredAssets.size()) {
            JOptionPane.showMessageDialog(this, "Select an asset first.", "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        AssetEntry ae = filteredAssets.get(idx);
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select destination directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (currentDir != null) chooser.setCurrentDirectory(currentDir);
        int result = chooser.showDialog(this, "Move Here");
        if (result != JFileChooser.APPROVE_OPTION) return;

        File destDir = chooser.getSelectedFile();
        File newFile = new File(destDir, ae.file.getName());
        if (newFile.exists()) {
            JOptionPane.showMessageDialog(this, "A file with that name already exists in the destination!",
                "Move Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String msg = "Preview of move:\n\n"
            + "FROM: " + ae.file.getAbsolutePath() + "\n"
            + "  TO: " + newFile.getAbsolutePath() + "\n\n"
            + "Tags will be transferred.\n"
            + "This operation cannot be undone automatically.\n\n"
            + "Proceed?";
        int confirm = JOptionPane.showConfirmDialog(this, msg, "Confirm Move",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            Set<String> t = tags.remove(ae.file.getAbsolutePath());
            Files.move(ae.file.toPath(), newFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
            if (t != null) tags.put(newFile.getAbsolutePath(), t);
            saveConfig();
            refreshView();
            statusLabel.setText(" Moved: " + ae.file.getName() + " -> " + destDir.getName());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Move failed: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void confirmDelete() {
        int idx = assetList.getSelectedIndex();
        if (idx < 0 || idx >= filteredAssets.size()) {
            JOptionPane.showMessageDialog(this, "Select an asset first.", "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        AssetEntry ae = filteredAssets.get(idx);
        String msg = "Are you sure you want to delete?\n\n"
            + ae.file.getAbsolutePath() + "\n\n"
            + "This will PERMANENTLY remove the file.\n"
            + "This operation cannot be undone automatically.";
        int confirm = JOptionPane.showConfirmDialog(this, msg, "Confirm Delete",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        int doubleCheck = JOptionPane.showConfirmDialog(this,
            "Final confirmation: Delete " + ae.file.getName() + "?",
            "Double Check", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
        if (doubleCheck != JOptionPane.YES_OPTION) return;

        if (ae.file.delete()) {
            tags.remove(ae.file.getAbsolutePath());
            saveConfig();
            refreshView();
            statusLabel.setText(" Deleted: " + ae.file.getName());
        } else {
            JOptionPane.showMessageDialog(this, "Delete failed. Check permissions.",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshView() {
        loadDirectory(currentDir != null ? currentDir : new File(rootPath));
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
            "AssetForge - Project Asset Manager\n\n"
            + "A single-file Java Swing application for managing\n"
            + "project assets including images, audio, video,\n"
            + "fonts, icons, and game assets.\n\n"
            + "Features:\n"
            + "- File browser with directory tree\n"
            + "- Image preview and metadata display\n"
            + "- Duplicate detection\n"
            + "- Search, tagging, and project assignment\n"
            + "- Safe rename and move operations\n"
            + "- Unused asset reporting\n\n"
            + "Dark theme with keyboard shortcuts.\n"
            + "Built with javax.swing only.",
            "About AssetForge", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showSettings() {
        JFrame frame = new JFrame("AssetForge Settings");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(450, 200);
        frame.getContentPane().setBackground(BG);
        frame.setLayout(new BorderLayout(8, 8));

        JPanel center = new JPanel(new GridLayout(2, 2, 8, 8));
        center.setBackground(BG);
        center.setBorder(new EmptyBorder(16, 16, 8, 16));

        JLabel lbl = new JLabel("Default Scan Directory:");
        lbl.setForeground(TEXT);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        center.add(lbl);

        JTextField pathField = new JTextField(rootPath);
        pathField.setBackground(CARD_BG);
        pathField.setForeground(TEXT);
        pathField.setCaretColor(TEXT);
        pathField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT), new EmptyBorder(4, 6, 4, 6)));
        center.add(pathField);

        JLabel lbl2 = new JLabel("Total Assets:");
        lbl2.setForeground(TEXT);
        lbl2.setFont(new Font("SansSerif", Font.BOLD, 13));
        center.add(lbl2);
        center.add(new JLabel(String.valueOf(allAssets.size())) {{ setForeground(TEXT); }});

        frame.add(center, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(BG);
        JButton okBtn = makeButton("Save");
        okBtn.addActionListener(e -> {
            rootPath = pathField.getText().trim();
            dirTree.setModel(buildTreeModel());
            styleTree();
            saveConfig();
            frame.dispose();
            refreshView();
            statusLabel.setText(" Settings saved. Root: " + rootPath);
        });
        btnPanel.add(okBtn);
        JButton cancelBtn = makeButton("Cancel");
        cancelBtn.addActionListener(e -> frame.dispose());
        btnPanel.add(cancelBtn);
        frame.add(btnPanel, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(this);
        frame.setVisible(true);
    }

    class AssetEntry {
        final File file;
        private final String displayName;

        AssetEntry(File file) {
            this.file = file;
            this.displayName = file.getName();
        }

        String displayName() {
            return displayName;
        }
    }

    class AssetListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);
            label.setFont(new Font("Monospaced", Font.PLAIN, 13));
            if (!isSelected) {
                label.setBackground(BG);
                label.setForeground(TEXT);
            } else {
                label.setBackground(SELECTION_BG);
                label.setForeground(Color.WHITE);
            }
            label.setOpaque(true);
            label.setBorder(new EmptyBorder(4, 8, 4, 8));

            if (index >= 0 && index < filteredAssets.size()) {
                AssetEntry ae = filteredAssets.get(index);
                String name = ae.file.getName().toLowerCase();
                String prefix = "";
                if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                        || name.endsWith(".gif") || name.endsWith(".bmp")) prefix = "IMG ";
                else if (name.endsWith(".svg")) prefix = "SVG ";
                else if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".ogg")
                        || name.endsWith(".flac")) prefix = "AUD ";
                else if (name.endsWith(".mp4") || name.endsWith(".webm")
                        || name.endsWith(".avi") || name.endsWith(".mkv")) prefix = "VID ";
                else if (name.endsWith(".ttf") || name.endsWith(".otf") || name.endsWith(".woff"))
                    prefix = "FNT ";
                else prefix = "FILE";

                String orig = value.toString();
                if (!orig.startsWith("[")) {
                    label.setText("[" + prefix + "] " + orig + "  (" + formatSize(ae.file.length()) + ")");
                }
            }
            return label;
        }
    }
}
