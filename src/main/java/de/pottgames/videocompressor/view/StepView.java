package de.pottgames.videocompressor.view;

import javafx.scene.Node;
import javafx.scene.control.Button;

/**
 * Interface for all wizard step views in the video compressor app.
 * Each step must provide a JavaFX Node to be displayed in the center of the main layout.
 */
public interface StepView {
    /**
     * Is being called when the StepView becomes active/is shown.
     */
    void activate(Button backButton, Button centerButton, Button nextButton);

    /**
     * Is being called when the StepView becomes inactive/is hidden.
     */
    void deactivate();

    /**
     * Returns the JavaFX Node that represents this step's view.
     * @return the Node to display in the wizard
     */
    Node getNode();
}
