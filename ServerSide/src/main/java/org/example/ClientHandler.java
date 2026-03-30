package org.example;

import java.net.Socket;

public class ClientHandler extends Thread {
	private Socket socket;
	
	ClientHandler(Socket socket){
		this.socket=socket;
	}
	
	public synchronized void send(String message) {
		//TODO
	}
	
	public void run() {
		
	}

}
