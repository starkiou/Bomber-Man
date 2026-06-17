package network.message;

import org.json.JSONObject;

public class RoomRefusedJoinMessage extends AbstractRoomIdMessage {

	public RoomRefusedJoinMessage(int idRoom) {
		super(idRoom);
	}

	public RoomRefusedJoinMessage(JSONObject dataJson) {
		super(dataJson);
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.REFUSED_ROOM_JOIN;
	}
}
