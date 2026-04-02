package clientside.controllers;

import clientside.network.NetworkManager;
import javafx.animation.TranslateTransition;
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
import javafx.util.Duration;
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
import java.util.concurrent.ThreadLocalRandom;

public class GameBoardController {

    @FXML private GridPane gameGrid;
    @FXML private Label hpLabel, bombsLabel, timerLabel;

    private static final int TILE_SIZE = 40;

    // Constantes Directions
    private static final int DIR_DOWN = 0;
    private static final int DIR_UP = 1;
    private static final int DIR_LEFT = 2;
    private static final int DIR_RIGHT = 3;

    // Décors
    private int currentWidth, currentHeight;
    private ImageView[][] tileViews;
    private Image wallImg, floorImg, brickImg;

    // Entités & Animations
    private final Map<Integer, PlayerVisual> playerVisuals = new HashMap<>();
    private final Image[][] playerSprites = new Image[4][3];
    private final Image[][] botSprites = new Image[4][3];

    // Bombes & Explosions
    private static final int BOMB_IDLE_FRAMES = 28;
    private static final int EXPLOSION_SPRITE_TYPES = 7;
    private static final int EXPLOSION_FRAMES = 14;
    private static final long EXPLOSION_ANIM_MS = 700;
    private static final long BOMB_ANIM_TOTAL_MS = 3000;

    private final Image[] bombIdleFrames = new Image[BOMB_IDLE_FRAMES];
    private final Image[][] explosionFrames = new Image[EXPLOSION_SPRITE_TYPES][EXPLOSION_FRAMES];

    private final Map<Integer, ImageView> bombViews = new HashMap<>();
    private final Map<Integer, Long> bombFirstSeen = new HashMap<>();
    private final Map<String, ImageView> explosionViews = new HashMap<>();

    private Game game;
    private long startTime;

    // --- CLASSE UTILITAIRE POUR L'ANIMATION ---
    private class PlayerVisual {
        ImageView view;
        Image[][] sprites;
        TranslateTransition transition;
        double targetX = -1, targetY = -1;
        int currentDir = DIR_DOWN;
        boolean isMoving = false;

        PlayerVisual(Image[][] sprites) {
            this.sprites = sprites;
            this.view = new ImageView(sprites[DIR_DOWN][0]);
            this.view.setFitWidth(TILE_SIZE);
            this.view.setFitHeight(TILE_SIZE);
            this.transition = new TranslateTransition(Duration.millis(150), view);

            this.transition.setOnFinished(e -> {
                this.isMoving = false;
                this.view.setImage(this.sprites[currentDir][0]);
            });

            gameGrid.add(view, 0, 0); // Placé en 0,0 puis translaté
        }
    }

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

        Number myId = NetworkManager.getInstance().getSelectedCharacterId();
        String botId = String.valueOf(ThreadLocalRandom.current().nextInt(1, 35));
        String[] dirs = {"S", "N", "W", "E"};

        for (int d = 0; d < 4; d++) {
            for (int f = 0; f < 3; f++) {
                playerSprites[d][f] = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/characters/" + myId + "/" + dirs[d] + "_" + f + ".png")));
                botSprites[d][f] = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/characters/" + botId + "/" + dirs[d] + "_" + f + ".png")));
            }
        }

        for (int i = 0; i < BOMB_IDLE_FRAMES; i++)
            bombIdleFrames[i] = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/bomb/B2_" + i + ".png")));

        for (int t = 0; t < EXPLOSION_SPRITE_TYPES; t++)
            for (int f = 0; f < EXPLOSION_FRAMES; f++)
                explosionFrames[t][f] = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/explosions/explosion_" + t + "_" + f + ".png")));
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

