package clientside.controllers;

import model.maze.MazeFactory;
import model.maze.CellType;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import java.util.Objects;

public class GameBoardController {

    @FXML private GridPane gameGrid;

    // Constantes (Checkstyle : évite les Magic Numbers [cite: 28, 29])
    private static final int MAZE_WIDTH = 11;
    private static final int MAZE_HEIGHT = 15;
    private static final int TILE_SIZE = 40;

    @FXML
    public void initialize() {
        generateAndDisplayMap();
    }

    private void generateAndDisplayMap() {
        // 1. Appel de ton programme de génération
        CellType[][] grid = MazeFactory.createMaze(MazeFactory.Algorithm.EXHAUSTIVE, MAZE_WIDTH, MAZE_HEIGHT);

        // 2. Chargement des textures (à mettre dans src/main/resources/sprites/)
        Image wallImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/wall.png")));
        Image floorImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/floor.png")));

        // 3. Parcours de la grid[][] pour afficher les textures
        for (int x = 0; x < MAZE_WIDTH; x++) {
            for (int y = 0; y < MAZE_HEIGHT; y++) {
                ImageView tile = new ImageView();
                tile.setFitWidth(TILE_SIZE);
                tile.setFitHeight(TILE_SIZE);

                if (grid[x][y] == CellType.WALL) {
                    tile.setImage(wallImg);
                } else {
                    tile.setImage(floorImg);
                }

                // Ajout au GridPane (colonne, ligne)
                gameGrid.add(tile, x, y);
            }
        }
    }
}