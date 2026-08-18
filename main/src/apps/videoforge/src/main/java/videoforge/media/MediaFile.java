package videoforge.media;

import videoforge.utils.FileUtils;

import java.nio.file.Path;
import java.util.UUID;

/**
 * An imported media asset referenced by projects. Clips reference media by id;
 * the library also keeps the resolved path so missing files can be relinked.
 */
public final class MediaFile {

    private String id = UUID.randomUUID().toString();
    private String path;
    private String kind = "video";          // video | audio | image | subtitle | other
    private String folder = "Uncategorized";
    private boolean favorite;
    private String name;
    private transient String thumbnailPath;
    private final MediaMetadata metadata = new MediaMetadata();

    public MediaFile() {}

    public MediaFile(Path path) {
        setPath(path.toString());
        this.name = path.getFileName() != null ? path.getFileName().toString() : path.toString();
        this.kind = FileUtils.kindOf(path);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getFolder() { return folder; }
    public void setFolder(String folder) { this.folder = folder; }
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getThumbnailPath() { return thumbnailPath; }
    public void setThumbnailPath(String thumbnailPath) { this.thumbnailPath = thumbnailPath; }
    public MediaMetadata metadata() { return metadata; }

    public boolean exists() {
        return path != null && java.nio.file.Files.exists(Path.of(path));
    }

    /** Human-readable library listing line. */
    public String detailText() {
        StringBuilder sb = new StringBuilder(name);
        if (!metadata.resolutionText().isEmpty()) {
            sb.append("  ").append(metadata.resolutionText());
        }
        if (!metadata.durationText().isEmpty()) {
            sb.append("  ").append(metadata.durationText());
        }
        return sb.toString();
    }
}
