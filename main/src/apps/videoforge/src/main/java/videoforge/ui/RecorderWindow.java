package videoforge.ui;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import videoforge.logging.AppLog;
import videoforge.recording.ScreenRecorder;
import videoforge.utils.TimeUtils;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Screen recorder window: choose resolution, frame rate, microphone / system
 * audio and webcam overlay, run a countdown, then record with a live timer and
 * file-size readout. Finished recordings can be imported straight into the
 * media library and timeline.
 */
public final class RecorderWindow {

    private static final AppLog LOG = AppLog.get("recorder");

    private final AppContext ctx;
    private final Stage stage = new Stage();

    private final TextField widthField = new TextField();
    private final TextField heightField = new TextField();
    private final ComboBox<String> fpsBox = new ComboBox<>();
    private final ComboBox<String> audioBox = new ComboBox<>();
    private final CheckBox webcamBox = new CheckBox("Webcam picture-in-picture (if available)");
    private final Label countdownLabel = new Label();
    private final Label timerLabel = new Label("00:00:00");
    private final Label sizeLabel = new Label("0 MB");
    private final Button startBtn = new Button("Start Recording");
    private final Button stopBtn = new Button("Stop");
    private final Button importBtn = new Button("Import into Project");
    private final Button openBtn = new Button("Open Folder");

