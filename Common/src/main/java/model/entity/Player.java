package main.java.model.entity;

import model.maze.CellType;

public class Player extends Entity{
    private int hp;
    private double speed;
    private int maxBombs;
    private int currentBombs;

    private long lastBombRegenTime;
    private static final long REGEN_TIME_MS = 5000;

    public Player(int ID, int x, int y, boolean isSolid, int hp, double speed, int maxBombs, int currentBombs) {
        super(ID, x, y, isSolid);
        this.hp = hp;
        this.speed = speed;
        this.maxBombs = maxBombs;
        this.currentBombs = currentBombs;
        this.lastBombRegenTime = System.currentTimeMillis();
    }

    // ─── movement ──────────────────────────────────────────────────────────

    public void deplacement(CellType[][] grid, Direction dir) {
        int nextX = this.x;
        int nextY = this.y;
        switch (dir) {
            case UP    -> nextY--;
            case DOWN  -> nextY++;
            case LEFT  -> nextX--;
            case RIGHT -> nextX++;
        }
        if (nextY >= 0 && nextY < grid.length && nextX >= 0 && nextX < grid[0].length) {
            if (grid[nextY][nextX] == CellType.EMPTY) {
                this.x = nextX;
                this.y = nextY;
                System.out.println("Déplacement réussi en : " + x + ", " + y);
            } else {
                System.out.println("C'est un mur !");
            }
        } else {
            System.out.println("Hors des limites du labyrinthe !");
        }
    }

    // ─── life ──────────────────────────────────────────────────────────

    public void takeDamage(int amount) {
        this.hp = Math.max(0, this.hp - amount);
    }

    public boolean isDead() {
        return hp <= 0;
    }

    // ─── Bombs ───────────────────────────────────────────────────────────────

    public boolean canPlaceBomb() {
        return currentBombs > 0;
    }

    public void onBombPlaced() {
        if (currentBombs > 0) {
            currentBombs--;
        }
    }

    private void regenBombs() {
        if (currentBombs >= maxBombs) {
            lastBombRegenTime = System.currentTimeMillis();
            return;
        }

        long currentTime = System.currentTimeMillis();

        if (currentTime - lastBombRegenTime >= REGEN_TIME_MS) {
            currentBombs++;
            System.out.println("Bombe récupérée ! Total : " + currentBombs);

            lastBombRegenTime = currentTime;
        }
    }

    // ─── Logic ──────────────────────────────────────────────────────────────

    // à mettre dans la boucle de gameplay
    public void update() {
        regenBombs();
    }

}
