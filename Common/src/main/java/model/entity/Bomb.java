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
}
