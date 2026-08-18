package videoforge.config;

import videoforge.logging.AppLog;
import videoforge.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Application-wide configuration and workspace paths.
 *
 * <p>The workspace layout mirrors the project structure:</p>
 * <pre>
 *   projects/   .vforge project files
 *   cache/      thumbnails, waveforms, preview frames, proxies
 *   autosave/   periodic recovery copies
 *   exports/    rendered output
 *   recordings/ raw screen/mic/webcam captures
 *   temp/       intermediate render files
 *   logs/       editor.log, ffmpeg.log, recording.log, youtube.log
 *   config/     app.json, external-tools.json, oauth.json
 * </pre>
 *
 * <p>Paths are stored as {@link Path} values resolved lazily against the workspace
 * base directory. The base may be redirected with {@code -Dvideoforge.base=...}.</p>
 */
public final class AppConfig {

    private static final AppLog LOG = AppLog.get("editor");

    private final Path baseDir;
    private final Map<String, Object> settings = new LinkedHashMap<>();

    private String ffmpegPath = "ffmpeg";
    private String ffprobePath = "ffprobe";

    private AppConfig(Path baseDir) {
        this.baseDir = baseDir.toAbsolutePath().normalize();
        defaults();
        load();
    }

    /** Lazily-initialized singleton. */
    private static final class Holder {
        static final AppConfig INSTANCE = new AppConfig(Path.of(System.getProperty("videoforge.base", ".")));
    }

    public static AppConfig get() {
        return Holder.INSTANCE;
    }

    private void defaults() {
        settings.put("version", "1.0.0");
        settings.put("theme", "dark");
        settings.put("accent", "#0af");
        settings.put("autosaveIntervalSeconds", 30);
        settings.put("autosaveEnabled", true);
        settings.put("previewQuality", "HALF");
        settings.put("projectFps", 30.0);
        settings.put("canvasWidth", 1920);
        settings.put("canvasHeight", 1080);
        settings.put("previewPlaybackFps", 30.0);
        settings.put("snapEnabled", true);
        settings.put("snapThresholdPixels", 10);
        settings.put("microphoneMonitoring", false);
        settings.put("recordingCountdown", 3);
        settings.put("recordingFolder", "recordings");
        settings.put("recentProjects", new ArrayList<String>());
    }

    // ---------- workspace paths ----------

    public Path baseDir() { return baseDir; }
    public Path projectsDir() { return ensure(baseDir.resolve("projects")); }
    public Path cacheDir() { return ensure(baseDir.resolve("cache")); }
    public Path autosaveDir() { return ensure(baseDir.resolve("autosave")); }
    public Path exportsDir() { return ensure(baseDir.resolve("exports")); }
    public Path recordingsDir() { return ensure(baseDir.resolve("recordings")); }
    public Path tempDir() { return ensure(baseDir.resolve("temp")); }
    public Path logsDir() { return ensure(baseDir.resolve("logs")); }
    public Path configDir() { return ensure(baseDir.resolve("config")); }
    public Path cacheDir(String sub) { return ensure(cacheDir().resolve(sub)); }

    public Path appConfigFile() { return configDir().resolve("app.json"); }
    public Path externalToolsFile() { return configDir().resolve("external-tools.json"); }
    public Path oauthConfigFile() { return configDir().resolve("oauth.json"); }

    private static Path ensure(Path p) {
        try {
            Files.createDirectories(p);
        } catch (IOException e) {
            LOG.warn("Could not create dir " + p + ": " + e.getMessage());
        }
        return p;
    }

    // ---------- settings access ----------

    public int getInt(String key) {
        Object v = settings.get(key);
        return v instanceof Number ? ((Number) v).intValue() : 0;
    }

    public double getDouble(String key) {
        Object v = settings.get(key);
        return v instanceof Number ? ((Number) v).doubleValue() : 0.0;
    }

    public boolean getBool(String key) {
        return Boolean.TRUE.equals(settings.get(key));
    }

    public String getString(String key) {
        Object v = settings.get(key);
        return v == null ? null : String.valueOf(v);
    }

    @SuppressWarnings("unchecked")
    public List<String> getStringList(String key) {
        Object v = settings.get(key);
        return v instanceof List ? (List<String>) v : new ArrayList<>();
    }

    public void put(String key, Object value) {
        settings.put(key, value);
        save();
    }

    // ---------- recent projects ----------

    public List<String> getRecentProjects() {
        return new ArrayList<>(getStringList("recentProjects"));
    }

    public void addRecentProject(Path projectFile) {
        List<String> list = getRecentProjects();
        list.remove(projectFile.toString());
        list.add(0, projectFile.toString());
        while (list.size() > 10) {
            list.remove(list.size() - 1);
        }
        settings.put("recentProjects", list);
        save();
    }

    // ---------- external tools ----------

    public synchronized String ffmpeg() { return ffmpegPath; }
    public synchronized String ffprobe() { return ffprobePath; }

    public synchronized void setFfmpegPath(String path) {
        this.ffmpegPath = path;
        saveExternalTools();
    }

    public synchronized void setFfprobePath(String path) {
        this.ffprobePath = path;
        saveExternalTools();
    }

    private void saveExternalTools() {
        Map<String, Object> tools = new LinkedHashMap<>();
        tools.put("ffmpeg", ffmpegPath);
        tools.put("ffprobe", ffprobePath);
        try {
            FileUtils.writeTextAtomic(externalToolsFile(), new org.json.JSONObject(tools).toString(2));
        } catch (IOException e) {
            LOG.warn("Could not persist external tool paths: " + e.getMessage());
        }
    }

    // ---------- persistence ----------

    public synchronized void save() {
        try {
            org.json.JSONObject json = new org.json.JSONObject();
            for (Map.Entry<String, Object> e : settings.entrySet()) {
                json.put(e.getKey(), e.getValue());
            }
            FileUtils.writeTextAtomic(appConfigFile(), json.toString(2));
        } catch (IOException e) {
            LOG.warn("Could not save app config: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void load() {
        String text = FileUtils.readText(appConfigFile());
        if (text == null) {
            save();
            return;
        }
        try {
            org.json.JSONObject json = new org.json.JSONObject(text);
            for (String key : json.keySet()) {
                settings.put(key, json.get(key));
            }
            // Nested lists come back as JSONArray; convert for our list accessors.
            for (String key : List.of("recentProjects")) {
                Object v = settings.get(key);
                if (v instanceof org.json.JSONArray arr) {
                    List<String> list = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        list.add(arr.getString(i));
                    }
                    settings.put(key, list);
                }
            }
        } catch (Exception e) {
            LOG.error("Could not parse app config, using defaults", e);
        }
        loadExternalTools();
    }

    private void loadExternalTools() {
        String text = FileUtils.readText(externalToolsFile());
        if (text == null) {
            return;
        }
        try {
            org.json.JSONObject json = new org.json.JSONObject(text);
            if (json.has("ffmpeg")) ffmpegPath = json.getString("ffmpeg");
            if (json.has("ffprobe")) ffprobePath = json.getString("ffprobe");
        } catch (Exception e) {
            LOG.warn("Could not parse external-tools.json: " + e.getMessage());
        }
    }

    /** Return true the first time the app runs (no config yet). */
    public boolean isFirstRun() {
        return !Files.exists(appConfigFile());
    }
}
