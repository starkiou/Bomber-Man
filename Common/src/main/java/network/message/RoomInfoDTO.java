package network.message;

import org.json.JSONObject;

public class RoomInfoDTO {
    private int id;
    private int currentPlayers;
    private int maxPlayers;
    private boolean inGame;

    public RoomInfoDTO(int id, int currentPlayers, int maxPlayers, boolean inGame) {
        this.id = id;
        this.currentPlayers = currentPlayers;
        this.maxPlayers = maxPlayers;
        this.inGame = inGame;
    }

    public RoomInfoDTO(JSONObject json) {
        this.id = json.getInt("id");
        this.currentPlayers = json.getInt("currentPlayers");
        this.maxPlayers = json.getInt("maxPlayers");
        this.inGame = json.getBoolean("inGame");
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("id", this.id);
        obj.put("currentPlayers", this.currentPlayers);
        obj.put("maxPlayers", this.maxPlayers);
        obj.put("inGame", this.inGame);
        return obj;
    }

    public int getRoomId() {
    	return id;
    }
    public int getCurrentPlayers() {
    	return currentPlayers;
    }
    public int getMaxPlayers() {
    	return maxPlayers;
    }
    public boolean isInGame() {
    	return inGame;
    }
    public boolean isFull() {
    	return currentPlayers >= maxPlayers;
    }
}