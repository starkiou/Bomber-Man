package network.message;

import org.json.JSONObject;

public class RoomInfoDTO {
    private int id;
    private int currentPlayers;
    private int maxPlayers;
    private boolean inGame;
    private String roomName;
    private String mapSize;
    private String difficulty;
    private int botCount;

    public RoomInfoDTO(int id, int currentPlayers, int maxPlayers, boolean inGame, String roomName, String mapSize, String difficulty, int botCount) {
        this.id = id;
        this.currentPlayers = currentPlayers;
        this.maxPlayers = maxPlayers;
        this.inGame = inGame;
        this.roomName = roomName;
        this.mapSize = mapSize;
        this.difficulty = difficulty;
        this.botCount = botCount;
    }

    /** Désérialisation depuis JSON */
    public RoomInfoDTO(JSONObject json) {
        this.id = json.getInt("id");
        this.currentPlayers = json.getInt("currentPlayers");
        this.maxPlayers = json.getInt("maxPlayers");
        this.inGame = json.getBoolean("inGame");
        this.roomName = json.getString("roomName");
        this.mapSize = json.getString("mapSize");
        this.difficulty = json.getString("difficulty");
        this.botCount = json.getInt("botCount");
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("currentPlayers", this.currentPlayers);
        json.put("maxPlayers", this.maxPlayers);
        json.put("inGame", this.inGame);
        json.put("roomName", this.roomName);
        json.put("mapSize", this.mapSize);
        json.put("difficulty", this.difficulty);
        json.put("botCount", this.botCount);
        return json;
    }

    public int getRoomId() { return id; }
    public int getCurrentPlayers() { return currentPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public boolean isInGame() { return inGame; }
    public boolean isFull() { return currentPlayers >= maxPlayers; }
    public String getRoomName() { return roomName; }
    public String getMapSize() { return mapSize; }
    public String getDifficulty() { return difficulty; }
    public int getBotCount() { return botCount; }
}
