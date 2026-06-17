package network.message;

import org.json.JSONObject;

public class RoomQuitMessage extends AbstractEmptyMessage {

	public RoomQuitMessage() {
	}

	public RoomQuitMessage(JSONObject dataJson) {
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.ROOM_QUIT;
	}
}
