package de.pottgames.videocompressor.view.step;

import atlantafx.base.theme.Styles;
import de.pottgames.videocompressor.WizardState;
import de.pottgames.videocompressor.engine.JobProcessor;
import de.pottgames.videocompressor.engine.JobProgressListener;
import de.pottgames.videocompressor.engine.VideoJob;
import de.pottgames.videocompressor.engine.VideoJobStatus;
import de.pottgames.videocompressor.view.StepView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;

/**
 * Step 3 of the video compressor wizard: displays the processing progress,
 * status updates, and any errors that occur during video processing.
 */
public class Step3View implements StepView {

    // ── Color constants (Dracula palette) ────────────────────────────────

    private static final String C_COMMENT = "#6272a4";
    private static final String C_FG = "#f8f8f2";

    // ── Layout ───────────────────────────────────────────────────────────

    private final VBox root;

    // ── Progress header ──────────────────────────────────────────────────

    private final Label progressCounterLabel;

    // ── Current video info panel ─────────────────────────────────────────

    private final VBox videoInfoPanel;
    private final Label currentFileNameLabel;
    private final Label currentPassLabel;
    private final ProgressBar progressBar;

    // ── Log output ───────────────────────────────────────────────────────

    private final TextArea logArea;

    // ── Engine ───────────────────────────────────────────────────────────

    private final JobProcessor jobProcessor;
    private List<VideoJob> preparedJobs;

    public Step3View() {
        root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER_LEFT);

        // ── Progress Counter ─────────────────────────────────────────────
        progressCounterLabel = new Label("Bereit zum Starten");
        progressCounterLabel.getStyleClass().addAll(Styles.TITLE_4);

        // ── Current Video Info Panel ─────────────────────────────────────
        videoInfoPanel = new VBox(8);
        videoInfoPanel.setPadding(new Insets(12));
        videoInfoPanel.setStyle(
            "-fx-background-color: #44475a; -fx-background-radius: 8;"
        );
        videoInfoPanel.setAlignment(Pos.CENTER_LEFT);

        currentFileNameLabel = new Label("Kein Video ausgewählt");
        currentFileNameLabel.setTextFill(Paint.valueOf(C_FG));

        currentPassLabel = new Label("");
        currentPassLabel.setTextFill(Paint.valueOf(C_COMMENT));
        //currentPassLabel.setVisible(false);

