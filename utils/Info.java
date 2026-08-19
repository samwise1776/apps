package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/** Reads application source information. */
public final class Info {
    private static final Set<String> SOURCE_EXTENSIONS = Set.of(
            "java", "c", "cc", "cpp", "h", "hpp", "cs", "js", "jsx", "ts", "tsx",
            "py", "go", "rs", "rb", "php", "swift", "kt", "kts", "scala", "sh",
            "html", "css", "scss", "sql", "xml", "json", "yaml", "yml", "toml", "md");

    private final Logger logger;

    public Info(Logger logger) {
        this.logger = logger;
    }

    public AppDetails read(Path application) throws IOException {
        long lines = Files.isRegularFile(application)
                ? (isSourceFile(application) ? countLines(application) : 0)
                : countDirectoryLines(application);
        return new AppDetails(application.getFileName().toString(), application, lines);
    }

    public void display(AppDetails app) {
        System.out.printf("%-28s %,7d lines -> v%s%n",
                app.name(), app.lines(), Updater.semanticVersion(app.lines() / 100));
    }

    private long countDirectoryLines(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) return 0;
        try (Stream<Path> files = Files.walk(directory)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> !isGeneratedPath(directory, path))
                    .filter(this::isSourceFile)
                    .mapToLong(file -> {
                        try {
                            return countLines(file);
                        } catch (IOException exception) {
                            logger.error("Could not count " + file, exception);
                            return 0;
                        }
                    })
                    .sum();
        }
    }

    private boolean isGeneratedPath(Path application, Path path) {
        Path relative = application.relativize(path);
        for (Path part : relative) {
            String name = part.toString();
            if (name.equals("bin") || name.equals("obj") || name.equals("build")
                    || name.equals("runtime") || name.equals(".git")) return true;
        }
        return false;
    }

    private boolean isSourceFile(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0
                && SOURCE_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private long countLines(Path file) throws IOException {
        long count = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            while (reader.readLine() != null) count++;
        }
        return count;
    }

    public record AppDetails(String name, Path path, long lines) { }
}
