package clientside.controllers;

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

        String username = usernameField.getText();
        if (username.isBlank() || username.equals("")) {
            debugText.setText("Username can't be blank");
            return;
        } else if (username.length() >= 30) {
            debugText.setText("Username too long");
            return;
        }

        try{
            
        }catch(Exception e){

        }



    }
}
