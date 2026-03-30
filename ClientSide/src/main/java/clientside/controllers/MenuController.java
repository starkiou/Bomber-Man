package clientside.controllers;

import clientside.SceneManager;
import clientside.network.NetworkManager;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class MenuController {
    // Variable simple et fondamentale pour stocker l'état
    public static boolean isConnected = false;

    private static final int MAX_USERNAME_LENGTH = 30;
    private static final int RETRY_DURATION_MS = 3000;
    private static final int SLEEP_BETWEEN_RETRY = 500;
    private static final double REDIRECT_DELAY = 1.0;

    @FXML public TextField usernameField;
    @FXML private Label debugText;

    @FXML
    public void onConnectButtonClick(ActionEvent actionEvent) {
        String username = usernameField.getText();
        if (username == null || username.isBlank()) {
            updateDebugStatus("Username can't be blank!", Color.RED);
            return;
        }

        updateDebugStatus("Connecting...", Color.BLUE);

        Task<Boolean> connectionTask = new Task<>() {
            @Override
            protected Boolean call() {
                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < RETRY_DURATION_MS) {
                    try {
                        NetworkManager.getInstance().connect("127.0.0.1", 12345, username);
                        return true;
                    } catch (Exception e) {
                        try { Thread.sleep(SLEEP_BETWEEN_RETRY); } catch (InterruptedException ie) { return false; }
                    }
                }
                return false;
            }
        };

        connectionTask.setOnSucceeded(e -> {
            isConnected = connectionTask.getValue(); // On stocke le résultat ici
            if (isConnected) {
                SceneManager.getInstance().loadScene("connection-choice.fxml");
            } else {
                updateDebugStatus("Connection failed. Entering anyway...", Color.RED);
                PauseTransition delay = new PauseTransition(Duration.seconds(REDIRECT_DELAY));
                delay.setOnFinished(event -> SceneManager.getInstance().loadScene("connection-choice.fxml"));
                delay.play();
            }
        });
        new Thread(connectionTask).start();
    }

    private void updateDebugStatus(String message, Color color) {
        debugText.setText(message);
        debugText.setTextFill(color);
    }
}