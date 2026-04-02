package clientside.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import model.aiPlayer.AIFactory;
import model.aiPlayer.AIPlayer;
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
    @FXML private Label modeLabel;

    private static final int MAZE_WIDTH = 15;
    private static final int MAZE_HEIGHT = 11;
    private static final int TILE_SIZE = 40;

    private ImageView[][] tileViews = new ImageView[MAZE_WIDTH][MAZE_HEIGHT];
    private Image wallImg, floorImg, brickImg;
    private Map<Integer, ImageView> playerViews = new HashMap<>();
    private Image player1Img, botImg;

    private static final int    BOMB_IDLE_FRAMES       = 28;
    private static final int    EXPLOSION_SPRITE_TYPES  = 7;
    private static final int    EXPLOSION_FRAMES        = 14;
    private static final long   EXPLOSION_ANIM_MS      = 700;
    private static final long   BOMB_ANIM_TOTAL_MS     = 3000;

    private Image[]    bombIdleFrames  = new Image[BOMB_IDLE_FRAMES];
    private Image[][] explosionFrames = new Image[EXPLOSION_SPRITE_TYPES][EXPLOSION_FRAMES];
    private Map<Integer, ImageView> bombViews     = new HashMap<>();
    private Map<Integer, Long> bombFirstSeen = new HashMap<>();
    private Map<Integer, Long>      bombFirstSeenReal = new HashMap<>();
    private Map<String, ImageView>  explosionViews = new HashMap<>();

    private Game game;

    @FXML
    public void initialize() {
        loadImages();
        initMap();
        startGame();

        Platform.runLater(() -> {
            gameGrid.getScene().setOnKeyPressed(this::handleKeyPress);
            gameGrid.getScene().getRoot().requestFocus();
        });
    }

    private void loadImages() {
        wallImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/walls/block_07.png")));
        floorImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/ground/ground_06.png")));
        brickImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/walls/block_08.png")));
        player1Img = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/characters/0/D_0.png")));
        botImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/characters/1/D_0.png")));

        for (int i = 0; i < BOMB_IDLE_FRAMES; i++) {
            bombIdleFrames[i] = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/bomb/B2_" + i + ".png")));
        }
        for (int type = 0; type < EXPLOSION_SPRITE_TYPES; type++) {
            for (int f = 0; f < EXPLOSION_FRAMES; f++) {
                explosionFrames[type][f] = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/explosions/explosion_" + type + "_" + f + ".png")));
            }
        }
    }

    private void initMap() {
        for (int x = 0; x < MAZE_WIDTH; x++) {
            for (int y = 0; y < MAZE_HEIGHT; y++) {
                ImageView tile = new ImageView();
                tile.setFitWidth(TILE_SIZE);
                tile.setFitHeight(TILE_SIZE);
                tileViews[x][y] = tile;
                gameGrid.add(tile, x, y);
            }
        }
    }

    private void startGame() {
        CellType[][] grid = MazeFactory.createMaze(MazeFactory.Algorithm.EXHAUSTIVE, MAZE_WIDTH, MAZE_HEIGHT);
        List<Player> players = new ArrayList<>();
        players.add(new Player(1, 1, 1, 3, 1.0, 1));

        game = new Game(grid, players);
        AIPlayer bot = AIFactory.create(Strategy.SURVIVALIST, 2, 13, 9, 3, 1.0, 1);
        game.addBot(bot);

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
        int myPlayerId = 1;
        switch (event.getCode()) {
            case Z, UP -> game.handleAction(myPlayerId, ActionType.MOVE_UP);
            case S, DOWN -> game.handleAction(myPlayerId, ActionType.MOVE_DOWN);
            case Q, LEFT -> game.handleAction(myPlayerId, ActionType.MOVE_LEFT);
            case D, RIGHT -> game.handleAction(myPlayerId, ActionType.MOVE_RIGHT);
            case SPACE -> game.handleAction(myPlayerId, ActionType.PLACE_BOMB);
            default -> {}
        }
    }

    private void drawMap(GameSnapshot snap) {
        // 1. Grille
        CellType[][] currentGrid = snap.getGrid();
        for (int x = 0; x < MAZE_WIDTH; x++) {
            for (int y = 0; y < MAZE_HEIGHT; y++) {
                if (currentGrid[y][x] == CellType.WALL) tileViews[x][y].setImage(wallImg);
                else if (currentGrid[y][x] == CellType.BRICK) tileViews[x][y].setImage(brickImg);
                else tileViews[x][y].setImage(floorImg);
            }
        }

        // 2. Bombes & Explosions (Logique existante simplifiée pour la lecture)
        long now = System.currentTimeMillis();
        // ... (gestion des bombes et explosions identique à ton code précédent)

        // 3. Joueurs & HUD
        for (GameSnapshot.PlayerState p : snap.getPlayers()) {

            // MISE À JOUR DES STATS POUR TOI (ID 1)
            if (p.id() == 1) {
                hpLabel.setText(String.valueOf(p.hp()));
                bombsLabel.setText(p.currentBombs() + " / " + p.maxBombs());
            }

            if (p.isDead()) {
                ImageView deadView = playerViews.remove(p.id());
                if (deadView != null) gameGrid.getChildren().remove(deadView);
                continue;
            }

            ImageView pView = playerViews.computeIfAbsent(p.id(), id -> {
                ImageView v = new ImageView(id == 1 ? player1Img : botImg);
                v.setFitWidth(TILE_SIZE);
                v.setFitHeight(TILE_SIZE);
                gameGrid.getChildren().add(v);
                return v;
            });
            GridPane.setColumnIndex(pView, p.x());
            GridPane.setRowIndex(pView, p.y());
        }
    }
}