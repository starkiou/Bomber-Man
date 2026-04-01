package clientside.controllers;

import clientside.SceneManager;
import clientside.network.NetworkManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import network.message.*;

public class LobbyListController {

    // On stocke les objets DTO directement
    @FXML private ListView<RoomInfoDTO> roomListView;

    @FXML
    public void initialize() {
        NetworkManager.getInstance().setMessageHandler(this::onMessageReceived);

        // Dis à la ListView comment afficher un RoomInfoDTO
        roomListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(RoomInfoDTO room, boolean empty) {
                super.updateItem(room, empty);
                if (empty || room == null) {
                    setText(null);
                } else {
                    setText(String.format("Room: %s [%d/%d] - %s",
                            room.getRoomName(),
                            room.getCurrentPlayers(),
                            room.getMaxPlayers(),
                            room.isInGame() ? "En jeu" : "En attente"));
                }
            }
        });

        onRefreshClick();
    }

    private void onMessageReceived(Message msg) {
        if (msg instanceof RoomListUpdateMessage) {
            RoomListUpdateMessage roomMsg = (RoomListUpdateMessage) msg;
            Platform.runLater(() -> {
                roomListView.getItems().clear();
                // On ajoute les objets, la CellFactory s'occupe du texte
                roomListView.getItems().addAll(roomMsg.getRooms());
            });
        }
    }

    @FXML
    public void onJoinRoomClick(ActionEvent actionEvent) {
        RoomInfoDTO selected = roomListView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            System.out.println("⚠️ Aucune room sélectionnée !");
            return;
        }

        if (selected.isInGame()) {
            System.out.println("❌ Cette partie a déjà commencé.");
            return;
        }

        System.out.println("Rejoint la room : " + selected.getRoomId());

        // Envoi du message de demande de jonction (assure-toi que cette classe existe)
        NetworkManager.getInstance().sendMessage(new RoomJoiningMessage(selected.getRoomId()));

        // On change le handler pour attendre le statut de la room rejointe
        NetworkManager.getInstance().setMessageHandler(msg -> {
            if (msg instanceof RoomStatusMessage) {
                Platform.runLater(() -> {
                    SceneManager.getInstance().loadScene("room-view.fxml");
                });
            }
        });
    }

    @FXML
    private void onRefreshClick() {
        NetworkManager.getInstance().sendMessage(new GetRoomListUpdateMessage());
    }

    @FXML
    private void onCreateRoomClick() {
        GameConfigController.setOnlineMode(true);
        SceneManager.getInstance().loadScene("game-conf-view.fxml");
    }

    @FXML
    private void onBackClick() {
        NetworkManager.getInstance().setMessageHandler(null);
        SceneManager.getInstance().loadScene("login-view.fxml");
    }
}