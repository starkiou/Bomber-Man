package clientside.controllers;

import clientside.network.NetworkManager;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
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
import network.message.ClientInfoDTO;
import network.message.LaunchGameMessage;
import network.message.Message;
import network.message.PlayActionMessage;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GameBoardController {

    @FXML private GridPane gameGrid;
    @FXML private Label hpLabel, bombsLabel, timerLabel, modeLabel;

    private static final int TILE_SIZE = 40;

    /** Délai entre deux déplacements d'une case quand une touche reste enfoncée. */
    private static final long MOVE_INTERVAL_MS = 150;

    // La planche d'explosions contient une animation de 7 images par ligne :
    // ligne 0 = centre, ligne 2 = flamme horizontale, ligne 3 = flamme verticale.
    private static final int EXPLOSION_FRAMES = 7;
    private static final int[] EXPLOSION_ROWS = {0, 2, 3};
    private static final int EXPLO_CENTER = 0, EXPLO_HORIZONTAL = 1, EXPLO_VERTICAL = 2;

    // Directions
    private static final int DIR_DOWN  = 0;
    private static final int DIR_UP    = 1;
    private static final int DIR_LEFT  = 2;
    private static final int DIR_RIGHT = 3;
    /** Ligne des images de mort dans le tableau de sprites d'un personnage. */
    private static final int DEATH_ROW = 4;
    private static final int DEATH_FRAME_MS = 200;

    private Image wallImg, floorImg, brickImg;
    private final Image[] bombIdleFrames = new Image[28];
    private final Image[][] explosionFrames  = new Image[3][EXPLOSION_FRAMES];

    // Caches et Mappings
    private final Map<Integer, Image[][]> entitySpritesCache = new HashMap<>();
    private final Map<Integer, Integer>   playerSkinMap      = new HashMap<>();

    private final Map<Integer, PlayerVisual> playerVisuals = new HashMap<>();
    private final Map<Integer, ImageView>    bombViews     = new HashMap<>();
    private final Map<Integer, Long>         bombFirstSeen = new HashMap<>();
    private final Map<String,  ImageView>    explosionViews = new HashMap<>();

    private Game    game;
    private int     currentWidth, currentHeight;
    private ImageView[][] tileViews;
    private long    startTime;

    // Multiplayer state
    private boolean isOnline   = false;
    private int     myPlayerId = 1;

    // Clavier : directions maintenues, la plus récente en tête
    private final Deque<ActionType> heldMoves = new ArrayDeque<>();
    private boolean bombKeyHeld = false;
    private long    lastMoveSentMs = 0;
    private AnimationTimer inputTimer;
    private Scene   boundScene;

    private boolean gameOverShown = false;

    // ── Sprite d'animation de mouvement ──────────────────────────────────────

    private class PlayerVisual {
        ImageView view;
        Image[][] sprites;
        TranslateTransition transition;
        double targetX = -1, targetY = -1;
        int  currentDir = DIR_DOWN;
        boolean isMoving = false;
        boolean dying = false;

        PlayerVisual(Image[][] sprites) {
            this.sprites = sprites;
            this.view    = new ImageView(sprites[DIR_DOWN][0]);
            this.view.setFitWidth(TILE_SIZE);
            this.view.setFitHeight(TILE_SIZE);
            this.transition = new TranslateTransition(Duration.millis(150), view);
            this.transition.setOnFinished(e -> {
                isMoving = false;
                view.setImage(this.sprites[currentDir][0]);
            });
            // Ajout en 0,0 pour permettre le TranslateTransition par dessus la grille
            gameGrid.add(view, 0, 0);
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        loadImages();
        startGame();

        NetworkManager.getInstance().setMessageHandler(this::onMessageReceived);

        // Le plateau n'est pas encore attaché à la scène pendant le chargement du FXML :
        // on branche le clavier dès qu'il l'est, sans dépendre de l'ordre des runLater.
        if (gameGrid.getScene() != null) {
            bindKeyboard(gameGrid.getScene());
        } else {
            gameGrid.sceneProperty().addListener((obs, oldScene, scene) -> {
                if (scene != null && boundScene == null) bindKeyboard(scene);
            });
        }
    }

    private void onMessageReceived(Message msg) {
        if (msg instanceof PlayActionMessage pam) {
            if (game != null) {
                game.handleAction(pam.getPlayerID(), pam.getActionType());
            }
        }
    }

    // ── Chargement des sprites ───────────────────────────────────────────────

    private void loadImages() {
        wallImg  = img("/sprites/output/walls/block_07.png");
        floorImg = img("/sprites/output/ground/ground_06.png");
        brickImg = img("/sprites/output/walls/block_08.png");

        for (int i = 0; i < 28; i++) {
            bombIdleFrames[i] = img("/sprites/output/bomb/B2_" + i + ".png");
        }

        for (int t = 0; t < EXPLOSION_ROWS.length; t++) {
            for (int f = 0; f < EXPLOSION_FRAMES; f++) {
                explosionFrames[t][f] = img("/sprites/output/explosions/explosion_" + EXPLOSION_ROWS[t] + "_" + f + ".png");
            }
        }
    }

    private Image img(String path) {
        return new Image(Objects.requireNonNull(getClass().getResourceAsStream(path)));
    }

    private Image[][] getOrLoadSprites(int entityId) {
        return entitySpritesCache.computeIfAbsent(entityId, id -> {
            Image[][] sprites = new Image[5][3];

            String skinId;
            if (id == myPlayerId) {
                skinId = String.valueOf(NetworkManager.getInstance().getSelectedCharacterId());
            } else if (playerSkinMap.containsKey(id)) {
                skinId = String.valueOf(playerSkinMap.get(id));
            } else {
                int random;
                do {
                    random = ThreadLocalRandom.current().nextInt(1, 35);
                } while (CharacterSelectionController.BROKEN_SKINS.contains(random));
                skinId = String.valueOf(random);
            }

            // S = face (vers le bas), N = dos, W = gauche, E = droite.
            // Les fichiers D_* sont l'animation de mort (DEATH_ROW), pas la direction « down ».
            String[] dirs = {"S", "N", "W", "E", "D"};
            for (int d = 0; d < dirs.length; d++) {
                for (int f = 0; f < 3; f++) {
                    sprites[d][f] = img("/sprites/output/characters/" + skinId + "/" + dirs[d] + "_" + f + ".png");
                }
            }
            return sprites;
        });
    }

    // ── Démarrage de la partie ───────────────────────────────────────────────

    private void startGame() {
        this.startTime = System.currentTimeMillis();

        LaunchGameMessage config = NetworkManager.getInstance().getPendingLaunch();
        CellType[][] grid;
        List<Player> players = new ArrayList<>();
        int botCount;

        int[][] spawnPos;
        int spawnIdx = 0;

        if (config != null && config.getGrid() != null && !config.getPlayers().isEmpty()) {
            isOnline = true;
            grid     = config.getGrid();
            botCount = config.getBotCount();
            currentWidth  = grid[0].length;
            currentHeight = grid.length;
            spawnPos = cornerSpawns(currentWidth, currentHeight);

            String myNick = NetworkManager.getInstance().getNickname();
            List<ClientInfoDTO> roomPlayers = config.getPlayers();
            boolean foundMe = false;

            for (int i = 0; i < roomPlayers.size() && spawnIdx < spawnPos.length; i++) {
                // 1. RÉCUPÉRER LE VRAI ID DU SERVEUR
                int pid = roomPlayers.get(i).getClientId();

                int[] pos = spawnPos[spawnIdx++];

                // 2. CRÉER L'ENTITÉ AVEC LE VRAI ID
                players.add(new Player(pid, pos[0], pos[1], 3, 1.0, 1));
                playerSkinMap.put(pid, roomPlayers.get(i).getCharacterId());

                // 3. IDENTIFIER NOTRE PROPRE JOUEUR
                if (myNick != null && roomPlayers.get(i).getPseudo() != null &&
                        myNick.trim().equalsIgnoreCase(roomPlayers.get(i).getPseudo().trim())) {
                    myPlayerId = pid;
                    foundMe = true;
                }
            }

            if (!foundMe) {
                System.err.println("⚠️ Attention: Pseudo non trouvé (" + myNick + "). myPlayerId forcé à 1 !");
            }

        } else {
            isOnline = false;
            configureDimensions();
            spawnPos = cornerSpawns(currentWidth, currentHeight);
            grid     = MazeFactory.createMaze(MazeFactory.Algorithm.EXHAUSTIVE, currentWidth, currentHeight);
            botCount = GameConfigController.selectedBots;
            myPlayerId = 1;
            players.add(new Player(1, spawnPos[spawnIdx][0], spawnPos[spawnIdx][1], 3, 1.0, GameConfigController.selectedBombs));
            spawnIdx++;
        }

        tileViews = new ImageView[currentWidth][currentHeight];
        gameGrid.getChildren().clear();
        initBackgroundGrid();

        int duration = isOnline ? -1 : GameConfigController.selectedTime;
        game = new Game(grid, players, duration);

        // Les identifiants des joueurs en ligne viennent du serveur et ne partent pas de 1 :
        // les bots prennent la suite du plus grand pour ne jamais remplacer un joueur.
        int botStartId = players.stream().mapToInt(Player::getId).max().orElse(0) + 1;
        for (int i = 0; i < botCount && spawnIdx < spawnPos.length; i++) {
            int bid   = botStartId + i;
            int[] pos = spawnPos[spawnIdx++];
            game.addBot(AIFactory.create(randomBotStrategy(), bid, pos[0], pos[1], 3, 1.0, 1));
        }

        modeLabel.setText(isOnline ? "EN LIGNE" : "HORS LIGNE");

        game.addListener(new GameStateListener() {
            @Override public void onGameStateUpdate(GameSnapshot snap) { Platform.runLater(() -> drawMap(snap)); }
            @Override public void onPlayerDied(int id) {
                // En solo, la partie s'arrête dès que le joueur est éliminé
                if (!isOnline && id == myPlayerId) {
                    game.stop();
                    Platform.runLater(() -> scheduleGameOverScreen(-1));
                }
            }
            @Override public void onGameOver(int winnerId) { Platform.runLater(() -> scheduleGameOverScreen(winnerId)); }
        });

        game.start();
    }

    /**
     * Tire au sort la stratégie d'un bot. Plus la difficulté choisie dans le menu
     * est élevée, plus les stratégies offensives sont probables.
     */
    private Strategy randomBotStrategy() {
        String difficulty = GameConfigController.selectedDifficulty;
        // Poids en pourcentage : {survivaliste, tactique, agressif}
        int[] weights;
        if (difficulty == null)                    weights = new int[]{25, 50, 25};
        else if (difficulty.contains("Facile"))    weights = new int[]{70, 30, 0};
        else if (difficulty.contains("Difficile")) weights = new int[]{10, 40, 50};
        else if (difficulty.contains("Extr"))      weights = new int[]{0, 25, 75};
        else                                       weights = new int[]{25, 50, 25};

        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < weights[0]) return Strategy.SURVIVALIST;
        if (roll < weights[0] + weights[1]) return Strategy.TACTICAL;
        return Strategy.AGGRESSIVE;
    }

    private void configureDimensions() {
        String size = GameConfigController.selectedMapSize;
        if (size != null && size.contains("Petite"))      { currentWidth = 11; currentHeight = 11; }
        else if (size != null && size.contains("Grande")) { currentWidth = 19; currentHeight = 19; }
        else                                               { currentWidth = 15; currentHeight = 15; }
    }

    /** Positions de spawn aux quatre coins, adaptées aux dimensions réelles de la carte. */
    private int[][] cornerSpawns(int w, int h) {
        return new int[][] { {1, 1}, {w - 2, h - 2}, {w - 2, 1}, {1, h - 2} };
    }

    private void initBackgroundGrid() {
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

    // ── Fin de partie ────────────────────────────────────────────────────────

    /** Affiche l'écran de fin après un court délai, le temps que l'animation de mort se joue. */
    private void scheduleGameOverScreen(int winnerId) {
        PauseTransition delay = new PauseTransition(Duration.millis(3 * DEATH_FRAME_MS + 300));
        delay.setOnFinished(e -> showGameOverScreen(winnerId));
        delay.play();
    }

    private void showGameOverScreen(int winnerId) {
        if (gameOverShown) return;
        gameOverShown = true;
        unbindKeyboard();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientside/game-over.fxml"));
            Parent overlay = loader.load();

            GameOverController ctrl = loader.getController();
            long elapsed = System.currentTimeMillis() - startTime;
            String timeStr = String.format("%02d:%02d", elapsed / 60000, (elapsed / 1000) % 60);
            boolean iAmAlive = game.getSnapshot().getPlayers().stream()
                    .anyMatch(p -> p.id() == myPlayerId && !p.isDead());
            String title = winnerId == myPlayerId ? "VICTOIRE !"
                    : (winnerId == -1 && iAmAlive) ? "ÉGALITÉ" : "DÉFAITE...";
            ctrl.setStats(title, timeStr, game.getKills(myPlayerId));

            StackPane root = (StackPane) gameGrid.getScene().getRoot();
            root.getChildren().add(overlay);
            overlay.toFront();

            // La partie est terminée : on détache ce contrôleur du flux réseau
            // pour éviter qu'un contrôleur fantôme continue de traiter les messages.
            NetworkManager.getInstance().setMessageHandler(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Rendu ────────────────────────────────────────────────────────────────

    private void drawMap(GameSnapshot snap) {
        CellType[][] grid = snap.getGrid();
        for (int x = 0; x < currentWidth; x++) {
            for (int y = 0; y < currentHeight; y++) {
                tileViews[x][y].setImage(
                        grid[y][x] == CellType.WALL  ? wallImg  :
                                grid[y][x] == CellType.BRICK ? brickImg : floorImg);
            }
        }

        renderDynamicObjects(snap);
        renderPlayers(snap);

        long rem = snap.getRemainingSeconds();
        timerLabel.setText(rem < 0 ? "--:--"
                : String.format("%02d:%02d", rem / 60, rem % 60));
    }

    private void renderDynamicObjects(GameSnapshot snap) {
        // Bombes
        Set<Integer> activeBombs = new HashSet<>();
        for (var b : snap.getBombs()) {
            activeBombs.add(b.id());
            long firstSeen = bombFirstSeen.computeIfAbsent(b.id(), id -> System.currentTimeMillis());
            int frame = (int) ((System.currentTimeMillis() - firstSeen) / 100 % 28);
            ImageView v = bombViews.computeIfAbsent(b.id(), id -> {
                ImageView iv = new ImageView(); iv.setFitWidth(TILE_SIZE); iv.setFitHeight(TILE_SIZE);
                gameGrid.add(iv, 0, 0);
                return iv;
            });
            v.setImage(bombIdleFrames[frame]);
            v.setTranslateX(b.x() * TILE_SIZE);
            v.setTranslateY(b.y() * TILE_SIZE);
        }

        bombViews.keySet().removeIf(id -> {
            if (activeBombs.contains(id)) return false;
            gameGrid.getChildren().remove(bombViews.get(id));
            bombFirstSeen.remove(id);
            return true;
        });

        // Explosions
        Set<String> activeExplosions = new HashSet<>();
        for (var e : snap.getExplosions()) {
            String key = e.x() + "_" + e.y(); activeExplosions.add(key);
            ImageView v = explosionViews.computeIfAbsent(key, k -> {
                ImageView iv = new ImageView(); iv.setFitWidth(TILE_SIZE); iv.setFitHeight(TILE_SIZE);
                gameGrid.add(iv, 0, 0);
                return iv;
            });
            // Types du moteur : 0 centre ; 1 et 5 bras droit, 6 et 7 bras gauche ;
            // 2 et 3 bras du haut, 4 et 8 bras du bas.
            int visualType = switch (e.spriteType()) {
                case 0          -> EXPLO_CENTER;
                case 1, 5, 6, 7 -> EXPLO_HORIZONTAL;
                default         -> EXPLO_VERTICAL;
            };
            // Les images sont dessinées vers la gauche et vers le bas : on les
            // retourne pour les bras opposés, afin que la flamme parte de la bombe.
            v.setScaleX(e.spriteType() == 1 || e.spriteType() == 5 ? -1 : 1);
            v.setScaleY(e.spriteType() == 2 || e.spriteType() == 3 ? -1 : 1);
            int frame = (int) Math.min(e.ageMs() / 100, EXPLOSION_FRAMES - 1);
            v.setImage(explosionFrames[visualType][frame]);
            v.setTranslateX(e.x() * TILE_SIZE);
            v.setTranslateY(e.y() * TILE_SIZE);
        }
        explosionViews.keySet().removeIf(k -> {
            if (activeExplosions.contains(k)) return false;
            gameGrid.getChildren().remove(explosionViews.get(k)); return true;
        });
    }

    private void renderPlayers(GameSnapshot snap) {
        Set<Integer> aliveIds = new HashSet<>();
        for (var p : snap.getPlayers()) {
            aliveIds.add(p.id());

            if (p.id() == myPlayerId) {
                hpLabel.setText(String.valueOf(p.hp()));
                bombsLabel.setText(p.currentBombs() + "/" + p.maxBombs());
            }

            if (p.isDead()) {
                PlayerVisual dead = playerVisuals.get(p.id());
                if (dead != null && !dead.dying) playDeath(dead);
                continue;
            }

            PlayerVisual pv = playerVisuals.computeIfAbsent(p.id(),
                    id -> new PlayerVisual(getOrLoadSprites(id)));

            double nx = p.x() * TILE_SIZE;
            double ny = p.y() * TILE_SIZE;

            if (pv.targetX < 0) {
                pv.view.setTranslateX(nx); pv.view.setTranslateY(ny);
                pv.targetX = nx; pv.targetY = ny;
            } else if (nx != pv.targetX || ny != pv.targetY) {
                if      (nx > pv.targetX) pv.currentDir = DIR_RIGHT;
                else if (nx < pv.targetX) pv.currentDir = DIR_LEFT;
                else if (ny > pv.targetY) pv.currentDir = DIR_DOWN;
                else                      pv.currentDir = DIR_UP;

                pv.targetX = nx; pv.targetY = ny; pv.isMoving = true;
                pv.transition.stop();
                pv.transition.setToX(nx); pv.transition.setToY(ny);
                pv.transition.play();
            }

            if (pv.isMoving) {
                int frame = (int) ((System.currentTimeMillis() / 100) % 2) + 1;
                pv.view.setImage(pv.sprites[pv.currentDir][frame]);
            }
        }

        playerVisuals.keySet().removeIf(id -> {
            if (aliveIds.contains(id)) return false;
            if (playerVisuals.get(id) != null)
                gameGrid.getChildren().remove(playerVisuals.get(id).view);
            return true;
        });
    }

    /** Joue les trois images de mort du personnage, puis le retire du plateau. */
    private void playDeath(PlayerVisual pv) {
        pv.dying = true;
        pv.isMoving = false;
        pv.transition.stop();

        Timeline death = new Timeline();
        for (int f = 0; f < 3; f++) {
            Image frame = pv.sprites[DEATH_ROW][f];
            death.getKeyFrames().add(new KeyFrame(Duration.millis(f * DEATH_FRAME_MS), e -> pv.view.setImage(frame)));
        }
        death.getKeyFrames().add(new KeyFrame(Duration.millis(3 * DEATH_FRAME_MS),
                e -> gameGrid.getChildren().remove(pv.view)));
        death.play();
    }

    // ── Clavier ──────────────────────────────────────────────────────────────

    private void bindKeyboard(Scene scene) {
        boundScene = scene;
        scene.getRoot().requestFocus();
        scene.setOnKeyPressed(this::handleKeyPress);
        scene.setOnKeyReleased(this::handleKeyRelease);

        // Une touche maintenue déplace à vitesse constante, sans dépendre de la
        // répétition automatique du clavier.
        inputTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                ActionType move = heldMoves.peekFirst();
                long nowMs = System.currentTimeMillis();
                if (move != null && nowMs - lastMoveSentMs >= MOVE_INTERVAL_MS) {
                    lastMoveSentMs = nowMs;
                    sendAction(move);
                }
            }
        };
        inputTimer.start();
    }

    private void unbindKeyboard() {
        if (inputTimer != null) inputTimer.stop();
        heldMoves.clear();
        if (boundScene != null) {
            boundScene.setOnKeyPressed(null);
            boundScene.setOnKeyReleased(null);
        }
    }

    private ActionType moveFor(KeyCode code) {
        return switch (code) {
            case Z, UP    -> ActionType.MOVE_UP;
            case S, DOWN  -> ActionType.MOVE_DOWN;
            case Q, LEFT  -> ActionType.MOVE_LEFT;
            case D, RIGHT -> ActionType.MOVE_RIGHT;
            default       -> null;
        };
    }

    private void handleKeyPress(KeyEvent event) {
        if (game == null) return;
        if (event.getCode() == KeyCode.SPACE) {
            // Une bombe par appui, même si la touche reste enfoncée
            if (!bombKeyHeld) {
                bombKeyHeld = true;
                sendAction(ActionType.PLACE_BOMB);
            }
            return;
        }
        ActionType move = moveFor(event.getCode());
        if (move != null && !heldMoves.contains(move)) {
            heldMoves.addFirst(move);
        }
    }

    private void handleKeyRelease(KeyEvent event) {
        if (event.getCode() == KeyCode.SPACE) {
            bombKeyHeld = false;
            return;
        }
        ActionType move = moveFor(event.getCode());
        if (move != null) heldMoves.remove(move);
    }

    private void sendAction(ActionType action) {
        if (isOnline) {
            NetworkManager.getInstance().sendMessage(new PlayActionMessage(myPlayerId, action));
        } else {
            game.handleAction(myPlayerId, action);
        }
    }
}
