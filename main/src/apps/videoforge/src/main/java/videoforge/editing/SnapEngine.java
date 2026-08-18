package videoforge.editing;

import videoforge.timeline.Timeline;
import videoforge.timeline.TimelineClip;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Snap engine for timeline editing. Converts candidate times to pixel space and
 * snaps them onto playhead, markers, clip edges and the timeline boundaries when
 * within the configured pixel threshold.
 */
public final class SnapEngine {

    public interface PxConverter {
        double timeToPx(long micros);
    }

    private final PxConverter converter;
    private final int thresholdPx;

    public SnapEngine(PxConverter converter, int thresholdPx) {
        this.converter = converter;
        this.thresholdPx = thresholdPx;
    }

    /** All candidate snap times for a given timeline state. */
    public static List<Long> collectSnapTimes(Timeline tl, Collection<TimelineClip> excluded) {
        List<Long> out = new ArrayList<>();
        out.add(0L);                                    // timeline start
        out.add(tl.duration());                          // timeline end
        out.add(tl.playhead());                          // playhead
        for (var m : tl.markers()) {
            out.add(m.getTimeMicros());
        }
        for (TimelineClip c : tl.allClips()) {
            if (excluded != null && excluded.contains(c)) {
                continue;
            }
            out.add(c.getTimelineStart());
            out.add(c.timelineEnd());
        }
        return out;
    }

    /**
     * Snap a candidate time. Returns the snapped time if a snap point is within
     * threshold pixels, otherwise the original candidate.
     */
    public long snap(long candidate, List<Long> snapTimes) {
        double px = converter.timeToPx(candidate);
        long best = candidate;
        double bestDist = Double.MAX_VALUE;
        for (Long t : snapTimes) {
            double d = Math.abs(converter.timeToPx(t) - px);
            if (d < bestDist) {
                bestDist = d;
                best = t;
            }
        }
        return bestDist <= thresholdPx ? best : candidate;
    }

    /** Convenience: snap using the timeline's current clips/playhead/markers. */
    public long snap(Timeline tl, long candidate, Collection<TimelineClip> excluded) {
        return snap(candidate, collectSnapTimes(tl, excluded));
    }

    /** Test whether two clip edges would snap together (used for proximity guides). */
    public boolean wouldSnap(long a, long b) {
        return Math.abs(converter.timeToPx(a) - converter.timeToPx(b)) <= thresholdPx;
    }
}
