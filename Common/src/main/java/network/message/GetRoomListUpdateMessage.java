package network.message;

public class GetRoomListUpdateMessage extends AbstractEmptyMessage {

	@Override
	public MessageType getMessageType() {
		return MessageType.GET_ROOM_LIST_UPDATE;
	}
}
