package org.example;

import network.message.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoomThread extends Thread {
	private List<ClientHandler> listClient = Collections.synchronizedList(new ArrayList<ClientHandler>());

	public MessageFactory factory = new MessageFactory();
	
	public boolean isWaitingToLaunch = true;
	
	public final int id;
	
	private static final int timeBeforeLaunchWhenReady = 5000;
	
	private boolean readyToLaunch = false;
	
	private boolean inGame = false;
	
	private int maxPlayer;
	
	private String roomName;
	
	
	
	public RoomThread(int maxPlayer, int id, String roomName) {
		this.maxPlayer=maxPlayer;
		this.id = id;
		this.roomName=roomName;
	}
	
	public int getRoomId() {
		return this.id;
	}
	
	
	
	
	
	public void addClient(ClientHandler clientHandler) {
		listClient.add(clientHandler);
	}
	
	

	@Override
	public void run() {
		while(isWaitingToLaunch) {
			this.updateReadyToLaunch();

			JSONObject json = new JSONObject();
			json.put("roomId", this.getRoomId());
			json.put("isWaitingToLaunch", this.isWaitingToLaunch);
	        JSONArray array = new JSONArray();
	        for(ClientHandler client : listClient) {
	        	ClientInfoDTO clientInfoDTO = new ClientInfoDTO(client.getClientId(), client.isReady(), client.getPseudo());
	        	array.put(clientInfoDTO);
	        }
	        json.put("clients", array);
			
			RoomStatusMessage roomStatusMessage = (RoomStatusMessage) factory.make(MessageType.ROOM_STATUS_UPDATE, json);
			for(ClientHandler client : this.listClient) {
				client.addMessage(roomStatusMessage);
				
			}

			try {
				sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	public void stopWaiting() {
		isWaitingToLaunch = false;
	}
	
	private void updateReadyToLaunch() {
		for(ClientHandler client : listClient) {		
			this.readyToLaunch=true;
			if(!client.isReady()) {
				readyToLaunch = false;
			}
		}
	}
	
	public synchronized void broadcast(Message message) {
		for(ClientHandler client : listClient) {
			client.addMessage(message);
		}
	}



	public boolean isInGame() {
		return inGame;
	}



	public void setInGame(boolean inGame) {
		this.inGame = inGame;
	}



	public int getMaxPlayers() {
		return maxPlayer;
	}



	public void setMaxPlayer(int maxPlayer) {
		this.maxPlayer = maxPlayer;
	}



	public int getPlayerCount() {
		return this.listClient.size();
	}
	
	public String getRoomName() {
		return roomName;
	}
	
	public void setRoomName(String roomName) {
		this.roomName=roomName;
	}
	
	public boolean isFull() {
		return (this.getPlayerCount()>=this.maxPlayer);
	}
	
	
	
}
