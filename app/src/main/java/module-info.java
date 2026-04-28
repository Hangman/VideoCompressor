module org.example {
// JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;

    // Atlantafx Base for styling
    requires atlantafx.base;

    // Standard Java modules
    requires java.desktop;
    requires java.logging;

    // Export packages for application use
    exports org.example;
    exports org.example.view;
    exports org.example.view.cell;
    exports org.example.view.step;

    // Open packages for JavaFX FXML reflection
    opens org.example to javafx.fxml;
    opens org.example.view to javafx.fxml;
    opens org.example.view.cell to javafx.fxml;
    opens org.example.view.step to javafx.fxml;
}
