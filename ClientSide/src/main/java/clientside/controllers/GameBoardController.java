package clientside.controllers;

import clientside.network.NetworkManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import model.aiPlayer.AIFactory;
import model.aiPlayer.Strategy;
import model.entity.Player;
import model.game.Game;
import model.game.GameSnapshot;
import model.game.GameStateListener;
import model.maze.CellType;
import model.maze.MazeFactory;
import network.message.ActionType;
import java.util.*;

public class GameBoardController {

    @FXML private GridPane gameGrid;
    @FXML private Label hpLabel;
    @FXML private Label bombsLabel;
    @FXML private Label timerLabel;

    private static final int TILE_SIZE = 40;

    private Image wallImg, floorImg, brickImg, playerImg, botImg;
    private Map<Integer, ImageView> playerViews = new HashMap<>();
    private Game game;

    private int currentWidth;
    private int currentHeight;
    private ImageView[][] tileViews;

    @FXML
    public void initialize() {
        loadImages();
        // On ne lance que startGame, car c'est elle qui va définir les dimensions
        // et appeler initMap() avec les bonnes tailles.
        startGame();

        Platform.runLater(() -> {
            if (gameGrid.getScene() != null) {
                gameGrid.getScene().setOnKeyPressed(this::handleKeyPress);
                gameGrid.getScene().getRoot().requestFocus();
            }
        });
    }

    private void loadImages() {
        wallImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/walls/block_07.png")));
        floorImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/ground/ground_06.png")));
        brickImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/walls/block_08.png")));

        int charId = NetworkManager.getInstance().getSelectedCharacterId();
        playerImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/characters/" + charId + "/D_0.png")));
        botImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/characters/1/D_0.png")));
    }

    private void initMap() {
        for (int x = 0; x < currentWidth; x++) {
            for (int y = 0; y < currentHeight; y++) {
                ImageView iv = new ImageView();
                iv.setFitWidth(TILE_SIZE);
                iv.setFitHeight(TILE_SIZE);
                tileViews[x][y] = iv;
                gameGrid.add(iv, x, y);
            }
        }
    }

    private void startGame() {
        // 1. Déterminer la taille dynamique selon GameConfigController
        String selectedSize = GameConfigController.selectedMapSize;
        if (selectedSize != null && selectedSize.contains("Petite")) {
            currentWidth = 11;
            currentHeight = 11;
        } else if (selectedSize != null && selectedSize.contains("Grande")) {
            currentWidth = 19;
            currentHeight = 15;
        } else {
            currentWidth = 15;
            currentHeight = 11;
        }

        // 2. Initialiser le tableau de vues et la grille graphique
        tileViews = new ImageView[currentWidth][currentHeight];
        gameGrid.getChildren().clear();
        initMap();

        // 3. Génération de la logique du labyrinthe
        CellType[][] grid = MazeFactory.createMaze(MazeFactory.Algorithm.EXHAUSTIVE, currentWidth, currentHeight);

        // 4. Création des joueurs
        List<Player> players = new ArrayList<>();
        players.add(new Player(1, 1, 1, 3, 1.0, GameConfigController.selectedBombs));

        // 5. Création du moteur de jeu
        game = new Game(grid, players, GameConfigController.selectedTime);

        // 6. Ajout des bots selon la config
        int nbBots = GameConfigController.selectedBots;
        for (int i = 0; i < nbBots; i++) {
            int startX = (i % 2 == 0) ? currentWidth - 2 : 1;
            int startY = (i < 2) ? currentHeight - 2 : currentHeight / 2;
            game.addBot(AIFactory.create(Strategy.SURVIVALIST, i + 2, startX, startY, 3, 1.0, 1));
        }

        // 7. Écouteur de mise à jour
        game.addListener(new GameStateListener() {
            @Override
            public void onGameStateUpdate(GameSnapshot snap) {
                Platform.runLater(() -> drawMap(snap));
            }
            @Override public void onPlayerDied(int id) {}
            @Override public void onGameOver(int winnerId) {}
        });

        game.start();
    }

    private void handleKeyPress(KeyEvent event) {
        if (game == null) return;
        switch (event.getCode()) {
            case Z, UP    -> game.handleAction(1, ActionType.MOVE_UP);
            case S, DOWN  -> game.handleAction(1, ActionType.MOVE_DOWN);
            case Q, LEFT  -> game.handleAction(1, ActionType.MOVE_LEFT);
            case D, RIGHT -> game.handleAction(1, ActionType.MOVE_RIGHT);
            case SPACE    -> game.handleAction(1, ActionType.PLACE_BOMB);
        }
    }

    private void drawMap(GameSnapshot snap) {
        // Rendu de la grille (Dynamique sur currentWidth/Height)
        CellType[][] grid = snap.getGrid();
        for (int x = 0; x < currentWidth; x++) {
            for (int y = 0; y < currentHeight; y++) {
                CellType type = grid[y][x];
                if (type == CellType.WALL) tileViews[x][y].setImage(wallImg);
                else if (type == CellType.BRICK) tileViews[x][y].setImage(brickImg);
                else tileViews[x][y].setImage(floorImg);
            }
        }

        // Mise à jour du Timer
        long sec = snap.getRemainingSeconds();
        if (timerLabel != null) {
            timerLabel.setText(String.format("%02d:%02d", sec / 60, sec % 60));
        }

        // Mise à jour des Joueurs
        for (GameSnapshot.PlayerState p : snap.getPlayers()) {
            if (p.id() == 1) {
                hpLabel.setText(String.valueOf(p.hp()));
                bombsLabel.setText(p.currentBombs() + "/" + p.maxBombs());
            }

            if (p.isDead()) {
                ImageView v = playerViews.remove(p.id());
                if (v != null) gameGrid.getChildren().remove(v);
            } else {
                ImageView v = playerViews.computeIfAbsent(p.id(), id -> {
                    ImageView iv = new ImageView(id == 1 ? playerImg : botImg);
                    iv.setFitWidth(TILE_SIZE);
                    iv.setFitHeight(TILE_SIZE);
                    gameGrid.getChildren().add(iv);
                    return iv;
                });
                GridPane.setColumnIndex(v, p.x());
                GridPane.setRowIndex(v, p.y());
            }
        }
    }
}