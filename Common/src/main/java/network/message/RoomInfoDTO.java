// RoomInfoDTO.java
package network.message;

import org.json.JSONObject;

public class RoomInfoDTO {
    private String name;
    private int currentPlayers;
    private int maxPlayers;
    private boolean inGame;

    public RoomInfoDTO(String name, int currentPlayers, int maxPlayers, boolean inGame) {
        this.name = name;
        this.currentPlayers = currentPlayers;
        this.maxPlayers = maxPlayers;
        this.inGame = inGame;
    }

    public RoomInfoDTO(JSONObject json) {
        this.name = json.getString("name");
        this.currentPlayers = json.getInt("currentPlayers");
        this.maxPlayers = json.getInt("maxPlayers");
        this.inGame = json.getBoolean("inGame");
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("name", this.name);
        obj.put("currentPlayers", this.currentPlayers);
        obj.put("maxPlayers", this.maxPlayers);
        obj.put("inGame", this.inGame);
        return obj;
    }

    public String getName() {
    	return name;
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