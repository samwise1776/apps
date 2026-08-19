package videoforge.ui;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import videoforge.logging.AppLog;
import videoforge.rendering.PreviewEngine;
import videoforge.timeline.Timeline;
import videoforge.utils.TimeUtils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Central preview panel. Renders the timeline through {@link PreviewEngine}
 * (real FFmpeg frames composited with text/images/effects) and offers the full
 * transport control set: play/pause/stop, frame stepping, jumps, in/out range,
 * playback rate and preview quality.
 */
public final class PreviewPanel extends VBox {

    private static final AppLog LOG = AppLog.get("editor");

    private final AppContext ctx;
    private final ImageView view = new ImageView();
    private final Label timeLabel = new Label("00:00:00.000");
    private final Label durationLabel = new Label("/ 00:00:00.000");
    private final Label fpsLabel = new Label();

    private final Button playBtn = new Button("\u25B6");
    private final Button stopBtn = new Button("\u25A0");
    private final Button prevFrameBtn = new Button("|<\u2503");
    private final Button nextFrameBtn = new Button("\u2503>|");
    private final Button back5Btn = new Button("<\u25B6");
    private final Button fwd5Btn = new Button("\u25B6>");
    private final Button startBtn = new Button("|<\u25B6");
    private final Button endBtn = new Button("\u25B6>|");
    private final ComboBox<String> quality = new ComboBox<>();
    private final ComboBox<String> rate = new ComboBox<>();
    private final ToggleButton fullscreenBtn = new ToggleButton("\u26F6");
    private final Slider scrubber = new Slider();

    private final AnimationTimer timer;
    private boolean playing;
    private double lastFrameAt = -1;
    private volatile BufferedImage lastImage;
    private long lastShownTime = -1;

