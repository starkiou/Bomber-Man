package network.message;

import org.json.JSONObject;

public class RoomCorrectQuitMessage implements Message {
	
	
	public RoomCorrectQuitMessage() {
		
	}
	
	public RoomCorrectQuitMessage(JSONObject dataJson) {

	}
	
	@Override
	public JSONObject getData() {
	    return new JSONObject();
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.ROOM_CORRECT_QUIT;
	}

}
