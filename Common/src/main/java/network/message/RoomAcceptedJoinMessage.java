package network.message;

import org.json.JSONObject;

public class RoomAcceptedJoinMessage extends AbstractRoomIdMessage {

	public RoomAcceptedJoinMessage(int idRoom) {
		super(idRoom);
	}

	public RoomAcceptedJoinMessage(JSONObject dataJson) {
		super(dataJson);
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.ACCEPTED_ROOM_JOIN;
	}
}
