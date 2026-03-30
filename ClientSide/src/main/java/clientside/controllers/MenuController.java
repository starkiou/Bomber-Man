package clientside.controllers;

import clientside.SceneManager;
import clientside.network.NetworkManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

public class MenuController {

    public TextField usernameField;

    @FXML
    private Label debugText;

    @FXML
    public void onConnectButtonClick(ActionEvent actionEvent) {
        debugText.setText("Connexion in progress...");
        debugText.setTextFill(Color.RED);

        String username = usernameField.getText();
        if (username.isBlank() || username.equals("")) {
            showError("Erreur", "Username can't be blank !");
            return;
        } else if (username.length() >= 30) {
            showError("Erreur", "Username too long");
            return;
        }

        try{
            NetworkManager.getInstance().connect("127.0.0.1", 12345, username);
            SceneManager.getInstance().loadScene("lobby-list.fxml");
        }catch(Exception e){
            showError("Erreur", e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
