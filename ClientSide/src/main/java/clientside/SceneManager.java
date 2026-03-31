package clientside;

import javafx.fxml.FXMLLoader;
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

    // call once in Bomberman.java
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    // use this metho to change screens
    public void loadScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientside/" + fxmlFile));
            Scene scene = new Scene(loader.load(), 800, 600);

            stage.setScene(scene);
        } catch (IOException e) {
            System.err.println("Impossible de charger : " + fxmlFile);
            e.printStackTrace();
        }
    }
}