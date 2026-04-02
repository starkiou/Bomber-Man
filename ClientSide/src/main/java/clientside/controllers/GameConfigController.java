package clientside.controllers;

import clientside.SceneManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.aiPlayer.Strategy;

import java.util.ArrayList;
import java.util.List;

public class GameConfigController {

    @FXML private ComboBox<String> mapSizeBox;
    @FXML private Spinner<Integer> botCountSpinner;
    @FXML private ComboBox<String> difficultyBox;
    @FXML private Spinner<Integer> bombCountSpinner;
    @FXML private Spinner<Integer> timeSpinner;
    @FXML private VBox botStrategyContainer;

    @FXML private Button startButton;
    @FXML private Button backButton;

    // Variables statiques pour transmettre les données au plateau de jeu
    public static int selectedBombs = 3;
    public static int selectedTime = 120;
    public static int selectedBots = 1;
    public static String selectedMapSize = "Moyenne (15x15)";
    public static List<Strategy> selectedStrategies = new ArrayList<>();

    private static final String[] STRATEGY_LABELS = {
            "Agressif",
            "Survivant️",
            "Tactique"
    };

    private final List<ComboBox<String>> strategyBoxes = new ArrayList<>();

    private static final Strategy[] STRATEGY_VALUES = {
            Strategy.AGGRESSIVE,
            Strategy.SURVIVALIST,
            Strategy.TACTICAL
    };

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

        // Construire les sélecteurs pour la valeur initiale du spinner (1 bot)
        rebuildStrategySelectors(botCountSpinner.getValue());

        // Reconstruire à chaque changement
        botCountSpinner.valueProperty().addListener(
                (obs, oldVal, newVal) -> rebuildStrategySelectors(newVal)
        );
    }

    private void rebuildStrategySelectors(int botCount) {
        botStrategyContainer.getChildren().clear();
        strategyBoxes.clear();

        for (int i = 0; i < botCount; i++) {
            // Label
            Label label = new Label("Bot " + (i + 1) + " :");
            label.setMinWidth(60);

            // ComboBox
            ComboBox<String> box = new ComboBox<>();
            box.getItems().addAll(STRATEGY_LABELS);
            box.setValue(STRATEGY_LABELS[1]); // Survivant par défaut
            box.setPrefWidth(160);

            strategyBoxes.add(box);

            // Ligne horizontale
            HBox row = new HBox(10, label, box);
            row.setPadding(new Insets(2, 0, 2, 0));
            botStrategyContainer.getChildren().add(row);
        }

        // Si 0 bot, on affiche un message discret
        if (botCount == 0) {
            botStrategyContainer.getChildren().add(
                    new Label("Aucun bot dans cette partie.")
            );
        }
    }

    private List<Strategy> readSelectedStrategies() {
        List<Strategy> result = new ArrayList<>();
        for (ComboBox<String> box : strategyBoxes) {
            String label = box.getValue();
            Strategy strategy = Strategy.SURVIVALIST; // fallback
            for (int i = 0; i < STRATEGY_LABELS.length; i++) {
                if (STRATEGY_LABELS[i].equals(label)) {
                    strategy = STRATEGY_VALUES[i];
                    break;
                }
            }
            result.add(strategy);
        }
        return result;
    }

    @FXML
    void onStartButtonClick(ActionEvent event) {
        // On récupère toutes les valeurs avant de changer de scène
        selectedBots = botCountSpinner.getValue();
        selectedBombs = bombCountSpinner.getValue();
        selectedTime = timeSpinner.getValue();
        selectedMapSize = mapSizeBox.getValue();
        selectedStrategies = readSelectedStrategies();

        System.out.println("🚀 Config : Map=" + selectedMapSize + " | Bots=" + selectedBots + " | Time=" + selectedTime + "s");

        SceneManager.getInstance().loadScene("game-board.fxml");
    }

    @FXML
    void onBackButtonClick(ActionEvent event) {
        SceneManager.getInstance().loadScene("connection-choice.fxml");
    }
}