package org.example.view.cell;

import java.io.File;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Custom list cell for displaying video files with a remove button.
 */
public class FileListCell extends ListCell<File> {

    private final Label nameLabel;
    private final Button removeButton;
    private final HBox root;

    public FileListCell() {
        nameLabel = new Label();
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(Double.MAX_VALUE);

        removeButton = new Button("✕");
        removeButton.setOnAction(e -> {
            int index = getIndex();
            if (index >= 0 && index < getListView().getItems().size()) {
                getListView().getItems().remove(index);
            }
        });

        root = new HBox(8, nameLabel, removeButton);
        root.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);
    }

    @Override
    protected void updateItem(File file, boolean empty) {
        super.updateItem(file, empty);

        if (empty || file == null) {
            setGraphic(null);
            setText(null);
        } else {
            nameLabel.setText(file.getName());
            setGraphic(root);
        }
    }
}
