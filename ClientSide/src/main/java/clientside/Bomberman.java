package clientside;

import javafx.application.Application;
import javafx.stage.Stage;
import model.logger.LogManager;
import java.io.IOException;

public class Bomberman extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        LogManager.getInstance().info("Démarrage de l'application client");
        SceneManager.getInstance().setStage(stage);
        stage.setTitle("Bomberman \uD83D\uDCA3");
        // Fenêtre maximisée au lancement ; F11 bascule le plein écran
        stage.setMinWidth(1000);
        stage.setMinHeight(760);
        stage.setMaximized(true);

        SceneManager.getInstance().loadScene("home-view.fxml");

        stage.show();
    }
}