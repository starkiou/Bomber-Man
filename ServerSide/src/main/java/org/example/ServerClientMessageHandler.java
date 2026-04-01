package org.example;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import network.message.ConnectionMessage;
import network.message.RoomCreationMessage;
import network.message.RoomListUpdateMessage;
import network.message.Message;
import network.message.MessageFactory;
import network.message.MessageType;
import network.message.RoomInfoDTO;

public class ServerClientMessageHandler {
	private ServerManager serverManager;
	
	public MessageFactory factory = new MessageFactory();
	
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
				JSONObject roomInfosJson = new JSONObject();
		        JSONArray array = new JSONArray();

			    for (RoomThread room : serverManager.getRooms()) {
			    	JSONObject roomJson = new JSONObject();
			    	roomJson.put("id", room.id);
			    	roomJson.put("currentPlayers", room.getPlayerCount());
			    	roomJson.put("maxPlayers", room.getMaxPlayers());
			    	roomJson.put("inGame", room.isInGame());
			    	roomJson.put("roomName", room.getRoomName());
			    	array.put(roomJson);
			    }
			    
			    
			    roomInfosJson.put("rooms", array);
			    
			    sender.addMessage(factory.make(MessageType.ROOM_LIST_UPDATE, roomInfosJson));
			    break;
			case GAME_STATE:
				break;
			case ROOM_CREATION:
				RoomCreationMessage messageLobbyCreation = (RoomCreationMessage) message;
				this.serverManager.createRoomAsClient(sender, messageLobbyCreation.getMaxPlayer(), messageLobbyCreation.getName());
				
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
	
	public ServerClientMessageHandler(ServerManager serverManager) {
		this.serverManager = serverManager;
	}

}
