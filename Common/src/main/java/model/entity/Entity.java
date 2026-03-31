package main.java.model.entity;

public  abstract class Entity {
    private int ID;
    private double X;
    private double Y;
    private boolean isSolid;

    public Entity(int ID, double x, double y, boolean isSolid) {
        this.ID = ID;
        X = x;
        Y = y;
        this.isSolid = isSolid;
    }
}
