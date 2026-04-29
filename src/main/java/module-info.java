module de.pottgames.videocompressor {
// JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;

    // Atlantafx Base for styling
    requires atlantafx.base;

    // Standard Java modules
    requires java.desktop;
    requires java.logging;
    requires javafx.base;
    // Jackson 3.x automatic modules for JSON parsing
    requires tools.jackson.databind;
    requires tools.jackson.core;
    requires com.fasterxml.jackson.annotation;

    // Export packages for application use
    exports de.pottgames.videocompressor;
    exports de.pottgames.videocompressor.view;
    exports de.pottgames.videocompressor.view.cell;
    exports de.pottgames.videocompressor.view.step;

    // Open packages for JavaFX FXML reflection
    opens de.pottgames.videocompressor to javafx.fxml;
    opens de.pottgames.videocompressor.view to javafx.fxml;
    opens de.pottgames.videocompressor.view.cell to javafx.fxml;
    opens de.pottgames.videocompressor.view.step to javafx.fxml;
}
