package videoforge.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Minimal thread-safe file logger. Writes to {@code logs/<name>.log}.
 * One logger per named topic (editor, ffmpeg, recording, youtube).
 */
public final class AppLog {

    private static final ConcurrentMap<String, AppLog> INSTANCES = new ConcurrentHashMap<>();
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final Path file;
    private final Object lock = new Object();

    private AppLog(Path dir, String name) {
        this.file = dir.resolve(name + ".log");
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
        }
    }

    public static AppLog get(String name) {
        return INSTANCES.computeIfAbsent(name, n -> new AppLog(Path.of(System.getProperty("videoforge.logs.dir", "logs")), n));
    }

    public void info(String message) {
        write("INFO", message, null);
    }

    public void warn(String message) {
        write("WARN", message, null);
    }

    public void error(String message, Throwable t) {
        write("ERROR", message, t);
    }

    public void raw(String line) {
        synchronized (lock) {
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, line + System.lineSeparator(), StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (IOException ignored) {
            }
        }
    }

    private void write(String level, String message, Throwable t) {
        StringBuilder sb = new StringBuilder();
        sb.append(LocalDateTime.now().format(TS))
                .append(' ').append(level).append(' ')
                .append(message);
        if (t != null) {
            sb.append('\n').append(exceptionText(t));
        }
        raw(sb.toString());
        System.out.println("[" + level + "] " + message);
    }

    private static String exceptionText(Throwable t) {
        StringBuilder sb = new StringBuilder(t.toString());
        for (StackTraceElement e : t.getStackTrace()) {
            sb.append("\n    at ").append(e);
        }
        return sb.toString();
    }
}
