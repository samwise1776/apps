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
import videoforge.youtube.YouTubeManager;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * YouTube upload + setup window. The setup tab walks through creating an OAuth
 * client in Google Cloud and completing the device authorization; the upload
 * tab posts a rendered video with title/description/tags/privacy and shows live
 * upload progress.
 */
public final class YouTubeWindow {

    private final AppContext ctx;
    private final Stage stage = new Stage();
    private final YouTubeManager yt = new YouTubeManager();

    private final TextField clientIdField = new TextField();
    private final TextField userCodeField = new TextField();
    private final Label authStatus = new Label();
    private final Button authorizeBtn = new Button("Authorize Device");

    private final TextField videoField = new TextField();
    private final TextField titleField = new TextField();
    private final TextArea descArea = new TextArea();
    private final TextField tagsField = new TextField();
    private final ComboBox<String> privacyBox = new ComboBox<>();
    private final ProgressBar progress = new ProgressBar(0);
    private final Label progressLabel = new Label();
    private final Button uploadBtn = new Button("Upload to YouTube");

    private YouTubeWindow(AppContext ctx) {
        this.ctx = ctx;
    }

    public static void show(AppContext ctx) {
        new YouTubeWindow(ctx).showInternal();
    }

    public static void showSetup(AppContext ctx) {
        YouTubeWindow w = new YouTubeWindow(ctx);
        w.showInternal();
    }

