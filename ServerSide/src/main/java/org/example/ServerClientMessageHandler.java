package org.example;

import network.message.ClientCorrectReadyUpdateMessage;
import network.message.ClientReadyMessage;
import network.message.ConnectionMessage;
import network.message.Message;
import network.message.RoomAcceptedCreationMessage;
import network.message.RoomCorrectQuitMessage;
import network.message.RoomAcceptedJoinMessage;
import network.message.RoomCreationMessage;
import network.message.RoomJoiningMessage;
import network.message.RoomRefusedCreationMessage;
import network.message.RoomRefusedJoinMessage;

public class ServerClientMessageHandler {
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
				// relay action à tous les joueurs de la room
				if (sender.getRoom() != null && sender.getRoom().isInGame()) {
					sender.getRoom().broadcast(message);
				}
				break;

			case LAUNCH_GAME:
				if (sender.getRoom() != null) {
					// vérif que tout le monde est prêt, puis lancement centralisé
					if (!sender.getRoom().isReadyToLaunch()) break;
					sender.getRoom().launchGame();
				}
				break;
			case ROOM_JOIN:
				this.roomJoin(sender, message);
				break;
			case ROOM_UPDATE:
				break;
			case ROOM_QUIT:
				if (sender.getRoom() != null) {
					sender.getRoom().removeClient(sender);
					sender.setRoom(null);
					sender.addMessage(new RoomCorrectQuitMessage());
				}
				break;
			case READY_CLIENT:
				ClientReadyMessage clientReadyMessage = (ClientReadyMessage) message;
				sender.setReady(clientReadyMessage.isReady());
				sender.addMessage(new ClientCorrectReadyUpdateMessage());
				break;
			default:
				break;
		}
	}

	private synchronized void roomJoin(ClientHandler sender, Message message) {
		RoomJoiningMessage messageRoomJoining = (RoomJoiningMessage) message;
		int roomId = messageRoomJoining.getIdRoom();
		RoomThread room = this.serverManager.getRoomsById(roomId);
		if(room == null || room.isInGame() || room.isFull()) {
			// On construit le message directement : la fabrique attend la clé
			// "idRoom", pas "roomId" (sinon JSONException côté client).
			sender.addMessage(new RoomRefusedJoinMessage(roomId));
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
