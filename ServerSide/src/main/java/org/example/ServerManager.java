package org.example;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerManager {
	private List<ClientHandler> listClient = Collections.synchronizedList(new ArrayList<ClientHandler>());
	private AcceptConnectionThread acceptConnectionThread;
	
    public ServerManager() {
    	acceptConnectionThread = new AcceptConnectionThread(this, 0);
        
    }
    
    public synchronized void addClientWithSocket(Socket socket) {
    	listClient.add(new ClientHandler(socket));
    }
}
