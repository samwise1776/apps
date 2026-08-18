package videoforge.timeline;

import java.util.UUID;

/**
 * A named marker on the timeline. Markers can optionally generate YouTube
 * chapter timestamps (a chapter is a marker with {@code chapter=true}).
 */
public final class Marker {

    private String id = UUID.randomUUID().toString();
    private long timeMicros;
    private String name = "Marker";
    private String color = "#ffcc00";
    private String description = "";
    private boolean chapter;

    public Marker() {}

    public Marker(long timeMicros) {
        this.timeMicros = timeMicros;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTimeText() {
        return Timecode.of(timeMicros);
    }
    public long getTimeMicros() { return timeMicros; }
    public void setTimeMicros(long timeMicros) { this.timeMicros = Math.max(0, timeMicros); }
    public String getName() { return name; }
    public void setName(String name) { this.name = name == null || name.isBlank() ? "Marker" : name; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isChapter() { return chapter; }
    public void setChapter(boolean chapter) { this.chapter = chapter; }
}
