package org.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoomThread extends Thread {
	private List<ClientHandler> listClient = Collections.synchronizedList(new ArrayList<ClientHandler>());

	public boolean isLooping = true;
	
	private static final int waitingPlayerTime = 1000;
	
	private boolean readyToLaunch = false;
	
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
	
	private void broadcast() {
		for(ClientHandler client : listClient) {
//			client.send();
		}
	}
	
	
	
}
