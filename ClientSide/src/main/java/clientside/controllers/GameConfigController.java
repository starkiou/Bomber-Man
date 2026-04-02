package clientside.controllers;

import clientside.SceneManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

public class GameConfigController {

    @FXML
    private ComboBox<String> mapSizeBox;

    @FXML
    private Spinner<Integer> botCountSpinner;

    @FXML
    private ComboBox<String> difficultyBox;

    @FXML
    private Button startButton;

    @FXML
    private Button backButton;

    @FXML
    public void initialize() {
        mapSizeBox.getItems().addAll("Petite (10x10)", "Moyenne (15x15)", "Grande (20x20)");
        mapSizeBox.setValue("Moyenne (15x15)");

        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 3, 1);
        botCountSpinner.setValueFactory(valueFactory);

        difficultyBox.getItems().addAll("Facile", "Normal", "Difficile", "Extrême");
        difficultyBox.setValue("Normal");
    }

    @FXML
    void onStartButtonClick(ActionEvent event) {
        String taille = mapSizeBox.getValue();
        int nbBots = botCountSpinner.getValue();
        String diff = difficultyBox.getValue();

        System.out.println("🚀 Lancement de la partie Offline !");
        System.out.println("Carte : " + taille + " | Bots : " + nbBots + " | Difficulté : " + diff);

        SceneManager.getInstance().loadScene("game-board.fxml");
    }

    @FXML
    void onBackButtonClick(ActionEvent event) {
        System.out.println("🔙 Retour au choix de connexion.");
        
        // TODO : Utiliser le SceneManager pour revenir en arrière
       SceneManager.getInstance().loadScene("connection-choice.fxml");
    }
}