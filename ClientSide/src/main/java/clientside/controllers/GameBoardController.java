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
    private static final int MAZE_WIDTH = 15;
    private static final int MAZE_HEIGHT = 11;
    private static final int TILE_SIZE = 40;

    @FXML
    public void initialize() {
        generateAndDisplayMap();
    }

    private void generateAndDisplayMap() {
        CellType[][] grid = MazeFactory.createMaze(MazeFactory.Algorithm.EXHAUSTIVE, MAZE_WIDTH, MAZE_HEIGHT);

        // Chargement des 3 textures
        Image wallImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/block_07.png")));
        Image floorImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/ground_06.png")));
        Image brickImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/block_08.png"))); // Ta texture de brique

        for (int x = 0; x < MAZE_WIDTH; x++) {
            for (int y = 0; y < MAZE_HEIGHT; y++) {
                ImageView tile = new ImageView();
                tile.setFitWidth(TILE_SIZE);
                tile.setFitHeight(TILE_SIZE);

                // Gestion des trois types de cellules
                if (grid[x][y] == CellType.WALL) {
                    tile.setImage(wallImg);
                } else if (grid[x][y] == CellType.BRICK) { // Vérifie le nom exact dans ton enum CellType
                    tile.setImage(brickImg);
                } else {
                    tile.setImage(floorImg);
                }

                gameGrid.add(tile, x, y);
            }
        }
    }
}