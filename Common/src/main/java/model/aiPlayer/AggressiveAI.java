package model.aiPlayer;

import model.entity.Bomb;
import model.entity.Direction;
import model.entity.Player;
import model.maze.CellType;

import java.util.List;
import java.util.Set;

public class AggressiveAI extends AIPlayer{

    private static final int BOMB_RANGE_TRIGGER = 3;

    public AggressiveAI(int id, int x, int y, int hp, double speed, int maxBombs) {
        super(id, x, y, hp, speed, maxBombs);
    }

    @Override
    public AIAction computeAction(CellType[][] grid, List<Player> players, List<Bomb> bombs) {

        Set<String> dangerZone = computeDangerZone(grid, bombs);
        Player enemy = nearestEnemy(players);

        // ── 1. Flee enemies bombs ───────────────────────────────────────
        Set<String> enemyDanger = computeEnemyDangerZone(grid, bombs);
        if (isInDanger(getX(), getY(), enemyDanger)) {
            Direction flee = fleeToSafety(getX(), getY(), grid, enemyDanger);
            if (flee != null){
                return new AIAction(flee, false);
            }
        }

        // ── 2. No enemy -> stay still ────────────────────────────────
        if (enemy == null){
            return new AIAction(null, false);
        }

        int ex = enemy.getX(), ey = enemy.getY();

        // ── 3.Drop bomb if next to enemy (distance 1) ───────────────
        if (manhattan(getX(), getY(), ex, ey) == 1 && canPlaceBomb()) {
            return new AIAction(null, true);
        }

        // ── 4. Drop bomb if enemy is align with action range ───────
        if (canPlaceBomb() && isAlignedWith(ex, ey, grid) && manhattan(getX(), getY(), ex, ey) <= BOMB_RANGE_TRIGGER) {
            return new AIAction(null, true);
        }

        // ── 5. go for ennemies ───────────────────────────────────────────
        Direction toward = bfsToward(getX(), getY(), ex, ey, grid);
        return new AIAction(toward, false);
    }

    // ─── private utilities ────────────────────────────────────────────────────


    private Set<String> computeEnemyDangerZone(CellType[][] grid, List<Bomb> bombs) {
        var filtered = bombs.stream().filter(b -> b.getOwnerID() != this.getId()).toList();
        return computeDangerZone(grid, filtered);
    }
}
