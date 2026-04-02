package org.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import network.message.ClientInfoDTO;
import network.message.Message;
import network.message.MessageFactory;
import network.message.MessageType;
import network.message.RoomStatusMessage;

public class RoomThread extends Thread {
	private List<ClientHandler> listClient = Collections.synchronizedList(new ArrayList<ClientHandler>());

	public MessageFactory factory = new MessageFactory();
	
	public boolean isWaiting = true;
	
	public final int id;
	
	private long readyStartTime = -1;
	private static final int TIME_BEFORE_LAUNCH = 10000;
	
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
	    while (isWaiting) {
	        this.updateReadyToLaunch();
	        
	        if (listClient.isEmpty()) {
	            readyToLaunch = false;
	            readyStartTime = -1;
	            
	            return;
	        }

	        if (readyToLaunch && readyStartTime != -1) {
	            long elapsed = System.currentTimeMillis() - readyStartTime;

	            if (elapsed >= TIME_BEFORE_LAUNCH) {
	                System.out.println("Lancement de la partie !");
	                this.stopWaiting();
	                this.setInGame(true);
	                break;
	            }
	        }

	        JSONObject json = new JSONObject();
	        json.put("roomId", this.getRoomId());
	        json.put("isWaiting", this.isWaiting);

	        if (readyStartTime != -1) {
	            long remaining = TIME_BEFORE_LAUNCH - (System.currentTimeMillis() - readyStartTime);
	            json.put("countdown", Math.max(0, remaining));
	        } else {
	        	json.put("countdown", -1);
	        }

	        JSONArray array = new JSONArray();
	        synchronized (listClient) {
	            for (ClientHandler client : listClient) {
	                ClientInfoDTO dto = new ClientInfoDTO(
	                        client.getClientId(),
	                        client.isReady(),
	                        client.getPseudo()
	                );
	                array.put(dto);
	            }
	        }

	        json.put("clients", array);

	        RoomStatusMessage roomStatusMessage = (RoomStatusMessage) factory.make(MessageType.ROOM_STATUS_UPDATE, json);

	        synchronized (listClient) {
	            for (ClientHandler client : listClient) {
	                client.addMessage(roomStatusMessage);
	            }
	        }

	        try {
	            sleep(200);
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        }
	    }
	}
	
	public void stopWaiting() {
		isWaiting = false;
	}
	
	private void updateReadyToLaunch() {
	    readyToLaunch = true;

	    for (ClientHandler client : listClient) {
	        if (!client.isReady()) {
	            readyToLaunch = false;
	            break;
	        }
	    }

	    if (readyToLaunch) {
	        if (readyStartTime == -1) {
	            readyStartTime = System.currentTimeMillis();
	        }
	    } else {
	        readyStartTime = -1; // reset si quelqu’un n’est plus prêt
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
	
	public void removeClient(ClientHandler client) {
		this.listClient.remove(client);
	}
	
	
	
}
