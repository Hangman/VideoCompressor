package de.pottgames.videocompressor.view.step;

import atlantafx.base.theme.Styles;
import de.pottgames.videocompressor.WizardState;
import de.pottgames.videocompressor.engine.Ffprobe;
import de.pottgames.videocompressor.engine.ProbeInfo;
import de.pottgames.videocompressor.engine.VideoJob;
import de.pottgames.videocompressor.engine.VideoJobStatus;
import de.pottgames.videocompressor.engine.VideoJobStatus.Status;
import de.pottgames.videocompressor.view.StepView;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * Step 4 of the video compressor wizard: displays a side-by-side comparison
 * of the imported (source) video files and the exported (output) video files.
 *
 * <p>On activation, this view asynchronously probes all source and output files
 * via ffprobe and presents the results in a structured comparison layout.</p>
 */
public class Step4View implements StepView {

    // ── Color constants (Dracula palette) ────────────────────────────────

    private static final String C_BG = "#282a36";
    private static final String C_DARK_BG = "#1e1f29";
    private static final String C_COMMENT = "#6272a4";
    private static final String C_FG = "#f8f8f2";
    private static final String C_GREEN = "#50fa7b";
    private static final String C_RED = "#ff5555";
    private static final String C_YELLOW = "#f1fa8c";
    private static final String C_CYAN = "#8be9fd";
    private static final String C_PURPLE = "#bd93f9";
    private static final String C_ACCENT = "#9580ff";

    // ── Layout ───────────────────────────────────────────────────────────

    private final VBox root;

    // ── Loading indicator ────────────────────────────────────────────────

    private final VBox loadingPanel;
    private final ProgressIndicator progressIndicator;
    private final Label loadingLabel;

    // ── Results content ──────────────────────────────────────────────────

    private final ScrollPane scrollPane;
    private final VBox resultsContent;
    private final Label summaryLabel;

