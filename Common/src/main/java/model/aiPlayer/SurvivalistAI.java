package model.aiPlayer;

import model.entity.Bomb;
import model.entity.Direction;
import model.entity.Player;
import model.maze.CellType;

import java.util.List;
import java.util.Set;

public class SurvivalistAI extends AIPlayer{

    private static final int OFFENSIVE_THRESHOLD = 1;

    private static final int SAFE_DISTANCE = 4;

    public SurvivalistAI(int id, int x, int y, int hp, double speed, int maxBombs) {
        super(id, x, y, hp, speed, maxBombs);
    }

    @Override
    public AIAction computeAction(CellType[][] grid, List<Player> players, List<Bomb> bombs) {

        Set<String> dangerZone = computeDangerZone(grid, bombs);

        // ── 1. flee if possible ───────────────────────────────────────
        if (isInDanger(getX(), getY(), dangerZone)) {
            Direction flee = fleeToSafety(getX(), getY(), grid, dangerZone);
            return new AIAction(flee, false);
        }

        long enemyCount = aliveEnemiesCount(players);
        Player enemy = nearestEnemy(players);

        if (enemy == null){
            return new AIAction(null, false);
        }

        // ── 2. Survival phase : more than one ennemy alive ─────────────────
        if (enemyCount > OFFENSIVE_THRESHOLD) {
            return survivalMove(grid, players, dangerZone, enemy);
        }

        // ── 3. 1v1 phase : try to attack ───────────────────────────────────
        return offensiveMove(grid, bombs, dangerZone, enemy);
    }

    // ─── Survival phase ────────────────────────────────────────────────────────

    private AIAction survivalMove(CellType[][] grid, List<Player> players, Set<String> dangerZone, Player nearest) {

        if (manhattan(getX(), getY(), nearest.getX(), nearest.getY()) < SAFE_DISTANCE) {
            Direction away = bfsAwayFrom(nearest.getX(), nearest.getY(), grid, dangerZone);
            if (away != null){
                return new AIAction(away, false);
            }
        }

        return new AIAction(null, false);
    }

    // ───  1v1 phase ───────────────────────────────────────────────────────────

    private AIAction offensiveMove(CellType[][] grid, List<Bomb> bombs, Set<String> dangerZone, Player enemy) {

        int ex = enemy.getX(), ey = enemy.getY();
        int dist = manhattan(getX(), getY(), ex, ey);

        if (dist <= 2 && canPlaceBomb()) {
            Set<String> futureZone = dangerIfBombAt(getX(), getY(),getCurrentBombRadius(), grid, bombs);
            if (hasEscapeRoute(getX(), getY(), grid, futureZone)) {
                return new AIAction(null, true);
            }
        }

        if (canPlaceBomb() && isAlignedWith(ex, ey, grid) && dist <= 3) {
            Set<String> futureZone = dangerIfBombAt(getX(), getY(),getCurrentBombRadius(), grid, bombs);
            if (hasEscapeRoute(getX(), getY(), grid, futureZone)) {
                return new AIAction(null, true);
            }
        }

        Direction toward = bfsTowardSafe(getX(), getY(), ex, ey, grid, dangerZone);
        return new AIAction(toward, false);
    }

    // ─── movement utilities ──────────────────────────────────────────

    private Direction bfsAwayFrom(int fx, int fy, CellType[][] grid, Set<String> dangerZone) {
        int w = grid[0].length, h = grid.length;
        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        Direction[] dirEnum = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

        int bestDir = -1;
        int bestDist = manhattan(getX(), getY(), fx, fy);

        for (int d = 0; d < 4; d++) {
            int nx = getX() + dirs[d][0];
            int ny = getY() + dirs[d][1];
            if (!inBounds(nx, ny, w, h)) continue;
            if (grid[ny][nx] != CellType.EMPTY) continue;
            if (isInDanger(nx, ny, dangerZone)) continue;
            int dist = manhattan(nx, ny, fx, fy);
            if (dist > bestDist) {
                bestDist = dist;
                bestDir = d;
            }
        }
        return bestDir >= 0 ? dirEnum[bestDir] : null;
    }

    private Direction bfsTowardSafe(int startX, int startY, int targetX, int targetY, CellType[][] grid, Set<String> dangerZone) {
        int w = grid[0].length, h = grid.length;
        boolean[][] visited = new boolean[w][h];
        java.util.Queue<int[]> queue = new java.util.LinkedList<>();
        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        Direction[] dirEnum = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

        visited[startX][startY] = true;
        for (int d = 0; d < 4; d++) {
            int nx = startX + dirs[d][0];
            int ny = startY + dirs[d][1];
            if (!inBounds(nx, ny, w, h)) continue;
            if (grid[ny][nx] != CellType.EMPTY) continue;
            if (isInDanger(nx, ny, dangerZone)) continue;
            if (!visited[nx][ny]) {
                visited[nx][ny] = true;
                queue.add(new int[]{nx, ny, d});
            }
        }
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int cx = cur[0], cy = cur[1], firstDir = cur[2];
            if (cx == targetX && cy == targetY) return dirEnum[firstDir];
            for (int[] d : dirs) {
                int nx = cx + d[0];
                int ny = cy + d[1];
                if (!inBounds(nx, ny, w, h)) continue;
                if (grid[ny][nx] != CellType.EMPTY) continue;
                if (isInDanger(nx, ny, dangerZone)) continue;
                if (!visited[nx][ny]) {
                    visited[nx][ny] = true;
                    queue.add(new int[]{nx, ny, firstDir});
                }
            }
        }
        return null;
    }

    private int getCurrentBombRadius() {
        return 2; // à modifier si la valeur peut changer
    }
}
