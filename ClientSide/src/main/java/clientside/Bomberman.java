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
        
        SceneManager.getInstance().loadScene("/clientside/game-config.fxml");

        //SceneManager.getInstance().loadScene("login-view.fxml");

        stage.show();
    }
}