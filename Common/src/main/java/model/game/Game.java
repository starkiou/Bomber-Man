package model.game;

import model.aiPlayer.AIPlayer;
import model.entity.Bomb;
import model.entity.Direction;
import model.entity.Player;
import model.logger.LogManager;
import model.maze.CellType;
import network.message.ActionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core game engine — shared by ServerSide (multi-player) and ClientSide (offline/solo).
 *
 * <p><b>Thread-safety contract:</b>
 * <ul>
 *   <li>{@link #handleAction} is the only public entry-point for external threads (network,
 *       UI event thread). It acquires {@code stateLock} before mutating any game state.</li>
 *   <li>The game loop runs on its own dedicated thread and acquires the same lock only
 *       during compound state reads (snapshot capture) or writes (explosion processing).</li>
 *   <li>{@code activeBombs} is a synchronized list; every iteration is wrapped in a
 *       {@code synchronized(activeBombs)} block as required by the Java spec.</li>
 *   <li>{@code players} is a {@link ConcurrentHashMap} — safe for concurrent per-key
 *       get/put, but compound operations still use {@code stateLock}.</li>
 * </ul>
 *
 * <p><b>Observer integration (ServerSide):</b>
 * <pre>{@code
 *   game.addListener(new GameStateListener() {
 *       public void onGameStateUpdate(GameSnapshot snap) {
 *           // build GameStateMessage from snap, broadcast to all ClientHandlers
 *       }
 *       public void onPlayerDied(int id)  { ... }
 *       public void onGameOver(int winner) { ... }
 *   });
 *   game.start();
 * }</pre>
 */
public class Game implements Runnable {

    // ── Constants ────────────────────────────────────────────────────
    public static final int  TARGET_FPS = 60;
    public static final long FRAME_TIME_MS = 1000L / TARGET_FPS; // ≈ 16 ms

    private static final int  DEFAULT_BOMB_RADIUS = 2;
    private static final int  DEFAULT_BOMB_DELAY = 3000; // ms before explosion

    // ── State ────────────────────────────────────────────────────────
    /** The maze grid, indexed grid[y][x]. Mutated during explosion (BRICK → EMPTY). */
    private final CellType[][] grid;

    /** All players (alive and dead) keyed by their ID. */
    private final ConcurrentHashMap<Integer, Player> players;

    /** Bombs currently on the field. Use synchronized(activeBombs) when iterating. */
    private final List<Bomb> activeBombs;

    /**
     * Master lock for compound state mutations:
     * movement, bomb placement, explosion processing, victory check.
     * Any thread touching more than one field at once must hold this lock.
     */
    private final Object stateLock = new Object();

    /** Auto-incrementing ID counter for new bombs. */
    private final AtomicInteger bombIdSeq = new AtomicInteger(1000);

    private volatile boolean gameOver = false;
    private volatile int winnerId = -1; // -1 = no winner yet / draw

    // ── AI ──────────────────────────────────────────────────────────
    /** Active bots, keyed by player ID. AIPlayer extends Player so they also live in {@code players}. */
    private final Map<Integer, AIPlayer> bots = new ConcurrentHashMap<>();

    // ── Observer ────────────────────────────────────────────────────
    /**
     * Registered listeners notified every tick and on game events.
     * CopyOnWriteArrayList allows listeners to register/unregister safely
     * from any thread without blocking the game loop during iteration.
     */
    private final List<GameStateListener> listeners = new CopyOnWriteArrayList<>();

    // ── Game loop ────────────────────────────────────────────────────
    private volatile boolean running = false;
    private Thread gameThread;

    // ── Logger ───────────────────────────────────────────────────────
    private final LogManager log = LogManager.getInstance();

    // ─────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────

