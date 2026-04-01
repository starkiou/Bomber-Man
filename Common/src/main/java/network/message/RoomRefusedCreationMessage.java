package network.message;

import org.json.JSONObject;

public class RoomRefusedCreationMessage implements Message {
	
	private String cause;
	
	public RoomRefusedCreationMessage(String cause) {
		this.cause=cause;
	}
	
	public RoomRefusedCreationMessage(JSONObject dataJson) {
		this.cause = dataJson.getString("cause");
	}
	
	@Override
	public JSONObject getData() {
		JSONObject obj = new JSONObject();
	    obj.put("cause", this.cause);
	    return obj;
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.REFUSED_ROOM_CREATION;
	}

	public String getCause() {
		return cause;
	}

	public void setCause(String cause) {
		this.cause = cause;
	}

}
