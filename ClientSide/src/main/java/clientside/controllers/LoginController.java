package clientside.controllers;

import clientside.SceneManager;
import clientside.network.NetworkManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;
import javafx.scene.paint.Color;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private TextField ipAddressField;
    @FXML private TextField serverPortField;
    @FXML private Label debugText;
    @FXML private Button playOnlineButton;
    @FXML private TilePane spriteContainer;

    private String selectedSprite = "default_bomber.png";

    @FXML
    public void initialize() {
        ipAddressField.setText("");
        serverPortField.setText("3000");

        if (NetworkManager.getInstance().isConnected()) {
            playOnlineButton.setDisable(false);
            updateDebugStatus("Déjà connecté !", Color.GREEN);
        }
    }

    @FXML
    public void onConnectButtonClick() {
        String username = usernameField.getText();
        String ipAddress = ipAddressField.getText();
        String portStr = serverPortField.getText();

        if (username.isBlank() || ipAddress.isBlank() || portStr.isBlank()) {
            updateDebugStatus("Champs incomplets !", Color.RED);
            return;
        }

        updateDebugStatus("Connexion...", Color.BLUE);

        Task<Boolean> connectionTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                // On laisse l'exception remonter pour pouvoir afficher la cause réelle.
                int port = Integer.parseInt(portStr);
                NetworkManager.getInstance().connect(ipAddress, port, username);
                return true;
            }
        };

        connectionTask.setOnSucceeded(e -> {
            updateDebugStatus("Connecté !", Color.GREEN);
            playOnlineButton.setDisable(false); // Active le bouton Online
        });

        connectionTask.setOnFailed(e -> {
            Throwable ex = connectionTask.getException();
            String reason = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "erreur inconnue";
            updateDebugStatus("Échec connexion : " + reason, Color.RED);
        });

        new Thread(connectionTask).start();
    }

    @FXML
    private void onPlayOnlineClick() {
        if (NetworkManager.getInstance().isConnected()) {
            SceneManager.getInstance().loadScene("lobby-list.fxml");
        }
    }

    @FXML
    private void onPlayOfflineClick() {
        String username = usernameField.getText().isBlank() ? "Player1" : usernameField.getText();
        NetworkManager.getInstance().setNickname(username);

        SceneManager.getInstance().loadScene("game-config.fxml");
    }

    @FXML
    private void onBackClick() {
        // Déconnecte le joueur si besoin avant de quitter
        if (NetworkManager.getInstance().isConnected()) {
            NetworkManager.getInstance().disconnect();
        }
        SceneManager.getInstance().loadScene("home-view.fxml");
    }

    private void updateDebugStatus(String message, Color color) {
        debugText.setText(message);
        debugText.setTextFill(color);
    }
}