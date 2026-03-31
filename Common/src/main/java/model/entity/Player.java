package main.java.model.entity;

import model.maze.CellType;

public class Player extends Entity{
    private int hp;
    private double speed;
    private int maxBombs;
    private int currentBombs;

    public Player(int ID, int x, int y, boolean isSolid, int hp, double speed, int maxBombs, int currentBombs) {
        super(ID, x, y, isSolid);
        this.hp = hp;
        this.speed = speed;
        this.maxBombs = maxBombs;
        this.currentBombs = currentBombs;
    }

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
}
