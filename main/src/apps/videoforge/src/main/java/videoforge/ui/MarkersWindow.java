package videoforge.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import videoforge.timeline.Marker;
import videoforge.timeline.Timecode;
import videoforge.utils.TimeUtils;

/**
 * Marker and chapter manager. Markers can be tagged as YouTube chapters;
 * chapter text can be copied to the clipboard for the description box.
 */
public final class MarkersWindow {

    private MarkersWindow() {}

    public static void show(AppContext ctx) {
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Markers / Chapters");

        ObservableList<Marker> items = FXCollections.observableArrayList(ctx.project().timeline().markers());

        TableView<Marker> table = new TableView<>(items);
        table.setPrefSize(620, 320);
        table.setEditable(true);

        TableColumn<Marker, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timeText"));
        timeCol.setPrefWidth(90);

        TableColumn<Marker, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(e -> {
            e.getRowValue().setName(e.getNewValue());
            ctx.project().timeline().notifyMarkersChanged();
            ctx.markDirty();
        });
        nameCol.setPrefWidth(180);

        TableColumn<Marker, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setCellFactory(TextFieldTableCell.forTableColumn());
        descCol.setOnEditCommit(e -> {
            e.getRowValue().setDescription(e.getNewValue());
            ctx.project().timeline().notifyMarkersChanged();
            ctx.markDirty();
        });
        descCol.setPrefWidth(220);

        TableColumn<Marker, String> colorCol = new TableColumn<>("Color");
        colorCol.setCellValueFactory(new PropertyValueFactory<>("color"));
        colorCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText("  ");
                    setStyle("-fx-background-color: " + item + ";");
                }
            }
        });
        colorCol.setPrefWidth(60);

        TableColumn<Marker, Boolean> chapterCol = new TableColumn<>("Chapter");
        chapterCol.setCellValueFactory(new PropertyValueFactory<>("chapter"));
        chapterCol.setCellFactory(cc -> new javafx.scene.control.cell.CheckBoxTableCell<>());
        chapterCol.setOnEditCommit(e -> {
            e.getRowValue().setChapter(e.getNewValue());
            ctx.project().timeline().notifyMarkersChanged();
            ctx.markDirty();
        });
        chapterCol.setPrefWidth(70);

        table.getColumns().addAll(timeCol, nameCol, descCol, colorCol, chapterCol);

        Button add = new Button("Add at Playhead");
        add.setOnAction(e -> {
            Marker m = new Marker(ctx.project().timeline().playhead());
            m.setName("Marker " + (items.size() + 1));
            ctx.project().timeline().addMarker(m);
            items.setAll(ctx.project().timeline().markers());
            ctx.markDirty();
        });
        Button editColor = new Button("Set Color...");
        editColor.setOnAction(e -> {
            Marker m = table.getSelectionModel().getSelectedItem();
            if (m != null) {
                ColorPicker picker = new ColorPicker(Color.web(m.getColor()));
                Stage s = new Stage();
                Button ok = new Button("OK");
                ok.setOnAction(ev -> {
                    m.setColor(toHex(picker.getValue()));
                    ctx.project().timeline().notifyMarkersChanged();
                    items.setAll(ctx.project().timeline().markers());
                    ctx.markDirty();
                    s.close();
                });
                s.setScene(new Scene(new VBox(8, new Label("Marker color"), picker, ok), 220, 130));
                s.initOwner(stage);
                s.show();
            }
        });
        Button remove = new Button("Remove Selected");
        remove.setOnAction(e -> {
            Marker m = table.getSelectionModel().getSelectedItem();
            if (m != null) {
                ctx.project().timeline().removeMarker(m.getId());
                items.setAll(ctx.project().timeline().markers());
                ctx.markDirty();
            }
        });
        Button chapters = new Button("Copy Chapters Text");
        chapters.setOnAction(e -> {
            String text = ctx.project().timeline().chapterText();
            if (text.isBlank()) {
                ctx.status("No chapter markers defined");
            } else {
                javafx.scene.input.Clipboard.getSystemClipboard()
                        .setContent(java.util.Map.of(javafx.scene.input.DataFormat.PLAIN_TEXT, text));
                ctx.status("Chapters copied to clipboard");
            }
        });
        Button close = new Button("Close");
        close.setOnAction(e -> stage.close());

        HBox buttons = new HBox(6, add, editColor, remove, chapters, close);
        buttons.setAlignment(Pos.CENTER_LEFT);

        Label hint = new Label("Markers marked as \"Chapter\" are converted to YouTube chapter timestamps. "
                + "Tip: use Edit > Copy Chapters Text to copy the generated block.");
        hint.setWrapText(true);
        hint.getStyleClass().add("muted");

        VBox root = new VBox(8, table, buttons, hint);
        root.setPadding(new Insets(10));
        stage.setScene(new Scene(root));
        stage.show();
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x",
                Math.round(c.getRed() * 255), Math.round(c.getGreen() * 255), Math.round(c.getBlue() * 255));
    }
}
