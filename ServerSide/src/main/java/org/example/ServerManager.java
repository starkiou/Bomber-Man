package org.example;


import java.io.IOException;
import model.logger.LogManager;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import network.message.Message;
import network.message.MessageFactory;
import network.message.MessageType;
import network.message.RoomInfoDTO;

public class ServerManager {

	private List<ClientHandler> listClient = Collections.synchronizedList(new ArrayList<ClientHandler>());
	private Map<Integer, RoomThread> roomMap = Collections.synchronizedMap(new HashMap<>());
	public MessageFactory factory = new MessageFactory();
	
	private static ServerManager instance;
	

	private ServerClientMessageHandler serverMessageHandler;
	private AcceptConnectionThread acceptConnectionThread;
	private int nextRoomId = 1;
	private int nextClientId = 1;
	
    private ServerManager(int port) {
    	instance = this;
    	System.out.println("Lancement d'un serveur sur le port : "+port);
    	LogManager.getInstance().info("Démarrage du serveur sur le port "+port);
    	acceptConnectionThread = new AcceptConnectionThread(this, port);
		acceptConnectionThread.start();
		this.setServerMessageHandler(new ServerClientMessageHandler());
		System.out.println("Serveur lancé sur le port : "+port);
    	

    }
    
    public static synchronized ServerManager init(int port) {
        if (instance == null) {
            instance = new ServerManager(port);
        }
        return instance;
    }
    
    
    
    public static ServerManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ServerManager uninitialized !");
        }
        return instance;

    }

    public synchronized void addClientWithSocket(Socket socket) {
    	ClientHandler clientHandler = new ClientHandler(socket, nextClientId);
    	nextClientId++;
    	listClient.add(clientHandler);
    	clientHandler.start();
    }

    public synchronized void createRoomAsClient(ClientHandler client, int nbMaxPlayers, String name, String mapSize, String difficulty, int botCount) {
    	RoomThread newRoom = new RoomThread(nbMaxPlayers, nextRoomId, name, mapSize, difficulty, botCount);
    	this.roomMap.put(Integer.valueOf(nextRoomId), newRoom);
    	this.nextRoomId++;
    	newRoom.start();
    	newRoom.addClient(client);
    	client.setRoom(newRoom);
    	this.updateRoomsListOfClients();
    }

    public synchronized Collection<RoomThread> getRooms() {
        return roomMap.values();
    }

    public RoomThread getRoomsById(int id) {
        return roomMap.get(id);
    }

	public ServerClientMessageHandler getServerMessageHandler() {
		return serverMessageHandler;
	}

	public void setServerMessageHandler(ServerClientMessageHandler serverMessageHandler) {
		this.serverMessageHandler = serverMessageHandler;
	}

	public void removeClient(ClientHandler client) {
		this.listClient.remove(client);
	}

	public void broadCastToAllClient(Message message) {
		synchronized (listClient) {
			for(ClientHandler client : listClient) {
				client.addMessage(message);
			}
		}
	}

	/** Construit le JSON d'une room et l'envoie à tous les clients */
	private JSONObject buildRoomJson(RoomThread room) {
		JSONObject roomJson = new JSONObject();
		roomJson.put("id", room.id);
		roomJson.put("currentPlayers", room.getPlayerCount());
		roomJson.put("maxPlayers", room.getMaxPlayers());
		roomJson.put("inGame", room.isInGame());
		roomJson.put("roomName", room.getRoomName());
		roomJson.put("mapSize", room.getMapSize());
		roomJson.put("difficulty", room.getDifficulty());
		roomJson.put("botCount", room.getBotCount());
		return roomJson;
	}

	public void updateRoomsListOfClients() {
		JSONObject roomInfosJson = new JSONObject();
		JSONArray array = new JSONArray();
		synchronized (roomMap) {
			for (RoomThread room : roomMap.values()) {
				array.put(buildRoomJson(room));
			}
		}
		roomInfosJson.put("rooms", array);
		this.broadCastToAllClient(factory.make(MessageType.ROOM_LIST_UPDATE, roomInfosJson));
	}

	/** Envoie la liste des rooms à un seul client (réponse à GET_ROOM_LIST_UPDATE) */
	public void sendRoomsListToClient(ClientHandler client) {
		JSONObject roomInfosJson = new JSONObject();
		JSONArray array = new JSONArray();
		synchronized (roomMap) {
			for (RoomThread room : roomMap.values()) {
				array.put(buildRoomJson(room));
			}
		}
		roomInfosJson.put("rooms", array);
		client.addMessage(factory.make(MessageType.ROOM_LIST_UPDATE, roomInfosJson));
	}

	public void removeRoom(RoomThread room) {
		synchronized (roomMap) {
			roomMap.values().remove(room);
		}
	}
	
	public static void reset() {
	    instance = null;
	}
}
