<<<<<<< HEAD
package model.entity;

import model.maze.CellType;
import java.util.ArrayList;
import java.util.List;

public class Bomb extends Entity {
    private int ownerID;
    private int radius;
    private long placedTime;
    private int explosionDelay;
    private boolean exploded = false;

    public Bomb(int id, int x, int y, int ownerID, int radius, int explosionDelay) {
        super(id, x, y, true);
        this.ownerID = ownerID;
        this.radius = radius;
        this.explosionDelay = explosionDelay;
        this.placedTime = System.currentTimeMillis();
    }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public boolean isExploded() {
        return exploded;
    }

    public int getRadius() {
        return radius;
    }

    public int getOwnerID() {
        return ownerID;
    }

    // ─── Logic ───────────────────────────────────────────────────────────────

    // à mettre dans la boucle de gameplay
    public void update() {
        if (!exploded && isTimeToExplode()) {
            explode();
        }
    }

    private boolean isTimeToExplode() {
        return (System.currentTimeMillis() - placedTime) >= explosionDelay;
    }

    private void explode() {
        this.exploded = true;
        System.out.println("BOOM ! La bombe " + id + " explose avec un rayon de " + radius);
    }

    // ─── Explosion Logic ──────────────────────────────────────────────────────

    public List<int[]> getExplosionArea(CellType[][] grid) {
        List<int[]> affectedCells = new ArrayList<>();

        affectedCells.add(new int[]{this.x, this.y});

        int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

        for (int[] dir : directions) {
            for (int i = 1; i <= this.radius; i++) {
                int nextX = this.x + (dir[0] * i);
                int nextY = this.y + (dir[1] * i);
                if (nextY < 0 || nextY >= grid.length || nextX < 0 || nextX >= grid[0].length) {
                    break;
                }
                CellType cell = grid[nextY][nextX];
                if (cell == CellType.WALL) {
                    break;
                }
                affectedCells.add(new int[]{nextX, nextY});
                if (cell == CellType.BRICK) {
                    break;
                }
            }
        }
        return affectedCells;
    }

=======
package main.java.model.entity;

public class Bomb extends Entity{
    private int ownerID;
    private int radius;
    private int explosionDelay;

    public Bomb(int ID, double x, double y, boolean isSolid, int ownerID, int radius, int explosionDelay) {
        super(ID, x, y, isSolid);
        this.ownerID = ownerID;
        this.radius = radius;
        this.explosionDelay = explosionDelay;
    }
>>>>>>> 4aa410e (feat(game-mechanics): create classes skeleton)
}
