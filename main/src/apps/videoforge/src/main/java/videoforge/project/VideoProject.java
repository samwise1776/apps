package videoforge.project;

import videoforge.effects.BackgroundEffect;
import videoforge.timeline.Timeline;

import java.nio.file.Path;
import java.time.Instant;

/**
 * An open editing project. Holds the timeline, project background, canvas/fps
 * and export settings. The media library is application-scoped and referenced
 * through clip media ids.
 */
public final class VideoProject {

    private String name = "Untitled Project";
    private Path filePath;               // where the .vforge file lives (null until saved)
    private final Instant createdAt = Instant.now();
    private Instant modifiedAt = Instant.now();

    private final Timeline timeline = new Timeline();
    private final BackgroundEffect background = new BackgroundEffect();
    private final ExportSettings exportSettings = new ExportSettings();
    private boolean dirty;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Path getFilePath() { return filePath; }
    public void setFilePath(Path filePath) { this.filePath = filePath; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getModifiedAt() { return modifiedAt; }
    public void touch() {
        modifiedAt = Instant.now();
        dirty = true;
    }

    public Timeline timeline() { return timeline; }
    public BackgroundEffect background() { return background; }
    public ExportSettings exportSettings() { return exportSettings; }

    public boolean isDirty() { return dirty; }
    public void setDirty(boolean dirty) { this.dirty = dirty; }

    /** Track list kept in sync with timeline structure so the UI can show counts. */
    public int videoTrackCount() { return timeline.tracksOf(videoforge.timeline.Track.Kind.VIDEO).size(); }
    public int audioTrackCount() { return timeline.tracksOf(videoforge.timeline.Track.Kind.AUDIO).size(); }
    public long durationMicros() { return timeline.duration(); }
}
