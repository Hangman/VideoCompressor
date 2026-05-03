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
import javafx.scene.control.ScrollPane;
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
 * via ffprobe and presents the results in a modern before/after card layout.</p>
 */
public class Step4View implements StepView {

    // ── Color constants (CSS theme variables) ────────────────────────────

    private static final String C_COMMENT = "-color-fg-subtle";
    private static final String C_FG = "-color-fg-default";

    // Dracula palette
    private static final String HEX_BG = "#282a36";
    private static final String HEX_COMMENT = "#6272a4";
    private static final String HEX_ACCENT = "#9580ff";
    private static final String HEX_CYAN = "#8be9fd";
    private static final String HEX_GREEN = "#50fa7b";
    private static final String HEX_RED = "#ff5555";
    private static final String HEX_ORANGE = "#ffb86c";
    private static final String HEX_CARD_BG = "#2f3143";
    private static final String HEX_PANEL_SOURCE = "#24263a";
    private static final String HEX_PANEL_OUTPUT = "#1d2338";
    private static final String HEX_BORDER = "#3b3d57";
    private static final String HEX_DIVIDER = "#3a3c52";

    // ── Layout ───────────────────────────────────────────────────────────

    private final VBox root;

    // ── Results content ──────────────────────────────────────────────────

    private final ScrollPane scrollPane;
    private final VBox resultsContent;
    private final Label summaryLabel;

