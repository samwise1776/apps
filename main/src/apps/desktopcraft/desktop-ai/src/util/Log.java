package util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

/** Simple thread-safe leveled logger for the desktop AI platform. */
public final class Log {
    public enum Level { DEBUG, INFO, WARN, ERROR }

    private static final AtomicBoolean ENABLED = new AtomicBoolean(true);
    private static volatile Level threshold = Level.INFO;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private Log() {}

    public static void setEnabled(boolean on) {
        ENABLED.set(on);
    }

    public static void setThreshold(Level level) {
        threshold = level;
    }

    public static void debug(String message, Object... args) {
        write(Level.DEBUG, message, args);
    }

    public static void info(String message, Object... args) {
        write(Level.INFO, message, args);
    }

    public static void warn(String message, Object... args) {
        write(Level.WARN, message, args);
    }

    public static void error(String message, Object... args) {
        write(Level.ERROR, message, args);
    }

    public static void error(String message, Throwable throwable) {
        StringWriter buffer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(buffer));
        write(Level.ERROR, message + "\n" + buffer);
    }

    private static void write(Level level, String message, Object... args) {
        if (!ENABLED.get() || level.ordinal() < threshold.ordinal()) {
            return;
        }
        String rendered = args.length == 0 ? message : String.format(message, args);
        System.err.println(String.format("[%s] %s %s",
                LocalDateTime.now().format(TS), level, rendered));
    }
}
