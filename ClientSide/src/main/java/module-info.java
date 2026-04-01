module org.example.clientside {
    requires javafx.controls;
    requires javafx.fxml;
    requires Common;
    requires org.json;

    opens clientside to javafx.fxml;
    exports clientside;
    exports clientside.network;
    exports clientside.controllers;
    opens clientside.controllers to javafx.fxml;
}