package data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Creates a current size, file, line, and log report for the Datacenter workspace. */
public final class Data {
    private static final int TEXT_SAMPLE_SIZE = 8_192;

    private Data() { }

    public static void main(String[] args) {
        Path root = args.length == 0
                ? Paths.get(System.getProperty("user.home"), "Data")
                : Paths.get(args[0]).toAbsolutePath().normalize();
        Path output = root.resolve("data/data.txt");
        Path logPath = root.resolve(".data/logs/system.log");

        try {
            WorkspaceStats stats = inspect(root);
            List<String> logs = Files.isRegularFile(logPath)
                    ? Files.readAllLines(logPath, StandardCharsets.UTF_8)
                    : List.of("No system log found.");

            String report = "VERSION: v1.0.0\n"
                    + "TITLE: Datacenter\n"
                    + "LOCATION: " + root + "\n"
                    + "SIZE: " + humanSize(stats.bytes()) + "\n"
                    + "SIZE_BYTES: " + stats.bytes() + "\n"
                    + "FILES: " + stats.files() + "\n"
                    + "TEXT_FILES: " + stats.textFiles() + "\n"
                    + "LINES: " + stats.lines() + "\n"
                    + "JAVA_FILES: " + stats.javaFiles() + "\n"
                    + "JAVA_LINES: " + stats.javaLines() + "\n"
                    + "LOGS:\n"
                    + String.join("\n", logs)
                    + "\n";

            Files.createDirectories(output.getParent());
            Files.writeString(output, report, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.print(report);
        } catch (IOException exception) {
            System.err.println("Could not inspect Datacenter: " + exception.getMessage());
            System.exit(1);
        }
    }

    public static WorkspaceStats inspect(Path root) throws IOException {
        if (!Files.isDirectory(root)) throw new IOException("Directory not found: " + root);

        List<Path> files = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile).forEach(files::add);
        }

        long bytes = 0;
        long lines = 0;
        long textFiles = 0;
        long javaFiles = 0;
        long javaLines = 0;

        for (Path file : files) {
            bytes += Files.size(file);
            if (!isProbablyText(file)) continue;
            try {
                long fileLines = countLines(file);
                lines += fileLines;
                textFiles++;
                if (file.getFileName().toString().endsWith(".java")) {
                    javaFiles++;
                    javaLines += fileLines;
                }
            } catch (MalformedInputException ignored) {
                // A binary file passed the sample check; exclude it from text-line totals.
            }
        }
        return new WorkspaceStats(bytes, files.size(), textFiles, lines, javaFiles, javaLines);
    }

    private static boolean isProbablyText(Path file) throws IOException {
        byte[] sample = new byte[TEXT_SAMPLE_SIZE];
        int length;
        try (InputStream input = Files.newInputStream(file)) {
            length = input.read(sample);
        }
        if (length <= 0) return true;

        int suspicious = 0;
        for (int index = 0; index < length; index++) {
            int value = sample[index] & 0xff;
            if (value == 0) return false;
            if (value < 9 || (value > 13 && value < 32)) suspicious++;
        }
        return suspicious * 100L <= length;
    }

    private static long countLines(Path file) throws IOException {
        long count = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            while (reader.readLine() != null) count++;
        }
        return count;
    }

    private static String humanSize(long bytes) {
        if (bytes < 1_024) return bytes + " B";
        String[] units = {"KiB", "MiB", "GiB", "TiB"};
        double value = bytes;
        int unit = -1;
        do {
            value /= 1_024.0;
            unit++;
        } while (value >= 1_024 && unit < units.length - 1);
        return String.format("%.2f %s", value, units[unit]);
    }

    public record WorkspaceStats(
            long bytes,
            long files,
            long textFiles,
            long lines,
            long javaFiles,
            long javaLines) { }
}
