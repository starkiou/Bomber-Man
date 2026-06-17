package network.message;

import org.json.JSONObject;

public class RoomCorrectQuitMessage extends AbstractEmptyMessage {

	public RoomCorrectQuitMessage() {
	}

	public RoomCorrectQuitMessage(JSONObject dataJson) {
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.ROOM_CORRECT_QUIT;
	}
}
