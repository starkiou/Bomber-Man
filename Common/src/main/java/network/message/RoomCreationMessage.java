package network.message;

import org.json.JSONObject;

public class RoomCreationMessage implements Message {

	private int maxPlayer;
	private String name;
	private String mapSize;
	private String difficulty;
	private int botCount;

	public RoomCreationMessage(int maxPlayer, String name, String mapSize, String difficulty, int botCount) {
		this.maxPlayer = maxPlayer;
		this.name = name;
		this.mapSize = mapSize;
		this.difficulty = difficulty;
		this.botCount = botCount;
	}

	/** Désérialisation depuis JSON reçu par le serveur */
	public RoomCreationMessage(JSONObject dataJson) {
		this.maxPlayer = dataJson.getInt("maxPlayer");
		this.name = dataJson.getString("name");
		this.mapSize = dataJson.getString("mapSize");
		this.difficulty = dataJson.getString("difficulty");
		this.botCount = dataJson.getInt("botCount");
	}

	@Override
	public JSONObject getData() {
		JSONObject obj = new JSONObject();
		obj.put("maxPlayer", this.maxPlayer);
		obj.put("name", this.name);
		obj.put("mapSize", this.mapSize);
		obj.put("difficulty", this.difficulty);
		obj.put("botCount", this.botCount);
		return obj;
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.ROOM_CREATION;
	}

	public int getMaxPlayer() { return maxPlayer; }
	public void setMaxPlayer(int maxPlayer) { this.maxPlayer = maxPlayer; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public String getMapSize() { return mapSize; }
	public String getDifficulty() { return difficulty; }
	public int getBotCount() { return botCount; }
}
