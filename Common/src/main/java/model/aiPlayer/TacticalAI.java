package model.aiPlayer;

import model.entity.Bomb;
import model.entity.Direction;
import model.entity.Player;
import model.maze.CellType;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class TacticalAI extends AIPlayer{

    private static final int DEFAULT_BOMB_RADIUS = 2;

    private static final int COMBAT_RANGE = 5;

    // ─── Internal state ────────────────────────────────────────────────────────

    private boolean retreating = false;

    public TacticalAI(int id, int x, int y, int hp, double speed, int maxBombs) {
        super(id, x, y, hp, speed, maxBombs);
    }

    @Override
    public AIAction computeAction(CellType[][] grid, List<Player> players, List<Bomb> bombs) {

        Set<String> dangerZone = computeDangerZone(grid, bombs);

        // ── 1. Flee if possible ───────────────────────────────────────
        if (isInDanger(getX(), getY(), dangerZone)) {
            retreating = true;
            Direction flee = fleeToSafety(getX(), getY(), grid, dangerZone);
            return new AIAction(flee, false);
        }

        retreating = false;

        Player enemy = nearestEnemy(players);
        if (enemy == null) return new AIAction(null, false);

        int dist = manhattan(getX(), getY(), enemy.getX(), enemy.getY());

        // ── 2. Combat phase : ennemy is close and align ───────────────────────
        if (dist <= COMBAT_RANGE && isAlignedWith(enemy.getX(), enemy.getY(), grid)) {
            return combatAction(grid, bombs, dangerZone, enemy);
        }

        // ── 3. Combat phase : near ennemies ──────────────────
        if (dist <= 2) {
            return combatAction(grid, bombs, dangerZone, enemy);
        }

        // ── 4. Exploration phase : try to find a brick to destroy ─────────────
        AIAction exploration = exploreAndDestroy(grid, bombs, dangerZone, enemy);
        if (exploration != null) {
            return exploration;
        }

        // ── 5. Fallback : try to get to ennemies ──────────────────────────
        Direction toward = bfsToward(getX(), getY(), enemy.getX(), enemy.getY(), grid);
        return new AIAction(toward, false);
    }

    // ─── Combat phase ────────────────────────────────────────────────────────

    private AIAction combatAction(CellType[][] grid, List<Bomb> bombs, Set<String> dangerZone, Player enemy) {
        if (!canPlaceBomb()) {
            Direction toward = bfsTowardAvoidingDanger(getX(), getY(), enemy.getX(), enemy.getY(), grid, dangerZone);
            return new AIAction(toward, false);
        }

        Set<String> futureZone = dangerIfBombAt(getX(), getY(), DEFAULT_BOMB_RADIUS, grid, bombs);

        if (hasEscapeRoute(getX(), getY(), grid, futureZone)) {
            retreating = true;
            return new AIAction(null, true);
        }

        Direction reposition = repositionForBomb(grid, bombs, dangerZone, enemy);
        return new AIAction(reposition, false);
    }

    // ─── Exploring phase ───────────────────────────────────────────────────

    private AIAction exploreAndDestroy(CellType[][] grid, List<Bomb> bombs, Set<String> dangerZone, Player enemy) {
        int[] nearestBrick = findNearestBrick(grid);
        if (nearestBrick == null){
            return null;
        }

        int bx = nearestBrick[0], by = nearestBrick[1];

        if (isAdjacentTo(bx, by) && canPlaceBomb()) {
            Set<String> futureZone = dangerIfBombAt(getX(), getY(), DEFAULT_BOMB_RADIUS, grid, bombs);
            if (hasEscapeRoute(getX(), getY(), grid, futureZone)) {
                return new AIAction(null, true);
            }
            return null;
        }

        Direction toward = bfsTowardBrick(getX(), getY(), bx, by, grid);
        if (toward != null){
            return new AIAction(toward, false);
        }

        return null;
    }

    // ─── Repositioned ────────────────────────────────────────────────────

    private Direction repositionForBomb(CellType[][] grid, List<Bomb> bombs, Set<String> dangerZone, Player enemy) {
        int w = grid[0].length, h = grid.length;
        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        Direction[] dirEnum = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

        int bestDir = -1;
        int bestScore = Integer.MAX_VALUE;

        for (int d = 0; d < 4; d++) {
            int nx = getX() + dirs[d][0];
            int ny = getY() + dirs[d][1];
            if (!inBounds(nx, ny, w, h)) continue;
            if (grid[ny][nx] != CellType.EMPTY) continue;
            if (isInDanger(nx, ny, dangerZone)) continue;

            Set<String> futureZone = dangerIfBombAt(nx, ny, DEFAULT_BOMB_RADIUS, grid, bombs);
            if (!hasEscapeRoute(nx, ny, grid, futureZone)) continue;

            int distToEnemy = manhattan(nx, ny, enemy.getX(), enemy.getY());
            if (distToEnemy < bestScore) {
                bestScore = distToEnemy;
                bestDir = d;
            }
        }
        return bestDir >= 0 ? dirEnum[bestDir] : null;
    }

    // ─── BFS with avoiding danger ────────────────────────────────────────

    private Direction bfsTowardAvoidingDanger(int startX, int startY, int targetX, int targetY, CellType[][] grid, Set<String> dangerZone) {
        int w = grid[0].length, h = grid.length;
        boolean[][] visited = new boolean[w][h];
        Queue<int[]> queue = new LinkedList<>();
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
            if (cur[0] == targetX && cur[1] == targetY) return dirEnum[cur[2]];
            for (int[] d : dirs) {
                int nx = cur[0] + d[0];
                int ny = cur[1] + d[1];
                if (!inBounds(nx, ny, w, h)) continue;
                if (grid[ny][nx] != CellType.EMPTY) continue;
                if (isInDanger(nx, ny, dangerZone)) continue;
                if (!visited[nx][ny]) {
                    visited[nx][ny] = true;
                    queue.add(new int[]{nx, ny, cur[2]});
                }
            }
        }
        return bfsToward(startX, startY, targetX, targetY, grid);
    }

    // ─── utilities ─────────────────────────────────────────────────────────

    private int[] findNearestBrick(CellType[][] grid) {
        int w = grid[0].length, h = grid.length;
        int bestDist = Integer.MAX_VALUE;
        int[] best = null;

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (grid[y][x] == CellType.BRICK) {
                    int d = manhattan(getX(), getY(), x, y);
                    if (d < bestDist) {
                        bestDist = d;
                        best = new int[]{x, y};
                    }
                }
            }
        }
        return best;
    }

    private boolean isAdjacentTo(int tx, int ty) {
        return manhattan(getX(), getY(), tx, ty) == 1;
    }
}
