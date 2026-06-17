package network.message;

import org.json.JSONObject;

public class RoomJoiningMessage extends AbstractRoomIdMessage {

	public RoomJoiningMessage(int idRoom) {
		super(idRoom);
	}

	public RoomJoiningMessage(JSONObject dataJson) {
		super(dataJson);
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.ROOM_JOIN;
	}
}
