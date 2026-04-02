package clientside.controllers;

import clientside.SceneManager;
import clientside.network.NetworkManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import network.message.Message;
import network.message.RoomAcceptedCreationMessage;
import network.message.RoomCreationMessage;
import network.message.RoomRefusedCreationMessage;

/**
 * Écran de configuration avant création d'une room en ligne.
 * Envoie RoomCreationMessage avec les paramètres choisis, puis
 * attend la confirmation du serveur pour retourner à la liste.
 */
public class RoomCreationConfigController {

    @FXML private TextField roomNameField;
    @FXML private ComboBox<String> mapSizeBox;
    @FXML private Spinner<Integer> botCountSpinner;
    @FXML private ComboBox<String> difficultyBox;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        mapSizeBox.getItems().addAll("Petite (10x10)", "Moyenne (15x15)", "Grande (20x20)");
        mapSizeBox.setValue("Moyenne (15x15)");
        difficultyBox.getItems().addAll("Facile", "Normal", "Difficile", "Extrême");
        difficultyBox.setValue("Normal");
        botCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 3, 1));

        NetworkManager.getInstance().setMessageHandler(this::onMessageReceived);
    }

    private void onMessageReceived(Message msg) {
        if (msg instanceof RoomAcceptedCreationMessage) {
            RoomAcceptedCreationMessage accepted = (RoomAcceptedCreationMessage) msg;
            // STORE roomId + isHost avant de naviguer
            NetworkManager.getInstance().setCurrentRoomId(accepted.getIdRoom());
            NetworkManager.getInstance().setHost(true);
            Platform.runLater(() -> SceneManager.getInstance().loadScene("room-view.fxml"));
        } else if (msg instanceof RoomRefusedCreationMessage) {
            RoomRefusedCreationMessage refused = (RoomRefusedCreationMessage) msg;
            Platform.runLater(() -> errorLabel.setText("Refusé : " + refused.getCause()));
        }
    }

    @FXML
    private void onCreateClick() {
        String name = roomNameField.getText().isBlank() ? "Room" : roomNameField.getText().trim();
        String mapSize = mapSizeBox.getValue();
        String difficulty = difficultyBox.getValue();
        int botCount = botCountSpinner.getValue();

        errorLabel.setText("");
        // NOTE maxPlayer fixé à 4, à exposer en option si besoin
        NetworkManager.getInstance().sendMessage(new RoomCreationMessage(4, name, mapSize, difficulty, botCount));
    }

    @FXML
    private void onBackClick() {
        NetworkManager.getInstance().setMessageHandler(null);
        SceneManager.getInstance().loadScene("lobby-list.fxml");
    }
}
