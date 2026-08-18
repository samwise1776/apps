package videoforge.timeline;

import videoforge.effects.BlurEffect;
import videoforge.effects.ChromaKeyEffect;
import videoforge.effects.ColorEffect;
import videoforge.effects.CropEffect;
import videoforge.effects.Effect;
import videoforge.effects.TextEffect;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A single clip on the timeline.
 *
 * <p>Editing is non-destructive: the source file is never modified. The visible
 * window into the source is defined by {@code sourceStart} (in point) and
 * {@code sourceEnd} (out point). Trimming only changes these bounds, so any cut
 * can be restored by dragging the edge back (uncut).</p>
 */
public final class TimelineClip {

    public enum Kind { VIDEO, AUDIO, TEXT, IMAGE, SHAPE }

    /** Property names usable as keyframe targets. */
    public static final String P_POSITION_X = "positionX";
    public static final String P_POSITION_Y = "positionY";
    public static final String P_SCALE = "scale";
    public static final String P_ROTATION = "rotation";
    public static final String P_OPACITY = "opacity";
    public static final String P_VOLUME = "volume";
    public static final String P_BLUR = "blurStrength";

    private String id = UUID.randomUUID().toString();
    private Kind kind = Kind.VIDEO;
    private String name = "Clip";
    private String sourcePath;                 // null for generated clips (text/shape)
    private String mediaId;                    // reference into the media library
    private String trackId;
    private String color = "#5a7d9a";          // track/clip label color
    private String label = "";

    // ---- timing (all microseconds) ----
    private long timelineStart;
    private long sourceStart;
    private long sourceEnd;
    private double speed = 1.0;
    private boolean reverse;
    private boolean freezeFrame;
    private double fps = 30.0;

    // ---- transform ----
    private double positionX = 960;            // overlay center in canvas pixels
    private double positionY = 540;
    private double scale = 1.0;                // multiplier on top of "fit"
    private double scaleX = 1.0;
    private double scaleY = 1.0;
    private double rotation;                    // degrees
    private double opacity = 1.0;               // 0..1

    // ---- audio ----
    private double volume = 1.0;                // linear multiplier 0..2
    private boolean muted;
    private boolean hasAudio;                   // source contains an audio stream
    private boolean audioDetached;              // true once "Detach Audio" ran

    // ---- state ----
    private boolean enabled = true;
    private boolean locked;
    private boolean hidden;
    private boolean favorite;

    private final List<Effect> effects = new ArrayList<>();
    private final Map<String, List<Keyframe>> keyframes = new java.util.LinkedHashMap<>();

    private TextEffect text;                    // non-null for TEXT clips
    private String shapeType = "rectangle";     // for SHAPE clips (pen/highlighter/circle/arrow)

    // ---- transitions ----
    private TransitionType transitionIn = TransitionType.NONE;
    private TransitionType transitionOut = TransitionType.NONE;
    private double transitionInDuration = 0.5;
    private double transitionOutDuration = 0.5;

    public TimelineClip() {}

    public TimelineClip(Kind kind, String name) {
        this.kind = kind;
        this.name = name;
        if (kind == Kind.TEXT) {
            this.text = new TextEffect();
        }
    }

    // ---------- keyframe helpers ----------

    public List<Keyframe> keyframes(String property) {
        return keyframes.computeIfAbsent(property, k -> new ArrayList<>());
    }

    public Map<String, List<Keyframe>> allKeyframes() {
        return keyframes;
    }

    /**
     * Evaluate a keyframed property at a clip-local time (microseconds).
     * Returns the static value when no keyframes exist for the property.
     */
    public double evaluate(String property, long clipLocalTime, double fallback) {
        List<Keyframe> frames = keyframes.get(property);
        if (frames == null || frames.isEmpty()) {
            return fallback;
        }
        Keyframe prev = null;
        Keyframe next = null;
        for (Keyframe k : frames) {
            if (k.getTimeMicros() <= clipLocalTime) {
                prev = k;
            } else {
                next = k;
                break;
            }
        }
        if (prev == null) return frames.get(0).getValue();
        if (next == null || prev == next) return prev.getValue();
        double span = next.getTimeMicros() - prev.getTimeMicros();
        double t = span <= 0 ? 1.0 : (clipLocalTime - prev.getTimeMicros()) / (double) span;
        double eased = next.getInterpolation().ease(t);
        return prev.getValue() + (next.getValue() - prev.getValue()) * eased;
    }

    // ---------- computed duration ----------

    /** Clip duration on the timeline, in microseconds (honors speed/reverse). */
    public long duration() {
        // text clips store their duration in the source span; freeze frames
        // ignore speed and hold their source span as-is
        if (kind == Kind.TEXT || freezeFrame) {
            return Math.max(1, sourceEnd - sourceStart);
        }
        long sourceLen = Math.max(1, sourceEnd - sourceStart);
        return Math.max(1, Math.round(sourceLen / speed));
    }

    public long timelineEnd() {
        return timelineStart + duration();
    }

    // ---------- mutators used by edit operations ----------

    public void setTimelineStart(long t) { this.timelineStart = Math.max(0, t); }
    public void setSourceStart(long t) { this.sourceStart = Math.max(0, t); }
    public void setSourceEnd(long t) { this.sourceEnd = Math.max(t, sourceStart + 1); }

    /** Trim so that the on-timeline duration matches the given value. */
    public void setDuration(long micros) {
        long target = Math.max(1, micros);
        if (speed > 0) {
            sourceEnd = sourceStart + Math.round(target * speed);
        }
    }

    public void setSpeed(double speed) {
        this.speed = speed > 0 ? speed : 0.01;
    }

