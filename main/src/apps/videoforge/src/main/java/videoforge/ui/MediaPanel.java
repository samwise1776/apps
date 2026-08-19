package videoforge.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import videoforge.config.AppConfig;
import videoforge.logging.AppLog;
import videoforge.media.MediaFile;
import videoforge.timeline.TimelineClip;
import videoforge.utils.FileUtils;
import videoforge.utils.TimeUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Left-hand media library panel: import, search, filter, sort and a thumbnail
 * list. Double-click adds a clip to the timeline; drag-and-drop works both for
 * dropping files into the panel and for dragging media onto the timeline.
 */
public final class MediaPanel extends VBox {

    private static final AppLog LOG = AppLog.get("editor");
    private static final double THUMB = 64;

    private final AppContext ctx;
    private final ObservableList<MediaFile> items = FXCollections.observableArrayList();
    private final ListView<MediaFile> list = new ListView<>(items);

    private final TextField search = new TextField();
    private final ComboBox<String> kindFilter = new ComboBox<>();
    private final ComboBox<String> folderFilter = new ComboBox<>();
    private final ComboBox<String> sort = new ComboBox<>();
    private final Button addToTimeline = new Button("+ Timeline");
    private final Label infoLabel = new Label();

    public MediaPanel(AppContext ctx) {
        this.ctx = ctx;
        setPrefWidth(260);
        setMinWidth(220);
        getStyleClass().add("app-panel");
        getStyleClass().add("app-panel-border");

        buildControls();
        buildList();

        refresh();
        ctx.library().thumbnails().addCallback((media, thumb) -> {
            javafx.application.Platform.runLater(this::refreshListOnly);
        });
    }

    private void buildControls() {
        Label title = new Label("Media Library");
        title.getStyleClass().add("section-title");

        search.setPromptText("Search media...");
        search.setPrefColumnCount(14);
        search.textProperty().addListener((o, a, b) -> {
            ctx.library().setSearchQuery(b);
            refresh();
        });

        kindFilter.setItems(FXCollections.observableArrayList("All", "video", "audio", "image", "subtitle"));
        kindFilter.setValue("All");
        kindFilter.setOnAction(e -> {
            ctx.library().setKindFilter(kindFilter.getValue());
            refresh();
        });

        folderFilter.setPromptText("Folder");
        folderFilter.setOnAction(e -> {
            ctx.library().setFolderFilter(folderFilter.getValue());
            refresh();
        });

        sort.setItems(FXCollections.observableArrayList("date", "name", "kind", "size"));
        sort.setValue("date");
        sort.setOnAction(e -> {
            ctx.library().setSortBy(sort.getValue());
            refresh();
        });

        Button importBtn = new Button("Import");
        importBtn.setOnAction(e -> importDialog());
        addToTimeline.setOnAction(e -> addSelectedToTimeline());

        HBox row1 = new HBox(6, importBtn, addToTimeline);
        HBox row2 = new HBox(6, new Label("Type"), kindFilter);
        HBox row3 = new HBox(6, new Label("Folder"), folderFilter, new Label("Sort"), sort);
        row2.setAlignment(Pos.CENTER_LEFT);
        row3.setAlignment(Pos.CENTER_LEFT);
        infoLabel.getStyleClass().add("muted");

        getChildren().addAll(title, search, row1, row2, row3, list, infoLabel);
        VBox.setVgrow(list, Priority.ALWAYS);
        setPadding(new Insets(8));
        setSpacing(6);
    }

