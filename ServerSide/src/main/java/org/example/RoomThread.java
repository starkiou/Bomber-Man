package org.example;

import model.maze.CellType;
import model.maze.MazeFactory;
import network.message.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoomThread extends Thread {
    private List<ClientHandler> listClient = Collections.synchronizedList(new ArrayList<ClientHandler>());

    public MessageFactory factory = new MessageFactory();

    public volatile boolean isWaiting = true;

    public final int id;

    private volatile long readyStartTime = -1;
    private static final int TIME_BEFORE_LAUNCH = 10000;

    private volatile boolean readyToLaunch = false;

    private volatile boolean inGame = false;

    private int maxPlayer;
    private String roomName;
    private String mapSize;
    private String difficulty;
    private int botCount;

    public RoomThread(int maxPlayer, int id, String roomName, String mapSize, String difficulty, int botCount) {
        this.maxPlayer = maxPlayer;
        this.id = id;
        this.roomName = roomName;
        this.mapSize = mapSize;
        this.difficulty = difficulty;
        this.botCount = botCount;
    }

    public int getRoomId() {
        return this.id;
    }

    public void addClient(ClientHandler clientHandler) {
        listClient.add(clientHandler);
    }

    @Override
    public void run() {
        while (isWaiting) {
            this.updateReadyToLaunch();

            if (readyToLaunch && readyStartTime != -1) {
                long elapsed = System.currentTimeMillis() - readyStartTime;

                if (elapsed >= TIME_BEFORE_LAUNCH) {
                    // Le compte à rebours est terminé : on lance réellement la partie.
                    this.launchGame();
                    break;
                }
            }

            JSONObject json = new JSONObject();
            json.put("roomId", this.getRoomId());
            json.put("isWaiting", this.isWaiting);

            if (readyStartTime != -1) {
                long remaining = TIME_BEFORE_LAUNCH - (System.currentTimeMillis() - readyStartTime);
                json.put("countdown", Math.max(0, remaining));
            } else {
                json.put("countdown", -1);
            }

            JSONArray array = new JSONArray();
            synchronized (listClient) {
                for (ClientHandler client : listClient) {
                    ClientInfoDTO clientInfoDTO = new ClientInfoDTO(
                            client.getClientId(),
                            client.isReady(),
                            client.getPseudo(),
                            client.getCharacterId() // Ajout de l'ID du skin ici
                    );
                    array.put(clientInfoDTO.toJson());
                }
            }

            json.put("clients", array);

            RoomStatusMessage roomStatusMessage = (RoomStatusMessage) factory.make(MessageType.ROOM_STATUS_UPDATE, json);

            synchronized (listClient) {
                for (ClientHandler client : listClient) {
                    client.addMessage(roomStatusMessage);
                }
            }

            try {
                sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void stopWaiting() {
        isWaiting = false;
    }

    private void updateReadyToLaunch() {
        if (listClient.isEmpty()) { readyToLaunch = false; readyStartTime = -1; return; }
        readyToLaunch = true;
        synchronized (listClient) {
            for (ClientHandler client : listClient) {
                if (!client.isReady()) { readyToLaunch = false; break; }
            }
        }
        if (readyToLaunch) {
            if (readyStartTime == -1) readyStartTime = System.currentTimeMillis();
        } else {
            readyStartTime = -1;
        }
    }

    public boolean isReadyToLaunch() { return readyToLaunch; }

    // liste des clients sous forme DTO pour le LaunchGameMessage
    public List<ClientInfoDTO> getClientInfoList() {
        List<ClientInfoDTO> list = new ArrayList<>();
        synchronized (listClient) {
            for (ClientHandler c : listClient)
                list.add(new ClientInfoDTO(c.getClientId(), c.isReady(), c.getPseudo(), c.getCharacterId())); // Ajout de l'ID du skin ici
        }
        return list;
    }

    /**
     * Lance réellement la partie : génère la grille, envoie à chaque client un
     * LaunchGameMessage personnalisé (avec son playerId positionnel) et marque la
     * room en jeu. Appelée à la fois par le bouton explicite (LAUNCH_GAME) et par
     * le compte à rebours automatique. Idempotente.
     */
    public synchronized void launchGame() {
        if (inGame) return; // déjà lancée

        CellType[][] grid = MazeFactory.createMaze(MazeFactory.Algorithm.EXHAUSTIVE, 15, 11);
        List<ClientInfoDTO> playerList = getClientInfoList();

        // Snapshot cohérent de la liste des clients (évite une course sur les index).
        List<ClientHandler> snapshot;
        synchronized (listClient) {
            snapshot = new ArrayList<>(listClient);
        }
        for (int i = 0; i < snapshot.size(); i++) {
            snapshot.get(i).addMessage(new LaunchGameMessage(playerList, botCount, grid, i + 1));
        }

        setInGame(true);
        stopWaiting();
        ServerManager.getInstance().updateRoomsListOfClients();
    }

    public synchronized void broadcast(Message message) {
        synchronized (listClient) {
            for(ClientHandler client : listClient) {
                client.addMessage(message);
            }
        }
    }

    public boolean isInGame() {
        return inGame;
    }

    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }

    public int getMaxPlayers() {
        return maxPlayer;
    }

    public int getPlayerCount() {
        return this.listClient.size();
    }

    public String getRoomName() { return roomName; }
    public String getMapSize() { return mapSize; }
    public String getDifficulty() { return difficulty; }
    public int getBotCount() { return botCount; }
    public boolean isFull() { return (this.getPlayerCount() >= this.maxPlayer); }

    public void removeClient(ClientHandler client) {
        this.listClient.remove(client);
        if(this.listClient.isEmpty()) {
            ServerManager.getInstance().removeRoom(this);
            // La room est vide : on arrête sa boucle pour ne pas laisser le thread
            // tourner dans le vide (broadcast à personne toutes les 200 ms).
            this.stopWaiting();
        }
    }

}