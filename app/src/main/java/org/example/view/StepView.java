package org.example.view;

import javafx.scene.Node;

/**
 * Interface for all wizard step views in the video compressor app.
 * Each step must provide a JavaFX Node to be displayed in the center of the main layout.
 */
public interface StepView {

    /**
     * Returns the JavaFX Node that represents this step's view.
     * @return the Node to display in the wizard
     */
    Node getNode();
}
