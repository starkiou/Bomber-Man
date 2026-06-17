package org.example;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import network.message.Message;
import network.message.MessageSerializer;
import network.message.RoomQuitMessage;

import java.nio.ByteBuffer;

public class ClientHandler extends Thread {
	private Socket socket;

	
	private ServerManager serverManager;
	
	private int clientId;
	
	private final MessageSerializer serializer = new MessageSerializer();

	private final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();

    private volatile boolean mustContinueListen = true;

    private volatile RoomThread room = null;

    public InputStream in;
    public OutputStream out;


	private volatile boolean isReady = false; //maybe need to be moved into roomThread
	
	private String pseudo;

	private int skinId; 


	
	public ClientHandler(Socket socket, int clientId){
		this.socket=socket;
		this.serverManager=ServerManager.getInstance();
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
	    }
	}
	
	

	public boolean isReady() {
		return isReady;
	}

	public void setReady(boolean isReady) {
		this.isReady = isReady;
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
	    messageQueue.offer(message);
	}

	private void startSendingInAnotherThread() {
		new Thread(() -> {
            while (mustContinueListen) {
                Message message;
                try {
                    // Bloque jusqu'à ce qu'un message arrive (plus de busy-wait CPU).
                    message = messageQueue.poll(200, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                if (message != null) {
                    try {
                        byte[] data = serializer.serialize(message);
                        out.write(data);
                        out.flush();
                    } catch (IOException e) {
                        // Flux rompu : on arrête proprement le client au lieu de boucler.
                        closeConnection();
                        break;
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


    public int getCharacterId() {
        return this.skinId;
    }
}
