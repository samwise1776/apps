import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

/** AppCenter: the safe installer for public Datacenter source packages. */
public final class App {
    private static final Color BACKGROUND = new Color(248, 250, 252);
    private static final Color SURFACE = Color.WHITE;
    private static final Color TEXT = new Color(15, 23, 42);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color PRIMARY = new Color(37, 99, 235);
    private static final Color PRIMARY_DARK = new Color(30, 64, 175);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color SUCCESS = new Color(22, 163, 74);
    private static final String BASE_URL =
            "https://raw.githubusercontent.com/samwise1776/apps/refs/heads/main/";
    private static final Path DATA_HOME = Path.of(
            System.getProperty("user.home"), ".local", "share", "datacenter", "appcenter");
    private static final Path INSTALL_HOME = DATA_HOME.resolve("apps");
    private static final Path STATE_FILE = DATA_HOME.resolve("installed.properties");
    private static final Path APPLICATIONS_HOME = Path.of(
            System.getenv().getOrDefault("XDG_DATA_HOME",
                    Path.of(System.getProperty("user.home"), ".local", "share").toString()),
            "applications");
    private static final int MAX_FILES = 20_000;
    private static final long MAX_UNCOMPRESSED_BYTES = 512L * 1024 * 1024;

    private static final List<Product> PRODUCTS = List.of(
            new Product("datadocs", "DataDocs", "1.0.0", "Documentation", "Java included",
                    "A focused writing app for creating and editing documents.",
                    "apps/datadocs/datadocs.zip", "dcca7af443ff4ae2d098544dfeccdd4106cdc908b210e5ab430bb131a30d45cb", "DOC", "", Kind.JAVA_GUI, "DataDocs"),
            new Product("learner", "Learner", "1.0.0", "Education", "Java included",
                    "Lessons, quizzes, profiles, favorites, and saved progress.",
                    "apps/learner/learner.zip", "165c9c687f81546d93598129cf76c986b8da73be70f5cc0435b9c854d4986a3b", "LRN", "", Kind.JAVA_GUI, "Learner"),
            new Product("projecthub", "ProjectHub", "1.0.0", "Productivity", "Java included",
                    "Plan projects, tasks, bugs, progress, and releases locally.",
                    "apps/projecthub/projecthub.zip", "3038dc577fe43bb7d0e69e624db539e1da645b61157fb94bcaafe1ef59ead207", "HUB", "", Kind.JAVA_GUI, "App"),
            new Product("trestrio", "Trestrio", "1.0.0", "Personal workspace", "Node.js 18+",
                    "A calm workspace for tasks, notes, focus, and utilities.",
                    "apps/trestrio/trestrio.zip", "55d4e8a9587e40544473b0f51afbd4701e70e9ce3cccae00df2d025b9b839a96", "TRI", "", Kind.NODE, "desktop"),
            new Product("scrapzone", "ScrapZone", "0.1.0", "3D game", ".NET 10 SDK",
                    "An experimental 3D game built with Raylib and .NET.",
                    "games/scrapzone/ScrapZone.zip", "b13db837a6f6f461a92be4f8677798facd48b43e73cc853af5bacfe0a8bad790", "GAME", "", Kind.DOTNET, "ScrapZone"),
            new Product("vexa", "Vexa", "0.1.0", "Programming language", "Java included",
                    "A small language with readable variables and print commands.",
                    "apps/vexa/vexa.zip", "47273f348db6e44e375f98d7ea57ea64dd81c9a9dc051b307d123e3162af23c8", "</>", "", Kind.JAVA_CLI, "Interpreter"),
            new Product("velice", "Velice", "0.1.0", "Programming language", "Python 3.10+",
                    "A clear general-purpose language with functions, classes, pattern matching, tooling, and native GUI apps.",
                    "apps/velice/velice.zip", "b6e5cbe593a9274d2d4c93ab3e0fa5e67980ac4785e4caa4ceaef23495ce6481", "VL",
                    "https://samwise1776.github.io/apps/velice.html", Kind.PYTHON_CLI, "velice"),
            new Product("descr1be", "Descr1be", "0.1.0", "UI builder", "Python 3.10+",
                    "A visual UI builder written in Velice: design windows on a canvas, edit declarative source, or describe apps in plain English.",
                    "apps/descr1be/descr1be.zip", "0eb5bfce8b70cabe469bcc2e42338ff87360059af400fe88ae2d3397dba6007d", "D1",
                    "", Kind.PYTHON_GUI, "src/main.velice"));

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final Properties installed = new Properties();
    private final List<ProductCard> cards = new ArrayList<>();
    private JFrame frame;
    private JLabel status;
    private JProgressBar progress;

