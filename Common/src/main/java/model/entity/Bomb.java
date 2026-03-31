package main.java.model.entity;

public class Bomb extends Entity{
    private int ownerID;
    private int radius;
    private long placedTime;
    private int explosionDelay;
    private boolean exploded = false;

    public Bomb(int ID, int x, int y, boolean isSolid, int ownerID, int radius, int explosionDelay) {
        super(ID, x, y, isSolid);
        this.ownerID = ownerID;
        this.radius = radius;
        this.explosionDelay = explosionDelay;
        this.placedTime = System.currentTimeMillis();
    }

    public boolean isExploded() {
        return exploded;
    }

    public int getRadius() {
        return radius;
    }

    public int getOwnerID() {
        return ownerID;
    }

    // ─── Logic ──────────────────────────────────────────────────────────────

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
}
