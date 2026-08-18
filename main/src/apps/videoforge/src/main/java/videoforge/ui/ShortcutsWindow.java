package videoforge.ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Help > Keyboard Shortcuts reference.
 */
public final class ShortcutsWindow {

    private ShortcutsWindow() {}

    public static void show() {
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Keyboard Shortcuts");

        String shortcuts = """
                Space ......... Play / Pause
                S ............. Split at Playhead
                Delete ........ Delete Selection
                Ctrl+Z ........ Undo
                Ctrl+Y ........ Redo
                Ctrl+S ........ Save Project
                Ctrl+Shift+S .. Save As
                Ctrl+O ........ Open Project
                Ctrl+N ........ New Project
                Left .......... Previous Frame
                Right ......... Next Frame
                Home .......... Timeline Start
                End ........... Timeline End
                Ctrl+C ........ Copy Clip
                Ctrl+V ........ Paste Clip
                Ctrl+D ........ Duplicate Clip
                Ctrl+Wheel .... Timeline Zoom
                Ctrl+B ........ Set In Point
                Ctrl+E ........ Set Out Point
                Ctrl+Shift+R .. Render / Export
                F9 ............ Start / Stop Recording
                F11 ........... Fullscreen Preview
                """;

        Label text = new Label(shortcuts);
        text.setFont(javafx.scene.text.Font.font("Monospaced", 12));
        Button close = new Button("Close");
        close.setOnAction(e -> stage.close());

        VBox root = new VBox(10, text, close);
        root.setPadding(new Insets(14));
        stage.setScene(new Scene(root));
        stage.show();
    }
}
