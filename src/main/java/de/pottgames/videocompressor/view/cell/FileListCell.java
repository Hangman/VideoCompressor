package de.pottgames.videocompressor.view.cell;

import atlantafx.base.theme.Styles;
import de.pottgames.videocompressor.engine.Ffprobe;
import de.pottgames.videocompressor.engine.ProbeInfo;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Separator;
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
    private final Label durationLabel;
    private final HBox bottomRow;

    private final VBox root;
    private final Separator separator;

    private CompletableFuture<ProbeInfo> probeFuture;

    public FileListCell() {
        // Top row components
        nameLabel = new Label();
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        nameLabel.getStyleClass().addAll(Styles.TEXT_BOLD);

        removeButton = new Button("X");
        removeButton.setStyle("-fx-cursor: hand;");
        removeButton
            .getStyleClass()
            .addAll(Styles.TITLE_4, Styles.TEXT_BOLD, Styles.DANGER);
        removeButton.setPadding(new Insets(0, 8, 0, 8));
        removeButton.setOnAction(_ -> {
            int index = getIndex();
            if (index >= 0 && index < getListView().getItems().size()) {
                getListView().getItems().remove(index);
            }
        });

        topRow = new HBox(8, nameLabel, removeButton);
        topRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        // Bottom row components - fixed size labels with Dracula tag styling
        String tagStyle =
            "-fx-background-color: #44475a; -fx-text-fill: #f8f8f2; -fx-background-radius: 8; -fx-padding: 4 8 4 8;";

        resolutionFpsLabel = new Label();
        resolutionFpsLabel.setMaxWidth(120);
        resolutionFpsLabel.setMinWidth(120);
        resolutionFpsLabel.setStyle(tagStyle);
        resolutionFpsLabel.getStyleClass().addAll(Styles.TEXT_SMALL);

        bitrateLabel = new Label();
        bitrateLabel.setMaxWidth(80);
        bitrateLabel.setMinWidth(80);
        bitrateLabel.setStyle(tagStyle);
        bitrateLabel.getStyleClass().addAll(Styles.TEXT_SMALL);

        codecLabel = new Label();
        codecLabel.setMaxWidth(80);
        codecLabel.setMinWidth(80);
        codecLabel.setStyle(tagStyle);
        codecLabel.getStyleClass().addAll(Styles.TEXT_SMALL);

        durationLabel = new Label();
        durationLabel.setMaxWidth(80);
        durationLabel.setMinWidth(80);
        durationLabel.setStyle(tagStyle);
        durationLabel.getStyleClass().addAll(Styles.TEXT_SMALL);

        fileSizeLabel = new Label();
        fileSizeLabel.setMaxWidth(80);
        fileSizeLabel.setMinWidth(80);
        fileSizeLabel.setStyle(tagStyle);
        fileSizeLabel.getStyleClass().addAll(Styles.TEXT_SMALL);

        bottomRow = new HBox(
            8,
            resolutionFpsLabel,
            bitrateLabel,
            codecLabel,
            durationLabel,
            fileSizeLabel
        );
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        // Separator spacer
        separator = new Separator();
        separator.setStyle(
            "-fx-background-color: transparent; -fx-padding: 6 0 6 0;"
        );

        // Root layout - card-like appearance
        root = new VBox(2, topRow, separator, bottomRow);
        root.setStyle(
            "-fx-padding: 8 12 8 12; -fx-background-color: #343646; -fx-background-radius: 10;"
        );

        // Set padding on the cell itself for spacing between entries
        setPadding(new Insets(8, 8, 8, 8));
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
            durationLabel.setText("");
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
        durationLabel.setText(info.formatDuration());
    }
}
