package de.pottgames.videocompressor.view.step;

import atlantafx.base.theme.Styles;
import de.pottgames.videocompressor.WizardState;
import de.pottgames.videocompressor.engine.ExportPathResolver;
import de.pottgames.videocompressor.engine.Ffprobe;
import de.pottgames.videocompressor.engine.PathOpener;
import de.pottgames.videocompressor.engine.ProbeInfo;
import de.pottgames.videocompressor.engine.VideoJob;
import de.pottgames.videocompressor.engine.VideoJobStatus;
import de.pottgames.videocompressor.engine.VideoJobStatus.Status;
import de.pottgames.videocompressor.i18n.I18n;
import de.pottgames.videocompressor.view.StepView;
import de.pottgames.videocompressor.view.Theme;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
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

    // Theme constants are centralized in Theme.java

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
        centerButton.setText(I18n.get("step4.open_export_folder"));
        centerButton.setVisible(true);
        centerButton.setDisable(false);
        centerButton.setOnAction(_ ->
            PathOpener.openAsync(ExportPathResolver.getExportPath())
        );
        nextButton.setVisible(false);
        nextButton.setDisable(true);

        resultsContent.getChildren().clear();
        summaryLabel.setText(I18n.get("step4.loading"));

        List<VideoJob> jobs = state.getPreparedJobs();
        if (jobs == null || jobs.isEmpty()) {
            Platform.runLater(() -> {
                summaryLabel.setText(I18n.get("step4.no_results"));
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
                .thenCompose(_ -> {
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
                            I18n.get(
                                "step4.loading_progress",
                                index + 1,
                                totalFiles
                            )
                        );
                    });

                    return info;
                });
        }

        chain.whenComplete((_, _) -> {
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
            I18n.get("step4.summary") +
                " " +
                I18n.get("step4.file_count", numJobs) +
                "  ·  " +
                successCount +
                " " +
                I18n.get("step4.successful") +
                (failedCount > 0
                    ? "  ·  " + failedCount + " " + I18n.get("step4.failed")
                    : "") +
                (skippedCount > 0
                    ? "  ·  " + skippedCount + " " + I18n.get("step4.skipped")
                    : "")
        );

        // Decorative separator under summary
        Pane summarySep = new Pane();
        summarySep.setPrefHeight(1);
        summarySep.setStyle("-fx-background-color: " + Theme.HEX_DIVIDER + ";");
        HBox.setHgrow(summarySep, Priority.ALWAYS);
        resultsContent.getChildren().add(summarySep);

        // Build comparison card for each job
        for (int i = 0; i < jobs.size(); i++) {
            VideoJob job = jobs.get(i);
            ProbeInfo sourceInfo = job.sourceInfo();
            ProbeInfo outputInfo = outputProbes.get(i);
            String outputProbeError =
                outputInfo == null ? I18n.get("step4.probe_error") : null;

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
                Theme.HEX_CARD_BG +
                ";" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: " +
                Theme.HEX_BORDER +
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
                Theme.TEXT_FILL_HEX_ORANGE_STYLE +
                    Theme.FONT_BASE_STYLE +
                    "-fx-padding: 8 12;"
            );
            errorLabel.setWrapText(true);
            card.getChildren().add(errorLabel);

            if (sourceInfo != null) {
                VBox sourcePanel = buildSinglePanel(
                    I18n.get("step4.source_file_panel"),
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
                Theme.CSS_ACCENT_FG +
                ";" +
                Theme.TEXT_FILL_BG_DEFAULT_STYLE +
                ";" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 5 9;" +
                Theme.FONT_XLARGE_STYLE
        );

        // File names
        Label nameLabel = new Label();
        String srcName =
            src != null ? src.file().getName() : I18n.get("step4.unknown");
        String outName =
            out != null ? out.file().getName() : I18n.get("step4.unknown");
        if (srcName.equals(outName)) {
            nameLabel.setText(srcName);
        } else {
            nameLabel.setText(srcName + "  →  " + outName);
        }
        nameLabel.setStyle(Theme.TEXT_FILL_FG_STYLE + Theme.FONT_LARGE_STYLE);

        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        // Status pill
        Label statusPill = new Label();
        if (status.getStatus() == Status.COMPLETED) {
            statusPill.setText(I18n.get("step4.status_completed"));
            statusPill.setStyle(
                Theme.TEXT_FILL_SUCCESS_STYLE +
                    "-fx-background-color: rgba(80,250,123,0.1);" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 4 10;" +
                    Theme.FONT_SMALL_STYLE
            );
        } else if (status.getStatus() == Status.FAILED) {
            statusPill.setText(I18n.get("step4.status_failed"));
            statusPill.setStyle(
                Theme.TEXT_FILL_HEX_RED_STYLE +
                    "-fx-background-color: rgba(255,85,85,0.1);" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 4 10;" +
                    Theme.FONT_SMALL_STYLE
            );
        } else {
            statusPill.setText(I18n.get("step4.status_skipped"));
            statusPill.setStyle(
                Theme.TEXT_FILL_HEX_ORANGE_STYLE +
                    "-fx-background-color: rgba(255,184,108,0.1);" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 4 10;" +
                    Theme.FONT_SMALL_STYLE
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
            textColor = Theme.CSS_SUCCESS;
            icon = "↓";
            label = I18n.get("step4.savings");
        } else if (pct < -0.5) {
            bgColor = "rgba(255,85,85,0.08)";
            textColor = Theme.HEX_DANGER;
            icon = "↑";
            label = I18n.get("step4.larger");
        } else {
            bgColor = "rgba(98,114,164,0.08)";
            textColor = Theme.CSS_FG_SUBTLE;
            icon = "—";
            label = I18n.get("step4.unchanged");
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
            "-fx-text-fill: " + textColor + "; " + Theme.FONT_XLARGE_STYLE
        );

        // Main percentage
        Label pctLabel = new Label(
            String.format(I18n.getLocale(), "%s: %.1f%%", label, Math.abs(pct))
        );
        pctLabel.setStyle(
            "-fx-text-fill: " + textColor + "; " + Theme.FONT_LARGE_STYLE
        );

        // Sizes
        Label sizeLabel = new Label(
            formatFileSize(srcSize) + "  →  " + formatFileSize(outSize)
        );
        sizeLabel.setStyle(
            Theme.TEXT_FILL_FG_SUBTLE_STYLE + Theme.FONT_SMALL_STYLE
        );

        banner.getChildren().addAll(iconLabel, pctLabel, sizeLabel);
        HBox.setHgrow(pctLabel, Priority.ALWAYS);

        return banner;
    }

    // ── Two-Panel Comparison ─────────────────────────────────────────────

    private HBox buildComparisonPanels(ProbeInfo source, ProbeInfo output) {
        HBox panels = new HBox(12);
        panels.setAlignment(Pos.CENTER_LEFT);

        VBox leftPanel = buildSinglePanel(
            I18n.get("step4.source_panel"),
            source,
            output
        );
        VBox rightPanel = buildSinglePanel(
            I18n.get("step4.output_panel"),
            output,
            source
        );

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
                (title.equals(I18n.get("step4.source_panel"))
                    ? Theme.HEX_PANEL_SOURCE
                    : Theme.HEX_PANEL_OUTPUT) +
                ";" +
                "-fx-background-radius: 10;"
        );
        HBox.setHgrow(panel, Priority.ALWAYS);

        // Panel title row: label (left) + play button (right)
        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.setSpacing(8);
        titleRow.setPadding(new Insets(0, 0, 12, 0));

        Label titleLabel = new Label(title.toUpperCase());
        titleLabel.setStyle(
            Theme.TEXT_FILL_FG_SUBTLE_STYLE +
                Theme.FONT_SMALL_STYLE +
                "-fx-letter-spacing: 1.5;"
        );

        Button playButton = new Button("▶");
        playButton.setStyle(
            "-fx-background-color: transparent;" +
                "-fx-text-fill: " +
                Theme.HEX_ACCENT +
                ";" +
                "-fx-font-size: 14px;" +
                "-fx-cursor: hand;" +
                "-fx-border: 1px;" +
                "-fx-border-color: " +
                Theme.HEX_ACCENT +
                ";" +
                "-fx-border-radius: 6px;"
        );
        playButton.setPadding(new Insets(4, 10, 4, 10));
        playButton.setOnAction(e -> {
            PathOpener.openAsync(info.file().toPath());
        });

        titleRow.getChildren().addAll(titleLabel, new Pane(), playButton);
        HBox.setHgrow((Pane) titleRow.getChildren().get(1), Priority.ALWAYS);
        panel.getChildren().add(titleRow);

        // Divider under title
        Pane div = new Pane();
        div.setPrefHeight(1);
        div.setStyle("-fx-background-color: " + Theme.HEX_DIVIDER + ";");
        panel.getChildren().add(div);

        VBox props = new VBox(10);
        props.setPadding(new Insets(12, 0, 0, 0));

        // ── Resolution (hero value) ────────────────────────────────────
        props
            .getChildren()
            .add(
                buildHeroProperty(
                    I18n.get("step4.property.resolution"),
                    info.getAbbreviatedResolution(),
                    Theme.CSS_FG
                )
            );

        // Divider
        Pane d1 = new Pane();
        d1.setPrefHeight(1);
        d1.setStyle("-fx-background-color: " + Theme.HEX_DIVIDER + ";");
        props.getChildren().add(d1);

        // ── Codec ──────────────────────────────────────────────────────
        props
            .getChildren()
            .add(
                buildPropertyRow(
                    I18n.get("step4.property.codec"),
                    info.codec(),
                    info,
                    other,
                    "codec"
                )
            );

        // ── FPS ────────────────────────────────────────────────────────
        props
            .getChildren()
            .add(
                buildPropertyRow(
                    I18n.get("step4.property.fps"),
                    ProbeInfo.formatFps(info.fps()),
                    info,
                    other,
                    "fps"
                )
            );

        // ── Bitrate ────────────────────────────────────────────────────
        props
            .getChildren()
            .add(
                buildPropertyRow(
                    I18n.get("step4.property.bitrate"),
                    formatBitrate(info.bitrate()),
                    info,
                    other,
                    "bitrate"
                )
            );

        // ── Audio Bitrate ──────────────────────────────────────────────
        props
            .getChildren()
            .add(
                buildPropertyRow(
                    I18n.get("step4.property.audio_bitrate"),
                    formatBitrate(info.audioBitrate()),
                    info,
                    other,
                    "audioBitrate"
                )
            );

        // ── Duration ───────────────────────────────────────────────────
        props
            .getChildren()
            .add(
                buildPropertyRow(
                    I18n.get("step4.property.duration"),
                    info.formatDuration(),
                    info,
                    other,
                    "duration"
                )
            );

        // ── File Size (hero value) ─────────────────────────────────────
        Pane d2 = new Pane();
        d2.setPrefHeight(1);
        d2.setStyle("-fx-background-color: " + Theme.HEX_DIVIDER + ";");
        props.getChildren().add(d2);

        props
            .getChildren()
            .add(
                buildHeroProperty(
                    I18n.get("step4.property.file_size"),
                    formatFileSize(info.fileSize()),
                    Theme.CSS_FG
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
            Theme.TEXT_FILL_FG_SUBTLE_STYLE +
                Theme.FONT_SMALL_STYLE +
                "-fx-letter-spacing: 1;"
        );

        Label val = new Label(value);
        val.setStyle(
            "-fx-text-fill: " + color + "; " + Theme.FONT_XLARGE_STYLE
        );

        box.getChildren().addAll(lbl, val);
        return box;
    }

    private HBox buildPropertyRow(
        String label,
        String value,
        ProbeInfo thisInfo,
        ProbeInfo other,
        String field
    ) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);

        // Indicator slot (always reserved so values stay aligned across panels)
        final double INDICATOR_SLOT_WIDTH = 20;
        Label indicator = buildChangeIndicator(thisInfo, other, field);
        if (indicator == null) {
            indicator = new Label();
            indicator.setVisible(false);
        }
        indicator.setMinWidth(INDICATOR_SLOT_WIDTH);
        indicator.setPrefWidth(INDICATOR_SLOT_WIDTH);

        // Label
        Label lbl = new Label(label);
        lbl.setStyle(Theme.TEXT_FILL_FG_SUBTLE_STYLE + Theme.FONT_SMALL_STYLE);
        lbl.setMinWidth(80);

        // Value
        Label val = new Label(value);
        val.setStyle(Theme.TEXT_FILL_FG_STYLE + Theme.FONT_BASE_STYLE);
        HBox.setHgrow(val, Priority.ALWAYS);

        box.getChildren().addAll(indicator, lbl, val);
        return box;
    }

    private Label buildChangeIndicator(
        ProbeInfo thisInfo,
        ProbeInfo other,
        String field
    ) {
        if (other == null) return null;

        boolean changed;
        double thisNum = 0;
        double otherNum = 0;

        switch (field) {
            case "codec":
                changed = !thisInfo.codec().equals(other.codec());
                break;
            case "fps":
                thisNum = thisInfo.fps();
                otherNum = other.fps();
                changed = thisNum != otherNum;
                break;
            case "bitrate":
                thisNum = thisInfo.bitrate();
                otherNum = other.bitrate();
                changed = thisNum != otherNum;
                break;
            case "audioBitrate":
                thisNum = thisInfo.audioBitrate();
                otherNum = other.audioBitrate();
                changed = thisNum != otherNum;
                break;
            case "duration":
                thisNum = thisInfo.duration();
                otherNum = other.duration();
                changed = Math.abs(thisNum - otherNum) > 0.5;
                break;
            default:
                return null;
        }

        if (!changed) return null;

        // Determine direction: smaller bitrate = good (green ↓), larger = bad (red ↑)
        String icon;
        String color;

        if (field.equals("codec")) {
            icon = "↔";
            color = Theme.HEX_ORANGE;
        } else if (thisNum < otherNum) {
            icon = "↓";
            color = Theme.CSS_SUCCESS;
        } else if (thisNum > otherNum) {
            icon = "↑";
            color = Theme.HEX_DANGER;
        } else {
            return null;
        }

        Label lbl = new Label(icon);
        lbl.setStyle(
            "-fx-text-fill: " +
                color +
                ";" +
                Theme.FONT_BASE_STYLE +
                "-fx-padding: 0 0 0 4;"
        );
        return lbl;
    }

    // ── Formatting helpers ──────────────────────────────────────────────

    private String formatBitrate(int bps) {
        if (bps <= 0) return "—";
        if (bps >= 1_000_000) {
            return String.format(
                I18n.getLocale(),
                "%.1f Mbps",
                bps / 1_000_000.0
            );
        } else if (bps >= 1_000) {
            return String.format(I18n.getLocale(), "%d kbps", bps / 1_000);
        }
        return String.format(I18n.getLocale(), "%d bps", bps);
    }

    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "—";
        if (bytes >= 1_073_741_824) {
            return String.format(
                I18n.getLocale(),
                "%.2f GB",
                bytes / 1_073_741_824.0
            );
        } else if (bytes >= 1_048_576) {
            return String.format(
                I18n.getLocale(),
                "%.1f MB",
                bytes / 1_048_576.0
            );
        } else if (bytes >= 1_024) {
            return String.format(I18n.getLocale(), "%d kB", bytes / 1024);
        }
        return String.format(I18n.getLocale(), "%d B", bytes);
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
