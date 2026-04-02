package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import model.logger.LogManager;

public class AcceptConnectionThread extends Thread {


	private boolean mustContinueAccept = true;
	private ServerManager serverMain; 
	private ServerSocket serverSocket;
	
	
	
	
	public AcceptConnectionThread(ServerManager serverMain, ServerSocket serverSocket) {
		this.serverSocket=serverSocket;
		this.serverMain=serverMain;
	}


		

	public AcceptConnectionThread(ServerManager serverMain, int port) {
		try {
			this.serverSocket=new ServerSocket(port);
			LogManager.getInstance().info("Serveur en écoute sur le port " + port);
		} catch (IOException e) {
			LogManager.getInstance().error("Impossible de démarrer le serveur sur le port " + port + " : " + e.getMessage());
		}

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
