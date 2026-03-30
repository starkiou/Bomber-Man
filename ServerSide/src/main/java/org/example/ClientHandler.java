package org.example;

import java.net.Socket;

public class ClientHandler extends Thread {
	private Socket socket;
	
	ClientHandler(Socket socket){
		this.socket=socket;
	}
	
	public void run() {
		
	}

}
