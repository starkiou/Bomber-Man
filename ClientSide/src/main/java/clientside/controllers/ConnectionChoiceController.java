package clientside.controllers;

import clientside.SceneManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

public class ConnectionChoiceController {

    @FXML private Label statusLabel;
    @FXML private Button onlineButton;

    @FXML
    public void initialize() {
        // Au chargement, on vérifie la variable statique du premier controller
        if (MenuController.isConnected) {
            statusLabel.setText("Connected to server");
            statusLabel.setTextFill(Color.GREEN);
            onlineButton.setDisable(false); // Bouton cliquable
        } else {
            statusLabel.setText("Not connected");
            statusLabel.setTextFill(Color.RED);
            onlineButton.setDisable(true);  // Bouton grisé
        }
    }

    @FXML
    public void onOnlineClick(ActionEvent event) {
        SceneManager.getInstance().loadScene("lobby-list.fxml");
    }

    @FXML
    public void onOfflineClick(ActionEvent event) {
        SceneManager.getInstance().loadScene("game-board.fxml");
    }
}