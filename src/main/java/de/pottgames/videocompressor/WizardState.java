package de.pottgames.videocompressor;

import de.pottgames.videocompressor.engine.Preset;
import de.pottgames.videocompressor.engine.VideoJob;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.Button;

/**
 * Shared state object that holds wizard session data across all steps.
 * This allows data to flow between Step1View, Step2View, and Step3View
 * without direct coupling between the views.
 */
public class WizardState {

    private final Button backButton;
    private final Button centerButton;
    private final Button nextButton;

    private final List<File> importedFiles = new ArrayList<>();

    private Preset selectedPreset;

    // The original preset that selectedPreset was derived from.
    // Used to detect whether user modifications diverged from the base.
    private Preset basePreset;

    /**
     * The prepared video jobs from Step 3, stored here so that
     * Step 4 can access the source-to-output file mappings and their status.
     */
    private List<VideoJob> preparedJobs;

    public WizardState(
        Button backButton,
        Button centerButton,
        Button nextButton
    ) {
        this.backButton = backButton;
        this.centerButton = centerButton;
        this.nextButton = nextButton;
    }

    public Button getBackButton() {
        return backButton;
    }

    public Button getCenterButton() {
        return centerButton;
    }

    public Button getNextButton() {
        return nextButton;
    }

    /**
     * Returns the list of files imported in Step 1.
     * @return observable list of imported video files
     */
    public List<File> getImportedFiles() {
        return importedFiles;
    }

    /**
     * Returns the preset configuration selected/modified in Step 2.
     * @return the current preset, or null if not yet set
     */
    public Preset getSelectedPreset() {
        return selectedPreset;
    }

    /**
     * Sets the preset configuration.
     * @param preset the preset to store
     */
    public void setSelectedPreset(Preset preset) {
        this.selectedPreset = preset;
    }

    /**
     * Returns the base preset that the selected preset was derived from.
     * @return the base preset, or null if not set
     */
    public Preset getBasePreset() {
        return basePreset;
    }

    /**
     * Sets the base preset.
     * @param preset the base preset to store
     */
    public void setBasePreset(Preset preset) {
        this.basePreset = preset;
    }

    /**
     * Sets the prepared video jobs from Step 3.
     * @param jobs the list of prepared jobs (may be null)
     */
    public void setPreparedJobs(List<VideoJob> jobs) {
        this.preparedJobs = jobs;
    }

    /**
     * Returns the prepared video jobs from Step 3.
     * @return the list of jobs, or null if not yet set
     */
    public List<VideoJob> getPreparedJobs() {
        return preparedJobs;
    }
}
