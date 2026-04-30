package de.pottgames.videocompressor.view.step;

import de.pottgames.videocompressor.view.StepView;
import javafx.scene.control.Button;

/**
 * Step 2 of the video compressor wizard: allows the user to select a processing preset.
 * Each preset defines a combination of encoding parameters optimized for different use cases.
 */
public class Step2View implements StepView {

    @Override
    public javafx.scene.Node getNode() {
        return null;
    }

    @Override
    public void activate(
        Button backButton,
        Button centerButton,
        Button nextButton
    ) {
        backButton.setVisible(true);
        backButton.setDisable(false);
        nextButton.setVisible(false);
        nextButton.setDisable(false);
        centerButton.setVisible(false);
    }

    @Override
    public void deactivate() {}
}
