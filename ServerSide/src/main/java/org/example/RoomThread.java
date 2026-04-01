package org.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import network.message.ClientInfoDTO;
import network.message.Message;
import network.message.MessageFactory;
import network.message.MessageType;
import network.message.RoomStatusMessage;

public class RoomThread extends Thread {
    private final List<ClientHandler> listClient = Collections.synchronizedList(new ArrayList<>());
    private final MessageFactory factory = new MessageFactory();

    public final int id;
    private final String roomName;
    private int maxPlayer;

    private boolean isWaitingToLaunch = true;
    private boolean inGame = false;

    public RoomThread(int maxPlayer, int id, String roomName) {
        this.maxPlayer = maxPlayer;
        this.id = id;
        this.roomName = roomName;
    }

    /**
     * Construit et envoie l'état actuel de la salle à tous les clients connectés.
     * Appelé uniquement lors d'un changement d'état (Join/Leave/Ready).
     */
    public synchronized void broadcastRoomStatus() {
        JSONObject json = new JSONObject();
        json.put("roomId", this.id);
        json.put("isWaiting", this.isWaitingToLaunch);

        JSONArray array = new JSONArray();
        for (ClientHandler client : listClient) {
            // Création du DTO pour chaque client
            ClientInfoDTO dto = new ClientInfoDTO(client.getClientId(), client.isReady(), client.getPseudo());
            array.put(dto.toJson());
        }
        json.put("clients", array);

        // Création du message via la factory
        RoomStatusMessage msg = (RoomStatusMessage) factory.make(MessageType.ROOM_STATUS_UPDATE, json);

        // Envoi à tout le monde
        for (ClientHandler client : listClient) {
            client.addMessage(msg);
        }

        System.out.println("[Room " + id + "] Status broadcasté aux " + listClient.size() + " joueurs.");
    }

    @Override
    public void run() {
        System.out.println("[Room " + id + "] Thread démarré.");

        while (isWaitingToLaunch) {
            // Ici, on vérifie périodiquement si tout le monde est prêt pour lancer la game
            checkReadyToLaunch();

            try {
                // On dort 1 seconde, c'est suffisant pour un check de lancement
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }

        // Logique de lancement de la partie ici...
        System.out.println("[Room " + id + "] Lancement de la partie !");
    }

    private void checkReadyToLaunch() {
        if (listClient.isEmpty()) return;

        boolean allReady = true;
        for (ClientHandler client : listClient) {
            if (!client.isReady()) {
                allReady = false;
                break;
            }
        }

        if (allReady && listClient.size() >= 1) { // Au moins 1 ou 2 selon tes règles
            // Optionnel : Déclencher un compte à rebours
        }
    }

    // --- Méthodes d'entrée/sortie ---

    public void addClient(ClientHandler clientHandler) {
        listClient.add(clientHandler);
        System.out.println("Joueur " + clientHandler.getPseudo() + " a rejoint la room " + id);
        broadcastRoomStatus(); // UPDATE IMMEDIAT
    }

    public void removeClient(ClientHandler client) {
        listClient.remove(client);
        System.out.println("Joueur a quitté la room " + id);
        broadcastRoomStatus(); // UPDATE IMMEDIAT
    }

    /**
     * À appeler depuis le ServerHandler quand un message de type "READY" est reçu.
     */
    public void onPlayerReadyStatusChanged() {
        broadcastRoomStatus(); // UPDATE IMMEDIAT pour afficher le check "Prêt" chez les autres
    }

    // --- Getters / Setters ---

    public int getRoomId() { return id; }
    public String getRoomName() { return roomName; }
    public int getPlayerCount() { return listClient.size(); }
    public boolean isFull() { return listClient.size() >= maxPlayer; }
    public void stopWaiting() { this.isWaitingToLaunch = false; }
    public int getMaxPlayers() {return this.maxPlayer;}
    public boolean isInGame() { return this.inGame; }
}