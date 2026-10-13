package model.aiPlayer;

import model.entity.Bomb;
import model.entity.Direction;
import model.entity.Player;
import model.maze.CellType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AIPlayerTest {

    /** Construit une grille à partir de lignes de texte : '#' = mur, 'B' = brique, '.' = vide. */
    private static CellType[][] grid(String... rows) {
        CellType[][] g = new CellType[rows.length][rows[0].length()];
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                g[y][x] = switch (rows[y].charAt(x)) {
                    case '#' -> CellType.WALL;
                    case 'B' -> CellType.BRICK;
                    default  -> CellType.EMPTY;
                };
            }
        }
        return g;
    }

    /** Bot minimal : se dirige vers une cible fixe avec le BFS commun. */
    private static class ProbeAI extends AIPlayer {
        private final int targetX, targetY;

        ProbeAI(int x, int y, int targetX, int targetY) {
            super(1, x, y, 1, 1.0, 1);
            this.targetX = targetX;
            this.targetY = targetY;
        }

        @Override
        protected AIAction decide(CellType[][] grid, List<Player> players, List<Bomb> bombs) {
            return new AIAction(bfsToward(getX(), getY(), targetX, targetY, grid), false);
        }
    }

    private static final String[] RING = {
            "######",
            "#....#",
            "#.##.#",
            "#....#",
            "######"
    };

    // ─── BFS commun ───────────────────────────────────────────────────────────

    @Test
    void testBfsTakesShortestPath() {
        ProbeAI bot = new ProbeAI(1, 1, 4, 1);
        assertEquals(Direction.RIGHT, bot.computeAction(grid(RING), List.of(bot), List.of()).move());
    }

    @Test
    void testBfsGoesAroundBomb() {
        ProbeAI bot = new ProbeAI(1, 1, 4, 1);
        Bomb bomb = new Bomb(1000, 2, 1, 2, 2, 3000);
        // La bombe bloque le couloir du haut : le seul chemin passe par le bas
        assertEquals(Direction.DOWN, bot.computeAction(grid(RING), List.of(bot), List.of(bomb)).move());
    }

    @Test
    void testBfsGoesAroundOtherPlayer() {
        ProbeAI bot = new ProbeAI(1, 1, 4, 1);
        Player other = new Player(2, 2, 1, 1, 1.0, 1);
        // Un bot ne se superpose pas à un autre joueur croisé en chemin
        assertEquals(Direction.DOWN, bot.computeAction(grid(RING), List.of(bot, other), List.of()).move());
    }

    @Test
    void testBfsStillReachesOccupiedTarget() {
        ProbeAI bot = new ProbeAI(1, 1, 3, 1);
        Player target = new Player(2, 3, 1, 1, 1.0, 1);
        assertEquals(Direction.RIGHT, bot.computeAction(grid(RING), List.of(bot, target), List.of()).move());
    }

    @Test
    void testBfsDoesNotStepOnAdjacentTarget() {
        ProbeAI bot = new ProbeAI(1, 1, 2, 1);
        assertNull(bot.computeAction(grid(RING), List.of(bot), List.of()).move());
    }

    @Test
    void testBfsReturnsNullWhenTargetUnreachable() {
        ProbeAI bot = new ProbeAI(1, 1, 3, 1);
        CellType[][] g = grid(
                "#####",
                "#.#.#",
                "#####");
        assertNull(bot.computeAction(g, List.of(bot), List.of()).move());
    }

    // ─── Bots superposés ──────────────────────────────────────────────────────

    private static final String[] OPEN = {
            "#######",
            "#.....#",
            "#.....#",
            "#.....#",
            "#######"
    };

    @Test
    void testStackedTwinBotsSeparate() {
        // Deux bots tactiques sur la même case prendraient indéfiniment les mêmes décisions
        AIPlayer first = AIFactory.create(Strategy.TACTICAL, 1, 3, 2, 1, 1.0, 1);
        AIPlayer twin = AIFactory.create(Strategy.TACTICAL, 2, 3, 2, 1, 1.0, 1);
        Player far = new Player(3, 1, 1, 1, 1.0, 1);
        List<Player> players = List.of(first, twin, far);

        AIPlayer.AIAction firstAction = first.computeAction(grid(OPEN), players, List.of());
        AIPlayer.AIAction twinAction = twin.computeAction(grid(OPEN), players, List.of());

        // Celui qui a le plus grand identifiant s'écarte, dans une autre direction que son jumeau
        assertNotNull(twinAction.move());
        assertNotEquals(firstAction.move(), twinAction.move());
        assertFalse(twinAction.placeBomb());
    }

    @Test
    void testBotAloneOnItsCellIsNotDisturbed() {
        AIPlayer bot = AIFactory.create(Strategy.SURVIVALIST, 2, 5, 3, 1, 1.0, 1);
        Player a = new Player(1, 1, 1, 1, 1.0, 1);
        Player b = new Player(3, 1, 2, 1, 1.0, 1);
        // Survivaliste, deux adversaires loin de lui : il ne bouge pas
        assertNull(bot.computeAction(grid(OPEN), List.of(a, bot, b), List.of()).move());
    }

    @Test
    void testStackedBotsStillFleeFirst() {
        AIPlayer first = AIFactory.create(Strategy.SURVIVALIST, 1, 2, 1, 1, 1.0, 1);
        AIPlayer twin = AIFactory.create(Strategy.SURVIVALIST, 2, 2, 1, 1, 1.0, 1);
        Bomb bomb = new Bomb(1000, 1, 1, 3, 2, 3000);
        // Sur la ligne du haut, dans le souffle : la fuite passe avant la séparation
        AIPlayer.AIAction action = twin.computeAction(grid(OPEN), List.of(first, twin), List.of(bomb));
        assertEquals(Direction.DOWN, action.move());
    }

    @Test
    void testFleeingBotPrefersAnEmptyShelter() {
        // Deux abris à égale distance : (2,2) est occupé, le bot choisit l'autre
        AIPlayer bot = AIFactory.create(Strategy.SURVIVALIST, 1, 2, 1, 1, 1.0, 1);
        Player onShelter = new Player(2, 2, 2, 1, 1.0, 1);
        Bomb bomb = new Bomb(1000, 1, 1, 3, 2, 3000);
        CellType[][] g = grid(
                "######",
                "#....#",
                "##.#.#",
                "######");
        // Souffle : (1,1) (2,1) (3,1). Abris : (2,2) à 1 pas mais occupé, (4,1) à 2 pas et libre
        assertEquals(Direction.RIGHT, bot.computeAction(g, List.of(bot, onShelter), List.of(bomb)).move());
    }

    // ─── Stratégies ───────────────────────────────────────────────────────────

    @Test
    void testAggressiveFleesItsOwnBomb() {
        AIPlayer bot = AIFactory.create(Strategy.AGGRESSIVE, 1, 1, 1, 1, 1.0, 1);
        Bomb ownBomb = new Bomb(1000, 1, 1, bot.getId(), 2, 3000);
        CellType[][] g = grid(
                "#####",
                "#...#",
                "#...#",
                "#...#",
                "#####");

        AIPlayer.AIAction action = bot.computeAction(g, List.of(bot), List.of(ownBomb));

        assertNotNull(action.move(), "le bot doit quitter la zone de sa propre bombe");
        assertFalse(action.placeBomb());
    }

    @Test
    void testAggressiveDoesNotWalkOntoEnemy() {
        AIPlayer bot = AIFactory.create(Strategy.AGGRESSIVE, 1, 1, 1, 1, 1.0, 1);
        bot.onBombPlaced(); // plus de bombe en stock : il ne peut que se déplacer
        Player enemy = new Player(2, 2, 1, 1, 1.0, 1);

        AIPlayer.AIAction action = bot.computeAction(grid(RING), List.of(bot, enemy), List.of());

        assertNull(action.move());
        assertFalse(action.placeBomb());
    }

    @Test
    void testSurvivalistFleesBlastZone() {
        AIPlayer bot = AIFactory.create(Strategy.SURVIVALIST, 1, 2, 1, 1, 1.0, 1);
        Bomb bomb = new Bomb(1000, 1, 1, 2, 2, 3000);
        CellType[][] g = grid(
                "#####",
                "#...#",
                "#...#",
                "#...#",
                "#####");

        // (2,1) est dans le souffle ; la case sûre la plus proche est (2,2)
        assertEquals(Direction.DOWN, bot.computeAction(g, List.of(bot), List.of(bomb)).move());
    }

    @Test
    void testTacticalBombsAdjacentBrickOnlyWithEscapeRoute() {
        // Cul-de-sac : poser une bombe ici ne laisse aucune case sûre
        AIPlayer trapped = AIFactory.create(Strategy.TACTICAL, 1, 1, 1, 1, 1.0, 1);
        Player far = new Player(2, 5, 1, 1, 1.0, 1);
        CellType[][] deadEnd = grid(
                "#######",
                "#.B#..#",
                "#######");
        assertFalse(trapped.computeAction(deadEnd, List.of(trapped, far), List.of()).placeBomb());

        // Avec une échappatoire hors de l'axe de la bombe, il la pose
        AIPlayer free = AIFactory.create(Strategy.TACTICAL, 1, 1, 1, 1, 1.0, 1);
        Player farAway = new Player(2, 5, 3, 1, 1.0, 1);
        CellType[][] open = grid(
                "#######",
                "#.B#..#",
                "#.##..#",
                "#.....#",
                "#######");
        assertTrue(free.computeAction(open, List.of(free, farAway), List.of()).placeBomb());
    }
}
