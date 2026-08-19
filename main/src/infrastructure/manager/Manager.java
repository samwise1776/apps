import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/** Watches Datacenter applications and derives a version from each completed 100 lines of code. */
public final class Manager {
    private static final long CHECK_INTERVAL_MS = 2_000;
    private static final Set<String> SOURCE_EXTENSIONS = Set.of(
            "java", "c", "cc", "cpp", "h", "hpp", "cs", "js", "jsx", "ts", "tsx",
            "py", "go", "rs", "rb", "php", "swift", "kt", "kts", "scala", "sh",
            "html", "css", "scss", "sql", "xml", "json", "yaml", "yml", "toml", "md");

    private final Path appsPath;
    private final Path versionsPath;
    private final Path appCountPath;

    private Manager(Path dataPath) {
        appsPath = dataPath.resolve("apps");
        versionsPath = dataPath.resolve("versions");
        appCountPath = dataPath.resolve(".apps.txt");
    }

    public static void main(String[] args) {
        Path dataPath = Path.of(System.getProperty("user.home"), "Data");
        Manager manager = new Manager(dataPath);
        Runtime.getRuntime().addShutdownHook(new Thread(
                () -> System.out.println("Datacenter Manager stopped."), "manager-shutdown"));
        manager.run();
    }

    private void run() {
        System.out.println("Datacenter Manager started. One version step = 100 completed lines.");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                scan();
                Thread.sleep(CHECK_INTERVAL_MS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (IOException exception) {
                System.err.println("Manager scan failed: " + exception.getMessage());
                sleepAfterFailure();
            } catch (RuntimeException exception) {
                System.err.println("Manager unexpected error: " + exception.getMessage());
                sleepAfterFailure();
            }
        }
    }

    private void scan() throws IOException {
        Files.createDirectories(appsPath);
        Files.createDirectories(versionsPath);

        List<Path> applications;
        try (Stream<Path> stream = Files.list(appsPath)) {
            applications = stream
                    .filter(path -> !path.getFileName().toString().startsWith("."))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .toList();
        }

        writeIfChanged(appCountPath, applications.isEmpty() ? "None" : String.valueOf(applications.size()));
        for (Path application : applications) versionApplication(application);
        removeMetadataForDeletedApplications(applications);
    }

    private void versionApplication(Path application) throws IOException {
        long lines = countApplicationLines(application);
        long completedHundreds = lines / 100;
        String version = semanticVersion(completedHundreds);
        String appName = application.getFileName().toString();
        String metadataName = safeMetadataName(appName) + ".version";
        String metadata = "app=" + appName + "\n"
                + "lines=" + lines + "\n"
                + "completed_hundreds=" + completedHundreds + "\n"
                + "version=" + version + "\n";

        Path output = versionsPath.resolve(metadataName);
        if (writeIfChanged(output, metadata)) {
            System.out.printf("Versioned %-28s %,7d lines -> v%s%n", appName, lines, version);
        }
    }

    private long countApplicationLines(Path application) throws IOException {
        if (Files.isRegularFile(application)) {
            return isSourceFile(application) ? countLines(application) : 0;
        }
        if (!Files.isDirectory(application)) return 0;

        try (Stream<Path> stream = Files.walk(application)) {
            return stream.filter(Files::isRegularFile)
                    .filter(this::isSourceFile)
                    .mapToLong(path -> {
                        try {
                            return countLines(path);
                        } catch (IOException exception) {
                            System.err.println("Could not count " + path + ": " + exception.getMessage());
                            return 0;
                        }
                    })
                    .sum();
        }
    }

    private boolean isSourceFile(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 && SOURCE_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private long countLines(Path path) throws IOException {
        long count = 0;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            while (reader.readLine() != null) count++;
        }
        return count;
    }

    /** 100 lines = 0.0.1, 1,000 lines = 0.1.0, and 10,000 lines = 1.0.0. */
    private String semanticVersion(long completedHundreds) {
        long major = completedHundreds / 100;
        long minor = (completedHundreds / 10) % 10;
        long patch = completedHundreds % 10;
        return major + "." + minor + "." + patch;
    }

    private String safeMetadataName(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private void removeMetadataForDeletedApplications(List<Path> applications) throws IOException {
        Set<String> expected = applications.stream()
                .map(path -> safeMetadataName(path.getFileName().toString()) + ".version")
                .collect(java.util.stream.Collectors.toSet());
        try (Stream<Path> stream = Files.list(versionsPath)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".version"))
                    .filter(path -> !expected.contains(path.getFileName().toString()))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                            System.out.println("Removed stale version metadata: " + path.getFileName());
                        } catch (IOException exception) {
                            System.err.println("Could not remove " + path + ": " + exception.getMessage());
                        }
                    });
        }
    }

    private boolean writeIfChanged(Path path, String content) throws IOException {
        if (Files.isRegularFile(path) && Files.readString(path, StandardCharsets.UTF_8).equals(content)) return false;
        Path temporary = Files.createTempFile(path.getParent(), ".manager-", ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        return true;
    }

    private void sleepAfterFailure() {
        try {
            Thread.sleep(CHECK_INTERVAL_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}