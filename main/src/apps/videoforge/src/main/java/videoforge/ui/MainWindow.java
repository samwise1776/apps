package videoforge.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import videoforge.config.AppConfig;
import videoforge.logging.AppLog;
import videoforge.project.ProjectManager;
import videoforge.project.VideoProject;
import videoforge.timeline.Timeline;
import videoforge.timeline.TimelineClip;
import videoforge.timeline.Track;
import videoforge.undo.UndoManager;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main editor window: menu bar, quick toolbar, media library, preview with
 * transport controls, inspector, multi-track timeline and a status bar. Wires
 * file operations, editing, autosave, export, recording and the auxiliary
 * dialogs together and handles global keyboard shortcuts.
 */
public final class MainWindow extends BorderPane {

    private static final AppLog LOG = AppLog.get("editor");

    private final AppContext ctx;
    private final MediaPanel mediaPanel;
    private final PreviewPanel previewPanel;
    private final TimelineView timelineView;
    private final InspectorPanel inspectorPanel;
    private final Toolbar toolbar = new Toolbar();
    private final Label statusLabel = new Label("Ready");
    private final Label dirtyLabel = new Label();
    private final Label undoLabel = new Label();

    private final ScheduledExecutorService autosave = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "autosave");
        t.setDaemon(true);
        return t;
    });

    private final MenuBar menuBar = new MenuBar();
    private boolean closing;

    public MainWindow(AppContext ctx) {
        this.ctx = ctx;
        mediaPanel = new MediaPanel(ctx);
        previewPanel = new PreviewPanel(ctx);
        timelineView = new TimelineView(ctx);
        inspectorPanel = new InspectorPanel(ctx);

        buildMenus();
        wireToolbar();
        wireStatus();

        SplitPane center = new SplitPane();
        SplitPane right = new SplitPane();
        right.setOrientation(javafx.geometry.Orientation.VERTICAL);
        right.getItems().addAll(previewPanel, timelineView);
        right.setDividerPositions(0.55);
        ScrollPane inspectorScroll = new ScrollPane(inspectorPanel);
        inspectorScroll.setFitToWidth(true);
        inspectorScroll.setFitToHeight(true);
        center.getItems().addAll(mediaPanel, right, inspectorScroll);
        center.setDividerPositions(0.20, 0.78);

        HBox statusBar = new HBox(12, statusLabel, dirtyLabel, undoLabel);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.getStyleClass().add("statusbar");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        statusBar.getChildren().add(spacer);
        Label hint = new Label("Space Play  |  S Split  |  Ctrl+Shift+R Export  |  F9 Record");
        hint.getStyleClass().add("muted");
        statusBar.getChildren().add(hint);

        VBox top = new VBox(menuBar, toolbar);
        setTop(top);
        setCenter(center);
        setBottom(statusBar);

        installKeyboardShortcuts();
        startAutosave();
        ctx.onProjectChanged(this::setTitleFromProject);
        setTitleFromProject();
        updateUndoStatus();
    }

    // ======================================================================
    //  Title / status
    // ======================================================================

    public String title() {
        VideoProject p = ctx.project();
        String name = p.getName();
        return name + (p.isDirty() ? " *" : "") + " - VideoForge Studio";
    }

    public void setTitleFromProject() {
        dirtyLabel.setText(ctx.project().isDirty() ? "\u2022 unsaved" : "");
        javafx.stage.Window w = getScene() == null ? null : getScene().getWindow();
        if (w instanceof javafx.stage.Stage stage) {
            stage.setTitle(title());
        }
    }

    private void wireStatus() {
        ctx.onStatus(msg -> Platform.runLater(() -> statusLabel.setText(msg)));
    }

    private void updateUndoStatus() {
        undoLabel.setText(ctx.undo().canUndo() || ctx.undo().canRedo()
                ? "Undo: " + ctx.undo().undoLabel() + "  Redo: " + ctx.undo().redoLabel() : "");
        undoLabel.getStyleClass().add("muted");
    }

    // ======================================================================
    //  Toolbar
    // ======================================================================

    private void wireToolbar() {
        toolbar.newProject.setOnAction(e -> newProject());
        toolbar.open.setOnAction(e -> openProject());
        toolbar.save.setOnAction(e -> saveProject());
        toolbar.importMedia.setOnAction(e -> mediaPanel.importDialog());
        toolbar.addText.setOnAction(e -> addTextClip());
        toolbar.split.setOnAction(e -> timelineView.splitAtPlayhead());
        toolbar.undo.setOnAction(e -> undo());
        toolbar.redo.setOnAction(e -> redo());
        toolbar.record.setOnAction(e -> RecorderWindow.show(ctx));
        toolbar.export.setOnAction(e -> ExportWindow.show(ctx));
        toolbar.youtube.setOnAction(e -> YouTubeWindow.show(ctx));
    }

    // ======================================================================
    //  Menus
    // ======================================================================

    private void buildMenus() {
        menuBar.getMenus().addAll(
                fileMenu(), editMenu(), clipMenu(), timelineMenu(),
                audioMenu(), effectsMenu(), recordingMenu(), exportMenu(),
                youtubeMenu(), settingsMenu(), helpMenu());
    }

    private Menu fileMenu() {
        Menu m = new Menu("File");
        MenuItem mi;

        mi = item("New Project", "Ctrl+N", () -> newProject());
        m.getItems().add(mi);

        mi = item("Open Project...", "Ctrl+O", () -> openProject());
        m.getItems().add(mi);

        Menu recent = new Menu("Open Recent");
        refreshRecentMenu(recent);
        m.getItems().add(recent);

        m.getItems().add(new SeparatorMenuItem());

        mi = item("Save", "Ctrl+S", () -> saveProject());
        m.getItems().add(mi);

        mi = item("Save As...", "Ctrl+Shift+S", () -> saveProjectAs());
        m.getItems().add(mi);

        mi = item("Save a Copy...", null, () -> saveCopy());
        m.getItems().add(mi);

        m.getItems().add(new SeparatorMenuItem());

        mi = item("Import Media...", "Ctrl+I", () -> mediaPanel.importDialog());
        m.getItems().add(mi);

        mi = item("Save Current Frame...", null, () -> saveCurrentFrame());
        m.getItems().add(mi);

        m.getItems().add(new SeparatorMenuItem());

        mi = item("Exit", null, () -> shutdown());
        m.getItems().add(mi);
        return m;
    }

    private Menu editMenu() {
        Menu m = new Menu("Edit");
        MenuItem mi = item("Undo", "Ctrl+Z", () -> undo());
        m.getItems().add(mi);
        mi = item("Redo", "Ctrl+Y", () -> redo());
        m.getItems().add(mi);
        m.getItems().add(new SeparatorMenuItem());
        m.getItems().add(item("Cut", "Ctrl+X", () -> {
            timelineView.copySelected();
            timelineView.deleteSelected(false);
        }));
        m.getItems().add(item("Copy", "Ctrl+C", () -> timelineView.copySelected()));
        m.getItems().add(item("Paste", "Ctrl+V", () -> timelineView.pasteClips()));
        m.getItems().add(item("Duplicate", "Ctrl+D", () -> timelineView.duplicateSelected()));
        m.getItems().add(item("Select All", "Ctrl+A", this::selectAll));
        m.getItems().add(new SeparatorMenuItem());
        m.getItems().add(item("Set In Point", "I", () -> setInPoint()));
        m.getItems().add(item("Set Out Point", "O", () -> setOutPoint()));
        m.getItems().add(item("Clear In/Out", null, () -> ctx.project().timeline().clearRange()));
        m.getItems().add(new SeparatorMenuItem());
        m.getItems().add(item("Markers...", "M", () -> MarkersWindow.show(ctx)));
        m.getItems().add(item("History...", null, () -> HistoryWindow.show(ctx)));
        return m;
    }

    private Menu clipMenu() {
        Menu m = new Menu("Clip");
        m.getItems().add(item("Split at Playhead", "S", () -> timelineView.splitAtPlayhead()));
        m.getItems().add(item("Join Selected", "J", () -> timelineView.joinSelected()));
        m.getItems().add(new SeparatorMenuItem());
        m.getItems().add(item("Restore Clip (Uncut)", null, () -> timelineView.restoreSelected()));
        m.getItems().add(item("Reset Speed", null, () -> timelineView.resetSpeedSelected()));
        m.getItems().add(item("Detach Audio", "Ctrl+Shift+D", () -> timelineView.detachAudio()));
        m.getItems().add(new SeparatorMenuItem());
        m.getItems().add(item("Toggle Mute", "Shift+M", () -> timelineView.toggleMute()));
        m.getItems().add(item("Toggle Lock", "Shift+L", () -> timelineView.toggleLock()));
        m.getItems().add(item("Toggle Hidden", "Shift+H", () -> timelineView.toggleHidden()));
        m.getItems().add(new SeparatorMenuItem());
        m.getItems().add(item("Transition In (next)", "T", () -> timelineView.cycleTransitionIn()));
        m.getItems().add(item("Transition Out (next)", "Shift+T", () -> timelineView.cycleTransitionOut()));
        return m;
    }

    private Menu timelineMenu() {
        Menu m = new Menu("Timeline");
        m.getItems().add(item("Add Video Track", null, () -> addTrack(Track.Kind.VIDEO)));
        m.getItems().add(item("Add Audio Track", null, () -> addTrack(Track.Kind.AUDIO)));
        m.getItems().add(item("Add Text Track", null, () -> addTrack(Track.Kind.TEXT)));
        m.getItems().add(item("Add Image Track", null, () -> addTrack(Track.Kind.IMAGE)));
        m.getItems().add(new SeparatorMenuItem());
        m.getItems().add(item("Add Text Clip", "Ctrl+T", () -> addTextClip()));
        m.getItems().add(item("Add Marker at Playhead", null, () -> addMarkerAtPlayhead()));
        m.getItems().add(new SeparatorMenuItem());
        m.getItems().add(item("Zoom In", "=", () -> timelineView.setZoom(timelineView.zoom() * 1.25)));
        m.getItems().add(item("Zoom Out", "-", () -> timelineView.setZoom(timelineView.zoom() / 1.25)));
        m.getItems().add(item("Scroll to Playhead", "Ctrl+Home", () -> timelineView.scrollToPlayhead()));
        return m;
    }

    private Menu audioMenu() {
        Menu m = new Menu("Audio");
        m.getItems().add(item("Toggle Mute Selected", "Shift+M", () -> timelineView.toggleMute()));
        m.getItems().add(item("Detach Audio from Video", "Ctrl+Shift+D", () -> timelineView.detachAudio()));
        m.getItems().add(item("Reset Volume of Selected", null, () -> resetVolume()));
        return m;
    }

    private Menu effectsMenu() {
        Menu m = new Menu("Effects");
        m.getItems().add(item("Add Blur Effect", null, () -> addEffect("blur")));
        m.getItems().add(item("Add Color Adjustment", null, () -> addEffect("color")));
        m.getItems().add(item("Add Crop Effect", null, () -> addEffect("crop")));
        m.getItems().add(item("Add Chroma Key (Green Screen)", null, () -> addEffect("chroma")));
        m.getItems().add(item("Add Text Overlay", "Ctrl+T", () -> addTextClip()));
        return m;
    }

    private Menu recordingMenu() {
        Menu m = new Menu("Recording");
        m.getItems().add(item("Screen Recorder...", "F9", () -> RecorderWindow.show(ctx)));
        m.getItems().add(item("Recording Folder...", null, () -> revealFolder(ctx.config().recordingsDir())));
        return m;
    }

    private Menu exportMenu() {
        Menu m = new Menu("Export");
        m.getItems().add(item("Export Video...", "Ctrl+Shift+R", () -> ExportWindow.show(ctx)));
        m.getItems().add(item("Save Current Frame...", null, () -> saveCurrentFrame()));
        m.getItems().add(item("Exports Folder", null, () -> revealFolder(ctx.config().exportsDir())));
        return m;
    }

    private Menu youtubeMenu() {
        Menu m = new Menu("YouTube");
        m.getItems().add(item("Upload to YouTube...", null, () -> YouTubeWindow.show(ctx)));
        m.getItems().add(item("Setup & Instructions...", null, () -> YouTubeWindow.showSetup(ctx)));
        return m;
    }

    private Menu settingsMenu() {
        Menu m = new Menu("Settings");
        m.getItems().add(item("Settings...", null, () -> SettingsWindow.show(ctx)));
        m.getItems().add(item("Dependency Check...", null, () -> DependencyCheckWindow.show(ctx, null)));
        return m;
    }

    private Menu helpMenu() {
        Menu m = new Menu("Help");
        m.getItems().add(item("Keyboard Shortcuts", null, () -> ShortcutsWindow.show()));
        m.getItems().add(item("About VideoForge", null, () -> AboutWindow.show()));
        return m;
    }

    private void refreshRecentMenu(Menu recent) {
        recent.getItems().clear();
        List<String> list = ctx.config().getRecentProjects();
        if (list.isEmpty()) {
            recent.getItems().add(new MenuItem("(none)"));
            return;
        }
        for (String path : list) {
            Path p = Path.of(path);
            MenuItem mi = new MenuItem(p.getFileName() + "   \u2014  " + p.getParent());
            mi.setOnAction(e -> openProjectPath(p));
            recent.getItems().add(mi);
        }
    }

    private static MenuItem item(String text, String shortcut, Runnable action) {
        MenuItem mi = new MenuItem(text);
        if (shortcut != null) {
            mi.setAccelerator(KeyCombination.keyCombination(shortcut));
        }
        mi.setOnAction(e -> action.run());
        return mi;
    }

    // ======================================================================
    //  File operations
    // ======================================================================

    private void newProject() {
        if (!confirmDiscard()) {
            return;
        }
        ctx.setProject(ctx.projects().createNew("Untitled Project"));
        ctx.undo().clear();
        ctx.status("New project created");
        setTitleFromProject();
    }

    private void openProject() {
        if (!confirmDiscard()) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Project");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("VideoForge Projects", "*" + ProjectManager.EXTENSION));
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            openProjectPath(file.toPath());
        }
    }

    private void openProjectPath(Path path) {
        try {
            VideoProject p = ctx.projects().open(path);
            ctx.setProject(p);
            ctx.undo().clear();
            ctx.status("Opened '" + p.getName() + "'");
            setTitleFromProject();
        } catch (Exception e) {
            LOG.error("Open failed", e);
            showError("Could not open project", e.getMessage());
        }
    }

    private boolean saveProject() {
        VideoProject p = ctx.project();
        if (p.getFilePath() == null) {
            return saveProjectAs();
        }
        try {
            ctx.projects().save(p);
            p.setDirty(false);
            ctx.status("Saved '" + p.getName() + "'");
            setTitleFromProject();
            return true;
        } catch (Exception e) {
            showError("Could not save project", e.getMessage());
            return false;
        }
    }

    private boolean saveProjectAs() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Project");
        chooser.setInitialFileName(ctx.project().getName().replaceAll("[^\\w-]+", "_") + ProjectManager.EXTENSION);
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) {
            return false;
        }
        Path path = file.toPath();
        if (!path.toString().endsWith(ProjectManager.EXTENSION)) {
            path = Path.of(path + ProjectManager.EXTENSION);
        }
        try {
            ctx.projects().saveAs(ctx.project(), path);
            ctx.project().setFilePath(path);
            ctx.project().setDirty(false);
            ctx.status("Saved '" + ctx.project().getName() + "'");
            setTitleFromProject();
            return true;
        } catch (Exception e) {
            showError("Could not save project", e.getMessage());
            return false;
        }
    }

    private void saveCopy() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save a Copy");
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            VideoProject copy = ctx.projects().duplicate(ctx.project(), ctx.project().getName() + " copy");
            copy.setFilePath(file.toPath());
            ctx.projects().saveAs(copy, file.toPath());
            ctx.status("Saved copy to " + file);
        } catch (Exception e) {
            showError("Could not save copy", e.getMessage());
        }
    }

    private boolean confirmDiscard() {
        if (!ctx.project().isDirty()) {
            return true;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved changes");
        alert.setHeaderText("The current project has unsaved changes.");
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType discard = new ButtonType("Discard", ButtonBar.ButtonData.NO);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(save, discard, cancel);
        ButtonType choice = alert.showAndWait().orElse(cancel);
        if (choice == save) {
            return saveProject();
        }
        return choice == discard;
    }

    private void saveCurrentFrame() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Current Frame");
        chooser.setInitialFileName("frame-" + System.currentTimeMillis() + ".png");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG image", "*.png"),
                new FileChooser.ExtensionFilter("JPEG image", "*.jpg"));
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            previewPanel.saveCurrentFrame(file, file.getName().endsWith(".jpg") ? "jpg" : "png");
        }
    }

    private void revealFolder(Path folder) {
        videoforge.utils.FileUtils.revealInFileManager(folder);
    }

    // ======================================================================
    //  Edit operations
    // ======================================================================

    private void undo() {
        ctx.undo().undo();
        updateUndoStatus();
        ctx.status("Undo: " + ctx.undo().undoLabel());
        setTitleFromProject();
    }

    private void redo() {
        ctx.undo().redo();
        updateUndoStatus();
        ctx.status("Redo: " + ctx.undo().redoLabel());
        setTitleFromProject();
    }

    private void selectAll() {
        List<String> ids = ctx.project().timeline().allClips().stream()
                .map(TimelineClip::getId).collect(java.util.stream.Collectors.toList());
        ctx.project().timeline().select(ids);
    }

    private void setInPoint() {
        ctx.project().timeline().setInPoint(ctx.project().timeline().playhead());
        ctx.status("In point set at " + videoforge.utils.TimeUtils.toHMS(ctx.project().timeline().playhead()));
    }

    private void setOutPoint() {
        ctx.project().timeline().setOutPoint(ctx.project().timeline().playhead());
        ctx.status("Out point set at " + videoforge.utils.TimeUtils.toHMS(ctx.project().timeline().playhead()));
    }

    private void addTrack(Track.Kind kind) {
        Timeline tl = ctx.project().timeline();
        UndoManager.TimelineSnapshotCommand cmd = new UndoManager.TimelineSnapshotCommand(tl, "Add " + kind.toString().toLowerCase() + " track");
        tl.addTrack(kind);
        cmd.captureAfter();
        ctx.undo().execute(cmd);
        ctx.markDirty();
        setTitleFromProject();
    }

    private void addTextClip() {
        Timeline tl = ctx.project().timeline();
        UndoManager.TimelineSnapshotCommand cmd = new UndoManager.TimelineSnapshotCommand(tl, "Add text clip");
        TimelineClip clip = ctx.operations().createTextClip("New Text", tl.playhead());
        tl.addClip(clip);
        tl.select(clip.getId());
        cmd.captureAfter();
        ctx.undo().execute(cmd);
        ctx.markDirty();
        ctx.status("Added text clip at " + videoforge.utils.TimeUtils.toHMS(tl.playhead()));
        setTitleFromProject();
    }

    private void addMarkerAtPlayhead() {
        videoforge.timeline.Marker m = new videoforge.timeline.Marker(ctx.project().timeline().playhead());
        m.setName("Marker " + (ctx.project().timeline().markers().size() + 1));
        ctx.project().timeline().addMarker(m);
        ctx.markDirty();
        ctx.status("Added marker");
    }

    private void addEffect(String kind) {
        List<TimelineClip> sel = ctx.project().timeline().selectedClips();
        if (sel.isEmpty()) {
            ctx.status("Select a clip on the timeline first");
            return;
        }
        List<String> ids = sel.stream().map(TimelineClip::getId).collect(java.util.stream.Collectors.toList());
        UndoManager.ClipSnapshotCommand cmd = new UndoManager.ClipSnapshotCommand(ctx.project().timeline(), "Add " + kind + " effect", ids);
        for (TimelineClip clip : sel) {
            switch (kind) {
                case "blur" -> clip.addEffect(new videoforge.effects.BlurEffect());
                case "color" -> clip.addEffect(new videoforge.effects.ColorEffect());
                case "crop" -> clip.addEffect(new videoforge.effects.CropEffect());
                case "chroma" -> clip.addEffect(new videoforge.effects.ChromaKeyEffect());
                default -> {
                }
            }
            ctx.project().timeline().notifyClipChanged(clip);
        }
        cmd.captureAfter();
        ctx.undo().execute(cmd);
        ctx.markDirty();
        ctx.status("Added " + kind + " effect to " + sel.size() + " clip(s)");
        setTitleFromProject();
    }

    private void resetVolume() {
        List<TimelineClip> sel = ctx.project().timeline().selectedClips();
        if (sel.isEmpty()) {
            return;
        }
        List<String> ids = sel.stream().map(TimelineClip::getId).collect(java.util.stream.Collectors.toList());
        UndoManager.ClipSnapshotCommand cmd = new UndoManager.ClipSnapshotCommand(ctx.project().timeline(), "Reset volume", ids);
        for (TimelineClip clip : sel) {
            clip.setVolume(1.0);
            ctx.project().timeline().notifyClipChanged(clip);
        }
        cmd.captureAfter();
        ctx.undo().execute(cmd);
        ctx.markDirty();
        ctx.status("Volume reset to 100%");
    }

    // ======================================================================
    //  Keyboard shortcuts & autosave
    // ======================================================================

    private void installKeyboardShortcuts() {
        addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown() || e.isMetaDown()) {
                return; // accelerators handle these
            }
            switch (e.getCode()) {
                case SPACE -> {
                    if (e.isShiftDown()) {
                        ctx.project().timeline().setPlayhead(ctx.project().timeline().playhead()
                                - videoforge.utils.TimeUtils.secondsToMicros(5));
                    } else {
                        previewPanel.togglePlay();
                    }
                    e.consume();
                }
                case RIGHT -> stepPlayhead(e.isShiftDown() ? 10 : 1);
                case LEFT -> stepPlayhead(e.isShiftDown() ? -10 : -1);
                case HOME -> {
                    ctx.project().timeline().setPlayhead(0);
                    e.consume();
                }
                case END -> {
                    ctx.project().timeline().setPlayhead(ctx.project().timeline().duration());
                    e.consume();
                }
                default -> {
                }
            }
        });
    }

    private void stepPlayhead(int frames) {
        double fps = ctx.config().getDouble("previewPlaybackFps") > 0
                ? ctx.config().getDouble("previewPlaybackFps") : 30;
        long step = videoforge.utils.TimeUtils.secondsToMicros(frames / fps);
        ctx.project().timeline().setPlayhead(Math.max(0, ctx.project().timeline().playhead() + step));
    }

    private void startAutosave() {
        int seconds = Math.max(10, ctx.config().getInt("autosaveIntervalSeconds"));
        autosave.scheduleAtFixedRate(() -> {
            try {
                if (!closing && ctx.project().isDirty()) {
                    ctx.projects().autosave(ctx.project());
                }
            } catch (Exception e) {
                LOG.warn("Autosave error: " + e.getMessage());
            }
        }, seconds, seconds, TimeUnit.SECONDS);
    }

    // ======================================================================
    //  Shutdown
    // ======================================================================

    public void shutdown() {
        if (closing) {
            return;
        }
        closing = true;
        autosave.shutdownNow();
        try {
            if (ctx.project().isDirty()) {
                ctx.projects().autosave(ctx.project());
            }
        } catch (Exception e) {
            LOG.warn("Final autosave failed: " + e.getMessage());
        }
        ctx.library().save();
        ctx.preview().shutdown();
        Platform.exit();
    }

    private static void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(header);
        alert.setHeaderText(header);
        Label l = new Label(message == null ? "Unknown error" : message);
        l.setWrapText(true);
        l.setMaxWidth(500);
        alert.getDialogPane().setContent(l);
        alert.showAndWait();
    }
}
