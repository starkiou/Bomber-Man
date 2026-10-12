package network.message;

import model.game.GameSnapshot;
import model.game.GameSnapshot.BombState;
import model.game.GameSnapshot.ExplosionState;
import model.game.GameSnapshot.PlayerState;
import model.maze.CellType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GameStateMessageTest {

    private static GameSnapshot sample() {
        CellType[][] grid = {
                {CellType.WALL, CellType.WALL, CellType.WALL},
                {CellType.WALL, CellType.EMPTY, CellType.BRICK},
                {CellType.WALL, CellType.WALL, CellType.WALL}
        };
        return GameSnapshot.of(
                List.of(new PlayerState(7, 1, 1, false, 3, 0, 1, 2), new PlayerState(8, 2, 1, true, 0, 1, 1, 0)),
                List.of(new BombState(1000, 1, 1, 2, 7, 1500)),
                List.of(new ExplosionState(2, 1, 250, 5)),
                grid, true, 7, 42);
    }

    /** Sérialise puis relit le message exactement comme le fait le réseau. */
    private static GameSnapshot roundTrip(GameSnapshot snapshot) {
        MessageSerializer serializer = new MessageSerializer();
        Message decoded = serializer.deserialize(serializer.serialize(new GameStateMessage(snapshot)));
        assertInstanceOf(GameStateMessage.class, decoded);
        return ((GameStateMessage) decoded).getSnapshot();
    }

    @Test
    void testSnapshotSurvivesTheNetwork() {
        GameSnapshot before = sample();
        GameSnapshot after = roundTrip(before);

        assertEquals(before.getPlayers(), after.getPlayers());
        assertEquals(before.getBombs(), after.getBombs());
        assertEquals(before.getExplosions(), after.getExplosions());
        assertArrayEquals(before.getGrid(), after.getGrid());
        assertTrue(after.isGameOver());
        assertEquals(7, after.getWinnerId());
        assertEquals(42, after.getRemainingSeconds());
    }

    @Test
    void testKillsAreTransmitted() {
        GameSnapshot after = roundTrip(sample());
        assertEquals(2, after.getPlayers().get(0).kills());
        assertEquals(0, after.getPlayers().get(1).kills());
    }
}
