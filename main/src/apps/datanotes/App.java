package apps.datanotes;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.swing.text.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.nio.file.*;
import java.text.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import java.util.stream.*;

@SuppressWarnings({"serial", "this-escape"})
public class App extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final Color BG = new Color(0x1a, 0x1a, 0x2e);
    private static final Color ACCENT = new Color(0x0f, 0x34, 0x60);
    private static final Color TEXT = new Color(0xe0, 0xe0, 0xe0);
    private static final Color BTN = new Color(0x16, 0x21, 0x3e);
    private static final Color BORDER_C = new Color(0x3a, 0x3a, 0x5e);
    private static final Color MUTED = new Color(0x88, 0x88, 0xaa);
    private static final Color LINK_C = new Color(0x6a, 0xaa, 0xff);
    private static final Color CODE_BG = new Color(0x0d, 0x0d, 0x1a);
    private static final Color TAG_BG = new Color(0x1a, 0x2a, 0x4a);
    private static final Color HIGHLIGHT = new Color(0x3a, 0x5a, 0x0a);

    private static final File HOME = new File(System.getProperty("user.home"));
    private static final File NOTES_DIR = new File(HOME, ".datadocs/notes");
    private static final File BACKUP_DIR = new File(HOME, ".datadocs/backups");
    private static final File APPS_CONFIG = new File(System.getProperty("user.dir"), "config/apps.json");

    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    private JTree notebookTree;
    private JTextPane editorPane;
    private JTextField searchField;
    private JLabel statusWordCount;
    private JLabel statusSaved;
    private JLabel statusNotebook;
    private JPanel tagsPanel;
    private JPanel projectsPanel;
    private JPanel metadataPanel;
    private JTextField tagInput;

    private javax.swing.Timer autoSaveTimer;
    private int autoSaveDelay = 2000;
    private int fontSize = 14;
    private NotebookData currentNotebook;
    private PageData currentPage;
    private final List<NotebookData> notebooks = new ArrayList<>();
    private final List<String> appList = new ArrayList<>();
    private boolean dirty = false;

    public App() {
        super("DataNotes");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1400, 850);
        setMinimumSize(new Dimension(1000, 600));
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        NOTES_DIR.mkdirs();
        BACKUP_DIR.mkdirs();

        loadAppList();
        loadAllNotebooks();
        setupAutoSave();

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

        JSplitPane contentSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        contentSplit.setDividerSize(4);
        contentSplit.setBorder(null);
        contentSplit.setLeftComponent(mainSplit);

        JPanel rightPanel = createRightPanel();
        rightPanel.setPreferredSize(new Dimension(260, 0));
        contentSplit.setRightComponent(rightPanel);
        contentSplit.setDividerLocation(1100);

        add(contentSplit, BorderLayout.CENTER);

        JPanel statusBar = createStatusBar();
        add(statusBar, BorderLayout.SOUTH);

        bindKeyboardShortcuts();
        setupEditorListener();
        refreshTree();

        if (!notebooks.isEmpty()) {
            selectFirstNotebook();
        }
    }

    private void loadAppList() {
        appList.clear();
        File configFile = new File(System.getProperty("user.dir"), "config/apps.json");
        if (configFile.exists()) {
            try {
                String content = new String(Files.readAllBytes(configFile.toPath()));
                Matcher m = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"").matcher(content);
                while (m.find()) {
                    appList.add(m.group(1));
                }
            } catch (IOException e) {
                appList.addAll(Arrays.asList(
                    "assetforge", "canvasally", "codeshelf", "datadocs", "datavault",
                    "desktopcraft", "devpulse", "filepilot", "helper", "javagpt",
                    "learner", "logscope", "packforge", "projecthub", "testbench",
                    "trestigo", "utilor", "videoforge"
                ));
            }
        } else {
            appList.addAll(Arrays.asList(
                "assetforge", "canvasally", "codeshelf", "datadocs", "datavault",
                "desktopcraft", "devpulse", "filepilot", "helper", "javagpt",
                "learner", "logscope", "packforge", "projecthub", "testbench",
                "trestigo", "utilor", "videoforge"
            ));
        }
    }

    private void setupAutoSave() {
        autoSaveTimer = new javax.swing.Timer(autoSaveDelay, e -> {
            if (dirty && currentPage != null) {
                saveCurrentPage();
                dirty = false;
            }
        });
        autoSaveTimer.setRepeats(false);
    }

    private void setupEditorListener() {
        editorPane.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { markDirty(); }
            public void removeUpdate(DocumentEvent e) { markDirty(); }
            public void changedUpdate(DocumentEvent e) { markDirty(); }
        });
    }

    private void markDirty() {
        dirty = true;
        autoSaveTimer.restart();
        updateWordCount();
    }

    private JToolBar createToolbar() {
        JToolBar tb = new JToolBar();
        tb.setBackground(BTN);
        tb.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_C));
        tb.setFloatable(false);

        JButton newNbBtn = makeButton("New Notebook", '\u2795');
        newNbBtn.addActionListener(e -> createNewNotebook());
        tb.add(newNbBtn);

        JButton newPgBtn = makeButton("New Page", '\u270f');
        newPgBtn.addActionListener(e -> createNewPage());
        tb.add(newPgBtn);

        tb.add(Box.createHorizontalStrut(8));

        JButton meetingBtn = makeButton("Meeting Notes", '\u260e');
        meetingBtn.addActionListener(e -> createTemplatePage("meeting"));
        tb.add(meetingBtn);

        JButton devBtn = makeButton("Dev Notes", '\u2699');
        devBtn.addActionListener(e -> createTemplatePage("dev"));
        tb.add(devBtn);

        tb.add(Box.createHorizontalStrut(8));

        searchField = new JTextField(20);
        searchField.putClientProperty("Nimbus.Overrides.Foreground", TEXT);
        searchField.setMargin(new Insets(4, 8, 4, 8));
        searchField.addActionListener(e -> performSearch());
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { performSearch(); }
        });
        tb.add(new JLabel(" Search: "));
        tb.add(searchField);

        tb.add(Box.createHorizontalGlue());

        JButton boldBtn = makeButton("B", '\0');
        boldBtn.setFont(boldBtn.getFont().deriveFont(Font.BOLD));
        boldBtn.addActionListener(e -> toggleBold());
        tb.add(boldBtn);

        JButton italicBtn = makeButton("I", '\0');
        italicBtn.setFont(italicBtn.getFont().deriveFont(Font.ITALIC));
        italicBtn.addActionListener(e -> toggleItalic());
        tb.add(italicBtn);

        JButton codeBtn = makeButton("Code", '\u25b6');
        codeBtn.addActionListener(e -> insertCodeBlock());
        tb.add(codeBtn);

        JButton linkBtn = makeButton("Link", '\u26d3');
        linkBtn.addActionListener(e -> insertLink());
        tb.add(linkBtn);

        tb.add(Box.createHorizontalStrut(8));

        JButton exportBtn = makeButton("Export", '\u2b07');
        exportBtn.addActionListener(e -> exportCurrentPage());
        tb.add(exportBtn);

        JButton importBtn = makeButton("Import", '\u2b06');
        importBtn.addActionListener(e -> importMarkdownFile());
        tb.add(importBtn);

        tb.add(Box.createHorizontalStrut(8));

        JButton settingsBtn = makeButton("Settings", '\u2699');
        settingsBtn.addActionListener(e -> showSettingsDialog());
        tb.add(settingsBtn);

        JButton aboutBtn = makeButton("About", '\u2139');
        aboutBtn.addActionListener(e -> showAboutDialog());
        tb.add(aboutBtn);

        return tb;
    }

    private JButton makeButton(String text, char icon) {
        String label = (icon != '\0') ? icon + " " + text : text;
        JButton btn = new JButton(label);
        btn.setForeground(TEXT);
        btn.setBackground(BTN);
        btn.setFocusPainted(false);
        btn.setBorderPainted(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_C, 1),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        btn.setOpaque(true);
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(ACCENT); }
            public void mouseExited(MouseEvent e) { btn.setBackground(BTN); }
        });
        return btn;
    }

    private JPanel createTreePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_C));

        JLabel header = new JLabel("  Notebooks");
        header.setForeground(TEXT);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 13f));
        header.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
        panel.add(header, BorderLayout.NORTH);

        rootNode = new DefaultMutableTreeNode("Root");
        treeModel = new DefaultTreeModel(rootNode);
        notebookTree = new JTree(treeModel);
        notebookTree.setBackground(BG);
        notebookTree.setForeground(TEXT);
        notebookTree.setCellRenderer(new NotebookTreeRenderer());
        notebookTree.setRootVisible(false);
        notebookTree.setShowsRootHandles(true);
        notebookTree.addTreeSelectionListener(e -> onTreeSelection(e));
        notebookTree.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showTreeContextMenu(e);
            }
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showTreeContextMenu(e);
            }
        });

        JScrollPane scroll = new JScrollPane(notebookTree);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);

        editorPane = new JTextPane();
        editorPane.setBackground(new Color(0x12, 0x12, 0x24));
        editorPane.setForeground(TEXT);
        editorPane.setCaretColor(TEXT);
        editorPane.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        editorPane.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        editorPane.setEditable(false);
        editorPane.setContentType("text/plain");

        JScrollPane scroll = new JScrollPane(editorPane);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(0x12, 0x12, 0x24));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER_C));

        JPanel metaSection = createSection("Metadata");
        metadataPanel = new JPanel();
        metadataPanel.setLayout(new BoxLayout(metadataPanel, BoxLayout.Y_AXIS));
        metadataPanel.setBackground(BG);
        JLabel createdLabel = new JLabel("Created: --");
        createdLabel.setForeground(MUTED);
        createdLabel.setName("createdLabel");
        createdLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        metadataPanel.add(createdLabel);
        JLabel modifiedLabel = new JLabel("Modified: --");
        modifiedLabel.setForeground(MUTED);
        modifiedLabel.setName("modifiedLabel");
        modifiedLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        metadataPanel.add(modifiedLabel);
        JLabel pageCountLabel = new JLabel("Pages: 0");
        pageCountLabel.setForeground(MUTED);
        pageCountLabel.setName("pageCountLabel");
        pageCountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        metadataPanel.add(pageCountLabel);
        metaSection.add(metadataPanel);
        panel.add(metaSection);

        JPanel tagSection = createSection("Tags");
        tagsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        tagsPanel.setBackground(BG);
        tagsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tagSection.add(tagsPanel);

        tagInput = new JTextField(12);
        tagInput.setMaximumSize(new Dimension(160, 26));
        tagInput.setMargin(new Insets(2, 6, 2, 6));
        tagInput.addActionListener(e -> addTag());
        tagSection.add(tagInput);
        panel.add(tagSection);

        JPanel projSection = createSection("Project Association");
        projectsPanel = new JPanel();
        projectsPanel.setLayout(new BoxLayout(projectsPanel, BoxLayout.Y_AXIS));
        projectsPanel.setBackground(BG);
        projSection.add(projectsPanel);
        panel.add(projSection);

        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createSection(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_C),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        panel.setMaximumSize(new Dimension(260, Integer.MAX_VALUE));
        panel.setPreferredSize(new Dimension(260, 0));

        JLabel label = new JLabel(title);
        label.setForeground(LINK_C);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        panel.add(label);

        return panel;
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BTN);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_C));
        bar.setPreferredSize(new Dimension(0, 26));

        statusNotebook = new JLabel("  No notebook");
        statusNotebook.setForeground(MUTED);
        bar.add(statusNotebook, BorderLayout.WEST);

        statusWordCount = new JLabel("Words: 0  ");
        statusWordCount.setForeground(MUTED);
        bar.add(statusWordCount, BorderLayout.EAST);

        statusSaved = new JLabel("Not saved  ");
        statusSaved.setForeground(MUTED);
        bar.add(statusSaved, BorderLayout.CENTER);

        return bar;
    }

    // ========================= NOTEBOOK / PAGE DATA =========================

    static class NotebookData {
        String name;
        String id;
        transient List<PageData> pages = new ArrayList<>();
        long created;
        long modified;

        NotebookData(String name) {
            this.name = name;
            this.id = UUID.randomUUID().toString();
            this.created = System.currentTimeMillis();
            this.modified = created;
        }
    }

    static class PageData {
        String title;
        String id;
        String content;
        transient List<String> tags = new ArrayList<>();
        String project;
        String template;
        long created;
        long modified;

        PageData(String title) {
            this.title = title;
            this.id = UUID.randomUUID().toString();
            this.content = "";
            this.created = System.currentTimeMillis();
            this.modified = created;
        }
    }

    // ========================= CRUD OPERATIONS =========================

    private void createNewNotebook() {
        String name = JOptionPane.showInputDialog(this, "Notebook name:", "New Notebook", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;
        NotebookData nb = new NotebookData(name.trim());
        notebooks.add(nb);
        saveNotebookMeta(nb);
        refreshTree();
        selectNotebook(nb);
    }

    private void createNewPage() {
        if (currentNotebook == null) {
            JOptionPane.showMessageDialog(this, "Select or create a notebook first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String title = JOptionPane.showInputDialog(this, "Page title:", "New Page", JOptionPane.PLAIN_MESSAGE);
        if (title == null || title.trim().isEmpty()) return;
        PageData page = new PageData(title.trim());
        currentNotebook.pages.add(page);
        currentNotebook.modified = System.currentTimeMillis();
        saveNotebookMeta(currentNotebook);
        refreshTree();
        selectPage(page);
    }

    private void createTemplatePage(String type) {
        if (currentNotebook == null) {
            JOptionPane.showMessageDialog(this, "Select or create a notebook first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        PageData page;
        if ("meeting".equals(type)) {
            page = new PageData("Meeting Notes - " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
            page.content = "# Meeting Notes\n\n**Date:** " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()) +
                "\n\n**Attendees:**\n- \n\n## Agenda\n1. \n\n## Discussion\n\n## Action Items\n- [ ] \n\n## Next Meeting\n";
            page.template = "meeting";
        } else {
            page = new PageData("Dev Notes - " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
            page.content = "# Development Notes\n\n**Feature:** \n\n## Summary\n\n## Implementation\n\n```java\n// code here\n```\n\n## Issues\n\n## TODO\n- [ ] \n";
            page.template = "dev";
        }
        currentNotebook.pages.add(page);
        currentNotebook.modified = System.currentTimeMillis();
        saveNotebookMeta(currentNotebook);
        refreshTree();
        selectPage(page);
    }

    private void deleteSelectedNode() {
        TreePath path = notebookTree.getSelectionPath();
        if (path == null) return;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObj = node.getUserObject();

        if (userObj instanceof PageData) {
            PageData page = (PageData) userObj;
            int confirm = JOptionPane.showConfirmDialog(this, "Delete page \"" + page.title + "\"?",
                "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            currentNotebook.pages.remove(page);
            if (currentPage == page) {
                currentPage = null;
                editorPane.setText("");
                editorPane.setEditable(false);
            }
            currentNotebook.modified = System.currentTimeMillis();
            saveNotebookMeta(currentNotebook);
            refreshTree();
        } else if (userObj instanceof NotebookData) {
            NotebookData nb = (NotebookData) userObj;
            int confirm = JOptionPane.showConfirmDialog(this, "Delete notebook \"" + nb.name + "\" and all its pages?",
                "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            notebooks.remove(nb);
            deleteNotebookFiles(nb);
            if (currentNotebook == nb) {
                currentNotebook = null;
                currentPage = null;
                editorPane.setText("");
                editorPane.setEditable(false);
            }
            refreshTree();
        }
    }

    private void renameSelectedNode() {
        TreePath path = notebookTree.getSelectionPath();
        if (path == null) return;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObj = node.getUserObject();

        if (userObj instanceof NotebookData) {
            NotebookData nb = (NotebookData) userObj;
            String newName = JOptionPane.showInputDialog(this, "Rename notebook:", nb.name);
            if (newName != null && !newName.trim().isEmpty()) {
                nb.name = newName.trim();
                nb.modified = System.currentTimeMillis();
                saveNotebookMeta(nb);
                refreshTree();
                updateStatusBar();
            }
        } else if (userObj instanceof PageData) {
            PageData page = (PageData) userObj;
            String newTitle = JOptionPane.showInputDialog(this, "Rename page:", page.title);
            if (newTitle != null && !newTitle.trim().isEmpty()) {
                page.title = newTitle.trim();
                page.modified = System.currentTimeMillis();
                currentNotebook.modified = System.currentTimeMillis();
                saveNotebookMeta(currentNotebook);
                refreshTree();
            }
        }
    }

    // ========================= TREE MANAGEMENT =========================

    private void refreshTree() {
        rootNode.removeAllChildren();
        for (NotebookData nb : notebooks) {
            DefaultMutableTreeNode nbNode = new DefaultMutableTreeNode(nb);
            for (PageData page : nb.pages) {
                nbNode.add(new DefaultMutableTreeNode(page));
            }
            rootNode.add(nbNode);
        }
        treeModel.reload();
        notebookTree.expandRow(0);
        for (int i = 0; i < notebookTree.getRowCount(); i++) {
            notebookTree.expandRow(i);
        }
    }

    private void onTreeSelection(TreeSelectionEvent e) {
        TreePath path = e.getNewLeadSelectionPath();
        if (path == null) return;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObj = node.getUserObject();

        if (userObj instanceof PageData) {
            selectPage((PageData) userObj);
        } else if (userObj instanceof NotebookData) {
            selectNotebook((NotebookData) userObj);
        }
    }

    private void selectNotebook(NotebookData nb) {
        currentNotebook = nb;
        currentPage = null;
        editorPane.setText("");
        editorPane.setEditable(false);
        editorPane.setContentType("text/plain");
        updateStatusBar();
        updateMetadata(nb);
        updateProjectPanel(null);
        updateTagsPanel(null);
    }

    private void selectPage(PageData page) {
        for (NotebookData nb : notebooks) {
            if (nb.pages.contains(page)) {
                currentNotebook = nb;
                break;
            }
        }
        currentPage = page;
        editorPane.setEditable(true);
        renderMarkdown(page.content);
        updateStatusBar();
        updateMetadata(null);
        updateTagsPanel(page);
        updateProjectPanel(page);
    }

    private void selectFirstNotebook() {
        if (!notebooks.isEmpty()) {
            NotebookData nb = notebooks.get(0);
            TreePath path = findNodePath(rootNode, nb);
            if (path != null) {
                notebookTree.setSelectionPath(path);
                notebookTree.scrollPathToVisible(path);
            }
        }
    }

    private TreePath findNodePath(DefaultMutableTreeNode root, Object target) {
        Enumeration<?> en = root.depthFirstEnumeration();
        while (en.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) en.nextElement();
            if (node.getUserObject() == target || node.getUserObject().equals(target)) {
                return new TreePath(node.getPath());
            }
        }
        return null;
    }

    private void showTreeContextMenu(MouseEvent e) {
        int row = notebookTree.getRowForLocation(e.getX(), e.getY());
        if (row < 0) return;
        notebookTree.setSelectionRow(row);

        TreePath path = notebookTree.getPathForRow(row);
        if (path == null) return;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(BTN);
        menu.setForeground(TEXT);

        Object userObj = node.getUserObject();
        if (userObj instanceof NotebookData) {
            addMenuItem(menu, "New Page", e2 -> createNewPage());
            addMenuItem(menu, "Rename", e2 -> renameSelectedNode());
            addMenuItem(menu, "Delete", e2 -> deleteSelectedNode());
        } else if (userObj instanceof PageData) {
            addMenuItem(menu, "Rename", e2 -> renameSelectedNode());
            addMenuItem(menu, "Duplicate", e2 -> duplicatePage((PageData) userObj));
            addMenuItem(menu, "Delete", e2 -> deleteSelectedNode());
        }

        menu.show(notebookTree, e.getX(), e.getY());
    }

    private void addMenuItem(JPopupMenu menu, String label, ActionListener al) {
        JMenuItem item = new JMenuItem(label);
        item.setForeground(TEXT);
        item.setBackground(BTN);
        item.addActionListener(al);
        menu.add(item);
    }

    private void duplicatePage(PageData original) {
        if (currentNotebook == null) return;
        PageData copy = new PageData(original.title + " (copy)");
        copy.content = original.content;
        copy.tags.addAll(original.tags);
        copy.project = original.project;
        currentNotebook.pages.add(copy);
        currentNotebook.modified = System.currentTimeMillis();
        saveNotebookMeta(currentNotebook);
        refreshTree();
        selectPage(copy);
    }

    // ========================= MARKDOWN RENDERING =========================

    private void renderMarkdown(String text) {
        editorPane.setContentType("text/plain");
        editorPane.setText("");
        editorPane.setEditable(true);

        if (text == null || text.isEmpty()) return;

        StyledDocument doc = editorPane.getStyledDocument();
        editorPane.setCharacterAttributes(new SimpleAttributeSet(), false);

        String[] lines = text.split("\n", -1);
        boolean inCodeBlock = false;
        StringBuilder codeBuffer = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("```")) {
                if (inCodeBlock) {
                    appendCodeBlock(doc, codeBuffer.toString());
                    codeBuffer.setLength(0);
                    inCodeBlock = false;
                } else {
                    inCodeBlock = true;
                }
                continue;
            }

            if (inCodeBlock) {
                codeBuffer.append(line).append("\n");
                continue;
            }

            if (line.startsWith("# ")) {
                appendStyled(doc, line.substring(2) + "\n", Font.BOLD, 20f, LINK_C);
            } else if (line.startsWith("## ")) {
                appendStyled(doc, line.substring(3) + "\n", Font.BOLD, 17f, LINK_C);
            } else if (line.startsWith("### ")) {
                appendStyled(doc, line.substring(4) + "\n", Font.BOLD, 15f, TEXT);
            } else if (line.startsWith("- [ ] ") || line.startsWith("- [x] ")) {
                boolean checked = line.startsWith("- [x] ");
                String prefix = checked ? "\u2611 " : "\u2610 ";
                appendStyled(doc, prefix + line.substring(6) + "\n", Font.PLAIN, fontSize, TEXT);
            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                appendStyled(doc, "  \u2022 " + line.substring(2) + "\n", Font.PLAIN, fontSize, TEXT);
            } else if (line.startsWith("1. ") || line.startsWith("2. ")) {
                appendStyled(doc, "  " + line + "\n", Font.PLAIN, fontSize, TEXT);
            } else if (line.startsWith("> ")) {
                appendStyled(doc, "  \u25b8 " + line.substring(2) + "\n", Font.ITALIC, fontSize, MUTED);
            } else if (line.startsWith("---")) {
                appendStyled(doc, "\u2500".repeat(50) + "\n", Font.PLAIN, fontSize, BORDER_C);
            } else {
                appendInlineMarkdown(doc, line + "\n");
            }
        }

        if (inCodeBlock && codeBuffer.length() > 0) {
            appendCodeBlock(doc, codeBuffer.toString());
        }
    }

    private void appendInlineMarkdown(StyledDocument doc, String text) {
        int idx = 0;
        Pattern inlinePattern = Pattern.compile(
            "\\[\\[([^\\]]+)\\]\\]|\\*\\*([^*]+)\\*\\*|\\*([^*]+)\\*|`([^`]+)`"
        );
        Matcher matcher = inlinePattern.matcher(text);

        while (matcher.find()) {
            if (matcher.start() > idx) {
                appendStyled(doc, text.substring(idx, matcher.start()), Font.PLAIN, fontSize, TEXT);
            }
            if (matcher.group(1) != null) {
                appendStyled(doc, matcher.group(1), Font.PLAIN, fontSize, LINK_C);
            } else if (matcher.group(2) != null) {
                appendStyled(doc, matcher.group(2), Font.BOLD, fontSize, TEXT);
            } else if (matcher.group(3) != null) {
                appendStyled(doc, matcher.group(3), Font.ITALIC, fontSize, TEXT);
            } else if (matcher.group(4) != null) {
                appendInlineCode(doc, matcher.group(4));
            }
            idx = matcher.end();
        }

        if (idx < text.length()) {
            appendStyled(doc, text.substring(idx), Font.PLAIN, fontSize, TEXT);
        }
    }

    private void appendStyled(StyledDocument doc, String text, int style, float size, Color color) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attrs, "SansSerif");
        StyleConstants.setFontSize(attrs, (int) size);
        StyleConstants.setBold(attrs, (style & Font.BOLD) != 0);
        StyleConstants.setItalic(attrs, (style & Font.ITALIC) != 0);
        StyleConstants.setForeground(attrs, color);
        try {
            doc.insertString(doc.getLength(), text, attrs);
        } catch (Exception ignored) {}
    }

    private void appendInlineCode(StyledDocument doc, String code) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attrs, "Monospaced");
        StyleConstants.setFontSize(attrs, fontSize);
        StyleConstants.setForeground(attrs, new Color(0xff, 0x99, 0x66));
        StyleConstants.setBackground(attrs, CODE_BG);
        try {
            doc.insertString(doc.getLength(), code, attrs);
        } catch (Exception ignored) {}
    }

    private void appendCodeBlock(StyledDocument doc, String code) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attrs, "Monospaced");
        StyleConstants.setFontSize(attrs, fontSize - 1);
        StyleConstants.setForeground(attrs, new Color(0xcc, 0xcc, 0xcc));
        StyleConstants.setBackground(attrs, CODE_BG);
        StyleConstants.setLeftIndent(attrs, 20f);
        try {
            doc.insertString(doc.getLength(), code + "\n", attrs);
        } catch (Exception ignored) {}
    }

    // ========================= INLINE EDITING WITH FORMATTING =========================

    private void toggleBold() {
        if (!editorPane.isEditable()) return;
        String sel = editorPane.getSelectedText();
        if (sel == null || sel.isEmpty()) return;
        int start = editorPane.getSelectionStart();
        int end = editorPane.getSelectionEnd();
        String content = currentPage.content;
        String before = content.substring(0, start);
        String after = content.substring(end);
        currentPage.content = before + "**" + sel + "**" + after;
        renderMarkdown(currentPage.content);
        markDirty();
    }

    private void toggleItalic() {
        if (!editorPane.isEditable()) return;
        String sel = editorPane.getSelectedText();
        if (sel == null || sel.isEmpty()) return;
        int start = editorPane.getSelectionStart();
        int end = editorPane.getSelectionEnd();
        String content = currentPage.content;
        String before = content.substring(0, start);
        String after = content.substring(end);
        currentPage.content = before + "*" + sel + "*" + after;
        renderMarkdown(currentPage.content);
        markDirty();
    }

    private void insertCodeBlock() {
        if (!editorPane.isEditable()) return;
        int pos = editorPane.getCaretPosition();
        String content = currentPage.content;
        String insert = "```\n\n```";
        currentPage.content = content.substring(0, pos) + insert + content.substring(pos);
        renderMarkdown(currentPage.content);
        editorPane.setCaretPosition(pos + 4);
        markDirty();
    }

    private void insertLink() {
        if (!editorPane.isEditable()) return;
        int pos = editorPane.getCaretPosition();
        String content = currentPage.content;
        String insert = "[[Note Name]]";
        currentPage.content = content.substring(0, pos) + insert + content.substring(pos);
        renderMarkdown(currentPage.content);
        editorPane.setCaretPosition(pos + 2);
        markDirty();
    }

    // ========================= TAGS =========================

    private void updateTagsPanel(PageData page) {
        tagsPanel.removeAll();
        if (page == null) {
            tagsPanel.revalidate();
            tagsPanel.repaint();
            return;
        }
        for (String tag : page.tags) {
            JLabel tagLabel = new JLabel(" " + tag + " \u2716 ");
            tagLabel.setForeground(TEXT);
            tagLabel.setBackground(TAG_BG);
            tagLabel.setOpaque(true);
            tagLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_C, 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
            ));
            tagLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            tagLabel.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    page.tags.remove(tag);
                    currentNotebook.modified = System.currentTimeMillis();
                    saveNotebookMeta(currentNotebook);
                    updateTagsPanel(page);
                }
            });
            tagsPanel.add(tagLabel);
        }
        tagsPanel.revalidate();
        tagsPanel.repaint();
    }

    private void addTag() {
        if (currentPage == null) return;
        String tag = tagInput.getText().trim();
        if (tag.isEmpty()) return;
        if (!currentPage.tags.contains(tag)) {
            currentPage.tags.add(tag);
            currentNotebook.modified = System.currentTimeMillis();
            saveNotebookMeta(currentNotebook);
        }
        tagInput.setText("");
        updateTagsPanel(currentPage);
    }

    // ========================= PROJECT ASSOCIATION =========================

    private void updateProjectPanel(PageData page) {
        projectsPanel.removeAll();
        if (page == null) {
            projectsPanel.revalidate();
            projectsPanel.repaint();
            return;
        }
        ButtonGroup group = new ButtonGroup();
        JRadioButton noneBtn = new JRadioButton("(none)");
        noneBtn.setForeground(TEXT);
        noneBtn.setBackground(BG);
        noneBtn.setOpaque(false);
        noneBtn.setSelected(page.project == null || page.project.isEmpty());
        noneBtn.addActionListener(e -> {
            page.project = null;
            currentNotebook.modified = System.currentTimeMillis();
            saveNotebookMeta(currentNotebook);
        });
        group.add(noneBtn);
        projectsPanel.add(noneBtn);
        projectsPanel.add(Box.createVerticalStrut(2));

        for (String app : appList) {
            JRadioButton btn = new JRadioButton(app);
            btn.setForeground(TEXT);
            btn.setBackground(BG);
            btn.setOpaque(false);
            btn.setSelected(app.equals(page.project));
            btn.addActionListener(e -> {
                page.project = app;
                currentNotebook.modified = System.currentTimeMillis();
                saveNotebookMeta(currentNotebook);
            });
            group.add(btn);
            projectsPanel.add(btn);
            projectsPanel.add(Box.createVerticalStrut(1));
        }

        projectsPanel.revalidate();
        projectsPanel.repaint();
    }

    // ========================= METADATA =========================

    private void updateMetadata(Object obj) {
        for (Component c : metadataPanel.getComponents()) {
            if (c instanceof JLabel) {
                JLabel lbl = (JLabel) c;
                if ("createdLabel".equals(lbl.getName())) {
                    lbl.setText("Created: " + (obj instanceof NotebookData
                        ? formatTime(((NotebookData) obj).created)
                        : obj instanceof PageData ? formatTime(((PageData) obj).created) : "--"));
                } else if ("modifiedLabel".equals(lbl.getName())) {
                    lbl.setText("Modified: " + (obj instanceof NotebookData
                        ? formatTime(((NotebookData) obj).modified)
                        : obj instanceof PageData ? formatTime(((PageData) obj).modified) : "--"));
                } else if ("pageCountLabel".equals(lbl.getName())) {
                    lbl.setText("Pages: " + (currentNotebook != null ? currentNotebook.pages.size() : 0));
                }
            }
        }
        metadataPanel.revalidate();
        metadataPanel.repaint();
    }

    private String formatTime(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(millis));
    }

    // ========================= SEARCH =========================

    private void performSearch() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            refreshTree();
            return;
        }
        rootNode.removeAllChildren();
        for (NotebookData nb : notebooks) {
            DefaultMutableTreeNode nbNode = new DefaultMutableTreeNode(nb);
            boolean nbMatch = nb.name.toLowerCase().contains(query);
            for (PageData page : nb.pages) {
                boolean match = page.title.toLowerCase().contains(query)
                    || page.content.toLowerCase().contains(query)
                    || page.tags.stream().anyMatch(t -> t.toLowerCase().contains(query));
                if (match || nbMatch) {
                    nbNode.add(new DefaultMutableTreeNode(page));
                }
            }
            if (nbNode.getChildCount() > 0) {
                rootNode.add(nbNode);
            }
        }
        treeModel.reload();
        for (int i = 0; i < notebookTree.getRowCount(); i++) {
            notebookTree.expandRow(i);
        }
    }

    // ========================= EXPORT / IMPORT =========================

    private void exportCurrentPage() {
        if (currentPage == null) {
            JOptionPane.showMessageDialog(this, "No page selected.", "Export", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(currentPage.title.replaceAll("[^a-zA-Z0-9_\\- ]", "") + ".md"));
        fc.setDialogTitle("Export as Markdown");
        fc.setFileFilter(new FileNameExtensionFilter("Markdown files (*.md)", "md"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if (!file.getName().endsWith(".md")) {
                file = new File(file.getAbsolutePath() + ".md");
            }
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("---\n");
                sb.append("title: ").append(currentPage.title).append("\n");
                sb.append("tags: [").append(String.join(", ", currentPage.tags)).append("]\n");
                if (currentPage.project != null) {
                    sb.append("project: ").append(currentPage.project).append("\n");
                }
                sb.append("created: ").append(formatTime(currentPage.created)).append("\n");
                sb.append("modified: ").append(formatTime(currentPage.modified)).append("\n");
                sb.append("---\n\n");
                sb.append(currentPage.content);
                Files.write(file.toPath(), sb.toString().getBytes());
                JOptionPane.showMessageDialog(this, "Exported to " + file.getName(), "Export", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importMarkdownFile() {
        if (currentNotebook == null) {
            JOptionPane.showMessageDialog(this, "Select a notebook first.", "Import", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Import Markdown");
        fc.setFileFilter(new FileNameExtensionFilter("Markdown files (*.md)", "md"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            try {
                String raw = new String(Files.readAllBytes(file.toPath()));
                String title = file.getName().replaceAll("\\.md$", "");
                List<String> tags = new ArrayList<>();
                String project = null;
                String content = raw;

                if (raw.startsWith("---")) {
                    int endFront = raw.indexOf("---", 3);
                    if (endFront > 0) {
                        String front = raw.substring(3, endFront).trim();
                        content = raw.substring(endFront + 3).trim();
                        for (String line : front.split("\n")) {
                            line = line.trim();
                            if (line.startsWith("title:")) {
                                title = line.substring(6).trim();
                            } else if (line.startsWith("tags:")) {
                                String tagStr = line.substring(5).trim();
                                tagStr = tagStr.replaceAll("[\\[\\]]", "");
                                for (String t : tagStr.split(",")) {
                                    String trimmed = t.trim();
                                    if (!trimmed.isEmpty()) tags.add(trimmed);
                                }
                            } else if (line.startsWith("project:")) {
                                project = line.substring(8).trim();
                            }
                        }
                    }
                }

                PageData page = new PageData(title);
                page.content = content;
                page.tags.addAll(tags);
                page.project = project;
                currentNotebook.pages.add(page);
                currentNotebook.modified = System.currentTimeMillis();
                saveNotebookMeta(currentNotebook);
                refreshTree();
                selectPage(page);
                JOptionPane.showMessageDialog(this, "Imported: " + title, "Import", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Import Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ========================= PERSISTENCE =========================

    private void saveCurrentPage() {
        if (currentPage == null || currentNotebook == null) return;
        currentPage.content = editorPane.getText();
        currentPage.modified = System.currentTimeMillis();
        savePageContent(currentPage);
        createBackup(currentPage);
        statusSaved.setText("Saved " + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "  ");
    }

    private void savePageContent(PageData page) {
        File nbDir = new File(NOTES_DIR, currentNotebook.id);
        nbDir.mkdirs();
        File pageFile = new File(nbDir, page.id + ".dat");
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("TITLE:").append(page.title).append("\n");
            sb.append("TAGS:").append(String.join(",", page.tags)).append("\n");
            sb.append("PROJECT:").append(page.project != null ? page.project : "").append("\n");
            sb.append("TEMPLATE:").append(page.template != null ? page.template : "").append("\n");
            sb.append("CREATED:").append(page.created).append("\n");
            sb.append("MODIFIED:").append(page.modified).append("\n");
            sb.append("CONTENT:\n");
            sb.append(page.content);
            Files.write(pageFile.toPath(), sb.toString().getBytes());
        } catch (IOException e) {
            System.err.println("Failed to save page: " + e.getMessage());
        }
    }

    private void saveNotebookMeta(NotebookData nb) {
        File metaFile = new File(NOTES_DIR, nb.id + ".nb");
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("NAME:").append(nb.name).append("\n");
            sb.append("CREATED:").append(nb.created).append("\n");
            sb.append("MODIFIED:").append(nb.modified).append("\n");
            sb.append("PAGES:");
            sb.append(nb.pages.stream().map(p -> p.id).collect(Collectors.joining(",")));
            sb.append("\n");
            Files.write(metaFile.toPath(), sb.toString().getBytes());
            for (PageData page : nb.pages) {
                savePageContent(page);
            }
        } catch (IOException e) {
            System.err.println("Failed to save notebook: " + e.getMessage());
        }
    }

    private void loadAllNotebooks() {
        notebooks.clear();
        File[] nbFiles = NOTES_DIR.listFiles((dir, name) -> name.endsWith(".nb"));
        if (nbFiles == null) return;

        for (File nbFile : nbFiles) {
            try {
                String[] lines = new String(Files.readAllBytes(nbFile.toPath())).split("\n");
                NotebookData nb = new NotebookData("");
                nb.id = nbFile.getName().replace(".nb", "");

                for (String line : lines) {
                    if (line.startsWith("NAME:")) nb.name = line.substring(5);
                    else if (line.startsWith("CREATED:")) nb.created = Long.parseLong(line.substring(8));
                    else if (line.startsWith("MODIFIED:")) nb.modified = Long.parseLong(line.substring(9));
                    else if (line.startsWith("PAGES:")) {
                        String pageIds = line.substring(6);
                        if (!pageIds.isEmpty()) {
                            for (String pid : pageIds.split(",")) {
                                PageData page = loadPage(nb.id, pid.trim());
                                if (page != null) nb.pages.add(page);
                            }
                        }
                    }
                }
                notebooks.add(nb);
            } catch (Exception e) {
                System.err.println("Failed to load notebook: " + nbFile.getName());
            }
        }
        notebooks.sort((a, b) -> Long.compare(b.modified, a.modified));
    }

    private PageData loadPage(String nbId, String pageId) {
        File pageFile = new File(NOTES_DIR, nbId + "/" + pageId + ".dat");
        if (!pageFile.exists()) return null;
        try {
            String raw = new String(Files.readAllBytes(pageFile.toPath()));
            PageData page = new PageData("");
            page.id = pageId;

            int contentIdx = raw.indexOf("CONTENT:\n");
            String header = contentIdx >= 0 ? raw.substring(0, contentIdx) : raw;
            page.content = contentIdx >= 0 ? raw.substring(contentIdx + 9) : "";

            for (String line : header.split("\n")) {
                if (line.startsWith("TITLE:")) page.title = line.substring(6);
                else if (line.startsWith("TAGS:")) {
                    String tags = line.substring(5);
                    if (!tags.isEmpty()) {
                        page.tags.addAll(Arrays.asList(tags.split(",")));
                    }
                }
                else if (line.startsWith("PROJECT:")) {
                    String p = line.substring(8);
                    page.project = p.isEmpty() ? null : p;
                }
                else if (line.startsWith("TEMPLATE:")) {
                    String t = line.substring(9);
                    page.template = t.isEmpty() ? null : t;
                }
                else if (line.startsWith("CREATED:")) page.created = Long.parseLong(line.substring(8));
                else if (line.startsWith("MODIFIED:")) page.modified = Long.parseLong(line.substring(9));
            }
            return page;
        } catch (Exception e) {
            System.err.println("Failed to load page: " + pageId);
            return null;
        }
    }

    private void deleteNotebookFiles(NotebookData nb) {
        File metaFile = new File(NOTES_DIR, nb.id + ".nb");
        metaFile.delete();
        File nbDir = new File(NOTES_DIR, nb.id);
        if (nbDir.exists()) {
            File[] pages = nbDir.listFiles();
            if (pages != null) {
                for (File f : pages) f.delete();
            }
            nbDir.delete();
        }
    }

    // ========================= BACKUPS =========================

    private void createBackup(PageData page) {
        File backupDir = new File(BACKUP_DIR, currentNotebook.id);
        backupDir.mkdirs();
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File backupFile = new File(backupDir, page.id + "_" + timestamp + ".bak");
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("TITLE:").append(page.title).append("\n");
            sb.append("CONTENT:\n").append(page.content);
            Files.write(backupFile.toPath(), sb.toString().getBytes());
            cleanOldBackups(backupDir, page.id, 20);
        } catch (IOException e) {
            System.err.println("Backup failed: " + e.getMessage());
        }
    }

    private void cleanOldBackups(File backupDir, String pageId, int maxKeep) {
        File[] backups = backupDir.listFiles((dir, name) -> name.startsWith(pageId + "_"));
        if (backups == null || backups.length <= maxKeep) return;
        Arrays.sort(backups, (a, b) -> b.getName().compareTo(a.getName()));
        for (int i = maxKeep; i < backups.length; i++) {
            backups[i].delete();
        }
    }

    // ========================= STATUS BAR =========================

    private void updateStatusBar() {
        if (currentNotebook != null) {
            statusNotebook.setText("  " + currentNotebook.name);
        } else {
            statusNotebook.setText("  No notebook");
        }
        updateWordCount();
    }

    private void updateWordCount() {
        String text = editorPane.getText();
        if (text == null || text.isEmpty()) {
            statusWordCount.setText("Words: 0  ");
        } else {
            int words = text.trim().split("\\s+").length;
            statusWordCount.setText("Words: " + words + "  ");
        }
    }

    // ========================= KEYBOARD SHORTCUTS =========================

    private void bindKeyboardShortcuts() {
        InputMap im = editorPane.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = editorPane.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "save");
        am.put("save", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { saveCurrentPage(); dirty = false; }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "newPage");
        am.put("newPage", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { createNewPage(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "newNotebook");
        am.put("newNotebook", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { createNewNotebook(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "find");
        am.put("find", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { searchField.requestFocusInWindow(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK), "bold");
        am.put("bold", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { toggleBold(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK), "italic");
        am.put("italic", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { toggleItalic(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK), "export");
        am.put("export", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { exportCurrentPage(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK), "link");
        am.put("link", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { insertLink(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
        am.put("undo", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { /* undo handled externally */ }
        });
    }

    // ========================= SETTINGS / ABOUT =========================

    private void showSettingsDialog() {
        JDialog dialog = new JDialog(this, "Settings", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(BG);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG);
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel delayLabel = new JLabel("Auto-save delay (ms):");
        delayLabel.setForeground(TEXT);
        form.add(delayLabel, gbc);

        gbc.gridx = 1;
        JTextField delayField = new JTextField(String.valueOf(autoSaveDelay), 10);
        delayField.setForeground(TEXT);
        delayField.setBackground(BTN);
        delayField.setCaretColor(TEXT);
        form.add(delayField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel sizeLabel = new JLabel("Font size:");
        sizeLabel.setForeground(TEXT);
        form.add(sizeLabel, gbc);

        gbc.gridx = 1;
        JTextField sizeField = new JTextField(String.valueOf(fontSize), 10);
        sizeField.setForeground(TEXT);
        sizeField.setBackground(BTN);
        sizeField.setCaretColor(TEXT);
        form.add(sizeField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(BG);

        JButton cancelBtn = makeButton("Cancel", '\0');
        cancelBtn.addActionListener(e -> dialog.dispose());
        btnPanel.add(cancelBtn);

        JButton saveBtn = makeButton("Save", '\0');
        saveBtn.addActionListener(e -> {
            try {
                int newDelay = Integer.parseInt(delayField.getText().trim());
                int newSize = Integer.parseInt(sizeField.getText().trim());
                if (newDelay >= 500 && newDelay <= 30000) {
                    autoSaveDelay = newDelay;
                    autoSaveTimer.setDelay(autoSaveDelay);
                }
                if (newSize >= 10 && newSize <= 36) {
                    fontSize = newSize;
                    editorPane.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
                    if (currentPage != null) {
                        renderMarkdown(currentPage.content);
                    }
                }
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid number.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        btnPanel.add(saveBtn);

        form.add(btnPanel, gbc);

        dialog.add(form, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void showAboutDialog() {
        JDialog dialog = new JDialog(this, "About DataNotes", true);
        dialog.setSize(420, 320);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(BG);
        dialog.setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG);
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel title = new JLabel("DataNotes");
        title.setForeground(LINK_C);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(title);

        content.add(Box.createVerticalStrut(12));

        JLabel version = new JLabel("Version 1.0.0");
        version.setForeground(MUTED);
        version.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(version);

        content.add(Box.createVerticalStrut(16));

        String info = "<html><div style='text-align:center; color:#e0e0e0;'>" +
            "A local notes/wiki application.<br><br>" +
            "Features: Notebooks, Pages, Markdown,<br>" +
            "Tags, Search, Templates, Backups,<br>" +
            "Internal links, Export/Import.<br><br>" +
            "<span style='color:#8888aa;'>Storage: ~/.datadocs/notes/</span><br>" +
            "<span style='color:#8888aa;'>Backups: ~/.datadocs/backups/</span>" +
            "</div></html>";
        JLabel infoLabel = new JLabel(info);
        infoLabel.setForeground(TEXT);
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(infoLabel);

        content.add(Box.createVerticalStrut(20));

        JButton okBtn = makeButton("OK", '\0');
        okBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        okBtn.addActionListener(e -> dialog.dispose());
        content.add(okBtn);

        dialog.add(content, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    // ========================= INTERNAL LINK HANDLING =========================

    private void setupInternalLinks() {
        editorPane.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (currentPage == null) return;
                int pos = editorPane.viewToModel2D(new Point2D.Double(e.getX(), e.getY()));
                String text = currentPage.content;
                Pattern linkPat = Pattern.compile("\\[\\[([^\\]]+)\\]\\]");
                Matcher matcher = linkPat.matcher(text);
                while (matcher.find()) {
                    if (pos >= matcher.start() && pos <= matcher.end()) {
                        openInternalLink(matcher.group(1));
                        break;
                    }
                }
            }
        });
    }

    private void openInternalLink(String noteName) {
        for (NotebookData nb : notebooks) {
            for (PageData page : nb.pages) {
                if (page.title.equalsIgnoreCase(noteName)) {
                    TreePath path = findNodePath(rootNode, page);
                    if (path != null) {
                        notebookTree.setSelectionPath(path);
                        notebookTree.scrollPathToVisible(path);
                    }
                    return;
                }
            }
        }
        int opt = JOptionPane.showConfirmDialog(this,
            "Page \"" + noteName + "\" not found. Create it?",
            "Internal Link", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION && currentNotebook != null) {
            PageData newPage = new PageData(noteName);
            currentNotebook.pages.add(newPage);
            currentNotebook.modified = System.currentTimeMillis();
            saveNotebookMeta(currentNotebook);
            refreshTree();
            selectPage(newPage);
        }
    }

    // ========================= TREE RENDERER =========================

    private class NotebookTreeRenderer extends DefaultTreeCellRenderer {
        private static final long serialVersionUID = 1L;

        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            setBackgroundNonSelectionColor(BG);
            setBackgroundSelectionColor(ACCENT);
            setForeground(sel ? TEXT : MUTED);

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userObj = node.getUserObject();

            if (userObj instanceof NotebookData) {
                NotebookData nb = (NotebookData) userObj;
                setText("\uD83D\uDCD3 " + nb.name);
                setFont(getFont().deriveFont(Font.BOLD, 13f));
            } else if (userObj instanceof PageData) {
                PageData page = (PageData) userObj;
                String icon = "\uD83D\uDCC4";
                if ("meeting".equals(page.template)) icon = "\u260E";
                else if ("dev".equals(page.template)) icon = "\u2699";
                setText(icon + " " + page.title);
                setFont(getFont().deriveFont(Font.PLAIN, 12f));
            }
            return this;
        }
    }

    // ========================= MAIN =========================

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            App app = new App();
            app.setupInternalLinks();
            app.setVisible(true);
        });
    }
}
