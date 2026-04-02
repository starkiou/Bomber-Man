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

    private Image[] bombIdleFrames = new Image[28];
    private Image[][] explosionFrames = new Image[7][14];
    private Map<Integer, ImageView> bombViews = new HashMap<>();
    private Map<String, ImageView> explosionViews = new HashMap<>();

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
        // Chargement des 28 images de l'animation de la bombe
        for (int i = 0; i < 28; i++) {
            bombIdleFrames[i] = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/sprites/output/bomb/B2_" + i + ".png")));
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
        addBots();

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

    private void addBots() {
        List<Strategy> strategies = GameConfigController.selectedStrategies;

        // Positions de spawn prédéfinies pour chaque bot (évite les chevauchements)
        int[][] spawnPositions = {
                { currentWidth - 2, currentHeight - 2 }, // bot 0 : bas-droit
                { 1,                currentHeight - 2 }, // bot 1 : bas-gauche
                { currentWidth - 2, 1                }, // bot 2 : haut-droit
                { currentWidth / 2, currentHeight - 2 }, // bot 3 : bas-milieu
        };

        for (int i = 0; i < strategies.size(); i++) {
            Strategy strategy = strategies.get(i);
            int botId = i + 2; // les IDs bots commencent à 2 (1 = joueur local)
            int[] pos = spawnPositions[Math.min(i, spawnPositions.length - 1)];

            game.addBot(AIFactory.create(strategy, botId, pos[0], pos[1],3, 1.0, GameConfigController.selectedBombs));

            System.out.println(" Bot " + botId + " ajouté — stratégie : " + strategy + " en (" + pos[0] + ", " + pos[1] + ")");
        }
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
        // 1. RENDU DE LA GRILLE (SOL, MURS, BRIQUES)
        CellType[][] grid = snap.getGrid();
        for (int x = 0; x < currentWidth; x++) {
            for (int y = 0; y < currentHeight; y++) {
                CellType type = grid[y][x];
                if (type == CellType.WALL) tileViews[x][y].setImage(wallImg);
                else if (type == CellType.BRICK) tileViews[x][y].setImage(brickImg);
                else tileViews[x][y].setImage(floorImg);
            }
        }

        // 2. RENDU DES BOMBES (ANIMATION)
        Set<Integer> activeBombIds = new HashSet<>();
        for (GameSnapshot.BombState b : snap.getBombs()) {
            activeBombIds.add(b.id());

            // Animation simple : on fait défiler les 28 frames toutes les 100ms
            int frame = (int) ((System.currentTimeMillis() / 100) % 28);

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
        // Nettoyage des bombes disparues
        bombViews.keySet().removeIf(id -> {
            if (!activeBombIds.contains(id)) {
                gameGrid.getChildren().remove(bombViews.get(id));
                return true;
            }
            return false;
        });

        // 3. RENDU DES EXPLOSIONS
        Set<String> activeExplosionKeys = new HashSet<>();
        for (GameSnapshot.ExplosionState e : snap.getExplosions()) {
            String key = e.x() + "_" + e.y();
            activeExplosionKeys.add(key);

            // Calcul de la frame d'explosion (on a 14 frames par type)
            int frame = (int) Math.min(e.ageMs() / 50, 13);

            ImageView eView = explosionViews.computeIfAbsent(key, k -> {
                ImageView v = new ImageView();
                v.setFitWidth(TILE_SIZE);
                v.setFitHeight(TILE_SIZE);
                gameGrid.getChildren().add(v);
                return v;
            });
            // Utilise le spriteType envoyé par le moteur pour choisir la bonne direction
            eView.setImage(explosionFrames[e.spriteType()][frame]);
            GridPane.setColumnIndex(eView, e.x());
            GridPane.setRowIndex(eView, e.y());
        }
        // Nettoyage des explosions terminées
        explosionViews.keySet().removeIf(key -> {
            if (!activeExplosionKeys.contains(key)) {
                gameGrid.getChildren().remove(explosionViews.get(key));
                return true;
            }
            return false;
        });

        // 4. MISE À JOUR DU TIMER
        long sec = snap.getRemainingSeconds();
        if (timerLabel != null) {
            timerLabel.setText(String.format("%02d:%02d", sec / 60, sec % 60));
        }

        // 5. MISE À JOUR DES JOUEURS ET HUD
        for (GameSnapshot.PlayerState p : snap.getPlayers()) {
            if (p.id() == 1) { // Ton joueur local
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