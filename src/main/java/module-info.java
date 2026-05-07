module de.pottgames.videocompressor {
// JavaFX modules
    requires transitive javafx.controls;

    // AtlantaFX Base for styling
    requires atlantafx.base;

    // Standard Java modules
    requires java.desktop;
    requires javafx.base;
    // Jackson 3.x automatic modules for JSON parsing
    requires tools.jackson.databind;

    // Export packages for application use
    exports de.pottgames.videocompressor;
    exports de.pottgames.videocompressor.engine;
    exports de.pottgames.videocompressor.i18n;
    exports de.pottgames.videocompressor.view;
    exports de.pottgames.videocompressor.view.cell;
    exports de.pottgames.videocompressor.view.step;

    // Open packages for JavaFX FXML reflection
    opens de.pottgames.videocompressor to javafx.fxml;
    opens de.pottgames.videocompressor.view to javafx.fxml;
    opens de.pottgames.videocompressor.view.cell to javafx.fxml;
    opens de.pottgames.videocompressor.view.step to javafx.fxml;
}
