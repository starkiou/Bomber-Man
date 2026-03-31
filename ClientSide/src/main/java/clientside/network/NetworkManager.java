package clientside.network;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class NetworkManager {
    private static NetworkManager instance;
    private Socket socket;
    private ObjectOutputStream out;
    private String nickname;

    // Singleton : une seule instance pour toute l'appli
    public static NetworkManager getInstance() {
        if (instance == null) instance = new NetworkManager();
        return instance;
    }

    public void connect(String host, int port, String user) throws IOException {
        this.nickname = user;
        this.socket = new Socket(host, port);
        this.out = new ObjectOutputStream(socket.getOutputStream());

        // Lancer le thread qui écoute le serveur ici
        new Thread(new NetworkListener(socket)).start();
    }

    public void sendMessage(Object msg) {
        try {
            out.writeObject(msg);
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
}