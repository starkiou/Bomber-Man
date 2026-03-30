package org.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoomThread extends Thread {
	private List<ClientHandler> listClient = Collections.synchronizedList(new ArrayList<ClientHandler>());

	public void addClient(ClientHandler clientHandler) {
		listClient.add(clientHandler);
	}
	
}
