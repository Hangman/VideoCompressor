package de.pottgames.videocompressor.view.step;

import atlantafx.base.theme.Styles;
import de.pottgames.videocompressor.WizardState;
import de.pottgames.videocompressor.engine.AudioCodec;
import de.pottgames.videocompressor.engine.Engine;
import de.pottgames.videocompressor.engine.FfmpegPreset;
import de.pottgames.videocompressor.engine.Preset;
import de.pottgames.videocompressor.engine.Preset.ValidationResult;
import de.pottgames.videocompressor.engine.Tune;
import de.pottgames.videocompressor.engine.VideoCodec;
import de.pottgames.videocompressor.engine.VideoContainer;
import de.pottgames.videocompressor.view.StepView;
import java.util.List;
import java.util.Objects;
import javafx.animation.PauseTransition;
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
import javafx.util.Duration;

/**
 * Step 2 of the video compressor wizard: allows the user to select a preset
 * and optionally modify individual encoding parameters.
 *
 * The preset card is the visual focal point. Detailed parameter edits are
 * shown in a scrollable section below.
 *
 * A live validation panel shows real-time feedback (debounced) as the user
 * tweaks settings, colour-coded green / amber / red.
 *
 * This view starts in a disabled state until the Engine is initialized
 * asynchronously. Once {@link #setEngine(Engine)} is called, the UI is
 * populated with loaded presets and enabled.
 */
public class Step2View implements StepView {

    // ── Color constants (Dracula palette) ────────────────────────────────

    private static final String C_COMMENT = "#6272a4";
    private static final String C_FG = "#f8f8f2";

    // ── Validation UI colors ─────────────────────────────────────────────

    private static final String C_VALID = "#50fa7b"; // Green
    private static final String C_WARNING = "#f1fa8c"; // Yellow/Amber
    private static final String C_ERROR = "#ff5555"; // Red

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

    // ── Validation UI ────────────────────────────────────────────────────
    private VBox validationPanel = null;
    private Label validationIconLabel = null;
    private Label validationLabel = null;
    private PauseTransition validationDebounce = null;

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
    private ChoiceBox<VideoContainer> containerBox = null;
    private VBox audioRow = null;
    private HBox resolutionRow = null;
    private ChoiceBox<FfmpegPreset> ffmpegPresetBox = null;
    private ChoiceBox<Tune> tuneBox = null;

    // ─────────────────────────────────────────────────────────────────────
    //  Constructor
    // ─────────────────────────────────────────────────────────────────────

    public Step2View() {
        // ── Build UI ───────────────────────────────────────────────────
        root = new VBox(16);
        root.setPadding(new Insets(20));

        // Preset selector dropdown
        HBox presetSelector = buildPresetSelector();

        // Tabbed detail section
        Node detailSection = buildDetailSection();

        // Validation feedback panel
        VBox validation = buildValidationPanel();

        root.getChildren().addAll(presetSelector, detailSection, validation);
        VBox.setVgrow(detailSection, Priority.ALWAYS);

        // Initially disable all interactive controls until engine is ready
        setControlsEnabled(false);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Engine initialization
    // ─────────────────────────────────────────────────────────────────────

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
        setControlsEnabled(true);

        // Wire up validation listeners after controls are enabled
        setupValidationListeners();
    }

