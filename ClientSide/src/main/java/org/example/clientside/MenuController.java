package org.example.clientside;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

public class MenuController {

    public TextField usernameField;

    @FXML
    private Label debugText;

    @FXML
    public void onConnectButtonClick(ActionEvent actionEvent) {
        debugText.setText("Connexion en cours...");
        debugText.setTextFill(Color.RED);
    }
}
