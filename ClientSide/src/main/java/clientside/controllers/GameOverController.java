package clientside.controllers;

import clientside.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class GameOverController {
    @FXML private Label resultTitle;
    @FXML private Label timeLabel;
    @FXML private Label killsLabel;

    public void setStats(String title, String time, int kills) {
        resultTitle.setText(title);
        timeLabel.setText(time);
        killsLabel.setText(String.valueOf(kills));

        if (title.contains("PERDU")) resultTitle.setStyle("-fx-text-fill: #ff5555;");
        else resultTitle.setStyle("-fx-text-fill: #55ff55;");
    }

    @FXML
    private void onReturnMenu() {
        SceneManager.getInstance().loadScene("home-view.fxml");
    }
}