    private final AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            ScreenRecorder r = recorder.get();
            if (r != null && r.isRunning()) {
                timerLabel.setText(TimeUtils.toHMS(TimeUtils.secondsToMicros(r.elapsedSeconds())));
                sizeLabel.setText(String.format("%.1f MB", r.fileSize() / 1_048_576.0));
            }
        }
    };
    private final AtomicReference<ScreenRecorder> recorder = new AtomicReference<>();
    private int countdown;
    private Path lastRecording;

    private RecorderWindow(AppContext ctx) {
        this.ctx = ctx;
    }

    public static void show(AppContext ctx) {
        new RecorderWindow(ctx).showInternal();
    }

    private void showInternal() {
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Screen Recorder");

        int[] screen = detectScreenSize();
        widthField.setText(String.valueOf(screen[0]));
        heightField.setText(String.valueOf(screen[1]));
        fpsBox.getItems().addAll("15", "24", "30", "60");
        fpsBox.setValue("30");
        audioBox.getItems().addAll("No audio", "Microphone", "System audio");
        audioBox.setValue("No audio");
        webcamBox.setSelected(false);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        int row = 0;
        grid.add(new Label("Region"), 0, row);
        HBox res = new HBox(4, widthField, new Label("x"), heightField);
        grid.add(res, 1, row++);
        grid.add(new Label("Frame rate"), 0, row);
        grid.add(fpsBox, 1, row++);
        grid.add(new Label("Audio"), 0, row);
        grid.add(audioBox, 1, row++);
        grid.add(webcamBox, 1, row++);

        startBtn.setOnAction(e -> startRecording());
        stopBtn.setDisable(true);
        stopBtn.setOnAction(e -> stopRecording());
        importBtn.setDisable(true);
        importBtn.setOnAction(e -> importRecording());
        openBtn.setDisable(true);
        openBtn.setOnAction(e -> videoforge.utils.FileUtils.revealInFileManager(lastRecording));

        HBox buttons = new HBox(8, startBtn, stopBtn, importBtn, openBtn);
        buttons.setAlignment(Pos.CENTER);

        VBox status = new VBox(4, timerLabel, sizeLabel, countdownLabel);
        status.setAlignment(Pos.CENTER);
        timerLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        VBox root = new VBox(14, new Label("Record your screen, microphone and webcam."),
                grid, buttons, status);
        root.setPadding(new Insets(16));
        root.setPrefWidth(420);
        stage.setScene(new javafx.scene.Scene(root, 420, 340));
        timer.start();
        stage.show();
    }

    private void startRecording() {
        try {
            int w = Integer.parseInt(widthField.getText().trim());
            int h = Integer.parseInt(heightField.getText().trim());
            double fps = Double.parseDouble(fpsBox.getValue());
            String audio = switch (audioBox.getValue()) {
                case "Microphone" -> "mic";
                case "System audio" -> "system";
                default -> "none";
            };
            String base = ctx.config().getString("recordingFolder");
            Path dir = "recordings".equals(base) ? ctx.config().recordingsDir() : Path.of(base);
            Path out = dir.resolve("recording-" + System.currentTimeMillis() + ".mp4");

            countdown = Math.max(0, ctx.config().getInt("recordingCountdown"));
            startBtn.setDisable(true);
            stopBtn.setDisable(true);
            importBtn.setDisable(true);
            openBtn.setDisable(true);
            countdownLabel.setText(countdown > 0 ? "Starting in " + countdown + "..." : "");
            new Thread(() -> {
                while (countdown > 0) {
                    int left = countdown;
                    Platform.runLater(() -> countdownLabel.setText("Starting in " + left + "..."));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        return;
                    }
                    countdown--;
                }
                Platform.runLater(() -> countdownLabel.setText("Recording..."));
                try {
                    ScreenRecorder rec = ScreenRecorder.start(out, w, h, fps, audio, webcamBox.isSelected());
                    recorder.set(rec);
                    Platform.runLater(() -> {
                        timerLabel.setText("00:00:00");
                        stopBtn.setDisable(false);
                    });
                } catch (Exception e) {
                    LOG.error("Could not start recording", e);
                    recorder.set(null);
                    Platform.runLater(() -> {
                        startBtn.setDisable(false);
                        countdownLabel.setText("Error: " + e.getMessage());
                    });
                }
            }, "recorder-start").start();
        } catch (NumberFormatException e) {
            ctx.status("Invalid recording settings");
        }
    }

    private void stopRecording() {
        ScreenRecorder rec = recorder.getAndSet(null);
        if (rec == null) {
            return;
        }
        stopBtn.setDisable(true);
        countdownLabel.setText("Finalizing...");
        new Thread(() -> {
            Path out = rec.stop();
            lastRecording = out;
            Platform.runLater(() -> {
                countdownLabel.setText("Saved " + out.getFileName());
                startBtn.setDisable(false);
                importBtn.setDisable(false);
                openBtn.setDisable(false);
            });
        }, "recorder-stop").start();
    }

    private void importRecording() {
        if (lastRecording == null) {
            return;
        }
        mediaPanelImport(lastRecording);
        stage.close();
    }

    private void mediaPanelImport(Path path) {
        new Thread(() -> {
            ctx.library().importPath(path);
            Platform.runLater(() -> {
                ctx.status("Imported recording into the media library");
            });
        }, "recorder-import").start();
    }

    private static int[] detectScreenSize() {
        try {
            Process p = new ProcessBuilder("xrandr", "--current").redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            p.waitFor();
            for (String line : out.split("\n")) {
                if (line.contains("*")) {
                    String res = line.trim().split("\\s+")[0];
                    int x = res.indexOf('x');
                    int w = Integer.parseInt(res.substring(0, x));
                    int h = Integer.parseInt(res.substring(x + 1));
                    if (w > 0 && h > 0) {
                        return new int[]{w, h};
                    }
                }
            }
        } catch (Exception ignored) {
        }
        try {
            java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
            java.awt.GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
            java.awt.Dimension d = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            if (d.width > 0 && d.height > 0) {
                java.awt.geom.AffineTransform t = gc.getDefaultTransform();
                double sx = t.getScaleX();
                double sy = t.getScaleY();
                if (sx > 1.0 || sy > 1.0) {
                    return new int[]{(int) Math.round(d.width * sx), (int) Math.round(d.height * sy)};
                }
                return new int[]{d.width, d.height};
            }
        } catch (Exception ignored) {
        }
        return new int[]{1920, 1080};
    }
}
