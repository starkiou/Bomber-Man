package org.example;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerManager {
	private List<ClientHandler> listClient = Collections.synchronizedList(new ArrayList<ClientHandler>());
	private AcceptConnectionThread acceptConnectionThread;
	
	
	
    public ServerManager(int port) {
    	acceptConnectionThread = new AcceptConnectionThread(this, port);
    	acceptConnectionThread.start();
        
    }
    
    public synchronized void addClientWithSocket(Socket socket) {
    	ClientHandler clientHandler = new ClientHandler(socket);
    	listClient.add(clientHandler);
    	clientHandler.start();
    }
}
