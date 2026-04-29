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
import javafx.scene.layout.VBox;

/**
 * Custom list cell for displaying video files with a remove button.
 * Displays file name in the top row and probe information in the bottom row.
 */
public class FileListCell extends ListCell<File> {

    private final Label nameLabel;
    private final Button removeButton;
    private final HBox topRow;

    private final Label resolutionFpsLabel;
    private final Label bitrateLabel;
    private final Label codecLabel;
    private final Label fileSizeLabel;
    private final HBox bottomRow;

    private final VBox root;

    private CompletableFuture<ProbeInfo> probeFuture;

    public FileListCell() {
        // Top row components
        nameLabel = new Label();
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(Double.MAX_VALUE);

        removeButton = new Button("✕");
        removeButton.setOnAction(_ -> {
            int index = getIndex();
            if (index >= 0 && index < getListView().getItems().size()) {
                getListView().getItems().remove(index);
            }
        });

        topRow = new HBox(8, nameLabel, removeButton);
        topRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        // Bottom row components - fixed size labels
        resolutionFpsLabel = new Label();
        resolutionFpsLabel.setMaxWidth(120);
        resolutionFpsLabel.setMinWidth(120);

        bitrateLabel = new Label();
        bitrateLabel.setMaxWidth(80);
        bitrateLabel.setMinWidth(80);

        codecLabel = new Label();
        codecLabel.setMaxWidth(80);
        codecLabel.setMinWidth(80);

        fileSizeLabel = new Label();
        fileSizeLabel.setMaxWidth(80);
        fileSizeLabel.setMinWidth(80);

        bottomRow = new HBox(
            8,
            resolutionFpsLabel,
            bitrateLabel,
            codecLabel,
            fileSizeLabel
        );
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        // Root layout
        root = new VBox(4, topRow, bottomRow);
        root.setStyle("-fx-padding: 4;");
    }

    @Override
    protected void updateItem(File file, boolean empty) {
        super.updateItem(file, empty);

        // Cancel previous probe if exists
        if (probeFuture != null) {
            probeFuture.cancel(true);
        }

        if (empty || file == null) {
            setGraphic(null);
            setText(null);
            // Reset labels
            resolutionFpsLabel.setText("");
            bitrateLabel.setText("");
            codecLabel.setText("");
            fileSizeLabel.setText("");
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
                        updateProbeInfoLabels(info);
                    }
                });
            });
        }
    }

    private void updateProbeInfoLabels(ProbeInfo info) {
        resolutionFpsLabel.setText(info.getResolutionFpsString());
        bitrateLabel.setText(
            String.format("%.0f kbit/s", info.bitrate() / 1000.0)
        );
        codecLabel.setText(info.codec());
        fileSizeLabel.setText(
            String.format("%.1f MiB", info.fileSize() / (1024.0 * 1024.0))
        );
    }
}