    public Step4View() {
        root = new VBox();
        root.setPadding(new Insets(20));
        root.setSpacing(16);

        // ── Loading panel ────────────────────────────────────────────────

        progressIndicator = new ProgressIndicator(0);
        progressIndicator.setPrefSize(48, 48);

        loadingLabel = new Label("Ergebnisse werden geladen...");
        loadingLabel.setStyle(
            "-fx-text-fill: " + C_FG + "; -fx-font-size: 16px;"
        );
        loadingLabel.getStyleClass().addAll(Styles.TEXT_BOLD);

        loadingPanel = new VBox(16, progressIndicator, loadingLabel);
        loadingPanel.setAlignment(Pos.CENTER);
        loadingPanel.setPrefHeight(300);
        loadingPanel.setVisible(false);

        // ── Results content ──────────────────────────────────────────────

        summaryLabel = new Label();
        summaryLabel.setStyle(
            "-fx-text-fill: " +
                C_CYAN +
                "; -fx-font-size: 14px; -fx-alignment: center;"
        );
        summaryLabel.getStyleClass().addAll(Styles.TEXT_BOLD);
        summaryLabel.setTextAlignment(TextAlignment.CENTER);

        resultsContent = new VBox(12);
        resultsContent.getChildren().add(summaryLabel);
        resultsContent.setPadding(new Insets(0, 4, 0, 4));

        scrollPane = new ScrollPane(resultsContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent;");
        scrollPane.setPadding(new Insets(0));
        scrollPane.setBackground(Background.EMPTY);

        // ── Root layout ──────────────────────────────────────────────────

        root.getChildren().addAll(loadingPanel, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
    }

    @Override
    public void activate(WizardState state) {
        var backButton = state.getBackButton();
        var centerButton = state.getCenterButton();
        var nextButton = state.getNextButton();

        backButton.setVisible(true);
        centerButton.setVisible(false);
        nextButton.setVisible(false);
        nextButton.setDisable(true);

        // Show loading panel, hide results
        loadingPanel.setVisible(true);
        scrollPane.setVisible(false);
        resultsContent.getChildren().clear();

        List<VideoJob> jobs = state.getPreparedJobs();
        if (jobs == null || jobs.isEmpty()) {
            // No jobs to display
            Platform.runLater(() -> {
                loadingPanel.setVisible(false);
                scrollPane.setVisible(true);
                Label noDataLabel = new Label(
                    "Keine Ergebnisse verfügbar. Bitte bearbeiten Sie zuerst Videos in Schritt 3."
                );
                noDataLabel.setStyle(
                    "-fx-text-fill: " + C_YELLOW + "; -fx-font-size: 14px;"
                );
                noDataLabel.setWrapText(true);
                resultsContent.getChildren().add(noDataLabel);
            });
            return;
        }

        // Probe output files sequentially to avoid parallel I/O load.
        // Each probe waits for the previous one to finish.
        // Results are collected in a list; null is stored on failure.
        List<ProbeInfo> outputProbes = new ArrayList<>(
            Collections.nCopies(jobs.size(), null)
        );
        final int totalFiles = jobs.size();

        // Start with a completed future holding null (no previous result)
        CompletableFuture<ProbeInfo> chain = CompletableFuture.completedFuture(
            null
        );

        for (int i = 0; i < jobs.size(); i++) {
            final int index = i;
            final VideoJob job = jobs.get(i);

            // Chain: wait for previous probe, then probe this file
            chain = chain
                .thenCompose(ignored -> {
                    File outputFile = job.outputFile();
                    if (outputFile != null && outputFile.exists()) {
                        return Ffprobe.probeAsync(outputFile);
                    }
                    // File doesn't exist - return null so the chain continues
                    return CompletableFuture.completedFuture((ProbeInfo) null);
                })
                .handle((info, ex) -> {
                    // handle() catches exceptions from probeAsync and always
                    // returns a value, so the sequential chain never breaks.
                    if (ex != null) {
                        outputProbes.set(index, null);
                    } else {
                        outputProbes.set(index, info);
                    }

                    // Update progress UI after each sequential probe
                    double progress = (double) (index + 1) / totalFiles;
                    Platform.runLater(() -> {
                        progressIndicator.setProgress(progress);
                        loadingLabel.setText(
                            "Ergebnisse werden geladen... (" +
                                (index + 1) +
                                "/" +
                                totalFiles +
                                ")"
                        );
                    });

                    // Always return info (or null) so the chain continues
                    return info;
                });
        }

        // When the entire sequential chain is done, build the comparison UI
        chain.whenComplete((ignored, ex) -> {
            Platform.runLater(() -> {
                buildComparisonUI(state, jobs, outputProbes);
                loadingPanel.setVisible(false);
                scrollPane.setVisible(true);
            });
        });
    }

    /**
     * Builds the comparison UI with all probe results.
     * The outputProbes list contains resolved ProbeInfo objects or null on failure.
     */
    private void buildComparisonUI(
        WizardState state,
        List<VideoJob> jobs,
        List<ProbeInfo> outputProbes
    ) {
        resultsContent.getChildren().clear();

        // Count success/fail
        int successCount = 0;
        int failedCount = 0;
        int skippedCount = 0;

        for (VideoJob job : jobs) {
            Status status = job.status().getStatus();
            if (status == Status.COMPLETED) {
                successCount++;
            } else if (status == Status.FAILED) {
                failedCount++;
            } else {
                skippedCount++;
            }
        }

        // Summary label
        summaryLabel.setText(
            "Zusammenfassung: " +
                jobs.size() +
                " Datei(en) | " +
                successCount +
                " erfolgreich | " +
                failedCount +
                " fehlgeschlagen | " +
                skippedCount +
                " übersprungen"
        );
        resultsContent.getChildren().add(summaryLabel);

        // Separator
        Pane separator = new Pane();
        separator.setPrefHeight(1);
        separator.setStyle("-fx-background-color: " + C_COMMENT + ";");
        HBox.setHgrow(separator, Priority.ALWAYS);
        resultsContent.getChildren().add(separator);

        // Build comparison card for each job
        for (int i = 0; i < jobs.size(); i++) {
            VideoJob job = jobs.get(i);
            ProbeInfo sourceInfo = job.sourceInfo();

            // Directly use the resolved probe info (null indicates failure)
            ProbeInfo outputInfo = outputProbes.get(i);
            String outputProbeError =
                outputInfo == null
                    ? "Konnte Ausgabedatei nicht analysieren"
                    : null;

            VBox card = createComparisonCard(
                i + 1,
                sourceInfo,
                outputInfo,
                outputProbeError,
                job.status()
            );
            resultsContent.getChildren().add(card);
        }
    }

    /**
     * Creates a comparison card for a single source/output file pair.
     */
    private VBox createComparisonCard(
        int jobNumber,
        ProbeInfo sourceInfo,
        ProbeInfo outputInfo,
        String outputProbeError,
        VideoJobStatus status
    ) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle(
            "-fx-background-color: " + C_DARK_BG + "; -fx-background-radius: 8;"
        );

        // ── Card header with file names and status ───────────────────────

        HBox headerBox = new HBox(12);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        // Job number badge
        Label numberBadge = new Label(String.valueOf(jobNumber));
        numberBadge.setStyle(
            "-fx-background-color: " +
                C_ACCENT +
                "; -fx-text-fill: " +
                C_BG +
                "; -fx-background-radius: 12; -fx-padding: 4 10; -fx-font-size: 12px;"
        );
        numberBadge.getStyleClass().addAll(Styles.TEXT_BOLD);

        // File name mapping
        Label fileNameLabel = new Label();
        String sourceName =
            sourceInfo != null ? sourceInfo.file().getName() : "Unknown";
        String outputName =
            outputInfo != null ? outputInfo.file().getName() : "Unknown";
        fileNameLabel.setText(sourceName + "  →  " + outputName);
        fileNameLabel.setStyle(
            "-fx-text-fill: " + C_FG + "; -fx-font-size: 13px;"
        );
        fileNameLabel.getStyleClass().addAll(Styles.TEXT_BOLD);

        // Status indicator
        Label statusIndicator = new Label();
        if (status.getStatus() == Status.COMPLETED) {
            statusIndicator.setText("✓");
            statusIndicator.setStyle(
                "-fx-text-fill: " + C_GREEN + "; -fx-font-size: 16px;"
            );
        } else if (status.getStatus() == Status.FAILED) {
            statusIndicator.setText("✗");
            statusIndicator.setStyle(
                "-fx-text-fill: " + C_RED + "; -fx-font-size: 16px;"
            );
        } else {
            statusIndicator.setText("—");
            statusIndicator.setStyle(
                "-fx-text-fill: " + C_YELLOW + "; -fx-font-size: 16px;"
            );
        }

        HBox.setHgrow(fileNameLabel, Priority.ALWAYS);
        headerBox
            .getChildren()
            .addAll(numberBadge, fileNameLabel, statusIndicator);
        card.getChildren().add(headerBox);

        // ── Error message if output probing failed ───────────────────────

        if (outputProbeError != null) {
            Label errorLabel = new Label(
                "Fehler beim Analysieren der Ausgabedatei: " + outputProbeError
            );
            errorLabel.setStyle(
                "-fx-text-fill: " + C_RED + "; -fx-font-size: 12px;"
            );
            errorLabel.setWrapText(true);
            card.getChildren().add(errorLabel);

            // Show source info only
            if (sourceInfo != null) {
                GridPane sourceOnlyGrid = createComparisonGrid(
                    "Quelldatei",
                    sourceInfo,
                    null
                );
                card.getChildren().add(sourceOnlyGrid);
            }
            return card;
        }

        // ── Comparison grid ──────────────────────────────────────────────

        if (sourceInfo != null && outputInfo != null) {
            GridPane grid = createComparisonGrid(
                "Vergleich",
                sourceInfo,
                outputInfo
            );
            card.getChildren().add(grid);
        }

        // ── File size comparison with savings ────────────────────────────

        if (sourceInfo != null && outputInfo != null) {
            long sourceSize = sourceInfo.fileSize();
            long outputSize = outputInfo.fileSize();

            if (sourceSize > 0) {
                double savingsPercent =
                    ((sourceSize - outputSize) / (double) sourceSize) * 100.0;
                String savingsText;
                String savingsColor;

                if (savingsPercent > 0) {
                    savingsText = String.format(
                        "Ersparnis: %.1f%%",
                        savingsPercent
                    );
                    savingsColor = C_GREEN;
                } else if (savingsPercent < 0) {
                    savingsText = String.format(
                        "Größer um: %.1f%%",
                        Math.abs(savingsPercent)
                    );
                    savingsColor = C_RED;
                } else {
                    savingsText = "Keine Änderung";
                    savingsColor = C_YELLOW;
                }

                Label savingsLabel = new Label(savingsText);
                savingsLabel.setStyle(
                    "-fx-text-fill: " + savingsColor + "; -fx-font-size: 12px;"
                );
                savingsLabel.getStyleClass().addAll(Styles.TEXT_BOLD);
                card.getChildren().add(savingsLabel);
            }
        }

        return card;
    }

    /**
     * Creates a GridPane comparing source and output ProbeInfo values.
     */
    private GridPane createComparisonGrid(
        String title,
        ProbeInfo sourceInfo,
        ProbeInfo outputInfo
    ) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(4);

        // Column headers
        Label titleLabel = new Label(title);
        titleLabel.setStyle(
            "-fx-text-fill: " +
                C_PURPLE +
                "; -fx-font-size: 13px; -fx-font-weight: bold;"
        );
        titleLabel.setAlignment(Pos.CENTER_LEFT);
        grid.add(titleLabel, 0, 0);

        Label sourceHeader = new Label("Quelle");
        sourceHeader.setStyle(
            "-fx-text-fill: " + C_COMMENT + "; -fx-font-size: 11px;"
        );
        sourceHeader.setAlignment(Pos.CENTER);
        grid.add(sourceHeader, 1, 0);

        Label outputHeader = new Label("Ausgabe");
        outputHeader.setStyle(
            "-fx-text-fill: " + C_COMMENT + "; -fx-font-size: 11px;"
        );
        outputHeader.setAlignment(Pos.CENTER);
        grid.add(outputHeader, 2, 0);

        // Separator line
        for (int col = 0; col < 3; col++) {
            Pane sep = new Pane();
            sep.setPrefHeight(1);
            sep.setStyle("-fx-background-color: " + C_COMMENT + ";");
            grid.add(sep, col, 1);
        }

        int row = 2;

        // ── Video Codec ──────────────────────────────────────────────────
        row = addComparisonRow(
            grid,
            row,
            "Video-Codec",
            sourceInfo != null ? sourceInfo.codec() : "—",
            outputInfo != null ? outputInfo.codec() : "—"
        );

        // ── Resolution ───────────────────────────────────────────────────
        row = addComparisonRow(
            grid,
            row,
            "Auflösung",
            sourceInfo != null
                ? sourceInfo.resolutionWidth() +
                  "×" +
                  sourceInfo.resolutionHeight()
                : "—",
            outputInfo != null
                ? outputInfo.resolutionWidth() +
                  "×" +
                  outputInfo.resolutionHeight()
                : "—"
        );

        // ── FPS ──────────────────────────────────────────────────────────
        row = addComparisonRow(
            grid,
            row,
            "FPS",
            sourceInfo != null ? String.valueOf(sourceInfo.fps()) : "—",
            outputInfo != null ? String.valueOf(outputInfo.fps()) : "—"
        );

        // ── Bitrate ──────────────────────────────────────────────────────
        row = addComparisonRow(
            grid,
            row,
            "Bitrate",
            sourceInfo != null ? formatBitrate(sourceInfo.bitrate()) : "—",
            outputInfo != null ? formatBitrate(outputInfo.bitrate()) : "—"
        );

        // ── Audio Bitrate ────────────────────────────────────────────────
        row = addComparisonRow(
            grid,
            row,
            "Audio-Bitrate",
            sourceInfo != null ? formatBitrate(sourceInfo.audioBitrate()) : "—",
            outputInfo != null ? formatBitrate(outputInfo.audioBitrate()) : "—"
        );

        // ── Duration ─────────────────────────────────────────────────────
        row = addComparisonRow(
            grid,
            row,
            "Dauer",
            sourceInfo != null ? sourceInfo.formatDuration() : "—",
            outputInfo != null ? outputInfo.formatDuration() : "—"
        );

        // ── File Size ────────────────────────────────────────────────────
        row = addComparisonRow(
            grid,
            row,
            "Dateigröße",
            sourceInfo != null ? formatFileSize(sourceInfo.fileSize()) : "—",
            outputInfo != null ? formatFileSize(outputInfo.fileSize()) : "—"
        );

        return grid;
    }

