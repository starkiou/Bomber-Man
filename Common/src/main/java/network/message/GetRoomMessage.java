package network.message;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class GetRoomMessage implements Message {
	
	

	@Override
	public JSONObject getData() {
		return new JSONObject();
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.GET_ROOM_LIST_UPDATE;
	}

}
