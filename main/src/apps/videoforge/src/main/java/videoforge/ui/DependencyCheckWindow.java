package videoforge.ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import videoforge.rendering.FFmpegManager;

/**
 * Dependency checker: verifies FFmpeg/FFprobe availability, reports versions
 * and available encoders. Shown at first run and from Settings.
 */
public final class DependencyCheckWindow {

    private DependencyCheckWindow() {}

    public static void show(videoforge.ui.AppContext ctx) {
        show(ctx, null);
    }

    public static void show(videoforge.ui.AppContext ctx, Stage owner) {
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Dependency Check");

        FFmpegManager.Availability a = ctx.ffmpeg().checkAvailability();
        StringBuilder sb = new StringBuilder();
        sb.append("FFmpeg: ").append(a.ffmpegOk ? "OK" : "MISSING").append('\n');
        if (a.ffmpegOk) sb.append("  ").append(a.ffmpegVersion).append('\n');
        sb.append('\n');
        sb.append("FFprobe: ").append(a.ffprobeOk ? "OK" : "MISSING").append('\n');
        if (a.ffprobeOk) sb.append("  ").append(a.ffprobeVersion).append('\n');
        sb.append('\n');
        if (!a.message.isBlank()) {
            sb.append("Note: ").append(a.message).append('\n').append('\n');
            sb.append("On Debian/Ubuntu: sudo apt install ffmpeg\n");
            sb.append("On Fedora: sudo dnf install ffmpeg\n");
            sb.append("On macOS (Homebrew): brew install ffmpeg\n");
            sb.append("On Windows: download from ffmpeg.org and set the path in Settings.\n");
        }
        sb.append('\n');
        sb.append("Available video encoders (used for export):\n");
        for (String enc : ctx.ffmpeg().availableVideoEncoders()) {
            sb.append("  ").append(enc).append('\n');
        }

        Label text = new Label(sb.toString());
        text.setWrapText(true);
        text.setFont(javafx.scene.text.Font.font("Monospaced", 11));
        javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(text);
        sp.setFitToWidth(true);
        sp.setPrefSize(560, 420);

        Button close = new Button("Close");
        close.setOnAction(e -> stage.close());
        VBox root = new VBox(10, sp, close);
        root.setPadding(new Insets(12));
        stage.setScene(new Scene(root));
        stage.show();
    }
}
