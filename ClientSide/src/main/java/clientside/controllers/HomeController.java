package clientside.controllers;

import clientside.SceneManager;
import javafx.application.Platform;
import javafx.fxml.FXML;

public class HomeController {

    @FXML
    private void onPlayClick() {
        // Renvoie vers ton écran de connexion/config
        SceneManager.getInstance().loadScene("login-view.fxml");
    }

    @FXML
    private void onSettingsClick() {
        // À créer plus tard
        System.out.println("Ouverture des paramètres...");
        // SceneManager.getInstance().loadScene("settings-view.fxml");
    }

    @FXML
    private void onExitClick() {
        // Ferme proprement l'application JavaFX
        Platform.exit();
        System.exit(0);
    }
}