    private void buildList() {
        list.setCellFactory(lv -> new MediaCell(ctx.library().thumbnails()));
        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                addSelectedToTimeline();
            }
        });
        ContextMenu menu = new ContextMenu();
        MenuItem add = new MenuItem("Add to Timeline");
        add.setOnAction(e -> addSelectedToTimeline());
        MenuItem favorite = new MenuItem("Toggle Favorite");
        favorite.setOnAction(e -> {
            MediaFile mf = list.getSelectionModel().getSelectedItem();
            if (mf != null) {
                mf.setFavorite(!mf.isFavorite());
                ctx.library().save();
                refresh();
            }
        });
        MenuItem reveal = new MenuItem("Reveal in File Manager");
        reveal.setOnAction(e -> {
            MediaFile mf = list.getSelectionModel().getSelectedItem();
            if (mf != null) {
                videoforge.utils.FileUtils.revealInFileManager(Path.of(mf.getPath()));
            }
        });
        MenuItem remove = new MenuItem("Remove from Library");
        remove.setOnAction(e -> {
            MediaFile mf = list.getSelectionModel().getSelectedItem();
            if (mf != null) {
                ctx.library().removeMedia(mf);
                refresh();
            }
        });
        menu.getItems().addAll(add, favorite, reveal, remove);
        list.setContextMenu(menu);
    }

    public void importFiles(List<Path> paths) {
        List<Path> supported = paths.stream()
                .filter(FileUtils::isSupportedMedia)
                .collect(java.util.stream.Collectors.toList());
        if (supported.isEmpty()) {
            ctx.status("No supported media files in selection");
            return;
        }
        ctx.status("Importing " + supported.size() + " file(s)...");
        ctx.library().importPaths(supported, folderFilter.getValue(), mf -> {
            javafx.application.Platform.runLater(this::refresh);
        });
        refresh();
    }

    public void importDialog() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Import Media");
        chooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Media files",
                        "*.mp4", "*.mov", "*.mkv", "*.avi", "*.webm", "*.m4v", "*.mp3", "*.wav", "*.flac",
                        "*.ogg", "*.m4a", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.srt"),
                new javafx.stage.FileChooser.ExtensionFilter("Video", "*.mp4", "*.mov", "*.mkv", "*.avi", "*.webm", "*.m4v"),
                new javafx.stage.FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav", "*.flac", "*.ogg", "*.m4a"),
                new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"),
                new javafx.stage.FileChooser.ExtensionFilter("All files", "*.*"));
        List<File> files = chooser.showOpenMultipleDialog(getScene().getWindow());
        if (files != null) {
            importFiles(files.stream().map(File::toPath).collect(java.util.stream.Collectors.toList()));
        }
    }

    private void addSelectedToTimeline() {
        MediaFile mf = list.getSelectionModel().getSelectedItem();
        if (mf == null) {
            return;
        }
        long time = ctx.project().timeline().playhead();
        TimelineClip clip = ctx.operations().createClip(mf, time);
        ctx.project().timeline().addClip(clip);
        ctx.project().timeline().select(clip.getId());
        ctx.markDirty();
        ctx.status("Added '" + clip.getName() + "' to timeline at " + TimeUtils.toDuration(time));
    }

    public void refresh() {
        refreshFolders();
        refreshListOnly();
    }

    private void refreshFolders() {
        List<String> folders = new ArrayList<>();
        folders.add("All");
        folders.addAll(ctx.library().folders());
        folderFilter.setItems(FXCollections.observableArrayList(folders));
        String current = folderFilter.getValue();
        if (current == null || (!"All".equals(current) && !folders.contains(current))) {
            folderFilter.setValue("All");
            ctx.library().setFolderFilter("All");
        }
    }

    private void refreshListOnly() {
        List<MediaFile> visible = ctx.library().visible();
        items.setAll(visible);
        infoLabel.setText(visible.size() + " item(s)");
    }

    /** Dragging media from the list onto the timeline view. */
    public void installDragSource() {
        list.setOnDragDetected(e -> {
            MediaFile mf = list.getSelectionModel().getSelectedItem();
            if (mf == null) {
                return;
            }
            Dragboard db = list.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString(mf.getPath());
            content.putFiles(List.of(Path.of(mf.getPath()).toFile()));
            db.setContent(content);
            e.consume();
        });
    }

    /** Accept OS-level file drags directly into the panel. */
    public void installDropTarget() {
        setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });
        setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean done = false;
            if (db.hasFiles()) {
                importFiles(db.getFiles().stream().map(File::toPath).collect(java.util.stream.Collectors.toList()));
                done = true;
            }
            e.setDropCompleted(done);
            e.consume();
        });
    }

    private static final class MediaCell extends ListCell<MediaFile> {
        private final videoforge.media.ThumbnailGenerator thumbs;
        private final ImageView thumb = new ImageView();
        private final Label name = new Label();
        private final Label detail = new Label();
        private final HBox box = new HBox(8);

        MediaCell(videoforge.media.ThumbnailGenerator thumbs) {
            this.thumbs = thumbs;
            thumb.setFitWidth(THUMB);
            thumb.setFitHeight(THUMB);
            thumb.setPreserveRatio(true);
            thumb.setSmooth(true);
            name.setWrapText(false);
            detail.getStyleClass().add("muted");
            VBox textBox = new VBox(2, name, detail);
            textBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(textBox, Priority.ALWAYS);
            box.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().addAll(thumb, textBox);
        }

        @Override
        protected void updateItem(MediaFile mf, boolean empty) {
            super.updateItem(mf, empty);
            if (empty || mf == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            name.setText(mf.getName());
            String d = kindLabel(mf.getKind());
            if (mf.metadata().resolutionText().length() > 0) {
                d += "  " + mf.metadata().resolutionText();
            }
            if (mf.metadata().durationText().length() > 0) {
                d += "  " + mf.metadata().durationText();
            }
            if (mf.metadata().getFps() > 0) {
                d += "  " + String.format("%.0f fps", mf.metadata().getFps());
            }
            if (mf.metadata().getVideoCodec().length() > 0) {
                d += "  " + mf.metadata().getVideoCodec();
            }
            detail.setText(d);
            if (mf.isFavorite()) {
                name.setText(name.getText() + " \u2605");
            }
            loadThumb(mf);
            setGraphic(box);
        }

        private void loadThumb(MediaFile mf) {
            String tp = mf.getThumbnailPath();
            if (tp != null && new File(tp).exists()) {
                thumb.setImage(new Image(new File(tp).toURI().toString()));
                return;
            }
            thumb.setImage(null);
            thumbs.generateAsync(mf);
        }
    }

    private static String kindLabel(String kind) {
        return switch (kind) {
            case "video" -> "VIDEO";
            case "audio" -> "AUDIO";
            case "image" -> "IMAGE";
            case "subtitle" -> "SUB";
            default -> "MEDIA";
        };
    }
}
