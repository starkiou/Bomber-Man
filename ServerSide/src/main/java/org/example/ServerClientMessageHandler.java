package org.example;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import network.message.ClientCorrectReadyUpdateMessage;
import network.message.ClientReadyMessage;
import network.message.ConnectionMessage;
import network.message.RoomCreationMessage;
import network.message.RoomListUpdateMessage;
import network.message.RoomRefusedCreationMessage;
import network.message.Message;
import network.message.MessageFactory;
import network.message.MessageType;
import network.message.RoomAcceptedCreationMessage;
import network.message.RoomCorrectQuitMessage;
import network.message.RoomInfoDTO;
import network.message.RoomJoiningMessage;

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
				sender.setSkinId(messageConnection.getSkinId());
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
				if(sender.getRoom()!=null) {
					sender.addMessage(new RoomRefusedCreationMessage("Client already in a room."));
				} else {
					this.serverManager.createRoomAsClient(sender, messageLobbyCreation.getMaxPlayer(), messageLobbyCreation.getName());
					sender.addMessage(new RoomAcceptedCreationMessage(sender.getRoom().getRoomId()));
				}
				
				break;
			case ROOM_LIST_UPDATE:
				break;
			case MOVE:
				break;
			case ROOM_JOIN:
				this.roomJoin(sender,message);
				break;
			case ROOM_UPDATE:
				break;
			case ROOM_QUIT:
				sender.getRoom().removeClient(sender);
				sender.addMessage(new RoomCorrectQuitMessage());
				break;
			case READY_CLIENT:
				ClientReadyMessage clientReadyMessage = (ClientReadyMessage) message;
				sender.setReady(clientReadyMessage.isReady());
				System.out.println(sender.getPseudo()+" PRÊT");
				sender.addMessage(new ClientCorrectReadyUpdateMessage());
			default:
				break;
				
			
			}
	}
	
	private synchronized void roomJoin(ClientHandler sender, Message message) {
		RoomJoiningMessage messageRoomJoining = (RoomJoiningMessage) message;
		int roomId = messageRoomJoining.getIdRoom();
		RoomThread room = this.serverManager.getRoomsById(roomId);
		if(room.isInGame() || room.isFull()) {
			JSONObject json = new JSONObject();
			json.put("roomId", room.getRoomId());
			sender.addMessage(factory.make(MessageType.REFUSED_ROOM_JOIN, json));
		} else {
			room.addClient(sender);
			sender.setRoom(room);
	    	this.serverManager.updateRoomsListOfClients();

		}
	}
	
	public ServerClientMessageHandler(ServerManager serverManager) {
		this.serverManager = serverManager;
	}

}
