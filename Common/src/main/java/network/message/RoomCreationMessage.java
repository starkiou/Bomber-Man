package network.message;

import org.json.JSONObject;

public class RoomCreationMessage implements Message {
	
	private int maxPlayer;
	
	public RoomCreationMessage(int maxPlayer) {
		this.maxPlayer=maxPlayer;
	}
	
	public RoomCreationMessage(JSONObject dataJson) {
		this.maxPlayer = dataJson.getInt("maxPlayer");
	}
	
	@Override
	public JSONObject getData() {
		JSONObject obj = new JSONObject();
	    obj.put("maxPlayer", this.maxPlayer);
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

}
