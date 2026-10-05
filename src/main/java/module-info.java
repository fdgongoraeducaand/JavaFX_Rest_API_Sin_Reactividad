module com.example.javafx_sinreactividad {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;

    opens com.example.javafx_sinreactividad.modelos to com.fasterxml.jackson.databind;
    opens com.example.javafx_sinreactividad to javafx.fxml;
    exports com.example.javafx_sinreactividad;
}