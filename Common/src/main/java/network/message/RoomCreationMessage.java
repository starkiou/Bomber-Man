package network.message;

import org.json.JSONObject;

public class RoomCreationMessage implements Message {
	
	private int maxPlayer;
	private String name;
	
	public RoomCreationMessage(int maxPlayer, String name) {
		this.maxPlayer=maxPlayer;
		this.name=name;
	}
	
	public RoomCreationMessage(JSONObject dataJson) {
		this.maxPlayer = dataJson.getInt("maxPlayer");
		this.name = dataJson.getString("name");

	}
	
	@Override
	public JSONObject getData() {
		JSONObject obj = new JSONObject();
	    obj.put("maxPlayer", this.maxPlayer);
	    obj.put("name", this.name);
	    return obj;
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.ROOM_CREATION;
	}

	public int getMaxPlayer() {
		return maxPlayer;
	}

	public void setMaxPlayer(int maxPlayer) {
		this.maxPlayer = maxPlayer;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
