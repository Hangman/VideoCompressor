package org.example.view.step;

import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import org.example.view.StepView;

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
        statusLabel.setFont(new Font(14));
        statusLabel.setStyle("-fx-text-fill: #333;");

        logArea = new TextArea();
        logArea.setPrefRowCount(8);
        logArea.setWrapText(true);
        logArea.setEditable(false);
        logArea.setStyle(
                "-fx-background-color: #f5f5f5; " +
                        "-fx-border-color: #ddd; " +
                        "-fx-border-radius: 4; " +
                        "-fx-font-family: monospace; " +
                        "-fx-font-size: 12px;"
        );

        startButton = new Button("Start Processing");
        startButton.setStyle(
            "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 24;"
        );
        startButton.setOnAction(e -> onStartClicked());

        cancelButton = new Button("Cancel");
        cancelButton.setStyle(
            "-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 24;"
        );
        cancelButton.setOnAction(e -> onCancelClicked());
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
        statusLabel.setStyle("-fx-text-fill: #4CAF50;");
        progressBar.setProgress(1);
        addLogEntry("Processing completed successfully.");
    }

    public void onProcessingFailed(String error) {
        isProcessing = false;
        startButton.setVisible(true);
        cancelButton.setVisible(false);
        statusLabel.setText("Processing failed");
        statusLabel.setStyle("-fx-text-fill: #f44336;");
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
}
