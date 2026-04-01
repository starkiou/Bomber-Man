package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AcceptConnectionThread extends Thread {


	private boolean mustContinueAccept = true;
	private ServerManager serverMain; 
	private ServerSocket serverSocket;
	
	
	public AcceptConnectionThread(ServerManager serverMain, ServerSocket serverSocket) {
		this.serverSocket=serverSocket;
		this.serverMain=serverMain;
	}
	
	public AcceptConnectionThread(ServerManager serverMain, int port) throws IOException {
		
		this.serverSocket=new ServerSocket(port);
	    System.out.println("Socket ouvert sur le port : "+port);
		
		this.serverMain=serverMain;
	}
	
	public void stopAcceptation() {
		this.mustContinueAccept=false;
	}
	
	public void run() {
		try {
			System.out.println("Début d'écoute de connexion");
			while(mustContinueAccept) {
				Socket socket=serverSocket.accept();
				this.serverMain.addClientWithSocket(socket);
			}
			System.out.println("Fin d'écoute de connexion");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
}