    public boolean isClipAt(long timelineTime) {
        return timelineTime >= timelineStart && timelineTime < timelineEnd();
    }

    public void addEffect(Effect e) {
        effects.add(e);
    }

    public boolean removeEffect(String effectId) {
        return effects.removeIf(e -> e.getId().equals(effectId));
    }

    public TimelineClip copy() {
        TimelineClip c = new TimelineClip(kind, name);
        c.sourcePath = sourcePath;
        c.mediaId = mediaId;
        c.trackId = trackId;
        c.color = color;
        c.label = label;
        c.timelineStart = timelineStart;
        c.sourceStart = sourceStart;
        c.sourceEnd = sourceEnd;
        c.speed = speed;
        c.reverse = reverse;
        c.freezeFrame = freezeFrame;
        c.fps = fps;
        c.positionX = positionX;
        c.positionY = positionY;
        c.scale = scale;
        c.scaleX = scaleX;
        c.scaleY = scaleY;
        c.rotation = rotation;
        c.opacity = opacity;
        c.volume = volume;
        c.muted = muted;
        c.hasAudio = hasAudio;
        c.audioDetached = audioDetached;
        c.enabled = enabled;
        c.locked = locked;
        c.hidden = hidden;
        c.favorite = favorite;
        c.shapeType = shapeType;
        c.transitionIn = transitionIn;
        c.transitionOut = transitionOut;
        c.transitionInDuration = transitionInDuration;
        c.transitionOutDuration = transitionOutDuration;
        for (Effect e : effects) {
            c.effects.add(e.copy());
        }
        for (Map.Entry<String, List<Keyframe>> entry : allKeyframes().entrySet()) {
            List<Keyframe> list = c.keyframes(entry.getKey());
            for (Keyframe k : entry.getValue()) {
                list.add(k.copy());
            }
        }
        if (text != null) {
            c.text = (TextEffect) text.copy();
        }
        return c;
    }

    // ---------- accessors ----------

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Kind getKind() { return kind; }
    public void setKind(Kind kind) { this.kind = kind; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
    public String getMediaId() { return mediaId; }
    public void setMediaId(String mediaId) { this.mediaId = mediaId; }
    public String getTrackId() { return trackId; }
    public void setTrackId(String trackId) { this.trackId = trackId; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public long getTimelineStart() { return timelineStart; }
    public long getSourceStart() { return sourceStart; }
    public long getSourceEnd() { return sourceEnd; }
    public double getSpeed() { return speed; }
    public boolean isReverse() { return reverse; }
    public void setReverse(boolean reverse) { this.reverse = reverse; }
    public boolean isFreezeFrame() { return freezeFrame; }
    public void setFreezeFrame(boolean freezeFrame) { this.freezeFrame = freezeFrame; }
    public double getFps() { return fps; }
    public void setFps(double fps) { this.fps = fps; }

    public double getPositionX() { return positionX; }
    public void setPositionX(double v) { this.positionX = v; }
    public double getPositionY() { return positionY; }
    public void setPositionY(double v) { this.positionY = v; }
    public double getScale() { return scale; }
    public void setScale(double v) { this.scale = v; }
    public double getScaleX() { return scaleX; }
    public void setScaleX(double v) { this.scaleX = v; }
    public double getScaleY() { return scaleY; }
    public void setScaleY(double v) { this.scaleY = v; }
    public double getRotation() { return rotation; }
    public void setRotation(double v) { this.rotation = v; }
    public double getOpacity() { return opacity; }
    public void setOpacity(double v) { this.opacity = v; }
    public double getVolume() { return volume; }
    public void setVolume(double v) { this.volume = v; }
    public boolean isMuted() { return muted; }
    public void setMuted(boolean muted) { this.muted = muted; }
    public boolean isHasAudio() { return hasAudio; }
    public void setHasAudio(boolean hasAudio) { this.hasAudio = hasAudio; }
    public boolean isAudioDetached() { return audioDetached; }
    public void setAudioDetached(boolean audioDetached) { this.audioDetached = audioDetached; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }

    public List<Effect> getEffects() { return effects; }
    public TextEffect getText() { return text; }
    public void setText(TextEffect text) { this.text = text; }
    public String getShapeType() { return shapeType; }
    public void setShapeType(String shapeType) { this.shapeType = shapeType; }

    public TransitionType getTransitionIn() { return transitionIn; }
    public void setTransitionIn(TransitionType t) { this.transitionIn = t; }
    public TransitionType getTransitionOut() { return transitionOut; }
    public void setTransitionOut(TransitionType t) { this.transitionOut = t; }
    public double getTransitionInDuration() { return transitionInDuration; }
    public void setTransitionInDuration(double d) { this.transitionInDuration = d; }
    public double getTransitionOutDuration() { return transitionOutDuration; }
    public void setTransitionOutDuration(double d) { this.transitionOutDuration = d; }

    // effect helpers
    public BlurEffect blurEffect() {
        return (BlurEffect) effects.stream().filter(e -> e instanceof BlurEffect).findFirst().orElse(null);
    }

    public ColorEffect colorEffect() {
        return (ColorEffect) effects.stream().filter(e -> e instanceof ColorEffect).findFirst().orElse(null);
    }

    public CropEffect cropEffect() {
        return (CropEffect) effects.stream().filter(e -> e instanceof CropEffect).findFirst().orElse(null);
    }

    public ChromaKeyEffect chromaKeyEffect() {
        return (ChromaKeyEffect) effects.stream().filter(e -> e instanceof ChromaKeyEffect).findFirst().orElse(null);
    }
}
