package videoforge.timeline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The observable timeline model: tracks, clips, playhead, markers, selection.
 *
 * <p>All edits funnel through this class and fire {@link Listener} callbacks so
 * the UI can refresh itself. The undo system wraps mutations into
 * {@code EditCommand}s executed against this model.</p>
 */
public final class Timeline {

    public enum ChangeType {
        STRUCTURE,      // tracks added/removed/reordered, clips added/removed
        CLIP,           // a clip's properties changed
        SELECTION,
        PLAYHEAD,
        MARKER,
        TRACK,          // track-level flags (mute/solo/lock/hide/rename)
        RANGE,          // in/out points changed
        PROJECT         // resolution/fps changed
    }

    @FunctionalInterface
    public interface Listener {
        void timelineChanged(ChangeType type);
    }

    private final List<Track> tracks = new ArrayList<>();
    private final List<Marker> markers = new ArrayList<>();
    private final Set<String> selectedIds = new LinkedHashSet<>();

    private final List<Listener> listeners = new ArrayList<>();

    private double fps = 30.0;
    private int canvasWidth = 1920;
    private int canvasHeight = 1080;
    private long playheadMicros;
    private long inPoint = -1;
    private long outPoint = -1;

    public Timeline() {
        resetToDefaults();
    }

    public void resetToDefaults() {
        tracks.clear();
        markers.clear();
        selectedIds.clear();
        playheadMicros = 0;
        inPoint = -1;
        outPoint = -1;
        Track v1 = new Track(Track.Kind.VIDEO, "V1");
        Track v2 = new Track(Track.Kind.VIDEO, "V2");
        Track v3 = new Track(Track.Kind.VIDEO, "V3");
        Track a1 = new Track(Track.Kind.AUDIO, "A1");
        Track a2 = new Track(Track.Kind.AUDIO, "A2");
        Track a3 = new Track(Track.Kind.AUDIO, "A3");
        Track t1 = new Track(Track.Kind.TEXT, "TEXT");
        Track i1 = new Track(Track.Kind.IMAGE, "IMAGES");
        Track e1 = new Track(Track.Kind.EFFECT, "EFFECTS");
        tracks.addAll(List.of(v1, v2, v3, a1, a2, a3, t1, i1, e1));
        assignZOrders();
        fire(ChangeType.STRUCTURE);
    }

    private void assignZOrders() {
        for (int i = 0; i < tracks.size(); i++) {
            tracks.get(i).setZOrder(i);
        }
    }

    // ---------- listeners ----------

