package de.pottgames.videocompressor.view;

import de.pottgames.videocompressor.WizardState;
import javafx.scene.Node;

/**
 * Interface for all wizard step views in the video compressor app.
 * Each step must provide a JavaFX Node to be displayed in the center of the main layout.
 */
public interface StepView {
    /**
     * Is being called when the StepView becomes active/is shown.
     */
    void activate(WizardState state);

    /**
     * Is being called when the StepView becomes inactive/is hidden.
     */
    void deactivate(WizardState state);

    /**
     * Returns the JavaFX Node that represents this step's view.
     * @return the Node to display in the wizard
     */
    Node getNode();
}
