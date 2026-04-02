package model.game;

import model.aiPlayer.AIPlayer;
import model.entity.Bomb;
import model.entity.Direction;
import model.entity.Player;
import model.logger.LogManager;
import model.maze.CellType;
import network.message.ActionType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Game implements Runnable {
    public static final int TARGET_FPS = 60;
    public static final long FRAME_TIME_MS = 1000L / TARGET_FPS;
    private static final long EXPLOSION_ANIM_DURATION_MS = 700;

    private final CellType[][] grid;
    private final ConcurrentHashMap<Integer, Player> players;
    private final List<Bomb> activeBombs;
    private final List<long[]> activeExplosionCells;
    private final Object stateLock = new Object();
    private final AtomicInteger bombIdSeq = new AtomicInteger(1000);

    private volatile boolean gameOver = false;
    private volatile int winnerId = -1;
    private final Map<Integer, AIPlayer> bots = new ConcurrentHashMap<>();
    private final List<GameStateListener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean running = false;
    private Thread gameThread;

    // Gestion du temps
    private final long gameDurationMs;
    private long startTimeMs;
    private long remainingSeconds;

    public Game(CellType[][] grid, List<Player> initialPlayers, int durationSeconds) {
        this.grid = deepCopyGrid(grid);
        this.players = new ConcurrentHashMap<>();
        this.activeBombs = Collections.synchronizedList(new ArrayList<>());
        this.activeExplosionCells = Collections.synchronizedList(new ArrayList<>());
        this.gameDurationMs = durationSeconds * 1000L;
        this.remainingSeconds = durationSeconds;

        for (Player p : initialPlayers) {
            players.put(p.getId(), p);
        }
    }

    public void start() {
        if (running) return;
        running = true;
        gameThread = new Thread(this, "GameLoop");
        gameThread.setDaemon(true);
        gameThread.start();
    }

    @Override
    public void run() {
        this.startTimeMs = System.currentTimeMillis();
        while (running && !gameOver) {
            long frameStart = System.currentTimeMillis();
            update();
            broadcastState();
            long sleep = FRAME_TIME_MS - (System.currentTimeMillis() - frameStart);
            if (sleep > 0) {
                try { Thread.sleep(sleep); } catch (InterruptedException e) { break; }
            }
        }
    }

    private void update() {
        updateTime();
        updatePlayers();
        updateBombs();
        updateExplosions();
        updateBots();
        checkVictory();
    }

    private void updateTime() {
        long elapsed = System.currentTimeMillis() - startTimeMs;
        long remMs = Math.max(0, gameDurationMs - elapsed);
        this.remainingSeconds = remMs / 1000;
        if (remMs <= 0) gameOver = true;
    }

    private void updatePlayers() {
        for (Player p : players.values()) if (!p.isDead()) p.update();
    }

    private void updateExplosions() {
        long now = System.currentTimeMillis();
        synchronized (activeExplosionCells) {
            activeExplosionCells.removeIf(e -> (now - e[2]) >= EXPLOSION_ANIM_DURATION_MS);
        }
    }

    private void updateBombs() {
        List<Bomb> toExplode = new ArrayList<>();
        synchronized (activeBombs) {
            for (Bomb b : activeBombs) {
                b.update();
                if (b.isExploded()) toExplode.add(b);
            }
        }
        for (Bomb b : toExplode) processExplosion(b);
        synchronized (activeBombs) { activeBombs.removeIf(Bomb::isExploded); }
    }

    private void processExplosion(Bomb bomb) {
        synchronized (stateLock) {
            int bx = bomb.getX(), by = bomb.getY();
            long now = System.currentTimeMillis();
            int[][] dirs = {{0, -1}, {0, 1}, {1, 0}, {-1, 0}};

            synchronized (activeExplosionCells) {
                activeExplosionCells.add(new long[]{bx, by, now, 0});
                for (int d = 0; d < 4; d++) {
                    for (int i = 1; i <= bomb.getRadius(); i++) {
                        int nx = bx + dirs[d][0] * i, ny = by + dirs[d][1] * i;
                        if (!isInBounds(nx, ny) || grid[ny][nx] == CellType.WALL) break;
                        activeExplosionCells.add(new long[]{nx, ny, now, d + 3});
                        if (grid[ny][nx] == CellType.BRICK) { grid[ny][nx] = CellType.EMPTY; break; }
                    }
                }
            }
            // Simple damage
            for (Player p : players.values()) {
                if (!p.isDead()) {
                    synchronized (activeExplosionCells) {
                        for (long[] c : activeExplosionCells) {
                            if (c[2] == now && p.getX() == c[0] && p.getY() == c[1]) {
                                p.takeDamage(1);
                                if (p.isDead()) notifyPlayerDied(p.getId());
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateBots() {
        for (AIPlayer bot : bots.values()) {
            if (bot.isDead()) continue;
            AIPlayer.AIAction action = bot.computeAction(grid, new ArrayList<>(players.values()), new ArrayList<>(activeBombs));
            synchronized (stateLock) {
                if (action.move() != null && bot.canMove()) processMovement(bot, action.move());
                if (action.placeBomb() && bot.canBomb()) placeBomb(bot);
            }
        }
    }

    private void checkVictory() {
        if (gameOver) return;
        List<Player> alive = players.values().stream().filter(p -> !p.isDead()).toList();
        if (alive.size() <= 1) {
            gameOver = true;
            winnerId = alive.size() == 1 ? alive.get(0).getId() : -1;
            notifyGameOver(winnerId);
        }
    }

    public void handleAction(int playerId, ActionType action) {
        if (gameOver) return;
        Player p = players.get(playerId);
        if (p == null || p.isDead()) return;
        synchronized (stateLock) {
            switch (action) {
                case MOVE_UP -> processMovement(p, Direction.UP);
                case MOVE_DOWN -> processMovement(p, Direction.DOWN);
                case MOVE_LEFT -> processMovement(p, Direction.LEFT);
                case MOVE_RIGHT -> processMovement(p, Direction.RIGHT);
                case PLACE_BOMB -> placeBomb(p);
            }
        }
    }

    private void processMovement(Player p, Direction d) {
        int nx = p.getX(), ny = p.getY();
        switch (d) { case UP->ny--; case DOWN->ny++; case LEFT->nx--; case RIGHT->nx++; }
        if (isInBounds(nx, ny) && grid[ny][nx] == CellType.EMPTY && !isBombAt(nx, ny)) {
            p.setX(nx); p.setY(ny);
        }
    }

    private void placeBomb(Player p) {
        if (p.canPlaceBomb() && !isBombAt(p.getX(), p.getY())) {
            activeBombs.add(new Bomb(bombIdSeq.getAndIncrement(), p.getX(), p.getY(), p.getId(), 2, 3000));
            p.onBombPlaced();
        }
    }

    public GameSnapshot getSnapshot() {
        synchronized (stateLock) {
            return GameSnapshot.capture(players.values(), activeBombs, activeExplosionCells, grid, gameOver, winnerId, remainingSeconds);
        }
    }

    private boolean isBombAt(int x, int y) {
        synchronized (activeBombs) { return activeBombs.stream().anyMatch(b -> b.getX() == x && b.getY() == y); }
    }
    private boolean isInBounds(int x, int y) { return y >= 0 && y < grid.length && x >= 0 && x < grid[0].length; }
    private void broadcastState() {
        GameSnapshot snap = getSnapshot();
        for (GameStateListener l : listeners) l.onGameStateUpdate(snap);
    }
    public void addListener(GameStateListener l) { listeners.add(l); }
    private void notifyPlayerDied(int id) { for (GameStateListener l : listeners) l.onPlayerDied(id); }
    private void notifyGameOver(int id) { for (GameStateListener l : listeners) l.onGameOver(id); }
    public void addBot(AIPlayer bot) { players.put(bot.getId(), bot); bots.put(bot.getId(), bot); }
    private CellType[][] deepCopyGrid(CellType[][] s) {
        CellType[][] c = new CellType[s.length][];
        for (int i = 0; i < s.length; i++) c[i] = s[i].clone();
        return c;
    }
}