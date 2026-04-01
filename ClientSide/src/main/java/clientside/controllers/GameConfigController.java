package clientside.controllers;

import clientside.SceneManager;
import clientside.network.NetworkManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import network.message.Message;
import network.message.RoomAcceptedCreationMessage;
import network.message.RoomCreationMessage;

public class GameConfigController {

    @FXML private Spinner<Integer> playerCountSpinner;
    @FXML private Spinner<Integer> botCountSpinner;
    @FXML private ComboBox<String> difficultySelector;
    @FXML private ComboBox<String> mapSelector;

    private static boolean isOnlineMode = false;

    public static void setOnlineMode(boolean online) {
        isOnlineMode = online;
    }

    @FXML
    public void initialize() {
        if (mapSelector != null) {
            mapSelector.getItems().addAll("Petite (10x10)", "Moyenne (15x15)", "Grande (20x20)");
            mapSelector.setValue("Moyenne (15x15)");
        }

        if (difficultySelector != null) {
            difficultySelector.getItems().addAll("Facile", "Normal", "Difficile", "Extrême");
            difficultySelector.setValue("Normal");
        }
    }

    @FXML
    void onStartButtonClick(ActionEvent event) {
        if (isOnlineMode) {
            handleOnlineCreation();
            // ⚠️ SURTOUT PAS de SceneManager ici, on attend le serveur.
        } else {
            handleOfflineStart();
        }
    }

    private void handleOfflineStart() {
        System.out.println("🚀 Lancement Offline direct !");
        // En mode solo, on va directement au jeu
        SceneManager.getInstance().loadScene("game-board.fxml");
    }

    private void handleOnlineCreation() {
        int maxPlayers = playerCountSpinner.getValue();
        String roomName = "Room de " + NetworkManager.getInstance().getNickname();

        System.out.println("🌐 Envoi de la demande de création...");

        // 1. On prévient le serveur
        NetworkManager.getInstance().sendMessage(new RoomCreationMessage(maxPlayers, roomName));

        // 2. On configure l'écouteur pour changer d'écran dès que le serveur répond
        NetworkManager.getInstance().setMessageHandler(msg -> {
            System.out.println("DEBUG: Reçu pendant config -> " + msg.getMessageType());

            // Si c'est un succès OU un statut de room, c'est que la room existe !
            if (msg instanceof RoomAcceptedCreationMessage || msg.getMessageType().toString().contains("ROOM_STATUS")) {

                // On nettoie le handler pour que le RoomController puisse prendre la suite
                NetworkManager.getInstance().setMessageHandler(null);

                Platform.runLater(() -> {
                    System.out.println("✅ Room confirmée, chargement de room-view.fxml");
                    SceneManager.getInstance().loadScene("room-view.fxml");
                });
            }
        });
    }

    private void onMessageReceived(Message msg) {
        if (msg instanceof RoomAcceptedCreationMessage) {
            Platform.runLater(() -> {
                System.out.println("✅ Room acceptée, passage au lobby...");
                SceneManager.getInstance().loadScene("room-view.fxml");
            });
        }
    }

    @FXML
    void onBackButtonClick(ActionEvent event) {
        if (isOnlineMode) {
            SceneManager.getInstance().loadScene("lobby-list.fxml");
        } else {
            SceneManager.getInstance().loadScene("login-view.fxml");
        }
    }
}