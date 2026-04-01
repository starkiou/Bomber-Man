package network.message;

import org.json.JSONObject;

public class RoomInfoDTO {
    private int id;
    private int currentPlayers;
    private int maxPlayers;
    private boolean inGame;
    private String roomName;

    public RoomInfoDTO(int id, int currentPlayers, int maxPlayers, boolean inGame, String roomName) {
        this.id = id;
        this.currentPlayers = currentPlayers;
        this.maxPlayers = maxPlayers;
        this.inGame = inGame;
        this.roomName=roomName;
    }

    public RoomInfoDTO(JSONObject json) {
        this.id = json.getInt("id");
        this.currentPlayers = json.getInt("currentPlayers");
        this.maxPlayers = json.getInt("maxPlayers");
        this.inGame = json.getBoolean("inGame");
        this.roomName = json.getString("roomName");
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("currentPlayers", this.currentPlayers);
        json.put("maxPlayers", this.maxPlayers);
        json.put("inGame", this.inGame);
        json.put("roomName", this.roomName);
        return json;
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
    public String getRoomName() {
    	return roomName;
    }
}