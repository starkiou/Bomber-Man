package org.example;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import network.message.Message;
import network.message.MessageSerializer;
import network.message.RoomQuitMessage;

import java.nio.ByteBuffer;

public class ClientHandler extends Thread {
	private Socket socket;

	
	private ServerManager serverManager;
	
	private int clientId;
	
	private final MessageSerializer serializer = new MessageSerializer();
	
	private final Queue<Message> messageQueue = new LinkedList<>();
    
    private boolean mustContinueListen = true;
    
    private RoomThread room = null;
    
    public InputStream in;
    public OutputStream out;
    

	private boolean isReady = false; //maybe need to be moved into roomThread
	
	private String pseudo;

	private int skinId; 


	
	public ClientHandler(Socket socket, ServerManager serverManager, int clientId){
		this.socket=socket;
		this.serverManager=serverManager;
		this.clientId=clientId;
		
		

	}
	
	
	public void run() {
		try {
			in = socket.getInputStream();
	        out = this.socket.getOutputStream();
	        startSendingInAnotherThread();
	        startListening();

	    } catch (IOException e) {
	        System.out.println("Client a l'adresse " + socket.getInetAddress() + " deconnecte.");
	    } finally {
	    	
	    }
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

	public synchronized void setRoom(RoomThread room) {
		this.room = room;
	}
	
	public String getPseudo() {
		return pseudo;
	}

	public void setPseudo(String pseudo) {
		this.pseudo = pseudo;
	}
	
	public int getClientId() {
		return this.clientId;
	}

	
	public void addMessage(Message message) {
	    synchronized (messageQueue) {
	        messageQueue.add(message);
	    }
	}
	
	private void startSendingInAnotherThread() {
		new Thread(() -> {
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
	
	private void startListening() {
	    try {
	        while (mustContinueListen) {

	            int type = in.read();
	            if (type == -1) break;

	            byte[] lengthBytes = in.readNBytes(4);
	            if (lengthBytes.length < 4) break;

	            int length = ByteBuffer.wrap(lengthBytes).getInt();

	            byte[] dataBytes = in.readNBytes(length);
	            if (dataBytes.length < length) break;

	            ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + length);
	            buffer.put((byte) type);
	            buffer.putInt(length);
	            buffer.put(dataBytes);

	            Message message = this.serializer.deserialize(buffer.array());

	            if (message != null) {
	                this.serverManager.getServerMessageHandler().handle(this, message);
	            }
	        }
	    } catch (IOException e) {
	        if (mustContinueListen) e.printStackTrace();
	    } finally {
	        closeConnection();
	        this.serverManager.getServerMessageHandler().handle(this, new RoomQuitMessage());
	        serverManager.removeClient(this);
	    }
	}


	public void setSkinId(int skinId) {
		this.skinId=skinId;
	}


}
