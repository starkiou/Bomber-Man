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
import javafx.scene.layout.Pane; // Ajouté pour l'affichage de l'overlay
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
    @FXML private Label hpLabel;
    @FXML private Label bombsLabel;
    @FXML private Label timerLabel;

    private static final int TILE_SIZE = 40;

    private Image wallImg, floorImg, brickImg, playerImg, botImg;
    private Map<Integer, ImageView> playerViews = new HashMap<>();
    private Game game;

    private Image[] bombIdleFrames = new Image[28];
    private Image[][] explosionFrames = new Image[7][14];
    private Map<Integer, ImageView> bombViews = new HashMap<>();
    private Map<String, ImageView> explosionViews = new HashMap<>();

    private int currentWidth;
    private int currentHeight;
    private ImageView[][] tileViews;

    // Ajouté pour le calcul du score final
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
        wallImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/walls/block_07.png")));
        floorImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/ground/ground_06.png")));
        brickImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/walls/block_08.png")));

        int charId = NetworkManager.getInstance().getSelectedCharacterId();
        playerImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/characters/" + charId + "/D_0.png")));
        botImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/characters/1/D_0.png")));

        // Chargement des bombes
        for (int i = 0; i < 28; i++) {
            bombIdleFrames[i] = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/sprites/output/bomb/B2_" + i + ".png")));
        }

        // --- CORRECTION : Chargement des explosions (indispensable) ---
        for (int t = 0; t < 7; t++) {
            for (int f = 0; f < 14; f++) {
                explosionFrames[t][f] = new Image(Objects.requireNonNull(
                        getClass().getResourceAsStream("/sprites/output/explosions/explosion_" + t + "_" + f + ".png")));
            }
        }
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
        // Initialisation du chrono
        this.startTime = System.currentTimeMillis();

        String selectedSize = GameConfigController.selectedMapSize;
        if (selectedSize != null && selectedSize.contains("Petite")) {
            currentWidth = 11; currentHeight = 11;
        } else if (selectedSize != null && selectedSize.contains("Grande")) {
            currentWidth = 19; currentHeight = 15;
        } else {
            currentWidth = 15; currentHeight = 11;
        }

        tileViews = new ImageView[currentWidth][currentHeight];
        gameGrid.getChildren().clear();
        initMap();

        CellType[][] grid = MazeFactory.createMaze(MazeFactory.Algorithm.EXHAUSTIVE, currentWidth, currentHeight);
        List<Player> players = new ArrayList<>();
        players.add(new Player(1, 1, 1, 3, 1.0, GameConfigController.selectedBombs));

        game = new Game(grid, players, GameConfigController.selectedTime);

        int nbBots = GameConfigController.selectedBots;
        for (int i = 0; i < nbBots; i++) {
            int startX = (i % 2 == 0) ? currentWidth - 2 : 1;
            int startY = (i < 2) ? currentHeight - 2 : currentHeight / 2;
            game.addBot(AIFactory.create(Strategy.SURVIVALIST, i + 2, startX, startY, 3, 1.0, 1));
        }

        game.addListener(new GameStateListener() {
            @Override
            public void onGameStateUpdate(GameSnapshot snap) {
                Platform.runLater(() -> drawMap(snap));
            }
            @Override public void onPlayerDied(int id) {}
            @Override
            public void onGameOver(int winnerId) {
                Platform.runLater(() -> showGameOverScreen(winnerId));
            }
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

    private void showGameOverScreen(int winnerId) {
        try {
            // Remplacez par le vrai chemin de votre fichier FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/game-over.fxml"));
            Parent gameOverRoot = loader.load();

            GameOverController controller = loader.getController();

            long duration = (System.currentTimeMillis() - startTime) / 1000;
            String timeStr = String.format("%02d:%02d", duration / 60, duration % 60);
            String title = (winnerId == 1) ? "VICTOIRE !" : "VOUS AVEZ PERDU !!";

            // On suppose que l'ID 1 est le joueur local
            controller.setStats(title, timeStr, 0);

            // Affichage par dessus la grille (si le root de la scène est un StackPane)
            Pane root = (Pane) gameGrid.getScene().getRoot();
            root.getChildren().add(gameOverRoot);

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de l'écran Game Over : " + e.getMessage());
        }
    }

    private void drawMap(GameSnapshot snap) {
        // 1. Grille
        CellType[][] grid = snap.getGrid();
        for (int x = 0; x < currentWidth; x++) {
            for (int y = 0; y < currentHeight; y++) {
                CellType type = grid[y][x];
                if (type == CellType.WALL) tileViews[x][y].setImage(wallImg);
                else if (type == CellType.BRICK) tileViews[x][y].setImage(brickImg);
                else tileViews[x][y].setImage(floorImg);
            }
        }

        // 2. Bombes
        Set<Integer> activeBombIds = new HashSet<>();
        for (GameSnapshot.BombState b : snap.getBombs()) {
            activeBombIds.add(b.id());
            int frame = (int) ((System.currentTimeMillis() / 100) % 28);
            ImageView bView = bombViews.computeIfAbsent(b.id(), id -> {
                ImageView v = new ImageView();
                v.setFitWidth(TILE_SIZE); v.setFitHeight(TILE_SIZE);
                gameGrid.getChildren().add(v);
                return v;
            });
            bView.setImage(bombIdleFrames[frame]);
            GridPane.setColumnIndex(bView, b.x());
            GridPane.setRowIndex(bView, b.y());
        }
        bombViews.keySet().removeIf(id -> {
            if (!activeBombIds.contains(id)) {
                gameGrid.getChildren().remove(bombViews.get(id));
                return true;
            }
            return false;
        });

        // 3. Explosions
        Set<String> activeExplosionKeys = new HashSet<>();
        for (GameSnapshot.ExplosionState e : snap.getExplosions()) {
            String key = e.x() + "_" + e.y();
            activeExplosionKeys.add(key);
            int frame = (int) Math.min(e.ageMs() / 50, 13);
            ImageView eView = explosionViews.computeIfAbsent(key, k -> {
                ImageView v = new ImageView();
                v.setFitWidth(TILE_SIZE); v.setFitHeight(TILE_SIZE);
                gameGrid.getChildren().add(v);
                return v;
            });
            eView.setImage(explosionFrames[e.spriteType()][frame]);
            GridPane.setColumnIndex(eView, e.x());
            GridPane.setRowIndex(eView, e.y());
        }
        explosionViews.keySet().removeIf(key -> {
            if (!activeExplosionKeys.contains(key)) {
                gameGrid.getChildren().remove(explosionViews.get(key));
                return true;
            }
            return false;
        });

        // 4. Timer
        long sec = snap.getRemainingSeconds();
        if (timerLabel != null) {
            timerLabel.setText(String.format("%02d:%02d", sec / 60, sec % 60));
        }

        // 5. Joueurs
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
                    iv.setFitWidth(TILE_SIZE); iv.setFitHeight(TILE_SIZE);
                    gameGrid.getChildren().add(iv);
                    return iv;
                });
                GridPane.setColumnIndex(v, p.x());
                GridPane.setRowIndex(v, p.y());
            }
        }
    }
}