package model.entity;

public abstract class Entity {
    protected int id;
    protected int x;
    protected int y;
    protected boolean isSolid;

    public Entity(int id, int x, int y, boolean isSolid) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.isSolid = isSolid;
    }

    public int getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isSolid() {
        return isSolid;
    }

}
