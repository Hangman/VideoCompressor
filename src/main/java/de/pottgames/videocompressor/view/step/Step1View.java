package de.pottgames.videocompressor.view.step;

import de.pottgames.videocompressor.WizardState;
import de.pottgames.videocompressor.engine.Engine;
import de.pottgames.videocompressor.view.StepView;
import de.pottgames.videocompressor.view.cell.FileListCell;
import java.io.File;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
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
    private final StackPane root;
    private Button nextButton;
    private ImageView logoView;
    private VBox placeholderVBox;
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

        // Logo shown when no videos have been added yet
        Image logoImage = new Image(
            getClass().getResourceAsStream("/logo_466.png")
        );
        logoView = new ImageView(logoImage);
        logoView.setFitHeight(300);
        logoView.setFitWidth(300);
        logoView.setPreserveRatio(true);

        // Placeholder VBox containing logo and drop label, hidden when files are added
        placeholderVBox = new VBox(10, logoView, dropLabel);
        placeholderVBox.setAlignment(Pos.CENTER);

        // Overlay placeholder inside the ListView area using a StackPane
        root = new StackPane(fileListView, placeholderVBox);
        root.setPadding(new Insets(20));

        setupDragAndDrop();

        files.addListener(
            (ListChangeListener<File>) _ -> {
                updateLogoVisibility();
                updateNextButtonState();
            }
        );
    }

    @Override
    public javafx.scene.Node getNode() {
        return root;
    }

    public ObservableList<File> getFiles() {
        return files;
    }

    private boolean isDuplicate(File file) {
        String absolutePath = file.getAbsolutePath();
        return files
            .stream()
            .anyMatch(f -> f.getAbsolutePath().equals(absolutePath));
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
                .filter(file -> !isDuplicate(file))
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
                    .filter(file -> !isDuplicate(file))
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
    public void activate(WizardState state) {
        var backButton = state.getBackButton();
        var centerButton = state.getCenterButton();
        var nextButton = state.getNextButton();

        activated = true;
        centerButton.setText("Videos importieren");
        centerButton.setVisible(true);
        centerButton.setDisable(false);
        centerButton.setOnAction(_ -> showFileChooser());
        backButton.setVisible(false);
        this.nextButton = nextButton;
        updateNextButtonState();
    }

    private void updateLogoVisibility() {
        placeholderVBox.setVisible(files.isEmpty());
    }

    private void updateNextButtonState() {
        if (!activated) return;

        nextButton.setVisible(!files.isEmpty());
    }

    @Override
    public void deactivate(WizardState state) {
        activated = false;
        state.getImportedFiles().clear();
        state.getImportedFiles().addAll(files);
    }
}
