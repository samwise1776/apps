package videoforge.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import videoforge.config.AppConfig;

/**
 * Application settings: theme, accent, autosave, snapping, preview, recording,
 * and external FFmpeg/FFprobe paths.
 */
public final class SettingsWindow {

    private SettingsWindow() {}

    public static void show(AppContext ctx) {
        AppConfig cfg = ctx.config();
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Settings");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));

        ComboBox<String> theme = new ComboBox<>();
        theme.getItems().addAll("dark", "light");
        theme.setValue(cfg.getString("theme"));
        theme.setOnAction(e -> {
            cfg.put("theme", theme.getValue());
            reapplyTheme(stage);
        });

        ColorPicker accent = new ColorPicker(parseColor(cfg.getString("accent"), Color.CYAN));
        accent.setOnAction(e -> cfg.put("accent", toHex(accent.getValue())));

        CheckBox autosaveEnabled = new CheckBox("Autosave enabled");
        autosaveEnabled.setSelected(cfg.getBool("autosaveEnabled"));
        autosaveEnabled.setOnAction(e -> cfg.put("autosaveEnabled", autosaveEnabled.isSelected()));

        TextField autosaveInterval = new TextField(String.valueOf(cfg.getInt("autosaveIntervalSeconds")));
        autosaveInterval.setPrefColumnCount(5);
        autosaveInterval.setOnAction(e -> {
            try {
                int v = Integer.parseInt(autosaveInterval.getText());
                if (v >= 5) {
                    cfg.put("autosaveIntervalSeconds", v);
                }
            } catch (NumberFormatException ex) {
            }
        });

        CheckBox snap = new CheckBox("Enable snapping");
        snap.setSelected(cfg.getBool("snapEnabled"));
        snap.setOnAction(e -> cfg.put("snapEnabled", snap.isSelected()));

        CheckBox loopPreview = new CheckBox("Loop preview playback");
        loopPreview.setSelected(cfg.getBool("loopPreview"));
        loopPreview.setOnAction(e -> cfg.put("loopPreview", loopPreview.isSelected()));

        ComboBox<String> previewFps = new ComboBox<>();
        previewFps.getItems().addAll("24", "30", "50", "60");
        previewFps.setValue(String.valueOf((int) Math.round(cfg.getDouble("previewPlaybackFps"))));
        previewFps.setOnAction(e -> cfg.put("previewPlaybackFps", Double.parseDouble(previewFps.getValue())));

        ComboBox<String> countdown = new ComboBox<>();
        countdown.getItems().addAll("0", "3", "5", "10");
        countdown.setValue(String.valueOf(cfg.getInt("recordingCountdown")));
        countdown.setOnAction(e -> cfg.put("recordingCountdown", Integer.parseInt(countdown.getValue())));

        CheckBox micMonitor = new CheckBox("Microphone monitoring");
        micMonitor.setSelected(cfg.getBool("microphoneMonitoring"));
        micMonitor.setOnAction(e -> cfg.put("microphoneMonitoring", micMonitor.isSelected()));

        TextField ffmpegPath = new TextField(cfg.ffmpeg());
        ffmpegPath.setPrefColumnCount(30);
        ffmpegPath.setOnAction(e -> cfg.setFfmpegPath(ffmpegPath.getText()));
        TextField ffprobePath = new TextField(cfg.ffprobe());
        ffprobePath.setPrefColumnCount(30);
        ffprobePath.setOnAction(e -> cfg.setFfprobePath(ffprobePath.getText()));

        int r = 0;
        grid.add(new Label("Theme"), 0, r);
        grid.add(theme, 1, r++);
        grid.add(new Label("Accent color"), 0, r);
        grid.add(accent, 1, r++);
        grid.add(autosaveEnabled, 1, r++);
        grid.add(new Label("Autosave interval (s)"), 0, r);
        grid.add(autosaveInterval, 1, r++);
        grid.add(snap, 1, r++);
        grid.add(loopPreview, 1, r++);
        grid.add(new Label("Preview playback FPS"), 0, r);
        grid.add(previewFps, 1, r++);
        grid.add(new Label("Recording countdown (s)"), 0, r);
        grid.add(countdown, 1, r++);
        grid.add(micMonitor, 1, r++);
        grid.add(new Label("FFmpeg path"), 0, r);
        grid.add(ffmpegPath, 1, r++);
        grid.add(new Label("FFprobe path"), 0, r);
        grid.add(ffprobePath, 1, r++);

        Button check = new Button("Check FFmpeg...");
        check.setOnAction(e -> DependencyCheckWindow.show(ctx));
        Button close = new Button("Close");
        close.setOnAction(e -> stage.close());
        HBox buttons = new HBox(6, check, close);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(10, grid, buttons);
        root.setPadding(new Insets(8));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    private static void reapplyTheme(Stage stage) {
        stage.getScene().getRoot().setStyle(null);
        // theme is applied globally by Main; here we just hint to refresh
    }

    static Color parseColor(String hex, Color fallback) {
        try {
            if (hex != null && hex.startsWith("#")) {
                return Color.web(hex);
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    static String toHex(Color c) {
        return String.format("#%02x%02x%02x",
                Math.round(c.getRed() * 255), Math.round(c.getGreen() * 255), Math.round(c.getBlue() * 255));
    }
}
