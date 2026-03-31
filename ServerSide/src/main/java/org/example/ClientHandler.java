package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import network.message.Message;
import network.message.MessageSerializer;

public class ClientHandler extends Thread {
	private Socket socket;
	
	private final MessageSerializer serializer = new MessageSerializer();
	
	private final Queue<Message> messageQueue = new LinkedList<>();
    
    private boolean mustContinueListen = true;
    
    private RoomThread room = null;
    
    public InputStream in;
    public OutputStream out;
    
	private boolean isReady = false; //maybe need to be moved into roomThread

	
	public ClientHandler(Socket socket){
		this.socket=socket;
		
		
	}
	
	
	public void run() {
		try {
			in = socket.getInputStream();
	        out = this.socket.getOutputStream();
	        startListeningThread();
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
	    synchronized (messageQueue) {
	        messageQueue.add(message);
	    }
	}
	
	private void startSendingThread() {
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
	
	private void startListeningThread() {
	    new Thread(() -> {
	        try {
	            while (mustContinueListen) {

	                int type = in.read();
	                if (type == -1) break;

	                byte[] lengthBytes = in.readNBytes(4);
	                if (lengthBytes.length < 4) break;

	                int length = java.nio.ByteBuffer.wrap(lengthBytes).getInt();

	                byte[] dataBytes = in.readNBytes(length);
	                if (dataBytes.length < length) break;

	                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(1 + 4 + length);
	                buffer.put((byte) type);
	                buffer.putInt(length);
	                buffer.put(dataBytes);

	                Message message = this.serializer.deserialize(buffer.array());

	                if (message != null) {
	                    System.out.println("Reçu: " + message.getMessageType());
	                }
	            }
	        } catch (IOException e) {
	            if (mustContinueListen) e.printStackTrace();
	        } finally {
	            closeConnection();
	        }
	    }).start();
	}


}
