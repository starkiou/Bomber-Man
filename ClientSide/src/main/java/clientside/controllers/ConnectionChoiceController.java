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
        // Sécurité pour éviter le crash si les ID FXML sont manquants
        if (statusLabel == null || onlineButton == null) return;

        if (MenuController.isConnected) {
            statusLabel.setText("Connected to server");
            statusLabel.setTextFill(Color.GREEN);
            onlineButton.setDisable(false);
        } else {
            statusLabel.setText("Not connected");
            statusLabel.setTextFill(Color.RED);
            onlineButton.setDisable(true);
        }
    }

    @FXML
    public void onOnlineClick(ActionEvent event) {
        SceneManager.getInstance().loadScene("lobby-list.fxml");
    }

    @FXML
    public void onOfflineClick(ActionEvent event) {
        // Cette ligne charge la vue du plateau de jeu
        // C'est le GameBoardController qui dessinera le terrain au lancement
        SceneManager.getInstance().loadScene("game-board.fxml");
    }
}