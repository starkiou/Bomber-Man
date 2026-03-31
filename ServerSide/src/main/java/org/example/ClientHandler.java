package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import network.message.Message;
import network.message.MessageSerializer;

public class ClientHandler extends Thread {
	private Socket socket;
	
	private final Queue<Message> messageQueue = new LinkedList<>();
    
    private boolean mustContinueListen = true;
    
    private RoomThread room = null;
    
    public InputStream in;
    public OutputStream out;
    
	private boolean isReady = false; //maybe need to be moved into roomThread

	
	public ClientHandler(Socket socket){
		this.socket=socket;
		
		
	}
	
	public synchronized void send(Message message) throws IOException {
		MessageSerializer serializer = new MessageSerializer();
		byte[] serializedMessage = serializer.serialize(message);
		out.write(serializedMessage);
		out.flush();
	}
	
	public void run() {
		try {
			in = socket.getInputStream();
	        out = this.socket.getOutputStream();

	        startSendingThread();
	    } catch (IOException e) {
	        System.out.println("Client a l'adresse " + socket.getInetAddress() + " deconnecte.");
	    } finally {
	    	
	    }
	}
	
	private boolean ping() {
		//TODO
		return true;
		
	}

	public boolean isReady() {
		return isReady;
	}

	public void setReady(boolean isReady) {
		this.isReady = isReady;
	}
	
	public void stopThread() {
		this.closeConnection();
	}
	
	private void closeConnection() {
		this.mustContinueListen=false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (!socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	public RoomThread getRoom() {
		return room;
	}

	public void setRoom(RoomThread room) {
		this.room = room;
	}
	
	public void addMessage(Message message) {
		this.messageQueue.add(message);
	}
	
	private void startSendingThread() {
		new Thread(() -> {
			MessageSerializer serializer = new MessageSerializer();
            while (mustContinueListen) {
                Message message = null;
                synchronized (messageQueue) {
                    if (!messageQueue.isEmpty()) {
                        message = messageQueue.poll();
                    }
                }
                if (message != null) {
                    try {
                        byte[] data = serializer.serialize(message);
                        out.write(data);
                        out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
	}


}
