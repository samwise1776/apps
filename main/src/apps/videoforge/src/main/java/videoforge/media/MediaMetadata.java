package videoforge.media;

/**
 * Metadata for a media file, populated from FFprobe where available.
 * All values are nullable-friendly: a failed probe leaves zeros/flags false.
 */
public final class MediaMetadata {

    private String format = "";
    private int width;
    private int height;
    private double durationSeconds;
    private double fps;
    private String videoCodec = "";
    private String audioCodec = "";
    private long bitrate;             // bits per second
    private boolean hasVideo;
    private boolean hasAudio;

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public double getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(double durationSeconds) { this.durationSeconds = durationSeconds; }

    public double getFps() { return fps; }
    public void setFps(double fps) { this.fps = fps; }

    public String getVideoCodec() { return videoCodec; }
    public void setVideoCodec(String videoCodec) { this.videoCodec = videoCodec; }

    public String getAudioCodec() { return audioCodec; }
    public void setAudioCodec(String audioCodec) { this.audioCodec = audioCodec; }

    public long getBitrate() { return bitrate; }
    public void setBitrate(long bitrate) { this.bitrate = bitrate; }

    public boolean isHasVideo() { return hasVideo; }
    public void setHasVideo(boolean hasVideo) { this.hasVideo = hasVideo; }

    public boolean isHasAudio() { return hasAudio; }
    public void setHasAudio(boolean hasAudio) { this.hasAudio = hasAudio; }

    public String resolutionText() {
        if (width <= 0 || height <= 0) {
            return "";
        }
        return width + "x" + height;
    }

    public String durationText() {
        if (durationSeconds <= 0) {
            return "";
        }
        long total = Math.round(durationSeconds);
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%d:%02d", m, s);
    }
}
