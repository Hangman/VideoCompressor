package de.pottgames.videocompressor.view.step;

import de.pottgames.videocompressor.view.StepView;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;

/**
 * Step 3 of the video compressor wizard: displays the processing progress,
 * status updates, and any errors that occur during video processing.
 */
public class Step3View implements StepView {

    private final ProgressBar progressBar;
    private final Label statusLabel;
    private final TextArea logArea;
    private final Button startButton;
    private final Button cancelButton;
    private final VBox root;
    private boolean isProcessing = false;
    private Consumer<Boolean> cancelListener;

    public Step3View() {
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);

        statusLabel = new Label("Ready to process");

        logArea = new TextArea();
        logArea.setPrefRowCount(8);
        logArea.setWrapText(true);
        logArea.setEditable(false);
        logArea.setStyle("-fx-font-family: monospace; ");

        startButton = new Button("Start Processing");
        startButton.setStyle("-fx-padding: 8 24;");
        startButton.setOnAction(_ -> onStartClicked());

        cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-padding: 8 24;");
        cancelButton.setOnAction(_ -> onCancelClicked());
        cancelButton.setVisible(false);

        HBox buttonBox = new HBox(12, startButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(16, 0, 0, 0));

        VBox progressBox = new VBox(8, statusLabel, progressBar);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setPadding(new Insets(12, 0, 0, 0));

        root = new VBox(
            16,
            new Label("Processing Video"),
            progressBox,
            logArea,
            buttonBox
        );
        root.setPadding(new Insets(20));
        VBox.setVgrow(logArea, Priority.ALWAYS);
    }

    @Override
    public javafx.scene.Node getNode() {
        return root;
    }

    public void setCancelListener(Consumer<Boolean> listener) {
        this.cancelListener = listener;
    }

    public void onProcessingStarted() {
        isProcessing = true;
        startButton.setVisible(false);
        cancelButton.setVisible(true);
        statusLabel.setText("Processing...");
        progressBar.setProgress(0);
        logArea.clear();
        addLogEntry("Processing started.");
    }

    public void onProcessingCompleted() {
        isProcessing = false;
        startButton.setVisible(true);
        cancelButton.setVisible(false);
        statusLabel.setText("Processing complete!");
        // Status color handled by Dracula theme
        progressBar.setProgress(1);
        addLogEntry("Processing completed successfully.");
    }

    public void onProcessingFailed(String error) {
        isProcessing = false;
        startButton.setVisible(true);
        cancelButton.setVisible(false);
        statusLabel.setText("Processing failed");
        // Status color handled by Dracula theme
        progressBar.setProgress(-1);
        addLogEntry("Error: " + error);
    }

    public void updateProgress(double progress) {
        if (progress >= 0 && progress <= 1) {
            progressBar.setProgress(progress);
            int percent = (int) (progress * 100);
            statusLabel.setText("Processing... " + percent + "%");
        }
    }

    public void addLogEntry(String message) {
        String timestamp = java.time.LocalDateTime.now().toString();
        logArea.appendText("[" + timestamp + "] " + message + "\n");
        logArea.setScrollTop(Double.MAX_VALUE);
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    private void onStartClicked() {
        if (cancelListener != null) {
            cancelListener.accept(false);
        }
    }

    private void onCancelClicked() {
        if (cancelListener != null) {
            cancelListener.accept(true);
        }
    }

    @Override
    public void activate(
        Button backButton,
        Button centerButton,
        Button nextButton
    ) {
        // TODO
    }

    @Override
    public void deactivate() {
        // TODO
    }
}
