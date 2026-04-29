package de.pottgames.videocompressor.view.step;

import de.pottgames.videocompressor.view.StepView;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Step 2 of the video compressor wizard: allows the user to select a processing preset.
 * Each preset defines a combination of encoding parameters optimized for different use cases.
 */
public class Step2View implements StepView {

    public enum Preset {
        HIGH_QUALITY(
            "High Quality",
            "Minimal compression, best possible output quality",
            "Large",
            "-c:v libx264 -preset slow -crf 18 -c:a aac -b:a 256k"
        ),
        BALANCED(
            "Balanced",
            "Good quality with reasonable file size",
            "Medium",
            "-c:v libx264 -preset medium -crf 23 -c:a aac -b:a 192k"
        ),
        SMALL_SIZE(
            "Small Size",
            "Maximum compression, acceptable quality loss",
            "Small",
            "-c:v libx264 -preset fast -crf 28 -c:a aac -b:a 128k"
        ),
        YOUTUBE(
            "YouTube Upload",
            "Optimized for YouTube upload",
            "Medium",
            "-c:v libx264 -preset medium -crf 20 -c:a aac -b:a 192k -movflags +faststart"
        ),
        VIMEO(
            "Vimeo Upload",
            "Optimized for Vimeo upload",
            "Medium",
            "-c:v libx264 -preset medium -crf 20 -c:a aac -b:a 192k -movflags +faststart"
        ),
        WEB_OPTIMIZED(
            "Web Optimized",
            "Fast streaming, web-friendly format",
            "Small",
            "-c:v libx264 -preset fast -crf 24 -c:a aac -b:a 128k -movflags +faststart"
        );

        private final String displayName;
        private final String description;
        private final String estimatedSize;
        private final String ffmpegArgs;

        Preset(
            String displayName,
            String description,
            String estimatedSize,
            String ffmpegArgs
        ) {
            this.displayName = displayName;
            this.description = description;
            this.estimatedSize = estimatedSize;
            this.ffmpegArgs = ffmpegArgs;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public String getEstimatedSize() {
            return estimatedSize;
        }

        public String getFfmpegArgs() {
            return ffmpegArgs;
        }
    }

    private final ObservableList<Preset> presets =
        FXCollections.observableArrayList();
    private final ListView<Preset> presetListView;
    private final Label detailLabel;
    private final Label sizeLabel;
    private final VBox root;
    private Preset selectedPreset;
    private Consumer<Preset> selectionListener;

    public Step2View() {
        presets.addAll(Preset.values());

        presetListView = new ListView<>(presets);
        presetListView.setPrefHeight(250);
        presetListView.setCellFactory(_ -> new PresetListCell());
        presetListView
            .getSelectionModel()
            .selectedItemProperty()
            .addListener((_, _, newVal) -> {
                if (newVal != null) {
                    selectedPreset = newVal;
                    updateDetailPane(newVal);
                    notifySelection();
                }
            });

        detailLabel = new Label("Select a preset to see details");
        detailLabel.setWrapText(true);

        sizeLabel = new Label("Estimated output size: —");

        VBox detailBox = new VBox(5, detailLabel, sizeLabel);
        detailBox.setPadding(new Insets(10, 0, 0, 0));

        root = new VBox(
            15,
            new Label("Select a Processing Preset"),
            presetListView,
            detailBox
        );
        root.setPadding(new Insets(20));
        VBox.setVgrow(presetListView, Priority.ALWAYS);

        // Style the list view
        presetListView.setStyle("-fx-background-color: transparent;");
    }

    @Override
    public javafx.scene.Node getNode() {
        return root;
    }

    public Preset getSelectedPreset() {
        return selectedPreset;
    }

    public void setSelectionListener(Consumer<Preset> listener) {
        this.selectionListener = listener;
    }

    private void updateDetailPane(Preset preset) {
        detailLabel.setText(preset.getDescription());
        sizeLabel.setText(
            "Estimated output size: " + preset.getEstimatedSize()
        );
    }

    private void notifySelection() {
        if (selectionListener != null && selectedPreset != null) {
            selectionListener.accept(selectedPreset);
        }
    }

    /**
     * Custom list cell for displaying presets with visual indicators.
     */
    private static class PresetListCell
        extends javafx.scene.control.ListCell<Preset>
    {

        private final VBox root;
        private final Label nameLabel;
        private final Label sizeIndicator;

        public PresetListCell() {
            nameLabel = new Label();

            sizeIndicator = new Label();

            root = new VBox(4, nameLabel, sizeIndicator);
            root.setPadding(new Insets(8, 12, 8, 12));
            root.setStyle("-fx-background-color: transparent;");
        }

        @Override
        protected void updateItem(Preset preset, boolean empty) {
            super.updateItem(preset, empty);

            if (empty || preset == null) {
                setGraphic(null);
                setText(null);
            } else {
                nameLabel.setText(preset.getDisplayName());
                sizeIndicator.setText(preset.getEstimatedSize() + " output");
                setGraphic(root);
                setText(null);

                // Style selected item
                if (isSelected()) {
                    root.setStyle(
                        "-fx-border-color: #2196f3; -fx-border-width: 1;"
                    );
                } else {
                    root.setStyle("-fx-background-color: transparent;");
                }
            }
        }
    }

    @Override
    public void activate(
        Button backButton,
        Button centerButton,
        Button nextButton
    ) {
        backButton.setVisible(true);
        backButton.setDisable(false);
        nextButton.setVisible(false);
        nextButton.setDisable(false);
        centerButton.setVisible(false);
    }

    @Override
    public void deactivate() {
        
    }
}
