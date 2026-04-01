package clientside.controllers;

import clientside.network.NetworkManager; // On importe ton Manager
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.json.JSONObject;
import network.message.Message;
import network.message.MessageFactory;
import network.message.MessageType;

public class ChatController {

    @FXML private TextArea chatDisplay;
    @FXML private TextField inputField;

    @FXML
    public void onSendMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            JSONObject data = new JSONObject();

            // 1. On récupère le pseudo via le NetworkManager
            String myPseudo = NetworkManager.getInstance().getNickname();

            // 2. On ajoute les DEUX clés attendues par le constructeur de ChatMessage
            data.put("sender", myPseudo); // Ajout indispensable
            data.put("content", text);

            // 3. Création et envoi
            Message msg = new MessageFactory().make(MessageType.CHAT, data);
            NetworkManager.getInstance().sendMessage(msg);

            inputField.clear();
        }
    }

    public void appendMessage(String fullMessage) {
        javafx.application.Platform.runLater(() -> {
            chatDisplay.appendText(fullMessage + "\n");
        });
    }
}