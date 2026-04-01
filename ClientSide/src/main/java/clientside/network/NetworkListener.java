package clientside.network;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

import network.message.Message;
import network.message.MessageSerializer;
import network.message.MessageType;
import org.json.JSONObject;

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
            e.printStackTrace();
        } finally {
            stop();
        }
    }

    private void handleMessage(Message message) {
        if (message.getMessageType() == MessageType.CHAT) {
            JSONObject data = (JSONObject) message.getData();
            String content = data.getString("content");

            // TODO: Il faudra une méthode pour récupérer l'instance
            // actuelle du ChatController.
            // Pour l'instant, on imprime en console pour vérifier
            System.out.println("CHAT REÇU : " + content);

            // Si tu as une référence vers ton controller (via SceneManager par exemple) :
            // sceneManager.getChatController().appendMessage(content);
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