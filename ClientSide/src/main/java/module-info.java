module org.example.clientside {
    requires javafx.controls;
    requires javafx.fxml;


    opens clientside to javafx.fxml;
    exports clientside;
    exports clientside.network;
    opens clientside.network to javafx.fxml;
    exports clientside.controllers;
    opens clientside.controllers to javafx.fxml;
}