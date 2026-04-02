package clientside.controllers;

import clientside.network.NetworkManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import model.aiPlayer.AIFactory;
import model.aiPlayer.Strategy;
import model.entity.Player;
import model.game.Game;
import model.game.GameSnapshot;
import model.game.GameStateListener;
import model.maze.CellType;
import model.maze.MazeFactory;
import network.message.ActionType;
import java.io.IOException;
import java.util.*;

public class GameBoardController {

    @FXML private GridPane gameGrid;
    @FXML private Label hpLabel, bombsLabel, timerLabel;

    private static final int TILE_SIZE = 40;
    private Image wallImg, floorImg, brickImg, playerImg, botImg;
    private final Image[] bombIdleFrames = new Image[28];
    private final Image[][] explosionFrames = new Image[7][14];

    private final Map<Integer, ImageView> playerViews = new HashMap<>();
    private final Map<Integer, ImageView> bombViews = new HashMap<>();
    private final Map<Integer, Long> bombFirstSeen = new HashMap<>();
    private final Map<String, ImageView> explosionViews = new HashMap<>();

    private Game game;
    private int currentWidth, currentHeight;
    private ImageView[][] tileViews;
    private long startTime;

    @FXML
    public void initialize() {
        loadImages();
        startGame();
        Platform.runLater(() -> {
            if (gameGrid.getScene() != null) {
                gameGrid.getScene().setOnKeyPressed(this::handleKeyPress);
                gameGrid.getScene().getRoot().requestFocus();
            }
        });
    }

    private void loadImages() {
        wallImg = new Image(getClass().getResourceAsStream("/sprites/output/walls/block_07.png"));
        floorImg = new Image(getClass().getResourceAsStream("/sprites/output/ground/ground_06.png"));
        brickImg = new Image(getClass().getResourceAsStream("/sprites/output/walls/block_08.png"));
        int charId = NetworkManager.getInstance().getSelectedCharacterId();
        playerImg = new Image(getClass().getResourceAsStream("/sprites/output/characters/" + charId + "/D_0.png"));
        botImg = new Image(getClass().getResourceAsStream("/sprites/output/characters/1/D_0.png"));

        for (int i = 0; i < 28; i++)
            bombIdleFrames[i] = new Image(getClass().getResourceAsStream("/sprites/output/bomb/B2_" + i + ".png"));
        for (int t = 0; t < 7; t++)
            for (int f = 0; f < 14; f++)
                explosionFrames[t][f] = new Image(getClass().getResourceAsStream("/sprites/output/explosions/explosion_" + t + "_" + f + ".png"));
    }

    private void startGame() {
        this.startTime = System.currentTimeMillis();
        configureDimensions();

        tileViews = new ImageView[currentWidth][currentHeight];
        gameGrid.getChildren().clear();
        initBackgroundGrid();

        CellType[][] grid = MazeFactory.createMaze(MazeFactory.Algorithm.EXHAUSTIVE, currentWidth, currentHeight);
        game = new Game(grid, List.of(new Player(1, 1, 1, 3, 1.0, GameConfigController.selectedBombs)), GameConfigController.selectedTime);

        for (int i = 0; i < GameConfigController.selectedBots; i++) {
            int startX = (i % 2 == 0) ? currentWidth - 2 : 1;
            int startY = (i < 2) ? currentHeight - 2 : currentHeight / 2;
            game.addBot(AIFactory.create(Strategy.SURVIVALIST, i + 2, startX, startY, 3, 1.0, 1));
        }

        game.addListener(new GameStateListener() {
            @Override public void onGameStateUpdate(GameSnapshot snap) { Platform.runLater(() -> drawMap(snap)); }
            @Override public void onPlayerDied(int id) {}
            @Override public void onGameOver(int winnerId) { Platform.runLater(() -> showGameOverScreen(winnerId)); }
        });
        game.start();
    }

    private void configureDimensions() {
        String size = GameConfigController.selectedMapSize;
        if (size != null && size.contains("Petite")) { currentWidth = 11; currentHeight = 11; }
        else if (size != null && size.contains("Grande")) { currentWidth = 19; currentHeight = 15; }
        else { currentWidth = 15; currentHeight = 11; }
    }

    private void initBackgroundGrid() {
        for (int x = 0; x < currentWidth; x++) {
            for (int y = 0; y < currentHeight; y++) {
                ImageView iv = new ImageView();
                iv.setFitWidth(TILE_SIZE); iv.setFitHeight(TILE_SIZE);
                tileViews[x][y] = iv;
                gameGrid.add(iv, x, y);
            }
        }
    }

