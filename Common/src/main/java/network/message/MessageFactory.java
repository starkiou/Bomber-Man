package network.message;

import org.json.JSONObject;

public class MessageFactory {
	public Message make(MessageType type, JSONObject jsonObject) {
		switch(type) {
		case BOMB_PLACE:
			break;
		case CHAT:
			break;
		case CONNECTION:
			return new ConnectionMessage(jsonObject);
			
		case GAME_STATE:
			break;
		case GET_ROOM_LIST_UPDATE:
			return new GetRoomListUpdateMessage();

			
		case GET_ROOM_UPDATE:
			
			break;
		case MOVE:
			break;
		case READY_UPDATE:
			break;
		case ROOM_CREATION:
			return new RoomCreationMessage(jsonObject);

		case ROOM_JOIN:
			return new RoomJoiningMessage(jsonObject);

		case ROOM_LIST_UPDATE:
			return new RoomListUpdateMessage(jsonObject);

		case ROOM_UPDATE:
			break;
		case REFUSED_ROOM_JOIN:
			return new RoomRefusedJoinMessage(jsonObject);

		default:
			break;
		
		
		}
		
		
		return null;
		
	}
}
