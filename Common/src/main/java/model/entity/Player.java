package main.java.model.entity;

public class Player extends Entity{
    private int hp;
    private double speed;
    private int maxBombs;
    private int currentBombs;

    public Player(int ID, double x, double y, boolean isSolid, int hp, double speed, int maxBombs, int currentBombs) {
        super(ID, x, y, isSolid);
        this.hp = hp;
        this.speed = speed;
        this.maxBombs = maxBombs;
        this.currentBombs = currentBombs;
    }
}
