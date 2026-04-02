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
import network.message.ClientInfoDTO;
import network.message.LaunchGameMessage;
import network.message.Message;
import network.message.PlayActionMessage;
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

public class GameBoardController {

    @FXML private GridPane gameGrid;

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

    private Image[]   bombIdleFrames  = new Image[BOMB_IDLE_FRAMES];
    private Image[][] explosionFrames = new Image[EXPLOSION_SPRITE_TYPES][EXPLOSION_FRAMES];

    private Map<Integer, ImageView> bombViews     = new HashMap<>();
    private Map<Integer, Long>      bombFirstSeen = new HashMap<>();
    private Map<String, ImageView>  explosionViews = new HashMap<>();

    private Game game;
    private int myPlayerId = 1;
    private boolean isOnline = false; // true = partie multi, actions passent par le serveur

    @FXML
    public void initialize() {
        loadImages();
        initMap();
        startGame();

        // handler réseau : reçoit les actions broadcastées par le serveur
        NetworkManager.getInstance().setMessageHandler(this::onMessageReceived);

        Platform.runLater(() -> {
            gameGrid.getScene().setOnKeyPressed(this::handleKeyPress);
            gameGrid.getScene().getRoot().requestFocus();
        });
    }

    private void onMessageReceived(Message msg) {
        if (msg instanceof PlayActionMessage) {
            PlayActionMessage pam = (PlayActionMessage) msg;
            // toutes les actions (y compris les miennes) arrivent via le serveur en multi
            game.handleAction(pam.getPlayerID(), pam.getActionType());
        }
    }

    private void loadImages() {
        wallImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/walls/block_07.png")));
        floorImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/ground/ground_06.png")));
        brickImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/walls/block_08.png")));

        player1Img = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/characters/0/D_0.png")));
        botImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/output/characters/1/D_0.png")));

        for (int i = 0; i < BOMB_IDLE_FRAMES; i++) {
            bombIdleFrames[i] = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/sprites/output/bomb/B2_" + i + ".png")));
        }

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
        LaunchGameMessage config = NetworkManager.getInstance().getPendingLaunch();

        CellType[][] grid;
        List<Player> players = new ArrayList<>();
        int botCount;
        String myNickname = NetworkManager.getInstance().getNickname();

        // positions de spawn pour 4 joueurs max : coins du labyrinthe
        int[][] allPos = {{1, 1}, {13, 9}, {13, 1}, {1, 9}};
        int nextPosIdx = 0;

        if (config != null && config.getPlayers() != null && !config.getPlayers().isEmpty() && config.getGrid() != null) {
            // mode multi : on utilise la config envoyée par le serveur
            isOnline = true;
            grid = config.getGrid();
            botCount = config.getBotCount();

            List<ClientInfoDTO> roomPlayers = config.getPlayers();
            for (int i = 0; i < roomPlayers.size(); i++) {
                int pid = i + 1;
                int[] pos = allPos[nextPosIdx++];
                players.add(new Player(pid, pos[0], pos[1], 3, 1.0, 1));
                // on trouve notre propre ID par le pseudo
                if (myNickname != null && myNickname.equals(roomPlayers.get(i).getPseudo()))
                    myPlayerId = pid;
            }
        } else {
            // mode solo offline (fallback)
            isOnline = false;
            grid = MazeFactory.createMaze(MazeFactory.Algorithm.EXHAUSTIVE, MAZE_WIDTH, MAZE_HEIGHT);
            botCount = 1;
            int[] soloPos = allPos[nextPosIdx++];
            players.add(new Player(1, soloPos[0], soloPos[1], 3, 1.0, 1));
            myPlayerId = 1;
        }

        game = new Game(grid, players);

        // ajout des bots après les vrais joueurs
        int botStartId = players.size() + 1;
        for (int i = 0; i < botCount && nextPosIdx < allPos.length; i++) {
            int bid = botStartId + i;
            int[] pos = allPos[nextPosIdx++];
            AIPlayer bot = AIFactory.create(Strategy.SURVIVALIST, bid, pos[0], pos[1], 3, 1.0, 1);
            game.addBot(bot);
        }

        game.addListener(new GameStateListener() {
            @Override
            public void onGameStateUpdate(GameSnapshot snap) {
                Platform.runLater(() -> drawMap(snap));
            }
            @Override
            public void onPlayerDied(int id) { System.out.println("Mort de l'entité : " + id); }
            @Override
            public void onGameOver(int winnerId) { System.out.println("Fin de partie ! Gagnant : " + winnerId); }
        });

        game.start();
    }

    private void handleKeyPress(KeyEvent event) {
        if (game == null) return;

        ActionType action = switch (event.getCode()) {
            case Z, UP -> ActionType.MOVE_UP;
            case S, DOWN -> ActionType.MOVE_DOWN;
            case Q, LEFT -> ActionType.MOVE_LEFT;
            case D, RIGHT -> ActionType.MOVE_RIGHT;
            case SPACE -> ActionType.PLACE_BOMB;
            default -> null;
        };
        if (action == null) return;

        if (isOnline) {
            // envoi au serveur qui broadcast à tous -> onMessageReceived applique l'action
            NetworkManager.getInstance().sendMessage(new PlayActionMessage(myPlayerId, action));
        } else {
            game.handleAction(myPlayerId, action);
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
        Set<Integer> staleBombIds = new HashSet<>(bombViews.keySet());
        staleBombIds.removeAll(liveBombIds);
        for (int id : staleBombIds) {
            gameGrid.getChildren().remove(bombViews.remove(id));
            bombFirstSeen.remove(id);
        }

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
        Set<String> staleExplosionKeys = new HashSet<>(explosionViews.keySet());
        staleExplosionKeys.removeAll(liveExplosionKeys);
        for (String key : staleExplosionKeys)
            gameGrid.getChildren().remove(explosionViews.remove(key));

        for (GameSnapshot.PlayerState p : snap.getPlayers()) {
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