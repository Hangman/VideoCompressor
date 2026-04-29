package de.pottgames.videocompressor.view.step;

import de.pottgames.videocompressor.engine.Engine;
import de.pottgames.videocompressor.view.StepView;
import de.pottgames.videocompressor.view.cell.FileListCell;
import java.io.File;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Step 1 of the video compressor wizard: allows the user to add video files
 * either by drag-and-drop or via a file chooser dialog.
 */
public class Step1View implements StepView {

    private final ObservableList<File> files =
        FXCollections.observableArrayList();
    private final ListView<File> fileListView;
    private final Label dropLabel;
    private final VBox root;
    private Button nextButton;
    private boolean activated = false;

    public Step1View() {
        fileListView = new ListView<>(files);
        fileListView.setPrefHeight(200);
        fileListView.setCellFactory(_ -> new FileListCell());
        fileListView.setSelectionModel(null);

        dropLabel = new Label(
            "Drag & drop video files here\nor click the button below to browse"
        );
        dropLabel.setWrapText(true);

        root = new VBox(10, dropLabel, fileListView);
        root.setPadding(new Insets(20));
        VBox.setVgrow(fileListView, Priority.ALWAYS);

        setupDragAndDrop();

        files.addListener(
            (ListChangeListener<File>) _ -> updateNextButtonState()
        );
    }

    @Override
    public javafx.scene.Node getNode() {
        return root;
    }

    public ObservableList<File> getFiles() {
        return files;
    }

    private void showFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Video Files");
        fileChooser
            .getExtensionFilters()
            .addAll(
                new FileChooser.ExtensionFilter(
                    "Video Files",
                    Engine.SUPPORTED_EXTENSION_PATTERNS.toArray(new String[0])
                ),
                new FileChooser.ExtensionFilter("All Files", "*.*")
            );
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(
            fileListView.getScene().getWindow()
        );
        if (selectedFiles != null) {
            selectedFiles
                .stream()
                .filter(Engine::isCompatible)
                .forEach(files::add);
        }
    }

    private void setupDragAndDrop() {
        root.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            event.consume();
        });

        root.setOnDragDropped(event -> {
            javafx.scene.input.Dragboard dragboard = event.getDragboard();
            boolean success = false;
            if (dragboard.hasFiles()) {
                dragboard
                    .getFiles()
                    .stream()
                    .filter(Engine::isCompatible)
                    .forEach(files::add);
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        // Style the drop area when dragging over
        root.setOnDragEntered(event -> {
            event.consume();
        });

        root.setOnDragExited(event -> {
            event.consume();
        });
    }

    @Override
    public void activate(
        Button backButton,
        Button centerButton,
        Button nextButton
    ) {
        activated = true;
        centerButton.setText("Dateien hinzufügen");
        centerButton.setVisible(true);
        centerButton.setDisable(false);
        centerButton.setOnAction(_ -> showFileChooser());
        backButton.setVisible(false);
        this.nextButton = nextButton;
        updateNextButtonState();
    }

    private void updateNextButtonState() {
        if (!activated) return;

        nextButton.setVisible(!files.isEmpty());
    }

    @Override
    public void deactivate() {
        activated = false;
    }
}
