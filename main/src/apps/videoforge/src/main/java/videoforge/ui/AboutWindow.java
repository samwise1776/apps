package videoforge.ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Help > About window.
 */
public final class AboutWindow {

    private AboutWindow() {}

    public static void show() {
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("About VideoForge Studio");

        Label title = new Label("VideoForge Studio 1.0.0");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label body = new Label("""
                Open-source desktop video editor and screen recorder written in Java.

                Record your screen, microphone, system audio and webcam; edit on a
                professional multi-track timeline; add text, images, effects, audio;
                render with FFmpeg and publish to YouTube.

                - Java: JavaFX
                - Media processing: FFmpeg / FFprobe
                - Project format: .vforge (JSON)
                - Publishing: YouTube Data API v3 (OAuth 2.0)

                Licensed under the MIT License.
                """);
        body.setWrapText(true);
        Button close = new Button("Close");
        close.setOnAction(e -> stage.close());
        VBox root = new VBox(12, title, body, close);
        root.setPadding(new Insets(18));
        stage.setScene(new Scene(root, 460, 360));
        stage.show();
    }
}
