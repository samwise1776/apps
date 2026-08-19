package utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Records manager activity in the console and .data/logs/system.log. */
public final class Logger {
    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Path logFile;

    public Logger(Path dataDirectory) throws IOException {
        Path logsDirectory = dataDirectory.resolve(".data").resolve("logs");
        Files.createDirectories(logsDirectory);
        logFile = logsDirectory.resolve("system.log");
    }

    public synchronized void info(String message) {
        write("INFO", message, false);
    }

    public synchronized void error(String message, Throwable cause) {
        String details = cause == null || cause.getMessage() == null
                ? message
                : message + ": " + cause.getMessage();
        write("ERROR", details, true);
    }

    private void write(String level, String message, boolean error) {
        String entry = "[" + LocalDateTime.now().format(FORMAT) + "] [" + level + "] " + message;
        if (error) System.err.println(entry);
        else System.out.println(entry);

        try {
            Files.writeString(logFile, entry + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            System.err.println("Could not write manager log: " + exception.getMessage());
        }
    }
}
