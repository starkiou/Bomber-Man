package clientside.controllers;

import clientside.SceneManager;
import clientside.network.NetworkManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import network.message.*;

public class RoomController {

    @FXML private Label roomNameLabel;
    @FXML private Label mapInfoLabel;
    @FXML private Label difficultyInfoLabel;
    @FXML private Label botInfoLabel;
    @FXML private ListView<String> playerListView;
    @FXML private TextArea chatArea;
    @FXML private TextField chatInputField;
    @FXML private Button readyButton;

    private boolean isReady = false;

    @FXML
    public void initialize() {
        // On écoute les messages entrants
        NetworkManager.getInstance().setMessageHandler(this::onMessageReceived);

        // Message d'accueil dans le chat
        chatArea.appendText("[Système] Bienvenue dans le lobby !\n");
    }

    private void onMessageReceived(Message msg) {
        if (msg instanceof RoomStatusMessage statusMsg) {

            // SI LA PARTIE COMMENCE (isWaiting devient false sur le serveur)
            if (!statusMsg.isWaiting()) {
                Platform.runLater(() -> {
                    System.out.println("🚀 La partie commence !");
                    NetworkManager.getInstance().setMessageHandler(null);
                    SceneManager.getInstance().loadScene("game-board.fxml");
                });
                return;
            }

            // MISE À JOUR DE L'INTERFACE DU LOBBY
            Platform.runLater(() -> {
                // 1. Infos de la Room (nom, map, etc.)
                roomNameLabel.setText("Salon : " + statusMsg.getRoomName());

                // Si ton DTO contient ces infos, décommente les lignes suivantes :
                // mapInfoLabel.setText("Carte : " + statusMsg.getMapName());
                // difficultyInfoLabel.setText("Difficulté : " + statusMsg.getDifficulty());

                // 2. Liste des joueurs
                playerListView.getItems().clear();
                for (ClientInfoDTO client : statusMsg.getClients()) {
                    String statusText = client.isReady() ? " ✅ [PRÊT]" : " ⏳ [ATTENTE]";
                    playerListView.getItems().add(client.getPseudo() + statusText);
                }
            });
        }
        // Gestion du Chat (Si tu as implémenté ChatMessage)
        /* else if (msg instanceof ChatMessage chatMsg) {
            Platform.runLater(() -> {
                chatArea.appendText(chatMsg.getSender() + " : " + chatMsg.getContent() + "\n");
            });
        } */
    }

    @FXML
    private void onReadyClick() {
        //todo
    }

    @FXML
    private void onSendMessageClick() {
        String text = chatInputField.getText();
        if (text != null && !text.trim().isEmpty()) {
            // Optionnel : Envoyer au serveur si le ChatMessage est prêt
            // NetworkManager.getInstance().sendMessage(new ChatMessage(text));

            // Affichage local pour test
            chatArea.appendText("Moi : " + text + "\n");
            chatInputField.clear();
        }
    }

    @FXML
    private void onQuitClick() {
        System.out.println("Quitter la room...");

        NetworkManager.getInstance().sendMessage(new RoomQuitMessage());

        NetworkManager.getInstance().setMessageHandler(null);
        SceneManager.getInstance().loadScene("lobby-list.fxml");
    }
}