package network.message;

import org.json.JSONObject;

public class RoomAcceptedCreationMessage extends AbstractRoomIdMessage {

	public RoomAcceptedCreationMessage(int idRoom) {
		super(idRoom);
	}

	public RoomAcceptedCreationMessage(JSONObject dataJson) {
		super(dataJson);
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.ACCEPTED_ROOM_CREATION;
	}
}
