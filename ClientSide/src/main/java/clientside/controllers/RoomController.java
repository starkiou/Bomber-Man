package clientside.controllers;

import clientside.SceneManager;
import clientside.network.NetworkManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import network.message.*;

public class RoomController {

    @FXML private Label roomNameLabel;
    @FXML private ListView<String> playerListView;
    @FXML private TextArea chatArea;
    @FXML private TextField chatInputField;
    @FXML private Button readyButton;

    private boolean isReady = false;

    @FXML
    public void initialize() {
        // On écoute les messages entrants pour cette vue
        NetworkManager.getInstance().setMessageHandler(this::onMessageReceived);
    }

    private void onMessageReceived(Message msg) {
        if (msg instanceof RoomStatusMessage) {
            RoomStatusMessage statusMsg = (RoomStatusMessage) msg;

            Platform.runLater(() -> {
                roomNameLabel.setText("Room ID: " + statusMsg.getRoomId());
                playerListView.getItems().clear();

                // Mise à jour de la liste des joueurs
                for (ClientInfoDTO client : statusMsg.getClients()) {
                    String status = client.isReady() ? " [Prêt]" : "";
                    playerListView.getItems().add(client.getPseudo() + status);
                }
            });
        }

        // TODO: Si tu as un ChatMessage dans ton protocole, gère-le ici
        /* else if (msg instanceof ChatMessage) {
            ChatMessage chatMsg = (ChatMessage) msg;
            Platform.runLater(() -> {
                chatArea.appendText("[" + chatMsg.getTime() + "] " + chatMsg.getSender() + " : " + chatMsg.getContent() + "\n");
            });
        } */
    }

    @FXML
    private void onSendMessageClick() {
        String text = chatInputField.getText();
        if (!text.trim().isEmpty()) {
            // TODO: Créer et envoyer un message de chat au serveur
            // NetworkManager.getInstance().sendMessage(new ChatMessage(text));

            // Écho local temporaire pour tester l'interface
            chatArea.appendText("Moi : " + text + "\n");
            chatInputField.clear();
        }
    }

    @FXML
    private void onReadyClick() {
        isReady = !isReady;
        readyButton.setText(isReady ? "Pas prêt" : "Prêt");

        // TODO: Envoyer un message au serveur pour dire qu'on est prêt
        // NetworkManager.getInstance().sendMessage(new ClientReadyMessage(isReady));
    }

    @FXML
    private void onQuitClick() {
        // TODO: Envoyer un message pour quitter la room côté serveur
        // NetworkManager.getInstance().sendMessage(new LeaveRoomMessage());

        // On retire le listener et on retourne à la liste des rooms
        NetworkManager.getInstance().setMessageHandler(null);
        SceneManager.getInstance().loadScene("lobby-list-view.fxml"); // Vérifie le nom exact de ton fichier
    }
}