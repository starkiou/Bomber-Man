package clientside.controllers;

import java.util.ArrayList;
import java.util.List;

import clientside.SceneManager;
import clientside.network.NetworkManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import network.message.GetRoomListUpdateMessage;
import network.message.Message;
import network.message.RoomAcceptedJoinMessage;
import network.message.RoomInfoDTO;
import network.message.RoomJoiningMessage;
import network.message.RoomListUpdateMessage;

/**
 * Affiche la liste des rooms disponibles.
 * Permet de créer une room (via l'écran de config) ou d'en rejoindre une.
 */
public class LobbyListController {

    @FXML private ListView<String> roomListView;
    @FXML private Button joinButton;

    // STORE parallel à roomListView pour retrouver l'id à partir de la sélection
    private List<RoomInfoDTO> currentRooms = new ArrayList<>();

    @FXML
    public void initialize() {
        NetworkManager.getInstance().setMessageHandler(this::onMessageReceived);
        joinButton.setDisable(true);
        roomListView.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) ->
            joinButton.setDisable(newVal.intValue() < 0)
        );
        onRefreshClick();
    }

    private void onMessageReceived(Message msg) {
        if (msg instanceof RoomListUpdateMessage) {
            RoomListUpdateMessage roomMsg = (RoomListUpdateMessage) msg;
            Platform.runLater(() -> {
                currentRooms.clear();
                roomListView.getItems().clear();
                for (RoomInfoDTO room : roomMsg.getRooms()) {
                    currentRooms.add(room);
                    roomListView.getItems().add(buildDisplayText(room));
                }
            });
        } else if (msg instanceof RoomAcceptedJoinMessage) {
            RoomAcceptedJoinMessage accepted = (RoomAcceptedJoinMessage) msg;
            NetworkManager.getInstance().setCurrentRoomId(accepted.getIdRoom());
            NetworkManager.getInstance().setHost(false);
            Platform.runLater(() -> SceneManager.getInstance().loadScene("room-view.fxml"));
        }
    }

    private String buildDisplayText(RoomInfoDTO room) {
        return String.format("%s  [%d/%d]  |  %s  |  Diff: %s  |  Bots: %d  |  %s",
            room.getRoomName(),
            room.getCurrentPlayers(),
            room.getMaxPlayers(),
            room.getMapSize(),
            room.getDifficulty(),
            room.getBotCount(),
            room.isInGame() ? "En jeu" : "En attente");
    }

    @FXML
    private void onRefreshClick() {
        NetworkManager.getInstance().sendMessage(new GetRoomListUpdateMessage());
    }

    @FXML
    private void onCreateRoomClick() {
        NetworkManager.getInstance().setMessageHandler(null);
        SceneManager.getInstance().loadScene("room-creation-config.fxml");
    }

    @FXML
    private void onJoinClick() {
        int index = roomListView.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= currentRooms.size()) return;
        RoomInfoDTO selected = currentRooms.get(index);
        if (selected.isInGame() || selected.isFull()) return;
        NetworkManager.getInstance().sendMessage(new RoomJoiningMessage(selected.getRoomId()));
    }

    @FXML
    private void onBackClick() {
        NetworkManager.getInstance().setMessageHandler(null);
        SceneManager.getInstance().loadScene("login-view.fxml");
    }
}
