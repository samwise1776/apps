package utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/** Calculates versions, updates metadata, and removes stale version files. */
public final class Updater {
    private final Path versionsDirectory;
    private final Logger logger;

    public Updater(Path dataDirectory, Logger logger) throws IOException {
        versionsDirectory = dataDirectory.resolve("versions");
        Files.createDirectories(versionsDirectory);
        this.logger = logger;
    }

    public void update(Info.AppDetails app) throws IOException {
        long completedHundreds = app.lines() / 100;
        String version = semanticVersion(completedHundreds);
        String metadata = "app=" + app.name() + "\n"
                + "lines=" + app.lines() + "\n"
                + "completed_hundreds=" + completedHundreds + "\n"
                + "version=" + version + "\n";
        Path output = versionsDirectory.resolve(metadataName(app.name()));
        if (writeIfChanged(output, metadata)) {
            logger.info("Updated " + app.name() + " metadata to v" + version);
        }
    }

    public void removeStaleMetadata(List<Path> applications) throws IOException {
        Set<String> expected = new HashSet<>();
        for (Path app : applications) expected.add(metadataName(app.getFileName().toString()));

        try (Stream<Path> files = Files.list(versionsDirectory)) {
            for (Path file : files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".version"))
                    .toList()) {
                if (!expected.contains(file.getFileName().toString())) {
                    Files.deleteIfExists(file);
                    logger.info("Removed stale version metadata: " + file.getFileName());
                }
            }
        }
    }

    public static String semanticVersion(long completedHundreds) {
        long major = completedHundreds / 100;
        long minor = (completedHundreds / 10) % 10;
        long patch = completedHundreds % 10;
        return major + "." + minor + "." + patch;
    }

    private String metadataName(String appName) {
        return appName.replaceAll("[^A-Za-z0-9._-]", "_") + ".version";
    }

    private boolean writeIfChanged(Path path, String content) throws IOException {
        if (Files.isRegularFile(path)
                && Files.readString(path, StandardCharsets.UTF_8).equals(content)) return false;
        Path temporary = Files.createTempFile(versionsDirectory, ".updater-", ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        return true;
    }
}
