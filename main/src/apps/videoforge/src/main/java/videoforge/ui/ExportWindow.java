package videoforge.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import videoforge.logging.AppLog;
import videoforge.project.ExportSettings;
import videoforge.project.VideoProject;
import videoforge.rendering.ExportPreset;
import videoforge.rendering.RenderEngine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Export dialog: pick a preset or tune settings, choose a destination and watch
 * the render progress. The export runs on a background thread and the window
 * reports percent, estimated remaining time and live FFmpeg output.
 */
public final class ExportWindow {

    private static final AppLog LOG = AppLog.get("editor");

    private final AppContext ctx;
    private final ExportSettings settings;
    private final Stage stage = new Stage();

    private final ComboBox<String> presetBox = new ComboBox<>();
    private final TextField widthField = new TextField();
    private final TextField heightField = new TextField();
    private final TextField fpsField = new TextField();
    private final ComboBox<String> codecBox = new ComboBox<>();
    private final ComboBox<String> audioBox = new ComboBox<>();
    private final ComboBox<String> qualityMode = new ComboBox<>();
    private final TextField crfField = new TextField();
    private final TextField outPath = new TextField();
    private final ProgressBar progress = new ProgressBar(0);
    private final Label etaLabel = new Label();
    private final TextArea logArea = new TextArea();
    private final Button exportBtn = new Button("Export");
    private final Button cancelBtn = new Button("Cancel");

    private RenderEngine engine;

    private ExportWindow(AppContext ctx) {
        this.ctx = ctx;
        this.settings = ctx.project().exportSettings().copy();
    }

    public static void show(AppContext ctx) {
        new ExportWindow(ctx).showInternal();
    }

