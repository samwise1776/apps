package videoforge.timeline;

/**
 * Timecode formatting for YouTube chapter timestamps (HH:MM:SS).
 */
public final class Timecode {

    private Timecode() {}

    public static String of(long micros) {
        long totalSeconds = micros / 1_000_000L;
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        return h > 0
                ? String.format("%d:%02d:%02d", h, m, s)
                : String.format("%d:%02d", m, s);
    }
}
