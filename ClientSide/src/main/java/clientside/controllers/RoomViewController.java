package clientside.controllers;

import clientside.SceneManager;
import clientside.network.NetworkManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import network.message.ClientInfoDTO;
import network.message.LaunchGameMessage;
import network.message.Message;
import network.message.ReadyUpdateMessage;
import network.message.RoomStatusMessage;

public class RoomViewController {

    @FXML private ListView<String> playerListView;
    @FXML private Label roomInfoLabel;
    @FXML private Button readyButton;
    @FXML private Button launchButton;

    private boolean localReady = false;

    @FXML
    public void initialize() {
        NetworkManager.getInstance().setMessageHandler(this::onMessageReceived);
        roomInfoLabel.setText("Room #" + NetworkManager.getInstance().getCurrentRoomId()
            + (NetworkManager.getInstance().isHost() ? "  (vous êtes le créateur)" : ""));
        // désactivé par défaut, activé quand tous prêts
        launchButton.setDisable(true);
    }

    private void onMessageReceived(Message msg) {
        if (msg instanceof RoomStatusMessage) {
            RoomStatusMessage status = (RoomStatusMessage) msg;
            Platform.runLater(() -> {
                updatePlayerList(status);
                // activer lancer seulement si tous prêts et au moins 1 joueur
                boolean allReady = !status.getClients().isEmpty()
                    && status.getClients().stream().allMatch(ClientInfoDTO::isReady);
                launchButton.setDisable(!allReady);
            });
        } else if (msg instanceof LaunchGameMessage) {
            LaunchGameMessage launch = (LaunchGameMessage) msg;
            // stocker la config pour GameBoardController
            NetworkManager.getInstance().setPendingLaunch(launch);
            Platform.runLater(() -> SceneManager.getInstance().loadScene("game-board.fxml"));
        }
    }

    private void updatePlayerList(RoomStatusMessage status) {
        playerListView.getItems().clear();
        for (ClientInfoDTO client : status.getClients()) {
            String line = String.format("%s  —  %s",
                client.getPseudo() != null ? client.getPseudo() : "Joueur #" + client.getClientId(),
                client.isReady() ? "Prêt" : "Pas prêt");
            playerListView.getItems().add(line);
        }
    }

    @FXML
    private void onReadyClick() {
        localReady = !localReady;
        NetworkManager.getInstance().sendMessage(new ReadyUpdateMessage(localReady));
        readyButton.setText(localReady ? "Annuler le prêt" : "Je suis prêt");
        readyButton.setStyle(localReady
            ? "-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold;"
            : "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
    }

    @FXML
    private void onLaunchClick() {
        // envoie juste le trigger, le serveur vérifie isReadyToLaunch() et renvoie le vrai message
        NetworkManager.getInstance().sendMessage(new LaunchGameMessage());
    }

    @FXML
    private void onLeaveClick() {
        NetworkManager.getInstance().setCurrentRoomId(-1);
        NetworkManager.getInstance().setHost(false);
        NetworkManager.getInstance().setMessageHandler(null);
        SceneManager.getInstance().loadScene("lobby-list.fxml");
    }
}