    private void showInternal() {
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Export Video");

        presetBox.getItems().addAll(java.util.Arrays.stream(ExportPreset.all())
                .map(p -> p.label).collect(java.util.stream.Collectors.toList()));
        presetBox.setOnAction(e -> applyPreset());

        codecBox.getItems().addAll("h264 (AVC)", "h265 (HEVC)", "vp9 (WebM)", "av1");
        audioBox.getItems().addAll("aac", "mp3", "opus", "pcm_s16le (WAV/MOV)");
        qualityMode.getItems().addAll("Constant Quality (CRF)", "Target Bitrate");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        int row = 0;
        grid.add(new Label("Preset"), 0, row);
        grid.add(presetBox, 1, row++);
        grid.add(new Label("Resolution"), 0, row);
        HBox resBox = new HBox(4, widthField, new Label("x"), heightField);
        grid.add(resBox, 1, row++);
        grid.add(new Label("Frame rate"), 0, row);
        grid.add(fpsField, 1, row++);
        grid.add(new Label("Video codec"), 0, row);
        grid.add(codecBox, 1, row++);
        grid.add(new Label("Audio codec"), 0, row);
        grid.add(audioBox, 1, row++);
        grid.add(new Label("Quality"), 0, row);
        grid.add(qualityMode, 1, row++);
        grid.add(new Label("CRF (lower = better)"), 0, row);
        grid.add(crfField, 1, row++);
        grid.add(new Label("Output file"), 0, row);
        HBox outBox = new HBox(4, outPath, browseBtn());
        HBox.setHgrow(outPath, Priority.ALWAYS);
        grid.add(outBox, 1, row++);

        loadSettings();

        HBox progressRow = new HBox(8, progress, etaLabel);
        progress.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(progress, Priority.ALWAYS);
        progressRow.setAlignment(Pos.CENTER_LEFT);

        logArea.setEditable(false);
        logArea.setPrefHeight(180);
        logArea.setPromptText("FFmpeg output appears here...");

        HBox buttons = new HBox(8, exportBtn, cancelBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        exportBtn.setOnAction(e -> startExport());
        cancelBtn.setOnAction(e -> {
            if (engine != null) {
                engine.cancel();
            } else {
                stage.close();
            }
        });

        VBox root = new VBox(10, grid, progressRow, logArea, buttons);
        root.setPadding(new Insets(12));
        stage.setScene(new javafx.scene.Scene(root, 460, 520));
        stage.show();
    }

    private void loadSettings() {
        presetBox.setValue(settings.presetLabel);
        widthField.setText(String.valueOf(settings.width));
        heightField.setText(String.valueOf(settings.height));
        fpsField.setText(String.valueOf(settings.fps));
        codecBox.setValue(settings.videoCodec);
        audioBox.setValue(settings.audioCodec);
        qualityMode.setValue("crf".equals(settings.qualityMode)
                ? "Constant Quality (CRF)" : "Target Bitrate");
        crfField.setText(String.valueOf(settings.crf));
        Path def = ctx.config().exportsDir().resolve(sanitize(ctx.project().getName()) + ".mp4");
        outPath.setText(def.toString());
    }

    private void applyPreset() {
        String label = presetBox.getValue();
        for (ExportPreset p : ExportPreset.all()) {
            if (p.label.equals(label)) {
                widthField.setText(String.valueOf(p.width));
                heightField.setText(String.valueOf(p.height));
                fpsField.setText(String.valueOf(p.fps));
                codecBox.setValue(p.videoCodec);
                audioBox.setValue(p.audioCodec);
                qualityMode.setValue("Constant Quality (CRF)");
                crfField.setText(String.valueOf(p.crf));
                settings.presetLabel = p.label;
                break;
            }
        }
    }

    private Button browseBtn() {
        Button b = new Button("Browse...");
        b.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Export Video To");
            chooser.setInitialFileName(sanitize(ctx.project().getName()) + ".mp4");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("MP4 (H.264)", "*.mp4"),
                    new FileChooser.ExtensionFilter("MOV", "*.mov"),
                    new FileChooser.ExtensionFilter("WebM", "*.webm"),
                    new FileChooser.ExtensionFilter("MKV", "*.mkv"));
            File file = chooser.showSaveDialog(stage);
            if (file != null) {
                outPath.setText(file.toString());
            }
        });
        return b;
    }

    private void startExport() {
        ExportSettings s = new ExportSettings();
        try {
            s.width = Integer.parseInt(widthField.getText().trim());
            s.height = Integer.parseInt(heightField.getText().trim());
            s.fps = Double.parseDouble(fpsField.getText().trim());
            s.crf = Integer.parseInt(crfField.getText().trim());
            s.presetLabel = presetBox.getValue();
        } catch (NumberFormatException e) {
            ctx.status("Invalid export settings: " + e.getMessage());
            return;
        }
        String codec = codecBox.getValue();
        s.videoCodec = codec.contains("h265") ? "h265" : codec.contains("vp9") ? "vp9"
                : codec.contains("av1") ? "av1" : "h264";
        s.audioCodec = audioBox.getValue().startsWith("pcm") ? "pcm_s16le"
                : audioBox.getValue().startsWith("opus") ? "libopus"
                : audioBox.getValue().startsWith("mp3") ? "libmp3lame" : "aac";
        s.qualityMode = "Constant Quality (CRF)".equals(qualityMode.getValue()) ? "crf" : "bitrate";
        s.bitrate = Math.max(1_000_000, (long) (s.width * s.height * s.fps * 0.1));
        s.container = outPath.getText().endsWith(".webm") ? "webm"
                : outPath.getText().endsWith(".mov") ? "mov"
                : outPath.getText().endsWith(".mkv") ? "mkv" : "mp4";

        Path target = Path.of(outPath.getText());
        if (target.toString().isBlank()) {
            ctx.status("Choose an output file first");
            return;
        }
        if (Files.exists(target)) {
            ctx.status("Output file exists; overwriting");
        }

        // remember settings for this project
        ctx.project().exportSettings().load(s.toJson());
        ctx.project().exportSettings().presetLabel = s.presetLabel;

        exportBtn.setDisable(true);
        cancelBtn.setText("Cancel");
        progress.setProgress(0);
        logArea.clear();

        engine = new RenderEngine();
        Thread t = new Thread(() -> {
            RenderEngine.Result result = engine.render(ctx.project(), s, target, ctx.library(), (pct, eta, line) -> {
                Platform.runLater(() -> {
                    progress.setProgress(pct / 100.0);
                    etaLabel.setText(String.format("%.0f%%  ETA %s", pct, eta));
                    if (line != null && !line.isBlank()) {
                        logArea.appendText(line + "\n");
                        if (logArea.getLength() > 50_000) {
                            logArea.deleteText(0, 10_000);
                        }
                    }
                });
            });
            Platform.runLater(() -> {
                engine = null;
                exportBtn.setDisable(false);
                if (result.ok) {
                    progress.setProgress(1);
                    etaLabel.setText("Done");
                    logArea.appendText(result.message + "\n");
                    ctx.status(result.message);
                } else {
                    logArea.appendText(result.message + "\n");
                    ctx.status("Export failed: " + result.message);
                    progress.setProgress(0);
                }
            });
        }, "export-render");
        t.setDaemon(true);
        t.start();
    }

    private static String sanitize(String name) {
        return name == null ? "video" : name.replaceAll("[^\\w-]+", "_");
    }
}
