package org.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import network.message.Message;

public class RoomThread extends Thread {
	private List<ClientHandler> listClient = Collections.synchronizedList(new ArrayList<ClientHandler>());

	public boolean isLooping = true;
	
	public final int id;
	
	private static final int waitingPlayerTime = 1000;
	
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
		while(isLooping) {
			if(!readyToLaunch) {
				this.updateReadyToLaunch();
			}
			
		}
	}
	
	public void stopLooping() {
		isLooping = false;
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
	
	
	
}
