import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Element;
import javax.swing.undo.UndoManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

/**
 * DataDocs 4.0
 * A dependency-free Java Swing document workspace.
 *
 * Java 17+ recommended.
 */
public class DataDocs {

    // =========================================================
    // APP INFO
    // =========================================================

    private static final String APP_NAME = "DataDocs";
    private static final String VERSION = "4.0.0";

    // =========================================================
    // THEME
    // =========================================================

    private static final Color DARK_BG = new Color(24, 26, 27);
    private static final Color DARK_PANEL = new Color(34, 38, 41);
    private static final Color DARK_PANEL_2 = new Color(42, 46, 50);
    private static final Color DARK_TEXT = new Color(228, 224, 218);
    private static final Color DARK_MUTED = new Color(165, 170, 175);

    private static final Color LIGHT_BG = new Color(246, 247, 249);
    private static final Color LIGHT_PANEL = new Color(230, 233, 237);
    private static final Color LIGHT_PANEL_2 = new Color(218, 223, 228);
    private static final Color LIGHT_TEXT = new Color(28, 30, 33);
    private static final Color LIGHT_MUTED = new Color(90, 95, 100);

    private static final Color ACCENT = new Color(14, 134, 212);
    private static final Color ACCENT_HOVER = new Color(24, 154, 232);
    private static final Color SECONDARY = new Color(60, 64, 67);
    private static final Color SECONDARY_HOVER = new Color(80, 84, 87);
    private static final Color DANGER = new Color(185, 55, 55);

    private static final Font UI_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font SMALL_FONT = new Font("SansSerif", Font.PLAIN, 12);

    private static boolean darkMode = true;
    private static boolean focusMode = false;

    // =========================================================
    // DATA PATHS
    // =========================================================

    private static final Path COMPANY_DIR = Paths.get(System.getProperty("user.home"), "Data");
    private static final Path ERROR_DIR = COMPANY_DIR.resolve("errors");
    private static final Path ERROR_LOG = ERROR_DIR.resolve("errors.log");
    private static final Path APP_DATA_DIR = COMPANY_DIR.resolve(".datadocs");
    private static final Path AUTOSAVE_DIR = APP_DATA_DIR.resolve("autosave");
    private static final Path BACKUP_DIR = APP_DATA_DIR.resolve("backups");
    private static final Path SETTINGS_FILE = APP_DATA_DIR.resolve("settings.properties");
    private static final Path SESSION_FILE = APP_DATA_DIR.resolve("session.properties");

    // =========================================================
    // UI STATE
    // =========================================================

    private static JFrame frame;
    private static JTabbedPane tabs;
    private static JPanel toolbar;
    private static JPanel statusBar;
    private static JLabel statusLeft;
    private static JLabel statusRight;
    private static JComboBox<String> fontMenu;
    private static JComboBox<Integer> sizeMenu;
    private static JToggleButton boldButton;
    private static JToggleButton italicButton;
    private static JCheckBoxMenuItem wrapMenuItem;
    private static JCheckBoxMenuItem darkModeMenuItem;
    private static JCheckBoxMenuItem focusModeMenuItem;
    private static JMenu recentMenu;
    private static JCheckBoxMenuItem explorerMenuItem;
    private static JSplitPane workspaceSplit;
    private static JPanel explorerPanel;
    private static JTree fileTree;
    private static JLabel explorerRootLabel;
    private static Path explorerRoot;
    private static boolean explorerVisible = true;

    private static String currentFontFamily = "Monospaced";
    private static int currentFontSize = 16;
    private static int currentFontStyle = Font.PLAIN;
    private static boolean defaultWordWrap = true;

    private static final List<Path> recentFiles = new ArrayList<>();
    private static final int MAX_RECENT = 10;

    private static Timer autosaveTimer;
    private static Timer statusTimer;

    // =========================================================
    // DOCUMENT MODEL
    // =========================================================

    private static final class DocumentTab {
        private final String sessionId = UUID.randomUUID().toString();
        private final JTextArea area = new JTextArea();
        private final UndoManager undo = new UndoManager();
        private final JScrollPane scrollPane;
        private Path file;
        private boolean modified;
        private boolean suppressEvents;
        private boolean wordWrap = defaultWordWrap;

        private DocumentTab() {
            area.setFont(new Font(currentFontFamily, currentFontStyle, currentFontSize));
            area.setTabSize(4);
            area.setLineWrap(wordWrap);
            area.setWrapStyleWord(wordWrap);
            area.setMargin(new Insets(18, 18, 18, 18));
            area.setDragEnabled(true);

            scrollPane = new JScrollPane(area);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            scrollPane.setRowHeaderView(new LineNumberView(area));

            area.getDocument().addUndoableEditListener(e -> {
                if (!suppressEvents) {
                    undo.addEdit(e.getEdit());
                }
            });

            area.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { changed(); }
                @Override public void removeUpdate(DocumentEvent e) { changed(); }
                @Override public void changedUpdate(DocumentEvent e) { changed(); }

                private void changed() {
                    if (!suppressEvents) {
                        modified = true;
                        updateTabTitle(DocumentTab.this);
                        updateWindowTitle();
                        updateStatus();
                    }
                }
            });

            area.addCaretListener(e -> updateStatus());
            area.addMouseWheelListener(e -> {
                if (e.isControlDown()) {
                    int delta = e.getWheelRotation() < 0 ? 1 : -1;
                    changeFontSize(delta * 2);
                    e.consume();
                }
            });

