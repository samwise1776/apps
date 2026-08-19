package videoforge.editing;

import videoforge.media.MediaFile;
import videoforge.media.MediaLibrary;
import videoforge.timeline.Timeline;
import videoforge.timeline.TimelineClip;
import videoforge.timeline.Track;
import videoforge.utils.TimeUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Model-level editing operations. Every method mutates the timeline only
 * through its public API (so change events fire); the UI wraps calls in undo
 * commands. All operations are non-destructive: source files are never touched.
 */
public final class TimelineOperations {

    public static final long MIN_DURATION_MICROS = TimeUtils.secondsToMicros(0.1);
    private final MediaLibrary mediaLibrary;

    public TimelineOperations(MediaLibrary mediaLibrary) {
        this.mediaLibrary = mediaLibrary;
    }

    // ---------- creating clips ----------

    /** Create a timeline clip from a library asset. */
    public TimelineClip createClip(MediaFile media, long timelineStart) {
        TimelineClip clip = new TimelineClip();
        String kind = media.getKind();
        switch (kind) {
            case "video" -> {
                clip.setKind(TimelineClip.Kind.VIDEO);
                clip.setName(media.getName());
                clip.setHasAudio(media.metadata().isHasAudio());
                clip.setSourceStart(0);
                long sourceEnd = TimeUtils.secondsToMicros(media.metadata().getDurationSeconds());
                clip.setSourceEnd(sourceEnd > 0 ? sourceEnd : TimeUtils.secondsToMicros(5));
                clip.setFps(media.metadata().getFps() > 0 ? media.metadata().getFps() : 30.0);
            }
            case "audio" -> {
                clip.setKind(TimelineClip.Kind.AUDIO);
                clip.setName(media.getName());
                clip.setSourceStart(0);
                long sourceEnd = TimeUtils.secondsToMicros(media.metadata().getDurationSeconds());
                clip.setSourceEnd(sourceEnd > 0 ? sourceEnd : TimeUtils.secondsToMicros(5));
            }
            case "image" -> {
                clip.setKind(TimelineClip.Kind.IMAGE);
                clip.setName(media.getName());
                clip.setSourceStart(0);
                clip.setSourceEnd(TimeUtils.secondsToMicros(5));
            }
            default -> {
                clip.setKind(TimelineClip.Kind.IMAGE);
                clip.setName(media.getName());
                clip.setSourceStart(0);
                clip.setSourceEnd(TimeUtils.secondsToMicros(5));
            }
        }
        clip.setSourcePath(media.getPath());
        clip.setMediaId(media.getId());
        clip.setTimelineStart(timelineStart);
        clip.setPositionX(Double.NaN); // NaN -> auto-center
        clip.setPositionY(Double.NaN);
        return clip;
    }

    /** Create a text clip with default styling. */
    public TimelineClip createTextClip(String text, long timelineStart) {
        TimelineClip clip = new TimelineClip(TimelineClip.Kind.TEXT, "Text");
        clip.setText(new videoforge.effects.TextEffect());
        clip.getText().setText(text == null || text.isBlank() ? "Sample Text" : text);
        clip.setTimelineStart(timelineStart);
        clip.setSourceStart(0);
        clip.setSourceEnd(TimeUtils.secondsToMicros(5));
        clip.setPositionX(Double.NaN);
        clip.setPositionY(Double.NaN);
        return clip;
    }

    // ---------- splitting ----------

