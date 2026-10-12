package clientside.controllers;

import clientside.network.NetworkManager;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;

import java.util.Set;

public class CharacterSelectionController {

    /** Skins dont les images sont mal découpées dans la planche de sprites. */
    public static final Set<Integer> BROKEN_SKINS = Set.of(6, 9);

    @FXML private TilePane spriteContainer;

    @FXML
    public void initialize() {
        spriteContainer.getChildren().clear();

        for (int i = 0; i <= 34; i++) {
            if (BROKEN_SKINS.contains(i)) continue;
            final int charId = i;
            try {
                String imagePath = "/sprites/output/characters/" + i + "/R_0.png";
                Image img = new Image(getClass().getResourceAsStream(imagePath));

                ImageView view = new ImageView(img);
                view.setFitWidth(40); view.setFitHeight(40);

                Button btn = new Button();
                btn.setGraphic(view);
                btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

                btn.setOnAction(e -> {
                    NetworkManager.getInstance().setSelectedCharacterId(charId);
                    for (Node n : spriteContainer.getChildren()) {
                        n.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
                    }
                    btn.setStyle("-fx-border-color: red; -fx-border-width: 2;");
                });

                if (i == 0) btn.fire();
                spriteContainer.getChildren().add(btn);
            } catch (Exception e) {
                // Ignore silencieusement ou log
            }
        }
    }
}