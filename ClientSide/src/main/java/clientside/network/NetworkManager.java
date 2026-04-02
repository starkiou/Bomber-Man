package clientside.network;

import network.message.ConnectionMessage;
import network.message.Message;
import network.message.MessageSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.function.Consumer;

public class NetworkManager {
    private static NetworkManager instance;
    private Socket socket;
    private OutputStream out;
    private boolean connected = false;

    private String nickname;
    private int selectedCharacterId = 0;
    private int currentRoomId = -1;
    private boolean isHost = false;

    private Consumer<Message> currentMessageHandler;

    private final MessageSerializer serializer = new MessageSerializer();

    // Singleton
    public static NetworkManager getInstance() {
        if (instance == null) instance = new NetworkManager();
        return instance;
    }

    public void connect(String host, int port, String user) throws IOException {
        this.nickname = user;
        this.socket = new Socket(host, port);
        this.out = socket.getOutputStream();

        // Si on arrive ici, la socket est ouverte
        this.connected = true;

        // Lancer le thread qui écoute le serveur
        Thread listenerThread = new Thread(new NetworkListener(socket));
        listenerThread.setDaemon(true); // Important : s'arrête quand l'appli ferme
        listenerThread.start();

        this.sendMessage(new ConnectionMessage(this.nickname, selectedCharacterId));
    }

    public void sendMessage(Message message) {
        if (!connected || out == null) {
            System.err.println("Impossible d'envoyer : non connecté au serveur.");
            return;
        }

        try {
            byte[] data = serializer.serialize(message);
            out.write(data);
            out.flush();
        } catch (IOException e) {
            System.err.println("Erreur lors de l'envoi du message.");
            disconnect(); // On coupe tout si l'envoi échoue
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void disconnect() {
        this.connected = false; // On repasse à false immédiatement
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            out = null;
            socket = null;
        }
    }

    public void setMessageHandler(Consumer<Message> handler) {
        this.currentMessageHandler = handler;
    }

    public Consumer<Message> getMessageHandler() {
        return this.currentMessageHandler;
    }

    public int getSelectedCharacterId() {
        return selectedCharacterId;
    }

    public void setSelectedCharacterId(int id) {
        this.selectedCharacterId = id;
    }

    public int getCurrentRoomId() { return currentRoomId; }
    public void setCurrentRoomId(int id) { this.currentRoomId = id; }
    public boolean isHost() { return isHost; }
    public void setHost(boolean host) { this.isHost = host; }
}