    private void showInternal() {
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("YouTube");

        javafx.scene.control.TabPane tabs = new javafx.scene.control.TabPane();

        // ---------- setup tab ----------
        javafx.scene.control.Tab setup = new javafx.scene.control.Tab("Setup");
        setup.setClosable(false);
        Label howto = new Label("""
                How to get an OAuth client ID:
                1. Go to console.cloud.google.com and create a project.
                2. Enable the "YouTube Data API v3" in APIs & Services.
                3. Create OAuth credentials of type "Desktop app".
                4. Paste the client ID below (the long string, not the secret).
                5. Click "Authorize Device", open the shown URL, enter the code.
                The refresh token is stored locally; your client secret never leaves your machine.""");
        howto.setWrapText(true);
        howto.getStyleClass().add("muted");

        clientIdField.setPromptText("Paste your OAuth client ID here");
        clientIdField.setText(yt.clientId() == null ? "" : yt.clientId());
        clientIdField.setOnAction(e -> saveClientId());

        Button saveId = new Button("Save Client ID");
        saveId.setOnAction(e -> {
            saveClientId();
            ctx.status("YouTube client ID saved");
        });

        HBox idRow = new HBox(8, clientIdField, saveId);
        HBox.setHgrow(clientIdField, Priority.ALWAYS);

        authorizeBtn.setOnAction(e -> authorizeDevice());
        Button signOut = new Button("Sign Out");
        signOut.setOnAction(e -> {
            yt.signOut();
            updateAuthStatus();
            ctx.status("Signed out of YouTube");
        });

        VBox setupBox = new VBox(10, howto, new Label("OAuth Client ID"), idRow,
                new Label("Authorization"), new HBox(8, authorizeBtn, signOut),
                new Label("Your device code"), userCodeField, authStatus);
        setupBox.setPadding(new Insets(14));
        setup.setContent(setupBox);
        updateAuthStatus();

        // ---------- upload tab ----------
        javafx.scene.control.Tab upload = new javafx.scene.control.Tab("Upload");
        upload.setClosable(false);

        Button browse = new Button("Browse...");
        browse.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Video to Upload");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Video files",
                    "*.mp4", "*.mov", "*.mkv", "*.webm"));
            File f = chooser.showOpenDialog(stage);
            if (f != null) {
                videoField.setText(f.toString());
                if (titleField.getText().isBlank()) {
                    String name = f.getName();
                    titleField.setText(name.substring(0, name.lastIndexOf('.')) );
                }
            }
        });
        HBox videoRow = new HBox(8, videoField, browse);
        HBox.setHgrow(videoField, Priority.ALWAYS);

        descArea.setPromptText("Description");
        descArea.setPrefRowCount(4);
        tagsField.setPromptText("Comma-separated tags");
        privacyBox.getItems().addAll("public", "unlisted", "private");
        privacyBox.setValue("private");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        int row = 0;
        grid.add(new Label("Video"), 0, row);
        grid.add(videoRow, 1, row++);
        grid.add(new Label("Title"), 0, row);
        grid.add(titleField, 1, row++);
        grid.add(new Label("Description"), 0, row);
        grid.add(descArea, 1, row++);
        grid.add(new Label("Tags"), 0, row);
        grid.add(tagsField, 1, row++);
        grid.add(new Label("Privacy"), 0, row);
        grid.add(privacyBox, 1, row++);

        uploadBtn.setDisable(!yt.isAuthenticated());
        uploadBtn.setOnAction(e -> startUpload());

        HBox progressRow = new HBox(8, progress, progressLabel);
        HBox.setHgrow(progress, Priority.ALWAYS);
        progressRow.setAlignment(Pos.CENTER_LEFT);

        VBox uploadBox = new VBox(10, grid, progressRow, uploadBtn);
        uploadBox.setPadding(new Insets(14));
        upload.setContent(uploadBox);

        tabs.getTabs().addAll(setup, upload);
        stage.setScene(new javafx.scene.Scene(tabs, 540, 520));
        stage.show();
    }

    private void saveClientId() {
        yt.setClientId(clientIdField.getText().trim());
    }

    private void updateAuthStatus() {
        if (yt.isAuthenticated()) {
            authStatus.setText("Authenticated \u2713  (" + yt.accountLabel() + ")");
            uploadBtn.setDisable(false);
        } else {
            authStatus.setText("Not authenticated yet.");
            uploadBtn.setDisable(true);
        }
    }

    private void authorizeDevice() {
        saveClientId();
        try {
            YouTubeManager.DeviceCode code = yt.beginDeviceAuthorization(clientIdField.getText().trim());
            userCodeField.setText(code.userCode);
            authStatus.setText("Go to " + code.verificationUrl + " and enter: " + code.userCode
                    + "  (valid " + code.expiresIn + "s)");
            authorizeBtn.setDisable(true);
            new Thread(() -> {
                try {
                    boolean ok = yt.pollForAuthorization();
                    Platform.runLater(() -> {
                        authorizeBtn.setDisable(false);
                        if (ok) {
                            authStatus.setText("Authenticated successfully!");
                            ctx.status("YouTube authorized");
                        } else {
                            authStatus.setText("Authorization timed out. Try again.");
                        }
                        updateAuthStatus();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        authorizeBtn.setDisable(false);
                        authStatus.setText("Error: " + e.getMessage());
                    });
                }
            }, "youtube-auth").start();
        } catch (Exception e) {
            authStatus.setText("Error: " + e.getMessage());
        }
    }

    private void startUpload() {
        Path file = Path.of(videoField.getText());
        String title = titleField.getText().trim();
        if (file.toString().isBlank() || title.isBlank()) {
            ctx.status("Choose a video and enter a title");
            return;
        }
        List<String> tags = new ArrayList<>();
        for (String t : tagsField.getText().split(",")) {
            if (!t.trim().isBlank()) {
                tags.add(t.trim());
            }
        }
        uploadBtn.setDisable(true);
        progress.setProgress(0);
        progressLabel.setText("Starting upload...");
        new Thread(() -> {
            try {
                var result = yt.upload(file, title, descArea.getText(), tags, privacyBox.getValue(),
                        new YouTubeManager.UploadListener() {
                            @Override
                            public void onProgress(double percent) {
                                Platform.runLater(() -> {
                                    progress.setProgress(percent / 100.0);
                                    progressLabel.setText(String.format("%.0f%%", percent));
                                });
                            }

                            @Override
                            public void onMessage(String msg) {
                                Platform.runLater(() -> progressLabel.setText(msg));
                            }
                        });
                Platform.runLater(() -> {
                    uploadBtn.setDisable(false);
                    if (result.ok) {
                        progress.setProgress(1);
                        progressLabel.setText("Done! " + result.url);
                        ctx.status("Uploaded: " + result.url);
                    } else {
                        progressLabel.setText("Failed: " + result.message);
                        ctx.status("Upload failed: " + result.message);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    uploadBtn.setDisable(false);
                    progressLabel.setText("Failed: " + e.getMessage());
                });
            }
        }, "youtube-upload").start();
    }
}
