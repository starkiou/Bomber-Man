package clientside;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class SceneManager {
    private static SceneManager instance;
    private Stage stage;

    private SceneManager() {} // Singleton

    public static SceneManager getInstance() {
        if (instance == null) instance = new SceneManager();
        return instance;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        // On peut déjà configurer le stage ici pour le futur
        this.stage.setFullScreenExitHint(""); // Enlever le texte "Appuyez sur ESC"
    }

    public void loadScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientside/" + fxmlFile));
            Parent root = loader.load();

            // 1. Si on n'a pas encore de scène, on en crée une
            if (stage.getScene() == null) {
                Scene scene = new Scene(root);
                stage.setScene(scene);
            } else {
                // 2. IMPORTANT : On change la racine (root) de la scène EXISTANTE
                // Cela évite de recréer une fenêtre et de perdre le plein écran
                stage.getScene().setRoot(root);
            }

            // 3. Charger le CSS si nécessaire
            var cssUrl = getClass().getResource("/style.css");
            if (cssUrl != null) {
                String css = cssUrl.toExternalForm();
                if (!stage.getScene().getStylesheets().contains(css)) {
                    stage.getScene().getStylesheets().add(css);
                }
            }

            // 4. On s'assure qu'on est en plein écran (ne fera rien si déjà actif)
            if (!stage.isFullScreen()) {
                stage.setFullScreen(true);
            }

            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur de chargement : " + fxmlFile);
            e.printStackTrace();
        }
    }
}