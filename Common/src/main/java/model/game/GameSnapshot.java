package model.game;

import model.entity.Bomb;
import model.entity.Player;
import model.maze.CellType;

import java.util.*;

/**
 * Immutable snapshot of the game state captured at a single tick.
 *
 * The ServerSide converts this into {@code GameStateMessage} / DTOs for network broadcast.
 * The ClientSide (offline) reads it directly for rendering.
 * Immutability guarantees that a listener can safely inspect a snapshot
 * while the game loop continues updating on its own thread.
 */
public final class GameSnapshot {
    private final long remainingSeconds; // <--- AJOUT
    // ── Inner records ─────────────────────────────────────────────────

    /** Lightweight representation of a player's state at snapshot time. */
    public record PlayerState(
            int id,
            int x,
            int y,
            boolean isDead,
            int hp,
            int currentBombs,
            int maxBombs
    ) {}

    /** Lightweight representation of an active bomb at snapshot time. */
    public record BombState(
            int id,
            int x,
            int y,
            int radius,
            int ownerID,
            long timeRemainingMs
    ) {}

    /**
     * One blast cell currently animating on screen.
     * spriteType encodes which explosion_Z_F sprite to use:
     *   0 = centre, 1 = H-mid, 2 = V-mid,
     *   3 = top-end, 4 = bottom-end, 5 = right-end, 6 = left-end
     */
    public record ExplosionState(int x, int y, long ageMs, int spriteType) {}

    // ── Fields ────────────────────────────────────────────────────────

    private final List<PlayerState> players;
    private final List<BombState> bombs;
    private final List<ExplosionState> explosions;
    private final CellType[][] grid;       // deep copy — safe to share
    private final long timestamp;
    private final boolean gameOver;
    private final int winnerId;   // -1 if no winner yet / draw

    // ── Private constructor — use capture() ───────────────────────────

    private GameSnapshot(
            long remainingSeconds, List<PlayerState> players,
            List<BombState> bombs,
            List<ExplosionState> explosions,
            CellType[][] grid,
            long timestamp,
            boolean gameOver,
            int winnerId
    ) {
        this.remainingSeconds = remainingSeconds;
        this.players = Collections.unmodifiableList(players);
        this.bombs = Collections.unmodifiableList(bombs);
        this.explosions = Collections.unmodifiableList(explosions);
        this.grid = grid;
        this.timestamp = timestamp;
        this.gameOver = gameOver;
        this.winnerId = winnerId;
    }

    // ── Factory ───────────────────────────────────────────────────────

    /**
     * Captures the current game state into an immutable snapshot.
     * Must be called while holding {@code stateLock} in {@link Game}.
     *
     * @param players  live player map values
     * @param bombs    live bomb list (synchronized externally by caller)
     * @param grid     live grid (indexed grid[y][x])
     * @param gameOver whether the game has ended
     * @param winnerId winner ID, or -1
     * @return immutable snapshot
     */
    public static GameSnapshot capture(
            Collection<Player> players,
            List<Bomb> bombs,
            List<long[]> explosionCells,
            CellType[][] grid,
            boolean gameOver,
            int winnerId,
            long remainingSeconds
    ) {
        List<PlayerState> playerStates = new ArrayList<>(players.size());
        for (Player p : players) {
            playerStates.add(new PlayerState(
                    p.getId(), p.getX(), p.getY(),
                    p.isDead(), p.getHp(), p.getCurrentBombs(), p.getMaxBombs()
            ));
        }

        long now = System.currentTimeMillis();

        List<BombState> bombStates = new ArrayList<>(bombs.size());
        for (Bomb b : bombs) {
            bombStates.add(new BombState(
                    b.getId(), b.getX(), b.getY(),
                    b.getRadius(), b.getOwnerID(),
                    b.getTimeRemainingMs(now)
            ));
        }

        List<ExplosionState> explosionStates = new ArrayList<>(explosionCells.size());
        for (long[] cell : explosionCells) {
            explosionStates.add(new ExplosionState((int) cell[0], (int) cell[1], now - cell[2], (int) cell[3]));
        }

        // Deep-copy the grid so the snapshot is truly immutable
        CellType[][] gridCopy = new CellType[grid.length][];
        for (int i = 0; i < grid.length; i++) {
            gridCopy[i] = Arrays.copyOf(grid[i], grid[i].length);
        }

        return new GameSnapshot(remainingSeconds, playerStates, bombStates, explosionStates, gridCopy, now, gameOver, winnerId);
    }

    // ── Getters ───────────────────────────────────────────────────────

    public List<PlayerState> getPlayers() { return players; }
    public List<BombState> getBombs() { return bombs; }
    public List<ExplosionState> getExplosions() { return explosions; }
    /** Returns a defensive copy of the grid to prevent external mutation. */
    public CellType[][] getGrid()      {
        CellType[][] copy = new CellType[grid.length][];
        for (int i = 0; i < grid.length; i++) copy[i] = Arrays.copyOf(grid[i], grid[i].length);
        return copy;
    }
    public long getTimestamp() { return timestamp; }
    public boolean isGameOver()  { return gameOver; }
    public int getWinnerId() { return winnerId; }
    public long getRemainingSeconds() { return remainingSeconds; }
}