    private void drawMap(GameSnapshot snap) {
        // 1. Grille statique
        CellType[][] currentGrid = snap.getGrid();
        for (int x = 0; x < currentWidth; x++) {
            for (int y = 0; y < currentHeight; y++) {
                if (currentGrid[y][x] == CellType.WALL) tileViews[x][y].setImage(wallImg);
                else if (currentGrid[y][x] == CellType.BRICK) tileViews[x][y].setImage(brickImg);
                else tileViews[x][y].setImage(floorImg);
            }
        }

        // 2. HUD
        if(snap.getRemainingSeconds() >= 0) {
            timerLabel.setText(String.format("%02d:%02d", snap.getRemainingSeconds() / 60, snap.getRemainingSeconds() % 60));
        }

        // 3. Joueurs & Animations
        Set<Integer> aliveIds = new HashSet<>();
        for (GameSnapshot.PlayerState p : snap.getPlayers()) {
            aliveIds.add(p.id());

            // MAJ HUD du joueur local
            if (p.id() == 1) {
                hpLabel.setText(String.valueOf(p.hp()));
                bombsLabel.setText(p.currentBombs() + "/" + p.maxBombs());
            }

            if (p.isDead()) {
                PlayerVisual deadPV = playerVisuals.remove(p.id());
                if (deadPV != null) gameGrid.getChildren().remove(deadPV.view);
                continue;
            }

            PlayerVisual pv = playerVisuals.computeIfAbsent(p.id(), id -> new PlayerVisual(id == 1 ? playerSprites : botSprites));

            double newTargetX = p.x() * TILE_SIZE;
            double newTargetY = p.y() * TILE_SIZE;

            if (pv.targetX == -1) {
                pv.view.setTranslateX(newTargetX);
                pv.view.setTranslateY(newTargetY);
                pv.targetX = newTargetX;
                pv.targetY = newTargetY;
            } else if (newTargetX != pv.targetX || newTargetY != pv.targetY) {
                if (newTargetX > pv.targetX) pv.currentDir = DIR_RIGHT;
                else if (newTargetX < pv.targetX) pv.currentDir = DIR_LEFT;
                else if (newTargetY > pv.targetY) pv.currentDir = DIR_DOWN;
                else if (newTargetY < pv.targetY) pv.currentDir = DIR_UP;

                pv.targetX = newTargetX;
                pv.targetY = newTargetY;
                pv.isMoving = true;

                pv.transition.stop();
                pv.transition.setToX(newTargetX);
                pv.transition.setToY(newTargetY);
                pv.transition.play();
            }

            if (pv.isMoving) {
                int frame = (int) ((System.currentTimeMillis() / 100) % 2) + 1;
                pv.view.setImage(pv.sprites[pv.currentDir][frame]);
            }
        }

        // 4. Bombes (Position absolue)
        long now = System.currentTimeMillis();
        Set<Integer> liveBombIds = new HashSet<>();
        for (GameSnapshot.BombState b : snap.getBombs()) {
            liveBombIds.add(b.id());
            bombFirstSeen.computeIfAbsent(b.id(), id -> now);
            long elapsed = now - bombFirstSeen.get(b.id());
            int frame = (int) ((elapsed * BOMB_IDLE_FRAMES / BOMB_ANIM_TOTAL_MS) % BOMB_IDLE_FRAMES);

            ImageView bView = bombViews.computeIfAbsent(b.id(), id -> {
                ImageView v = new ImageView(); v.setFitWidth(TILE_SIZE); v.setFitHeight(TILE_SIZE);
                gameGrid.getChildren().add(v); return v;
            });
            bView.setImage(bombIdleFrames[frame]);
            bView.setTranslateX(b.x() * TILE_SIZE);
            bView.setTranslateY(b.y() * TILE_SIZE);
        }

        // 5. Explosions (Position absolue)
        Set<String> liveExplosionKeys = new HashSet<>();
        for (GameSnapshot.ExplosionState e : snap.getExplosions()) {
            String key = e.x() + "_" + e.y();
            liveExplosionKeys.add(key);
            int frame = (int) Math.min(e.ageMs() * EXPLOSION_FRAMES / EXPLOSION_ANIM_MS, EXPLOSION_FRAMES - 1);
            int type  = Math.min(Math.max(e.spriteType(), 0), EXPLOSION_SPRITE_TYPES - 1);

            ImageView eView = explosionViews.computeIfAbsent(key, k -> {
                ImageView v = new ImageView(); v.setFitWidth(TILE_SIZE); v.setFitHeight(TILE_SIZE);
                gameGrid.getChildren().add(v); return v;
            });
            eView.setImage(explosionFrames[type][frame]);
            eView.setTranslateX(e.x() * TILE_SIZE);
            eView.setTranslateY(e.y() * TILE_SIZE);
        }

        // --------------------------------------------------------
        // 6. NETTOYAGE FINAUX (Maintenant que les listes existent)
        // --------------------------------------------------------

        playerVisuals.keySet().removeIf(id -> {
            if(!aliveIds.contains(id)) {
                if(playerVisuals.get(id) != null) {
                    gameGrid.getChildren().remove(playerVisuals.get(id).view);
                }
                return true;
            }
            return false;
        });

        bombViews.keySet().removeIf(id -> {
            if(!liveBombIds.contains(id)) {
                gameGrid.getChildren().remove(bombViews.get(id));
                bombFirstSeen.remove(id);
                return true;
            }
            return false;
        });

        explosionViews.keySet().removeIf(k -> {
            if(!liveExplosionKeys.contains(k)) {
                gameGrid.getChildren().remove(explosionViews.get(k));
                return true;
            }
            return false;
        });
    }
}