    /** Split every clip containing {@code time} (from a selection or all tracks). */
    public List<TimelineClip> splitAt(Timeline tl, long time) {
        List<TimelineClip> created = new ArrayList<>();
        List<TimelineClip> targets = new ArrayList<>();
        if (!tl.selectedIds().isEmpty()) {
            targets.addAll(tl.selectedClips());
        } else {
            targets.addAll(tl.clipsAt(time));
        }
        for (TimelineClip clip : targets) {
            if (clip.isLocked()) {
                continue;
            }
            if (time <= clip.getTimelineStart() || time >= clip.timelineEnd()) {
                continue;
            }
            if (clip.getKind() == TimelineClip.Kind.TEXT || clip.isFreezeFrame()) {
                TimelineClip right = clip.copy();
                right.setTimelineStart(time);
                right.setSourceStart(0);
                right.setSourceEnd(0);
                Track track = tl.trackById(clip.getTrackId());
                clip.setTimelineStart(clip.getTimelineStart());
                clip.setSourceEnd(0);
                track.addClip(right);
                created.add(right);
                continue;
            }
            long offsetInSource = Math.round((time - clip.getTimelineStart()) * clip.getSpeed());
            TimelineClip right = clip.copy();
            right.setTimelineStart(time);
            right.setSourceStart(clip.getSourceStart() + offsetInSource);
            clip.setSourceEnd(clip.getSourceStart() + offsetInSource);
            Track track = tl.trackById(clip.getTrackId());
            track.addClip(right);
            created.add(right);
        }
        if (!created.isEmpty()) {
            tl.fire(Timeline.ChangeType.STRUCTURE);
        }
        return created;
    }

    // ---------- cutting ----------

    /**
     * Cut the range [start, end) from clips. With {@code ripple} the gap closes
     * by shifting later clips on the timeline left.
     */
    public long cutRange(Timeline tl, long start, long end, boolean ripple) {
        if (end <= start) {
            return 0;
        }
        long removed = end - start;
        for (Track track : tl.tracks()) {
            List<TimelineClip> clips = new ArrayList<>(track.clips());
            for (TimelineClip clip : clips) {
                if (clip.isLocked()) {
                    continue;
                }
                long cStart = clip.getTimelineStart();
                long cEnd = clip.timelineEnd();
                if (cEnd <= start || cStart >= end) {
                    continue;
                }
                boolean fullyInside = cStart >= start && cEnd <= end;
                boolean coversWhole = cStart <= start && cEnd >= end;
                if (fullyInside) {
                    track.removeClip(clip);
                } else if (coversWhole) {
                    // split into left and right parts, drop the middle
                    splitClipAt(tl, clip, start);
                    splitClipAt(tl, clip, end);
                } else if (cStart < start && cEnd > start) {
                    // trim right edge down to start
                    clip.setSourceEnd(clip.getSourceStart()
                            + Math.round((start - cStart) * clip.getSpeed()));
                } else if (cStart < end && cEnd > end) {
                    // trim left edge up to end
                    long offset = end - cStart;
                    clip.setTimelineStart(end);
                    clip.setSourceStart(clip.getSourceStart() + Math.round(offset * clip.getSpeed()));
                }
            }
        }
        if (ripple) {
            rippleShift(tl, end, -removed);
        }
        tl.clearSelection();
        tl.fire(Timeline.ChangeType.STRUCTURE);
        return removed;
    }

    private void splitClipAt(Timeline tl, TimelineClip clip, long at) {
        TimelineClip right = clip.copy();
        long offsetInSource = Math.round((at - clip.getTimelineStart()) * clip.getSpeed());
        right.setTimelineStart(at);
        right.setSourceStart(clip.getSourceStart() + offsetInSource);
        clip.setSourceEnd(clip.getSourceStart() + offsetInSource);
        Track track = tl.trackById(clip.getTrackId());
        track.addClip(right);
    }

    /** Shift all clips starting at or after {@code fromTime} by {@code delta}. */
    public void rippleShift(Timeline tl, long fromTime, long delta) {
        for (Track track : tl.tracks()) {
            for (TimelineClip clip : track.clips()) {
                if (clip.getTimelineStart() >= fromTime) {
                    clip.setTimelineStart(clip.getTimelineStart() + delta);
                }
            }
        }
    }

    // ---------- delete ----------

