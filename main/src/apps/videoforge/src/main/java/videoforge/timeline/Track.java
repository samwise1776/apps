package videoforge.timeline;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * A timeline track holding clips. Tracks are typed; the timeline guarantees at
 * least one track of each core type. Tracks support mute/solo/lock/hide/rename.
 */
public final class Track {

    public enum Kind { VIDEO, AUDIO, TEXT, IMAGE, EFFECT }

    private String id = UUID.randomUUID().toString();
    private Kind kind;
    private String name;
    private int zOrder;                     // higher renders on top
    private boolean muted;
    private boolean soloed;
    private boolean locked;
    private boolean hidden;

    private final List<TimelineClip> clips = new ArrayList<>();

    public Track() {}

    public Track(Kind kind, String name) {
        this.kind = kind;
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Kind getKind() { return kind; }
    public void setKind(Kind kind) { this.kind = kind; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getZOrder() { return zOrder; }
    public void setZOrder(int zOrder) { this.zOrder = zOrder; }
    public boolean isMuted() { return muted; }
    public void setMuted(boolean muted) { this.muted = muted; }
    public boolean isSoloed() { return soloed; }
    public void setSoloed(boolean soloed) { this.soloed = soloed; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }

    public List<TimelineClip> clips() { return clips; }

    public void addClip(TimelineClip clip) {
        clip.setTrackId(id);
        if (!clips.contains(clip)) {
            clips.add(clip);
        }
        clips.sort(Comparator.comparingLong(TimelineClip::getTimelineStart));
    }

    public void removeClip(String clipId) {
        clips.removeIf(c -> c.getId().equals(clipId));
    }

    public void removeClip(TimelineClip clip) {
        clips.remove(clip);
    }

    public TimelineClip clipAt(long timelineTime) {
        for (TimelineClip c : clips) {
            if (c.isClipAt(timelineTime)) {
                return c;
            }
        }
        return null;
    }

    public TimelineClip clipById(String clipId) {
        for (TimelineClip c : clips) {
            if (c.getId().equals(clipId)) {
                return c;
            }
        }
        return null;
    }

    public long end() {
        long end = 0;
        for (TimelineClip c : clips) {
            end = Math.max(end, c.timelineEnd());
        }
        return end;
    }

    public Track copy() {
        Track t = new Track(kind, name);
        t.zOrder = zOrder;
        t.muted = muted;
        t.soloed = soloed;
        t.locked = locked;
        t.hidden = hidden;
        return t;
    }
}
