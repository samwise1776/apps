package videoforge.project;

import org.json.JSONArray;
import org.json.JSONObject;
import videoforge.effects.BackgroundEffect;
import videoforge.effects.Effect;
import videoforge.effects.TextEffect;
import videoforge.logging.AppLog;
import videoforge.timeline.Interpolation;
import videoforge.timeline.Keyframe;
import videoforge.timeline.Marker;
import videoforge.timeline.Timeline;
import videoforge.timeline.TimelineClip;
import videoforge.timeline.Track;
import videoforge.timeline.TransitionType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Serializes a {@link VideoProject} to the custom {@code .vforge} format
 * (JSON under the hood). Media files are referenced by path; the huge video
 * bytes themselves are never stored inside the project file.
 */
public final class ProjectSerializer {

    public static final int FORMAT_VERSION = 1;
    private static final AppLog LOG = AppLog.get("editor");

    public String toJsonString(VideoProject project) {
        JSONObject root = new JSONObject();
        root.put("format", "vforge");
        root.put("version", FORMAT_VERSION);
        root.put("name", project.getName());
        root.put("fps", project.timeline().fps());
        root.put("canvasWidth", project.timeline().canvasWidth());
        root.put("canvasHeight", project.timeline().canvasHeight());

        root.put("background", project.background().toJson());
        root.put("export", project.exportSettings().toJson());

        JSONArray tracks = new JSONArray();
        for (Track t : project.timeline().tracks()) {
            tracks.put(trackToJson(t, project.timeline()));
        }
        root.put("tracks", tracks);

        JSONArray markers = new JSONArray();
        for (Marker m : project.timeline().markers()) {
            markers.put(markerToJson(m));
        }
        root.put("markers", markers);

        root.put("inPoint", project.timeline().inPoint());
        root.put("outPoint", project.timeline().outPoint());
        root.put("playhead", project.timeline().playhead());
        return root.toString(2);
    }

    public void save(VideoProject project, Path file) throws IOException {
        videoforge.utils.FileUtils.writeTextAtomic(file, toJsonString(project));
        project.setFilePath(file);
        project.setDirty(false);
    }

    public String saveToString(VideoProject project) {
        return toJsonString(project);
    }

    public VideoProject load(Path file) throws IOException, ProjectException {
        String text = videoforge.utils.FileUtils.readText(file);
        if (text == null) {
            throw new ProjectException("File could not be read: " + file);
        }
        VideoProject project = fromJsonString(text);
        project.setFilePath(file);
        return project;
    }

    public VideoProject fromJsonString(String text) throws ProjectException {
        try {
            JSONObject root = new JSONObject(text);
            VideoProject project = new VideoProject();
            project.setName(root.optString("name", "Untitled Project"));
            Timeline tl = project.timeline();
            tl.setFps(root.optDouble("fps", 30.0));
            int w = root.optInt("canvasWidth", 1920);
            int h = root.optInt("canvasHeight", 1080);
            tl.setCanvasSize(w, h);

            project.background().load(root.optJSONObject("background") != null
                    ? root.getJSONObject("background") : new BackgroundEffect().toJson());

            if (root.has("export")) {
                project.exportSettings().load(root.getJSONObject("export"));
            }

            JSONArray tracksArr = root.optJSONArray("tracks");
            if (tracksArr != null) {
                tl.tracks().clear();
                for (int i = 0; i < tracksArr.length(); i++) {
                    trackFromJson(tracksArr.getJSONObject(i), tl);
                }
                for (int i = 0; i < tl.tracks().size(); i++) {
                    tl.tracks().get(i).setZOrder(i);
                }
            }

            JSONArray markersArr = root.optJSONArray("markers");
            if (markersArr != null) {
                for (int i = 0; i < markersArr.length(); i++) {
                    tl.addMarker(markerFromJson(markersArr.getJSONObject(i)));
                }
            }

            if (root.has("inPoint")) tl.setInPoint(root.getLong("inPoint"));
            if (root.has("outPoint")) tl.setOutPoint(root.getLong("outPoint"));
            if (root.has("playhead")) tl.setPlayhead(root.getLong("playhead"));
            project.setDirty(false);
            return project;
        } catch (Exception e) {
            LOG.error("Failed to parse project JSON", e);
            throw new ProjectException("Project file is corrupted or not a VideoForge project: " + e.getMessage(), e);
        }
    }

