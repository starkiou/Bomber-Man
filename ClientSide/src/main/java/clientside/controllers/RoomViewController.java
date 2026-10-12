package clientside.controllers;

import clientside.SceneManager;
import clientside.network.NetworkManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import network.message.ClientInfoDTO;
import network.message.ClientReadyMessage;
import network.message.LaunchGameMessage;
import network.message.Message;
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
            + (NetworkManager.getInstance().isHost() ? "  (vous êtes l'hôte)" : ""));
        // désactivé par défaut, activé quand tous prêts
        launchButton.setDisable(true);
        launchButton.setVisible(NetworkManager.getInstance().isHost());
        launchButton.setManaged(NetworkManager.getInstance().isHost());
    }

    private void onMessageReceived(Message msg) {
        if (msg instanceof RoomStatusMessage) {
            RoomStatusMessage status = (RoomStatusMessage) msg;
            Platform.runLater(() -> {
                updatePlayerList(status);
                // L'hôte peut changer si le créateur quitte la room
                boolean host = isLocalPlayerHost(status);
                NetworkManager.getInstance().setHost(host);
                roomInfoLabel.setText("Room #" + NetworkManager.getInstance().getCurrentRoomId()
                    + (host ? "  (vous êtes l'hôte)" : ""));
                // activer lancer seulement pour l'hôte, si tous prêts et au moins 1 joueur
                boolean allReady = !status.getClients().isEmpty()
                    && status.getClients().stream().allMatch(ClientInfoDTO::isReady);
                launchButton.setVisible(host);
                launchButton.setManaged(host);
                launchButton.setDisable(!allReady);
            });
        } else if (msg instanceof LaunchGameMessage) {
            LaunchGameMessage launch = (LaunchGameMessage) msg;
            // stocker la config pour GameBoardController
            NetworkManager.getInstance().setPendingLaunch(launch);
            Platform.runLater(() -> SceneManager.getInstance().loadScene("game-board.fxml"));
        }
    }

    /** Le serveur désigne l'hôte par son identifiant ; on se reconnaît par le pseudo, comme en jeu. */
    private boolean isLocalPlayerHost(RoomStatusMessage status) {
        String myNick = NetworkManager.getInstance().getNickname();
        if (myNick == null) return false;
        for (ClientInfoDTO client : status.getClients()) {
            if (client.getClientId() == status.getHostId()) {
                return client.getPseudo() != null && myNick.trim().equalsIgnoreCase(client.getPseudo().trim());
            }
        }
        return false;
    }

    private void updatePlayerList(RoomStatusMessage status) {
        playerListView.getItems().clear();
        for (ClientInfoDTO client : status.getClients()) {
            String line = String.format("%s%s  —  %s",
                client.getPseudo() != null ? client.getPseudo() : "Joueur #" + client.getClientId(),
                client.getClientId() == status.getHostId() ? "  (hôte)" : "",
                client.isReady() ? "Prêt" : "Pas prêt");
            playerListView.getItems().add(line);
        }
    }

    @FXML
    private void onReadyClick() {
        localReady = !localReady;
        NetworkManager.getInstance().sendMessage(new ClientReadyMessage(localReady));
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