    /**
     * Adds a single comparison row to the grid.
     * Returns the next available row index.
     */
    private int addComparisonRow(
        GridPane grid,
        int row,
        String property,
        String sourceValue,
        String outputValue
    ) {
        // Property label (left column)
        Label propLabel = new Label(property);
        propLabel.setStyle(
            "-fx-text-fill: " + C_COMMENT + "; -fx-font-size: 12px;"
        );
        propLabel.setAlignment(Pos.CENTER_LEFT);
        grid.add(propLabel, 0, row);

        // Source value (middle column)
        Label sourceLabel = new Label(sourceValue);
        sourceLabel.setStyle(
            "-fx-text-fill: " + C_FG + "; -fx-font-size: 12px;"
        );
        sourceLabel.setAlignment(Pos.CENTER);
        grid.add(sourceLabel, 1, row);

        // Output value (right column)
        Label outputLabel = new Label(outputValue);
        outputLabel.setStyle(
            "-fx-text-fill: " + C_CYAN + "; -fx-font-size: 12px;"
        );
        outputLabel.setAlignment(Pos.CENTER);
        grid.add(outputLabel, 2, row);

        return row + 1;
    }

    // ── Formatting helpers ──────────────────────────────────────────────

    /**
     * Format bitrate in bps to a human-readable string.
     */
    private String formatBitrate(int bps) {
        if (bps <= 0) {
            return "—";
        }
        if (bps >= 1_000_000) {
            return String.format("%.1f Mbps", bps / 1_000_000.0);
        } else if (bps >= 1_000) {
            return String.format("%d kbps", bps / 1_000);
        }
        return String.format("%d bps", bps);
    }

    /**
     * Format bytes to a human-readable file size string.
     */
    private String formatFileSize(long bytes) {
        if (bytes <= 0) {
            return "—";
        }
        if (bytes >= 1_073_741_824) {
            return String.format("%.2f GB", bytes / 1_073_741_824.0);
        } else if (bytes >= 1_048_576) {
            return String.format("%.1f MB", bytes / 1_048_576.0);
        } else if (bytes >= 1_024) {
            return String.format("%.0f kB", bytes / 1024.0);
        }
        return String.format("%d B", bytes);
    }

    @Override
    public Node getNode() {
        return root;
    }

    @Override
    public void deactivate(WizardState state) {
        resultsContent.getChildren().clear();
        loadingPanel.setVisible(false);
        scrollPane.setVisible(false);
    }
}