        progressBar = new ProgressBar(0);
        //progressBar.setVisible(false);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle(
            "-fx-accent: #bd93f9; " +
                "-fx-background-color: #44475a; " +
                "-fx-control-inner-background: #44475a;"
        );

        videoInfoPanel
            .getChildren()
            .addAll(currentFileNameLabel, currentPassLabel, progressBar);

        // ── Log Area ─────────────────────────────────────────────────────
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefHeight(200);
        logArea.setStyle(
            "-fx-control-inner-background: #21222c; " +
                "-fx-background-color: #21222c; " +
                "-fx-text-fill: #f8f8f2; " +
                "-fx-font-family: monospace; " +
                "-fx-font-size: 12px; " +
                "-fx-background-radius: 4; " +
                "-fx-control-inner-background-radius: 4;"
        );
        VBox.setVgrow(logArea, Priority.ALWAYS);

        // ── Assemble Root ────────────────────────────────────────────────
        root
            .getChildren()
            .addAll(progressCounterLabel, videoInfoPanel, logArea);

        // ── Engine ─────────────────────────────────────────────────────
        jobProcessor = new JobProcessor();
        preparedJobs = new ArrayList<>();
    }

    /**
     * Update the progress counter display.
     * @param current The 1-based index of the currently processing video.
     * @param total The total number of videos to process.
     */
    private void updateProgressCounter(int current, int total) {
        progressCounterLabel.setText(
            "Bearbeite Video " + current + " von " + total
        );
    }

    /**
     * Append a line to the log output.
     * @param message The message to append.
     */
    private void appendLog(String message) {
        logArea.appendText(message + "\n");
    }

    /**
     * Reset all UI elements to their initial idle state.
     */
    private void resetUI() {
        progressCounterLabel.setText("Bereit zum Starten");
        currentFileNameLabel.setText("Kein Video ausgewählt");
        currentPassLabel.setVisible(false);
        currentPassLabel.setText("");
        progressBar.setVisible(false);
        progressBar.setProgress(0);
        logArea.clear();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Progress listener
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Creates a {@link JobProgressListener} that marshalls all callbacks
     * back to the JavaFX Application Thread for safe UI updates.
     */
    private JobProgressListener createProgressListener() {
        return new JobProgressListener() {
            // ── Preparation phase ──────────────────────────────────────

            @Override
            public void onPreparationStarted(int totalFiles) {
                Platform.runLater(() -> {
                    updateProgressCounter(0, totalFiles);
                    progressBar.setVisible(true);
                    progressBar.setProgress(0);
                    appendLog("Vorbereitung gestartet...");
                });
            }

            @Override
            public void onPreparationProgress(
                int current,
                int total,
                String fileName,
                String message
            ) {
                Platform.runLater(() -> {
                    updateProgressCounter(current, total);
                    currentFileNameLabel.setText(fileName);
                    currentPassLabel.setText(message);
                    currentPassLabel.setVisible(true);
                    progressBar.setProgress((double) current / total);
                });
            }

            @Override
            public void onPreparationCompleted(int jobCount) {
                Platform.runLater(() -> {
                    updateProgressCounter(jobCount, jobCount);
                    currentFileNameLabel.setText(
                        "Vorbereitung abgeschlossen (" + jobCount + " Job(s))"
                    );
                    currentPassLabel.setVisible(false);
                    progressBar.setProgress(1.0);
                });
            }

            @Override
            public void onPreparationFailed(String errorMessage) {
                Platform.runLater(() -> {
                    appendLog("Vorbereitung fehlgeschlagen: " + errorMessage);
                    currentFileNameLabel.setText("Fehler");
                    currentPassLabel.setVisible(false);
                    progressBar.setVisible(false);
                });
            }

            // ── Processing phase ─────────────────────────────────────

            @Override
            public void onJobStarted(
                int index,
                int total,
                File sourceFile,
                File outputFile
            ) {
                Platform.runLater(() -> {
                    updateProgressCounter(index + 1, total);
                    currentFileNameLabel.setText(sourceFile.getName());
                    currentPassLabel.setText("Encodierung läuft...");
                    currentPassLabel.setTextFill(Paint.valueOf(C_COMMENT));
                    currentPassLabel.setVisible(true);
                    progressBar.setVisible(true);
                    progressBar.setProgress(0);
                    appendLog(
                        "[" +
                            (index + 1) +
                            "/" +
                            total +
                            "] Starte: " +
                            sourceFile.getName() +
                            " → " +
                            outputFile.getName()
                    );
                });
            }

            @Override
            public void onJobProgress(int index, VideoJobStatus status) {
                Platform.runLater(() -> {
                    // Update progress bar
                    double progress = status.getProgressPercent() / 100.0;
                    progressBar.setProgress(Math.min(progress, 1.0));

                    // Build detailed status line
                    String timeInfo =
                        formatDuration(status.getCurrentTimeMs()) +
                        " / " +
                        formatDuration(status.getTotalDurationMs());
                    String fpsInfo = String.format("%.1f fps", status.getFps());
                    String bitrateInfo = formatBitrate(status.getBitrateBps());
                    String sizeInfo = formatFileSize(
                        status.getOutputSizeBytes()
                    );

                    currentPassLabel.setText(
                        timeInfo +
                            " | " +
                            fpsInfo +
                            " | " +
                            bitrateInfo +
                            " | " +
                            sizeInfo
                    );
                    currentPassLabel.setVisible(true);
                });
            }

            @Override
            public void onJobFinished(int index, VideoJobStatus status) {
                Platform.runLater(() -> {
                    int jobNumber = index + 1;
                    String fileName = currentFileNameLabel.getText();
                    if (status.isSuccess()) {
                        appendLog("[" + jobNumber + "] ✓ Fertig: " + fileName);
                        currentPassLabel.setText("✓ Abgeschlossen");
                        currentPassLabel.setTextFill(Paint.valueOf("#50fa7b"));
                    } else {
                        appendLog(
                            "[" +
                                jobNumber +
                                "] ✗ Fehler: " +
                                fileName +
                                " – " +
                                status.getErrorMessage()
                        );
                        currentPassLabel.setText("✗ Fehlgeschlagen");
                        currentPassLabel.setTextFill(Paint.valueOf("#ff5555"));
                    }
                });
            }

            @Override
            public void onAllJobsCompleted(
                int totalCompleted,
                int totalFailed
            ) {
                Platform.runLater(() -> {
                    int total = totalCompleted + totalFailed;
                    updateProgressCounter(total, total);
                    currentFileNameLabel.setText("Alle Jobs abgeschlossen");
                    currentPassLabel.setText(
                        totalCompleted +
                            " erfolgreich, " +
                            totalFailed +
                            " fehlgeschlagen"
                    );
                    currentPassLabel.setVisible(true);
                    currentPassLabel.setTextFill(Paint.valueOf(C_FG));
                    progressBar.setProgress(1.0);

                    appendLog("════════════════════════════════════════");
                    appendLog(
                        "Ergebnis: " +
                            totalCompleted +
                            " erfolgreich, " +
                            totalFailed +
                            " fehlgeschlagen"
                    );
                    appendLog("════════════════════════════════════════");
                });
            }

            // ── Logging ───────────────────────────────────────────────

            @Override
            public void onLog(String message) {
                Platform.runLater(() -> appendLog(message));
            }
        };
    }

    // ─────────────────────────────────────────────────────────────────────
    //  StepView interface
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public Node getNode() {
        return root;
    }

    @Override
    public void activate(WizardState state) {
        var backButton = state.getBackButton();
        var centerButton = state.getCenterButton();
        var nextButton = state.getNextButton();

        resetUI();
        centerButton.setText("Starte Bearbeitung");
        centerButton.setVisible(true);
        centerButton.setDisable(false);

        centerButton.setOnAction(_ -> {
            backButton.setVisible(false);

            // Prevent double-clicking
            centerButton.setDisable(true);
            centerButton.setText("Bearbeitung läuft...");

            // Get data from state
            var files = state.getImportedFiles();
            var preset = state.getSelectedPreset();

            if (files.isEmpty()) {
                appendLog("FEHLER: Keine Dateien importiert!");
                centerButton.setDisable(false);
                centerButton.setText("Starte Bearbeitung");
                return;
            }

            if (preset == null) {
                appendLog("FEHLER: Kein Preset ausgewählt!");
                centerButton.setDisable(false);
                centerButton.setText("Starte Bearbeitung");
                return;
            }

            // Create listener that marshalls callbacks to JavaFX thread
            JobProgressListener listener = createProgressListener();

            jobProcessor
                .prepareJobs(files, preset, listener)
                .thenAccept(jobs -> {
                    Platform.runLater(() -> {
                        preparedJobs = jobs;
                        appendLog(
                            "Vorbereitung abgeschlossen. " +
                                jobs.size() +
                                " Job(s) bereit. Starte Encodierung..."
                        );
                        startEncoding(preparedJobs, centerButton, backButton);
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        appendLog("Fehler: " + ex.getMessage());
                        centerButton.setDisable(false);
                        centerButton.setText("Starte Bearbeitung");
                    });
                    return null;
                });
        });

        backButton.setVisible(true);
        nextButton.setVisible(false);
    }

    /**
     * Starts the encoding phase for the prepared jobs.
     */
    private void startEncoding(
        List<VideoJob> jobs,
        javafx.scene.control.Button centerButton,
        javafx.scene.control.Button backButton
    ) {
        if (jobs == null || jobs.isEmpty()) {
            appendLog("FEHLER: Keine vorbereiteten Jobs gefunden!");
            centerButton.setDisable(false);
            centerButton.setText("Starte Bearbeitung");
            return;
        }

        appendLog("--- Starte Encodierung ---");

        JobProgressListener listener = createProgressListener();

        jobProcessor
            .executeJobs(jobs, listener)
            .whenComplete((_, ex) -> {
                Platform.runLater(() -> {
                    if (ex != null) {
                        appendLog(
                            "FEHLER bei der Verarbeitung: " + ex.getMessage()
                        );
                    }
                    centerButton.setVisible(false);
                    centerButton.setText("Fertig");
                    backButton.setVisible(true);
                });
            });
    }

    /**
     * Format milliseconds to a human-readable duration string.
     */
    private String formatDuration(long millis) {
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d:%02d", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Format bitrate in bps to a human-readable string.
     */
    private String formatBitrate(long bps) {
        if (bps >= 1_000_000) {
            return String.format("%.1f Mbps", bps / 1_000_000.0);
        } else if (bps >= 1_000) {
            return String.format("%.0f kbps", bps / 1_000.0);
        }
        return String.format("%d bps", bps);
    }

    /**
     * Format bytes to a human-readable file size string.
     */
    private String formatFileSize(long bytes) {
        if (bytes >= 1_073_741_824) {
            return String.format("%.1f GB", bytes / 1_073_741_824.0);
        } else if (bytes >= 1_048_576) {
            return String.format("%.1f MB", bytes / 1_048_576.0);
        } else if (bytes >= 1_024) {
            return String.format("%.0f kB", bytes / 1_024.0);
        }
        return String.format("%d B", bytes);
    }

    @Override
    public void deactivate(WizardState state) {
        preparedJobs = null;
        state.getCenterButton().setDisable(false);
        state.getCenterButton().setText("");
    }
}
