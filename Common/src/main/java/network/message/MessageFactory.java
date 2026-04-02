package network.message;

import org.json.JSONObject;

public class MessageFactory {
	public Message make(MessageType type, JSONObject jsonObject) {
		switch(type) {
		case BOMB_PLACE:
			//TODO
			return null;
		case CHAT:
			return new ChatMessage(jsonObject);
		case CONNECTION:
			return new ConnectionMessage(jsonObject);
			
		case GAME_STATE:
			break;
		case GET_ROOM_LIST_UPDATE:
			return new GetRoomListUpdateMessage();
		case MOVE:
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
		case ACCEPTED_ROOM_JOIN:
			return new RoomAcceptedJoinMessage(jsonObject);
		case ROOM_STATUS_UPDATE:
			return new RoomStatusMessage(jsonObject);
		case REFUSED_ROOM_CREATION:
			return new RoomRefusedCreationMessage(jsonObject);
		case ACCEPTED_ROOM_CREATION:

			return new RoomAcceptedCreationMessage(jsonObject);
		case CLIENT_READY_CORRECT_UPDATE:
			return new ClientCorrectReadyUpdateMessage(jsonObject);
		case READY_CLIENT:
			return new ClientReadyMessage(jsonObject);
		case ROOM_CORRECT_QUIT:
			return new RoomCorrectQuitMessage(jsonObject);
		case ROOM_QUIT:
			return new RoomQuitMessage(jsonObject);

			
		case LAUNCH_GAME:
			return new LaunchGameMessage(jsonObject);

		default:
			break;
		
		

		
		
		
		}
		
		
		return null;
		
	}
}
