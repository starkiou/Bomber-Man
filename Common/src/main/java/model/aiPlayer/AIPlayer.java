package model.aiPlayer;

import model.entity.Bomb;
import model.entity.Direction;
import model.entity.Player;
import model.maze.CellType;

import java.util.*;

public abstract class AIPlayer extends Player {

    public AIPlayer(int id, int x, int y, int hp, double speed, int maxBombs) {
        super(id, x, y, hp, speed, maxBombs);
    }

    // ─── Action ──────────────────────────────────────────────────────────────

    public record AIAction(Direction move, boolean placeBomb) {}


    // ─── Interface for AI ─────────────────────────────────────────────

    public abstract AIAction computeAction(CellType[][] grid, List<Player> players, List<Bomb> bombs);

    // ─── BFS ─────────────────────────────────────────────────────────────────

    protected Direction bfsToward(int startX, int startY, int targetX, int targetY, CellType[][] grid) {
        if (startX == targetX && startY == targetY) return null;

        int h = grid[0].length;
        int w = grid.length;
        boolean[][] visited = new boolean[w][h];
        Queue<int[]> queue = new LinkedList<>();

        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        Direction[] dirEnum = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

        visited[startX][startY] = true;
        for (int d = 0; d < 4; d++) {
            int nx = startX + dirs[d][0];
            int ny = startY + dirs[d][1];
            if (inBounds(nx, ny, w, h) && grid[nx][ny] == CellType.EMPTY && !visited[nx][ny]) {
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
                if (inBounds(nx, ny, w, h) && grid[nx][ny] == CellType.EMPTY && !visited[nx][ny]) {
                    visited[nx][ny] = true;
                    queue.add(new int[]{nx, ny, firstDir});
                }
            }
        }
        return null;
    }

