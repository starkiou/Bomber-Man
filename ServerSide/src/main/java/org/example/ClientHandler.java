package org.example;

import model.logger.LogManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler extends Thread {
	private Socket socket;
	private BufferedReader in;
    private PrintWriter out;

	private boolean isReady = false; //maybe need to be moved into roomThread


	public ClientHandler(Socket socket){
		this.socket=socket;
		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException e) {
			LogManager.getInstance().error("Erreur lors de l'initialisation du client " + socket.getInetAddress() + " : " + e.getMessage());
		}

	}
	
	public synchronized void send(String message) {
		//TODO
	}
	
	public void run() {
		
	}
	

	public boolean isReady() {
		return isReady;
	}

	public void setReady(boolean isReady) {
		this.isReady = isReady;
	}


}
