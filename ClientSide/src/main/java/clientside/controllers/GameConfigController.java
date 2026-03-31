package clientside.controllers;

import clientside.SceneManager;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class GameConfigController {

    @FXML private ComboBox<String> sizePresetCombo;
    @FXML private TextField livesField;
    @FXML private TextField bombsField;
    @FXML private TextField timerField;

    // Paramètres statiques pour être lus par le GameBoardController
    public static int mazeWidth;
    public static int mazeHeight;
    public static int initialLives;
    public static int initialBombs;
    public static double initialTimer;

    @FXML
    public void initialize() {
        // Ajout des presets (Format: Colonnes x Lignes)
        sizePresetCombo.setItems(FXCollections.observableArrayList(
                "15 x 11",
                "21 x 15",
                "31 x 21"
        ));
        sizePresetCombo.getSelectionModel().selectFirst();
    }

    @FXML
    public void onStartGameClick(ActionEvent event) {
        try {
            // Extraction des dimensions du preset "WxH"
            String selected = sizePresetCombo.getSelectionModel().getSelectedItem();
            String[] parts = selected.split(" x ");
            mazeWidth = Integer.parseInt(parts[0]);
            mazeHeight = Integer.parseInt(parts[1]);

            // Récupération des autres paramètres
            initialLives = Integer.parseInt(livesField.getText());
            initialBombs = Integer.parseInt(bombsField.getText());
            initialTimer = Double.parseDouble(timerField.getText());

            // Changement de scène
            SceneManager.getInstance().loadScene("game-board.fxml");

            // Mise en plein écran (Maximisé)
            Stage stage = (Stage) sizePresetCombo.getScene().getWindow();
            stage.setMaximized(true);

        } catch (Exception e) {
            System.err.println("Error in configuration: " + e.getMessage());
        }
    }
}