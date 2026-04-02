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

	
	private ServerClientMessageHandler serverMessageHandler;
	
	private AcceptConnectionThread acceptConnectionThread;
	
	private int nextRoomId = 1;
	
	private int nextClientId = 1;
	
	
	
    public ServerManager(int port) {
    	System.out.println("Lancement d'un serveur sur le port : "+port);
    	acceptConnectionThread = new AcceptConnectionThread(this, port);
		acceptConnectionThread.start();
		this.setServerMessageHandler(new ServerClientMessageHandler(this));
		System.out.println("Serveur lancé sur le port : "+port);
    	

    }
    
    public synchronized void addClientWithSocket(Socket socket) {
    	ClientHandler clientHandler = new ClientHandler(socket, this, nextClientId);
    	nextClientId++;
    	listClient.add(clientHandler);
    	clientHandler.start();
    }
    
    public synchronized void createRoomAsClient(ClientHandler client, int nbMaxPlayers, String name) {
    	RoomThread newRoom = new RoomThread(nbMaxPlayers, nextRoomId, name);
    	this.roomMap.put(Integer.valueOf(nextRoomId), newRoom);
    	this.nextRoomId++;
    	newRoom.start();
    	newRoom.addClient(client);
    	client.setRoom(newRoom);
    	this.updateRoomsListOfClients();
    }
    
    public synchronized void createRoom(int nbMaxPlayers, String name) {
    	RoomThread newRoom = new RoomThread(nbMaxPlayers, nextRoomId, name);
    	this.roomMap.put(Integer.valueOf(nextRoomId), newRoom);
    	this.nextRoomId++;
    	newRoom.start();
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
		for(ClientHandler client : listClient) {
			client.addMessage(message);
		}
	}
	
	public void updateRoomsListOfClients() {
		List<RoomInfoDTO> roomInfos = new ArrayList<>();
		JSONObject roomInfosJson = new JSONObject();
        JSONArray array = new JSONArray();

	    for (RoomThread room : this.getRooms()) {
	    	JSONObject roomJson = new JSONObject();
	    	roomJson.put("id", room.id);
	    	roomJson.put("currentPlayers", room.getPlayerCount());
	    	roomJson.put("maxPlayers", room.getMaxPlayers());
	    	roomJson.put("inGame", room.isInGame());
	    	roomJson.put("roomName", room.getRoomName());
	    	array.put(roomJson);
	    }
	    
	    
	    roomInfosJson.put("rooms", array);
	    
	    this.broadCastToAllClient(factory.make(MessageType.ROOM_LIST_UPDATE, roomInfosJson));
	}
}