    protected Direction bfsTowardBrick(int startX, int startY, int targetX, int targetY, CellType[][] grid) {
        if (startX == targetX && startY == targetY) return null;

        int h = grid[0].length;
        int w = grid.length;
        boolean[][] visited = new boolean[w][h];
        Queue<int[]> queue = new LinkedList<>();

        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        Direction[] dirEnum = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

        visited[startX][startY] = true;
        for (int d = 0; d < 4; d++) {
            int nx = startX + dirs[d][0];
            int ny = startY + dirs[d][1];
            if (!inBounds(nx, ny, w, h) || visited[nx][ny]) continue;
            CellType cell = grid[nx][ny];
            if (cell == CellType.WALL) continue;
            visited[nx][ny] = true;
            if (nx == targetX && ny == targetY) return dirEnum[d];
            if (cell == CellType.EMPTY) queue.add(new int[]{nx, ny, d});
        }

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int cx = cur[0], cy = cur[1], firstDir = cur[2];
            for (int d = 0; d < 4; d++) {
                int nx = cx + dirs[d][0];
                int ny = cy + dirs[d][1];
                if (!inBounds(nx, ny, w, h) || visited[nx][ny]) continue;
                CellType cell = grid[nx][ny];
                if (cell == CellType.WALL) continue;
                visited[nx][ny] = true;
                if (nx == targetX && ny == targetY) return dirEnum[firstDir];
                if (cell == CellType.EMPTY) queue.add(new int[]{nx, ny, firstDir});
            }
        }
        return null;
    }

    // ─── Danger ──────────────────────────────────────────────────────────────

    protected Set<String> computeDangerZone(CellType[][] grid, List<Bomb> bombs) {
        Set<String> danger = new HashSet<>();
        for (Bomb b : bombs) {
            if (b.isExploded()) continue;
            for (int[] cell : b.getExplosionArea(grid)) {
                danger.add(cell[0] + "," + cell[1]);
            }
        }
        return danger;
    }

    protected boolean isInDanger(int x, int y, Set<String> dangerZone) {
        return dangerZone.contains(x + "," + y);
    }

    protected Direction fleeToSafety(int startX, int startY, CellType[][] grid, Set<String> dangerZone) {
        if (!isInDanger(startX, startY, dangerZone)){
            return null;
        }

        int h = grid[0].length;
        int w = grid.length;
        boolean[][] visited = new boolean[w][h];
        Queue<int[]> queue = new LinkedList<>();

        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        Direction[] dirEnum = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

        visited[startX][startY] = true;

        for (int d = 0; d < 4; d++) {
            int nx = startX + dirs[d][0];
            int ny = startY + dirs[d][1];
            if (inBounds(nx, ny, w, h) && grid[nx][ny] == CellType.EMPTY && !visited[nx][ny]) {
                visited[nx][ny] = true;
                queue.add(new int[]{nx, ny, d});
            }
        }

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int cx = cur[0], cy = cur[1], firstDir = cur[2];
            if (!isInDanger(cx, cy, dangerZone)){
                return dirEnum[firstDir];
            }
            for (int[] d : dirs) {
                int nx = cx + d[0];
                int ny = cy + d[1];
                if (inBounds(nx, ny, w, h) && grid[nx][ny] == CellType.EMPTY && !visited[nx][ny]) {
                    visited[nx][ny] = true;
                    queue.add(new int[]{nx, ny, firstDir});
                }
            }
        }
        return null;
    }

    protected boolean hasEscapeRoute(int x, int y, CellType[][] grid, Set<String> futureDanger) {
        int h = grid[0].length;
        int w = grid.length;
        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        boolean[][] visited = new boolean[w][h];
        Queue<int[]> queue = new LinkedList<>();
        visited[x][y] = true;
        queue.add(new int[]{x, y});

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            if (!isInDanger(cur[0], cur[1], futureDanger)){
                return true;
            }
            for (int[] d : dirs) {
                int nx = cur[0] + d[0];
                int ny = cur[1] + d[1];
                if (inBounds(nx, ny, w, h) && grid[nx][ny] == CellType.EMPTY && !visited[nx][ny]) {
                    visited[nx][ny] = true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }
        return false;
    }

    // ─── Utilities ─────────────────────────────────────────────────────────

    protected int manhattan(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    protected boolean inBounds(int x, int y, int w, int h) {
        return x >= 0 && x < w && y >= 0 && y < h;
    }

    protected Player nearestEnemy(List<Player> players) {
        Player nearest = null;
        int best = Integer.MAX_VALUE;
        for (Player p : players) {
            if (p.getId() == this.getId()) continue;
            if (p.isDead()) continue;
            int d = manhattan(this.getX(), this.getY(), p.getX(), p.getY());
            if (d < best) { best = d; nearest = p; }
        }
        return nearest;
    }

    protected long aliveEnemiesCount(List<Player> players) {
        return players.stream().filter(p -> p.getId() != this.getId() && !p.isDead()).count();
    }

    protected boolean isAlignedWith(int tx, int ty, CellType[][] grid) {
        int ax = this.getX(), ay = this.getY();
        if (ax == tx) {
            int minY = Math.min(ay, ty);
            int maxY = Math.max(ay, ty);
            for (int y = minY; y <= maxY; y++) {
                if (grid[ax][y] == CellType.WALL){
                    return false;
                }
            }
            return true;
        }
        if (ay == ty) {
            int minX = Math.min(ax, tx);
            int maxX = Math.max(ax, tx);
            for (int x = minX; x <= maxX; x++) {
                if (grid[x][ay] == CellType.WALL){
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    protected Set<String> dangerIfBombAt(int x, int y, int radius, CellType[][] grid, List<Bomb> activeBombs) {
        Bomb fake = new Bomb(-1, x, y, this.getId(), radius, Integer.MAX_VALUE);
        Set<String> zone = computeDangerZone(grid, activeBombs);
        for (int[] cell : fake.getExplosionArea(grid)) {
            zone.add(cell[0] + "," + cell[1]);
        }
        return zone;
    }
}