    // ---------- tracks ----------

    private JSONObject trackToJson(Track t, Timeline tl) {
        JSONObject o = new JSONObject();
        o.put("id", t.getId());
        o.put("kind", t.getKind().name());
        o.put("name", t.getName());
        o.put("zOrder", t.getZOrder());
        o.put("muted", t.isMuted());
        o.put("soloed", t.isSoloed());
        o.put("locked", t.isLocked());
        o.put("hidden", t.isHidden());
        JSONArray clips = new JSONArray();
        for (TimelineClip c : t.clips()) {
            clips.put(clipToJson(c));
        }
        o.put("clips", clips);
        return o;
    }

    private void trackFromJson(JSONObject o, Timeline tl) {
        Track t = new Track(Track.Kind.valueOf(o.optString("kind", "VIDEO")), o.optString("name", "Track"));
        t.setId(o.getString("id"));
        t.setZOrder(o.optInt("zOrder", 0));
        t.setMuted(o.optBoolean("muted", false));
        t.setSoloed(o.optBoolean("soloed", false));
        t.setLocked(o.optBoolean("locked", false));
        t.setHidden(o.optBoolean("hidden", false));
        JSONArray clips = o.optJSONArray("clips");
        if (clips != null) {
            for (int i = 0; i < clips.length(); i++) {
                TimelineClip c = clipFromJson(clips.getJSONObject(i));
                t.addClip(c);
            }
        }
        tl.tracks().add(t);
    }

    // ---------- clips ----------

    private JSONObject clipToJson(TimelineClip c) {
        JSONObject o = new JSONObject();
        o.put("id", c.getId());
        o.put("kind", c.getKind().name());
        o.put("name", c.getName());
        o.put("sourcePath", c.getSourcePath() == null ? "" : c.getSourcePath());
        o.put("mediaId", c.getMediaId() == null ? "" : c.getMediaId());
        o.put("trackId", c.getTrackId());
        o.put("color", c.getColor());
        o.put("label", c.getLabel());

        o.put("timelineStart", c.getTimelineStart());
        o.put("sourceStart", c.getSourceStart());
        o.put("sourceEnd", c.getSourceEnd());
        o.put("speed", c.getSpeed());
        o.put("reverse", c.isReverse());
        o.put("freezeFrame", c.isFreezeFrame());
        o.put("fps", c.getFps());

        o.put("positionX", c.getPositionX());
        o.put("positionY", c.getPositionY());
        o.put("scale", c.getScale());
        o.put("scaleX", c.getScaleX());
        o.put("scaleY", c.getScaleY());
        o.put("rotation", c.getRotation());
        o.put("opacity", c.getOpacity());

        o.put("volume", c.getVolume());
        o.put("muted", c.isMuted());
        o.put("hasAudio", c.isHasAudio());
        o.put("audioDetached", c.isAudioDetached());

        o.put("enabled", c.isEnabled());
        o.put("locked", c.isLocked());
        o.put("hidden", c.isHidden());
        o.put("favorite", c.isFavorite());

        JSONArray effects = new JSONArray();
        for (Effect e : c.getEffects()) {
            effects.put(e.toJson());
        }
        o.put("effects", effects);

        JSONObject kf = new JSONObject();
        for (Map.Entry<String, List<Keyframe>> entry : c.allKeyframes().entrySet()) {
            JSONArray arr = new JSONArray();
            for (Keyframe k : entry.getValue()) {
                JSONObject ko = new JSONObject();
                ko.put("t", k.getTimeMicros());
                ko.put("v", k.getValue());
                ko.put("i", k.getInterpolation().name());
                arr.put(ko);
            }
            kf.put(entry.getKey(), arr);
        }
        o.put("keyframes", kf);

        if (c.getText() != null) {
            o.put("text", c.getText().toJson());
        }
        o.put("shapeType", c.getShapeType());

        o.put("transitionIn", c.getTransitionIn().name());
        o.put("transitionOut", c.getTransitionOut().name());
        o.put("transitionInDuration", c.getTransitionInDuration());
        o.put("transitionOutDuration", c.getTransitionOutDuration());
        return o;
    }

