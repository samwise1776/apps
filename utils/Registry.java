package utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** Finds and keeps track of every application in the apps directory. */
public final class Registry {
    private final Path appsDirectory;
    private final Path countFile;

    public Registry(Path dataDirectory) {
        appsDirectory = dataDirectory.resolve("apps");
        countFile = dataDirectory.resolve(".apps.txt");
    }

    public List<Path> refresh() throws IOException {
        Files.createDirectories(appsDirectory);
        List<Path> applications;
        try (Stream<Path> entries = Files.list(appsDirectory)) {
            applications = entries
                    .filter(path -> !path.getFileName().toString().startsWith("."))
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(
                            path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .toList();
        }

        String count = applications.isEmpty() ? "None" : Integer.toString(applications.size());
        if (!Files.isRegularFile(countFile)
                || !Files.readString(countFile, StandardCharsets.UTF_8).equals(count)) {
            Files.writeString(countFile, count, StandardCharsets.UTF_8);
        }
        return applications;
    }
}
