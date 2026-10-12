package model.game;

import model.entity.Player;
import model.maze.CellType;
import network.message.ActionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GameKillsTest {

    private static CellType[][] openGrid() {
        CellType[][] g = new CellType[5][5];
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                boolean border = x == 0 || y == 0 || x == 4 || y == 4;
                g[y][x] = border ? CellType.WALL : CellType.EMPTY;
            }
        }
        return g;
    }

    private static void waitForGameOver(Game game) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 6000;
        while (!game.isGameOver() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
    }

    @Test
    void testKillIsCreditedToBombOwner() throws InterruptedException {
        Player attacker = new Player(1, 1, 1, 1, 1.0, 1);
        Player victim   = new Player(2, 1, 2, 1, 1.0, 1);
        Game game = new Game(openGrid(), List.of(attacker, victim));

        // Le joueur 1 pose sa bombe en (1,1) puis sort de la croix de l'explosion
        game.handleAction(1, ActionType.PLACE_BOMB);
        game.handleAction(1, ActionType.MOVE_RIGHT);
        game.handleAction(1, ActionType.MOVE_DOWN);
        game.start();
        waitForGameOver(game);

        assertTrue(game.isGameOver());
        assertEquals(1, game.getWinnerId());
        assertEquals(1, game.getKills(1));
        assertEquals(0, game.getKills(2));
    }

    @Test
    void testSuicideIsNotAKill() throws InterruptedException {
        Player clumsy = new Player(1, 1, 1, 1, 1.0, 1);
        Player other  = new Player(2, 3, 3, 1, 1.0, 1);
        Game game = new Game(openGrid(), List.of(clumsy, other));

        game.handleAction(1, ActionType.PLACE_BOMB);
        game.start();
        waitForGameOver(game);

        assertEquals(2, game.getWinnerId());
        assertEquals(0, game.getKills(1));
        assertEquals(0, game.getKills(2));
    }
}