    private TimelineClip clipFromJson(JSONObject o) {
        TimelineClip c = new TimelineClip(
                TimelineClip.Kind.valueOf(o.optString("kind", "VIDEO")),
                o.optString("name", "Clip"));
        c.setId(o.getString("id"));
        c.setSourcePath(o.optString("sourcePath", ""));
        c.setMediaId(o.optString("mediaId", ""));
        c.setTrackId(o.optString("trackId", ""));
        c.setColor(o.optString("color", c.getColor()));
        c.setLabel(o.optString("label", ""));

        c.setTimelineStart(o.optLong("timelineStart", 0));
        c.setSourceStart(o.optLong("sourceStart", 0));
        c.setSourceEnd(o.optLong("sourceEnd", 0));
        c.setSpeed(o.optDouble("speed", 1.0));
        c.setReverse(o.optBoolean("reverse", false));
        c.setFreezeFrame(o.optBoolean("freezeFrame", false));
        c.setFps(o.optDouble("fps", 30.0));

        c.setPositionX(o.optDouble("positionX", 960));
        c.setPositionY(o.optDouble("positionY", 540));
        c.setScale(o.optDouble("scale", 1.0));
        c.setScaleX(o.optDouble("scaleX", 1.0));
        c.setScaleY(o.optDouble("scaleY", 1.0));
        c.setRotation(o.optDouble("rotation", 0));
        c.setOpacity(o.optDouble("opacity", 1.0));

        c.setVolume(o.optDouble("volume", 1.0));
        c.setMuted(o.optBoolean("muted", false));
        c.setHasAudio(o.optBoolean("hasAudio", false));
        c.setAudioDetached(o.optBoolean("audioDetached", false));

        c.setEnabled(o.optBoolean("enabled", true));
        c.setLocked(o.optBoolean("locked", false));
        c.setHidden(o.optBoolean("hidden", false));
        c.setFavorite(o.optBoolean("favorite", false));

        JSONArray effects = o.optJSONArray("effects");
        if (effects != null) {
            for (int i = 0; i < effects.length(); i++) {
                Effect e = Effect.fromJson(effects.getJSONObject(i));
                if (e != null) {
                    c.addEffect(e);
                }
            }
        }

        JSONObject kf = o.optJSONObject("keyframes");
        if (kf != null) {
            for (String prop : kf.keySet()) {
                JSONArray arr = kf.getJSONArray(prop);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject ko = arr.getJSONObject(i);
                    Keyframe k = new Keyframe(
                            ko.optLong("t", 0),
                            ko.optDouble("v", 0),
                            Interpolation.from(ko.optString("i", "LINEAR")));
                    c.keyframes(prop).add(k);
                }
            }
        }

        if (o.has("text")) {
            TextEffect te = new TextEffect();
            te.loadJson(o.getJSONObject("text"));
            te.setId(o.getJSONObject("text").optString("id", te.getId()));
            c.setText(te);
        }
        c.setShapeType(o.optString("shapeType", "rectangle"));

