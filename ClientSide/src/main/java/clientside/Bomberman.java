package clientside;

import javafx.application.Application;
import javafx.stage.Stage;
import java.io.IOException;

public class Bomberman extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        SceneManager.getInstance().setStage(stage);
        stage.setTitle("Bomberman \uD83D\uDCA3");

        SceneManager.getInstance().loadScene("home-view.fxml");

        stage.show();
    }
}