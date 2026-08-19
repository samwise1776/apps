package videoforge.rendering;

import videoforge.project.ExportSettings;

/**
 * Predefined export presets (YouTube variants, Shorts, quality-focused and
 * size-focused). The Export window applies these onto an {@link ExportSettings}.
 */
public final class ExportPreset {

    public final String label;
    public final String container;
    public final String videoCodec;
    public final int width;
    public final int height;
    public final double fps;
    public final int crf;
    public final String audioCodec;

    private ExportPreset(String label, String container, String videoCodec,
                         int width, int height, double fps, int crf, String audioCodec) {
        this.label = label;
        this.container = container;
        this.videoCodec = videoCodec;
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.crf = crf;
        this.audioCodec = audioCodec;
    }

    public ExportSettings toSettings() {
        ExportSettings s = new ExportSettings();
        s.container = container;
        s.videoCodec = videoCodec;
        s.width = width;
        s.height = height;
        s.fps = fps;
        s.crf = crf;
        s.audioCodec = audioCodec;
        s.presetLabel = label;
        s.qualityMode = "crf";
        return s;
    }

    public static ExportPreset[] all() {
        return new ExportPreset[]{
                new ExportPreset("YouTube 1080p", "mp4", "h264", 1920, 1080, 30, 20, "aac"),
                new ExportPreset("YouTube 1440p", "mp4", "h264", 2560, 1440, 30, 20, "aac"),
                new ExportPreset("YouTube 4K", "mp4", "h264", 3840, 2160, 30, 20, "aac"),
                new ExportPreset("YouTube Shorts", "mp4", "h264", 1080, 1920, 30, 22, "aac"),
                new ExportPreset("High Quality", "mov", "h264", 1920, 1080, 30, 14, "pcm_s16le"),
                new ExportPreset("Small File", "mp4", "h264", 1280, 720, 30, 30, "aac"),
                new ExportPreset("Web (VP9)", "webm", "vp9", 1920, 1080, 30, 32, "libopus"),
                new ExportPreset("Custom", "mp4", "h264", 1920, 1080, 30, 20, "aac"),
        };
    }
}
