package de.pottgames.videocompressor.view.step;

import de.pottgames.videocompressor.WizardState;
import de.pottgames.videocompressor.engine.Engine;
import de.pottgames.videocompressor.i18n.I18n;
import de.pottgames.videocompressor.view.StepView;
import de.pottgames.videocompressor.view.cell.FileListCell;
import java.io.File;
import java.util.List;
import java.util.Objects;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;

/**
 * Step 1 of the video compressor wizard: allows the user to add video files
 * either by drag-and-drop or via a file chooser dialog.
 */
public class Step1View implements StepView {

    private final ObservableList<File> files =
        FXCollections.observableArrayList();
    private final ListView<File> fileListView;
    private final StackPane root;
    private Button nextButton;
    private final VBox placeholderVBox;
    private boolean activated = false;

    public Step1View() {
        fileListView = new ListView<>(files);
        fileListView.setPrefHeight(200);
        fileListView.setCellFactory(_ -> new FileListCell());
        fileListView.setSelectionModel(null);

        var dropLabel = new Label(I18n.get("step1.drop_label"));
        dropLabel.setWrapText(true);
        dropLabel.textAlignmentProperty().set(TextAlignment.CENTER);

        // Logo shown when no videos have been added yet
        Image logoImage = new Image(
                Objects.requireNonNull(getClass().getResourceAsStream("/logo_466.png"))
        );
        var logoView = new ImageView(logoImage);
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
        fileChooser.setTitle(I18n.get("step1.file_chooser_title"));
        fileChooser
            .getExtensionFilters()
            .addAll(
                new FileChooser.ExtensionFilter(
                    I18n.get("step1.filter_video"),
                    Engine.SUPPORTED_EXTENSION_PATTERNS.toArray(new String[0])
                ),
                new FileChooser.ExtensionFilter(
                    I18n.get("step1.filter_all"),
                    "*.*"
                )
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
        root.setOnDragEntered(Event::consume);

        root.setOnDragExited(Event::consume);
    }

    @Override
    public void activate(WizardState state) {
        var backButton = state.getBackButton();
        var centerButton = state.getCenterButton();
        var nextButton = state.getNextButton();

        activated = true;
        centerButton.setText(I18n.get("step1.import_button"));
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