    private final ExecutorService renderPool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "preview-render");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean renderQueued = new AtomicBoolean();
    private volatile long requestedTime = -1;
    private boolean stopped = true;

    public PreviewPanel(AppContext ctx) {
        this.ctx = ctx;
        setSpacing(4);
        setPadding(new Insets(6));
        getStyleClass().add("app-panel");
        getStyleClass().add("app-panel-border");

        BorderPane center = new BorderPane();
        center.setCenter(view);
        view.setPreserveRatio(true);
        view.setSmooth(true);

        center.setOnMouseMoved(this::updateScrubHover);
        center.setOnMousePressed(this::onPreviewPress);

        quality.getItems().addAll("Full", "Half", "Quarter");
        quality.setValue(switch (ctx.config().getString("previewQuality")) {
            case "FULL" -> "Full";
            case "QUARTER" -> "Quarter";
            default -> "Half";
        });
        quality.setOnAction(e -> {
            ctx.preview().setQuality(switch (quality.getValue()) {
                case "Full" -> PreviewEngine.Quality.FULL;
                case "Quarter" -> PreviewEngine.Quality.QUARTER;
                default -> PreviewEngine.Quality.HALF;
            });
            lastImage = null;
            renderNow(false);
        });

        rate.getItems().addAll("0.25x", "0.5x", "1x", "1.5x", "2x");
        rate.setValue("1x");

        buildTransportButtons();
        buildScrubber();

        VBox top = new VBox(2, transportBar(), qualityBar());
        getChildren().addAll(top, center, scrubber, timeBar());

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (playing) {
                    stepPlayback();
                }
            }
        };
        timer.start();

        // keep the view in sync whenever the timeline changes
        ctx.project().timeline().addListener(this::onTimelineChanged);
        view.fitWidthProperty().bind(center.widthProperty().subtract(8));
        view.fitHeightProperty().bind(center.heightProperty().subtract(8));
        updateTimeLabels();
    }

    private void onTimelineChanged(Timeline.ChangeType type) {
        if (type == Timeline.ChangeType.PLAYHEAD) {
            Platform.runLater(() -> {
                updateTimeLabels();
                renderNow(false);
            });
        } else if (type == Timeline.ChangeType.STRUCTURE
                || type == Timeline.ChangeType.CLIP
                || type == Timeline.ChangeType.PROJECT) {
            Platform.runLater(() -> {
                updateTimeLabels();
                renderNow(true);
            });
        }
    }

    // ---------- transport ----------

    private void buildTransportButtons() {
        playBtn.setOnAction(e -> togglePlay());
        stopBtn.setOnAction(e -> stopPlayback());
        prevFrameBtn.setOnAction(e -> step(-1));
        nextFrameBtn.setOnAction(e -> step(1));
        back5Btn.setOnAction(e -> jump(-TimeUtils.secondsToMicros(5)));
        fwd5Btn.setOnAction(e -> jump(TimeUtils.secondsToMicros(5)));
        startBtn.setOnAction(e -> ctx.project().timeline().setPlayhead(0));
        endBtn.setOnAction(e -> ctx.project().timeline().setPlayhead(ctx.project().timeline().duration()));
        playBtn.setTooltip(new Tooltip("Play/Pause (Space)"));
        stopBtn.setTooltip(new Tooltip("Stop"));
    }

    private void buildScrubber() {
        scrubber.setMin(0);
        scrubber.setMax(1);
        scrubber.setOnMousePressed(e -> {
            stopPlayback();
            scrubTo(e);
        });
        scrubber.setOnMouseDragged(this::scrubTo);
    }

    private void scrubTo(MouseEvent e) {
        double pct = e.getX() / Math.max(1, scrubber.getWidth());
        long duration = Math.max(1, ctx.project().timeline().duration());
        ctx.project().timeline().setPlayhead(Math.round(pct * duration));
    }

    private void updateScrubHover(MouseEvent e) {
        // hover timecode feedback is handled by the timeline; nothing to do here
    }

    private void onPreviewPress(MouseEvent e) {
        // click-preview scrubbing by horizontal position
        stopPlayback();
        long duration = Math.max(1, ctx.project().timeline().duration());
        double pct = Math.max(0, Math.min(1, e.getX() / Math.max(1, view.getFitWidth())));
        ctx.project().timeline().setPlayhead(Math.round(pct * duration));
    }

    public void togglePlay() {
        if (playing) {
            pause();
        } else {
            play();
        }
    }

    public void play() {
        Timeline tl = ctx.project().timeline();
        if (tl.duration() <= 0) {
            ctx.status("Timeline is empty");
            return;
        }
        if (tl.outPoint() > tl.inPoint() && tl.inPoint() >= 0) {
            if (tl.playhead() < tl.inPoint() || tl.playhead() >= tl.outPoint()) {
                tl.setPlayhead(tl.inPoint());
            }
        }
        if (tl.playhead() >= tl.duration()) {
            tl.setPlayhead(tl.inPoint() >= 0 ? tl.inPoint() : 0);
        }
        playing = true;
        stopped = false;
        playBtn.setText("\u23F8");
        lastFrameAt = -1;
    }

    public void pause() {
        playing = false;
        playBtn.setText("\u25B6");
    }

    public void stopPlayback() {
        playing = false;
        stopped = true;
        playBtn.setText("\u25B6");
    }

    public boolean isPlaying() {
        return playing;
    }

    private void stepPlayback() {
        Timeline tl = ctx.project().timeline();
        double fps = previewFps();
        double rateMultiplier = parseRate();
        long interval = TimeUtils.secondsToMicros(1.0 / (fps * rateMultiplier));
        long now = System.nanoTime();
        if (lastFrameAt < 0) {
            lastFrameAt = now;
        }
        double elapsed = (now - lastFrameAt) / 1e9;
        if (elapsed < 1.0 / (fps * rateMultiplier) * 0.9) {
            return;
        }
        lastFrameAt = now;
        long next = tl.playhead() + interval;
        long end = tl.outPoint() > tl.inPoint() && tl.inPoint() >= 0 ? tl.outPoint() : tl.duration();
        if (next >= end) {
            next = tl.inPoint() >= 0 ? tl.inPoint() : 0;
            if (ctx.config().getBool("loopPreview")) {
                tl.setPlayhead(next);
            } else {
                tl.setPlayhead(end - interval);
                stopPlayback();
            }
        } else {
            tl.setPlayhead(next);
        }
    }

    private double parseRate() {
        try {
            return Double.parseDouble(rate.getValue().replace("x", ""));
        } catch (Exception e) {
            return 1.0;
        }
    }

    private int previewFps() {
        double f = ctx.config().getDouble("previewPlaybackFps");
        return (int) Math.round(f > 0 ? f : 30);
    }

    private void step(int dir) {
        stopPlayback();
        Timeline tl = ctx.project().timeline();
        double fps = previewFps();
        long frame = TimeUtils.secondsToMicros(1.0 / fps);
        long t = tl.playhead() + dir * frame;
        t = Math.max(0, Math.min(t, tl.duration()));
        tl.setPlayhead(t);
    }

    private void jump(long delta) {
        Timeline tl = ctx.project().timeline();
        tl.setPlayhead(Math.max(0, Math.min(tl.duration(), tl.playhead() + delta)));
    }

    private HBox transportBar() {
        HBox box = new HBox(3, startBtn, back5Btn, prevFrameBtn, playBtn, nextFrameBtn, fwd5Btn, stopBtn, endBtn);
        box.setAlignment(Pos.CENTER);
        for (Button b : new Button[]{startBtn, back5Btn, prevFrameBtn, playBtn, nextFrameBtn, fwd5Btn, stopBtn, endBtn}) {
            b.setStyle("-fx-background-radius: 4; -fx-padding: 3 8 3 8;");
        }
        return box;
    }

    private HBox qualityBar() {
        fullscreenBtn.setTooltip(new Tooltip("Fullscreen preview"));
        fullscreenBtn.setOnAction(e -> toggleFullscreen());
        HBox box = new HBox(8,
                new Label("Quality"), quality,
                new Label("Speed"), rate,
                fullscreenBtn);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private HBox timeBar() {
        HBox box = new HBox(4, timeLabel, durationLabel);
        box.setAlignment(Pos.CENTER_LEFT);
        timeLabel.getStyleClass().add("muted");
        durationLabel.getStyleClass().add("muted");
        fpsLabel.getStyleClass().add("muted");
        HBox right = new HBox(8, fpsLabel);
        right.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(right, javafx.scene.layout.Priority.ALWAYS);
        box.getChildren().add(right);
        return box;
    }

    private void updateTimeLabels() {
        Timeline tl = ctx.project().timeline();
        timeLabel.setText(TimeUtils.toHMS(tl.playhead()));
        durationLabel.setText("/ " + TimeUtils.toHMS(tl.duration()));
        scrubber.setValue(tl.duration() > 0 ? (double) tl.playhead() / tl.duration() : 0);
    }

    // ---------- rendering ----------

    /** Request a (re)render of the current playhead, always asynchronously. */
    public void renderNow(boolean force) {
        Timeline tl = ctx.project().timeline();
        long time = tl.playhead();
        if (!force && time == lastShownTime) {
            return;
        }
        lastShownTime = time;
        requestedTime = time;
        if (!renderQueued.compareAndSet(false, true)) {
            return; // a render is in flight; it will pick up the latest time
        }
        final boolean allowBlocking = !playing && !stopped || !playing;
        renderPool.submit(() -> {
            try {
                BufferedImage img = ctx.preview().renderFrame(tl, requestedTime, lastImage, !playing);
                if (img == null) {
                    renderQueued.set(false);
                    return;
                }
                lastImage = img;
                Platform.runLater(() -> {
                    view.setImage(toFx(img));
                    fpsLabel.setText("preview " + quality.getValue().toLowerCase() + "  " + String.format("%.0f", 1000.0 / Math.max(1, System.currentTimeMillis() - lastShownTime)) + "ms/frame");
                    renderQueued.set(false);
                });
            } catch (Exception e) {
                LOG.error("Preview render failed", e);
                renderQueued.set(false);
            }
        });
    }

    private static Image toFx(BufferedImage img) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(img, "png", baos);
            return new Image(new java.io.ByteArrayInputStream(baos.toByteArray()));
        } catch (Exception e) {
            return null;
        }
    }

    /** Save the current frame as an image file. */
    public void saveCurrentFrame(File out, String format) {
        BufferedImage img = lastImage;
        if (img == null) {
            ctx.status("No frame available yet");
            return;
        }
        try {
            javax.imageio.ImageIO.write(img, format, out);
            ctx.status("Saved frame to " + out);
        } catch (Exception e) {
            LOG.error("Screenshot failed", e);
            ctx.status("Could not save frame: " + e.getMessage());
        }
    }

    private void toggleFullscreen() {
        javafx.stage.Stage stage = (javafx.stage.Stage) getScene().getWindow();
        stage.setFullScreen(!stage.isFullScreen());
    }

    public void shutdown() {
        renderPool.shutdownNow();
    }
}
