package videoforge.media;

import org.json.JSONArray;
import org.json.JSONObject;
import videoforge.config.AppConfig;
import videoforge.logging.AppLog;
import videoforge.rendering.FFmpegManager;
import videoforge.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The media library: every imported asset, persisted to
 * {@code config/media-library.json} so the user's collection survives restarts.
 */
public final class MediaLibrary {

    private static final AppLog LOG = AppLog.get("editor");

    private final List<MediaFile> files = new ArrayList<>();
    private final List<String> folders = new ArrayList<>();
    private final FFmpegManager ffmpeg = new FFmpegManager();
    private final ThumbnailGenerator thumbs = new ThumbnailGenerator();

    private String searchQuery = "";
    private String folderFilter = "All";
    private String sortBy = "date";     // date | name | kind | size
    private boolean sortDescending = true;
    private String kindFilter = "All";  // All | video | audio | image | subtitle

    public MediaLibrary() {
        load();
    }

    public ThumbnailGenerator thumbnails() { return thumbs; }

    // ---------- import ----------

    /**
     * Import files, probing metadata and generating thumbnails in the background.
     * Returns the created media entries for immediate UI feedback.
     */
    public List<MediaFile> importPaths(List<Path> paths, String folder, Consumer<MediaFile> onReady) {
        List<MediaFile> added = new ArrayList<>();
        for (Path p : paths) {
            if (!FileUtils.isSupportedMedia(p)) {
                continue;
            }
            MediaFile mf = new MediaFile(p);
            if (containsPath(p)) {
                continue;
            }
            mf.setFolder(folder == null ? "Uncategorized" : folder);
            files.add(mf);
            if (!folders.contains(mf.getFolder())) {
                folders.add(mf.getFolder());
            }
            added.add(mf);
            // Probe synchronously (fast) so list rows have duration/size right away.
            mf.metadata().setFps(30.0);
            var meta = ffmpeg.probe(p);
            mf.metadata().setFormat(meta.getFormat());
            mf.metadata().setWidth(meta.getWidth());
            mf.metadata().setHeight(meta.getHeight());
            mf.metadata().setDurationSeconds(meta.getDurationSeconds());
            mf.metadata().setFps(meta.getFps());
            mf.metadata().setVideoCodec(meta.getVideoCodec());
            mf.metadata().setAudioCodec(meta.getAudioCodec());
            mf.metadata().setBitrate(meta.getBitrate());
            mf.metadata().setHasVideo(meta.isHasVideo());
            mf.metadata().setHasAudio(meta.isHasAudio());
            if (onReady != null) {
                onReady.accept(mf);
            }
            thumbs.generateAsync(mf);
        }
        save();
        return added;
    }

    public void importPath(Path path) {
        importPaths(List.of(path), null, null);
    }

    private boolean containsPath(Path p) {
        String abs = p.toAbsolutePath().normalize().toString();
        return files.stream().anyMatch(f -> Path.of(f.getPath()).toAbsolutePath().normalize().toString().equals(abs));
    }

    public MediaFile addMedia(Path path) {
        MediaFile mf = new MediaFile(path);
        files.add(mf);
        return mf;
    }

    public void removeMedia(String mediaId) {
        files.removeIf(f -> f.getId().equals(mediaId));
        save();
    }

    public void removeMedia(MediaFile media) {
        files.remove(media);
        save();
    }

    public MediaFile byId(String id) {
        return files.stream().filter(f -> f.getId().equals(id)).findFirst().orElse(null);
    }

    public MediaFile byPath(Path path) {
        String abs = path.toAbsolutePath().normalize().toString();
        return files.stream()
                .filter(f -> Path.of(f.getPath()).toAbsolutePath().normalize().toString().equals(abs))
                .findFirst()
                .orElse(null);
    }

    /** Called when a clip's stored source path needs re-resolving. */
    public String resolvePath(String storedPath, String mediaId) {
        if (storedPath != null && Files.exists(Path.of(storedPath))) {
            return storedPath;
        }
        MediaFile mf = byId(mediaId);
        if (mf != null && mf.exists()) {
            return mf.getPath();
        }
        return storedPath;
    }

    // ---------- queries ----------

    public List<MediaFile> all() { return files; }

    public List<MediaFile> visible() {
        return files.stream()
                .filter(this::matchesFilters)
                .sorted(comparator())
                .collect(Collectors.toList());
    }

