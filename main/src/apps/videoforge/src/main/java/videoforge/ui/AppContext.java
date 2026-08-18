package videoforge.ui;

import videoforge.config.AppConfig;
import videoforge.editing.TimelineOperations;
import videoforge.media.MediaLibrary;
import videoforge.project.ProjectManager;
import videoforge.project.VideoProject;
import videoforge.rendering.FFmpegManager;
import videoforge.rendering.PreviewEngine;
import videoforge.undo.UndoManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Application-wide state shared by all UI panels: the open project, media
 * library, undo manager, preview engine and the various service objects.
 */
public final class AppContext {

    private final AppConfig config = AppConfig.get();
    private final MediaLibrary library = new MediaLibrary();
    private final ProjectManager projectManager = new ProjectManager();
    private final UndoManager undo = new UndoManager();
    private final FFmpegManager ffmpeg = new FFmpegManager();
    private final PreviewEngine preview = new PreviewEngine();
    private final TimelineOperations operations;

    private VideoProject project;

    private final List<Consumer<String>> statusListeners = new ArrayList<>();
    private final List<Runnable> projectListeners = new ArrayList<>();

    public AppContext() {
        operations = new TimelineOperations(library);
        project = projectManager.createNew("Untitled Project");
    }

    public AppConfig config() { return config; }
    public MediaLibrary library() { return library; }
    public ProjectManager projects() { return projectManager; }
    public UndoManager undo() { return undo; }
    public FFmpegManager ffmpeg() { return ffmpeg; }
    public PreviewEngine preview() { return preview; }
    public TimelineOperations operations() { return operations; }
    public VideoProject project() { return project; }

    public void setProject(VideoProject project) {
        this.project = project;
        project.timeline().fire(videoforge.timeline.Timeline.ChangeType.STRUCTURE);
        for (Runnable r : new ArrayList<>(projectListeners)) {
            try {
                r.run();
            } catch (Exception ignored) {
            }
        }
    }

    public void onProjectChanged(Runnable r) {
        projectListeners.add(r);
    }

    public void onStatus(Consumer<String> c) {
        statusListeners.add(c);
    }

    public void status(String message) {
        for (Consumer<String> c : statusListeners) {
            try {
                c.accept(message);
            } catch (Exception ignored) {
            }
        }
    }

    /** Mark project dirty (any edit) and refresh title via listeners. */
    public void markDirty() {
        project.touch();
        for (Runnable r : projectListeners) {
            try {
                r.run();
            } catch (Exception ignored) {
            }
        }
    }
}
