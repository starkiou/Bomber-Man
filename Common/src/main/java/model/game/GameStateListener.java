package model.game;

/**
 * Observer interface for game state changes.
 *
 * The ServerSide registers an implementation to convert snapshots into
 * {@code GameStateMessage} and broadcast them to connected clients.
 * The ClientSide (offline mode) registers an implementation to drive the JavaFX rendering.
 */
public interface GameStateListener {

    /**
     * Called every game tick (60 Hz) with a fresh immutable snapshot of the world.
     *
     * @param snapshot current state of players, bombs, and grid
     */
    void onGameStateUpdate(GameSnapshot snapshot);

    /**
     * Called immediately when a player's HP reaches zero.
     *
     * @param playerId the ID of the eliminated player
     */
    void onPlayerDied(int playerId);

    /**
     * Called when the game is over (one survivor or draw).
     *
     * @param winnerId the ID of the winning player, or {@code -1} for a draw
     */
    void onGameOver(int winnerId);
}