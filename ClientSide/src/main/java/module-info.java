module org.example.clientside {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.clientside to javafx.fxml;
    exports org.example.clientside;
}