package clientside.controllers;

import clientside.network.NetworkManager;
import model.game.Game;
import model.game.GameSnapshot;
import model.game.GameStateListener;
import model.maze.MazeFactory;
import model.maze.CellType;
import model.entity.Player;
import model.aiPlayer.AIFactory;
import model.aiPlayer.Strategy;
import model.aiPlayer.AIPlayer;
import network.message.ActionType;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GameBoardController {

    @FXML private GridPane gameGrid;

    private static final int MAZE_WIDTH = 15;
    private static final int MAZE_HEIGHT = 11;
    private static final int TILE_SIZE = 40;

    // Décors
    private ImageView[][] tileViews = new ImageView[MAZE_WIDTH][MAZE_HEIGHT];
    private Image wallImg, floorImg, brickImg;

    // Constantes Directions (Index du tableau de sprites)
    private static final int DIR_DOWN = 0;
    private static final int DIR_UP = 1;
    private static final int DIR_LEFT = 2;
    private static final int DIR_RIGHT = 3;

    // Entités
    private Map<Integer, PlayerVisual> playerVisuals = new HashMap<>();
    private Image[][] playerSprites = new Image[4][3]; // [Direction][Frame 0,1,2]
    private Image[][] botSprites = new Image[4][3];

    // Bombes
    private static final int BOMB_IDLE_FRAMES = 28;
    private static final int EXPLOSION_SPRITE_TYPES = 7;
    private static final int EXPLOSION_FRAMES = 14;
    private static final long EXPLOSION_ANIM_MS = 700;
    private static final long BOMB_ANIM_TOTAL_MS = 3000;

    private Image[] bombIdleFrames = new Image[BOMB_IDLE_FRAMES];
    private Image[][] explosionFrames = new Image[EXPLOSION_SPRITE_TYPES][EXPLOSION_FRAMES];

    private Map<Integer, ImageView> bombViews = new HashMap<>();
    private Map<Integer, Long> bombFirstSeen = new HashMap<>();
    private Map<String, ImageView> explosionViews = new HashMap<>();

    private Game game;

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

            // À la fin du mouvement, on remet le sprite à l'arrêt (frame 0)
            this.transition.setOnFinished(e -> {
                this.isMoving = false;
                this.view.setImage(this.sprites[currentDir][0]);
            });

            gameGrid.add(view, 0, 0); // Placé en 0,0 puis translaté en pixels
        }
    }

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

        Number myId = NetworkManager.getInstance().getSelectedCharacterId();
        String botId = String.valueOf(ThreadLocalRandom.current().nextInt(1, 35));

        String[] dirs = {"S", "N", "W", "E"};

        for (int d = 0; d < 4; d++) {
            for (int f = 0; f < 3; f++) {
                String pPath = "/sprites/output/characters/" + myId + "/" + dirs[d] + "_" + f + ".png";
                String bPath = "/sprites/output/characters/" + botId + "/" + dirs[d] + "_" + f + ".png";
                playerSprites[d][f] = new Image(Objects.requireNonNull(getClass().getResourceAsStream(pPath)));
                botSprites[d][f] = new Image(Objects.requireNonNull(getClass().getResourceAsStream(bPath)));
            }
        }

        // Bombes et explosions
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
            @Override
            public void onPlayerDied(int id) {
                System.out.println("Mort : " + id);
            }
            @Override
            public void onGameOver(int winnerId) {
                System.out.println("Gagnant : " + winnerId);
            }
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
        CellType[][] currentGrid = snap.getGrid();
        for (int x = 0; x < MAZE_WIDTH; x++) {
            for (int y = 0; y < MAZE_HEIGHT; y++) {
                if (currentGrid[y][x] == CellType.WALL) tileViews[x][y].setImage(wallImg);
                else if (currentGrid[y][x] == CellType.BRICK) tileViews[x][y].setImage(brickImg);
                else tileViews[x][y].setImage(floorImg);
            }
        }

        // --- GESTION DES JOUEURS ET ANIMATIONS ---
        for (GameSnapshot.PlayerState p : snap.getPlayers()) {
            if (p.isDead()) {
                PlayerVisual deadPV = playerVisuals.remove(p.id());
                if (deadPV != null) gameGrid.getChildren().remove(deadPV.view);
                continue;
            }

            PlayerVisual pv = playerVisuals.computeIfAbsent(p.id(), id -> new PlayerVisual(id == 1 ? playerSprites : botSprites));

            double newTargetX = p.x() * TILE_SIZE;
            double newTargetY = p.y() * TILE_SIZE;

            // Spawn initial (Pas d'animation)
            if (pv.targetX == -1) {
                pv.view.setTranslateX(newTargetX);
                pv.view.setTranslateY(newTargetY);
                pv.targetX = newTargetX;
                pv.targetY = newTargetY;
            }
            // Si la cible a changé (Le joueur a bougé d'une case)
            else if (newTargetX != pv.targetX || newTargetY != pv.targetY) {
                // Détermination de la direction
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

            // Alternance des sprites de marche (1 et 2) pendant le glissement
            if (pv.isMoving) {
                // Change de frame toutes les 100ms environ (frame 1 ou 2)
                int frame = (int) ((System.currentTimeMillis() / 100) % 2) + 1;
                pv.view.setImage(pv.sprites[pv.currentDir][frame]);
            }
        }

        // Bombes ...
        long now = System.currentTimeMillis();
        Set<Integer> liveBombIds = new HashSet<>();
        for (GameSnapshot.BombState b : snap.getBombs()) {
            liveBombIds.add(b.id());
            bombFirstSeen.computeIfAbsent(b.id(), id -> now);
            long elapsed = now - bombFirstSeen.get(b.id());
            int frame = (int) ((elapsed * BOMB_IDLE_FRAMES / BOMB_ANIM_TOTAL_MS) % BOMB_IDLE_FRAMES);

            ImageView bView = bombViews.computeIfAbsent(b.id(), id -> {
                ImageView v = new ImageView();
                v.setFitWidth(TILE_SIZE);
                v.setFitHeight(TILE_SIZE);
                gameGrid.getChildren().add(v);
                return v;
            });
            bView.setImage(bombIdleFrames[frame]);
            bView.setTranslateX(b.x() * TILE_SIZE);
            bView.setTranslateY(b.y() * TILE_SIZE);
        }

        Set<Integer> staleBombIds = new HashSet<>(bombViews.keySet());
        staleBombIds.removeAll(liveBombIds);
        for (int id : staleBombIds) {
            gameGrid.getChildren().remove(bombViews.remove(id));
            bombFirstSeen.remove(id);
        }

        // Explosions ...
        Set<String> liveExplosionKeys = new HashSet<>();
        for (GameSnapshot.ExplosionState e : snap.getExplosions()) {
            String key = e.x() + "_" + e.y();
            liveExplosionKeys.add(key);
            int frame = (int) Math.min(e.ageMs() * EXPLOSION_FRAMES / EXPLOSION_ANIM_MS, EXPLOSION_FRAMES - 1);
            int type  = Math.min(Math.max(e.spriteType(), 0), EXPLOSION_SPRITE_TYPES - 1);

            ImageView eView = explosionViews.computeIfAbsent(key, k -> {
                ImageView v = new ImageView();
                v.setFitWidth(TILE_SIZE);
                v.setFitHeight(TILE_SIZE);
                gameGrid.getChildren().add(v);
                return v;
            });
            eView.setImage(explosionFrames[type][frame]);
            eView.setTranslateX(e.x() * TILE_SIZE);
            eView.setTranslateY(e.y() * TILE_SIZE);
        }

        Set<String> staleExplosionKeys = new HashSet<>(explosionViews.keySet());
        staleExplosionKeys.removeAll(liveExplosionKeys);
        for (String key : staleExplosionKeys) {
            gameGrid.getChildren().remove(explosionViews.remove(key));
        }
    }
}