    public Step4View() {
        root = new VBox();
        root.setPadding(new Insets(24));
        root.setSpacing(20);

        summaryLabel = new Label();
        summaryLabel.getStyleClass().addAll(Styles.TITLE_4, Styles.ACCENT);
        summaryLabel.setTextAlignment(TextAlignment.CENTER);
        summaryLabel.setPadding(new Insets(0, 0, 4, 0));

        resultsContent = new VBox(16);
        resultsContent.setPadding(new Insets(0, 4, 0, 4));

        scrollPane = new ScrollPane(resultsContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setPadding(new Insets(0));
        scrollPane.setStyle("-fx-background: transparent;");

        root.getChildren().addAll(summaryLabel, scrollPane);
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

        resultsContent.getChildren().clear();
        summaryLabel.setText("Ergebnisse werden geladen...");

        List<VideoJob> jobs = state.getPreparedJobs();
        if (jobs == null || jobs.isEmpty()) {
            Platform.runLater(() -> {
                summaryLabel.setText(
                    "Keine Ergebnisse verfügbar. Bitte bearbeiten Sie zuerst Videos in Schritt 3."
                );
            });
            return;
        }

        final int totalFiles = jobs.size();

        List<ProbeInfo> outputProbes = new ArrayList<>(
            Collections.nCopies(jobs.size(), null)
        );

        CompletableFuture<ProbeInfo> chain = CompletableFuture.completedFuture(
            null
        );

        for (int i = 0; i < jobs.size(); i++) {
            final int index = i;
            final VideoJob job = jobs.get(i);

            chain = chain
                .thenCompose(ignored -> {
                    File outputFile = job.outputFile();
                    if (outputFile != null && outputFile.exists()) {
                        return Ffprobe.probeAsync(outputFile);
                    }
                    return CompletableFuture.completedFuture((ProbeInfo) null);
                })
                .handle((info, ex) -> {
                    if (ex != null) {
                        outputProbes.set(index, null);
                    } else {
                        outputProbes.set(index, info);
                    }

                    Platform.runLater(() -> {
                        summaryLabel.setText(
                            "Ergebnisse werden geladen... (" +
                                (index + 1) +
                                "/" +
                                totalFiles +
                                ")"
                        );
                    });

                    return info;
                });
        }

        chain.whenComplete((ignored, ex) -> {
            Platform.runLater(() -> {
                buildComparisonUI(state, jobs, outputProbes);
            });
        });
    }

    // ── UI Building ──────────────────────────────────────────────────────

    private void buildComparisonUI(
        WizardState state,
        List<VideoJob> jobs,
        List<ProbeInfo> outputProbes
    ) {
        resultsContent.getChildren().clear();

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

        int numJobs = jobs.size();
        summaryLabel.setText(
            "Zusammenfassung: " +
                numJobs +
                (numJobs > 1 ? " Dateien" : " Datei") +
                "  ·  " +
                successCount +
                " erfolgreich" +
                (failedCount > 0
                    ? "  ·  " + failedCount + " fehlgeschlagen"
                    : "") +
                (skippedCount > 0
                    ? "  ·  " + skippedCount + " übersprungen"
                    : "")
        );

        // Decorative separator under summary
        Pane summarySep = new Pane();
        summarySep.setPrefHeight(1);
        summarySep.setStyle("-fx-background-color: " + HEX_DIVIDER + ";");
        HBox.setHgrow(summarySep, Priority.ALWAYS);
        resultsContent.getChildren().add(summarySep);

        // Build comparison card for each job
        for (int i = 0; i < jobs.size(); i++) {
            VideoJob job = jobs.get(i);
            ProbeInfo sourceInfo = job.sourceInfo();
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

    // ── Card Factory ─────────────────────────────────────────────────────

    private VBox createComparisonCard(
        int jobNumber,
        ProbeInfo sourceInfo,
        ProbeInfo outputInfo,
        String outputProbeError,
        VideoJobStatus status
    ) {
        VBox card = new VBox(16);
        card.setPadding(new Insets(20));
        card.setStyle(
            "-fx-background-color: " +
                HEX_CARD_BG +
                ";" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: " +
                HEX_BORDER +
                ";" +
                "-fx-border-radius: 14;" +
                "-fx-border-width: 1;"
        );

        // ── Header: badge + filename + status pill ──────────────────────
        HBox headerBox = buildHeader(jobNumber, sourceInfo, outputInfo, status);
        card.getChildren().add(headerBox);

        // ── Error fallback ──────────────────────────────────────────────
        if (outputProbeError != null) {
            Label errorLabel = new Label("⚠  " + outputProbeError);
            errorLabel.setStyle(
                "-fx-text-fill: " +
                    HEX_ORANGE +
                    ";" +
                    "-fx-font-size: 13px;" +
                    "-fx-padding: 8 12;"
            );
            errorLabel.setWrapText(true);
            card.getChildren().add(errorLabel);

            if (sourceInfo != null) {
                VBox sourcePanel = buildSinglePanel(
                    "Quelldatei",
                    sourceInfo,
                    null
                );
                card.getChildren().add(sourcePanel);
            }
            return card;
        }

        // ── Savings banner (prominent) ──────────────────────────────────
        if (sourceInfo != null && outputInfo != null) {
            long srcSize = sourceInfo.fileSize();
            long outSize = outputInfo.fileSize();
            if (srcSize > 0) {
                double pct = ((srcSize - outSize) / (double) srcSize) * 100.0;
                HBox savingsBanner = buildSavingsBanner(pct, srcSize, outSize);
                card.getChildren().add(savingsBanner);
            }
        }

        // ── Two-panel comparison ────────────────────────────────────────
        if (sourceInfo != null && outputInfo != null) {
            HBox panels = buildComparisonPanels(sourceInfo, outputInfo);
            card.getChildren().add(panels);
        }

        return card;
    }

    // ── Header ───────────────────────────────────────────────────────────

    private HBox buildHeader(
        int jobNumber,
        ProbeInfo src,
        ProbeInfo out,
        VideoJobStatus status
    ) {
        HBox box = new HBox(14);
        box.setAlignment(Pos.CENTER_LEFT);

        // Number circle
        Label badge = new Label(String.valueOf(jobNumber));
        badge.setStyle(
            "-fx-background-color: " +
                HEX_ACCENT +
                ";" +
                "-fx-text-fill: " +
                HEX_BG +
                ";" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 5 9;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;"
        );

        // File names
        Label nameLabel = new Label();
        String srcName = src != null ? src.file().getName() : "Unbekannt";
        String outName = out != null ? out.file().getName() : "Unbekannt";
        nameLabel.setText(srcName + "  →  " + outName);
        nameLabel.setStyle("-fx-text-fill: " + C_FG + "; -fx-font-size: 14px;");
        nameLabel.getStyleClass().add(Styles.TEXT_BOLD);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        // Status pill
        Label statusPill = new Label();
        if (status.getStatus() == Status.COMPLETED) {
            statusPill.setText("●  Fertig");
            statusPill.setStyle(
                "-fx-text-fill: " +
                    HEX_GREEN +
                    ";" +
                    "-fx-background-color: rgba(80,250,123,0.1);" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 4 10;" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: bold;"
            );
        } else if (status.getStatus() == Status.FAILED) {
            statusPill.setText("●  Fehlgeschlagen");
            statusPill.setStyle(
                "-fx-text-fill: " +
                    HEX_RED +
                    ";" +
                    "-fx-background-color: rgba(255,85,85,0.1);" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 4 10;" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: bold;"
            );
        } else {
            statusPill.setText("●  Übersprungen");
            statusPill.setStyle(
                "-fx-text-fill: " +
                    HEX_ORANGE +
                    ";" +
                    "-fx-background-color: rgba(255,184,108,0.1);" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 4 10;" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: bold;"
            );
        }

        box.getChildren().addAll(badge, nameLabel, statusPill);
        return box;
    }

    // ── Savings Banner ───────────────────────────────────────────────────

    private HBox buildSavingsBanner(double pct, long srcSize, long outSize) {
        HBox banner = new HBox(12);
        banner.setAlignment(Pos.CENTER);
        banner.setPadding(new Insets(10, 14, 10, 14));

        String bgColor;
        String textColor;
        String icon;
        String label;

        if (pct > 0.5) {
            bgColor = "rgba(80,250,123,0.08)";
            textColor = HEX_GREEN;
            icon = "↓";
            label = "Ersparnis";
        } else if (pct < -0.5) {
            bgColor = "rgba(255,85,85,0.08)";
            textColor = HEX_RED;
            icon = "↑";
            label = "Größer";
        } else {
            bgColor = "rgba(98,114,164,0.08)";
            textColor = HEX_COMMENT;
            icon = "—";
            label = "Unverändert";
        }

        banner.setStyle(
            "-fx-background-color: " +
                bgColor +
                ";" +
                "-fx-background-radius: 10;"
        );

        // Icon
        Label iconLabel = new Label(icon);
        iconLabel.setStyle(
            "-fx-text-fill: " +
                textColor +
                "; -fx-font-size: 18px; -fx-font-weight: bold;"
        );

        // Main percentage
        Label pctLabel = new Label(
            String.format("%s: %.1f%%", label, Math.abs(pct))
        );
        pctLabel.setStyle(
            "-fx-text-fill: " + textColor + "; -fx-font-size: 14px;"
        );
        pctLabel.getStyleClass().add(Styles.TEXT_BOLD);

        // Sizes
        Label sizeLabel = new Label(
            formatFileSize(srcSize) + "  →  " + formatFileSize(outSize)
        );
        sizeLabel.setStyle(
            "-fx-text-fill: " + C_COMMENT + "; -fx-font-size: 12px;"
        );

        banner.getChildren().addAll(iconLabel, pctLabel, sizeLabel);
        HBox.setHgrow(pctLabel, Priority.ALWAYS);

        return banner;
    }

    // ── Two-Panel Comparison ─────────────────────────────────────────────

    private HBox buildComparisonPanels(ProbeInfo source, ProbeInfo output) {
        HBox panels = new HBox(12);
        panels.setAlignment(Pos.CENTER_LEFT);

        VBox leftPanel = buildSinglePanel("Quelle", source, output);
        VBox rightPanel = buildSinglePanel("Ausgabe", output, source);

        panels.getChildren().addAll(leftPanel, rightPanel);
        HBox.setHgrow(leftPanel, Priority.ALWAYS);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        return panels;
    }

    private VBox buildSinglePanel(
        String title,
        ProbeInfo info,
        ProbeInfo other
    ) {
        VBox panel = new VBox(0);
        panel.setPadding(new Insets(16));
        panel.setStyle(
            "-fx-background-color: " +
                (title.equals("Quelle") ? HEX_PANEL_SOURCE : HEX_PANEL_OUTPUT) +
                ";" +
                "-fx-background-radius: 10;"
        );
        HBox.setHgrow(panel, Priority.ALWAYS);

        // Panel title
        Label titleLabel = new Label(title.toUpperCase());
        titleLabel.setStyle(
            "-fx-text-fill: " +
                HEX_COMMENT +
                ";" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;" +
                "-fx-letter-spacing: 1.5;" +
                "-fx-padding: 0 0 12 0;"
        );
        panel.getChildren().add(titleLabel);

        // Divider under title
        Pane div = new Pane();
        div.setPrefHeight(1);
        div.setStyle("-fx-background-color: " + HEX_DIVIDER + ";");
        panel.getChildren().add(div);

        VBox props = new VBox(10);
        props.setPadding(new Insets(12, 0, 0, 0));

        // ── Resolution (hero value) ────────────────────────────────────
        props
            .getChildren()
            .add(
                buildHeroProperty(
                    "Auflösung",
                    info.getAbbreviatedResolution(),
                    title.equals("Ausgabe") ? HEX_CYAN : C_FG
                )
            );

        // Divider
        Pane d1 = new Pane();
        d1.setPrefHeight(1);
        d1.setStyle("-fx-background-color: " + HEX_DIVIDER + ";");
        props.getChildren().add(d1);

        // ── Codec ──────────────────────────────────────────────────────
        props
            .getChildren()
            .add(buildPropertyRow("Codec", info.codec(), other, "codec"));

        // ── FPS ────────────────────────────────────────────────────────
        props
            .getChildren()
            .add(
                buildPropertyRow(
                    "FPS",
                    String.valueOf(info.fps()),
                    other,
                    "fps"
                )
            );

        // ── Bitrate ────────────────────────────────────────────────────
        props
            .getChildren()
            .add(
                buildPropertyRow(
                    "Bitrate",
                    formatBitrate(info.bitrate()),
                    other,
                    "bitrate"
                )
            );

        // ── Audio Bitrate ──────────────────────────────────────────────
        props
            .getChildren()
            .add(
                buildPropertyRow(
                    "Audio-Bitrate",
                    formatBitrate(info.audioBitrate()),
                    other,
                    "audioBitrate"
                )
            );

        // ── Duration ───────────────────────────────────────────────────
        props
            .getChildren()
            .add(
                buildPropertyRow(
                    "Dauer",
                    info.formatDuration(),
                    other,
                    "duration"
                )
            );

        // ── File Size (hero value) ─────────────────────────────────────
        Pane d2 = new Pane();
        d2.setPrefHeight(1);
        d2.setStyle("-fx-background-color: " + HEX_DIVIDER + ";");
        props.getChildren().add(d2);

        props
            .getChildren()
            .add(
                buildHeroProperty(
                    "Dateigröße",
                    formatFileSize(info.fileSize()),
                    title.equals("Ausgabe") ? HEX_CYAN : C_FG
                )
            );

        panel.getChildren().add(props);
        return panel;
    }

    // ── Property Row Builders ────────────────────────────────────────────

    private VBox buildHeroProperty(String label, String value, String color) {
        VBox box = new VBox(3);

        Label lbl = new Label(label);
        lbl.setStyle(
            "-fx-text-fill: " +
                HEX_COMMENT +
                "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1;"
        );

        Label val = new Label(value);
        val.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 18px;");
        val.getStyleClass().add(Styles.TEXT_BOLD);

        box.getChildren().addAll(lbl, val);
        return box;
    }

    private HBox buildPropertyRow(
        String label,
        String value,
        ProbeInfo other,
        String field
    ) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);

        // Label
        Label lbl = new Label(label);
        lbl.setStyle(
            "-fx-text-fill: " + HEX_COMMENT + "; -fx-font-size: 12px;"
        );
        lbl.setMinWidth(80);

        // Value
        Label val = new Label(value);
        val.setStyle("-fx-text-fill: " + C_FG + "; -fx-font-size: 13px;");
        HBox.setHgrow(val, Priority.ALWAYS);

        // Change indicator (only on output panel)
        Label indicator = buildChangeIndicator(value, other, field);
        if (indicator != null) {
            box.getChildren().add(indicator);
        }

        box.getChildren().addAll(lbl, val);
        return box;
    }

    private Label buildChangeIndicator(
        String thisValue,
        ProbeInfo other,
        String field
    ) {
        if (other == null) return null;

        String otherValue;
        boolean changed;

        switch (field) {
            case "codec":
                otherValue = other.codec();
                changed = !thisValue.equals(otherValue);
                break;
            case "fps":
                otherValue = String.valueOf(other.fps());
                changed = !thisValue.equals(otherValue);
                break;
            case "bitrate":
                otherValue = formatBitrate(other.bitrate());
                changed = !thisValue.equals(otherValue);
                break;
            case "audioBitrate":
                otherValue = formatBitrate(other.audioBitrate());
                changed = !thisValue.equals(otherValue);
                break;
            case "duration":
                otherValue = other.formatDuration();
                changed = !thisValue.equals(otherValue);
                break;
            default:
                return null;
        }

        if (!changed) return null;

        // Determine direction: smaller bitrate/size = good (green ↓), larger = bad (red ↑)
        String icon;
        String color;
        boolean thisIsNumeric = false;
        double thisNum = 0,
            otherNum = 0;

        try {
            // Try to extract numeric value for comparison
            String numStr = thisValue.replaceAll("[^0-9.]", "");
            if (!numStr.isEmpty()) {
                thisNum = Double.parseDouble(numStr);
                otherNum = Double.parseDouble(
                    otherValue.replaceAll("[^0-9.]", "")
                );
                thisIsNumeric = true;
            }
        } catch (NumberFormatException e) {
            // Non-numeric, just show neutral indicator
        }

        if (thisIsNumeric) {
            if (thisNum < otherNum) {
                icon = "↓";
                color = HEX_GREEN;
            } else if (thisNum > otherNum) {
                icon = "↑";
                color = HEX_RED;
            } else {
                return null;
            }
        } else {
            icon = "↔";
            color = HEX_ORANGE;
        }

        Label lbl = new Label(icon);
        lbl.setStyle(
            "-fx-text-fill: " +
                color +
                ";" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 0 0 0 4;"
        );
        return lbl;
    }

    // ── Formatting helpers ──────────────────────────────────────────────

    private String formatBitrate(int bps) {
        if (bps <= 0) return "—";
        if (bps >= 1_000_000) {
            return String.format("%.1f Mbps", bps / 1_000_000.0);
        } else if (bps >= 1_000) {
            return String.format("%d kbps", bps / 1_000);
        }
        return String.format("%d bps", bps);
    }

    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "—";
        if (bytes >= 1_073_741_824) {
            return String.format("%.2f GB", bytes / 1_073_741_824.0);
        } else if (bytes >= 1_048_576) {
            return String.format("%.1f MB", bytes / 1_048_576.0);
        } else if (bytes >= 1_024) {
            return String.format("%d kB", bytes / 1024);
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
        summaryLabel.setText("");
    }
}