    public static void main(String[] args) {
        if (args.length == 1 && "--self-test".equals(args[0])) {
            System.out.println("AppCenter catalog: " + PRODUCTS.size() + " public products");
            if (PRODUCTS.stream().noneMatch(product -> product.slug().equals("velice")
                    && product.sha256().matches("[0-9a-f]{64}") && !product.guideUrl().isBlank())) {
                throw new IllegalStateException("Velice catalog entry is incomplete");
            }
            System.out.println("AppCenter self-test passed.");
            return;
        }
        SwingUtilities.invokeLater(() -> new App().show());
    }

    private void show() {
        configureLookAndFeel();
        loadState();
        installAppCenterLauncher();
        frame = new JFrame("Datacenter AppCenter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(820, 620));
        frame.setSize(1060, 760);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);
        root.add(header(), BorderLayout.NORTH);
        root.add(catalog(), BorderLayout.CENTER);
        root.add(statusBar(), BorderLayout.SOUTH);
        frame.setContentPane(root);
        frame.setVisible(true);
    }

    private JPanel header() {
        JPanel panel = new JPanel(new BorderLayout(22, 0));
        panel.setBackground(new Color(15, 23, 42));
        panel.setBorder(new EmptyBorder(28, 34, 28, 34));
        JPanel words = transparentPanel();
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        JLabel eyebrow = label("DATACENTER", 12, Font.BOLD, new Color(147, 197, 253));
        JLabel title = label("AppCenter", 34, Font.BOLD, Color.WHITE);
        JLabel subtitle = label("One click to download, prepare, and launch. Java is included for you.", 15,
                Font.PLAIN, new Color(203, 213, 225));
        words.add(eyebrow); words.add(Box.createVerticalStrut(4)); words.add(title);
        words.add(Box.createVerticalStrut(7)); words.add(subtitle);
        panel.add(words, BorderLayout.CENTER);
        JLabel shield = label("✓  SHA-256 verified", 13, Font.BOLD, new Color(187, 247, 208));
        shield.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(22, 101, 52)), new EmptyBorder(9, 13, 9, 13)));
        panel.add(shield, BorderLayout.EAST);
        return panel;
    }

    private JScrollPane catalog() {
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(BACKGROUND);
        body.setBorder(new EmptyBorder(28, 30, 34, 30));
        JPanel heading = transparentPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(label("Public apps", 25, Font.BOLD, TEXT));
        heading.add(label("No code or terminal required. AppCenter prepares each installation automatically.", 14,
                Font.PLAIN, MUTED));
        body.add(heading, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 18, 18));
        grid.setBackground(BACKGROUND);
        grid.setBorder(new EmptyBorder(22, 0, 0, 0));
        for (Product product : PRODUCTS) {
            ProductCard card = new ProductCard(product);
            cards.add(card);
            grid.add(card.panel);
        }
        body.add(grid, BorderLayout.CENTER);
        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    private JPanel statusBar() {
        JPanel panel = new JPanel(new BorderLayout(14, 0));
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER), new EmptyBorder(12, 24, 12, 24)));
        status = label("Ready", 13, Font.PLAIN, MUTED);
        progress = new JProgressBar();
        progress.setVisible(false);
        progress.setPreferredSize(new Dimension(180, 8));
        panel.add(status, BorderLayout.CENTER);
        panel.add(progress, BorderLayout.EAST);
        return panel;
    }

    private final class ProductCard {
        final Product product;
        final JPanel panel;
        final JLabel installedLabel;
        final JButton primary;
        final JButton remove;

        ProductCard(Product product) {
            this.product = product;
            panel = new JPanel(new BorderLayout(0, 16));
            panel.setBackground(SURFACE);
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER), new EmptyBorder(22, 22, 20, 22)));

            JPanel top = transparentPanel();
            top.setLayout(new BoxLayout(top, BoxLayout.X_AXIS));
            JLabel icon = label(product.icon(), 13, Font.BOLD, Color.WHITE);
            icon.setHorizontalAlignment(SwingConstants.CENTER);
            icon.setPreferredSize(new Dimension(52, 52));
            icon.setMaximumSize(new Dimension(52, 52));
            icon.setOpaque(true); icon.setBackground(PRIMARY);
            icon.setBorder(new EmptyBorder(8, 5, 8, 5));
            JPanel names = transparentPanel(); names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
            names.setBorder(new EmptyBorder(1, 13, 0, 0));
            names.add(label(product.name(), 19, Font.BOLD, TEXT));
            names.add(label(product.category() + "  ·  v" + product.version(), 12, Font.BOLD, PRIMARY_DARK));
            top.add(icon); top.add(names); top.add(Box.createHorizontalGlue());
            panel.add(top, BorderLayout.NORTH);

            JPanel middle = transparentPanel(); middle.setLayout(new BoxLayout(middle, BoxLayout.Y_AXIS));
            JLabel description = label("<html><body style='width:280px'>" + product.description() + "</body></html>",
                    13, Font.PLAIN, MUTED);
            middle.add(description); middle.add(Box.createVerticalStrut(12));
            middle.add(label("Requires " + product.runtime(), 12, Font.BOLD, TEXT));
            installedLabel = label("", 12, Font.BOLD, SUCCESS);
            middle.add(Box.createVerticalStrut(6)); middle.add(installedLabel);
            panel.add(middle, BorderLayout.CENTER);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); actions.setOpaque(false);
            primary = button("Install", true);
            primary.addActionListener(event -> installOrLaunch(product));
            remove = button("Remove", false);
            remove.addActionListener(event -> remove(product));
            actions.add(primary);
            if (!product.guideUrl().isBlank()) {
                JButton guide = button("Guide", false);
                guide.addActionListener(event -> openWebsite(product.guideUrl()));
                actions.add(guide);
            }
            actions.add(remove);
            panel.add(actions, BorderLayout.SOUTH);
            refresh();
        }

        void refresh() {
            String version = installed.getProperty(product.slug());
            boolean present = version != null && Files.isDirectory(installPath(product));
            installedLabel.setText(present ? "✓ Installed v" + version : "Not installed");
            installedLabel.setForeground(present ? SUCCESS : MUTED);
            primary.setText(present ? "Launch" : "Install");
            remove.setVisible(present);
        }
    }

    private void installOrLaunch(Product product) {
        if (installed.getProperty(product.slug()) != null && Files.isDirectory(installPath(product))) {
            launch(product);
            return;
        }
        setBusy(true, "Downloading " + product.name() + "…");
        new SwingWorker<Path, Void>() {
            @Override protected Path doInBackground() throws Exception { return install(product); }
            @Override protected void done() {
                try {
                    Path path = get();
                    setBusy(false, product.name() + " installed and verified.");
                    refreshCards();
                    int choice = JOptionPane.showConfirmDialog(frame,
                            product.name() + " is installed and ready.\n\nLaunch it now?",
                            "Installation complete", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    if (choice == JOptionPane.YES_OPTION) launch(product);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt(); setBusy(false, "Installation interrupted.");
                } catch (ExecutionException exception) {
                    setBusy(false, "Installation failed.");
                    showError("Could not install " + product.name(), exception.getCause());
                }
            }
        }.execute();
    }

    private Path install(Product product) throws IOException, InterruptedException {
        Files.createDirectories(INSTALL_HOME);
        Path download = Files.createTempFile(DATA_HOME, product.slug() + "-", ".zip");
        Path staging = Files.createTempDirectory(INSTALL_HOME, "." + product.slug() + "-");
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + product.remotePath()))
                    .timeout(Duration.ofMinutes(2)).header("User-Agent", "Datacenter-AppCenter/1.0").build();
            HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(download));
            if (response.statusCode() != 200) throw new IOException("Download returned HTTP " + response.statusCode());
            String actual = sha256(download);
            if (!MessageDigest.isEqual(actual.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                    product.sha256().getBytes(java.nio.charset.StandardCharsets.US_ASCII))) {
                throw new IOException("Checksum verification failed; the package was not installed");
            }
            extractSafely(download, staging);
            prepare(product, staging);
            Path target = installPath(product);
            if (Files.exists(target)) deleteTree(target);
            try { Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException exception) { Files.move(staging, target); }
            installed.setProperty(product.slug(), product.version());
            saveState();
            installProductLauncher(product);
            return target;
        } finally {
            Files.deleteIfExists(download);
            if (Files.exists(staging)) deleteTree(staging);
        }
    }

    private void prepare(Product product, Path root) throws IOException, InterruptedException {
        switch (product.kind()) {
            case JAVA_GUI, JAVA_CLI -> compileJava(root);
            case PYTHON_CLI, PYTHON_GUI -> {
                runChecked(root, pythonCommand(), "-m", "compileall", "-q", "velice");
                runChecked(root, pythonCommand(), "-m", "velice", "version");
            }
            case NODE -> runChecked(root, "npm", "ci", "--no-audit", "--no-fund");
            case DOTNET -> runChecked(root, "dotnet", "build", "ScrapZone.csproj", "--nologo");
        }
    }

    private static void compileJava(Path root) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IOException("The bundled Java compiler is unavailable");
        List<Path> sources;
        try (var paths = Files.walk(root)) { sources = paths.filter(path -> path.toString().endsWith(".java")).toList(); }
        if (sources.isEmpty()) throw new IOException("The package contains no Java source files");
        Path classes = root.resolve(".appcenter/classes"); Files.createDirectories(classes);
        try (StandardJavaFileManager files = compiler.getStandardFileManager(null, null, null)) {
            var units = files.getJavaFileObjectsFromPaths(sources);
            boolean success = compiler.getTask(null, files, null, List.of("-d", classes.toString()), null, units).call();
            if (!success) throw new IOException("Java compilation failed");
        }
    }

    private static void runChecked(Path directory, String... command) throws IOException, InterruptedException {
        Process process;
        try {
            process = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true).start();
        } catch (IOException error) {
            throw new IOException(command[0] + " is required for this app but was not found", error);
        }
        String output;
        try (InputStream input = process.getInputStream()) { output = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8); }
        if (process.waitFor() != 0) throw new IOException(String.join(" ", command) + " failed:\n" + tail(output, 1200));
    }

    private void launch(Product product) {
        Path root = installPath(product);
        try {
            switch (product.kind()) {
                case JAVA_GUI -> new ProcessBuilder(javaCommand(), "-cp", root.resolve(".appcenter/classes").toString(), product.main()).start();
                case JAVA_CLI -> showVexa(root, product);
                case PYTHON_CLI -> showVelice(root);
                case PYTHON_GUI -> launchPythonGui(root, product);
                case NODE -> new ProcessBuilder(npmCommand(), "run", "desktop").directory(root.toFile()).start();
                case DOTNET -> launchDotnet(root);
            }
            status.setText(product.name() + " launched.");
        } catch (IOException error) { showError("Could not launch " + product.name(), error); }
    }

    private void showVexa(Path root, Product product) throws IOException {
        Path example = root.resolve("examples/hello.vexa");
        Process process = new ProcessBuilder(javaCommand(), "-cp", root.resolve(".appcenter/classes").toString(), product.main(), example.toString())
                .redirectErrorStream(true).start();
        try {
            String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            process.waitFor();
            JOptionPane.showMessageDialog(frame, output, "Vexa output", JOptionPane.INFORMATION_MESSAGE);
        } catch (InterruptedException error) { Thread.currentThread().interrupt(); }
    }

    private void showVelice(Path root) throws IOException {
        Path example = root.resolve("examples/hello_world.velice");
        Process process = new ProcessBuilder(pythonCommand(), "-m", "velice", "run", example.toString())
                .directory(root.toFile()).redirectErrorStream(true).start();
        try {
            String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            int code = process.waitFor();
            if (code != 0) throw new IOException("Velice exited with code " + code + ":\n" + tail(output, 1200));
            JOptionPane.showMessageDialog(frame, output, "Velice output", JOptionPane.INFORMATION_MESSAGE);
        } catch (InterruptedException error) { Thread.currentThread().interrupt(); }
    }

    private void launchPythonGui(Path root, Product product) throws IOException {
        Path entry = root.resolve(product.main());
        new ProcessBuilder(pythonCommand(), "-m", "velice", "run", entry.toString())
                .directory(root.toFile()).start();
    }

    private static void launchDotnet(Path root) throws IOException {
        new ProcessBuilder("dotnet", findScrapZoneDll(root).toString()).directory(root.toFile()).start();
    }

    private static Path findScrapZoneDll(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            return paths.filter(path -> path.getFileName().toString().equals("ScrapZone.dll") && path.toString().contains("bin"))
                    .findFirst().orElseThrow(() -> new IOException("Built ScrapZone application was not found"));
        }
    }

    private static String javaCommand() {
        String executable = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable).toString();
    }

    private static String npmCommand() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "npm.cmd" : "npm";
    }

    private static String pythonCommand() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "python" : "python3";
    }

    private static String tail(String text, int length) { return text.length() <= length ? text : text.substring(text.length() - length); }

    static void extractSafely(Path archive, Path destination) throws IOException {
        Path root = destination.toAbsolutePath().normalize();
        int files = 0;
        long total = 0;
        try (InputStream input = new BufferedInputStream(Files.newInputStream(archive));
                ZipInputStream zip = new ZipInputStream(input)) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null; ) {
                if (++files > MAX_FILES) throw new IOException("Archive contains too many entries");
                Path target = root.resolve(entry.getName()).normalize();
                if (!target.startsWith(root) || entry.getName().startsWith("/")) {
                    throw new IOException("Unsafe archive path: " + entry.getName());
                }
                if (entry.isDirectory()) { Files.createDirectories(target); continue; }
                Files.createDirectories(target.getParent());
                try (OutputStream output = Files.newOutputStream(target)) {
                    byte[] buffer = new byte[16 * 1024];
                    for (int count; (count = zip.read(buffer)) != -1; ) {
                        total += count;
                        if (total > MAX_UNCOMPRESSED_BYTES) throw new IOException("Archive is too large");
                        output.write(buffer, 0, count);
                    }
                }
            }
        }
        if (files == 0) throw new IOException("Archive is empty");
    }

    static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[16 * 1024];
                for (int count; (count = input.read(buffer)) != -1; ) digest.update(buffer, 0, count);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void remove(Product product) {
        int answer = JOptionPane.showConfirmDialog(frame,
                "Remove the AppCenter copy of " + product.name() + "?\n\n" + installPath(product),
                "Remove " + product.name(), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        try {
            Path target = installPath(product).toAbsolutePath().normalize();
            Path allowed = INSTALL_HOME.toAbsolutePath().normalize();
            if (!target.startsWith(allowed) || target.equals(allowed)) throw new IOException("Unsafe removal target");
            deleteTree(target);
            installed.remove(product.slug()); saveState(); refreshCards();
            Files.deleteIfExists(desktopFile(product));
            status.setText(product.name() + " removed.");
        } catch (IOException exception) { showError("Could not remove " + product.name(), exception); }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    private void loadState() {
        try {
            Files.createDirectories(DATA_HOME);
            restrictDirectory(DATA_HOME);
            if (Files.isRegularFile(STATE_FILE)) try (InputStream input = Files.newInputStream(STATE_FILE)) { installed.load(input); }
        } catch (IOException exception) { showStartupError(exception); }
    }

    private void saveState() throws IOException {
        Files.createDirectories(DATA_HOME);
        Path temporary = Files.createTempFile(DATA_HOME, "installed-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) { installed.store(output, "Datacenter AppCenter"); }
            try { Files.move(temporary, STATE_FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException exception) { Files.move(temporary, STATE_FILE, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    private void installAppCenterLauncher() {
        if (!isLinux()) return;
        try {
            Files.createDirectories(APPLICATIONS_HOME);
            String appPath = System.getProperty("jpackage.app-path");
            String command;
            if (appPath != null && !appPath.isBlank()) command = quoted(Path.of(appPath).toAbsolutePath().normalize().toString());
            else {
                String classPath = Path.of(System.getProperty("java.class.path")).toAbsolutePath().normalize().toString();
                command = quoted(javaCommand()) + " -cp " + quoted(classPath) + " App";
            }
            String desktop = desktopEntry("Datacenter AppCenter", "Install and launch Datacenter apps",
                    command, "system-software-install");
            writeDesktop(APPLICATIONS_HOME.resolve("datacenter-appcenter.desktop"), desktop);
        } catch (IOException error) {
            if (status != null) status.setText("Could not add AppCenter to the Start Menu: " + error.getMessage());
        }
    }

    private void installProductLauncher(Product product) throws IOException {
        if (!isLinux()) return;
        Files.createDirectories(APPLICATIONS_HOME);
        Path root = installPath(product);
        String command = switch (product.kind()) {
            case JAVA_GUI -> quoted(javaCommand()) + " -cp " + quoted(root.resolve(".appcenter/classes").toString()) + " " + product.main();
            case JAVA_CLI -> quoted(javaCommand()) + " -cp " + quoted(root.resolve(".appcenter/classes").toString()) + " "
                    + product.main() + " " + quoted(root.resolve("examples/hello.vexa").toString());
            case PYTHON_CLI -> quoted(pythonCommand()) + " " + quoted(root.resolve("velice/__main__.py").toString())
                    + " run " + quoted(root.resolve("examples/gui.velice").toString());
            case PYTHON_GUI -> quoted(pythonCommand()) + " " + quoted(root.resolve("velice/__main__.py").toString())
                    + " run " + quoted(root.resolve(product.main()).toString());
            case NODE -> "env ELECTRON_RUN_AS_NODE= " + quoted(root.resolve("node_modules/.bin/electron").toString()) + " " + quoted(root.toString());
            case DOTNET -> "dotnet " + quoted(findScrapZoneDll(root).toString());
        };
        writeDesktop(desktopFile(product), desktopEntry(product.name(), product.description(), command,
                product.kind() == Kind.DOTNET ? "applications-games" : "application-x-executable"));
    }

    private static Path desktopFile(Product product) {
        return APPLICATIONS_HOME.resolve("datacenter-" + product.slug() + ".desktop");
    }

    private static String desktopEntry(String name, String comment, String command, String icon) {
        return "[Desktop Entry]\nType=Application\nVersion=1.0\nName=" + name
                + "\nComment=" + comment.replace("\n", " ") + "\nExec=" + command
                + "\nIcon=" + icon + "\nTerminal=false\nCategories=Utility;\nStartupNotify=true\n";
    }

    private static void writeDesktop(Path path, String content) throws IOException {
        Path temporary = Files.createTempFile(path.getParent(), ".desktop-", ".tmp");
        try {
            Files.writeString(temporary, content, java.nio.charset.StandardCharsets.UTF_8);
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException error) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
            try { Files.setPosixFilePermissions(path, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ)); }
            catch (UnsupportedOperationException ignored) { }
        } finally { Files.deleteIfExists(temporary); }
    }

    private static boolean isLinux() { return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("linux"); }

    private static String quoted(String value) { return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }

    private static void restrictDirectory(Path path) {
        try { Files.setPosixFilePermissions(path, Set.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE)); }
        catch (IOException | UnsupportedOperationException ignored) { /* Account ACLs apply off POSIX. */ }
    }

    private Path installPath(Product product) { return INSTALL_HOME.resolve(product.slug()); }
    private void refreshCards() { cards.forEach(ProductCard::refresh); }

    private void openFolder(Path path) {
        try {
            if (!Desktop.isDesktopSupported()) throw new IOException("Desktop integration is unavailable");
            Desktop.getDesktop().open(path.toFile());
        } catch (IOException exception) { showError("Could not open the installation folder", exception); }
    }

    private void openWebsite(String url) {
        try {
            if (!Desktop.isDesktopSupported()) throw new IOException("Desktop integration is unavailable");
            Desktop.getDesktop().browse(URI.create(url));
        } catch (IOException | IllegalArgumentException exception) {
            showError("Could not open the guide", exception);
        }
    }

    private void setBusy(boolean busy, String message) {
        progress.setVisible(busy); progress.setIndeterminate(busy); status.setText(message);
        cards.forEach(card -> { card.primary.setEnabled(!busy); card.remove.setEnabled(!busy); });
        frame.setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void showError(String title, Throwable error) {
        JOptionPane.showMessageDialog(frame, title + ":\n" +
                (error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage()),
                "AppCenter", JOptionPane.ERROR_MESSAGE);
    }

    private static void showStartupError(Exception error) {
        JOptionPane.showMessageDialog(null, "AppCenter could not load its state:\n" + error.getMessage(),
                "AppCenter", JOptionPane.ERROR_MESSAGE);
    }

    private static JPanel transparentPanel() { JPanel panel = new JPanel(); panel.setOpaque(false); return panel; }

    private static JLabel label(String text, int size, int style, Color color) {
        JLabel label = new JLabel(text); label.setFont(new Font("SansSerif", style, size)); label.setForeground(color); return label;
    }

    private static JButton button(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setMargin(new Insets(9, 15, 9, 15));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (primary) { button.setForeground(Color.WHITE); button.setBackground(PRIMARY); }
        else { button.setForeground(TEXT); button.setBackground(new Color(241, 245, 249)); }
        return button;
    }

    private static void configureLookAndFeel() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) { /* Swing default remains usable. */ }
        UIManager.put("Button.arc", 12);
    }

    private enum Kind { JAVA_GUI, JAVA_CLI, PYTHON_CLI, PYTHON_GUI, NODE, DOTNET }

    private record Product(String slug, String name, String version, String category, String runtime,
                           String description, String remotePath, String sha256, String icon, String guideUrl,
                           Kind kind, String main) { }
}
