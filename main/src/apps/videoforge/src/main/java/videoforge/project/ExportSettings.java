package videoforge.project;

import org.json.JSONObject;

/**
 * Export configuration remembered with a project. The render engine consumes
 * these values; presets in the UI pre-fill them.
 */
public final class ExportSettings {

    public String container = "mp4";
    public String videoCodec = "h264";
    public String audioCodec = "aac";
    public String qualityMode = "crf";      // crf | bitrate
    public int crf = 20;
    public long bitrate = 8_000_000;        // bits per second when qualityMode=bitrate
    public double fps = 30.0;
    public int width = 1920;
    public int height = 1080;
    public String presetLabel = "YouTube 1080p";

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        o.put("container", container);
        o.put("videoCodec", videoCodec);
        o.put("audioCodec", audioCodec);
        o.put("qualityMode", qualityMode);
        o.put("crf", crf);
        o.put("bitrate", bitrate);
        o.put("fps", fps);
        o.put("width", width);
        o.put("height", height);
        o.put("presetLabel", presetLabel);
        return o;
    }

    public void load(JSONObject o) {
        container = o.optString("container", container);
        videoCodec = o.optString("videoCodec", videoCodec);
        audioCodec = o.optString("audioCodec", audioCodec);
        qualityMode = o.optString("qualityMode", qualityMode);
        crf = o.optInt("crf", crf);
        bitrate = o.optLong("bitrate", bitrate);
        fps = o.optDouble("fps", fps);
        width = o.optInt("width", width);
        height = o.optInt("height", height);
        presetLabel = o.optString("presetLabel", presetLabel);
    }

    public ExportSettings copy() {
        ExportSettings e = new ExportSettings();
        e.load(toJson());
        return e;
    }
}
