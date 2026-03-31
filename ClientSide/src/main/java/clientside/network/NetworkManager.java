package clientside.network;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import network.message.ConnectionMessage;
import network.message.Message;
import network.message.MessageSerializer;

public class NetworkManager {
    private static NetworkManager instance;
    private Socket socket;
    private OutputStream out;
    private String nickname;
    
    private final MessageSerializer serializer = new MessageSerializer();
   

    // Singleton : une seule instance pour toute l'appli
    public static NetworkManager getInstance() {
        if (instance == null) instance = new NetworkManager();
        return instance;
    }

    public void connect(String host, int port, String user) throws IOException {
        this.nickname = user;
        this.socket = new Socket(host, port);
        this.out = socket.getOutputStream();

        // Lancer le thread qui écoute le serveur ici
        new Thread(new NetworkListener(socket)).start();
        this.sendMessage(new ConnectionMessage(this.nickname));
    }

    public void sendMessage(Message message) {
        try {
        	byte[] data = serializer.serialize(message);
        	out.write(data);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    
    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            out = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}