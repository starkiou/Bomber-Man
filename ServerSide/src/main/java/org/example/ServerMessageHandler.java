package org.example;

import java.util.ArrayList;
import java.util.List;

import network.message.ConnectionMessage;
import network.message.RoomCreationMessage;
import network.message.RoomListUpdateMessage;
import network.message.Message;
import network.message.RoomInfoDTO;

public class ServerMessageHandler {
	private ServerManager serverManager;
	
	public synchronized void handle(ClientHandler sender, Message message) {
		switch(message.getMessageType()) {
		case BOMB_PLACE:
			break;
		case CHAT:
			break;
		case CONNECTION:
			ConnectionMessage messageConnection = (ConnectionMessage) message;
			sender.setPseudo(messageConnection.getPseudo());
			break;
		case GET_ROOM_LIST_UPDATE:
			List<RoomInfoDTO> roomInfos = new ArrayList<>();
		    for (RoomThread room : serverManager.getRooms()) {
		        roomInfos.add(new RoomInfoDTO(
		            room.getRoomId(),
		            room.getPlayerCount(),
		            room.getMaxPlayers(),
		            room.isInGame()
		        ));
		    }
		    sender.addMessage(new RoomListUpdateMessage(roomInfos));
		    break;
		case GAME_STATE:
			break;
		case ROOM_CREATION:
			RoomCreationMessage messageLobbyCreation = (RoomCreationMessage) message;
			this.serverManager.createRoomAsClient(sender, messageLobbyCreation.getMaxPlayer());
			
			break;
		case ROOM_LIST_UPDATE:
			break;
		case MOVE:
			break;
		case READY_UPDATE:
			break;
		case GET_ROOM_UPDATE:
			break;
		default:
			break;
		
		}
	}
	
	public ServerMessageHandler(ServerManager serverManager) {
		this.serverManager = serverManager;
	}

}
