package network.message;

import org.json.JSONObject;

public class RoomQuitMessage implements Message {
	
	
	public RoomQuitMessage() {
		
	}
	
	public RoomQuitMessage(JSONObject dataJson) {

	}
	
	@Override
	public JSONObject getData() {
	    return new JSONObject();
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.ROOM_QUIT;
	}

}