    public void deleteSelected(Timeline tl, boolean ripple) {
        List<TimelineClip> selected = tl.selectedClips();
        if (selected.isEmpty()) {
            return;
        }
        long minStart = selected.stream().mapToLong(TimelineClip::getTimelineStart).min().orElse(0);
        long maxEnd = selected.stream().mapToLong(TimelineClip::timelineEnd).max().orElse(0);
        Set<String> ids = new LinkedHashSet<>();
        for (TimelineClip c : selected) {
            if (c.isLocked()) {
                continue;
            }
            ids.add(c.getId());
        }
        tl.removeClips(ids);
        if (ripple) {
            rippleShift(tl, maxEnd, minStart - maxEnd);
            tl.fire(Timeline.ChangeType.STRUCTURE);
        }
    }

    // ---------- trim (non-destructive) ----------

    public enum Edge { START, END }

    /**
     * Trim a clip edge to a new timeline time. Only source in/out points move;
     * the original file is untouched so the edge can always be dragged back.
     */
    public void trimClip(Timeline tl, TimelineClip clip, Edge edge, long newTime, long sourceDurationMicros) {
        long cStart = clip.getTimelineStart();
        long cEnd = clip.timelineEnd();
        if (edge == Edge.START) {
            long newStart = Math.min(newTime, cEnd - MIN_DURATION_MICROS);
            newStart = Math.max(0, newStart);
            long delta = newStart - cStart;
            long newSourceStart = clip.getSourceStart() + Math.round(delta * clip.getSpeed());
            long maxSourceStart = Math.max(0, sourceDurationMicros - TimeUtils.secondsToMicros(0.05));
            newSourceStart = Math.max(0, Math.min(newSourceStart, maxSourceStart));
            clip.setTimelineStart(newStart);
            clip.setSourceStart(newSourceStart);
        } else {
            long newEnd = Math.max(newTime, cStart + MIN_DURATION_MICROS);
            long newDuration = newEnd - cStart;
            long targetSourceEnd = clip.getSourceStart() + Math.round(newDuration * clip.getSpeed());
            if (sourceDurationMicros > 0) {
                targetSourceEnd = Math.min(targetSourceEnd, sourceDurationMicros);
            }
            clip.setSourceEnd(Math.max(targetSourceEnd, clip.getSourceStart() + 1));
        }
        tl.notifyClipChanged(clip);
    }

    // ---------- restore / uncut ----------

    /** Reset trim bounds to the full source length (uncut). */
    public void restoreClip(Timeline tl, TimelineClip clip, long fullSourceMicros) {
        long duration = clip.timelineEnd() - clip.getTimelineStart();
        clip.setSourceStart(0);
        if (fullSourceMicros > 0) {
            clip.setSourceEnd(Math.min(fullSourceMicros, Math.max(1, Math.round(duration * clip.getSpeed()))));
        } else {
            clip.setSourceEnd(Math.max(1, Math.round(duration * clip.getSpeed())));
        }
        tl.notifyClipChanged(clip);
    }

    /** Reset speed to 1x. */
    public void resetSpeed(Timeline tl, TimelineClip clip) {
        long duration = clip.timelineEnd() - clip.getTimelineStart();
        clip.setSpeed(1.0);
        clip.setSourceEnd(clip.getSourceStart() + Math.max(1, duration));
        tl.notifyClipChanged(clip);
    }

    // ---------- move ----------

    public void moveClips(Timeline tl, List<String> ids, long deltaMicros) {
        for (String id : ids) {
            TimelineClip c = tl.clipById(id);
            if (c != null && !c.isLocked()) {
                c.setTimelineStart(c.getTimelineStart() + deltaMicros);
            }
        }
        tl.fire(Timeline.ChangeType.STRUCTURE);
    }

    // ---------- duplicate / copy-paste ----------