    /**
     * @param grid           maze grid to play on (will be deep-copied internally)
     * @param initialPlayers players joining the game (can be modified after construction
     *                       via {@link #addBot})
     */
    public Game(CellType[][] grid, List<Player> initialPlayers) {
        this.grid = deepCopyGrid(grid);
        this.players = new ConcurrentHashMap<>();
        this.activeBombs = Collections.synchronizedList(new ArrayList<>());

        for (Player p : initialPlayers) {
            players.put(p.getId(), p);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────

    /** Starts the game loop on a dedicated daemon thread. Idempotent. */
    public void start() {
        if (running) return;
        running = true;
        gameThread = new Thread(this, "GameLoop");
        gameThread.setDaemon(true);
        gameThread.start();
        log.info("Game started — " + players.size() + " player(s), " + TARGET_FPS + " FPS.");
    }

    /** Stops the game loop cleanly. Safe to call from any thread. */
    public void stop() {
        running = false;
        if (gameThread != null) {
            gameThread.interrupt();
        }
        log.info("Game stopped.");
    }

    // ─────────────────────────────────────────────────────────────────
    // Game loop (runs on gameThread)
    // ─────────────────────────────────────────────────────────────────

    @Override
    public void run() {
        log.info("Game loop thread started.");
        while (running && !gameOver) {
            long frameStart = System.currentTimeMillis();

            update();
            broadcastState(); // every tick = 60 Hz

            long elapsed = System.currentTimeMillis() - frameStart;
            long sleep = FRAME_TIME_MS - elapsed;
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.info("Game loop thread ended.");
    }

    // ─────────────────────────────────────────────────────────────────
    // Update pipeline (game thread only)
    // ─────────────────────────────────────────────────────────────────

    private void update() {
        updatePlayers();  // bomb regen
        updateBombs();    // countdown → explosion → damage
        updateBots();     // AI decision → handleAction
        checkVictory();   // last survivor wins
    }

    /** Ticks each living player (handles bomb regen timer internally). */
    private void updatePlayers() {
        for (Player p : players.values()) {
            if (!p.isDead()) p.update();
        }
    }

    /**
     * Ticks each bomb. When a bomb's timer fires, calls {@link #processExplosion(Bomb)}
     * then removes it from the active list.
     */
    private void updateBots() {
        List<Player> playerList = new ArrayList<>(players.values());
        List<Bomb>   bombList;
        synchronized (activeBombs) {
            bombList = new ArrayList<>(activeBombs);
        }

        for (AIPlayer bot : bots.values()) {
            if (bot.isDead()) continue;

            boolean moveReady = bot.canMove();
            boolean bombReady = bot.canBomb();
            if (!moveReady && !bombReady) continue;

            AIPlayer.AIAction action = bot.computeAction(grid, playerList, bombList);

            synchronized (stateLock) {
                if (action.move() != null && moveReady) {
                    processMovement(bot, action.move());
                    bot.onMoveDone();
                }
                if (action.placeBomb() && bombReady) {
                    placeBomb(bot);
                    bot.onBombDone();
                }
            }
        }
    }

    /**
     * Handles a single bomb explosion:
     * <ul>
     *   <li>Destroys BRICK cells within the blast area.</li>
     *   <li>Deals 1 HP damage to every living player caught in the blast.</li>
     *   <li>Fires {@link GameStateListener#onPlayerDied} for any newly eliminated player.</li>
     * </ul>
     */
    private void processExplosion(Bomb bomb) {
        synchronized (stateLock) {
            List<int[]> blast = bomb.getExplosionArea(grid);
            log.info("Bomb " + bomb.getId() + " exploded at ("
                    + bomb.getX() + "," + bomb.getY()
                    + ") — blast covers " + blast.size() + " cell(s).");

            for (int[] cell : blast) {
                int cx = cell[0];
                int cy = cell[1];

                // Destroy destructible walls
                if (isInBounds(cx, cy) && grid[cy][cx] == CellType.BRICK) {
                    grid[cy][cx] = CellType.EMPTY;
                    log.info("Brick destroyed at (" + cx + "," + cy + ").");
                }

                // Damage players standing in the blast
                for (Player p : players.values()) {
                    if (!p.isDead() && p.getX() == cx && p.getY() == cy) {
                        p.takeDamage(1);
                        log.warning("Player " + p.getId()
                                + " hit — HP remaining: " + p.getHp());
                        if (p.isDead()) {
                            log.warning("Player " + p.getId() + " eliminated!");
                            notifyPlayerDied(p.getId());
                        }
                    }
                }
            }
        }
    }

    /**
     * Ticks each registered bot: calls {@code AIPlayer.computeAction()} then applies
     * the resulting move and/or bomb placement through the normal game-logic methods.
     *
     * <p>Defensive copies of the grid and player list are passed to the AI so that
     * its pathfinding cannot mutate live game state.
     */
    private void updateBots() {
        List<Player> playerSnapshot;
        synchronized (stateLock) {
            playerSnapshot = new ArrayList<>(players.values());
        }

        List<Bomb> bombSnapshot;
        synchronized (activeBombs) {
            bombSnapshot = new ArrayList<>(activeBombs);
        }

        for (AIPlayer bot : bots.values()) {
            if (bot.isDead()) continue;

            AIPlayer.AIAction action = bot.computeAction(
                    deepCopyGrid(grid),
                    playerSnapshot,
                    bombSnapshot
            );

            synchronized (stateLock) {
                if (action.move() != null) {
                    processMovement(bot, action.move());
                }
                if (action.placeBomb()) {
                    placeBomb(bot);
                }
            }
        }
    }

    /** Checks for a single survivor or full wipe and notifies listeners. */
    private void checkVictory() {
        if (gameOver) return;

        List<Player> alive = players.values().stream()
                .filter(p -> !p.isDead())
                .toList();

        if (alive.size() <= 1) {
            gameOver = true;
            winnerId = alive.isEmpty() ? -1 : alive.get(0).getId();
            String result = (winnerId == -1)
                    ? "Draw — all players eliminated."
                    : "Player " + winnerId + " wins!";
            log.info("Game over. " + result);
            notifyGameOver(winnerId);
            stop();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Public input handler — safe to call from any thread
    // ─────────────────────────────────────────────────────────────────

    /**
     * Processes a player action. Designed to be called from network threads
     * (e.g., ServerSide's {@code ClientHandler}) or from the JavaFX event thread
     * in offline mode.
     *
     * <p>Acquires {@code stateLock} to ensure atomicity of the resulting state change.
     *
     * @param playerId the acting player's ID
     * @param action   the action to perform
     */
    public void handleAction(int playerId, ActionType action) {
        if (gameOver) return;
        Player player = players.get(playerId);
        if (player == null || player.isDead()) return;

        synchronized (stateLock) {
            switch (action) {
                case MOVE_UP -> processMovement(player, Direction.UP);
                case MOVE_DOWN -> processMovement(player, Direction.DOWN);
                case MOVE_LEFT -> processMovement(player, Direction.LEFT);
                case MOVE_RIGHT -> processMovement(player, Direction.RIGHT);
                case PLACE_BOMB -> placeBomb(player);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Business logic (must be called while holding stateLock)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Moves a player one cell in the given direction if the target cell is passable
     * (not a WALL, not a BRICK, and not occupied by an active bomb).
     */
    private void processMovement(Player player, Direction dir) {
        int nextX = player.getX();
        int nextY = player.getY();
        switch (dir) {
            case UP -> nextY--;
            case DOWN -> nextY++;
            case LEFT -> nextX--;
            case RIGHT -> nextX++;
        }

        if (!isInBounds(nextX, nextY)) return;

        CellType cell = grid[nextY][nextX];
        if (cell == CellType.WALL || cell == CellType.BRICK) return;
        if (isBombAt(nextX, nextY)) return; // bombs block movement

        log.info("Player " + player.getId() + " moved " + dir + ": (" + player.getX() + "," + player.getY() + ") → (" + nextX + "," + nextY + ").");

        player.setX(nextX);
        player.setY(nextY);
    }

    /**
     * Places a bomb at the player's current position if they have remaining stock
     * and no other bomb is already there.
     */
    private void placeBomb(Player player) {
        if (!player.canPlaceBomb()) {
            log.info("Player " + player.getId() + " has no bombs left to place.");
            return;
        }
        int bx = player.getX();
        int by = player.getY();
        if (isBombAt(bx, by)) return; // prevent stacking

        int  bombId = bombIdSeq.getAndIncrement();
        Bomb bomb = new Bomb(bombId, bx, by, player.getId(), DEFAULT_BOMB_RADIUS, DEFAULT_BOMB_DELAY);

        synchronized (activeBombs) {
            activeBombs.add(bomb);
        }
        player.onBombPlaced();

        log.info("Player " + player.getId() + " placed bomb " + bombId + " at (" + bx + "," + by + ") — " + player.getCurrentBombs() + "/" + player.getMaxBombs() + " bomb(s) left.");
    }

    // ─────────────────────────────────────────────────────────────────
    // Observer
    // ─────────────────────────────────────────────────────────────────

    public void addListener(GameStateListener listener) {
        listeners.add(listener);
    }

    public void removeListener(GameStateListener listener) {
        listeners.remove(listener);
    }

    /** Broadcasts the current snapshot to every registered listener. */
    private void broadcastState() {
        if (listeners.isEmpty()) return;
        GameSnapshot snapshot = getSnapshot();
        for (GameStateListener l : listeners) {
            l.onGameStateUpdate(snapshot);
        }
    }

    private void notifyPlayerDied(int playerId) {
        for (GameStateListener l : listeners) l.onPlayerDied(playerId);
    }

    private void notifyGameOver(int winner) {
        for (GameStateListener l : listeners) l.onGameOver(winner);
    }

    // ─────────────────────────────────────────────────────────────────
    // Snapshot (usable from any thread)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Returns an immutable snapshot of the current game state.
     * Safe to call from any thread — acquires {@code stateLock} briefly.
     */
    public GameSnapshot getSnapshot() {
        synchronized (stateLock) {
            synchronized (activeBombs) {
                return GameSnapshot.capture(players.values(), activeBombs, grid, gameOver, winnerId);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // AI support
    // ─────────────────────────────────────────────────────────────────

    /**
     * Registers an AI-controlled player.
     * The bot is added to both the player map and the bot registry so that
     * {@link #updateBots()} ticks its decision logic every frame.
     *
     * <p>Instantiate bots via {@code AIFactory.create(...)} from the {@code model.aiPlayer} package.
     *
     * @param bot the AIPlayer instance to register (must not already be in the game)
     */
    public void addBot(AIPlayer bot) {
        players.put(bot.getId(), bot);
        bots.put(bot.getId(), bot);
        log.info("Bot " + bot.getId() + " registered (" + bot.getClass().getSimpleName() + ").");
    }

    // ─────────────────────────────────────────────────────────────────
    // Getters
    // ─────────────────────────────────────────────────────────────────

    public boolean isGameOver() { return gameOver; }
    public int getWinnerId() { return winnerId; }

    /** Returns a defensive copy of the grid. */
    public CellType[][] getGrid() { return deepCopyGrid(grid); }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────

    private boolean isInBounds(int x, int y) {
        return y >= 0 && y < grid.length && x >= 0 && x < grid[0].length;
    }

    /** Returns true if any active bomb occupies the given cell. Caller must NOT hold activeBombs lock. */
    private boolean isBombAt(int x, int y) {
        synchronized (activeBombs) {
            for (Bomb b : activeBombs) {
                if (b.getX() == x && b.getY() == y) return true;
            }
        }
        return false;
    }


    private static CellType[][] deepCopyGrid(CellType[][] src) {
        CellType[][] copy = new CellType[src.length][];
        for (int i = 0; i < src.length; i++) {
            copy[i] = java.util.Arrays.copyOf(src[i], src[i].length);
        }
        return copy;
    }
}