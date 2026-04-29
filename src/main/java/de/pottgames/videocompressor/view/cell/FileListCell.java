package de.pottgames.videocompressor.view.cell;

import de.pottgames.videocompressor.engine.Ffprobe;
import de.pottgames.videocompressor.engine.ProbeInfo;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
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
    private final Button infoButton;
    private final Button removeButton;
    private final HBox root;

    private ProbeInfo probeInfo;
    private CompletableFuture<ProbeInfo> probeFuture;

    public FileListCell() {
        nameLabel = new Label();
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(Double.MAX_VALUE);

        infoButton = new Button("ℹ");
        infoButton.setVisible(false);
        infoButton.setOnAction(e -> {
            if (probeInfo != null) {
                System.out.println(probeInfo);
            }
        });

        removeButton = new Button("✕");
        removeButton.setOnAction(e -> {
            int index = getIndex();
            if (index >= 0 && index < getListView().getItems().size()) {
                getListView().getItems().remove(index);
            }
        });

        root = new HBox(8, nameLabel, infoButton, removeButton);
        root.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);
    }

    @Override
    protected void updateItem(File file, boolean empty) {
        super.updateItem(file, empty);

        // Cancel previous probe if exists
        if (probeFuture != null) {
            probeFuture.cancel(true);
        }

        if (empty || file == null) {
            probeInfo = null;
            infoButton.setVisible(false);
            setGraphic(null);
            setText(null);
        } else {
            nameLabel.setText(file.getName());
            setGraphic(root);

            // Start async probing
            probeFuture = Ffprobe.probeAsync(file);
            CompletableFuture<ProbeInfo> currentFuture = probeFuture;
            probeFuture.whenComplete((info, throwable) -> {
                if (throwable != null || probeFuture != currentFuture) {
                    return;
                }
                Platform.runLater(() -> {
                    if (probeFuture == currentFuture) {
                        probeInfo = info;
                        infoButton.setVisible(true);
                    }
                });
            });
        }
    }
}
