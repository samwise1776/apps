package apps.filepilot;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.text.*;
import java.util.*;
import java.util.List;
import java.util.stream.*;

public class App extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final Color BG = new Color(0x1a, 0x1a, 0x2e);
    private static final Color ACCENT = new Color(0x0f, 0x34, 0x60);
    private static final Color TEXT = new Color(0xe0, 0xe0, 0xe0);
    private static final Color BTN = new Color(0x16, 0x21, 0x3e);
    private static final Color TABLE_BG = new Color(0x12, 0x12, 0x24);
    private static final Color TABLE_SEL = new Color(0x0f, 0x34, 0x60);
    private static final Color TABLE_GRID = new Color(0x2a, 0x2a, 0x4e);
    private static final Color BORDER_C = new Color(0x3a, 0x3a, 0x5e);
    private static final Color MUTED = new Color(0x88, 0x88, 0xaa);
    private static final Color LINK = new Color(0xaa, 0xaa, 0xcc);

    private JTree dirTree;
    private JTable fileTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JLabel sizeLabel;
    private JTextField breadcrumbField;
    private JTextField searchField;
    private CardLayout detailsCard;
    private JPanel detailsCardPanel;
    private JTextArea checksumArea;
    private JTextArea duplicatesArea;
    private JTextArea organizeArea;
    private JPanel bookmarksListPanel;

    private File currentDir;
    private File[] currentFiles;
    private final List<String> bookmarks = new ArrayList<>();
    private final List<String> recentFiles = new ArrayList<>();
    private final Map<String, JCheckBox> extensionFilters = new LinkedHashMap<>();
    private final List<FileRenameEntry> pendingRenames = new ArrayList<>();

    public App() {
        super("FilePilot");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1400, 800);
        setMinimumSize(new Dimension(1000, 600));
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        JToolBar toolbar = createToolbar();
        add(toolbar, BorderLayout.NORTH);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerSize(4);
        mainSplit.setBorder(null);

        JPanel treePanel = createTreePanel();
        treePanel.setPreferredSize(new Dimension(240, 0));
        mainSplit.setLeftComponent(treePanel);

        JPanel centerPanel = createCenterPanel();
        mainSplit.setRightComponent(centerPanel);

        detailsCard = new CardLayout();
        detailsCardPanel = new JPanel(detailsCard);
        detailsCardPanel.setBackground(BG);
        detailsCardPanel.add(createDefaultDetailsPanel(), "default");
        detailsCardPanel.add(createChecksumPanel(), "checksums");
        detailsCardPanel.add(createDuplicatesPanel(), "duplicates");
        detailsCardPanel.add(createOrganizePanel(), "organize");
        detailsCardPanel.add(createBookmarksPanel(), "bookmarks");
        detailsCardPanel.setPreferredSize(new Dimension(280, 0));

        JSplitPane rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainSplit, detailsCardPanel);
        rightSplit.setDividerSize(4);
        rightSplit.setBorder(null);
        rightSplit.setResizeWeight(0.85);

        add(rightSplit, BorderLayout.CENTER);

        JPanel statusBar = createStatusBar();
        add(statusBar, BorderLayout.SOUTH);

        setupKeyboardShortcuts();
        navigateTo(new File(System.getProperty("user.home")));
    }

    private void styleButton(JButton btn) {
        btn.setBackground(BTN);
        btn.setForeground(TEXT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_C, 1),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(ACCENT); }
            public void mouseExited(MouseEvent e) { btn.setBackground(BTN); }
        });
    }

    private JToolBar createToolbar() {
        JToolBar bar = new JToolBar();
        bar.setBackground(BG);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_C));
        bar.setFloatable(false);

        String[] labels = {"Navigate", "Search", "Duplicates", "Rename", "Organize", "Checksums", "Bookmarks", "About"};
        String[] actions = {"nav", "search", "dup", "rename", "organize", "checksum", "bookmarks", "about"};

        for (int i = 0; i < labels.length; i++) {
            JButton btn = new JButton(labels[i]);
            styleButton(btn);
            final String action = actions[i];
            btn.addActionListener(e -> handleToolbarAction(action));
            bar.add(btn);
            if (i < labels.length - 1) bar.add(Box.createHorizontalStrut(4));
        }

        bar.add(Box.createHorizontalGlue());

        searchField = new JTextField(20);
        searchField.setBackground(TABLE_BG);
        searchField.setForeground(TEXT);
        searchField.setCaretColor(TEXT);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_C),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        searchField.setToolTipText("Search files (Ctrl+F)");
        searchField.addActionListener(e -> performSearch());
        bar.add(searchField);
        bar.add(Box.createHorizontalStrut(8));

        JButton refreshBtn = new JButton("Refresh");
        styleButton(refreshBtn);
        refreshBtn.addActionListener(e -> refreshCurrentDir());
        bar.add(refreshBtn);

        return bar;
    }

    private void handleToolbarAction(String action) {
        switch (action) {
            case "nav":
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setDialogTitle("Navigate to Directory");
                if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    navigateTo(chooser.getSelectedFile());
                }
                break;
            case "search":
                searchField.requestFocusInWindow();
                break;
            case "dup":
                showDuplicatesDialog();
                break;
            case "rename":
                showBatchRenameDialog();
                break;
            case "organize":
                showOrganizePreview();
                break;
            case "checksum":
                generateChecksums();
                break;
            case "bookmarks":
                detailsCard.show(detailsCardPanel, "bookmarks");
                break;
            case "about":
                showAboutDialog();
                break;
        }
    }

    private JPanel createTreePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_C));

        JLabel title = new JLabel("  Folders");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
        title.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        panel.add(title, BorderLayout.NORTH);

        DefaultMutableTreeNode rootNode = buildFileSystemTree(File.listRoots()[0]);
        dirTree = new JTree(rootNode);
        dirTree.setBackground(TABLE_BG);
        dirTree.setForeground(TEXT);
        dirTree.setCellRenderer(new DarkTreeCellRenderer());
        dirTree.addTreeSelectionListener(e -> {
            TreePath path = dirTree.getSelectionPath();
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObj = node.getUserObject();
                if (userObj instanceof File) {
                    File dir = (File) userObj;
                    if (dir.isDirectory()) {
                        navigateTo(dir);
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(dirTree);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(TABLE_BG);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private DefaultMutableTreeNode buildFileSystemTree(File root) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(root);
        if (root.isDirectory()) {
            File[] children = root.listFiles(File::isDirectory);
            if (children != null) {
                Arrays.sort(children, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
                int count = 0;
                for (File child : children) {
                    if (count >= 50) break;
                    node.add(buildFileSystemTree(child));
                    count++;
                }
            }
        }
        return node;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(BG);

        JPanel breadcrumbBar = new JPanel(new BorderLayout(4, 0));
        breadcrumbBar.setBackground(BG);
        breadcrumbBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        breadcrumbField = new JTextField();
        breadcrumbField.setBackground(TABLE_BG);
        breadcrumbField.setForeground(TEXT);
        breadcrumbField.setCaretColor(TEXT);
        breadcrumbField.setFont(breadcrumbField.getFont().deriveFont(Font.PLAIN, 13f));
        breadcrumbField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_C),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        breadcrumbField.addActionListener(e -> {
            File target = new File(breadcrumbField.getText().trim());
            if (target.isDirectory()) navigateTo(target);
        });
        breadcrumbBar.add(breadcrumbField, BorderLayout.CENTER);

        topPanel.add(breadcrumbBar, BorderLayout.NORTH);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        filterPanel.setBackground(BG);
        filterPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_C));
        JLabel filterLabel = new JLabel("Filter:");
        filterLabel.setForeground(TEXT);
        filterPanel.add(filterLabel);
        for (String ext : new String[]{"java", "txt", "py", "js", "xml", "json", "md", "csv", "class", "jar", "sh"}) {
            JCheckBox cb = new JCheckBox(ext);
            cb.setForeground(TEXT);
            cb.setOpaque(false);
            cb.addActionListener(e -> applyExtensionFilter());
            extensionFilters.put(ext, cb);
            filterPanel.add(cb);
        }
        topPanel.add(filterPanel, BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.NORTH);

        String[] cols = {"Name", "Type", "Size", "Modified", "Permissions"};
        tableModel = new DefaultTableModel(cols, 0) {
            private static final long serialVersionUID = 1L;
            public boolean isCellEditable(int r, int c) { return false; }
            public Class<?> getColumnClass(int col) { return col == 2 ? Long.class : String.class; }
        };
        fileTable = new JTable(tableModel);
        styleTable(fileTable);

        fileTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateDetailsForSelection();
        });

        fileTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = fileTable.getSelectedRow();
                    if (row >= 0) {
                        String name = (String) tableModel.getValueAt(row, 0);
                        File target = new File(currentDir, name);
                        if (target.isDirectory()) {
                            navigateTo(target);
                        } else {
                            addRecentFile(target.getAbsolutePath());
                            statusLabel.setText("  Opened: " + target.getName());
                        }
                    }
                }
            }
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showContextMenu(e);
            }
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showContextMenu(e);
            }
        });

        fileTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        fileTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        fileTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        fileTable.getColumnModel().getColumn(3).setPreferredWidth(160);
        fileTable.getColumnModel().getColumn(4).setPreferredWidth(100);

        JScrollPane tableScroll = new JScrollPane(fileTable);
        tableScroll.setBorder(null);
        tableScroll.getViewport().setBackground(TABLE_BG);
        panel.add(tableScroll, BorderLayout.CENTER);

        return panel;
    }

    private void styleTable(JTable table) {
        table.setBackground(TABLE_BG);
        table.setForeground(TEXT);
        table.setSelectionBackground(TABLE_SEL);
        table.setSelectionForeground(TEXT);
        table.setGridColor(TABLE_GRID);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setFont(table.getFont().deriveFont(Font.PLAIN, 12f));
        table.setRowHeight(24);
        table.getTableHeader().setBackground(ACCENT);
        table.getTableHeader().setForeground(TEXT);
        table.getTableHeader().setFont(table.getFont().deriveFont(Font.BOLD));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_C));
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }

    private JPanel createDefaultDetailsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("File Details");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(10));

        String[] labels = {"Name", "Path", "Type", "Size", "Modified", "Readable", "Writable", "Hidden"};
        for (String label : labels) {
            JPanel row = new JPanel(new BorderLayout(4, 0));
            row.setBackground(BG);
            row.setMaximumSize(new Dimension(260, 24));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel lbl = new JLabel(label + ":");
            lbl.setForeground(MUTED);
            lbl.setFont(lbl.getFont().deriveFont(11f));
            lbl.setPreferredSize(new Dimension(80, 24));
            row.add(lbl, BorderLayout.WEST);
            JLabel val = new JLabel(" ");
            val.setForeground(TEXT);
            val.setFont(val.getFont().deriveFont(12f));
            val.setName("detail_" + label);
            row.add(val, BorderLayout.CENTER);
            panel.add(row);
            panel.add(Box.createVerticalStrut(2));
        }

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel createChecksumPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Checksums (Ctrl+K)");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(title, BorderLayout.NORTH);

        checksumArea = new JTextArea();
        checksumArea.setBackground(TABLE_BG);
        checksumArea.setForeground(TEXT);
        checksumArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        checksumArea.setEditable(false);
        checksumArea.setMargin(new Insets(8, 8, 8, 8));
        panel.add(new JScrollPane(checksumArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createDuplicatesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Duplicate Files (Ctrl+D)");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(title, BorderLayout.NORTH);

        duplicatesArea = new JTextArea();
        duplicatesArea.setBackground(TABLE_BG);
        duplicatesArea.setForeground(TEXT);
        duplicatesArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        duplicatesArea.setEditable(false);
        duplicatesArea.setMargin(new Insets(8, 8, 8, 8));
        panel.add(new JScrollPane(duplicatesArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createOrganizePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Organize Preview");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(title, BorderLayout.NORTH);

        organizeArea = new JTextArea();
        organizeArea.setBackground(TABLE_BG);
        organizeArea.setForeground(TEXT);
        organizeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        organizeArea.setEditable(false);
        organizeArea.setMargin(new Insets(8, 8, 8, 8));
        panel.add(new JScrollPane(organizeArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBookmarksPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG);
        JLabel title = new JLabel("Bookmarks (Ctrl+B)");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        header.add(title, BorderLayout.WEST);

        JButton addBtn = new JButton("+");
        styleButton(addBtn);
        addBtn.setMargin(new Insets(2, 8, 2, 8));
        addBtn.addActionListener(e -> {
            if (currentDir != null && !bookmarks.contains(currentDir.getAbsolutePath())) {
                bookmarks.add(currentDir.getAbsolutePath());
                refreshBookmarksList();
            }
        });
        header.add(addBtn, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        bookmarksListPanel = new JPanel();
        bookmarksListPanel.setLayout(new BoxLayout(bookmarksListPanel, BoxLayout.Y_AXIS));
        bookmarksListPanel.setBackground(BG);
        JScrollPane scroll = new JScrollPane(bookmarksListPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void refreshBookmarksList() {
        bookmarksListPanel.removeAll();
        for (String path : bookmarks) {
            JPanel row = new JPanel(new BorderLayout(4, 0));
            row.setBackground(BG);
            row.setMaximumSize(new Dimension(260, 30));
            row.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

            JLabel lbl = new JLabel(path);
            lbl.setForeground(TEXT);
            lbl.setFont(lbl.getFont().deriveFont(11f));
            lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lbl.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    File dir = new File(path);
                    if (dir.isDirectory()) navigateTo(dir);
                }
            });
            row.add(lbl, BorderLayout.CENTER);

            JButton removeBtn = new JButton("x");
            removeBtn.setForeground(new Color(0xff, 0x66, 0x66));
            removeBtn.setOpaque(false);
            removeBtn.setBorderPainted(false);
            removeBtn.setContentAreaFilled(false);
            removeBtn.setFont(removeBtn.getFont().deriveFont(11f));
            removeBtn.addActionListener(e -> {
                bookmarks.remove(path);
                refreshBookmarksList();
            });
            row.add(removeBtn, BorderLayout.EAST);
            bookmarksListPanel.add(row);
        }
        bookmarksListPanel.add(Box.createVerticalGlue());
        bookmarksListPanel.revalidate();
        bookmarksListPanel.repaint();
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(ACCENT);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_C));
        bar.setPreferredSize(new Dimension(0, 26));

        statusLabel = new JLabel("  Ready");
        statusLabel.setForeground(TEXT);
        statusLabel.setFont(statusLabel.getFont().deriveFont(11f));
        bar.add(statusLabel, BorderLayout.WEST);

        sizeLabel = new JLabel("  ");
        sizeLabel.setForeground(TEXT);
        sizeLabel.setFont(sizeLabel.getFont().deriveFont(11f));
        sizeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        bar.add(sizeLabel, BorderLayout.EAST);

        return bar;
    }

    private void setupKeyboardShortcuts() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK), "duplicates");
        am.put("duplicates", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            public void actionPerformed(ActionEvent e) { showDuplicatesDialog(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK), "rename");
        am.put("rename", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            public void actionPerformed(ActionEvent e) { showBatchRenameDialog(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK), "checksums");
        am.put("checksums", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            public void actionPerformed(ActionEvent e) { generateChecksums(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh");
        am.put("refresh", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            public void actionPerformed(ActionEvent e) { refreshCurrentDir(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK), "bookmark");
        am.put("bookmark", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            public void actionPerformed(ActionEvent e) {
                if (currentDir != null && !bookmarks.contains(currentDir.getAbsolutePath())) {
                    bookmarks.add(currentDir.getAbsolutePath());
                    refreshBookmarksList();
                    statusLabel.setText("  Bookmarked: " + currentDir.getAbsolutePath());
                }
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "find");
        am.put("find", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            public void actionPerformed(ActionEvent e) { searchField.requestFocusInWindow(); }
        });
    }

    private void navigateTo(File dir) {
        if (!dir.isDirectory()) return;
        currentDir = dir;
        breadcrumbField.setText(dir.getAbsolutePath());
        refreshCurrentDir();
        expandTreeNode(dir);
    }

    private void expandTreeNode(File target) {
        TreePath path = findTreePath(dirTree.getModel().getRoot(), target);
        if (path != null) {
            dirTree.setSelectionPath(path);
            dirTree.scrollPathToVisible(path);
        }
    }

    private TreePath findTreePath(Object node, File target) {
        DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) node;
        Object userObj = dmtn.getUserObject();
        if (userObj instanceof File) {
            File f = (File) userObj;
            if (f.equals(target)) return new TreePath(dmtn.getPath());
        }
        for (int i = 0; i < dmtn.getChildCount(); i++) {
            TreePath result = findTreePath(dmtn.getChildAt(i), target);
            if (result != null) return result;
        }
        return null;
    }

    private void refreshCurrentDir() {
        if (currentDir == null || !currentDir.isDirectory()) return;
        File[] files = currentDir.listFiles();
        if (files == null) files = new File[0];
        currentFiles = files;
        Arrays.sort(files, (a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });
        tableModel.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        long totalSize = 0;
        int fileCount = 0;
        int folderCount = 0;
        for (File f : files) {
            String ext = getExtension(f.getName());
            boolean anyFilterChecked = extensionFilters.values().stream().anyMatch(AbstractButton::isSelected);
            if (anyFilterChecked && f.isFile() && !ext.isEmpty()) {
                JCheckBox cb = extensionFilters.get(ext);
                if (cb == null || !cb.isSelected()) continue;
            }
            String type = f.isDirectory() ? "Folder" : (ext.isEmpty() ? "File" : ext.toUpperCase());
            String sizeStr = f.isDirectory() ? "[" + formatSize(calcFolderSize(f)) + "]" : formatSize(f.length());
            long sizeBytes = f.isDirectory() ? calcFolderSize(f) : f.length();
            String mod = sdf.format(new Date(f.lastModified()));
            String perms = getPermissions(f);
            tableModel.addRow(new Object[]{f.getName(), type, sizeBytes, mod, perms});
            totalSize += sizeBytes;
            if (f.isDirectory()) folderCount++; else fileCount++;
        }
        statusLabel.setText(String.format("  %d folders, %d files", folderCount, fileCount));
        sizeLabel.setText("  " + formatSize(totalSize) + "  ");
        fileTable.revalidate();
        fileTable.repaint();
    }

    private long calcFolderSize(File dir) {
        long size = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        for (File f : files) {
            if (f.isFile()) size += f.length();
            else if (f.isDirectory()) size += calcFolderSize(f);
        }
        return size;
    }

    private void applyExtensionFilter() {
        refreshCurrentDir();
    }

    private void updateDetailsForSelection() {
        int[] rows = fileTable.getSelectedRows();
        if (rows.length == 0) {
            for (String label : new String[]{"Name", "Path", "Type", "Size", "Modified", "Readable", "Writable", "Hidden"}) {
                setDetailValue(label, " ");
            }
            sizeLabel.setText("  ");
            return;
        }
        if (rows.length == 1) {
            String name = (String) tableModel.getValueAt(rows[0], 0);
            File f = new File(currentDir, name);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            setDetailValue("Name", f.getName());
            setDetailValue("Path", f.getAbsolutePath());
            setDetailValue("Type", f.isDirectory() ? "Directory" : getExtension(f.getName()).toUpperCase());
            setDetailValue("Size", formatSize(f.length()));
            setDetailValue("Modified", sdf.format(new Date(f.lastModified())));
            setDetailValue("Readable", String.valueOf(f.canRead()));
            setDetailValue("Writable", String.valueOf(f.canWrite()));
            setDetailValue("Hidden", String.valueOf(f.isHidden()));
        } else {
            long total = 0;
            for (int r : rows) {
                String name = (String) tableModel.getValueAt(r, 0);
                File f = new File(currentDir, name);
                total += f.isDirectory() ? calcFolderSize(f) : f.length();
            }
            setDetailValue("Name", rows.length + " items selected");
            setDetailValue("Path", "");
            setDetailValue("Type", "");
            setDetailValue("Size", formatSize(total));
            setDetailValue("Modified", "");
            setDetailValue("Readable", "");
            setDetailValue("Writable", "");
            setDetailValue("Hidden", "");
            sizeLabel.setText("  Selected: " + formatSize(total) + "  ");
        }
        detailsCard.show(detailsCardPanel, "default");
    }

    private void setDetailValue(String label, String value) {
        Component root = detailsCardPanel.getComponent(0);
        if (root instanceof Container) {
            for (Component c : ((Container) root).getComponents()) {
                if (c instanceof JPanel) {
                    for (Component inner : ((JPanel) c).getComponents()) {
                        if (inner instanceof JLabel) {
                            JLabel jLabel = (JLabel) inner;
                            if (("detail_" + label).equals(jLabel.getName())) {
                                jLabel.setText(value);
                            }
                        }
                    }
                }
            }
        }
    }

    private String getExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot + 1).toLowerCase() : "";
    }

    private String formatSize(long bytes) {
        if (bytes < 0) return "? B";
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.1f MB", mb);
        double gb = mb / 1024.0;
        return String.format("%.2f GB", gb);
    }

    private String getPermissions(File f) {
        StringBuilder sb = new StringBuilder();
        sb.append(f.canRead() ? 'r' : '-');
        sb.append(f.canWrite() ? 'w' : '-');
        sb.append(f.isDirectory() ? 'd' : '-');
        return sb.toString();
    }

    private void performSearch() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty() || currentDir == null) {
            refreshCurrentDir();
            return;
        }
        File[] files = currentDir.listFiles();
        if (files == null) files = new File[0];
        tableModel.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        int count = 0;
        for (File f : files) {
            if (f.getName().toLowerCase().contains(query)) {
                String ext = getExtension(f.getName());
                String type = f.isDirectory() ? "Folder" : (ext.isEmpty() ? "File" : ext.toUpperCase());
                long size = f.isDirectory() ? calcFolderSize(f) : f.length();
                tableModel.addRow(new Object[]{
                    f.getName(), type, size,
                    sdf.format(new Date(f.lastModified())),
                    getPermissions(f)
                });
                count++;
            }
        }
        statusLabel.setText("  Search: " + count + " matches for \"" + query + "\"");
        fileTable.revalidate();
    }

    private void showContextMenu(MouseEvent e) {
        int row = fileTable.rowAtPoint(e.getPoint());
        if (row < 0) return;
        fileTable.setRowSelectionInterval(row, row);

        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(BG);
        menu.setBorder(BorderFactory.createLineBorder(BORDER_C));

        String name = (String) tableModel.getValueAt(row, 0);
        File file = new File(currentDir, name);

        JMenuItem open = new JMenuItem("Open");
        open.setForeground(TEXT);
        open.addActionListener(ev -> {
            if (file.isDirectory()) navigateTo(file);
            else addRecentFile(file.getAbsolutePath());
        });
        menu.add(open);

        JMenuItem info = new JMenuItem("Properties");
        info.setForeground(TEXT);
        info.addActionListener(ev -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String msg = "Name: " + file.getName()
                + "\nPath: " + file.getAbsolutePath()
                + "\nSize: " + formatSize(file.length())
                + "\nModified: " + sdf.format(new Date(file.lastModified()))
                + "\nReadable: " + file.canRead()
                + "\nWritable: " + file.canWrite();
            JOptionPane.showMessageDialog(this, msg, "Properties", JOptionPane.INFORMATION_MESSAGE);
        });
        menu.add(info);

        JMenuItem checksumItem = new JMenuItem("Get Checksum");
        checksumItem.setForeground(TEXT);
        checksumItem.addActionListener(ev -> {
            if (file.isFile()) {
                try {
                    String sha = computeSHA256(file);
                    String md5 = computeMD5(file);
                    JOptionPane.showMessageDialog(this,
                        "SHA-256: " + sha + "\n\nMD5: " + md5,
                        "Checksum - " + file.getName(),
                        JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            }
        });
        menu.add(checksumItem);

        if (file.isDirectory()) {
            JMenuItem folderSize = new JMenuItem("Calculate Folder Size");
            folderSize.setForeground(TEXT);
            folderSize.addActionListener(ev -> {
                JOptionPane.showMessageDialog(this,
                    file.getName() + ": " + formatSize(calcFolderSize(file)),
                    "Folder Size", JOptionPane.INFORMATION_MESSAGE);
            });
            menu.add(folderSize);
        }

        menu.addSeparator();

        JMenuItem bookmarkItem = new JMenuItem("Bookmark This Directory");
        bookmarkItem.setForeground(TEXT);
        bookmarkItem.addActionListener(ev -> {
            String path = file.isDirectory() ? file.getAbsolutePath() : file.getParent();
            if (path != null && !bookmarks.contains(path)) {
                bookmarks.add(path);
                refreshBookmarksList();
                statusLabel.setText("  Bookmarked: " + path);
            }
        });
        menu.add(bookmarkItem);

        menu.addSeparator();

        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.setForeground(new Color(0xff, 0x66, 0x66));
        deleteItem.addActionListener(ev -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Delete " + file.getName() + "?\nThis cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                if (file.delete()) {
                    statusLabel.setText("  Deleted: " + file.getName());
                    refreshCurrentDir();
                } else {
                    statusLabel.setText("  Failed to delete: " + file.getName());
                }
            }
        });
        menu.add(deleteItem);

        menu.show(fileTable, e.getX(), e.getY());
    }

    private void showDuplicatesDialog() {
        if (currentDir == null) return;
        statusLabel.setText("  Scanning for duplicates...");
        SwingWorker<Map<String, List<File>>, Void> worker = new SwingWorker<>() {
            protected Map<String, List<File>> doInBackground() {
                return findDuplicates(currentDir);
            }
            protected void done() {
                try {
                    Map<String, List<File>> dups = get();
                    detailsCard.show(detailsCardPanel, "duplicates");
                    StringBuilder sb = new StringBuilder();
                    int groupCount = 0;
                    for (Map.Entry<String, List<File>> entry : dups.entrySet()) {
                        if (entry.getValue().size() > 1) {
                            groupCount++;
                            sb.append("--- Group ").append(groupCount)
                              .append(" (").append(formatSize(entry.getValue().get(0).length()))
                              .append(" each) ---\n");
                            for (File f : entry.getValue()) {
                                sb.append("  ").append(f.getAbsolutePath()).append("\n");
                            }
                            sb.append("\n");
                        }
                    }
                    if (groupCount == 0) sb.append("No duplicates found.");
                    else sb.insert(0, "Found " + groupCount + " duplicate group(s):\n\n");
                    duplicatesArea.setText(sb.toString());
                    duplicatesArea.setCaretPosition(0);
                    statusLabel.setText("  Duplicate scan complete: " + groupCount + " groups");
                } catch (Exception ex) {
                    statusLabel.setText("  Error scanning duplicates");
                }
            }
        };
        worker.execute();
    }

    private Map<String, List<File>> findDuplicates(File root) {
        Map<String, List<File>> map = new HashMap<>();
        findDuplicatesRecursive(root, map, 0);
        return map;
    }

    private void findDuplicatesRecursive(File dir, Map<String, List<File>> map, int depth) {
        if (depth > 10) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                findDuplicatesRecursive(f, map, depth + 1);
            } else if (f.isFile() && f.length() > 0) {
                try {
                    String hash = computeSHA256(f);
                    map.computeIfAbsent(hash, k -> new ArrayList<>()).add(f);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void showBatchRenameDialog() {
        if (currentFiles == null || currentFiles.length == 0) {
            JOptionPane.showMessageDialog(this, "No files in current directory.", "Batch Rename", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 4, 4));
        inputPanel.setBackground(BG);

        JLabel findLabel = new JLabel("Find:");
        findLabel.setForeground(TEXT);
        inputPanel.add(findLabel);
        JTextField findField = new JTextField();
        findField.setBackground(TABLE_BG);
        findField.setForeground(TEXT);
        findField.setCaretColor(TEXT);
        inputPanel.add(findField);

        JLabel replaceLabel = new JLabel("Replace with:");
        replaceLabel.setForeground(TEXT);
        inputPanel.add(replaceLabel);
        JTextField replaceField = new JTextField();
        replaceField.setBackground(TABLE_BG);
        replaceField.setForeground(TEXT);
        replaceField.setCaretColor(TEXT);
        inputPanel.add(replaceField);

        JLabel prefixLabel = new JLabel("Add prefix:");
        prefixLabel.setForeground(TEXT);
        inputPanel.add(prefixLabel);
        JTextField prefixField = new JTextField();
        prefixField.setBackground(TABLE_BG);
        prefixField.setForeground(TEXT);
        prefixField.setCaretColor(TEXT);
        inputPanel.add(prefixLabel);
        inputPanel.add(prefixField);

        panel.add(inputPanel, BorderLayout.NORTH);

        JTextArea preview = new JTextArea();
        preview.setBackground(TABLE_BG);
        preview.setForeground(TEXT);
        preview.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        preview.setEditable(false);
        panel.add(new JScrollPane(preview), BorderLayout.CENTER);

        JButton previewBtn = new JButton("Generate Preview");
        styleButton(previewBtn);
        previewBtn.addActionListener(ev -> {
            String find = findField.getText();
            String replace = replaceField.getText();
            String prefix = prefixField.getText();
            StringBuilder sb = new StringBuilder();
            int count = 0;
            pendingRenames.clear();
            for (File f : currentFiles) {
                String oldName = f.getName();
                String newName = oldName;
                if (!find.isEmpty()) newName = newName.replace(find, replace);
                if (!prefix.isEmpty()) newName = prefix + newName;
                if (!oldName.equals(newName)) {
                    sb.append(String.format("  %-40s -> %s%n", oldName, newName));
                    pendingRenames.add(new FileRenameEntry(f, new File(f.getParent(), newName)));
                    count++;
                }
            }
            if (count == 0) sb.append("  No files would be renamed with current settings.");
            else sb.insert(0, "  " + count + " file(s) will be renamed:\n\n");
            preview.setText(sb.toString());
            preview.setCaretPosition(0);
        });
        panel.add(previewBtn, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(this, panel, "Batch Rename Preview", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION && !pendingRenames.isEmpty()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Rename " + pendingRenames.size() + " file(s)?",
                "Confirm Rename", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                int success = 0;
                for (FileRenameEntry entry : pendingRenames) {
                    if (entry.oldFile.renameTo(entry.newFile)) success++;
                }
                statusLabel.setText("  Renamed " + success + "/" + pendingRenames.size() + " files");
                pendingRenames.clear();
                refreshCurrentDir();
            }
        }
    }

    private void showOrganizePreview() {
        if (currentFiles == null || currentFiles.length == 0) {
            JOptionPane.showMessageDialog(this, "No files in current directory.", "Organize", JOptionPane.WARNING_MESSAGE);
            return;
        }
        detailsCard.show(detailsCardPanel, "organize");

        String[] options = {"By Extension", "By Date (Month)", "By Size"};
        int choice = JOptionPane.showOptionDialog(this, "Group files by:", "Organize Preview",
            JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice < 0) return;

        Map<String, List<File>> groups = new LinkedHashMap<>();
        SimpleDateFormat monthFmt = new SimpleDateFormat("yyyy-MM");

        for (File f : currentFiles) {
            if (f.isFile()) {
                String key;
                switch (choice) {
                    case 0:
                        key = getExtension(f.getName()).isEmpty() ? "no_extension" : getExtension(f.getName());
                        break;
                    case 1:
                        key = monthFmt.format(new Date(f.lastModified()));
                        break;
                    case 2:
                        long len = f.length();
                        if (len < 1024) key = "< 1 KB";
                        else if (len < 1024 * 1024) key = "1 KB - 1 MB";
                        else if (len < 1024 * 1024 * 1024L) key = "1 MB - 1 GB";
                        else key = "> 1 GB";
                        break;
                    default:
                        key = "other";
                }
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(f);
            }
        }

        StringBuilder sb = new StringBuilder("File Organization Preview (by " + options[choice].toLowerCase() + ")\n\n");
        for (Map.Entry<String, List<File>> entry : groups.entrySet()) {
            sb.append("[").append(entry.getKey()).append("] (").append(entry.getValue().size()).append(" files)\n");
            for (File f : entry.getValue()) {
                sb.append("  ").append(f.getName()).append("  (").append(formatSize(f.length())).append(")\n");
            }
            sb.append("\n");
        }
        sb.append("Would you like to move these files into subfolders?");
        organizeArea.setText(sb.toString());
        organizeArea.setCaretPosition(0);
        statusLabel.setText("  Organize preview: " + groups.size() + " groups");

        int confirm = JOptionPane.showConfirmDialog(this,
            "Create subfolders and move files?\n" + groups.size() + " groups identified.",
            "Confirm Organize", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            int moved = 0;
            for (Map.Entry<String, List<File>> entry : groups.entrySet()) {
                File subDir = new File(currentDir, entry.getKey());
                if (!subDir.exists()) subDir.mkdirs();
                for (File f : entry.getValue()) {
                    File target = new File(subDir, f.getName());
                    if (f.renameTo(target)) moved++;
                }
            }
            statusLabel.setText("  Moved " + moved + " files into subfolders");
            refreshCurrentDir();
        }
    }

    private void generateChecksums() {
        int[] rows = fileTable.getSelectedRows();
        if (rows.length == 0) {
            JOptionPane.showMessageDialog(this, "Select one or more files first.", "Checksums", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        detailsCard.show(detailsCardPanel, "checksums");
        StringBuilder sb = new StringBuilder();
        int processed = 0;
        for (int r : rows) {
            String name = (String) tableModel.getValueAt(r, 0);
            File f = new File(currentDir, name);
            if (f.isFile()) {
                try {
                    sb.append(f.getName()).append("\n");
                    sb.append("  SHA-256: ").append(computeSHA256(f)).append("\n");
                    sb.append("  MD5:     ").append(computeMD5(f)).append("\n");
                    sb.append("  Size:    ").append(formatSize(f.length())).append("\n\n");
                    processed++;
                } catch (Exception ex) {
                    sb.append(f.getName()).append(" - Error: ").append(ex.getMessage()).append("\n\n");
                }
            }
        }
        if (processed == 0) sb.append("No files selected.");
        else sb.insert(0, "Checksums for " + processed + " file(s):\n\n");
        checksumArea.setText(sb.toString());
        checksumArea.setCaretPosition(0);
        statusLabel.setText("  Generated checksums for " + processed + " file(s)");
    }

    private String computeSHA256(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }
        return bytesToHex(md.digest());
    }

    private String computeMD5(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }
        return bytesToHex(md.digest());
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void addRecentFile(String path) {
        recentFiles.remove(path);
        recentFiles.add(0, path);
        if (recentFiles.size() > 50) recentFiles.remove(recentFiles.size() - 1);
    }

    private void showAboutDialog() {
        JPanel panel = new JPanel();
        panel.setBackground(BG);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel name = new JLabel("FilePilot");
        name.setForeground(TEXT);
        name.setFont(name.getFont().deriveFont(Font.BOLD, 28f));
        name.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(name);

        panel.add(Box.createVerticalStrut(6));
        JLabel ver = new JLabel("Version 1.0");
        ver.setForeground(MUTED);
        ver.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(ver);

        panel.add(Box.createVerticalStrut(4));
        JLabel subtitle = new JLabel("Developer File Utility");
        subtitle.setForeground(LINK);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(subtitle);

        panel.add(Box.createVerticalStrut(20));

        String features = "<html><div style='text-align:center'>"
            + "<b>Features</b><br>"
            + "File browsing with directory tree<br>"
            + "Breadcrumb navigation<br>"
            + "Advanced search by name<br>"
            + "Extension filter checkboxes<br>"
            + "Folder size calculation<br>"
            + "Duplicate finder (SHA-256)<br>"
            + "Batch rename with preview<br>"
            + "File organization preview<br>"
            + "Checksum generator (SHA-256, MD5)<br>"
            + "Bookmarks &amp; Recent files<br>"
            + "</div></html>";
        JLabel featLabel = new JLabel(features);
        featLabel.setForeground(TEXT);
        featLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(featLabel);

        panel.add(Box.createVerticalStrut(16));

        String shortcuts = "<html><div style='text-align:center'>"
            + "<b>Keyboard Shortcuts</b><br>"
            + "<font color='#8888aa'>"
            + "Ctrl+D  Find Duplicates<br>"
            + "Ctrl+R  Batch Rename<br>"
            + "Ctrl+K  Checksums<br>"
            + "Ctrl+F  Search<br>"
            + "Ctrl+B  Bookmark<br>"
            + "F5      Refresh"
            + "</font></div></html>";
        JLabel scLabel = new JLabel(shortcuts);
        scLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(scLabel);

        panel.add(Box.createVerticalStrut(20));
        JLabel copyright = new JLabel("javax.swing only - No external dependencies");
        copyright.setForeground(MUTED);
        copyright.setFont(copyright.getFont().deriveFont(10f));
        copyright.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(copyright);

        JOptionPane.showMessageDialog(this, panel, "About FilePilot", JOptionPane.PLAIN_MESSAGE);
    }

    static class FileRenameEntry {
        final File oldFile;
        final File newFile;
        FileRenameEntry(File oldFile, File newFile) {
            this.oldFile = oldFile;
            this.newFile = newFile;
        }
    }

    static class DarkTreeCellRenderer extends DefaultTreeCellRenderer {
        private static final long serialVersionUID = 1L;

        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            setBackground(sel ? TABLE_SEL : TABLE_BG);
            setForeground(TEXT);
            setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            if (value instanceof DefaultMutableTreeNode) {
                Object userObj = ((DefaultMutableTreeNode) value).getUserObject();
                if (userObj instanceof File) {
                    File f = (File) userObj;
                    setText(f.getName().isEmpty() ? f.getAbsolutePath() : f.getName());
                }
            }
            return this;
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> {
            App app = new App();
            app.setVisible(true);
        });
    }
}
