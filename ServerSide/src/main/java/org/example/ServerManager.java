package org.example;

import model.logger.LogManager;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerManager {
	private final List<ClientHandler> listClient = Collections.synchronizedList(new ArrayList<>());

    public ServerManager(int port) {
    	AcceptConnectionThread acceptConnectionThread = new AcceptConnectionThread(this, port);
    	acceptConnectionThread.start();
    }

    public synchronized void addClientWithSocket(Socket socket) {
    	ClientHandler clientHandler = new ClientHandler(socket);
    	listClient.add(clientHandler);
    	clientHandler.start();
    	LogManager.getInstance().info("Client connecté : " + socket.getInetAddress() + ":" + socket.getPort());
    }
}
