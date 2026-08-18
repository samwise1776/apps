package videoforge.project;

import videoforge.config.AppConfig;
import videoforge.logging.AppLog;
import videoforge.utils.FileUtils;
import videoforge.utils.TimeUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Lifecycle management for projects: create, open, save, autosave and crash
 * recovery. Autosaves land in {@code autosave/} and are offered for recovery
 * when the last saved project file is older than its most recent autosave.
 */
public final class ProjectManager {

    public static final String EXTENSION = ".vforge";
    private static final AppLog LOG = AppLog.get("editor");

    private final ProjectSerializer serializer = new ProjectSerializer();
    private final AppConfig config = AppConfig.get();

    public VideoProject createNew(String name) {
        VideoProject p = new VideoProject();
        p.setName(name == null || name.isBlank() ? "Untitled Project" : name);
        p.setDirty(true);
        return p;
    }

    public VideoProject open(Path file) throws IOException, ProjectSerializer.ProjectException {
        VideoProject p = serializer.load(file);
        config.addRecentProject(file);
        return p;
    }

    public void save(VideoProject project) {
        if (project.getFilePath() == null) {
            throw new IllegalStateException("Project has no file path; use saveAs");
        }
        try {
            serializer.save(project, project.getFilePath());
            config.addRecentProject(project.getFilePath());
        } catch (IOException e) {
            throw new ProjectSaveException("Could not save project: " + e.getMessage(), e);
        }
    }

    public void saveAs(VideoProject project, Path file) {
        try {
            serializer.save(project, file);
            config.addRecentProject(file);
        } catch (IOException e) {
            throw new ProjectSaveException("Could not save project: " + e.getMessage(), e);
        }
    }

    public VideoProject duplicate(VideoProject source, String newName) {
        VideoProject copy = serializerStringSafeCopy(source);
        copy.setName(newName);
        copy.setFilePath(null);
        copy.setDirty(true);
        return copy;
    }

    private VideoProject serializerStringSafeCopy(VideoProject source) {
        try {
            return serializer.fromJsonString(serializer.saveToString(source));
        } catch (ProjectSerializer.ProjectException e) {
            LOG.error("Could not duplicate project", e);
            VideoProject p = createNew(source.getName() + " copy");
            return p;
        }
    }

    public void rename(VideoProject project, String newName) {
        project.setName(newName);
        project.setDirty(true);
    }

    // ---------- autosave ----------

    public Path autosaveFile(VideoProject project) {
        String base = FileUtils.sanitizeFileName(project.getName());
        String stamp = Instant.now().toString().replace(":", "-").replace(".", "-");
        return config.autosaveDir().resolve(base + " " + stamp + EXTENSION);
    }

    public void autosave(VideoProject project) {
        if (!config.getBool("autosaveEnabled")) {
            return;
        }
        try {
            serializer.save(project, autosaveFile(project));
            pruneAutosaves(40);
        } catch (IOException e) {
            LOG.warn("Autosave failed: " + e.getMessage());
        }
    }

    private void pruneAutosaves(int keep) {
        try {
            List<Path> files;
            try (var stream = Files.list(config.autosaveDir())) {
                files = stream.filter(p -> p.toString().endsWith(EXTENSION))
                        .sorted(Comparator.comparingLong(p -> {
                            try {
                                return Files.getLastModifiedTime(p).toMillis();
                            } catch (IOException e) {
                                return 0;
                            }
                        }))
                        .collect(java.util.stream.Collectors.toList());
            }
            while (files.size() > keep) {
                Files.deleteIfExists(files.remove(0));
            }
        } catch (IOException ignored) {
        }
    }

    // ---------- crash recovery ----------

    public static final class RecoveryCandidate {
        public Path file;
        public Instant time;
        public String label;
    }

    /**
     * If a project was edited but its autosave is newer than the saved file,
     * an unsaved-change crash likely happened. Returns candidates, newest first.
     */
    public List<RecoveryCandidate> findRecoveryCandidates() {
        List<RecoveryCandidate> out = new ArrayList<>();
        try (var stream = Files.list(config.autosaveDir())) {
            List<Path> files = stream.filter(p -> p.toString().endsWith(EXTENSION)).collect(java.util.stream.Collectors.toList());
            for (Path f : files) {
                Instant mod = Files.getLastModifiedTime(f).toInstant();
                if (isLostAutosave(f, mod)) {
                    RecoveryCandidate c = new RecoveryCandidate();
                    c.file = f;
                    c.time = mod;
                    c.label = f.getFileName().toString().replace(EXTENSION, "")
                            + "  (" + TimeUtils.toHMS(TimeUtils.secondsToMicros(
                                    Math.max(0, Instant.now().getEpochSecond() - mod.getEpochSecond()))) + " ago)";
                    out.add(c);
                }
            }
        } catch (IOException ignored) {
        }
        out.sort(Comparator.comparing((videoforge.project.ProjectManager.RecoveryCandidate c) -> c.time).reversed());
        return out;
    }

    private boolean isLostAutosave(Path autosave, Instant autosaveTime) {
        // An autosave whose matching project file is older (or missing) is "lost".
        String name = autosave.getFileName().toString();
        String base = name.contains(" ") ? name.substring(0, name.indexOf(' ')) : name.replace(EXTENSION, "");
        Path projectFile = config.projectsDir().resolve(base + EXTENSION);
        if (!Files.exists(projectFile)) {
            return true;
        }
        try {
            return Files.getLastModifiedTime(projectFile).toInstant().isBefore(autosaveTime);
        } catch (IOException e) {
            return true;
        }
    }

    public VideoProject recover(RecoveryCandidate candidate) throws IOException, ProjectSerializer.ProjectException {
        VideoProject p = serializer.load(candidate.file);
        p.setFilePath(null);
        p.setName(Path.of(candidate.file.getFileName().toString().replace(EXTENSION, "")).getFileName().toString());
        p.setDirty(true);
        return p;
    }

    public List<Path> listProjectFiles() {
        List<Path> out = new ArrayList<>();
        try (var stream = Files.list(config.projectsDir())) {
            stream.filter(p -> p.toString().endsWith(EXTENSION)).forEach(out::add);
        } catch (IOException ignored) {
        }
        out.sort(Comparator.comparingLong((Path p) -> {
            try {
                return Files.getLastModifiedTime(p).toMillis();
            } catch (IOException e) {
                return 0;
            }
        }).reversed());
        return out;
    }

    public static final class ProjectSaveException extends RuntimeException {
        public ProjectSaveException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
