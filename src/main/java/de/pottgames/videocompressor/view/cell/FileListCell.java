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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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

    private final HBox resolutionFpsBadge;
    private final Label resolutionFpsText;
    private final HBox bitrateBadge;
    private final Label bitrateText;
    private final HBox codecBadge;
    private final Label codecText;
    private final HBox fileSizeBadge;
    private final Label fileSizeText;
    private final HBox durationBadge;
    private final Label durationText;
    private final HBox bottomRow;

    private final VBox root;
    private final Separator separator;

    private CompletableFuture<ProbeInfo> probeFuture;

    public FileListCell() {
        Image clockImage = new Image(
            getClass().getResourceAsStream("/clock_icon_monochrome.png")
        );
        Image screenImage = new Image(
            getClass().getResourceAsStream("/screen_icon_monochrome.png")
        );
        Image storageImage = new Image(
            getClass().getResourceAsStream("/database_icon_monochrome.png")
        );
        Image codecImage = new Image(
            getClass().getResourceAsStream("/codec_icon_monochrome.png")
        );
        Image bitrateImage = new Image(
            getClass().getResourceAsStream("/bitrate_icon_monochrome.png")
        );

        // Top row components
        nameLabel = new Label();
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        nameLabel.getStyleClass().addAll(Styles.TITLE_4, Styles.TEXT_BOLD);

        removeButton = new Button("X");
        removeButton.setStyle("-fx-cursor: hand;");
        removeButton.getStyleClass().addAll(Styles.TITLE_4, Styles.TEXT_BOLD);
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

        // Bottom row components - badges with icon + label

        // Resolution + FPS badge
        resolutionFpsBadge = createBadge(120, screenImage);
        resolutionFpsText = (Label) resolutionFpsBadge
            .getChildren()
            .get(resolutionFpsBadge.getChildren().size() - 1);

        // Bitrate badge
        bitrateBadge = createBadge(100, bitrateImage);
        bitrateText = (Label) bitrateBadge
            .getChildren()
            .get(bitrateBadge.getChildren().size() - 1);

        // Codec badge
        codecBadge = createBadge(80, codecImage);
        codecText = (Label) codecBadge
            .getChildren()
            .get(codecBadge.getChildren().size() - 1);

        // Duration badge
        durationBadge = createBadge(100, clockImage);
        durationText = (Label) durationBadge
            .getChildren()
            .get(durationBadge.getChildren().size() - 1);

        // File size badge
        fileSizeBadge = createBadge(100, storageImage);
        fileSizeText = (Label) fileSizeBadge
            .getChildren()
            .get(fileSizeBadge.getChildren().size() - 1);

        bottomRow = new HBox(
            8,
            resolutionFpsBadge,
            bitrateBadge,
            codecBadge,
            durationBadge,
            fileSizeBadge
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
        setPadding(new Insets(10, 10, 10, 10));
    }

    /**
     * Creates a badge HBox containing an optional ImageView icon and a Label.
     */
    private HBox createBadge(double maxWidth, Image icon) {
        HBox badge = new HBox(6);
        badge.setStyle(
            "-fx-background-color: #282A36; -fx-background-radius: 8; -fx-padding: 4 8 4 8;"
        );
        badge.setMaxWidth(maxWidth);
        badge.setMinWidth(maxWidth);
        badge.setAlignment(Pos.CENTER_LEFT);

        boolean showIcon = icon != null;
        if (showIcon) {
            ImageView iconView = new ImageView(icon);
            iconView.setFitWidth(12);
            iconView.setFitHeight(12);
            badge.getChildren().add(iconView);
        }

        Label textLabel = new Label();
        textLabel.setMaxWidth(showIcon ? maxWidth - 16 : maxWidth);
        textLabel.setMinWidth(showIcon ? maxWidth - 16 : maxWidth);
        textLabel.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_SUBTLE);

        badge.getChildren().add(textLabel);
        return badge;
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
            resolutionFpsText.setText("");
            bitrateText.setText("");
            codecText.setText("");
            fileSizeText.setText("");
            durationText.setText("");
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
        resolutionFpsText.setText(info.getResolutionFpsString());
        bitrateText.setText(
            String.format("%.0f kbit/s", info.bitrate() / 1000.0)
        );
        codecText.setText(info.codec());
        fileSizeText.setText(
            String.format("%.1f MiB", info.fileSize() / (1024.0 * 1024.0))
        );
        durationText.setText(info.formatDuration());
    }
}
