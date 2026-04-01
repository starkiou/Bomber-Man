package clientside.controllers;

import clientside.SceneManager;
import clientside.network.NetworkManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import network.message.GetRoomListUpdateMessage;
import network.message.RoomListUpdateMessage;
import network.message.RoomInfoDTO;
import network.message.Message;

public class LobbyListController {

    @FXML private ListView<String> roomListView;

    @FXML
    public void initialize() {
        NetworkManager.getInstance().setMessageHandler(this::onMessageReceived);
        onRefreshClick();
    }

    private void onMessageReceived(Message msg) {
        // On verif le type du message

        // SI UPDATE DE LA LISTE DES ROOMS
        if (msg instanceof RoomListUpdateMessage) {
            RoomListUpdateMessage roomMsg = (RoomListUpdateMessage) msg;

            Platform.runLater(() -> {
                roomListView.getItems().clear();
                for (RoomInfoDTO room : roomMsg.getRooms()) {
                    String displayText = String.format("Room: %s [%d/%d] - %s",
                            room.getRoomName(),
                            room.getCurrentPlayers(),
                            room.getMaxPlayers(),
                            room.isInGame() ? "En jeu" : "En attente");

                    roomListView.getItems().add(displayText);
                }
            });
        }

        //TODO AUTRES TYPES
    }

    @FXML
    private void onRefreshClick() {
        NetworkManager.getInstance().sendMessage(new GetRoomListUpdateMessage());
    }

    @FXML
    private void onCreateRoomClick() {
        System.out.println("Aller vers création de room...");
    }

    @FXML
    private void onBackClick() {
        NetworkManager.getInstance().setMessageHandler(null);
        SceneManager.getInstance().loadScene("login-view.fxml");
    }
}