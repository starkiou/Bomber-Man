package model.game;

import model.entity.Bomb;
import model.entity.Player;
import model.maze.CellType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of the game state captured at a single tick.
 *
 * The ServerSide converts this into {@code GameStateMessage} / DTOs for network broadcast.
 * The ClientSide (offline) reads it directly for rendering.
 * Immutability guarantees that a listener can safely inspect a snapshot
 * while the game loop continues updating on its own thread.
 */
public final class GameSnapshot {

    // ── Inner records ─────────────────────────────────────────────────

    /** Lightweight representation of a player's state at snapshot time. */
    public record PlayerState(
            int id,
            int x,
            int y,
            boolean isDead,
            int hp,
            int currentBombs
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

    // ── Fields ────────────────────────────────────────────────────────

    private final List<PlayerState> players;
    private final List<BombState> bombs;
    private final CellType[][] grid;       // deep copy — safe to share
    private final long timestamp;
    private final boolean gameOver;
    private final int winnerId;   // -1 if no winner yet / draw

    // ── Private constructor — use capture() ───────────────────────────

    private GameSnapshot(
            List<PlayerState> players,
            List<BombState> bombs,
            CellType[][] grid,
            long timestamp,
            boolean gameOver,
            int winnerId
    ) {
        this.players = Collections.unmodifiableList(players);
        this.bombs = Collections.unmodifiableList(bombs);
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
            CellType[][] grid,
            boolean gameOver,
            int winnerId
    ) {
        List<PlayerState> playerStates = new ArrayList<>(players.size());
        for (Player p : players) {
            playerStates.add(new PlayerState(
                    p.getId(), p.getX(), p.getY(),
                    p.isDead(), p.getHp(), p.getCurrentBombs()
            ));
        }

        List<BombState> bombStates = new ArrayList<>(bombs.size());
        long now = System.currentTimeMillis();
        for (Bomb b : bombs) {
            bombStates.add(new BombState(
                    b.getId(), b.getX(), b.getY(),
                    b.getRadius(), b.getOwnerID(),
                    b.getTimeRemainingMs(now)
            ));
        }

        // Deep-copy the grid so the snapshot is truly immutable
        CellType[][] gridCopy = new CellType[grid.length][];
        for (int i = 0; i < grid.length; i++) {
            gridCopy[i] = Arrays.copyOf(grid[i], grid[i].length);
        }

        return new GameSnapshot(playerStates, bombStates, gridCopy, now, gameOver, winnerId);
    }

    // ── Getters ───────────────────────────────────────────────────────

    public List<PlayerState> getPlayers() { return players; }
    public List<BombState> getBombs() { return bombs; }
    /** Returns a defensive copy of the grid to prevent external mutation. */
    public CellType[][] getGrid()      {
        CellType[][] copy = new CellType[grid.length][];
        for (int i = 0; i < grid.length; i++) copy[i] = Arrays.copyOf(grid[i], grid[i].length);
        return copy;
    }
    public long getTimestamp() { return timestamp; }
    public boolean isGameOver()  { return gameOver; }
    public int getWinnerId() { return winnerId; }
}