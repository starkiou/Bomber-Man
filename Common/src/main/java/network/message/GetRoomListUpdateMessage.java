package network.message;

import org.json.JSONObject;

public class GetRoomListUpdateMessage implements Message {
	
	

	@Override
	public JSONObject getData() {
		return new JSONObject();
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.GET_ROOM_LIST_UPDATE;
	}

}