    public List<TimelineClip> duplicateClips(Timeline tl, List<String> ids) {
        List<TimelineClip> copies = new ArrayList<>();
        for (String id : ids) {
            TimelineClip src = tl.clipById(id);
            if (src == null) {
                continue;
            }
            TimelineClip copy = src.copy();
            long start = tl.duration();
            copy.setTimelineStart(start);
            Track track = tl.trackById(src.getTrackId());
            if (track != null) {
                track.addClip(copy);
                copies.add(copy);
            }
        }
        if (!copies.isEmpty()) {
            tl.fire(Timeline.ChangeType.STRUCTURE);
        }
        return copies;
    }

    /** Join two adjacent clips from the same source on the same track. */
    public boolean joinClips(Timeline tl, List<String> ids) {
        List<TimelineClip> clips = new ArrayList<>();
        for (String id : ids) {
            TimelineClip c = tl.clipById(id);
            if (c != null) {
                clips.add(c);
            }
        }
        if (clips.size() < 2) {
            return false;
        }
        clips.sort(Comparator.comparingLong(TimelineClip::getTimelineStart));
        TimelineClip first = clips.get(0);
        Track track = tl.trackById(first.getTrackId());
        for (int i = 1; i < clips.size(); i++) {
            TimelineClip next = clips.get(i);
            if (!next.getTrackId().equals(first.getTrackId())) {
                continue;
            }
            if (Math.abs(next.getTimelineStart() - first.timelineEnd()) > TimeUtils.secondsToMicros(0.02)) {
                continue;
            }
            if (first.getKind() == TimelineClip.Kind.TEXT || first.getKind() == TimelineClip.Kind.IMAGE) {
                first.setTimelineStart(Math.min(first.getTimelineStart(), next.getTimelineStart()));
                first.setSourceEnd(Math.max(first.timelineEnd(), next.timelineEnd()));
                track.removeClip(next);
            } else {
                first.setSourceEnd(next.getSourceEnd());
                track.removeClip(next);
            }
        }
        tl.fire(Timeline.ChangeType.STRUCTURE);
        return true;
    }

    // ---------- detach audio ----------

    /** Split a video clip's audio into a separate audio clip. */
    public TimelineClip detachAudio(Timeline tl, TimelineClip videoClip) {
        if (videoClip.getKind() != TimelineClip.Kind.VIDEO
                || !videoClip.isHasAudio() || videoClip.isAudioDetached()) {
            return null;
        }
        TimelineClip audio = new TimelineClip(TimelineClip.Kind.AUDIO, videoClip.getName() + " audio");
        audio.setSourcePath(videoClip.getSourcePath());
        audio.setMediaId(videoClip.getMediaId());
        audio.setSourceStart(videoClip.getSourceStart());
        audio.setSourceEnd(videoClip.getSourceEnd());
        audio.setTimelineStart(videoClip.getTimelineStart());
        audio.setVolume(videoClip.getVolume());
        audio.setMuted(videoClip.isMuted());
        Track audioTrack = tl.defaultTrackFor(TimelineClip.Kind.AUDIO);
        audioTrack.addClip(audio);
        videoClip.setAudioDetached(true);
        tl.fire(Timeline.ChangeType.STRUCTURE);
        return audio;
    }

    // ---------- source helpers ----------

    /** Full source duration for a clip, if resolvable. */
    public long sourceDurationMicros(TimelineClip clip) {
        if (clip.getSourcePath() != null && Files.exists(Path.of(clip.getSourcePath()))) {
            MediaFile mf = mediaLibrary.byPath(Path.of(clip.getSourcePath()));
            if (mf != null && mf.metadata().getDurationSeconds() > 0) {
                return TimeUtils.secondsToMicros(mf.metadata().getDurationSeconds());
            }
            return TimeUtils.secondsToMicros(videoforge.config.AppConfig.get().getDouble("probeDurationFallback"));
        }
        return 0;
    }

    public long sourceDurationMicrosOf(MediaFile media) {
        return media == null ? 0 : TimeUtils.secondsToMicros(media.metadata().getDurationSeconds());
    }
}
