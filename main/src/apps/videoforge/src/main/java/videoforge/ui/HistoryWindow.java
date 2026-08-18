package videoforge.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Edit > History: shows the recorded command history with undo/redo actions.
 */
public final class HistoryWindow {

    private HistoryWindow() {}

    public static void show(AppContext ctx) {
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Edit History");

        ListView<String> list = new ListView<>(FXCollections.observableArrayList(ctx.undo().history()));
        list.setPrefSize(420, 360);

        Button undo = new Button("Undo (" + ctx.undo().undoLabel() + ")");
        undo.setDisable(!ctx.undo().canUndo());
        undo.setOnAction(e -> {
            ctx.undo().undo();
            stage.close();
            ctx.markDirty();
        });
        Button redo = new Button("Redo (" + ctx.undo().redoLabel() + ")");
        redo.setDisable(!ctx.undo().canRedo());
        redo.setOnAction(e -> {
            ctx.undo().redo();
            stage.close();
            ctx.markDirty();
        });
        Button close = new Button("Close");
        close.setOnAction(e -> stage.close());

        HBox buttons = new HBox(6, undo, redo, close);
        VBox root = new VBox(8, list, buttons);
        VBox.setVgrow(list, Priority.ALWAYS);
        root.setPadding(new Insets(10));
        stage.setScene(new Scene(root));
        stage.show();
    }
}