    /**
     * Enables or disables all interactive UI controls.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     */
    private void setControlsEnabled(boolean enabled) {
        boolean disable = !enabled;
        presetChoiceBox.setDisable(disable);
        codecBox.setDisable(disable);
        crfField.setDisable(disable);
        resWidthField.setDisable(disable);
        resHeightField.setDisable(disable);
        fpsField.setDisable(disable);
        maxFileSizeField.setDisable(disable);
        containerBox.setDisable(disable);
        audioCodecBox.setDisable(disable);
        audioBitrateField.setDisable(disable);
        audioNormalizeCheck.setDisable(disable);
        mixToMonoCheck.setDisable(disable);
        fastStartCheck.setDisable(disable);
        keepSourceResCheck.setDisable(disable);
        keepSourceAudioCheck.setDisable(disable);
        ffmpegPresetBox.setDisable(disable);
        tuneBox.setDisable(disable);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Validation
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Builds a compact validation feedback panel that shows the current
     * preset validity status with color-coded icon and message.
     */
    private VBox buildValidationPanel() {
        validationPanel = new VBox(4);
        validationPanel.setPadding(new Insets(8, 12, 8, 12));
        validationPanel.setStyle(
            "-fx-background-color: rgba(0,0,0,0.15); " +
                "-fx-background-radius: 6; " +
                "-fx-border-radius: 6;"
        );
        validationPanel.setVisible(false); // Hidden until engine is ready

        // Icon + status line inside an HBox
        HBox iconRow = new HBox(8);
        iconRow.setAlignment(Pos.CENTER_LEFT);

        validationIconLabel = new Label();
        validationIconLabel.setStyle("-fx-font-size: 16px;");

        validationLabel = new Label();
        validationLabel.setWrapText(true);
        validationLabel.setMaxWidth(500);

        iconRow.getChildren().addAll(validationIconLabel, validationLabel);
        validationPanel.getChildren().add(iconRow);

        // Build debounce timer (rearms on each trigger)
        validationDebounce = new PauseTransition(Duration.millis(400));
        validationDebounce.setOnFinished(_evt -> {
            Preset preview = getModifiedPreset();
            if (preview != null) {
                ValidationResult result = preview.validate();
                showValidationResult(result);
            }
        });

        return validationPanel;
    }

    /**
     * Attaches listeners to all editable controls so that any change
     * restarts the debounce timer and re-validates the preset.
     */
    private void setupValidationListeners() {
        validationPanel.setVisible(true);

        // TextFields: listen to text property changes
        for (TextField tf : List.of(
            crfField,
            resWidthField,
            resHeightField,
            fpsField,
            maxFileSizeField,
            audioBitrateField
        )) {
            tf
                .textProperty()
                .addListener((_, _, _) -> {
                    validationDebounce.stop();
                    validationDebounce.playFromStart();
                });
        }

        // CheckBoxes: listen to selected property changes
        for (CheckBox cb : List.of(
            keepSourceResCheck,
            keepSourceAudioCheck,
            audioNormalizeCheck,
            mixToMonoCheck,
            fastStartCheck
        )) {
            cb
                .selectedProperty()
                .addListener((_, _, _) -> {
                    validationDebounce.stop();
                    validationDebounce.playFromStart();
                });
        }

        // ChoiceBoxes: listen to selected item changes
        for (ChoiceBox<?> cb : List.of(
            codecBox,
            containerBox,
            audioCodecBox,
            ffmpegPresetBox,
            tuneBox
        )) {
            cb
                .getSelectionModel()
                .selectedItemProperty()
                .addListener((_, _, _) -> {
                    validationDebounce.stop();
                    validationDebounce.playFromStart();
                });
        }

        // Initial validation run
        validationDebounce.stop();
        validationDebounce.playFromStart();
    }

    /**
     * Renders the validation result into the UI panel with appropriate
     * color coding: green (valid), amber (warnings only), red (errors).
     */
    private void showValidationResult(ValidationResult result) {
        if (result.valid() && result.warnings().isEmpty()) {
            // Fully valid — green checkmark
            validationIconLabel.setText("\u2714"); // ✓
            validationIconLabel.setStyle(
                "-fx-font-size: 16px; -fx-text-fill: " + C_VALID + ";"
            );
            validationLabel.setText("Konfiguration ist valide.");
            validationLabel.setStyle("-fx-text-fill: " + C_VALID + ";");
            validationPanel.setStyle(
                "-fx-background-color: rgba(80,250,123,0.08); " +
                    "-fx-background-radius: 6; -fx-border-radius: 6; " +
                    "-fx-border-color: " +
                    C_VALID +
                    "; -fx-border-width: 1;"
            );
        } else if (result.valid()) {
            // Valid but has warnings — amber
            validationIconLabel.setText("\u26A0"); // ⚠
            validationIconLabel.setStyle(
                "-fx-font-size: 16px; -fx-text-fill: " + C_WARNING + ";"
            );
            int wCount = result.warnings().size();
            validationLabel.setText(
                wCount +
                    " Hin" +
                    (wCount == 1 ? "weis" : "weise") +
                    " — " +
                    String.join(" ", result.warnings())
            );
            validationLabel.setStyle("-fx-text-fill: " + C_WARNING + ";");
            validationPanel.setStyle(
                "-fx-background-color: rgba(241,250,140,0.08); " +
                    "-fx-background-radius: 6; -fx-border-radius: 6; " +
                    "-fx-border-color: " +
                    C_WARNING +
                    "; -fx-border-width: 1;"
            );
        } else {
            // Has errors — red
            validationIconLabel.setText("\u2717"); // ✗
            validationIconLabel.setStyle(
                "-fx-font-size: 16px; -fx-text-fill: " + C_ERROR + ";"
            );
            int eCount = result.errors().size();
            int wCount = result.warnings().size();
            String msg = eCount + " Fehler";
            if (wCount > 0) {
                msg +=
                    " und " +
                    wCount +
                    " Hin" +
                    (wCount == 1 ? "weis" : "weise");
            }
            validationLabel.setText(
                msg + " — " + String.join(" ", result.errors())
            );
            validationLabel.setStyle("-fx-text-fill: " + C_ERROR + ";");
            validationPanel.setStyle(
                "-fx-background-color: rgba(255,85,85,0.08); " +
                    "-fx-background-radius: 6; -fx-border-radius: 6; " +
                    "-fx-border-color: " +
                    C_ERROR +
                    "; -fx-border-width: 1;"
            );
        }
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
                    // Re-trigger validation for the new preset values
                    validationDebounce.stop();
                    validationDebounce.playFromStart();
                }
            });

        selector
            .getChildren()
            .addAll(label, presetChoiceBox, presetDescriptionLabel);
        HBox.setHgrow(presetDescriptionLabel, Priority.ALWAYS);
        return selector;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Detail section (tabbed)
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

        Label xLabel = new Label("\u00D7");
        xLabel.setStyle("-fx-text-fill: " + C_COMMENT + ";");
        xLabel.setAlignment(Pos.CENTER);

        resHeightField = new TextField();
        resHeightField.setPromptText("1080");
        resHeightField.setPrefColumnCount(5);
        resHeightField.setStyle("-fx-prompt-text-fill: " + C_COMMENT + ";");

        resBox.getChildren().addAll(resWidthField, xLabel, resHeightField);

        resolutionRow = (HBox) buildSettingRow(
            "Auflösung",
            "Breite \u00D7 Höhe in Pixel",
            "1920 \u00D7 1080",
            resBox
        );

        // Keep source resolution checkbox - own row with label and description
        keepSourceResCheck = new CheckBox();
        HBox keepResCheckBox = buildCheckBoxRow(
            keepSourceResCheck,
            "Quellaufösung beibehalten"
        );
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
        HBox keepAudioCheckBox = buildCheckBoxRow(
            keepSourceAudioCheck,
            "Quellaudio beibehalten"
        );
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
        HBox checkBox = buildCheckBoxRow(
            audioNormalizeCheck,
            "Lautstärke normalisieren (EBU R128)"
        );

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
        HBox monoBox = buildCheckBoxRow(
            mixToMonoCheck,
            "Zu MONO heruntermischen"
        );

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

        // Container format
        containerBox = new ChoiceBox<>(
            FXCollections.observableArrayList(VideoContainer.values())
        );
        group
            .getChildren()
            .add(
                buildSettingRow(
                    "Container",
                    "Ausgabe-Containerformat",
                    "mp4",
                    containerBox
                )
            );

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
        HBox checkBox = buildCheckBoxRow(
            fastStartCheck,
            "Fast Start (Moov atom voranstellen)"
        );

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

    /**
     * Builds an HBox containing a CheckBox and a styled label next to it.
     */
    private HBox buildCheckBoxRow(CheckBox checkBox, String labelText) {
        HBox box = new HBox(8);
        box.getChildren().add(checkBox);
        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: " + C_FG + ";");
        box.getChildren().add(label);
        return box;
    }

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
        containerBox.getSelectionModel().select(selectedPreset.container());
        fastStartCheck.setSelected(selectedPreset.fastStart());
        ffmpegPresetBox
            .getSelectionModel()
            .select(selectedPreset.ffmpegPreset());
        tuneBox.getSelectionModel().select(selectedPreset.tune());
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Read current control values back into a new Preset
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Reads the current values from all UI controls and assembles them
     * into a new Preset. This is a mutable copy that reflects user edits.
     */
    public Preset getModifiedPreset() {
        if (selectedPreset == null) {
            return null;
        }

        return new Preset(
            selectedPreset.name(),
            selectedPreset.description(),
            Objects.requireNonNullElse(
                codecBox.getValue(),
                selectedPreset.videoCodec()
            ),
            safeInt(crfField, selectedPreset.crf()),
            keepSourceResCheck.isSelected(),
            safeInt(resWidthField, selectedPreset.resolutionWidth()),
            safeInt(resHeightField, selectedPreset.resolutionHeight()),
            safeDouble(fpsField, selectedPreset.fps()),
            Objects.requireNonNullElse(
                containerBox.getValue(),
                selectedPreset.container()
            ),
            safeInt(maxFileSizeField, selectedPreset.maxFileSize()),
            keepSourceAudioCheck.isSelected(),
            Objects.requireNonNullElse(
                audioCodecBox.getValue(),
                selectedPreset.audioCodec()
            ),
            safeInt(audioBitrateField, selectedPreset.audioBitrate()),
            audioNormalizeCheck.isSelected(),
            mixToMonoCheck.isSelected(),
            fastStartCheck.isSelected(),
            Objects.requireNonNullElse(
                ffmpegPresetBox.getValue(),
                selectedPreset.ffmpegPreset()
            ),
            Objects.requireNonNullElse(
                tuneBox.getValue(),
                selectedPreset.tune()
            )
        );
    }

    private int safeInt(TextField field, int fallback) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private double safeDouble(TextField field, double fallback) {
        try {
            return Double.parseDouble(field.getText().trim());
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
    public void activate(WizardState state) {
        var backButton = state.getBackButton();
        var centerButton = state.getCenterButton();
        var nextButton = state.getNextButton();

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
    public void deactivate(WizardState state) {
        state.setSelectedPreset(getModifiedPreset());
    }
}
