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
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class GameBoardController {

    @FXML private GridPane gameGrid;

    private static final int MAZE_WIDTH = 15;
    private static final int MAZE_HEIGHT = 11;
    private static final int TILE_SIZE = 40;

    // Décors
    private ImageView[][] tileViews = new ImageView[MAZE_WIDTH][MAZE_HEIGHT];
    private Image wallImg, floorImg, brickImg;

    // Entités
    private Map<Integer, ImageView> playerViews = new HashMap<>();
    private Image player1Img, botImg;

    // Bombes
    private static final int    BOMB_IDLE_FRAMES       = 28;  // B2_0..B2_27 (clignotement avant explosion)
    private static final int    EXPLOSION_SPRITE_TYPES  = 7;   // types 0-6 de explosion_Z_F
    private static final int    EXPLOSION_FRAMES        = 14;  // frames 0-13 par type
    private static final long   EXPLOSION_ANIM_MS      = 700;
    private static final long   BOMB_ANIM_TOTAL_MS     = 3000; // DEFAULT_BOMB_DELAY

    private Image[]   bombIdleFrames  = new Image[BOMB_IDLE_FRAMES];
    private Image[][] explosionFrames = new Image[EXPLOSION_SPRITE_TYPES][EXPLOSION_FRAMES];

    /** ImageViews for active bombs, keyed by bomb ID. */
    private Map<Integer, ImageView> bombViews     = new HashMap<>();
    /** First time each bomb ID was seen in a snapshot (for frame animation). */
    private Map<Integer, Long>      bombFirstSeen = new HashMap<>();
    /** ImageViews for explosion cells, keyed by "x_y". */
    private Map<String, ImageView>  explosionViews = new HashMap<>();

    private Game game;

    @FXML
    public void initialize() {
        loadImages();
        initMap();
        startGame();

        // Ajout de l'écouteur clavier une fois que la scène est chargée
        Platform.runLater(() -> {
            gameGrid.getScene().setOnKeyPressed(this::handleKeyPress);
            gameGrid.getScene().getRoot().requestFocus(); // Assure que la fenêtre capte le clavier
        });
    }

    private void loadImages() {
        wallImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/walls/block_07.png")));
        floorImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/ground/ground_06.png")));
        brickImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/walls/block_08.png")));

        // Sprites des joueurs
        int randomId = ThreadLocalRandom.current().nextInt(1, 35);

        String spriteJoueur = "/sprites/output/characters/" + NetworkManager.getInstance().getSelectedCharacterId() + "/R_0.png";
        String spriteBot = "/sprites/output/characters/" + randomId + "/R_0.png";

        player1Img = new Image(Objects.requireNonNull(getClass().getResourceAsStream(spriteJoueur)));
        botImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream(spriteBot)));

        // Sprites de la bombe avant explosion (clignotement d'avertissement)
        for (int i = 0; i < BOMB_IDLE_FRAMES; i++) {
            bombIdleFrames[i] = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/sprites/output/bomb/B2_" + i + ".png")));
        }

        // Sprites d'explosion directionnels : explosion_Z_F.png
        // Z : 0=centre, 1=H-mid, 2=V-mid, 3=bout-haut, 4=bout-bas, 5=bout-droit, 6=bout-gauche
        for (int type = 0; type < EXPLOSION_SPRITE_TYPES; type++) {
            for (int f = 0; f < EXPLOSION_FRAMES; f++) {
                explosionFrames[type][f] = new Image(Objects.requireNonNull(
                        getClass().getResourceAsStream(
                                "/sprites/output/explosions/explosion_" + type + "_" + f + ".png")));
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

        // 1. Ajout du vrai joueur
        List<Player> players = new ArrayList<>();
        players.add(new Player(1, 1, 1, 3, 1.0, 1)); // Toi (ID 1)

        // 2. Initialisation du moteur de jeu
        game = new Game(grid, players);

        // 3. Création et ajout du Bot via la Factory
        AIPlayer bot = AIFactory.create(Strategy.SURVIVALIST, 2, 13, 9, 3, 1.0, 1);
        game.addBot(bot);

        // 4. Écoute des événements du jeu
        game.addListener(new GameStateListener() {
            @Override
            public void onGameStateUpdate(GameSnapshot snap) {
                Platform.runLater(() -> drawMap(snap));
            }
            @Override
            public void onPlayerDied(int id) {
                System.out.println("Mort de l'entité : " + id);
            }
            @Override
            public void onGameOver(int winnerId) {
                System.out.println("Fin de partie ! Gagnant : " + winnerId);
            }
        });

        // 5. Démarrage de la boucle de jeu
        game.start();
    }

    // --- CONTRÔLES CLAVIER ---
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

    // --- DESSIN (60 FPS) ---
    private void drawMap(GameSnapshot snap) {
        // 1. Met à jour les murs/sols
        CellType[][] currentGrid = snap.getGrid();
        for (int x = 0; x < MAZE_WIDTH; x++) {
            for (int y = 0; y < MAZE_HEIGHT; y++) {
                if (currentGrid[y][x] == CellType.WALL) tileViews[x][y].setImage(wallImg);
                else if (currentGrid[y][x] == CellType.BRICK) tileViews[x][y].setImage(brickImg);
                else tileViews[x][y].setImage(floorImg);
            }
        }

        // 2. Bombes actives (pas encore explosées)
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
            GridPane.setColumnIndex(bView, b.x());
            GridPane.setRowIndex(bView, b.y());
        }
        // Nettoyer les bombes disparues
        Set<Integer> staleBombIds = new HashSet<>(bombViews.keySet());
        staleBombIds.removeAll(liveBombIds);
        for (int id : staleBombIds) {
            gameGrid.getChildren().remove(bombViews.remove(id));
            bombFirstSeen.remove(id);
        }

        // 3. Cellules en explosion
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
            GridPane.setColumnIndex(eView, e.x());
            GridPane.setRowIndex(eView, e.y());
        }
        // Nettoyer les cellules d'explosion disparues
        Set<String> staleExplosionKeys = new HashSet<>(explosionViews.keySet());
        staleExplosionKeys.removeAll(liveExplosionKeys);
        for (String key : staleExplosionKeys) {
            gameGrid.getChildren().remove(explosionViews.remove(key));
        }

        // 4. Met à jour les joueurs (Toi et le Bot)
        for (GameSnapshot.PlayerState p : snap.getPlayers()) {
            if (p.isDead()) {
                // Si le joueur est mort, on retire son image de la grille
                ImageView deadView = playerViews.remove(p.id());
                if (deadView != null) {
                    gameGrid.getChildren().remove(deadView);
                }
                continue;
            }

            // Si l'ImageView n'existe pas encore pour ce joueur, on la crée
            ImageView pView = playerViews.computeIfAbsent(p.id(), id -> {
                ImageView v = new ImageView(id == 1 ? player1Img : botImg);
                v.setFitWidth(TILE_SIZE);
                v.setFitHeight(TILE_SIZE);
                gameGrid.getChildren().add(v); // On l'ajoute par dessus la grille
                return v;
            });

            // On déplace l'image du joueur dans la grille
            GridPane.setColumnIndex(pView, p.x());
            GridPane.setRowIndex(pView, p.y());
        }
    }
}