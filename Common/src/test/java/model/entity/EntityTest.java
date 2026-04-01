package model.entity;

import model.maze.CellType;
import model.maze.MazeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EntityTest {

    private static final int MAZE_WIDTH = 11;
    private static final int MAZE_HEIGHT = 11;

    private CellType[][] grid;
    private Player player;

    @BeforeEach
    void setUp() {
        grid = MazeFactory.createMaze(MazeFactory.Algorithm.EXHAUSTIVE, MAZE_WIDTH, MAZE_HEIGHT);
        // (1,1) est toujours EMPTY dans un labyrinthe parfait (coin de départ)
        player = new Player(1, 1, 1, 3, 1.0, 3);
    }

    // ─── Entity ───────────────────────────────────────────────────────────────

    @Test
    void testEntityGetters() {
        assertEquals(1, player.getId());
        assertEquals(1, player.getX());
        assertEquals(1, player.getY());
        assertFalse(player.isSolid());
    }

    // ─── Player : état initial ────────────────────────────────────────────────

    @Test
    void testPlayerInitialState() {
        assertEquals(3, player.getHp());
        assertEquals(3, player.getCurrentBombs());
        assertEquals(3, player.getMaxBombs());
        assertFalse(player.isDead());
    }

    @Test
    void testPlayerCurrentBombsEqualsMaxBombsOnCreation() {
        Player p = new Player(2, 1, 1, 5, 1.5, 2);
        assertEquals(p.getMaxBombs(), p.getCurrentBombs());
    }

    // ─── Player : déplacements ────────────────────────────────────────────────

    @Test
    void testValidMovement() {
        int startX = player.getX();
        int startY = player.getY();
        // On cherche une direction libre depuis (1,1)
        // DOWN : (1,2) — dans un labyrinthe exhaustif (1,2) peut être EMPTY ou non
        // On teste les 4 et vérifie la cohérence position/grille
        Direction[] dirs = Direction.values();
        for (Direction dir : dirs) {
            int expectedX = startX + (dir == Direction.RIGHT ? 1 : dir == Direction.LEFT ? -1 : 0);
            int expectedY = startY + (dir == Direction.DOWN ? 1 : dir == Direction.UP ? -1 : 0);
            player.deplacement(grid, dir);
            if (expectedY >= 0 && expectedY < grid.length
                    && expectedX >= 0 && expectedX < grid[0].length
                    && grid[expectedY][expectedX] == CellType.EMPTY) {
                assertEquals(expectedX, player.getX());
                assertEquals(expectedY, player.getY());
            } else {
                assertEquals(startX, player.getX());
                assertEquals(startY, player.getY());
            }
            // reset position
            player = new Player(1, startX, startY, 3, 1.0, 3);
        }
    }

    @Test
    void testMovementBlockedByWall() {
        // (0,1) est toujours un WALL (bordure du labyrinthe)
        player.deplacement(grid, Direction.LEFT);
        assertEquals(1, player.getX());
        assertEquals(1, player.getY());
    }

    @Test
    void testMovementOutOfBounds() {
        Player corner = new Player(2, 0, 0, 3, 1.0, 1);
        corner.deplacement(grid, Direction.UP);
        assertEquals(0, corner.getX());
        assertEquals(0, corner.getY());
        corner.deplacement(grid, Direction.LEFT);
        assertEquals(0, corner.getX());
        assertEquals(0, corner.getY());
    }

    // ─── Player : vie ─────────────────────────────────────────────────────────

    @Test
    void testTakeDamage() {
        player.takeDamage(1);
        assertEquals(2, player.getHp());
        assertFalse(player.isDead());
    }

    @Test
    void testTakeDamageClampedToZero() {
        player.takeDamage(100);
        assertEquals(0, player.getHp());
        assertTrue(player.isDead());
    }

    @Test
    void testDeathExactlyAtZero() {
        player.takeDamage(3);
        assertEquals(0, player.getHp());
        assertTrue(player.isDead());
    }

    // ─── Player : bombes ──────────────────────────────────────────────────────

    @Test
    void testCanPlaceBombWhenStockAvailable() {
        assertTrue(player.canPlaceBomb());
    }

    @Test
    void testCannotPlaceBombWhenStockEmpty() {
        player.onBombPlaced();
        player.onBombPlaced();
        player.onBombPlaced();
        assertFalse(player.canPlaceBomb());
    }

    @Test
    void testOnBombPlacedDecrementsStock() {
        player.onBombPlaced();
        assertEquals(2, player.getCurrentBombs());
    }

    @Test
    void testOnBombPlacedDoesNotGoBelowZero() {
        player.onBombPlaced();
        player.onBombPlaced();
        player.onBombPlaced();
        player.onBombPlaced(); // appel en trop
        assertEquals(0, player.getCurrentBombs());
    }

    // ─── Bomb : état initial ──────────────────────────────────────────────────

    @Test
    void testBombInitialState() {
        Bomb bomb = new Bomb(100, 1, 1, player.getId(), 2, 3000);
        assertEquals(100, bomb.getId());
        assertEquals(1, bomb.getX());
        assertEquals(1, bomb.getY());
        assertEquals(player.getId(), bomb.getOwnerID());
        assertEquals(2, bomb.getRadius());
        assertFalse(bomb.isExploded());
        assertTrue(bomb.isSolid());
    }

    // ─── Bomb : explosion timer ───────────────────────────────────────────────

    @Test
    void testBombExplodesAfterDelay() throws InterruptedException {
        Bomb bomb = new Bomb(100, 1, 1, player.getId(), 2, 500);
        assertFalse(bomb.isExploded());
        Thread.sleep(600);
        bomb.update();
        assertTrue(bomb.isExploded());
    }

    @Test
    void testBombDoesNotExplodeBeforeDelay() throws InterruptedException {
        Bomb bomb = new Bomb(100, 1, 1, player.getId(), 2, 1000);
        Thread.sleep(200);
        bomb.update();
        assertFalse(bomb.isExploded());
    }

    @Test
    void testBombUpdateIdempotentAfterExplosion() throws InterruptedException {
        Bomb bomb = new Bomb(100, 1, 1, player.getId(), 2, 300);
        Thread.sleep(400);
        bomb.update();
        bomb.update(); // second appel ne doit pas crasher
        assertTrue(bomb.isExploded());
    }

    // ─── Bomb : zone d'explosion ──────────────────────────────────────────────

    @Test
    void testExplosionAreaContainsBombCell() {
        Bomb bomb = new Bomb(100, 1, 1, player.getId(), 2, 3000);
        List<int[]> area = bomb.getExplosionArea(grid);
        assertTrue(containsCell(area, 1, 1));
    }

    @Test
    void testExplosionAreaBlockedByWall() {
        // La bombe est en (1,1), (0,1) est un WALL → ne doit pas être dans la zone
        Bomb bomb = new Bomb(100, 1, 1, player.getId(), 3, 3000);
        List<int[]> area = bomb.getExplosionArea(grid);
        assertFalse(containsCell(area, 0, 1));
    }

    @Test
    void testExplosionAreaDoesNotExceedRadius() {
        Bomb bomb = new Bomb(100, 1, 1, player.getId(), 1, 3000);
        List<int[]> area = bomb.getExplosionArea(grid);
        for (int[] cell : area) {
            int dist = Math.abs(cell[0] - 1) + Math.abs(cell[1] - 1);
            assertTrue(dist <= 1, "Cellule hors rayon : (" + cell[0] + "," + cell[1] + ")");
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private boolean containsCell(List<int[]> area, int x, int y) {
        for (int[] cell : area) {
            if (cell[0] == x && cell[1] == y) {
                return true;
            }
        }
        return false;
    }

}
