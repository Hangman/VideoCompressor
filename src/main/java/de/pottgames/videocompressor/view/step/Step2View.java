package de.pottgames.videocompressor.view.step;

import atlantafx.base.theme.Styles;
import de.pottgames.videocompressor.engine.AudioCodec;
import de.pottgames.videocompressor.engine.Engine;
import de.pottgames.videocompressor.engine.FfmpegPreset;
import de.pottgames.videocompressor.engine.Preset;
import de.pottgames.videocompressor.engine.Tune;
import de.pottgames.videocompressor.engine.VideoCodec;
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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
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

    private static final String C_COMMENT = "#6272a4";
    private static final String C_FG = "#f8f8f2";

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
    private Label presetDescriptionLabel = null;

    // (expandButton entfernt – Detailbereich jetzt immer sichtbar und scrollbar)

    // ── Detail controls (bound to selected preset values) ────────────────
    // Initialized inline with null; actual instances are created in build methods
    // called from the constructor.
    private ChoiceBox<VideoCodec> codecBox = null;
    private TextField crfField = null;
    private TextField resWidthField = null;
    private TextField resHeightField = null;
    private TextField fpsField = null;
    private TextField maxFileSizeField = null;
    private TextField audioBitrateField = null;
    private CheckBox audioNormalizeCheck = null;
    private CheckBox mixToMonoCheck = null;
    private CheckBox fastStartCheck = null;
    private CheckBox keepSourceResCheck = null;
    private CheckBox keepSourceAudioCheck = null;
    private ChoiceBox<AudioCodec> audioCodecBox = null;
    private VBox audioRow = null;
    private HBox resolutionRow = null;
    private ChoiceBox<FfmpegPreset> ffmpegPresetBox = null;
    private ChoiceBox<Tune> tuneBox = null;

    public Step2View() {
        // ── Build UI ───────────────────────────────────────────────────
        root = new VBox(16);
        root.setPadding(new Insets(20));

        // Preset selector dropdown
        HBox presetSelector = buildPresetSelector();

        // Tabbed detail section
        Node detailSection = buildDetailSection();

        root.getChildren().addAll(presetSelector, detailSection);
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
        audioCodecBox.setDisable(true);
        audioBitrateField.setDisable(true);
        audioNormalizeCheck.setDisable(true);
        mixToMonoCheck.setDisable(true);
        fastStartCheck.setDisable(true);
        keepSourceResCheck.setDisable(true);
        keepSourceAudioCheck.setDisable(true);
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
        audioCodecBox.setDisable(false);
        audioBitrateField.setDisable(false);
        audioNormalizeCheck.setDisable(false);
        mixToMonoCheck.setDisable(false);
        fastStartCheck.setDisable(false);
        keepSourceResCheck.setDisable(false);
        keepSourceAudioCheck.setDisable(false);
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

        // Description label shown to the right of the dropdown
        presetDescriptionLabel = new Label();
        presetDescriptionLabel
            .getStyleClass()
            .addAll(Styles.TEXT_SMALL, Styles.TEXT_SUBTLE);
        presetDescriptionLabel.setWrapText(true);
        presetDescriptionLabel.setMaxWidth(320);

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

        selector
            .getChildren()
            .addAll(label, presetChoiceBox, presetDescriptionLabel);
        HBox.setHgrow(presetDescriptionLabel, Priority.ALWAYS);
        return selector;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Expandable detail section
    // ─────────────────────────────────────────────────────────────────────

    private TabPane buildDetailSection() {
        // Build the individual editor rows
        VBox videoContent = buildVideoSettingsGroup();
        VBox audioContent = buildAudioSettingsGroup();
        VBox outputContent = buildOutputSettingsGroup();

        // Wrap each section in its own ScrollPane
        ScrollPane videoScroll = wrapInScrollPane(videoContent);
        ScrollPane audioScroll = wrapInScrollPane(audioContent);
        ScrollPane outputScroll = wrapInScrollPane(outputContent);

        Tab videoTab = new Tab("Video", videoScroll);
        Tab audioTab = new Tab("Audio", audioScroll);
        Tab outputTab = new Tab("Output", outputScroll);

        TabPane tabPane = new TabPane(videoTab, audioTab, outputTab);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-tab-max-width: 120;");
        return tabPane;
    }

    private ScrollPane wrapInScrollPane(VBox content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle(
            "-fx-background-color: transparent; " +
                "-fx-border-color: " +
                C_COMMENT +
                "; " +
                "-fx-border-width: 1; "
        );
        return scrollPane;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Video settings group
    // ─────────────────────────────────────────────────────────────────────

    private VBox buildVideoSettingsGroup() {
        VBox group = new VBox(10);
        group.setPadding(new Insets(10));

        // Codec
        codecBox = new ChoiceBox<>(
            FXCollections.observableArrayList(VideoCodec.values())
        );
        group
            .getChildren()
            .add(buildSettingRow("Codec", "Video-Codec", "libx264", codecBox));

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
            FXCollections.observableArrayList(FfmpegPreset.values())
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
            FXCollections.observableArrayList(Tune.values())
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

        return group;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Audio settings group
    // ─────────────────────────────────────────────────────────────────────

    private VBox buildAudioSettingsGroup() {
        VBox group = new VBox(10);
        group.setPadding(new Insets(10));

        // Keep source audio checkbox
        keepSourceAudioCheck = new CheckBox();
        HBox keepAudioCheckBox = new HBox(8);
        keepAudioCheckBox.getChildren().add(keepSourceAudioCheck);
        Label keepAudioCheckLabel = new Label("Quellaudio beibehalten");
        keepAudioCheckLabel.setStyle("-fx-text-fill: " + C_FG + ";");
        keepAudioCheckBox.getChildren().add(keepAudioCheckLabel);
        keepSourceAudioCheck.setTooltip(
            new Tooltip("Nutzt das ursprüngliche Audio des Quellvideos")
        );

        // Container for audio settings that can be disabled
        audioRow = new VBox(10);

        // Audio codec
        audioCodecBox = new ChoiceBox<>(
            FXCollections.observableArrayList(AudioCodec.values())
        );
        audioRow
            .getChildren()
            .add(
                buildSettingRow(
                    "Audio Codec",
                    "Audio-Codec",
                    "AAC",
                    audioCodecBox
                )
            );

        // Audio bitrate
        audioBitrateField = new TextField();
        audioRow
            .getChildren()
            .add(
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

        audioRow
            .getChildren()
            .add(
                buildSettingRow(
                    "Normalisierung",
                    "Pegelausgleich über die gesamte Datei",
                    "",
                    checkBox
                )
            );

        // Mix to mono
        mixToMonoCheck = new CheckBox();
        HBox monoBox = new HBox(8);
        monoBox.getChildren().add(mixToMonoCheck);
        Label monoLabel = new Label("Zu MONO heruntermischen");
        monoLabel.setStyle("-fx-text-fill: " + C_FG + ";");
        monoBox.getChildren().add(monoLabel);

        audioRow
            .getChildren()
            .add(
                buildSettingRow(
                    "Mono-Mixdown",
                    "Audiokanäle zu einem MONO-Kanal mischen",
                    "",
                    monoBox
                )
            );

        // Disable audio settings when keepSourceAudio is checked
        keepSourceAudioCheck
            .selectedProperty()
            .addListener((obs, wasSelected, isSelected) -> {
                audioRow.setDisable(isSelected);
            });

        group
            .getChildren()
            .addAll(
                buildSettingRow(
                    "Quellaudio",
                    "Behält das ursprüngliche Audio bei",
                    "",
                    keepAudioCheckBox
                ),
                audioRow
            );

        return group;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Output settings group
    // ─────────────────────────────────────────────────────────────────────

    private VBox buildOutputSettingsGroup() {
        VBox group = new VBox(10);
        group.setPadding(new Insets(10));

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

        return group;
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
        hint.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_SUBTLE);

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

        // Update preset description label
        String desc = selectedPreset.description();
        if (desc != null && !desc.isEmpty()) {
            presetDescriptionLabel.setText(desc);
            presetDescriptionLabel.setOpacity(1.0);
        } else {
            presetDescriptionLabel.setOpacity(0.0);
        }

        codecBox.getSelectionModel().select(selectedPreset.videoCodec());
        crfField.setText(String.valueOf(selectedPreset.crf()));
        keepSourceResCheck.setSelected(selectedPreset.keepSourceResolution());
        resolutionRow.setDisable(selectedPreset.keepSourceResolution());
        resWidthField.setText(String.valueOf(selectedPreset.resolutionWidth()));
        resHeightField.setText(
            String.valueOf(selectedPreset.resolutionHeight())
        );
        fpsField.setText(String.valueOf(selectedPreset.fps()));
        maxFileSizeField.setText(String.valueOf(selectedPreset.maxFileSize()));
        keepSourceAudioCheck.setSelected(selectedPreset.keepSourceAudio());
        audioRow.setDisable(selectedPreset.keepSourceAudio());
        audioCodecBox.getSelectionModel().select(selectedPreset.audioCodec());
        audioBitrateField.setText(
            String.valueOf(selectedPreset.audioBitrate())
        );
        audioNormalizeCheck.setSelected(selectedPreset.audioNormalize());
        mixToMonoCheck.setSelected(selectedPreset.mixToMono());
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
                : selectedPreset.videoCodec(),
            safeInt(crfField, selectedPreset.crf()),
            keepSourceResCheck.isSelected(),
            safeInt(resWidthField, selectedPreset.resolutionWidth()),
            safeInt(resHeightField, selectedPreset.resolutionHeight()),
            safeInt(fpsField, selectedPreset.fps()),
            safeInt(maxFileSizeField, selectedPreset.maxFileSize()),
            keepSourceAudioCheck.isSelected(),
            audioCodecBox.getValue() != null
                ? audioCodecBox.getValue()
                : selectedPreset.audioCodec(),
            safeInt(audioBitrateField, selectedPreset.audioBitrate()),
            audioNormalizeCheck.isSelected(),
            mixToMonoCheck.isSelected(),
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
