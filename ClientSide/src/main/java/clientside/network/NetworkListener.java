package clientside.network;

import network.message.Message;
import network.message.MessageSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class NetworkListener implements Runnable {
    private final Socket socket;
    private boolean running = true;
    private final MessageSerializer serializer = new MessageSerializer();

    public NetworkListener(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (InputStream in = socket.getInputStream()) {
            while (running) {
                // On attend un message du serveur (bloquant)
                int type = in.read();
                byte[] lengthBytes = in.readNBytes(4);
                if (lengthBytes.length < 4) break;
                int length = ByteBuffer.wrap(lengthBytes).getInt();
                
                byte[] dataBytes = in.readNBytes(length);
                if (dataBytes.length < length) break;
                
                ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + length);
                buffer.put((byte) type);
                buffer.putInt(length);
                buffer.put(dataBytes);
                
                Message message = serializer.deserialize(buffer.array());
                
                if (message != null) {
                    handleMessage(message); //On traite le mess dedans
                } 
            }
        } catch (Exception e) {
            System.err.println("Connexion perdue avec le serveur.");
        } finally {
            stop();
        }
    }

    private void handleMessage(Message msg) {
        System.out.println("Message reçu du serveur : " + msg.getMessageType());

        // On transmet au contrôleur qui écoute actuellement
        if (NetworkManager.getInstance().getMessageHandler() != null) {
            NetworkManager.getInstance().getMessageHandler().accept(msg);
        }
    }

    public void stop() {
        this.running = false;
        try {
            if (!socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    
    
}