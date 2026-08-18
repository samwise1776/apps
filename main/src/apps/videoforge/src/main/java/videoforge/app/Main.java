package videoforge.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import videoforge.config.AppConfig;
import videoforge.logging.AppLog;
import videoforge.media.MediaLibrary;
import videoforge.project.ProjectManager;
import videoforge.project.ProjectSerializer;
import videoforge.project.VideoProject;
import videoforge.rendering.FFmpegManager;
import videoforge.ui.AppContext;
import videoforge.ui.DependencyCheckWindow;
import videoforge.ui.MainWindow;
import videoforge.ui.theme.Theme;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * VideoForge Studio entry point. Boots the JavaFX application, verifies FFmpeg,
 * offers crash-recovery of autosaved projects on the first run of a session and
 * opens the main editor window.
 */
public final class Main extends Application {

    private static final AppLog LOG = AppLog.get("editor");

    @Override
    public void start(Stage stage) {
        AppConfig config = AppConfig.get();
        AppContext ctx = new AppContext();

        // detect and auto-configure FFmpeg on first run or when unset
        FFmpegManager.Availability avail = ctx.ffmpeg().checkAvailability();
        if (!avail.ffmpegOk && !avail.message.isBlank()) {
            ctx.status(avail.message);
        }

        boolean firstRun = config.isFirstRun();
        if (firstRun || !avail.ffmpegOk) {
            DependencyCheckWindow.show(ctx, stage);
        }

        MainWindow window = new MainWindow(ctx);
        window.setTitleFromProject();
        Scene scene = new Scene(window, 1440, 860);
        applyTheme(scene);

        stage.setScene(scene);
        stage.setTitle(window.title());
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setOnCloseRequest(e -> window.shutdown());
        stage.show();

        // crash recovery for the previous session
        if (!firstRun) {
            List<ProjectManager.RecoveryCandidate> candidates = ctx.projects().findRecoveryCandidates();
            if (!candidates.isEmpty()) {
                offerRecovery(ctx, window, candidates);
            }
        }

        config.put("_lastLaunch", String.valueOf(System.currentTimeMillis()));
        config.save();
    }

    private void applyTheme(javafx.scene.Scene scene) {
        try {
            Path css = AppConfig.get().tempDir().resolve("theme.css");
            Files.writeString(css, Theme.css());
            scene.getStylesheets().clear();
            scene.getStylesheets().add(css.toUri().toURL().toExternalForm());
        } catch (Exception e) {
            LOG.warn("Could not apply theme: " + e.getMessage());
        }
    }

    private void offerRecovery(AppContext ctx, MainWindow window, List<ProjectManager.RecoveryCandidate> candidates) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Recover unsaved work?");
        alert.setHeaderText("VideoForge found " + candidates.size() + " autosaved project(s) newer than their last save.");
        StringBuilder body = new StringBuilder("Choose one to restore:\n\n");
        for (ProjectManager.RecoveryCandidate c : candidates) {
            body.append("  \u2022 ").append(c.label).append("\n");
        }
        javafx.scene.control.Label content = new javafx.scene.control.Label(body.toString());
        content.setWrapText(true);
        alert.getDialogPane().setContent(content);
        javafx.scene.control.ButtonType recover = new javafx.scene.control.ButtonType("Recover", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        javafx.scene.control.ButtonType skip = new javafx.scene.control.ButtonType("Skip", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(recover, skip);
        javafx.scene.control.ButtonType choice = alert.showAndWait().orElse(skip);
        if (choice == recover && !candidates.isEmpty()) {
            try {
                VideoProject p = ctx.projects().recover(candidates.get(0));
                ctx.setProject(p);
                window.setTitleFromProject();
                ctx.status("Recovered '" + p.getName() + "' from autosave");
            } catch (Exception e) {
                LOG.error("Recovery failed", e);
                ctx.status("Could not recover autosave: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
