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

    /** Cellules occupées par une bombe : infranchissables, comme pour les joueurs humains. */
    private Set<String> bombCells = Set.of();

    /** Cellules occupées par un autre joueur vivant. */
    private Set<String> occupiedCells = Set.of();

    public final AIAction computeAction(CellType[][] grid, List<Player> players, List<Bomb> bombs) {
        Set<String> cells = new HashSet<>();
        for (Bomb b : bombs) {
            if (!b.isExploded()) cells.add(b.getX() + "," + b.getY());
        }
        this.bombCells = cells;

        Set<String> occupied = new HashSet<>();
        for (Player p : players) {
            if (p.getId() != this.getId() && !p.isDead()) occupied.add(p.getX() + "," + p.getY());
        }
        this.occupiedCells = occupied;

        AIAction action = decide(grid, players, bombs);
        Direction aside = stepAside(grid, players, bombs, action);
        return aside != null ? new AIAction(aside, false) : action;
    }

    /**
     * Deux bots de même stratégie posés sur la même case prennent ensuite
     * exactement les mêmes décisions et restent superposés indéfiniment. Quand
     * ça arrive (en général après une fuite, où les bots ont le droit de se
     * croiser), celui qui a le plus grand identifiant s'écarte d'une case.
     *
     * @return la direction à prendre pour se séparer, ou {@code null} s'il n'y a rien à faire
     */
    private Direction stepAside(CellType[][] grid, List<Player> players, List<Bomb> bombs, AIAction decided) {
        boolean sharesCellWithLowerId = false;
        for (Player p : players) {
            if (!p.isDead() && p.getId() < this.getId() && p.getX() == getX() && p.getY() == getY()) {
                sharesCellWithLowerId = true;
                break;
            }
        }
        if (!sharesCellWithLowerId) return null;

        Set<String> danger = computeDangerZone(grid, bombs);
        // En danger, la fuite passe avant tout : on se séparera une fois à l'abri
        if (isInDanger(getX(), getY(), danger)) return null;

        int h = grid.length, w = grid[0].length;
        Direction fallback = null;
        for (int d = 0; d < 4; d++) {
            int nx = getX() + DIRS[d][0], ny = getY() + DIRS[d][1];
            if (!inBounds(nx, ny, w, h) || !isWalkable(grid, nx, ny) || isInDanger(nx, ny, danger)) continue;
            // L'autre bot va sans doute prendre la direction décidée ici : on en choisit une autre
            if (DIR_ENUM[d] != decided.move()) return DIR_ENUM[d];
            fallback = DIR_ENUM[d];
        }
        return fallback;
    }

    protected abstract AIAction decide(CellType[][] grid, List<Player> players, List<Bomb> bombs);

    /** Case franchissable : ni mur, ni brique, ni bombe. Sert aux fuites, où tout passage compte. */
    protected boolean isFree(CellType[][] grid, int x, int y) {
        return grid[y][x] == CellType.EMPTY && !bombCells.contains(x + "," + y);
    }

    /** Case où un bot accepte d'aller de lui-même : franchissable et sans autre joueur dessus. */
    protected boolean isWalkable(CellType[][] grid, int x, int y) {
        return isFree(grid, x, y) && !occupiedCells.contains(x + "," + y);
    }

    // ─── BFS core ────────────────────────────────────────────────────────────

    /** Quatre directions cardinales (ordre : haut, bas, gauche, droite). */
    protected static final int[][] DIRS = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
    protected static final Direction[] DIR_ENUM = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

    /** Test sur une cellule (coordonnées x,y). */
    @FunctionalInterface
    protected interface CellTest {
        boolean test(int x, int y);
    }

    /**
     * BFS générique : renvoie la première direction à prendre depuis (sx,sy) pour
     * atteindre une cellule satisfaisant {@code isGoal}, en ne traversant que des
     * cellules satisfaisant {@code canEnter}. {@code null} si aucun chemin.
     */
    protected Direction bfsFirstStep(int sx, int sy, CellTest canEnter, CellTest isGoal, CellType[][] grid) {
        int h = grid.length, w = grid[0].length;
        boolean[][] visited = new boolean[w][h];
        Queue<int[]> queue = new LinkedList<>();
        visited[sx][sy] = true;

        for (int d = 0; d < 4; d++) {
            int nx = sx + DIRS[d][0], ny = sy + DIRS[d][1];
            if (inBounds(nx, ny, w, h) && !visited[nx][ny] && canEnter.test(nx, ny)) {
                visited[nx][ny] = true;
                if (isGoal.test(nx, ny)) return DIR_ENUM[d];
                queue.add(new int[]{nx, ny, d});
            }
        }
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            for (int d = 0; d < 4; d++) {
                int nx = cur[0] + DIRS[d][0], ny = cur[1] + DIRS[d][1];
                if (inBounds(nx, ny, w, h) && !visited[nx][ny] && canEnter.test(nx, ny)) {
                    visited[nx][ny] = true;
                    if (isGoal.test(nx, ny)) return DIR_ENUM[cur[2]];
                    queue.add(new int[]{nx, ny, cur[2]});
                }
            }
        }
        return null;
    }

    /**
     * BFS générique booléen : {@code true} s'il existe une cellule atteignable
     * (départ inclus) satisfaisant {@code isGoal}, en ne traversant que des
     * cellules {@code canEnter}.
     */
    protected boolean bfsAnyReachable(int sx, int sy, CellTest canEnter, CellTest isGoal, CellType[][] grid) {
        int h = grid.length, w = grid[0].length;
        boolean[][] visited = new boolean[w][h];
        Queue<int[]> queue = new LinkedList<>();
        visited[sx][sy] = true;
        if (isGoal.test(sx, sy)) return true;
        queue.add(new int[]{sx, sy});

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            for (int[] dir : DIRS) {
                int nx = cur[0] + dir[0], ny = cur[1] + dir[1];
                if (inBounds(nx, ny, w, h) && !visited[nx][ny] && canEnter.test(nx, ny)) {
                    visited[nx][ny] = true;
                    if (isGoal.test(nx, ny)) return true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }
        return false;
    }

    // ─── BFS ─────────────────────────────────────────────────────────────────

    protected Direction bfsToward(int startX, int startY, int targetX, int targetY, CellType[][] grid) {
        // Au contact de la cible, on ne lui marche pas dessus
        if (manhattan(startX, startY, targetX, targetY) <= 1) return null;
        return bfsFirstStep(startX, startY,
                (x, y) -> isWalkable(grid, x, y) || (x == targetX && y == targetY),
                (x, y) -> x == targetX && y == targetY,
                grid);
    }

    protected Direction bfsTowardBrick(int startX, int startY, int targetX, int targetY, CellType[][] grid) {
        if (startX == targetX && startY == targetY) return null;

        int h = grid.length;
        int w = grid[0].length;
        boolean[][] visited = new boolean[w][h];
        Queue<int[]> queue = new LinkedList<>();

        visited[startX][startY] = true;
        for (int d = 0; d < 4; d++) {
            int nx = startX + DIRS[d][0];
            int ny = startY + DIRS[d][1];
            if (!inBounds(nx, ny, w, h) || visited[nx][ny]) continue;
            CellType cell = grid[ny][nx];
            if (cell == CellType.WALL) continue;
            visited[nx][ny] = true;
            if (nx == targetX && ny == targetY) return DIR_ENUM[d];
            if (isWalkable(grid, nx, ny)) queue.add(new int[]{nx, ny, d});
        }

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int cx = cur[0], cy = cur[1], firstDir = cur[2];
            for (int d = 0; d < 4; d++) {
                int nx = cx + DIRS[d][0];
                int ny = cy + DIRS[d][1];
                if (!inBounds(nx, ny, w, h) || visited[nx][ny]) continue;
                CellType cell = grid[ny][nx];
                if (cell == CellType.WALL) continue;
                visited[nx][ny] = true;
                if (nx == targetX && ny == targetY) return DIR_ENUM[firstDir];
                if (isWalkable(grid, nx, ny)) queue.add(new int[]{nx, ny, firstDir});
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
        if (!isInDanger(startX, startY, dangerZone)) {
            return null;
        }
        // On vise d'abord un abri libre ; à défaut, n'importe quel abri, quitte à
        // y rejoindre un autre joueur.
        Direction toFreeShelter = bfsFirstStep(startX, startY,
                (x, y) -> isFree(grid, x, y),
                (x, y) -> !isInDanger(x, y, dangerZone) && isWalkable(grid, x, y),
                grid);
        if (toFreeShelter != null) return toFreeShelter;

        return bfsFirstStep(startX, startY,
                (x, y) -> isFree(grid, x, y),
                (x, y) -> !isInDanger(x, y, dangerZone),
                grid);
    }

    protected boolean hasEscapeRoute(int x, int y, CellType[][] grid, Set<String> futureDanger) {
        return bfsAnyReachable(x, y,
                (nx, ny) -> isFree(grid, nx, ny),
                (cx, cy) -> !isInDanger(cx, cy, futureDanger),
                grid);
    }

    // ─── Cooldowns ───────────────────────────────────────────────────────────────

    private long moveCooldownMs = 200;

    private static final long BOMB_COOLDOWN_MS = 500;

    private long lastMoveTime  = 0;
    private long lastBombTime  = 0;

    public boolean canMove() {
        return System.currentTimeMillis() - lastMoveTime >= moveCooldownMs;
    }

    public boolean canBomb() {
        return System.currentTimeMillis() - lastBombTime >= BOMB_COOLDOWN_MS;
    }

    public void onMoveDone() {
        lastMoveTime = System.currentTimeMillis();
    }

    public void onBombDone() {
        lastBombTime = System.currentTimeMillis();
    }

    public void setMoveCooldownMs(long ms) {
        this.moveCooldownMs = ms;
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
                if (grid[y][ax] == CellType.WALL){
                    return false;
                }
            }
            return true;
        }
        if (ay == ty) {
            int minX = Math.min(ax, tx);
            int maxX = Math.max(ax, tx);
            for (int x = minX; x <= maxX; x++) {
                if (grid[ay][x] == CellType.WALL){
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
