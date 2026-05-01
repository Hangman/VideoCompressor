package de.pottgames.videocompressor.view.step;

import atlantafx.base.theme.Styles;
import de.pottgames.videocompressor.engine.Engine;
import de.pottgames.videocompressor.engine.Preset;
import de.pottgames.videocompressor.view.StepView;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Step 2 of the video compressor wizard: allows the user to select a preset
 * and optionally modify individual encoding parameters.
 *
 * The preset card is the visual focal point. Detailed parameter edits are
 * shown in a scrollable section below.
 *
 * This view starts in a disabled state until the Engine is initialized
 * asynchronously. Once {@link #setEngine(Engine)} is called, the UI is
 * populated with loaded presets and enabled.
 */
public class Step2View implements StepView {

    // ── Color constants (Dracula palette) ────────────────────────────────

    private static final String C_CURRENT = "#343646";
    private static final String C_COMMENT = "#6272a4";
    private static final String C_FG = "#f8f8f2";
    private static final String C_PURPLE = "#bd93f9";

    private static final String C_CYAN = "#8be9fd";
    private static final String C_ORANGE = "#ffb86c";

    // ── Engine state ────────────────────────────────────────────────────
    private Engine engine;

    // ── Available presets (mutable, updated when engine is set) ──────────
    private final ObservableList<Preset> presets =
        FXCollections.observableArrayList();

    // ── Currently selected preset (mutable copy for editing) ─────────────
    private Preset selectedPreset;

    // ── UI references ────────────────────────────────────────────────────
    // Assigned in build methods called from constructor, so not final.
    private VBox root = null;
    private ChoiceBox<Preset> presetChoiceBox = null;
    private VBox detailPanel = null;
    // (expandButton entfernt – Detailbereich jetzt immer sichtbar und scrollbar)

    // ── Detail controls (bound to selected preset values) ────────────────
    // Initialized inline with null; actual instances are created in build methods
    // called from the constructor.
    private ChoiceBox<String> codecBox = null;
    private TextField crfField = null;
    private TextField resWidthField = null;
    private TextField resHeightField = null;
    private TextField fpsField = null;
    private TextField maxFileSizeField = null;
    private TextField audioBitrateField = null;
    private CheckBox audioNormalizeCheck = null;
    private CheckBox fastStartCheck = null;
    private CheckBox keepSourceResCheck = null;
    private HBox resolutionRow = null;
    private ChoiceBox<String> ffmpegPresetBox = null;
    private ChoiceBox<String> tuneBox = null;

    public Step2View() {
        // ── Build UI ───────────────────────────────────────────────────
        root = new VBox(16);
        root.setPadding(new Insets(20));

        // Preset selector dropdown
        HBox presetSelector = buildPresetSelector();

        // Separator before expandable section
        Separator separator = new Separator();
        separator.setStyle("-fx-divider-color: " + C_COMMENT + ";");

        // Expandable detail section
        VBox detailSection = buildDetailSection();

        root.getChildren().addAll(presetSelector, separator, detailSection);
        VBox.setVgrow(detailSection, Priority.ALWAYS);

        // Initially disable all interactive controls until engine is ready
        disableAllControls();
    }

    /**
     * Called by the App when the Engine has finished initializing asynchronously.
     * Populates the preset list and enables all UI controls.
     *
     * @param engine the initialized Engine instance
     */
    public void setEngine(Engine engine) {
        if (this.engine != null) {
            return; // Already initialized
        }

        this.engine = engine;

        // Load presets from engine
        List<Preset> loaded = engine.getPresets();
        if (loaded != null && !loaded.isEmpty()) {
            presets.setAll(loaded);
            selectedPreset = loaded.get(0);
            presetChoiceBox.setValue(selectedPreset);
            populateControls();
        } else {
            System.out.println(
                "[Step2View] Warning: Engine initialized but no presets available."
            );
        }

        // Enable all controls now that data is loaded
        enableAllControls();
    }

    /**
     * Disables all interactive UI controls until the engine is ready.
     */
    private void disableAllControls() {
        presetChoiceBox.setDisable(true);
        codecBox.setDisable(true);
        crfField.setDisable(true);
        resWidthField.setDisable(true);
        resHeightField.setDisable(true);
        fpsField.setDisable(true);
        maxFileSizeField.setDisable(true);
        audioBitrateField.setDisable(true);
        audioNormalizeCheck.setDisable(true);
        fastStartCheck.setDisable(true);
        keepSourceResCheck.setDisable(true);
        ffmpegPresetBox.setDisable(true);
        tuneBox.setDisable(true);
    }

    /**
     * Enables all interactive UI controls after the engine is ready.
     */
    private void enableAllControls() {
        presetChoiceBox.setDisable(false);
        codecBox.setDisable(false);
        crfField.setDisable(false);
        resWidthField.setDisable(false);
        resHeightField.setDisable(false);
        fpsField.setDisable(false);
        maxFileSizeField.setDisable(false);
        audioBitrateField.setDisable(false);
        audioNormalizeCheck.setDisable(false);
        fastStartCheck.setDisable(false);
        keepSourceResCheck.setDisable(false);
        ffmpegPresetBox.setDisable(false);
        tuneBox.setDisable(false);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Preset selector
    // ─────────────────────────────────────────────────────────────────────

    private HBox buildPresetSelector() {
        HBox selector = new HBox(8);
        selector.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label("Preset:");
        label.setStyle("-fx-text-fill: " + C_FG + ";");

        presetChoiceBox = new ChoiceBox<>(presets);
        presetChoiceBox.setValue(null);

        // Custom string converter for readable preset names
        presetChoiceBox.setConverter(
            new javafx.util.StringConverter<>() {
                @Override
                public String toString(Preset preset) {
                    return preset != null ? preset.name() : "(Lade Presets...)";
                }

                @Override
                public Preset fromString(String string) {
                    return null;
                }
            }
        );

        // On preset change, update selectedPreset and repopulate controls
        presetChoiceBox
            .getSelectionModel()
            .selectedItemProperty()
            .addListener((_, __, newPreset) -> {
                if (newPreset != null) {
                    selectedPreset = newPreset;
                    populateControls();
                }
            });

        selector.getChildren().addAll(label, presetChoiceBox);
        return selector;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Expandable detail section
    // ─────────────────────────────────────────────────────────────────────

    private VBox buildDetailSection() {
        // Detail panel – immer sichtbar, in ScrollPane eingebettet
        detailPanel = new VBox(16);
        detailPanel.setPadding(new Insets(8, 12, 8, 12));

        // Build the individual editor rows
        buildVideoSettingsGroup();
        buildAudioSettingsGroup();
        buildOutputSettingsGroup();

        // Scrollbar um den Detailbereich, falls er größer als der Parent ist
        ScrollPane scrollPane = new ScrollPane(detailPanel);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle(
            "-fx-background-color: transparent; " +
                "-fx-border-color: " +
                C_COMMENT +
                "; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 4;"
        );

        VBox section = new VBox(4, scrollPane);
        section.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return section;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Video settings group
    // ─────────────────────────────────────────────────────────────────────

    private void buildVideoSettingsGroup() {
        VBox group = new VBox(8);
        group.setPadding(new Insets(8, 12, 8, 12));
        group.setStyle(
            "-fx-background-color: " +
                C_CURRENT +
                "; " +
                "-fx-background-radius: 10;"
        );

        Label groupTitle = new Label("Video");
        groupTitle.getStyleClass().addAll(Styles.TITLE_4, Styles.TEXT_BOLD);
        groupTitle.setStyle("-fx-text-fill: " + C_PURPLE + ";");

        // Codec
        codecBox = new ChoiceBox<>(
            javafx.collections.FXCollections.observableList(
                List.of(
                    "libx264",
                    "libx265",
                    "libsvtav1",
                    "libvpx-vp9",
                    "mpeg4",
                    "mpeg2video",
                    "libtheora",
                    "rawvideo"
                )
            )
        );
        group
            .getChildren()
            .addAll(
                groupTitle,
                buildSettingRow("Codec", "Video-Codec", "libx264", codecBox)
            );

        // CRF
        crfField = new TextField();
        group
            .getChildren()
            .add(
                buildSettingRow(
                    "CRF",
                    "Constrained Rate Factor (0–51, niedriger = besser)",
                    "23",
                    crfField
                )
            );

        // Resolution
        HBox resBox = new HBox(8);
        resWidthField = new TextField();
        resWidthField.setPromptText("1920");
        resWidthField.setPrefColumnCount(5);
        resWidthField.setStyle("-fx-prompt-text-fill: " + C_COMMENT + ";");

        Label xLabel = new Label("×");
        xLabel.setStyle("-fx-text-fill: " + C_COMMENT + ";");
        xLabel.setAlignment(Pos.CENTER);

        resHeightField = new TextField();
        resHeightField.setPromptText("1080");
        resHeightField.setPrefColumnCount(5);
        resHeightField.setStyle("-fx-prompt-text-fill: " + C_COMMENT + ";");

        resBox.getChildren().addAll(resWidthField, xLabel, resHeightField);

        resolutionRow = (HBox) buildSettingRow(
            "Auflösung",
            "Breite × Höhe in Pixel",
            "1920 × 1080",
            resBox
        );

        // Keep source resolution checkbox - own row with label and description
        keepSourceResCheck = new CheckBox();
        HBox keepResCheckBox = new HBox(8);
        keepResCheckBox.getChildren().add(keepSourceResCheck);
        Label keepResCheckLabel = new Label("Quellaufösung beibehalten");
        keepResCheckLabel.setStyle("-fx-text-fill: " + C_FG + ";");
        keepResCheckBox.getChildren().add(keepResCheckLabel);
        keepSourceResCheck.setTooltip(
            new Tooltip("Nutzt die ursprüngliche Auflösung des Quellvideos")
        );

        // Hide resolution row when keepSourceResolution is checked
        keepSourceResCheck
            .selectedProperty()
            .addListener((obs, wasSelected, isSelected) -> {
                resolutionRow.setDisable(isSelected);
            });

        group
            .getChildren()
            .addAll(
                buildSettingRow(
                    "Quellauflösung",
                    "Behält die ursprüngliche Auflösung bei",
                    "",
                    keepResCheckBox
                ),
                resolutionRow
            );

        // FPS
        fpsField = new TextField();
        group
            .getChildren()
            .add(buildSettingRow("FPS", "Bilder pro Sekunde", "30", fpsField));

        // FFmpeg preset
        ffmpegPresetBox = new ChoiceBox<>(
            javafx.collections.FXCollections.observableList(
                List.of(
                    "ultrafast",
                    "superfast",
                    "veryfast",
                    "faster",
                    "fast",
                    "medium",
                    "slow",
                    "slower",
                    "veryslow"
                )
            )
        );
        group
            .getChildren()
            .add(
                buildSettingRow(
                    "FFmpeg Preset",
                    "Geschwindigkeit vs. Qualität Trade-off",
                    "medium",
                    ffmpegPresetBox
                )
            );

        // Tune
        tuneBox = new ChoiceBox<>(
            javafx.collections.FXCollections.observableList(
                List.of(
                    "none",
                    "film",
                    "animation",
                    "grain",
                    "stillimage",
                    "psnr",
                    "ssim",
                    "fastdecode",
                    "zerolatency"
                )
            )
        );
        group
            .getChildren()
            .add(
                buildSettingRow(
                    "Tune",
                    "Optimierung für bestimmten Inhaltstyp",
                    "none",
                    tuneBox
                )
            );

        detailPanel.getChildren().add(group);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Audio settings group
    // ─────────────────────────────────────────────────────────────────────

    private void buildAudioSettingsGroup() {
        VBox group = new VBox(8);
        group.setPadding(new Insets(8, 12, 8, 12));
        group.setStyle(
            "-fx-background-color: " +
                C_CURRENT +
                "; " +
                "-fx-background-radius: 10;"
        );

        Label groupTitle = new Label("Audio");
        groupTitle.getStyleClass().addAll(Styles.TITLE_4, Styles.TEXT_BOLD);
        groupTitle.setStyle("-fx-text-fill: " + C_CYAN + ";");

        // Audio bitrate
        audioBitrateField = new TextField();
        group
            .getChildren()
            .addAll(
                groupTitle,
                buildSettingRow(
                    "Audio Bitrate",
                    "in kbps",
                    "192",
                    audioBitrateField
                )
            );

        // Audio normalize
        audioNormalizeCheck = new CheckBox();
        HBox checkBox = new HBox(8);
        checkBox.getChildren().add(audioNormalizeCheck);
        Label checkLabel = new Label("Lautstärke normalisieren (EBU R128)");
        checkLabel.setStyle("-fx-text-fill: " + C_FG + ";");
        checkBox.getChildren().add(checkLabel);

        group
            .getChildren()
            .add(
                buildSettingRow(
                    "Normalisierung",
                    "Pegelausgleich über die gesamte Datei",
                    "",
                    checkBox
                )
            );

        detailPanel.getChildren().add(group);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Output settings group
    // ─────────────────────────────────────────────────────────────────────

    private void buildOutputSettingsGroup() {
        VBox group = new VBox(8);
        group.setPadding(new Insets(8, 12, 8, 12));
        group.setStyle(
            "-fx-background-color: " +
                C_CURRENT +
                "; " +
                "-fx-background-radius: 10;"
        );

        Label groupTitle = new Label("Ausgabe");
        groupTitle.getStyleClass().addAll(Styles.TITLE_4, Styles.TEXT_BOLD);
        groupTitle.setStyle("-fx-text-fill: " + C_ORANGE + ";");

        // Max file size
        maxFileSizeField = new TextField();
        group
            .getChildren()
            .add(
                buildSettingRow(
                    "Max. Dateigröße",
                    "in MB (0 = unbegrenzt)",
                    "0",
                    maxFileSizeField
                )
            );

        // Fast start
        fastStartCheck = new CheckBox();
        HBox checkBox = new HBox(8);
        checkBox.getChildren().add(fastStartCheck);
        Label checkLabel = new Label("Fast Start (Moov atom voranstellen)");
        checkLabel.setStyle("-fx-text-fill: " + C_FG + ";");
        checkBox.getChildren().add(checkLabel);

        group
            .getChildren()
            .add(
                buildSettingRow(
                    "Fast Start",
                    "Ermöglicht sofortiges Streaming-Playback",
                    "",
                    checkBox
                )
            );

        detailPanel.getChildren().add(group);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Helper – build a single setting row
    // ─────────────────────────────────────────────────────────────────────

    private HBox buildSettingRow(
        String name,
        String tooltip,
        String promptOrHint,
        Node control
    ) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        // Label (fixed width column)
        Label label = new Label(name);
        label.setMinWidth(140);
        label.setMaxWidth(140);
        label.setStyle(
            "-fx-text-fill: " + C_FG + "; -fx-alignment: CENTER_RIGHT;"
        );

        // Tooltip hint
        Label hint = new Label(tooltip);
        hint.setMinWidth(240);
        hint.setMaxWidth(240);
        hint.setStyle("-fx-text-fill: " + C_COMMENT + ";");
        hint.getStyleClass().addAll(Styles.TEXT_SMALL);

        // Control
        if (control instanceof TextField tf) {
            tf.setPromptText(promptOrHint);
            tf.setStyle("-fx-prompt-text-fill: " + C_COMMENT + ";");
            tf.setPrefColumnCount(8);
        }

        HBox.setHgrow(control, Priority.NEVER);
        row.getChildren().addAll(label, hint, control);
        return row;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Populate controls from the currently selected preset
    // ─────────────────────────────────────────────────────────────────────

    private void populateControls() {
        if (selectedPreset == null) {
            return;
        }

        codecBox.getSelectionModel().select(selectedPreset.codec());
        crfField.setText(String.valueOf(selectedPreset.crf()));
        keepSourceResCheck.setSelected(selectedPreset.keepSourceResolution());
        resolutionRow.setDisable(selectedPreset.keepSourceResolution());
        resWidthField.setText(String.valueOf(selectedPreset.resolutionWidth()));
        resHeightField.setText(
            String.valueOf(selectedPreset.resolutionHeight())
        );
        fpsField.setText(String.valueOf(selectedPreset.fps()));
        maxFileSizeField.setText(String.valueOf(selectedPreset.maxFileSize()));
        audioBitrateField.setText(
            String.valueOf(selectedPreset.audioBitrate())
        );
        audioNormalizeCheck.setSelected(selectedPreset.audioNormalize());
        fastStartCheck.setSelected(selectedPreset.fastStart());
        ffmpegPresetBox
            .getSelectionModel()
            .select(selectedPreset.ffmpegPreset());
        tuneBox.getSelectionModel().select(selectedPreset.tune());
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Read current control values back into a new Preset
    // ─────────────────────────────────────────────────────────────────────

    public Preset getModifiedPreset() {
        if (selectedPreset == null) {
            return null;
        }

        return new Preset(
            selectedPreset.name(),
            selectedPreset.description(),
            codecBox.getValue() != null
                ? codecBox.getValue()
                : selectedPreset.codec(),
            safeInt(crfField, selectedPreset.crf()),
            keepSourceResCheck.isSelected(),
            safeInt(resWidthField, selectedPreset.resolutionWidth()),
            safeInt(resHeightField, selectedPreset.resolutionHeight()),
            safeInt(fpsField, selectedPreset.fps()),
            safeInt(maxFileSizeField, selectedPreset.maxFileSize()),
            safeInt(audioBitrateField, selectedPreset.audioBitrate()),
            audioNormalizeCheck.isSelected(),
            fastStartCheck.isSelected(),
            ffmpegPresetBox.getValue() != null
                ? ffmpegPresetBox.getValue()
                : selectedPreset.ffmpegPreset(),
            tuneBox.getValue() != null
                ? tuneBox.getValue()
                : selectedPreset.tune()
        );
    }

    private int safeInt(TextField field, int fallback) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  StepView interface
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public Node getNode() {
        return root;
    }

    @Override
    public void activate(
        Button backButton,
        Button centerButton,
        Button nextButton
    ) {
        // Populate controls with the currently selected preset
        if (selectedPreset != null) {
            populateControls();
        }

        // Navigation
        backButton.setVisible(true);
        backButton.setDisable(false);

        centerButton.setVisible(false);

        nextButton.setVisible(true);
        nextButton.setDisable(false);
    }

    @Override
    public void deactivate() {
        // Nothing to clean up
    }
}