    private void showGameOverScreen(int winnerId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientside/game-over.fxml"));
            Parent rootNode = loader.load();

            GameOverController controller = loader.getController();
            String timeStr = String.format("%02d:%02d", (System.currentTimeMillis() - startTime)/60000, ((System.currentTimeMillis() - startTime)/1000)%60);
            controller.setStats(winnerId == 1 ? "VICTOIRE !" : "DÉFAITE...", timeStr, 0);

            StackPane rootStack = (StackPane) gameGrid.getScene().getRoot();
            rootStack.getChildren().add(rootNode);
            rootNode.toFront();
        } catch (IOException e) { e.printStackTrace(); }
    }



    private void drawMap(GameSnapshot snap) {
        // 1. Grille statique
        CellType[][] grid = snap.getGrid();
        for (int x = 0; x < currentWidth; x++)
            for (int y = 0; y < currentHeight; y++)
                tileViews[x][y].setImage(grid[y][x] == CellType.WALL ? wallImg : (grid[y][x] == CellType.BRICK ? brickImg : floorImg));

        // 2. Objets dynamiques (Bombes & Explosions)
        renderDynamicObjects(snap);

        // 3. HUD & Joueurs
        timerLabel.setText(String.format("%02d:%02d", snap.getRemainingSeconds() / 60, snap.getRemainingSeconds() % 60));
        renderPlayers(snap);
    }

    private void renderDynamicObjects(GameSnapshot snap) {
        // Bombes
        Set<Integer> activeBombs = new HashSet<>();
        for (var b : snap.getBombs()) {
            activeBombs.add(b.id());
            long firstSeen = bombFirstSeen.computeIfAbsent(b.id(), id -> System.currentTimeMillis());
            int bombFrame = (int) ((System.currentTimeMillis() - firstSeen) / 100 % 28);
            ImageView v = bombViews.computeIfAbsent(b.id(), id -> {
                ImageView iv = new ImageView(); iv.setFitWidth(TILE_SIZE); iv.setFitHeight(TILE_SIZE);
                gameGrid.getChildren().add(iv); return iv;
            });
            v.setImage(bombIdleFrames[bombFrame]);
            GridPane.setColumnIndex(v, b.x()); GridPane.setRowIndex(v, b.y());
        }
        bombViews.keySet().removeIf(id -> { if(!activeBombs.contains(id)) { gameGrid.getChildren().remove(bombViews.get(id)); bombFirstSeen.remove(id); return true; } return false; });

        // Explosions
        Set<String> activeExplosions = new HashSet<>();
        for (var e : snap.getExplosions()) {
            String key = e.x() + "_" + e.y(); activeExplosions.add(key);
            ImageView v = explosionViews.computeIfAbsent(key, k -> {
                ImageView iv = new ImageView(); iv.setFitWidth(TILE_SIZE); iv.setFitHeight(TILE_SIZE);
                gameGrid.getChildren().add(iv); return iv;
            });
            v.setImage(explosionFrames[e.spriteType()][(int) Math.min(e.ageMs() / 50, 13)]);
            GridPane.setColumnIndex(v, e.x()); GridPane.setRowIndex(v, e.y());
        }
        explosionViews.keySet().removeIf(k -> { if(!activeExplosions.contains(k)) { gameGrid.getChildren().remove(explosionViews.get(k)); return true; } return false; });
    }

    private void renderPlayers(GameSnapshot snap) {
        Set<Integer> aliveIds = new HashSet<>();
        for (var p : snap.getPlayers()) {
            aliveIds.add(p.id());
            if (p.id() == 1) { hpLabel.setText(String.valueOf(p.hp())); bombsLabel.setText(p.currentBombs() + "/" + p.maxBombs()); }
            if (!p.isDead()) {
                ImageView v = playerViews.computeIfAbsent(p.id(), id -> {
                    ImageView iv = new ImageView(id == 1 ? playerImg : botImg);
                    iv.setFitWidth(TILE_SIZE); iv.setFitHeight(TILE_SIZE);
                    gameGrid.getChildren().add(iv); return iv;
                });
                GridPane.setColumnIndex(v, p.x()); GridPane.setRowIndex(v, p.y());
            }
        }
        playerViews.keySet().removeIf(id -> { if(!aliveIds.contains(id)) { gameGrid.getChildren().remove(playerViews.get(id)); return true; } return false; });
    }

    private void handleKeyPress(KeyEvent event) {
        if (game == null) return;
        switch (event.getCode()) {
            case Z, UP -> game.handleAction(1, ActionType.MOVE_UP);
            case S, DOWN -> game.handleAction(1, ActionType.MOVE_DOWN);
            case Q, LEFT -> game.handleAction(1, ActionType.MOVE_LEFT);
            case D, RIGHT -> game.handleAction(1, ActionType.MOVE_RIGHT);
            case SPACE -> game.handleAction(1, ActionType.PLACE_BOMB);
            default -> {}
        }
    }
}