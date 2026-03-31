package network.message;

import org.json.JSONObject;

public class BombDTO {
	private int id;
	private double y;
	private double x;
	private int timeRemaining;
	public BombDTO(int id, double y, double x,  int timeRemaining) {
		this.id = id;
		this.y = y;
		this.x = x;
		this.timeRemaining = timeRemaining;
	}
	
	public BombDTO(JSONObject datajason) {
		this.id = datajason.getInt("id");
		this.y = datajason.getInt("y");
		this.x = datajason.getDouble("x");
		this.timeRemaining = datajason.getInt("timeRemaining");
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

	public int getTimeRemaining() {
		return timeRemaining;
	}
	
	public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("id", this.id);
        obj.put("y", this.y);
        obj.put("x", this.x);
        obj.put("timeRemaining", this.timeRemaining);
        return obj;
	}
	
}
