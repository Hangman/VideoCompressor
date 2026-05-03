package de.pottgames.videocompressor;

import de.pottgames.videocompressor.engine.Preset;
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
}