        c.setTransitionIn(TransitionType.from(o.optString("transitionIn", "NONE")));
        c.setTransitionOut(TransitionType.from(o.optString("transitionOut", "NONE")));
        c.setTransitionInDuration(o.optDouble("transitionInDuration", 0.5));
        c.setTransitionOutDuration(o.optDouble("transitionOutDuration", 0.5));
        return c;
    }

    // ---------- markers ----------

    private JSONObject markerToJson(Marker m) {
        JSONObject o = new JSONObject();
        o.put("id", m.getId());
        o.put("time", m.getTimeMicros());
        o.put("name", m.getName());
        o.put("color", m.getColor());
        o.put("description", m.getDescription());
        o.put("chapter", m.isChapter());
        return o;
    }

    private Marker markerFromJson(JSONObject o) {
        Marker m = new Marker(o.optLong("time", 0));
        m.setId(o.getString("id"));
        m.setName(o.optString("name", "Marker"));
        m.setColor(o.optString("color", "#ffcc00"));
        m.setDescription(o.optString("description", ""));
        m.setChapter(o.optBoolean("chapter", false));
        return m;
    }

    /**
     * Serialize a single clip to JSON. Used by the undo system to snapshot
     * clip state before and after property edits.
     */
    public static String clipToJsonString(TimelineClip clip) {
        return new ProjectSerializer().clipToJson(clip).toString();
    }

    public static TimelineClip clipFromJsonString(String json) {
        return new ProjectSerializer().clipFromJson(new JSONObject(json));
    }

    /** Serialize a full timeline (tracks, clips, markers, playhead) to JSON. */
    public static String timelineToJsonString(Timeline tl) {
        ProjectSerializer s = new ProjectSerializer();
        JSONObject root = new JSONObject();
        root.put("fps", tl.fps());
        root.put("canvasWidth", tl.canvasWidth());
        root.put("canvasHeight", tl.canvasHeight());
        JSONArray tracks = new JSONArray();
        for (Track t : tl.tracks()) {
            tracks.put(s.trackToJson(t, tl));
        }
        root.put("tracks", tracks);
        JSONArray markers = new JSONArray();
        for (Marker m : tl.markers()) {
            markers.put(s.markerToJson(m));
        }
        root.put("markers", markers);
        root.put("inPoint", tl.inPoint());
        root.put("outPoint", tl.outPoint());
        root.put("playhead", tl.playhead());
        return root.toString();
    }

    /** Restore timeline contents from a JSON snapshot produced above. */
    public static void timelineFromJsonString(String json, Timeline tl) throws ProjectException {
        ProjectSerializer s = new ProjectSerializer();
        try {
            JSONObject root = new JSONObject(json);
            tl.setFps(root.optDouble("fps", 30.0));
            tl.setCanvasSize(root.optInt("canvasWidth", 1920), root.optInt("canvasHeight", 1080));
            tl.tracks().clear();
            JSONArray tracksArr = root.optJSONArray("tracks");
            if (tracksArr != null) {
                for (int i = 0; i < tracksArr.length(); i++) {
                    s.trackFromJson(tracksArr.getJSONObject(i), tl);
                }
                for (int i = 0; i < tl.tracks().size(); i++) {
                    tl.tracks().get(i).setZOrder(i);
                }
            }
            tl.markers().clear();
            JSONArray markersArr = root.optJSONArray("markers");
            if (markersArr != null) {
                for (int i = 0; i < markersArr.length(); i++) {
                    tl.addMarker(s.markerFromJson(markersArr.getJSONObject(i)));
                }
            }
            if (root.has("inPoint")) tl.setInPoint(root.getLong("inPoint"));
            if (root.has("outPoint")) tl.setOutPoint(root.getLong("outPoint"));
            if (root.has("playhead")) tl.setPlayhead(root.getLong("playhead"));
        } catch (Exception e) {
            throw new ProjectException("Could not restore timeline snapshot", e);
        }
    }

    /** Thrown for unreadable/corrupt project files. */
    public static final class ProjectException extends Exception {
        public ProjectException(String message) {
            super(message);
        }

        public ProjectException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
