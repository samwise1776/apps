import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IDE {
    private static final Color NAV = new Color(23, 37, 31);
    private static final Color PAPER = new Color(255, 254, 249);
    private static final Color CANVAS = new Color(245, 243, 236);
    private static final Color GREEN = new Color(46, 125, 91);
    private static final Color LIME = new Color(216, 239, 114);
    private static final Pattern CLASS_NAME = Pattern.compile("public\\s+class\\s+([A-Za-z_$][A-Za-z0-9_$]*)");
    private static final Map<String, String> AUTO_IMPORTS = buildAutoImports();

    private JFrame frame;
    private JTextArea editor;
    private JTextArea output;
    private JTextField projectName;
    private JLabel fileLabel;
    private JLabel status;
    private JCheckBox autoImport;
    private Path currentFile;
    private boolean changed;

    public static void main(String[] args) {
        if (!hasGraphicalDesktop()) return;
        SwingUtilities.invokeLater(() -> new IDE().createGUI());
    }

    public void createGUI() {
        if (!hasGraphicalDesktop()) return;
        frame = new JFrame("Desktopcraft IDE");
        URL iconUrl = IDE.class.getResource("desktopcraft-ide-icon.png");
        if (iconUrl != null) frame.setIconImage(new ImageIcon(iconUrl).getImage());
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(920, 620));
        frame.setSize(1180, 780);
        frame.setJMenuBar(buildMenuBar());
        frame.setContentPane(buildWorkspace());
        installShortcuts();
        newProject();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JComponent buildWorkspace() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(CANVAS);
        root.add(buildToolbar(), BorderLayout.NORTH);
        root.add(buildToolbox(), BorderLayout.WEST);

        editor = new JTextArea();
        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        editor.setTabSize(4);
        editor.setBackground(new Color(22, 33, 28));
        editor.setForeground(new Color(235, 243, 237));
        editor.setCaretColor(LIME);
        editor.setSelectionColor(GREEN);
        editor.setBorder(new EmptyBorder(14, 16, 14, 16));
        editor.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { documentChanged(); }
            public void removeUpdate(DocumentEvent event) { documentChanged(); }
            public void changedUpdate(DocumentEvent event) { documentChanged(); }
        });

        output = new JTextArea(7, 20);
        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        output.setBackground(new Color(31, 41, 37));
        output.setForeground(Color.WHITE);
        output.setBorder(new EmptyBorder(8, 10, 8, 10));

        JPanel editorPanel = new JPanel(new BorderLayout());
        fileLabel = new JLabel("Main.java");
        fileLabel.setBorder(new EmptyBorder(7, 12, 7, 12));
        fileLabel.setOpaque(true);
        fileLabel.setBackground(NAV);
        fileLabel.setForeground(Color.WHITE);
        editorPanel.add(fileLabel, BorderLayout.NORTH);
        editorPanel.add(new JScrollPane(editor), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorPanel, new JScrollPane(output));
        split.setResizeWeight(.82);
        split.setBorder(null);
        root.add(split, BorderLayout.CENTER);

        status = new JLabel("Ready");
        status.setBorder(new EmptyBorder(5, 10, 5, 10));
        root.add(status, BorderLayout.SOUTH);
        return root;
    }

    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 7));
        bar.setBackground(PAPER);
        projectName = new JTextField("desktopcraft-app", 18);
        bar.add(new JLabel("Project"));
        bar.add(projectName);
        bar.add(button("New", this::newProject));
        bar.add(button("Open", this::openProject));
        bar.add(button("Save", this::saveProject));
        bar.add(button("Auto import", this::applyAutoImports));
        bar.add(button("Run", this::runProject));
        bar.add(button("Build .deb", this::buildDebianPackage));
        JButton publish = button("Publish", this::publishProject);
        publish.setBackground(GREEN);
        publish.setForeground(Color.WHITE);
        bar.add(publish);
        autoImport = new JCheckBox("Import components automatically", true);
        autoImport.setOpaque(false);
        bar.add(autoImport);
        return bar;
    }

    private JComponent buildToolbox() {
        JTabbedPane tools = new JTabbedPane();
        tools.setPreferredSize(new Dimension(230, 0));
        tools.addTab("Components", toolList(new String[][] {
            {"JFrame", "JFrame window = new JFrame(\"My App\");"},
            {"JPanel", "JPanel panel = new JPanel();"},
            {"JLabel", "JLabel label = new JLabel(\"Label\");"},
            {"JButton", "JButton button = new JButton(\"Button\");"},
            {"JTextField", "JTextField field = new JTextField(20);"},
            {"JTextArea", "JTextArea textArea = new JTextArea(8, 30);"},
            {"JCheckBox", "JCheckBox checkBox = new JCheckBox(\"Option\");"},
            {"JComboBox", "JComboBox<String> choices = new JComboBox<>(new String[] {\"One\", \"Two\"});"},
            {"JList", "JList<String> list = new JList<>(new String[] {\"One\", \"Two\"});"},
            {"JTable", "JTable table = new JTable(new Object[][] {}, new String[] {\"Column\"});"},
            {"JScrollPane", "JScrollPane scrollPane = new JScrollPane(textArea);"},
            {"BorderLayout", "panel.setLayout(new BorderLayout());"},
            {"GridLayout", "panel.setLayout(new GridLayout(0, 2, 8, 8));"},
            {"Action listener", "button.addActionListener(event -> label.setText(\"Clicked\"));"}
        }));
        tools.addTab("Imports", toolList(AUTO_IMPORTS.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> new String[] {entry.getKey(), "import " + entry.getValue() + ";"})
            .toArray(String[][]::new)));
        return tools;
    }

    private JComponent toolList(String[][] items) {
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(new EmptyBorder(8, 8, 8, 8));
        for (String[] item : items) {
            JButton itemButton = button(item[0], () -> insertTool(item[1]));
            itemButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            itemButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            list.add(itemButton);
            list.add(Box.createVerticalStrut(5));
        }
        return new JScrollPane(list);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu file = new JMenu("File");
        file.add(menuItem("New", this::newProject));
        file.add(menuItem("Open…", this::openProject));
        file.add(menuItem("Save", this::saveProject));
        file.add(menuItem("Save as…", this::saveProjectAs));
        JMenu build = new JMenu("Build");
        build.add(menuItem("Auto import", this::applyAutoImports));
        build.add(menuItem("Run", this::runProject));
        build.add(menuItem("Build Debian package", this::buildDebianPackage));
        build.add(menuItem("Publish to Desktopcraft", this::publishProject));
        menuBar.add(file);
        menuBar.add(build);
        return menuBar;
    }

    private void newProject() {
        if (editor == null) return;
        currentFile = null;
        projectName.setText("desktopcraft-app");
        setEditorText("""
            import javax.swing.*;
            import java.awt.*;

            public class Main {
                public static void main(String[] args) {
                    SwingUtilities.invokeLater(() -> {
                        JFrame window = new JFrame("Desktopcraft App");
                        JLabel label = new JLabel("Ready to build", SwingConstants.CENTER);
                        window.add(label, BorderLayout.CENTER);
                        window.setSize(520, 320);
                        window.setLocationRelativeTo(null);
                        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                        window.setVisible(true);
                    });
                }
            }
            """);
        changed = false;
        refreshFileLabel();
        output.setText("Create a Swing app, add components from the toolbox, then Run or Publish.\n");
    }

    private void insertTool(String code) {
        if (code.startsWith("import ")) addImport(code.substring(7, code.length() - 1));
        else {
            editor.insert(code + System.lineSeparator(), editor.getCaretPosition());
            if (autoImport.isSelected()) applyAutoImports();
        }
        editor.requestFocusInWindow();
    }

    private void applyAutoImports() {
        String source = editor.getText();
        Set<String> needed = new TreeSet<>();
        for (Map.Entry<String, String> entry : AUTO_IMPORTS.entrySet()) {
            if (Pattern.compile("\\b" + Pattern.quote(entry.getKey()) + "\\b").matcher(source).find()
                && !hasImport(source, entry.getValue())) needed.add(entry.getValue());
        }
        if (needed.isEmpty()) {
            setStatus("Imports are already complete");
            return;
        }
        for (String value : needed) addImport(value);
        setStatus("Added " + needed.size() + " import" + (needed.size() == 1 ? "" : "s"));
    }

    private void addImport(String importName) {
        String source = editor.getText();
        if (hasImport(source, importName)) return;
        List<String> imports = new ArrayList<>();
        Matcher matcher = Pattern.compile("(?m)^import\\s+([^;]+);\\s*$").matcher(source);
        while (matcher.find()) imports.add(matcher.group(1).trim());
        imports.add(importName);
        imports = imports.stream().distinct().sorted().toList();
        source = source.replaceAll("(?m)^import\\s+[^;]+;\\s*\\R?", "");
        int insertAt = 0;
        Matcher packageLine = Pattern.compile("(?m)^package\\s+[^;]+;\\s*\\R?").matcher(source);
        if (packageLine.find()) insertAt = packageLine.end();
        String importBlock = imports.stream().map(value -> "import " + value + ";").reduce("", (a, b) -> a + b + System.lineSeparator());
        String prefix = insertAt > 0 ? "" : System.lineSeparator();
        setEditorText(source.substring(0, insertAt) + importBlock + prefix + source.substring(insertAt).stripLeading());
    }

    private boolean hasImport(String source, String importName) {
        if (source.contains("import " + importName + ";")) return true;
        int dot = importName.lastIndexOf('.');
        return dot > 0 && source.contains("import " + importName.substring(0, dot) + ".*;");
    }

    private void openProject() {
        JFileChooser chooser = javaChooser();
        if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        try {
            currentFile = chooser.getSelectedFile().toPath();
            setEditorText(Files.readString(currentFile));
            projectName.setText(slug(currentFile.getFileName().toString().replaceFirst("\\.java$", "")));
            changed = false;
            refreshFileLabel();
            setStatus("Opened " + currentFile);
        } catch (IOException exception) {
            showError("Could not open project", exception);
        }
    }

    private void saveProject() {
        if (currentFile == null) { saveProjectAs(); return; }
        writeCurrentFile();
    }

    private void saveProjectAs() {
        JFileChooser chooser = javaChooser();
        chooser.setSelectedFile(new java.io.File(className() + ".java"));
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        currentFile = chooser.getSelectedFile().toPath();
        if (!currentFile.toString().toLowerCase().endsWith(".java")) currentFile = Path.of(currentFile + ".java");
        writeCurrentFile();
    }

    private boolean writeCurrentFile() {
        try {
            if (autoImport.isSelected()) applyAutoImports();
            Files.writeString(currentFile, editor.getText(), StandardCharsets.UTF_8);
            changed = false;
            refreshFileLabel();
            setStatus("Saved " + currentFile);
            return true;
        } catch (IOException exception) {
            showError("Could not save project", exception);
            return false;
        }
    }

    private void runProject() {
        runBackground("Running", () -> {
            Path classes = compileProject();
            ProcessResult result = process(List.of(javaCommand(), "-cp", classes.toString(), className()), classes.getParent());
            appendOutput(result.output());
            if (result.exitCode() != 0) throw new IOException("App exited with code " + result.exitCode());
            return "Run finished";
        });
    }

    private void buildDebianPackage() {
        runBackground("Building package", () -> {
            Path packageFile = packageProject();
            appendOutput("Built " + packageFile + "\nInstall with: sudo apt install ./" + packageFile.getFileName() + "\n");
            return "Package ready: " + packageFile.getFileName();
        });
    }

    private Path compileProject() throws Exception {
        if (currentFile == null) {
            SwingUtilities.invokeAndWait(this::saveProjectAs);
            if (currentFile == null) throw new IOException("Save the project before building.");
        }
        if (!writeCurrentFile()) throw new IOException("The source file could not be saved.");
        Path build = currentFile.toAbsolutePath().getParent().resolve("build").resolve(slug(projectName.getText()));
        Path classes = build.resolve("classes");
        Files.createDirectories(classes);
        ProcessResult compiled = process(List.of("javac", "--release", "17", "-d", classes.toString(), currentFile.toString()), build);
        appendOutput(compiled.output());
        if (compiled.exitCode() != 0) throw new IOException("Compilation failed.");
        return classes;
    }

    private Path packageProject() throws Exception {
        Path classes = compileProject();
        String packageName = slug(projectName.getText());
        Path build = classes.getParent();
        Path jar = build.resolve(packageName + ".jar");
        ProcessResult jarResult = process(List.of("jar", "--create", "--file", jar.toString(), "--main-class", className(), "-C", classes.toString(), "."), build);
        if (jarResult.exitCode() != 0) throw new IOException("JAR creation failed. " + jarResult.output());
        if (!commandAvailable("dpkg-deb")) throw new IOException("Install dpkg-dev to build a Debian package.");

        Path root = build.resolve("debian-root");
        Path control = root.resolve("DEBIAN");
        Path appHome = root.resolve("usr/share").resolve(packageName);
        Path bin = root.resolve("usr/bin");
        Files.createDirectories(control);
        Files.createDirectories(appHome);
        Files.createDirectories(bin);
        Files.copy(jar, appHome.resolve(packageName + ".jar"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(control.resolve("control"), "Package: " + packageName + "\nVersion: 1.0.0\nSection: utils\nPriority: optional\nArchitecture: all\nDepends: default-jre | java17-runtime\nMaintainer: Desktopcraft Builder\nDescription: " + cleanDescription(projectName.getText()) + "\n");
        Path launcher = bin.resolve(packageName);
        Files.writeString(launcher, "#!/bin/sh\nexec java -jar /usr/share/" + packageName + "/" + packageName + ".jar \"$@\"\n");
        process(List.of("chmod", "755", launcher.toString()), build);
        Path deb = currentFile.toAbsolutePath().getParent().resolve(packageName + ".deb");
        ProcessResult debResult = process(List.of("dpkg-deb", "--build", root.toString(), deb.toString()), build);
        if (debResult.exitCode() != 0) throw new IOException("Debian package creation failed. " + debResult.output());
        return deb;
    }

    private void publishProject() {
        JTextField server = new JTextField("http://localhost:8000");
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JTextArea description = new JTextArea("A Java Swing app built with Desktopcraft IDE.", 4, 32);
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Desktopcraft server")); form.add(server);
        form.add(new JLabel("Username")); form.add(username);
        form.add(new JLabel("Password")); form.add(password);
        form.add(new JLabel("Description")); form.add(new JScrollPane(description));
        if (JOptionPane.showConfirmDialog(frame, form, "Publish project", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        runBackground("Publishing", () -> {
            Path deb = null;
            try { deb = packageProject(); } catch (Exception exception) { appendOutput("Package skipped: " + exception.getMessage() + "\nPublishing source instead.\n"); }
            String base = server.getText().trim().replaceAll("/+$", "");
            HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
            String loginJson = "{\"username\":\"" + json(username.getText()) + "\",\"password\":\"" + json(new String(password.getPassword())) + "\"}";
            HttpResponse<String> login = client.send(HttpRequest.newBuilder(URI.create(base + "/api/auth/login"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(loginJson)).build(), HttpResponse.BodyHandlers.ofString());
            if (login.statusCode() != 200) throw new IOException("Sign-in failed: " + apiError(login.body()));
            String cookie = login.headers().firstValue("set-cookie").orElseThrow(() -> new IOException("The server did not create a session.")).split(";", 2)[0];
            String packageFields = deb == null ? "" : ",\"packageFileName\":\"" + json(deb.getFileName().toString()) + "\",\"packageBase64\":\"" + Base64.getEncoder().encodeToString(Files.readAllBytes(deb)) + "\"";
            String publishJson = "{\"title\":\"" + json(projectName.getText()) + "\",\"description\":\"" + json(description.getText()) + "\",\"toolkit\":\"java\",\"sourceCode\":\"" + json(editor.getText()) + "\"" + packageFields + "}";
            HttpResponse<String> published = client.send(HttpRequest.newBuilder(URI.create(base + "/api/apps"))
                .header("Content-Type", "application/json").header("Cookie", cookie)
                .POST(HttpRequest.BodyPublishers.ofString(publishJson)).build(), HttpResponse.BodyHandlers.ofString());
            if (published.statusCode() != 201) throw new IOException("Publish failed: " + apiError(published.body()));
            appendOutput("Published successfully. Open " + base + "/view.html to download it.\n");
            return "Published to Desktopcraft";
        });
    }

    private void runBackground(String activity, Task task) {
        setStatus(activity + "…");
        output.setText("");
        new SwingWorker<String, Void>() {
            protected String doInBackground() throws Exception { return task.run(); }
            protected void done() {
                try { setStatus(get()); }
                catch (Exception exception) {
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    appendOutput("Error: " + cause.getMessage() + "\n");
                    setStatus(activity + " failed");
                }
            }
        }.execute();
    }

    private ProcessResult process(List<String> command, Path directory) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true).start();
        String text = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return new ProcessResult(process.waitFor(), text);
    }

    private boolean commandAvailable(String command) {
        try { return process(List.of("sh", "-c", "command -v " + command), Path.of(".").toAbsolutePath()).exitCode() == 0; }
        catch (Exception ignored) { return false; }
    }

    private String javaCommand() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    private String className() {
        Matcher matcher = CLASS_NAME.matcher(editor.getText());
        return matcher.find() ? matcher.group(1) : "Main";
    }

    private void documentChanged() { changed = true; refreshFileLabel(); }
    private void setEditorText(String text) { editor.setText(text); editor.setCaretPosition(0); }
    private void setStatus(String text) { if (status != null) status.setText(text); }
    private void appendOutput(String text) { SwingUtilities.invokeLater(() -> output.append(text)); }
    private void refreshFileLabel() { if (fileLabel != null) fileLabel.setText((currentFile == null ? className() + ".java" : currentFile.getFileName()) + (changed ? " •" : "")); }
    private JFileChooser javaChooser() { JFileChooser chooser = new JFileChooser(); chooser.setFileFilter(new FileNameExtensionFilter("Java source", "java")); return chooser; }
    private JButton button(String text, Runnable action) { JButton button = new JButton(text); button.addActionListener(event -> action.run()); return button; }
    private JMenuItem menuItem(String text, Runnable action) { JMenuItem item = new JMenuItem(text); item.addActionListener(event -> action.run()); return item; }
    private void showError(String title, Exception exception) { JOptionPane.showMessageDialog(frame, exception.getMessage(), title, JOptionPane.ERROR_MESSAGE); }

    private void installShortcuts() {
        int menu = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, menu), "save");
        frame.getRootPane().getActionMap().put("save", new AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent event) { saveProject(); } });
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, menu | InputEvent.SHIFT_DOWN_MASK), "run");
        frame.getRootPane().getActionMap().put("run", new AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent event) { runProject(); } });
    }

    private static Map<String, String> buildAutoImports() {
        Map<String, String> imports = new LinkedHashMap<>();
        for (String type : List.of("JFrame", "JPanel", "JLabel", "JButton", "JTextField", "JTextArea", "JCheckBox", "JRadioButton", "JComboBox", "JList", "JTable", "JTree", "JTabbedPane", "JSplitPane", "JScrollPane", "JOptionPane", "SwingUtilities", "SwingConstants", "ImageIcon", "JFileChooser")) imports.put(type, "javax.swing." + type);
        for (String type : List.of("BorderLayout", "FlowLayout", "GridLayout", "GridBagLayout", "Color", "Font", "Dimension", "Insets")) imports.put(type, "java.awt." + type);
        imports.put("ActionEvent", "java.awt.event.ActionEvent");
        imports.put("ActionListener", "java.awt.event.ActionListener");
        imports.put("ArrayList", "java.util.ArrayList");
        imports.put("List", "java.util.List");
        imports.put("Map", "java.util.Map");
        return imports;
    }

    private static String slug(String value) {
        String slug = value == null ? "" : value.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug.isBlank() ? "desktopcraft-app" : slug;
    }

    private static String cleanDescription(String value) { return String.valueOf(value).replaceAll("[\\r\\n]+", " ").trim(); }
    private static String json(String value) { return String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t"); }
    private static String apiError(String body) { Matcher matcher = Pattern.compile("\"error\"\\s*:\\s*\"([^\"]+)\"").matcher(body); return matcher.find() ? matcher.group(1) : body; }

    private static boolean hasGraphicalDesktop() {
        if (!GraphicsEnvironment.isHeadless()) return true;
        System.err.println("Desktopcraft IDE needs a graphical desktop. Run it with a full desktop Java runtime and an X11 or Wayland display.");
        return false;
    }

    @FunctionalInterface private interface Task { String run() throws Exception; }
    private record ProcessResult(int exitCode, String output) {}
}
