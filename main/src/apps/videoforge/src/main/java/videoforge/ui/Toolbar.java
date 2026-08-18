package videoforge.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

/**
 * Quick-action toolbar under the menu bar.
 */
public final class Toolbar extends HBox {

    public final Button newProject = tool("New", "Ctrl+N");
    public final Button open = tool("Open", "Ctrl+O");
    public final Button save = tool("Save", "Ctrl+S");
    public final Button importMedia = tool("Import Media", "Ctrl+I");
    public final Button addText = tool("Add Text", "");
    public final Button split = tool("Split", "S");
    public final Button undo = tool("Undo", "Ctrl+Z");
    public final Button redo = tool("Redo", "Ctrl+Y");
    public final Button record = tool("Record", "F9");
    public final Button export = tool("Export", "Ctrl+Shift+R");
    public final Button youtube = tool("YouTube", "");

    public Toolbar() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);
        setPadding(new Insets(6, 8, 6, 8));
        getStyleClass().add("toolbar");
        getChildren().addAll(newProject, open, save, importMedia, addText, split, undo, redo, record, export, youtube);
    }

    private static Button tool(String label, String shortcut) {
        Button b = new Button(label);
        b.setStyle("-fx-background-radius: 5; -fx-padding: 5 10 5 10;");
        if (!shortcut.isEmpty()) {
            b.setTooltip(new Tooltip(label + "  (" + shortcut + ")"));
        }
        return b;
    }
}
