package clientside.controllers;

import clientside.SceneManager;
import javafx.application.Platform;
import javafx.fxml.FXML;

public class HomeController {

    @FXML
    private void onPlayClick() {
        SceneManager.getInstance().loadScene("login-view.fxml");
    }

    @FXML
    private void onSettingsClick() {
        // TODO: écran de configuration du jeu (non implémenté)
    }

    @FXML
    private void onExitClick() {
        // Ferme l'app
        Platform.exit();
        System.exit(0);
    }
}