    private boolean matchesFilters(MediaFile f) {
        if (!"All".equals(kindFilter) && !f.getKind().equals(kindFilter)) {
            return false;
        }
        if (!"All".equals(folderFilter) && !f.getFolder().equals(folderFilter)) {
            return false;
        }
        if (searchQuery != null && !searchQuery.isBlank()
                && !f.getName().toLowerCase().contains(searchQuery.toLowerCase())) {
            return false;
        }
        return true;
    }

    private Comparator<MediaFile> comparator() {
        return switch (sortBy) {
            case "name" -> (a, b) -> sortDescending
                    ? b.getName().compareToIgnoreCase(a.getName())
                    : a.getName().compareToIgnoreCase(b.getName());
            case "kind" -> (a, b) -> sortDescending
                    ? b.getKind().compareTo(a.getKind())
                    : a.getKind().compareTo(b.getKind());
            case "size" -> (a, b) -> sortDescending
                    ? Long.compare(fileSize(b), fileSize(a))
                    : Long.compare(fileSize(a), fileSize(b));
            default -> (a, b) -> sortDescending
                    ? b.getId().compareTo(a.getId())
                    : a.getId().compareTo(b.getId());
        };
    }

    private static long fileSize(MediaFile f) {
        try {
            return Files.size(Path.of(f.getPath()));
        } catch (IOException | RuntimeException e) {
            return 0;
        }
    }

    public List<String> folders() { return folders; }

    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String q) { this.searchQuery = q; }
    public String getFolderFilter() { return folderFilter; }
    public void setFolderFilter(String f) { this.folderFilter = f; }
    public String getSortBy() { return sortBy; }
    public void setSortBy(String s) { this.sortBy = s; }
    public boolean isSortDescending() { return sortDescending; }
    public void setSortDescending(boolean d) { this.sortDescending = d; }
    public String getKindFilter() { return kindFilter; }
    public void setKindFilter(String k) { this.kindFilter = k; }

    // ---------- persistence ----------

    private Path libraryFile() {
        return AppConfig.get().configDir().resolve("media-library.json");
    }

    public synchronized void save() {
        JSONArray arr = new JSONArray();
        for (MediaFile f : files) {
            JSONObject o = new JSONObject();
            o.put("id", f.getId());
            o.put("path", f.getPath());
            o.put("kind", f.getKind());
            o.put("folder", f.getFolder());
            o.put("favorite", f.isFavorite());
            o.put("name", f.getName());
            JSONObject m = new JSONObject();
            m.put("width", f.metadata().getWidth());
            m.put("height", f.metadata().getHeight());
            m.put("duration", f.metadata().getDurationSeconds());
            m.put("fps", f.metadata().getFps());
            m.put("videoCodec", f.metadata().getVideoCodec());
            m.put("audioCodec", f.metadata().getAudioCodec());
            m.put("hasVideo", f.metadata().isHasVideo());
            m.put("hasAudio", f.metadata().isHasAudio());
            o.put("metadata", m);
            arr.put(o);
        }
        try {
            FileUtils.writeTextAtomic(libraryFile(), arr.toString(2));
        } catch (IOException e) {
            LOG.warn("Could not save media library: " + e.getMessage());
        }
    }

    private void load() {
        String text = FileUtils.readText(libraryFile());
        if (text == null) {
            return;
        }
        try {
            JSONArray arr = new JSONArray(text);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                MediaFile f = new MediaFile();
                f.setId(o.optString("id", f.getId()));
                f.setPath(o.optString("path", ""));
                f.setKind(o.optString("kind", "video"));
                f.setFolder(o.optString("folder", "Uncategorized"));
                f.setFavorite(o.optBoolean("favorite", false));
                f.setName(o.optString("name", Path.of(f.getPath()).getFileName().toString()));
                JSONObject m = o.optJSONObject("metadata");
                if (m != null) {
                    f.metadata().setWidth(m.optInt("width", 0));
                    f.metadata().setHeight(m.optInt("height", 0));
                    f.metadata().setDurationSeconds(m.optDouble("duration", 0));
                    f.metadata().setFps(m.optDouble("fps", 0));
                    f.metadata().setVideoCodec(m.optString("videoCodec", ""));
                    f.metadata().setAudioCodec(m.optString("audioCodec", ""));
                    f.metadata().setHasVideo(m.optBoolean("hasVideo", false));
                    f.metadata().setHasAudio(m.optBoolean("hasAudio", false));
                }
                files.add(f);
                if (!folders.contains(f.getFolder())) {
                    folders.add(f.getFolder());
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not parse media library: " + e.getMessage());
        }
    }
}
