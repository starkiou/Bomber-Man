package clientside.controllers;

import clientside.SceneManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

public class GameConfigController {

    @FXML private ComboBox<String> mapSizeBox;
    @FXML private Spinner<Integer> botCountSpinner;
    @FXML private ComboBox<String> difficultyBox;
    @FXML private Spinner<Integer> bombCountSpinner;
    @FXML private Spinner<Integer> timeSpinner;

    @FXML private Button startButton;
    @FXML private Button backButton;

    // Variables statiques pour transmettre les données au plateau de jeu
    public static int selectedBombs = 3;
    public static int selectedTime = 120;
    public static int selectedBots = 1;
    public static String selectedMapSize = "Moyenne (15x15)";
    public static String selectedDifficulty = "Normal";

    @FXML
    public void initialize() {
        // --- 1. Remplissage des ComboBox (Taille et Difficulté) ---
        mapSizeBox.getItems().clear();
        mapSizeBox.getItems().addAll("Petite (11x11)", "Moyenne (15x15)", "Grande (19x19)");
        mapSizeBox.setValue("Moyenne (15x15)");

        difficultyBox.getItems().clear();
        difficultyBox.getItems().addAll("Facile", "Normal", "Difficile", "Extrême");
        difficultyBox.setValue("Normal");

        // --- 2. Initialisation des Spinners (Indispensable pour l'affichage) ---
        // Format : new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, defaut)
        botCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 3, 1));
        bombCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 3));
        timeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(30, 600, 120));

        // Permet de taper directement au clavier dans les champs
        bombCountSpinner.setEditable(true);
        timeSpinner.setEditable(true);
    }

    @FXML
    void onStartButtonClick(ActionEvent event) {
        // On récupère toutes les valeurs avant de changer de scène
        selectedBots = botCountSpinner.getValue();
        selectedBombs = bombCountSpinner.getValue();
        selectedTime = timeSpinner.getValue();
        selectedMapSize = mapSizeBox.getValue();
        selectedDifficulty = difficultyBox.getValue();

        SceneManager.getInstance().loadScene("game-board.fxml");
    }

    @FXML
    void onBackButtonClick(ActionEvent event) {
        SceneManager.getInstance().loadScene("login-view.fxml");
    }
}