package org.example;

import java.util.List;
import org.json.JSONObject;

import model.maze.CellType;
import model.maze.MazeFactory;
import network.message.ClientCorrectReadyUpdateMessage;
import network.message.ClientInfoDTO;
import network.message.ClientReadyMessage;
import network.message.ConnectionMessage;
import network.message.LaunchGameMessage;
import network.message.Message;
import network.message.MessageFactory;
import network.message.MessageType;
import network.message.PlayActionMessage;
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
				// relay action à tous les joueurs de la room
				if (sender.getRoom() != null && sender.getRoom().isInGame()) {
					sender.getRoom().broadcast(message);
				}
				break;

			case LAUNCH_GAME:
				if (sender.getRoom() != null) {
					// vérif que tout le monde est prêt
					if (!sender.getRoom().isReadyToLaunch()) break;
					// génère la grille côté serveur
					CellType[][] grid = MazeFactory.createMaze(MazeFactory.Algorithm.EXHAUSTIVE, 15, 11);
					List<ClientInfoDTO> playerList = sender.getRoom().getClientInfoList();
					int bots = sender.getRoom().getBotCount();
					// Envoie un message personnalisé à chaque client avec son propre playerId
					List<ClientHandler> clients = sender.getRoom().getListClient();
					for (int i = 0; i < clients.size(); i++) {
						int pid = i + 1;
						clients.get(i).addMessage(new LaunchGameMessage(playerList, bots, grid, pid));
					}
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
				if (sender.getRoom() != null) {
					sender.getRoom().removeClient(sender);
					sender.setRoom(null);
					sender.addMessage(new RoomCorrectQuitMessage());
				}
				break;
			case READY_CLIENT:
				ClientReadyMessage clientReadyMessage = (ClientReadyMessage) message;
				sender.setReady(clientReadyMessage.isReady());
				System.out.println(sender.getPseudo()+" PRÊT");
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
