package videoforge.utils;

/**
 * Time helpers: timecode formatting and parsing.
 *
 * <p>All timeline times are stored in microseconds (long) to avoid float rounding
 * issues when frame-stepping and rendering. Media/duration values from FFprobe
 * arrive in seconds (double) and are converted with {@link #secondsToMicros}.</p>
 */
public final class TimeUtils {

    private TimeUtils() {}

    public static final long MICROS_PER_SECOND = 1_000_000L;

    /** Convert a second value (double, e.g. from FFprobe) to microseconds. */
    public static long secondsToMicros(double seconds) {
        return Math.round(seconds * MICROS_PER_SECOND);
    }

    /** Convert microseconds back to fractional seconds. */
    public static double microsToSeconds(long micros) {
        return micros / (double) MICROS_PER_SECOND;
    }

    /** Format microseconds as HH:MM:SS:FF at a given frame rate (drop none). */
    public static String toTimecode(long micros, double fps) {
        double frames = fps > 0 ? fps : 30.0;
        long totalFrames = Math.round(micros / MICROS_PER_SECOND * frames);
        long f = totalFrames % Math.round(frames);
        long totalSeconds = totalFrames / Math.round(frames);
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d:%02d", h, m, s, f);
    }

    /** Format microseconds as HH:MM:SS.mmm. */
    public static String toHMS(long micros) {
        long totalMs = micros / 1000;
        long h = totalMs / 3_600_000;
        long m = (totalMs % 3_600_000) / 60_000;
        long s = (totalMs % 60_000) / 1000;
        long ms = totalMs % 1000;
        return String.format("%02d:%02d:%02d.%03d", h, m, s, ms);
    }

    /** Format a duration (in microseconds) compactly, e.g. "1:23.456". */
    public static String toDuration(long micros) {
        long totalMs = micros / 1000;
        long m = totalMs / 60_000;
        long s = (totalMs % 60_000) / 1000;
        long ms = totalMs % 1000;
        if (m > 0) {
            return String.format("%d:%02d.%03d", m, s, ms);
        }
        return String.format("%d.%03ds", s, ms);
    }

    /** Parse "HH:MM:SS.mmm" or "MM:SS.mmm" or a plain second value into microseconds. */
    public static long parseTimecode(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        String v = value.trim();
        try {
            if (!v.contains(":")) {
                return secondsToMicros(Double.parseDouble(v));
            }
            String[] parts = v.split(":");
            long h = parts.length == 3 ? Long.parseLong(parts[0]) : 0;
            long m = parts.length == 3 ? Long.parseLong(parts[1]) : Long.parseLong(parts[0]);
            String secPart = parts[parts.length - 1];
            double s = Double.parseDouble(secPart);
            return (h * 3600 + m * 60 + (long) s) * MICROS_PER_SECOND
                    + Math.round((s - (long) s) * MICROS_PER_SECOND);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