            installDropTarget(area);
            applyThemeToTab(this);
        }

        private String displayName() {
            return file == null ? "Untitled" : file.getFileName().toString();
        }

        private Path autosaveFile() {
            return AUTOSAVE_DIR.resolve(sessionId + ".autosave");
        }
    }

    // =========================================================
    // MAIN
    // =========================================================

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                logError("Uncaught error in thread: " + thread.getName(), throwable));

        SwingUtilities.invokeLater(() -> {
            try {
                createFolders();
                loadSettings();
                setupLookAndFeel();
                createMainWindow();
                showWorkspace();
                startAutosaveTimer();
                frame.setVisible(true);

                if (args.length > 0) {
                    for (String arg : args) {
                        openPath(Paths.get(arg));
                    }
                } else if (!restoreSession()) {
                    newDocument();
                }
            } catch (Throwable ex) {
                logError("Failed to start DataDocs", ex);
                JOptionPane.showMessageDialog(null,
                        "DataDocs failed to start.\n\n" + safeMessage(ex) + "\n\nLogged to:\n" + ERROR_LOG,
                        "Startup Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            logError("Could not load system Look & Feel", ex);
        }
    }

    private static void createMainWindow() {
        frame = new JFrame(APP_NAME + " " + VERSION);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setMinimumSize(new Dimension(900, 650));
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });
    }

    // =========================================================
    // WORKSPACE
    // =========================================================

    private static void showWorkspace() {
        frame.getContentPane().removeAll();
        frame.getContentPane().setLayout(new BorderLayout());

        tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.addChangeListener(e -> {
            syncFormatControls();
            updateStatus();
            updateWindowTitle();
        });

        toolbar = buildToolbar();
        statusBar = buildStatusBar();
        explorerPanel = buildExplorerPanel();
        workspaceSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, explorerPanel, tabs);
        workspaceSplit.setContinuousLayout(true);
        workspaceSplit.setDividerSize(7);
        workspaceSplit.setResizeWeight(0.0);
        workspaceSplit.setDividerLocation(260);

        frame.setJMenuBar(buildMenuBar());
        frame.add(toolbar, BorderLayout.NORTH);
        frame.add(workspaceSplit, BorderLayout.CENTER);
        frame.add(statusBar, BorderLayout.SOUTH);
        setExplorerVisible(explorerVisible);

        applyTheme();
        frame.revalidate();
        frame.repaint();
    }

    private static JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 7));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, backgroundColor()));

        bar.add(toolButton("New", e -> newDocument()));
        bar.add(toolButton("Open", e -> openDocument()));
        bar.add(toolButton("Folder", e -> chooseExplorerFolder()));
        bar.add(toolButton("Save", e -> saveDocument(activeTab())));
        bar.add(createSeparator());
        bar.add(toolButton("Undo", e -> undo()));
        bar.add(toolButton("Redo", e -> redo()));
        bar.add(createSeparator());
        bar.add(toolButton("Find", e -> showFindDialog(false)));
        bar.add(toolButton("Replace", e -> showFindDialog(true)));
        bar.add(createSeparator());

        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        fontMenu = new JComboBox<>(fonts);
        fontMenu.setPreferredSize(new Dimension(165, 32));
        fontMenu.setFont(UI_FONT);
        fontMenu.setSelectedItem(currentFontFamily);
        fontMenu.addActionListener(e -> {
            String selected = (String) fontMenu.getSelectedItem();
            if (selected != null) {
                currentFontFamily = selected;
                applyEditorFont();
            }
        });
        bar.add(fontMenu);

        Integer[] sizes = {8, 9, 10, 11, 12, 13, 14, 16, 18, 20, 22, 24, 28, 32, 36, 40, 48, 56, 64, 72, 84, 96, 120, 144, 180, 200};
        sizeMenu = new JComboBox<>(sizes);
        sizeMenu.setPreferredSize(new Dimension(68, 32));
        sizeMenu.setFont(UI_FONT);
        sizeMenu.setEditable(true);
        sizeMenu.setSelectedItem(currentFontSize);
        sizeMenu.addActionListener(e -> {
            Object value = sizeMenu.getEditor().getItem();
            try {
                int size = Integer.parseInt(String.valueOf(value));
                setFontSize(size);
            } catch (NumberFormatException ignored) {
            }
        });
        bar.add(sizeMenu);

        boldButton = new JToggleButton("B");
        boldButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        boldButton.setFocusPainted(false);
        boldButton.setToolTipText("Bold");
        boldButton.addActionListener(e -> {
            if (boldButton.isSelected()) currentFontStyle |= Font.BOLD;
            else currentFontStyle &= ~Font.BOLD;
            applyEditorFont();
        });
        bar.add(boldButton);

        italicButton = new JToggleButton("I");
        italicButton.setFont(new Font("SansSerif", Font.ITALIC, 14));
        italicButton.setFocusPainted(false);
        italicButton.setToolTipText("Italic");
        italicButton.addActionListener(e -> {
            if (italicButton.isSelected()) currentFontStyle |= Font.ITALIC;
            else currentFontStyle &= ~Font.ITALIC;
            applyEditorFont();
        });
        bar.add(italicButton);

        bar.add(createSeparator());
        bar.add(toolButton("Command", e -> showCommandPalette()));
        bar.add(toolButton("Stats", e -> showStatistics()));

        return bar;
    }

    private static JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        statusLeft = new JLabel("Ready");
        statusLeft.setFont(SMALL_FONT);
        statusRight = new JLabel(APP_NAME + " " + VERSION);
        statusRight.setFont(SMALL_FONT);

        bar.add(statusLeft, BorderLayout.WEST);
        bar.add(statusRight, BorderLayout.EAST);
        return bar;
    }

    // =========================================================
    // MENU BAR
    // =========================================================

    private static JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.add(menuItem("New Tab", KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), e -> newDocument()));
        fileMenu.add(menuItem("Open...", KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), e -> openDocument()));
        fileMenu.add(menuItem("Open Folder...", KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), e -> chooseExplorerFolder()));

        recentMenu = new JMenu("Open Recent");
        rebuildRecentMenu();
        fileMenu.add(recentMenu);

        fileMenu.addSeparator();
        fileMenu.add(menuItem("Save", KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), e -> saveDocument(activeTab())));
        fileMenu.add(menuItem("Save As...", KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), e -> saveDocumentAs(activeTab())));
        fileMenu.add(menuItem("Save All", KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK), e -> saveAll()));
        fileMenu.addSeparator();
        fileMenu.add(menuItem("Export as HTML...", null, e -> exportHtml()));
        fileMenu.add(menuItem("Print...", KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK), e -> printDocument()));
        fileMenu.addSeparator();
        fileMenu.add(menuItem("Close Tab", KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK), e -> closeActiveTab()));
        fileMenu.add(menuItem("Close Other Tabs", null, e -> closeOtherTabs()));
        fileMenu.addSeparator();
        fileMenu.add(menuItem("Exit", KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK), e -> exitApplication()));

        JMenu editMenu = new JMenu("Edit");
        editMenu.add(menuItem("Undo", KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), e -> undo()));
        editMenu.add(menuItem("Redo", KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), e -> redo()));
        editMenu.addSeparator();
        editMenu.add(editorActionItem("Cut", new DefaultEditorKit.CutAction(), KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK)));
        editMenu.add(editorActionItem("Copy", new DefaultEditorKit.CopyAction(), KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK)));
        editMenu.add(editorActionItem("Paste", new DefaultEditorKit.PasteAction(), KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK)));
        editMenu.add(menuItem("Select All", KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), e -> withArea(JTextArea::selectAll)));
        editMenu.addSeparator();
        editMenu.add(menuItem("Find...", KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), e -> showFindDialog(false)));
        editMenu.add(menuItem("Replace...", KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK), e -> showFindDialog(true)));
        editMenu.add(menuItem("Go to Line...", KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK), e -> goToLine()));
        editMenu.addSeparator();
        editMenu.add(menuItem("Duplicate Line / Selection", KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK), e -> duplicateLineOrSelection()));
        editMenu.add(menuItem("Delete Current Line", KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), e -> deleteCurrentLine()));
        editMenu.add(menuItem("Uppercase Selection", null, e -> transformSelection(true)));
        editMenu.add(menuItem("Lowercase Selection", null, e -> transformSelection(false)));

        JMenu formatMenu = new JMenu("Format");
        wrapMenuItem = new JCheckBoxMenuItem("Word Wrap", defaultWordWrap);
        wrapMenuItem.addActionListener(e -> toggleWordWrap(wrapMenuItem.isSelected()));
        formatMenu.add(wrapMenuItem);
        formatMenu.addSeparator();
        formatMenu.add(menuItem("Zoom In", KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK), e -> changeFontSize(2)));
        formatMenu.add(menuItem("Zoom Out", KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), e -> changeFontSize(-2)));
        formatMenu.add(menuItem("Reset Zoom", KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK), e -> setFontSize(16)));
        formatMenu.addSeparator();
        formatMenu.add(menuItem("Trim Trailing Spaces", null, e -> trimTrailingSpaces()));
        formatMenu.add(menuItem("Tabs to 4 Spaces", null, e -> tabsToSpaces()));

        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.add(menuItem("Command Palette...", KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), e -> showCommandPalette()));
        toolsMenu.add(menuItem("Quick Switch Tab...", KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK), e -> showQuickTabSwitcher()));
        toolsMenu.addSeparator();
        toolsMenu.add(menuItem("Document Statistics", null, e -> showStatistics()));
        toolsMenu.add(menuItem("Insert Date/Time", null, e -> insertDateTime()));
        toolsMenu.add(menuItem("Create Backup Now", null, e -> backupCurrentDocument()));
        toolsMenu.add(menuItem("Recovery Center", null, e -> showRecoveryCenter()));
        toolsMenu.addSeparator();
        toolsMenu.add(menuItem("Open DataDocs Folder", null, e -> openFolder(APP_DATA_DIR)));
        toolsMenu.add(menuItem("Open Error Folder", null, e -> openFolder(ERROR_DIR)));

        JMenu viewMenu = new JMenu("View");
        darkModeMenuItem = new JCheckBoxMenuItem("Dark Mode", darkMode);
        darkModeMenuItem.addActionListener(e -> {
            darkMode = darkModeMenuItem.isSelected();
            applyTheme();
            saveSettings();
        });
        viewMenu.add(darkModeMenuItem);

        explorerMenuItem = new JCheckBoxMenuItem("File Explorer", explorerVisible);
        explorerMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        explorerMenuItem.addActionListener(e -> setExplorerVisible(explorerMenuItem.isSelected()));
        viewMenu.add(explorerMenuItem);

        focusModeMenuItem = new JCheckBoxMenuItem("Focus Mode", focusMode);
        focusModeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0));
        focusModeMenuItem.addActionListener(e -> setFocusMode(focusModeMenuItem.isSelected()));
        viewMenu.add(focusModeMenuItem);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(menuItem("Keyboard Shortcuts", null, e -> showShortcuts()));
        helpMenu.add(menuItem("About DataDocs", null, e -> showAbout()));

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(formatMenu);
        menuBar.add(toolsMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    // =========================================================
    // FILE EXPLORER
    // =========================================================

    private static final class ExplorerNode extends DefaultMutableTreeNode {
        private static final long serialVersionUID = 1L;
        private final transient Path path;
        private boolean loaded;

        private ExplorerNode(Path path) {
            super(path);
            this.path = path;
            if (Files.isDirectory(path)) add(new DefaultMutableTreeNode("Loading..."));
        }

        private void loadChildren() {
            if (loaded || !Files.isDirectory(path)) return;
            loaded = true;
            removeAllChildren();
            try (java.util.stream.Stream<Path> stream = Files.list(path)) {
                List<Path> children = stream.toList();
                children = new ArrayList<>(children);
                children.sort((a, b) -> {
                    boolean ad = Files.isDirectory(a);
                    boolean bd = Files.isDirectory(b);
                    if (ad != bd) return ad ? -1 : 1;
                    return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
                });
                for (Path child : children) {
                    if (!Files.isHidden(child)) add(new ExplorerNode(child));
                }
            } catch (IOException ex) {
                add(new DefaultMutableTreeNode("Could not read folder"));
                logError("Could not read explorer folder: " + path, ex);
            }
        }
    }

    private static JPanel buildExplorerPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setMinimumSize(new Dimension(170, 0));
        panel.setPreferredSize(new Dimension(260, 0));
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, backgroundColor()));

        JPanel header = new JPanel(new BorderLayout(6, 0));
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        explorerRootLabel = new JLabel("FILES");
        explorerRootLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        JButton open = toolButton("+", e -> chooseExplorerFolder());
        open.setToolTipText("Open folder");
        open.setMargin(new Insets(2, 7, 2, 7));
        JButton refresh = toolButton("↻", e -> refreshExplorer());
        refresh.setToolTipText("Refresh explorer");
        refresh.setMargin(new Insets(2, 7, 2, 7));
        buttons.add(open);
        buttons.add(refresh);

        header.add(explorerRootLabel, BorderLayout.CENTER);
        header.add(buttons, BorderLayout.EAST);

        fileTree = new JTree(new DefaultMutableTreeNode("Open a folder to browse files"));
        fileTree.setRootVisible(true);
        fileTree.setShowsRootHandles(true);
        fileTree.setRowHeight(24);
        fileTree.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        fileTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                          boolean expanded, boolean leaf, int row, boolean hasFocus) {
                JLabel label = (JLabel) super.getTreeCellRendererComponent(
                        tree, value, selected, expanded, leaf, row, hasFocus);
                if (value instanceof ExplorerNode node) {
                    Path name = node.path.getFileName();
                    label.setText(name == null ? node.path.toString() : name.toString());
                    label.setToolTipText(node.path.toString());
                }
                return label;
            }
        });

        fileTree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) {
                Object value = event.getPath().getLastPathComponent();
                if (value instanceof ExplorerNode node) {
                    node.loadChildren();
                    ((DefaultTreeModel) fileTree.getModel()).reload(node);
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) {
            }
        });

        fileTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) return;
                TreePath selection = fileTree.getPathForLocation(e.getX(), e.getY());
                if (selection == null) return;
                Object value = selection.getLastPathComponent();
                if (value instanceof ExplorerNode node && Files.isRegularFile(node.path)) {
                    openPath(node.path);
                }
            }
        });

        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(fileTree), BorderLayout.CENTER);

        if (explorerRoot != null && Files.isDirectory(explorerRoot)) {
            setExplorerRoot(explorerRoot);
        }

        return panel;
    }

    private static void chooseExplorerFolder() {
        JFileChooser chooser = new JFileChooser(explorerRoot == null ? COMPANY_DIR.toFile() : explorerRoot.toFile());
        chooser.setDialogTitle("Open Folder in DataDocs");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            setExplorerRoot(chooser.getSelectedFile().toPath());
            setExplorerVisible(true);
            saveSettings();
        }
    }

    private static void setExplorerRoot(Path root) {
        if (fileTree == null || root == null) return;
        explorerRoot = root.toAbsolutePath().normalize();
        ExplorerNode rootNode = new ExplorerNode(explorerRoot);
        rootNode.loadChildren();
        fileTree.setModel(new DefaultTreeModel(rootNode));
        explorerRootLabel.setText(explorerRoot.getFileName() == null ? explorerRoot.toString() : explorerRoot.getFileName().toString());
        fileTree.expandRow(0);
    }

    private static void refreshExplorer() {
        if (explorerRoot != null && Files.isDirectory(explorerRoot)) {
            setExplorerRoot(explorerRoot);
            setStatusMessage("Explorer refreshed");
        }
    }

    private static void setExplorerVisible(boolean visible) {
        explorerVisible = visible;
        if (explorerMenuItem != null) explorerMenuItem.setSelected(visible);
        if (explorerPanel == null || workspaceSplit == null) return;

        explorerPanel.setVisible(visible);
        explorerPanel.setMinimumSize(visible ? new Dimension(170, 0) : new Dimension(0, 0));
        workspaceSplit.setDividerSize(visible ? 7 : 0);
        workspaceSplit.setDividerLocation(visible ? 260 : 0);
        workspaceSplit.revalidate();
        saveSettings();
    }

    // =========================================================
    // COMMAND PALETTE / QUICK SWITCH
    // =========================================================

    private static final class CommandSpec {
        private final String name;
        private final String shortcut;
        private final Runnable action;

        private CommandSpec(String name, String shortcut, Runnable action) {
            this.name = name;
            this.shortcut = shortcut;
            this.action = action;
        }

        @Override
        public String toString() {
            return shortcut == null || shortcut.isBlank() ? name : name + "     " + shortcut;
        }
    }

    private static List<CommandSpec> commands() {
        List<CommandSpec> list = new ArrayList<>();
        list.add(new CommandSpec("New Tab", "Ctrl+N", DataDocs::newDocument));
        list.add(new CommandSpec("Open File", "Ctrl+O", DataDocs::openDocument));
        list.add(new CommandSpec("Open Folder", "Ctrl+Shift+O", DataDocs::chooseExplorerFolder));
        list.add(new CommandSpec("Save", "Ctrl+S", () -> saveDocument(activeTab())));
        list.add(new CommandSpec("Save As", "Ctrl+Shift+S", () -> saveDocumentAs(activeTab())));
        list.add(new CommandSpec("Save All", "Ctrl+Alt+S", DataDocs::saveAll));
        list.add(new CommandSpec("Find", "Ctrl+F", () -> showFindDialog(false)));
        list.add(new CommandSpec("Find and Replace", "Ctrl+H", () -> showFindDialog(true)));
        list.add(new CommandSpec("Go to Line", "Ctrl+G", DataDocs::goToLine));
        list.add(new CommandSpec("Document Statistics", null, DataDocs::showStatistics));
        list.add(new CommandSpec("Create Backup", null, DataDocs::backupCurrentDocument));
        list.add(new CommandSpec("Recovery Center", null, DataDocs::showRecoveryCenter));
        list.add(new CommandSpec("Toggle File Explorer", "Ctrl+Shift+B", () -> setExplorerVisible(!explorerVisible)));
        list.add(new CommandSpec("Toggle Dark Mode", null, () -> {
            darkMode = !darkMode;
            if (darkModeMenuItem != null) darkModeMenuItem.setSelected(darkMode);
            applyTheme();
            saveSettings();
        }));
        list.add(new CommandSpec("Toggle Focus Mode", "F11", () -> setFocusMode(!focusMode)));
        list.add(new CommandSpec("Quick Switch Tab", "Ctrl+K", DataDocs::showQuickTabSwitcher));
        list.add(new CommandSpec("Trim Trailing Spaces", null, DataDocs::trimTrailingSpaces));
        list.add(new CommandSpec("Tabs to 4 Spaces", null, DataDocs::tabsToSpaces));
        list.add(new CommandSpec("Insert Date/Time", null, DataDocs::insertDateTime));
        return list;
    }

    private static void showCommandPalette() {
        List<CommandSpec> all = commands();
        JDialog dialog = new JDialog(frame, "Command Palette", false);
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.setSize(560, 430);
        dialog.setLocationRelativeTo(frame);

        JTextField search = new JTextField();
        search.setFont(new Font("SansSerif", Font.PLAIN, 17));
        search.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 8, 0, 8), search.getBorder()));

        DefaultListModel<CommandSpec> model = new DefaultListModel<>();
        all.forEach(model::addElement);
        JList<CommandSpec> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFont(UI_FONT);
        list.setFixedCellHeight(32);
        if (!model.isEmpty()) list.setSelectedIndex(0);

        Runnable filter = () -> {
            String q = search.getText().trim().toLowerCase(Locale.ROOT);
            model.clear();
            for (CommandSpec command : all) {
                if (q.isEmpty() || command.name.toLowerCase(Locale.ROOT).contains(q)) model.addElement(command);
            }
            if (!model.isEmpty()) list.setSelectedIndex(0);
        };

        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filter.run(); }
            @Override public void removeUpdate(DocumentEvent e) { filter.run(); }
            @Override public void changedUpdate(DocumentEvent e) { filter.run(); }
        });

        Runnable execute = () -> {
            CommandSpec selected = list.getSelectedValue();
            if (selected == null) return;
            dialog.dispose();
            SwingUtilities.invokeLater(selected.action);
        };

        search.addActionListener(e -> execute.run());
        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) execute.run();
            }
        });
        list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "run");
        list.getActionMap().put("run", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { execute.run(); }
        });

        dialog.add(search, BorderLayout.NORTH);
        dialog.add(new JScrollPane(list), BorderLayout.CENTER);
        dialog.setVisible(true);
        search.requestFocusInWindow();
    }

    private static void showQuickTabSwitcher() {
        if (tabs == null || tabs.getTabCount() == 0) return;
        String[] names = new String[tabs.getTabCount()];
        for (int i = 0; i < names.length; i++) {
            DocumentTab tab = tabAt(i);
            names[i] = tab == null ? "Tab " + (i + 1) : tab.displayName() + (tab.modified ? " *" : "");
        }
        Object choice = JOptionPane.showInputDialog(frame, "Switch to:", "Quick Switch Tab",
                JOptionPane.PLAIN_MESSAGE, null, names, names[tabs.getSelectedIndex()]);
        if (choice == null) return;
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(choice)) {
                tabs.setSelectedIndex(i);
                activeAreaRequestFocus();
                return;
            }
        }
    }

    // =========================================================
    // SESSION RESTORE
    // =========================================================

    private static void saveSession() {
        try {
            Files.createDirectories(APP_DATA_DIR);
            Properties p = new Properties();
            int savedIndex = 0;
            int activeSavedIndex = -1;
            DocumentTab active = activeTab();

            for (int i = 0; i < tabs.getTabCount(); i++) {
                DocumentTab tab = tabAt(i);
                if (tab == null || tab.file == null || !Files.isRegularFile(tab.file)) continue;
                p.setProperty("file." + savedIndex, tab.file.toAbsolutePath().normalize().toString());
                p.setProperty("caret." + savedIndex, String.valueOf(tab.area.getCaretPosition()));
                if (tab == active) activeSavedIndex = savedIndex;
                savedIndex++;
            }

            p.setProperty("count", String.valueOf(savedIndex));
            p.setProperty("active", String.valueOf(activeSavedIndex));
            try (java.io.Writer writer = Files.newBufferedWriter(SESSION_FILE, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                p.store(writer, "DataDocs " + VERSION + " session");
            }
        } catch (IOException ex) {
            logError("Could not save session", ex);
        }
    }

    private static boolean restoreSession() {
        if (!Files.isRegularFile(SESSION_FILE)) return false;
        Properties p = new Properties();
        try (java.io.Reader reader = Files.newBufferedReader(SESSION_FILE, StandardCharsets.UTF_8)) {
            p.load(reader);
        } catch (IOException ex) {
            logError("Could not restore session", ex);
            return false;
        }

        int count;
        try { count = Integer.parseInt(p.getProperty("count", "0")); }
        catch (NumberFormatException ex) { count = 0; }
        if (count <= 0) return false;

        List<DocumentTab> restored = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String value = p.getProperty("file." + i);
            if (value == null || value.isBlank()) continue;
            Path path = Paths.get(value);
            if (!Files.isRegularFile(path)) continue;
            int before = tabs.getTabCount();
            openPath(path);
            if (tabs.getTabCount() > before) {
                DocumentTab tab = activeTab();
                restored.add(tab);
                try {
                    int caret = Integer.parseInt(p.getProperty("caret." + i, "0"));
                    tab.area.setCaretPosition(Math.max(0, Math.min(caret, tab.area.getDocument().getLength())));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (restored.isEmpty()) return false;
        try {
            int active = Integer.parseInt(p.getProperty("active", "0"));
            if (active >= 0 && active < restored.size()) {
                tabs.setSelectedComponent(restored.get(active).scrollPane);
            }
        } catch (NumberFormatException ignored) {
        }
        setStatusMessage("Previous session restored");
        return true;
    }

    // =========================================================
    // DOCUMENT OPERATIONS
    // =========================================================

    private static void newDocument() {
        DocumentTab tab = new DocumentTab();
        addTab(tab);
        tab.area.requestFocusInWindow();
    }

    private static void openDocument() {
        JFileChooser chooser = createFileChooser();
        chooser.setDialogTitle("Open Document");
        chooser.setMultiSelectionEnabled(true);
        if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;

        java.io.File[] files = chooser.getSelectedFiles();
        if (files.length == 0 && chooser.getSelectedFile() != null) {
            files = new java.io.File[]{chooser.getSelectedFile()};
        }
        for (java.io.File file : files) openPath(file.toPath());
    }

    private static void openPath(Path input) {
        if (input == null) return;
        Path path = input.toAbsolutePath().normalize();

        if (!Files.isRegularFile(path)) {
            JOptionPane.showMessageDialog(frame, "Not a readable file:\n" + path,
                    "Open", JOptionPane.WARNING_MESSAGE);
            return;
        }

        for (int i = 0; i < tabs.getTabCount(); i++) {
            DocumentTab existing = tabAt(i);
            if (existing != null && existing.file != null && existing.file.toAbsolutePath().normalize().equals(path)) {
                tabs.setSelectedIndex(i);
                existing.area.requestFocusInWindow();
                return;
            }
        }

        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            DocumentTab tab = new DocumentTab();
            tab.file = path;
            setDocumentText(tab, content);
            tab.modified = false;
            tab.undo.discardAllEdits();
            addTab(tab);
            addRecent(path);
            setStatusMessage("Opened: " + path.getFileName());
        } catch (IOException ex) {
            logError("Failed to open document: " + path, ex);
            showError("Could not open the file.", ex);
        }
    }

    private static void addTab(DocumentTab tab) {
        ensureRegistered(tab);
        tabs.addTab(tab.displayName(), tab.scrollPane);
        int index = tabs.indexOfComponent(tab.scrollPane);
        tabs.setTabComponentAt(index, createTabHeader(tab));
        tabs.setSelectedIndex(index);
        updateWindowTitle();
        updateStatus();
    }

    private static JPanel createTabHeader(DocumentTab tab) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        panel.setOpaque(false);

        JLabel label = new JLabel(tab.displayName());
        label.putClientProperty("tab", tab);
        panel.add(label);

        JButton close = new JButton("×");
        close.setMargin(new Insets(0, 4, 0, 4));
        close.setBorderPainted(false);
        close.setContentAreaFilled(false);
        close.setFocusPainted(false);
        close.setToolTipText("Close tab");
        close.addActionListener(e -> closeTab(tab));
        panel.add(close);

        return panel;
    }

    private static void updateTabTitle(DocumentTab tab) {
        int index = tabs.indexOfComponent(tab.scrollPane);
        if (index < 0) return;

        Component component = tabs.getTabComponentAt(index);
        if (component instanceof JPanel panel) {
            for (Component child : panel.getComponents()) {
                if (child instanceof JLabel label) {
                    label.setText(tab.displayName() + (tab.modified ? " *" : ""));
                    break;
                }
            }
        }
    }

    private static boolean saveDocument(DocumentTab tab) {
        if (tab == null) return false;
        if (tab.file == null) return saveDocumentAs(tab);
        return writeDocument(tab, tab.file);
    }

    private static boolean saveDocumentAs(DocumentTab tab) {
        if (tab == null) return false;

        JFileChooser chooser = createFileChooser();
        chooser.setDialogTitle("Save Document As");
        if (tab.file != null) chooser.setSelectedFile(tab.file.toFile());

        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return false;

        Path selected = chooser.getSelectedFile().toPath();
        if (!hasExtension(selected.getFileName().toString())) {
            selected = Paths.get(selected.toString() + ".txt");
        }

        if (Files.exists(selected) && (tab.file == null || !sameFileSafe(tab.file, selected))) {
            int overwrite = JOptionPane.showConfirmDialog(frame,
                    "That file already exists. Replace it?", "Confirm Replace",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (overwrite != JOptionPane.YES_OPTION) return false;
        }

        tab.file = selected.toAbsolutePath().normalize();
        return writeDocument(tab, tab.file);
    }

    private static boolean writeDocument(DocumentTab tab, Path file) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);

            if (Files.exists(file)) createBackup(file);

            Path temp = file.resolveSibling(file.getFileName() + ".datadocs.tmp");
            Files.writeString(temp, tab.area.getText(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }

            tab.file = file.toAbsolutePath().normalize();
            tab.modified = false;
            updateTabTitle(tab);
            updateWindowTitle();
            updateStatus();
            addRecent(tab.file);
            Files.deleteIfExists(tab.autosaveFile());
            setStatusMessage("Saved: " + tab.file.getFileName());
            return true;
        } catch (IOException ex) {
            logError("Failed to save document: " + file, ex);
            showError("Could not save the document.", ex);
            return false;
        }
    }

    private static void saveAll() {
        int saved = 0;
        for (int i = 0; i < tabs.getTabCount(); i++) {
            DocumentTab tab = tabAt(i);
            if (tab != null && tab.modified) {
                tabs.setSelectedIndex(i);
                if (!saveDocument(tab)) return;
                saved++;
            }
        }
        setStatusMessage(saved == 0 ? "Nothing to save" : "Saved " + saved + " document(s)");
    }

    private static void printDocument() {
        DocumentTab tab = activeTab();
        if (tab == null) return;

        new Thread(() -> {
            try {
                boolean complete = tab.area.print();
                if (complete) SwingUtilities.invokeLater(() -> setStatusMessage("Printing complete"));
            } catch (PrinterException ex) {
                logError("Failed to print document", ex);
                SwingUtilities.invokeLater(() -> showError("Printing failed.", ex));
            }
        }, "DataDocs-Printer").start();
    }

    private static void exportHtml() {
        DocumentTab tab = activeTab();
        if (tab == null) return;

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export as HTML");
        chooser.setFileFilter(new FileNameExtensionFilter("HTML Files (*.html)", "html", "htm"));
        String base = tab.file == null ? "document" : stripExtension(tab.file.getFileName().toString());
        chooser.setSelectedFile(new java.io.File(base + ".html"));

        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        Path target = chooser.getSelectedFile().toPath();
        if (!target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".html")) {
            target = Paths.get(target.toString() + ".html");
        }

        String escaped = escapeHtml(tab.area.getText());
        String html = "<!doctype html>\n<html><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>" + escapeHtml(tab.displayName()) + "</title>"
                + "<style>body{max-width:900px;margin:40px auto;padding:0 20px;font-family:system-ui,sans-serif;}"
                + "pre{white-space:pre-wrap;word-wrap:break-word;font:16px/1.5 monospace;}</style></head>"
                + "<body><pre>" + escaped + "</pre></body></html>";

        try {
            Files.writeString(target, html, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            setStatusMessage("Exported HTML: " + target.getFileName());
        } catch (IOException ex) {
            logError("HTML export failed: " + target, ex);
            showError("Could not export HTML.", ex);
        }
    }

    // =========================================================
    // TAB CLOSING
    // =========================================================

    private static void closeActiveTab() {
        DocumentTab tab = activeTab();
        if (tab != null) closeTab(tab);
    }

    private static boolean closeTab(DocumentTab tab) {
        if (tab.modified && !confirmSave(tab)) return false;
        int index = tabs.indexOfComponent(tab.scrollPane);
        if (index >= 0) tabs.removeTabAt(index);
        try { Files.deleteIfExists(tab.autosaveFile()); } catch (IOException ignored) {}
        if (tabs.getTabCount() == 0) newDocument();
        updateWindowTitle();
        updateStatus();
        return true;
    }

    private static void closeOtherTabs() {
        DocumentTab keep = activeTab();
        if (keep == null) return;
        List<DocumentTab> toClose = new ArrayList<>();
        for (int i = 0; i < tabs.getTabCount(); i++) {
            DocumentTab tab = tabAt(i);
            if (tab != null && tab != keep) toClose.add(tab);
        }
        for (DocumentTab tab : toClose) {
            if (!closeTab(tab)) return;
        }
    }

    private static boolean confirmSave(DocumentTab tab) {
        if (!tab.modified) return true;

        int choice = JOptionPane.showOptionDialog(frame,
                "Save changes to \"" + tab.displayName() + "\"?",
                "Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE, null,
                new Object[]{"Save", "Discard", "Cancel"}, "Save");

        if (choice == 0) return saveDocument(tab);
        if (choice == 1) return true;
        return false;
    }

    private static void exitApplication() {
        List<DocumentTab> all = new ArrayList<>();
        for (int i = 0; i < tabs.getTabCount(); i++) {
            DocumentTab tab = tabAt(i);
            if (tab != null) all.add(tab);
        }

        for (DocumentTab tab : all) {
            tabs.setSelectedComponent(tab.scrollPane);
            if (tab.modified && !confirmSave(tab)) return;
        }

        if (autosaveTimer != null) autosaveTimer.stop();
        saveSession();
        saveSettings();
        frame.dispose();
        System.exit(0);
    }

    // =========================================================
    // UNDO / REDO
    // =========================================================

    private static void undo() {
        DocumentTab tab = activeTab();
        if (tab == null) return;
        try {
            if (tab.undo.canUndo()) tab.undo.undo();
        } catch (Exception ex) {
            logError("Undo failed", ex);
        }
    }

    private static void redo() {
        DocumentTab tab = activeTab();
        if (tab == null) return;
        try {
            if (tab.undo.canRedo()) tab.undo.redo();
        } catch (Exception ex) {
            logError("Redo failed", ex);
        }
    }

    // =========================================================
    // FIND / REPLACE
    // =========================================================

    private static void showFindDialog(boolean showReplace) {
        DocumentTab tab = activeTab();
        if (tab == null) return;

        JDialog dialog = new JDialog(frame, showReplace ? "Find and Replace" : "Find", false);
        dialog.setLayout(new GridBagLayout());
        dialog.setSize(showReplace ? 500 : 460, showReplace ? 245 : 190);
        dialog.setLocationRelativeTo(frame);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField findField = new JTextField(26);
        JTextField replaceField = new JTextField(26);
        JCheckBox caseSensitive = new JCheckBox("Match case");
        JCheckBox wholeWord = new JCheckBox("Whole word");

        gbc.gridx = 0; gbc.gridy = 0;
        dialog.add(new JLabel("Find:"), gbc);
        gbc.gridx = 1;
        dialog.add(findField, gbc);

        int row = 1;
        if (showReplace) {
            gbc.gridx = 0; gbc.gridy = row;
            dialog.add(new JLabel("Replace:"), gbc);
            gbc.gridx = 1;
            dialog.add(replaceField, gbc);
            row++;
        }

        JPanel options = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        options.add(caseSensitive);
        options.add(wholeWord);
        gbc.gridx = 1; gbc.gridy = row++;
        dialog.add(options, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton findNext = new JButton("Find Next");
        findNext.addActionListener(e -> findNext(findField.getText(), caseSensitive.isSelected(), wholeWord.isSelected()));
        buttons.add(findNext);

        if (showReplace) {
            JButton replace = new JButton("Replace");
            replace.addActionListener(e -> replaceCurrent(findField.getText(), replaceField.getText(), caseSensitive.isSelected(), wholeWord.isSelected()));
            buttons.add(replace);

            JButton replaceAll = new JButton("Replace All");
            replaceAll.addActionListener(e -> replaceAll(findField.getText(), replaceField.getText(), caseSensitive.isSelected(), wholeWord.isSelected()));
            buttons.add(replaceAll);
        }

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        dialog.add(buttons, gbc);
        dialog.getRootPane().setDefaultButton(findNext);
        dialog.setVisible(true);
        findField.requestFocusInWindow();
    }

    private static boolean findNext(String needle, boolean caseSensitive, boolean wholeWord) {
        DocumentTab tab = activeTab();
        if (tab == null || needle == null || needle.isEmpty()) return false;

        JTextArea area = tab.area;
        String text = area.getText();
        String haystack = caseSensitive ? text : text.toLowerCase(Locale.ROOT);
        String search = caseSensitive ? needle : needle.toLowerCase(Locale.ROOT);
        int start = Math.max(area.getSelectionEnd(), area.getCaretPosition());

        int index = findIndex(haystack, search, start, wholeWord);
        if (index < 0 && start > 0) index = findIndex(haystack, search, 0, wholeWord);

        if (index >= 0) {
            area.requestFocusInWindow();
            area.select(index, index + needle.length());
            return true;
        }

        Toolkit.getDefaultToolkit().beep();
        setStatusMessage("Text not found");
        return false;
    }

    private static int findIndex(String text, String needle, int from, boolean wholeWord) {
        int index = text.indexOf(needle, from);
        while (index >= 0 && wholeWord) {
            boolean left = index == 0 || !Character.isLetterOrDigit(text.charAt(index - 1)) && text.charAt(index - 1) != '_';
            int end = index + needle.length();
            boolean right = end >= text.length() || !Character.isLetterOrDigit(text.charAt(end)) && text.charAt(end) != '_';
            if (left && right) return index;
            index = text.indexOf(needle, index + 1);
        }
        return index;
    }

    private static void replaceCurrent(String needle, String replacement, boolean caseSensitive, boolean wholeWord) {
        DocumentTab tab = activeTab();
        if (tab == null) return;

        String selected = tab.area.getSelectedText();
        if (selected != null) {
            boolean matches = caseSensitive ? selected.equals(needle) : selected.equalsIgnoreCase(needle);
            if (matches && (!wholeWord || selected.length() == needle.length())) {
                tab.area.replaceSelection(replacement);
            }
        }
        findNext(needle, caseSensitive, wholeWord);
    }

    private static void replaceAll(String needle, String replacement, boolean caseSensitive, boolean wholeWord) {
        DocumentTab tab = activeTab();
        if (tab == null || needle == null || needle.isEmpty()) return;

        String text = tab.area.getText();
        String searchText = caseSensitive ? text : text.toLowerCase(Locale.ROOT);
        String searchNeedle = caseSensitive ? needle : needle.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder();
        int from = 0;
        int count = 0;

        while (true) {
            int index = findIndex(searchText, searchNeedle, from, wholeWord);
            if (index < 0) break;
            out.append(text, from, index).append(replacement);
            from = index + needle.length();
            count++;
        }
        out.append(text.substring(from));

        if (count > 0) {
            tab.area.setText(out.toString());
            setStatusMessage("Replaced " + count + " occurrence(s)");
        } else {
            setStatusMessage("Nothing replaced");
        }
    }

    // =========================================================
    // EDITING TOOLS
    // =========================================================

    private static void goToLine() {
        DocumentTab tab = activeTab();
        if (tab == null) return;

        String input = JOptionPane.showInputDialog(frame, "Line number:", "Go to Line", JOptionPane.QUESTION_MESSAGE);
        if (input == null) return;

        try {
            int line = Integer.parseInt(input.trim());
            if (line < 1 || line > tab.area.getLineCount()) throw new NumberFormatException();
            int offset = tab.area.getLineStartOffset(line - 1);
            tab.area.setCaretPosition(offset);
            tab.area.requestFocusInWindow();
        } catch (NumberFormatException | BadLocationException ex) {
            JOptionPane.showMessageDialog(frame, "Enter a valid line number.", "Invalid Line", JOptionPane.WARNING_MESSAGE);
        }
    }

    private static void duplicateLineOrSelection() {
        DocumentTab tab = activeTab();
        if (tab == null) return;
        JTextArea area = tab.area;

        try {
            String selected = area.getSelectedText();
            if (selected != null && !selected.isEmpty()) {
                int end = area.getSelectionEnd();
                area.insert(selected, end);
                area.select(end, end + selected.length());
                return;
            }

            int caret = area.getCaretPosition();
            int line = area.getLineOfOffset(caret);
            int start = area.getLineStartOffset(line);
            int end = area.getLineEndOffset(line);
            String lineText = area.getText(start, end - start);
            area.insert(lineText, end);
        } catch (BadLocationException ex) {
            logError("Duplicate line failed", ex);
        }
    }

    private static void deleteCurrentLine() {
        DocumentTab tab = activeTab();
        if (tab == null) return;
        JTextArea area = tab.area;
        try {
            int line = area.getLineOfOffset(area.getCaretPosition());
            int start = area.getLineStartOffset(line);
            int end = area.getLineEndOffset(line);
            area.replaceRange("", start, end);
        } catch (BadLocationException ex) {
            logError("Delete line failed", ex);
        }
    }

    private static void transformSelection(boolean uppercase) {
        DocumentTab tab = activeTab();
        if (tab == null) return;
        String selected = tab.area.getSelectedText();
        if (selected == null || selected.isEmpty()) return;
        String replacement = uppercase ? selected.toUpperCase(Locale.ROOT) : selected.toLowerCase(Locale.ROOT);
        tab.area.replaceSelection(replacement);
    }

    private static void trimTrailingSpaces() {
        DocumentTab tab = activeTab();
        if (tab == null) return;
        String[] lines = tab.area.getText().split("\\R", -1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            out.append(lines[i].replaceFirst("[ \\t]+$", ""));
            if (i < lines.length - 1) out.append('\n');
        }
        tab.area.setText(out.toString());
        setStatusMessage("Trailing spaces removed");
    }

    private static void tabsToSpaces() {
        DocumentTab tab = activeTab();
        if (tab == null) return;
        tab.area.setText(tab.area.getText().replace("\t", "    "));
        setStatusMessage("Tabs converted to spaces");
    }

    private static void insertDateTime() {
        DocumentTab tab = activeTab();
        if (tab == null) return;
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        tab.area.insert(stamp, tab.area.getCaretPosition());
    }

    private static void showStatistics() {
        DocumentTab tab = activeTab();
        if (tab == null) return;
        String text = tab.area.getText();
        int chars = text.length();
        int charsNoSpaces = text.replaceAll("\\s", "").length();
        int words = countWords(text);
        int lines = tab.area.getLineCount();
        int paragraphs = countParagraphs(text);
        int selected = tab.area.getSelectedText() == null ? 0 : tab.area.getSelectedText().length();
        double minutes = words / 200.0;

        JOptionPane.showMessageDialog(frame,
                "Words: " + words
                        + "\nCharacters: " + chars
                        + "\nCharacters (no spaces): " + charsNoSpaces
                        + "\nLines: " + lines
                        + "\nParagraphs: " + paragraphs
                        + "\nSelected characters: " + selected
                        + "\nEstimated reading time: " + String.format(Locale.ROOT, "%.1f min", minutes),
                "Document Statistics", JOptionPane.INFORMATION_MESSAGE);
    }

    // =========================================================
    // FONT / FORMAT
    // =========================================================

    private static void applyEditorFont() {
        DocumentTab tab = activeTab();
        if (tab != null) {
            tab.area.setFont(new Font(currentFontFamily, currentFontStyle, currentFontSize));
            Component row = tab.scrollPane.getRowHeader().getView();
            if (row != null) row.repaint();
        }
        saveSettings();
        updateStatus();
    }

    private static void changeFontSize(int amount) {
        setFontSize(Math.max(8, Math.min(200, currentFontSize + amount)));
    }

    private static void setFontSize(int size) {
        currentFontSize = Math.max(8, Math.min(200, size));
        if (sizeMenu != null) sizeMenu.setSelectedItem(currentFontSize);
        applyEditorFont();
    }

    private static void toggleWordWrap(boolean enabled) {
        DocumentTab tab = activeTab();
        if (tab == null) return;
        tab.wordWrap = enabled;
        tab.area.setLineWrap(enabled);
        tab.area.setWrapStyleWord(enabled);
        defaultWordWrap = enabled;
        saveSettings();
    }

    private static void syncFormatControls() {
        DocumentTab tab = activeTab();
        if (tab == null) return;
        Font font = tab.area.getFont();
        currentFontFamily = font.getFamily();
        currentFontSize = font.getSize();
        currentFontStyle = font.getStyle();

        if (fontMenu != null) fontMenu.setSelectedItem(currentFontFamily);
        if (sizeMenu != null) sizeMenu.setSelectedItem(currentFontSize);
        if (boldButton != null) boldButton.setSelected((currentFontStyle & Font.BOLD) != 0);
        if (italicButton != null) italicButton.setSelected((currentFontStyle & Font.ITALIC) != 0);
        if (wrapMenuItem != null) wrapMenuItem.setSelected(tab.wordWrap);
    }

    // =========================================================
    // AUTOSAVE / BACKUPS / RECOVERY
    // =========================================================

    private static void startAutosaveTimer() {
        autosaveTimer = new Timer(20_000, e -> autosaveAll());
        autosaveTimer.setRepeats(true);
        autosaveTimer.start();
    }

    private static void autosaveAll() {
        if (tabs == null) return;
        int count = 0;
        for (int i = 0; i < tabs.getTabCount(); i++) {
            DocumentTab tab = tabAt(i);
            if (tab == null || !tab.modified) continue;
            try {
                Files.createDirectories(AUTOSAVE_DIR);
                String header = "# DATADOCS_AUTOSAVE_V3\n"
                        + "# file=" + (tab.file == null ? "" : tab.file.toString().replace("\n", "")) + "\n"
                        + "# time=" + LocalDateTime.now() + "\n"
                        + "# ---\n";
                Files.writeString(tab.autosaveFile(), header + tab.area.getText(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                count++;
            } catch (IOException ex) {
                logError("Autosave failed", ex);
            }
        }
        if (count > 0) setStatusMessage("Autosaved " + count + " document(s)");
    }

    private static void backupCurrentDocument() {
        DocumentTab tab = activeTab();
        if (tab == null) return;
        try {
            Files.createDirectories(BACKUP_DIR);
            String base = tab.file == null ? "Untitled" : tab.file.getFileName().toString();
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Path backup = BACKUP_DIR.resolve(sanitizeFileName(base) + "-" + stamp + ".bak.txt");
            Files.writeString(backup, tab.area.getText(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW);
            setStatusMessage("Backup created: " + backup.getFileName());
        } catch (IOException ex) {
            logError("Manual backup failed", ex);
            showError("Could not create the backup.", ex);
        }
    }

    private static void createBackup(Path file) {
        try {
            if (!Files.isRegularFile(file)) return;
            Files.createDirectories(BACKUP_DIR);
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));
            Path target = BACKUP_DIR.resolve(sanitizeFileName(file.getFileName().toString()) + "-" + stamp + ".bak");
            Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
            pruneBackups(50);
        } catch (IOException ex) {
            logError("Automatic backup failed for: " + file, ex);
        }
    }

    private static void pruneBackups(int max) {
        try {
            if (!Files.isDirectory(BACKUP_DIR)) return;
            List<Path> files = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(BACKUP_DIR)) {
                for (Path p : stream) if (Files.isRegularFile(p)) files.add(p);
            }
            files.sort((a, b) -> {
                try { return Files.getLastModifiedTime(a).compareTo(Files.getLastModifiedTime(b)); }
                catch (IOException e) { return 0; }
            });
            while (files.size() > max) {
                Files.deleteIfExists(files.remove(0));
            }
        } catch (IOException ex) {
            logError("Could not prune backups", ex);
        }
    }

    private static void showRecoveryCenter() {
        try {
            Files.createDirectories(AUTOSAVE_DIR);
            List<Path> autosaves = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(AUTOSAVE_DIR, "*.autosave")) {
                for (Path p : stream) autosaves.add(p);
            }
            autosaves.sort(Collections.reverseOrder());

            if (autosaves.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "No autosave files were found.",
                        "Recovery Center", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            DefaultListModel<Path> model = new DefaultListModel<>();
            autosaves.forEach(model::addElement);
            JList<Path> list = new JList<>(model);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setCellRenderer((jList, value, index, isSelected, cellHasFocus) -> {
                JLabel label = new JLabel(value.getFileName().toString());
                label.setOpaque(true);
                if (isSelected) {
                    label.setBackground(jList.getSelectionBackground());
                    label.setForeground(jList.getSelectionForeground());
                } else {
                    label.setBackground(jList.getBackground());
                    label.setForeground(jList.getForeground());
                }
                label.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
                return label;
            });

            Object[] options = {"Recover", "Delete", "Close"};
            while (true) {
                int choice = JOptionPane.showOptionDialog(frame, new JScrollPane(list),
                        "Recovery Center", JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE, null, options, "Recover");
                if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) break;
                Path selected = list.getSelectedValue();
                if (selected == null) continue;

                if (choice == 0) {
                    recoverAutosave(selected);
                    break;
                } else if (choice == 1) {
                    Files.deleteIfExists(selected);
                    model.removeElement(selected);
                    if (model.isEmpty()) break;
                }
            }
        } catch (IOException ex) {
            logError("Recovery Center failed", ex);
            showError("Could not open Recovery Center.", ex);
        }
    }

    private static void recoverAutosave(Path file) {
        try {
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            int marker = raw.indexOf("# ---\n");
            String text = marker >= 0 ? raw.substring(marker + 6) : raw;
            DocumentTab tab = new DocumentTab();
            setDocumentText(tab, text);
            tab.modified = true;
            addTab(tab);
            updateTabTitle(tab);
            setStatusMessage("Recovered autosave");
        } catch (IOException ex) {
            logError("Failed to recover autosave: " + file, ex);
            showError("Could not recover autosave.", ex);
        }
    }

    // =========================================================
    // STATUS / TITLE
    // =========================================================

    private static void updateStatus() {
        if (statusLeft == null || statusRight == null) return;
        DocumentTab tab = activeTab();
        if (tab == null) {
            statusLeft.setText("No document");
            statusRight.setText(APP_NAME + " " + VERSION);
            return;
        }

        JTextArea area = tab.area;
        String text = area.getText();
        int line = 1;
        int col = 1;
        try {
            int caret = area.getCaretPosition();
            line = area.getLineOfOffset(caret) + 1;
            col = caret - area.getLineStartOffset(line - 1) + 1;
        } catch (BadLocationException ignored) {
        }

        int selection = area.getSelectionStart() == area.getSelectionEnd()
                ? 0 : Math.abs(area.getSelectionEnd() - area.getSelectionStart());
        int words = countWords(text);
        int chars = text.length();
        int lines = area.getLineCount();

        statusLeft.setText("Ln " + line + ", Col " + col
                + "   |   Lines " + lines
                + "   |   Words " + words
                + "   |   Chars " + chars
                + (selection > 0 ? "   |   Selected " + selection : ""));

        statusRight.setText((tab.modified ? "Modified" : "Saved")
                + "   |   UTF-8   |   " + currentFontSize + " px");
    }

    private static void setStatusMessage(String message) {
        if (statusLeft == null) return;
        statusLeft.setText(message);
        if (statusTimer != null) statusTimer.stop();
        statusTimer = new Timer(2200, e -> updateStatus());
        statusTimer.setRepeats(false);
        statusTimer.start();
    }

    private static void updateWindowTitle() {
        if (frame == null) return;
        DocumentTab tab = activeTab();
        if (tab == null) {
            frame.setTitle(APP_NAME + " " + VERSION);
        } else {
            frame.setTitle(tab.displayName() + (tab.modified ? " *" : "") + " - " + APP_NAME);
        }
    }

    // =========================================================
    // RECENT FILES / SETTINGS
    // =========================================================

    private static void addRecent(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        recentFiles.remove(normalized);
        recentFiles.add(0, normalized);
        while (recentFiles.size() > MAX_RECENT) recentFiles.remove(recentFiles.size() - 1);
        rebuildRecentMenu();
        saveSettings();
    }

    private static void rebuildRecentMenu() {
        if (recentMenu == null) return;
        recentMenu.removeAll();
        recentFiles.removeIf(path -> !Files.isRegularFile(path));

        if (recentFiles.isEmpty()) {
            JMenuItem empty = new JMenuItem("No recent files");
            empty.setEnabled(false);
            recentMenu.add(empty);
        } else {
            for (Path path : recentFiles) {
                JMenuItem item = new JMenuItem(path.toString());
                item.addActionListener(e -> openPath(path));
                recentMenu.add(item);
            }
            recentMenu.addSeparator();
            recentMenu.add(menuItem("Clear Recent Files", null, e -> {
                recentFiles.clear();
                rebuildRecentMenu();
                saveSettings();
            }));
        }
    }

    private static void loadSettings() {
        Properties p = new Properties();
        try {
            if (Files.isRegularFile(SETTINGS_FILE)) {
                try (java.io.Reader reader = Files.newBufferedReader(SETTINGS_FILE, StandardCharsets.UTF_8)) {
                    p.load(reader);
                }
            }
        } catch (IOException ex) {
            logError("Could not load settings", ex);
        }

        darkMode = Boolean.parseBoolean(p.getProperty("darkMode", "true"));
        defaultWordWrap = Boolean.parseBoolean(p.getProperty("wordWrap", "true"));
        explorerVisible = Boolean.parseBoolean(p.getProperty("explorerVisible", "true"));
        String explorerPath = p.getProperty("explorerRoot", "");
        if (!explorerPath.isBlank()) {
            try {
                Path candidate = Paths.get(explorerPath);
                if (Files.isDirectory(candidate)) explorerRoot = candidate.toAbsolutePath().normalize();
            } catch (InvalidPathException ignored) {
            }
        }
        currentFontFamily = p.getProperty("fontFamily", "Monospaced");
        try { currentFontSize = Integer.parseInt(p.getProperty("fontSize", "16")); }
        catch (NumberFormatException ignored) { currentFontSize = 16; }
        try { currentFontStyle = Integer.parseInt(p.getProperty("fontStyle", String.valueOf(Font.PLAIN))); }
        catch (NumberFormatException ignored) { currentFontStyle = Font.PLAIN; }

        recentFiles.clear();
        for (int i = 0; i < MAX_RECENT; i++) {
            String value = p.getProperty("recent." + i);
            if (value != null && !value.isBlank()) recentFiles.add(Paths.get(value));
        }
    }

    private static void saveSettings() {
        try {
            Files.createDirectories(APP_DATA_DIR);
            Properties p = new Properties();
            p.setProperty("darkMode", String.valueOf(darkMode));
            p.setProperty("wordWrap", String.valueOf(defaultWordWrap));
            p.setProperty("fontFamily", currentFontFamily);
            p.setProperty("fontSize", String.valueOf(currentFontSize));
            p.setProperty("fontStyle", String.valueOf(currentFontStyle));
            p.setProperty("explorerVisible", String.valueOf(explorerVisible));
            if (explorerRoot != null) p.setProperty("explorerRoot", explorerRoot.toString());
            for (int i = 0; i < recentFiles.size() && i < MAX_RECENT; i++) {
                p.setProperty("recent." + i, recentFiles.get(i).toString());
            }
            try (java.io.Writer writer = Files.newBufferedWriter(SETTINGS_FILE, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                p.store(writer, "DataDocs " + VERSION + " settings");
            }
        } catch (IOException ex) {
            logError("Could not save settings", ex);
        }
    }

    // =========================================================
    // THEME / VIEW
    // =========================================================

    private static void applyTheme() {
        if (frame == null) return;
        frame.getContentPane().setBackground(backgroundColor());

        if (toolbar != null) {
            toolbar.setBackground(panelColor());
            toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, backgroundColor()));
            themeChildren(toolbar);
        }
        if (statusBar != null) {
            statusBar.setBackground(panelColor());
            themeChildren(statusBar);
        }
        if (statusLeft != null) statusLeft.setForeground(mutedColor());
        if (statusRight != null) statusRight.setForeground(mutedColor());

        if (tabs != null) {
            tabs.setBackground(panelColor());
            tabs.setForeground(textColor());
            for (int i = 0; i < tabs.getTabCount(); i++) {
                DocumentTab tab = tabAt(i);
                if (tab != null) applyThemeToTab(tab);
            }
        }
        if (explorerPanel != null) {
            explorerPanel.setBackground(panelColor());
            explorerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, backgroundColor()));
            themeChildren(explorerPanel);
        }
        if (fileTree != null) {
            fileTree.setBackground(panelColor());
            fileTree.setForeground(textColor());
        }
        if (explorerRootLabel != null) explorerRootLabel.setForeground(textColor());

        if (darkModeMenuItem != null) darkModeMenuItem.setSelected(darkMode);
        frame.repaint();
    }

    private static void themeChildren(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JLabel label) {
                label.setForeground(textColor());
            } else if (c instanceof JPanel panel) {
                panel.setBackground(panelColor());
                themeChildren(panel);
            }
        }
    }

    private static void applyThemeToTab(DocumentTab tab) {
        tab.area.setBackground(backgroundColor());
        tab.area.setForeground(textColor());
        tab.area.setCaretColor(textColor());
        tab.area.setSelectionColor(new Color(55, 105, 160));
        tab.area.setSelectedTextColor(Color.WHITE);
        tab.scrollPane.getViewport().setBackground(backgroundColor());
        Component row = tab.scrollPane.getRowHeader().getView();
        if (row != null) row.repaint();
    }

    private static void setFocusMode(boolean enabled) {
        focusMode = enabled;
        if (toolbar != null) toolbar.setVisible(!enabled);
        if (statusBar != null) statusBar.setVisible(!enabled);
        if (frame.getJMenuBar() != null) frame.getJMenuBar().setVisible(!enabled);
        if (explorerPanel != null && workspaceSplit != null) {
            if (enabled) {
                explorerPanel.setVisible(false);
                explorerPanel.setMinimumSize(new Dimension(0, 0));
                workspaceSplit.setDividerSize(0);
                workspaceSplit.setDividerLocation(0);
            } else {
                setExplorerVisible(explorerVisible);
            }
        }
        if (focusModeMenuItem != null) focusModeMenuItem.setSelected(enabled);
        frame.revalidate();
        activeAreaRequestFocus();
    }

    private static Color backgroundColor() { return darkMode ? DARK_BG : LIGHT_BG; }
    private static Color panelColor() { return darkMode ? DARK_PANEL : LIGHT_PANEL; }
    private static Color secondaryPanelColor() { return darkMode ? DARK_PANEL_2 : LIGHT_PANEL_2; }
    private static Color textColor() { return darkMode ? DARK_TEXT : LIGHT_TEXT; }
    private static Color mutedColor() { return darkMode ? DARK_MUTED : LIGHT_MUTED; }

    // =========================================================
    // DRAG AND DROP
    // =========================================================

    private static void installDropTarget(JTextArea area) {
        try {
            new DropTarget(area, new java.awt.dnd.DropTargetAdapter() {
                @Override
                public void drop(DropTargetDropEvent event) {
                    try {
                        if (!event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                            event.rejectDrop();
                            return;
                        }
                        event.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY);
                        @SuppressWarnings("unchecked")
                        List<java.io.File> files = (List<java.io.File>) event.getTransferable()
                                .getTransferData(DataFlavor.javaFileListFlavor);
                        for (java.io.File file : files) openPath(file.toPath());
                        event.dropComplete(true);
                    } catch (Exception ex) {
                        logError("Drag-and-drop open failed", ex);
                        event.dropComplete(false);
                    }
                }
            });
        } catch (HeadlessException ignored) {
        }
    }

    // =========================================================
    // ERROR SYSTEM
    // =========================================================

    private static void createFolders() {
        try {
            Files.createDirectories(COMPANY_DIR);
            Files.createDirectories(ERROR_DIR);
            Files.createDirectories(APP_DATA_DIR);
            Files.createDirectories(AUTOSAVE_DIR);
            Files.createDirectories(BACKUP_DIR);
        } catch (IOException ex) {
            System.err.println("Could not create DataDocs folders:");
            ex.printStackTrace();
        }
    }

    private static synchronized void logError(String message, Throwable error) {
        try {
            Files.createDirectories(ERROR_DIR);
            StringWriter stackWriter = new StringWriter();
            error.printStackTrace(new PrintWriter(stackWriter));

            String logEntry = "\n============================================================\n"
                    + "DATADOCS ERROR\n"
                    + "============================================================\n"
                    + "Time: " + LocalDateTime.now() + "\n"
                    + "Version: " + VERSION + "\n"
                    + "Message: " + message + "\n"
                    + "Type: " + error.getClass().getName() + "\n"
                    + "Details: " + safeMessage(error) + "\n\n"
                    + "Stack Trace:\n" + stackWriter
                    + "============================================================\n";

            Files.writeString(ERROR_LOG, logEntry, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException loggingError) {
            System.err.println("DataDocs could not write to " + ERROR_LOG);
            loggingError.printStackTrace();
            error.printStackTrace();
        }
    }

    private static void showError(String message, Throwable ex) {
        JOptionPane.showMessageDialog(frame,
                message + "\n\n" + safeMessage(ex) + "\n\nLogged to:\n" + ERROR_LOG,
                "DataDocs Error", JOptionPane.ERROR_MESSAGE);
    }

    private static String safeMessage(Throwable ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    private static void openFolder(Path path) {
        try {
            Files.createDirectories(path);
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(path.toFile());
            else JOptionPane.showMessageDialog(frame, path.toString(), "Folder", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            logError("Failed to open folder: " + path, ex);
            showError("Could not open the folder.", ex);
        }
    }

    // =========================================================
    // HELP
    // =========================================================

    private static void showAbout() {
        JOptionPane.showMessageDialog(frame,
                APP_NAME + " v" + VERSION
                        + "\n\nA Java Swing document editor for Datacenter."
                        + "\n\n3.0 highlights:"
                        + "\n• Multi-tab editing"
                        + "\n• Recent files"
                        + "\n• Per-tab undo/redo"
                        + "\n• Autosave Recovery Center"
                        + "\n• Automatic backups"
                        + "\n• Drag-and-drop open"
                        + "\n• Find/replace with whole-word mode"
                        + "\n• Line numbers"
                        + "\n• HTML export"
                        + "\n• Focus mode"
                        + "\n• Saved preferences"
                        + "\n• Error logging to " + ERROR_LOG,
                "About DataDocs", JOptionPane.INFORMATION_MESSAGE);
    }

    private static void showShortcuts() {
        JTextArea area = new JTextArea(
                "Ctrl+N  New tab\n"
                        + "Ctrl+O  Open file\n"
                        + "Ctrl+Shift+O  Open folder\n"
                        + "Ctrl+S  Save\n"
                        + "Ctrl+Shift+S  Save As\n"
                        + "Ctrl+Alt+S  Save All\n"
                        + "Ctrl+W  Close tab\n"
                        + "Ctrl+Z / Ctrl+Y  Undo / Redo\n"
                        + "Ctrl+F  Find\n"
                        + "Ctrl+H  Replace\n"
                        + "Ctrl+G  Go to line\n"
                        + "Ctrl+D  Duplicate line/selection\n"
                        + "Ctrl+P  Print\n"
                        + "Ctrl+Shift+P  Command palette\n"
                        + "Ctrl+K  Quick switch tab\n"
                        + "Ctrl+Shift+B  File explorer\n"
                        + "Ctrl++ / Ctrl+-  Zoom\n"
                        + "Ctrl+0  Reset zoom\n"
                        + "F11  Focus mode\n"
                        + "Ctrl+Mouse Wheel  Zoom");
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 14));
        area.setBackground(panelColor());
        area.setForeground(textColor());
        area.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JOptionPane.showMessageDialog(frame, area, "Keyboard Shortcuts", JOptionPane.INFORMATION_MESSAGE);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private static DocumentTab activeTab() {
        if (tabs == null || tabs.getSelectedIndex() < 0) return null;
        return tabAt(tabs.getSelectedIndex());
    }

    private static DocumentTab tabAt(int index) {
        if (tabs == null || index < 0 || index >= tabs.getTabCount()) return null;
        Component component = tabs.getComponentAt(index);
        if (!(component instanceof JScrollPane scroll)) return null;
        Component view = scroll.getViewport().getView();
        if (!(view instanceof JTextArea area)) return null;

        for (Object candidate : documentTabsSnapshot()) {
            DocumentTab tab = (DocumentTab) candidate;
            if (tab.area == area) return tab;
        }
        return null;
    }

    /*
     * We don't keep a second global tab list. Instead, each editor's DocumentTab
     * is stored as a client property on its text area. This keeps JTabbedPane as
     * the single source of truth.
     */
    private static List<DocumentTab> documentTabsSnapshot() {
        List<DocumentTab> result = new ArrayList<>();
        if (tabs == null) return result;
        for (int i = 0; i < tabs.getTabCount(); i++) {
            Component c = tabs.getComponentAt(i);
            if (c instanceof JScrollPane scroll && scroll.getViewport().getView() instanceof JTextArea area) {
                Object value = area.getClientProperty("DataDocs.DocumentTab");
                if (value instanceof DocumentTab tab) result.add(tab);
            }
        }
        return result;
    }

    private static void registerTab(DocumentTab tab) {
        tab.area.putClientProperty("DataDocs.DocumentTab", tab);
    }

    private static void setDocumentText(DocumentTab tab, String text) {
        tab.suppressEvents = true;
        try {
            tab.area.setText(text == null ? "" : text);
            tab.area.setCaretPosition(0);
        } finally {
            tab.suppressEvents = false;
        }
    }

    private static JFileChooser createFileChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Text Documents (*.txt)", "txt"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Markdown (*.md)", "md"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Java Source (*.java)", "java"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("JSON (*.json)", "json"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Log Files (*.log)", "log"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
        return chooser;
    }

    private static JMenuItem menuItem(String text, KeyStroke accelerator, ActionListener listener) {
        JMenuItem item = new JMenuItem(text);
        if (accelerator != null) item.setAccelerator(accelerator);
        item.addActionListener(listener);
        return item;
    }

    private static JMenuItem editorActionItem(String text, Action action, KeyStroke accelerator) {
        JMenuItem item = new JMenuItem(action);
        item.setText(text);
        if (accelerator != null) item.setAccelerator(accelerator);
        return item;
    }

    private static JButton toolButton(String text, ActionListener listener) {
        JButton button = new JButton(text);
        button.setFont(UI_FONT);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(listener);
        return button;
    }

    private static JSeparator createSeparator() {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(1, 28));
        return separator;
    }

    private static boolean hasExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 && dot < name.length() - 1;
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String sanitizeFileName(String text) {
        return text.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static boolean sameFileSafe(Path a, Path b) {
        try { return Files.isSameFile(a, b); }
        catch (IOException ex) { return a.toAbsolutePath().normalize().equals(b.toAbsolutePath().normalize()); }
    }

    private static int countWords(String text) {
        String trimmed = text == null ? "" : text.trim();
        return trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
    }

    private static int countParagraphs(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) return 0;
        return trimmed.split("(?:\\R\\s*){2,}").length;
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static void withArea(java.util.function.Consumer<JTextArea> action) {
        DocumentTab tab = activeTab();
        if (tab != null) action.accept(tab.area);
    }

    private static void activeAreaRequestFocus() {
        DocumentTab tab = activeTab();
        if (tab != null) tab.area.requestFocusInWindow();
    }

    // =========================================================
    // LINE NUMBER VIEW
    // =========================================================

    private static final class LineNumberView extends JComponent {
        private static final long serialVersionUID = 1L;
        private final JTextArea area;
        private static final int PADDING = 8;

        private LineNumberView(JTextArea area) {
            this.area = area;
            setFont(new Font("Monospaced", Font.PLAIN, 12));
            area.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { repaintAndResize(); }
                @Override public void removeUpdate(DocumentEvent e) { repaintAndResize(); }
                @Override public void changedUpdate(DocumentEvent e) { repaintAndResize(); }
            });
        }

        private void repaintAndResize() {
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            int lines = Math.max(1, area.getLineCount());
            int digits = Math.max(2, String.valueOf(lines).length());
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(PADDING * 2 + fm.charWidth('0') * digits, area.getHeight());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(secondaryPanelColor());
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setFont(getFont());
            g2.setColor(mutedColor());

            Rectangle clip = g2.getClipBounds();
            FontMetrics fm = g2.getFontMetrics();
            int lineHeight = area.getFontMetrics(area.getFont()).getHeight();
            int first = Math.max(0, clip.y / Math.max(1, lineHeight) - 1);
            int last = Math.min(area.getLineCount() - 1, (clip.y + clip.height) / Math.max(1, lineHeight) + 2);

            for (int line = first; line <= last; line++) {
                try {
                    int offset = area.getLineStartOffset(line);
                    java.awt.geom.Rectangle2D r = area.modelToView2D(offset);
                    if (r == null) continue;
                    String number = String.valueOf(line + 1);
                    int x = getWidth() - PADDING - fm.stringWidth(number);
                    int y = (int) r.getY() + area.getFontMetrics(area.getFont()).getAscent();
                    g2.drawString(number, x, y);
                } catch (BadLocationException ignored) {
                }
            }
            g2.dispose();
        }
    }

    // =========================================================
    // STATIC INITIALIZATION FIXUP
    // =========================================================

    /*
     * DocumentTab registration must happen after its Swing fields exist.
     * Keeping it here as a tiny helper makes the constructor easier to read.
     */
    static {
        // No eager UI creation. Swing starts on the EDT in main().
    }

    // Wrap constructor registration without changing the public API.
    // Java has no post-constructor hook, so addTab guarantees registration.
    private static void ensureRegistered(DocumentTab tab) {
        if (tab.area.getClientProperty("DataDocs.DocumentTab") == null) registerTab(tab);
    }

    // Re-declare add behavior through this helper call site.
    private static void ensureAllTabsRegistered() {
        if (tabs == null) return;
        for (int i = 0; i < tabs.getTabCount(); i++) {
            Component c = tabs.getComponentAt(i);
            if (c instanceof JScrollPane scroll && scroll.getViewport().getView() instanceof JTextArea area) {
                if (area.getClientProperty("DataDocs.DocumentTab") == null) {
                    // Existing tabs are always created by this class; nothing to infer here.
                }
            }
        }
    }
}