    public void addListener(Listener l) {
        listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    public void fire(ChangeType type) {
        for (Listener l : new ArrayList<>(listeners)) {
            try {
                l.timelineChanged(type);
            } catch (Exception e) {
                // a listener must never break the model
            }
        }
    }

    // ---------- tracks ----------

    public List<Track> tracks() { return tracks; }

    public List<Track> tracksOf(Track.Kind kind) {
        return tracks.stream().filter(t -> t.getKind() == kind).collect(java.util.stream.Collectors.toList());
    }

    public Track trackById(String id) {
        return tracks.stream().filter(t -> t.getId().equals(id)).findFirst().orElse(null);
    }

    public Track addTrack(Track.Kind kind) {
        int count = tracksOf(kind).size();
        String prefix = switch (kind) {
            case VIDEO -> "V";
            case AUDIO -> "A";
            case TEXT -> "TEXT";
            case IMAGE -> "IMG";
            case EFFECT -> "FX";
        };
        Track t = new Track(kind, prefix + (count + 1));
        tracks.add(t);
        assignZOrders();
        fire(ChangeType.STRUCTURE);
        return t;
    }

    public void removeTrack(String trackId) {
        Track t = trackById(trackId);
        if (t == null) {
            return;
        }
        long minVideo = tracksOf(Track.Kind.VIDEO).size();
        long minAudio = tracksOf(Track.Kind.AUDIO).size();
        boolean keep = (t.getKind() == Track.Kind.VIDEO && minVideo <= 1)
                || (t.getKind() == Track.Kind.AUDIO && minAudio <= 1);
        if (keep) {
            return;
        }
        tracks.remove(t);
        assignZOrders();
        fire(ChangeType.STRUCTURE);
    }

    /** Move a track in the display order; direction +1 = down, -1 = up. */
    public void reorderTrack(String trackId, int delta) {
        int idx = -1;
        for (int i = 0; i < tracks.size(); i++) {
            if (tracks.get(i).getId().equals(trackId)) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            return;
        }
        int target = idx + delta;
        if (target < 0 || target >= tracks.size()) {
            return;
        }
        Track t = tracks.remove(idx);
        tracks.add(target, t);
        assignZOrders();
        fire(ChangeType.STRUCTURE);
    }

    public Track defaultTrackFor(TimelineClip.Kind kind) {
        return switch (kind) {
            case VIDEO -> tracksOf(Track.Kind.VIDEO).get(0);
            case AUDIO -> tracksOf(Track.Kind.AUDIO).get(0);
            case TEXT -> tracksOf(Track.Kind.TEXT).get(0);
            case IMAGE -> tracksOf(Track.Kind.IMAGE).get(0);
            case SHAPE -> tracksOf(Track.Kind.IMAGE).get(0);
        };
    }

    // ---------- clips ----------

    public void addClip(TimelineClip clip) {
        addClip(clip, defaultTrackFor(clip.getKind()));
    }

    public void addClip(TimelineClip clip, Track track) {
        clip.setTrackId(track.getId());
        track.addClip(clip);
        fire(ChangeType.STRUCTURE);
    }

    public TimelineClip clipById(String id) {
        for (Track t : tracks) {
            TimelineClip c = t.clipById(id);
            if (c != null) {
                return c;
            }
        }
        return null;
    }

    public List<TimelineClip> allClips() {
        List<TimelineClip> out = new ArrayList<>();
        for (Track t : tracks) {
            out.addAll(t.clips());
        }
        return out;
    }

    public Track trackOf(TimelineClip clip) {
        return trackById(clip.getTrackId());
    }

    public void removeClips(Collection<String> clipIds) {
        Set<String> ids = new LinkedHashSet<>(clipIds);
        for (Track t : tracks) {
            t.clips().removeIf(c -> ids.contains(c.getId()));
        }
        selectedIds.removeAll(ids);
        fire(ChangeType.STRUCTURE);
    }

    public void removeClip(String clipId) {
        removeClips(List.of(clipId));
    }

    public void clear() {
        for (Track t : tracks) {
            t.clips().clear();
        }
        selectedIds.clear();
        fire(ChangeType.STRUCTURE);
    }

    public void moveClipToTrack(TimelineClip clip, Track target) {
        Track from = trackOf(clip);
        if (from != null) {
            from.removeClip(clip);
        }
        clip.setTrackId(target.getId());
        target.addClip(clip);
        fire(ChangeType.STRUCTURE);
    }

    public void notifyClipChanged(TimelineClip clip) {
        fire(ChangeType.CLIP);
    }

    /** Project duration = the furthest point any clip, marker or range reaches. */
    public long duration() {
        long end = 0;
        for (Track t : tracks) {
            end = Math.max(end, t.end());
        }
        for (Marker m : markers) {
            end = Math.max(end, m.getTimeMicros());
        }
        if (outPoint > end) {
            end = outPoint;
        }
        return end;
    }

    public List<TimelineClip> clipsAt(long time) {
        List<TimelineClip> out = new ArrayList<>();
        for (Track t : tracks) {
            TimelineClip c = t.clipAt(time);
            if (c != null) {
                out.add(c);
            }
        }
        return out;
    }

    // ---------- selection ----------

    public Set<String> selectedIds() { return selectedIds; }

    public List<TimelineClip> selectedClips() {
        List<TimelineClip> out = new ArrayList<>();
        for (String id : selectedIds) {
            TimelineClip c = clipById(id);
            if (c != null) {
                out.add(c);
            }
        }
        return out;
    }

    public void select(String clipId) {
        selectedIds.clear();
        selectedIds.add(clipId);
        fire(ChangeType.SELECTION);
    }

    public void select(List<String> ids) {
        selectedIds.clear();
        selectedIds.addAll(ids);
        fire(ChangeType.SELECTION);
    }

    public void addToSelection(String clipId) {
        selectedIds.add(clipId);
        fire(ChangeType.SELECTION);
    }

    public void clearSelection() {
        if (selectedIds.isEmpty()) {
            return;
        }
        selectedIds.clear();
        fire(ChangeType.SELECTION);
    }

    // ---------- playhead / range ----------

    public long playhead() { return playheadMicros; }

    public void setPlayhead(long time) {
        long clamped = Math.max(0, time);
        if (clamped == playheadMicros) {
            return;
        }
        playheadMicros = clamped;
        fire(ChangeType.PLAYHEAD);
    }

    public void setInPoint(long time) {
        this.inPoint = time;
        fire(ChangeType.RANGE);
    }

    public void setOutPoint(long time) {
        this.outPoint = time;
        fire(ChangeType.RANGE);
    }

    public long inPoint() { return inPoint; }
    public long outPoint() { return outPoint; }

    public void clearRange() {
        inPoint = -1;
        outPoint = -1;
        fire(ChangeType.RANGE);
    }

    // ---------- markers ----------

    public List<Marker> markers() { return markers; }

    public void addMarker(Marker m) {
        markers.add(m);
        markers.sort(Comparator.comparingLong(Marker::getTimeMicros));
        fire(ChangeType.MARKER);
    }

    public void removeMarker(String markerId) {
        markers.removeIf(m -> m.getId().equals(markerId));
        fire(ChangeType.MARKER);
    }

    public void notifyMarkersChanged() {
        fire(ChangeType.MARKER);
    }

    /** Markers marked as chapters, formatted as a YouTube description block. */
    public String chapterText() {
        StringBuilder sb = new StringBuilder();
        for (Marker m : markers) {
            if (!m.isChapter()) {
                continue;
            }
            String time = Timecode.of(m.getTimeMicros());
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(time).append(' ').append(m.getName());
        }
        return sb.toString();
    }

    // ---------- project settings ----------

    public double fps() { return fps; }

    public void setFps(double fps) {
        if (fps <= 0) {
            return;
        }
        this.fps = fps;
        fire(ChangeType.PROJECT);
    }

    public int canvasWidth() { return canvasWidth; }
    public int canvasHeight() { return canvasHeight; }

    public void setCanvasSize(int width, int height) {
        this.canvasWidth = width;
        this.canvasHeight = height;
        fire(ChangeType.PROJECT);
    }

    public void setCanvasWidth(int width) { setCanvasSize(width, canvasHeight); }
    public void setCanvasHeight(int height) { setCanvasSize(canvasWidth, height); }

    public void forEachClip(Consumer<TimelineClip> action) {
        for (Track t : tracks) {
            for (TimelineClip c : t.clips()) {
                action.accept(c);
            }
        }
    }
}
