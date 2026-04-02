package org.example;

import org.json.JSONObject;

import network.message.ClientCorrectReadyUpdateMessage;
import network.message.ClientReadyMessage;
import network.message.ConnectionMessage;
import network.message.LaunchGameMessage;
import network.message.Message;
import network.message.MessageFactory;
import network.message.MessageType;
import network.message.RoomAcceptedCreationMessage;
import network.message.RoomCorrectQuitMessage;
import network.message.RoomInfoDTO;
import network.message.RoomAcceptedJoinMessage;
import network.message.RoomCreationMessage;
import network.message.RoomJoiningMessage;
import network.message.RoomRefusedCreationMessage;

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
				serverManager.sendRoomsListToClient(sender);
				break;
			case GAME_STATE:
				break;
			case ROOM_CREATION:
				RoomCreationMessage msg = (RoomCreationMessage) message;
				if(sender.getRoom() != null) {
					sender.addMessage(new RoomRefusedCreationMessage("Client already in a room."));
				} else {
					this.serverManager.createRoomAsClient(sender, msg.getMaxPlayer(), msg.getName(), msg.getMapSize(), msg.getDifficulty(), msg.getBotCount());
					sender.addMessage(new RoomAcceptedCreationMessage(sender.getRoom().getRoomId()));
				}
				break;
			case ROOM_LIST_UPDATE:
				break;
			case MOVE:
				break;

			case LAUNCH_GAME:
				if(sender.getRoom() != null) {
					// BROADCAST à tous les membres de la room
					sender.getRoom().broadcast(new LaunchGameMessage());
					sender.getRoom().setInGame(true);
					sender.getRoom().stopWaiting();
					this.serverManager.updateRoomsListOfClients();
				}
				break;
			case ROOM_JOIN:
				this.roomJoin(sender, message);
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
		if(room == null || room.isInGame() || room.isFull()) {
			JSONObject json = new JSONObject();
			json.put("roomId", roomId);
			sender.addMessage(factory.make(MessageType.REFUSED_ROOM_JOIN, json));
		} else {
			room.addClient(sender);
			sender.setRoom(room);
			// FIX: envoyer confirmation au joueur qui rejoint
			sender.addMessage(new RoomAcceptedJoinMessage(roomId));
			this.serverManager.updateRoomsListOfClients();
		}
	}

	
	public ServerClientMessageHandler() {
		this.serverManager = ServerManager.getInstance();

	}
}
