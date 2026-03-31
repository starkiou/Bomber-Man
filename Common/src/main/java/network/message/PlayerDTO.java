package network.message;

import org.json.JSONObject;

public class PlayerDTO {
	private int id;
	private double y;
	private double x;
	private boolean isDead;
	public PlayerDTO(int id, double y, double x, boolean isDead) {
		this.id = id;
		this.y = y;
		this.x = x;
		this.isDead = isDead;
	}
	
	public PlayerDTO(JSONObject datajason) {
		this.id = datajason.getInt("id");
		this.y = datajason.getInt("y");
		this.x = datajason.getDouble("x");
		this.isDead = datajason.getBoolean("isDead");
	}
	
	public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("id", this.id);
        obj.put("y", this.y);
        obj.put("x", this.x);
        obj.put("isDead", this.isDead);
        return obj;
	}

	public int getId() {
		return id;
	}

	public double getY() {
		return y;
	}

	public double getX() {
		return x;
	}

	public boolean isDead() {
		return isDead;
	}
	
	
	
}
