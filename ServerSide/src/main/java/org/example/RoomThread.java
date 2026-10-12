package org.example;

import model.aiPlayer.AIFactory;
import model.entity.Player;
import model.game.Game;
import model.game.GameSnapshot;
import model.game.GameStateListener;
import model.maze.CellType;
import model.maze.MazeFactory;
import network.message.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RoomThread extends Thread {
    private List<ClientHandler> listClient = Collections.synchronizedList(new ArrayList<ClientHandler>());

    public MessageFactory factory = new MessageFactory();

    public volatile boolean isWaiting = true;

    public final int id;

    private volatile long readyStartTime = -1;
    private static final int TIME_BEFORE_LAUNCH = 10000;

    private volatile boolean readyToLaunch = false;

    private volatile boolean inGame = false;

    /** Partie en cours, simulée ici ; {@code null} tant que la room attend. */
    private volatile Game game = null;

    /** L'état est simulé à 60 ticks par seconde et diffusé un tick sur trois. */
    private static final int STATE_BROADCAST_EVERY_N_TICKS = 3;

    /** Un joueur ne peut pas avancer plus vite qu'une case par intervalle (marge pour la latence). */
    private static final long MIN_MOVE_INTERVAL_MS = 120;
    private final Map<Integer, Long> lastMoveMs = new ConcurrentHashMap<>();

    /** Nombre total de personnages sur une carte, joueurs et bots confondus. */
    public static final int MAX_CHARACTERS = 4;

    /** Créateur de la room ; seul lui peut lancer la partie avant la fin du décompte. */
    private volatile ClientHandler host = null;

    private int maxPlayer;
    private String roomName;
    private String mapSize;
    private String difficulty;
    private int botCount;

    public RoomThread(int maxPlayer, int id, String roomName, String mapSize, String difficulty, int botCount) {
        this.id = id;
        this.roomName = roomName;
        this.mapSize = mapSize;
        this.difficulty = difficulty;
        // Les bots occupent des places : il reste au moins une place pour un joueur
        this.botCount = Math.max(0, Math.min(botCount, MAX_CHARACTERS - 1));
        this.maxPlayer = Math.max(1, Math.min(maxPlayer, MAX_CHARACTERS - this.botCount));
    }

    public int getRoomId() {
        return this.id;
    }

    public void addClient(ClientHandler clientHandler) {
        listClient.add(clientHandler);
        if (host == null) host = clientHandler;
    }

    public boolean isHost(ClientHandler client) {
        return client != null && client == host;
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
            ClientHandler currentHost = this.host;
            json.put("hostId", currentHost != null ? currentHost.getClientId() : -1);

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
     * Lance la partie : génère la grille, place joueurs et bots, démarre la
     * simulation sur le serveur et envoie à chaque client la configuration de
     * départ. Appelée par l'hôte (LAUNCH_GAME) ou par le compte à rebours. Idempotente.
     */
    public synchronized void launchGame() {
        if (inGame) return; // déjà lancée

        int size = mapSide(mapSize);
        CellType[][] grid = MazeFactory.createMaze(MazeFactory.Algorithm.EXHAUSTIVE, size, size);
        int w = grid[0].length, h = grid.length;
        int[][] spawns = { {1, 1}, {w - 2, h - 2}, {w - 2, 1}, {1, h - 2} };

        // Snapshot cohérent de la liste des clients (évite une course sur les index).
        List<ClientHandler> snapshot;
        synchronized (listClient) {
            snapshot = new ArrayList<>(listClient);
        }
        List<ClientInfoDTO> playerList = getClientInfoList();

        int spawn = 0;
        int maxId = 0;
        List<Player> players = new ArrayList<>();
        for (ClientHandler client : snapshot) {
            if (spawn >= spawns.length) break;
            players.add(new Player(client.getClientId(), spawns[spawn][0], spawns[spawn][1], 3, 1.0, 1));
            maxId = Math.max(maxId, client.getClientId());
            spawn++;
        }

        Game newGame = new Game(grid, players);
        // Les bots prennent la suite du plus grand identifiant de joueur
        for (int i = 0; i < botCount && spawn < spawns.length; i++, spawn++) {
            newGame.addBot(AIFactory.create(AIFactory.randomStrategy(difficulty),
                    maxId + 1 + i, spawns[spawn][0], spawns[spawn][1], 3, 1.0, 1));
        }

        newGame.addListener(new GameStateListener() {
            private int tick = 0;

            @Override
            public void onGameStateUpdate(GameSnapshot snap) {
                if (snap.isGameOver()) {
                    // Dernier état, toujours envoyé : c'est lui qui annonce le vainqueur
                    broadcast(new GameStateMessage(snap));
                    closeRoom();
                } else if (tick++ % STATE_BROADCAST_EVERY_N_TICKS == 0) {
                    broadcast(new GameStateMessage(snap));
                }
            }

            @Override public void onPlayerDied(int playerId) {}
            @Override public void onGameOver(int winnerId) {}
        });

        for (ClientHandler client : snapshot) {
            client.addMessage(new LaunchGameMessage(playerList, botCount, grid, client.getClientId()));
        }

        this.game = newGame;
        setInGame(true);
        stopWaiting();
        newGame.start();
        ServerManager.getInstance().updateRoomsListOfClients();
    }

    /** Côté de la carte, en cases, pour le libellé choisi à la création (mêmes tailles qu'en solo). */
    static int mapSide(String mapSize) {
        if (mapSize != null && mapSize.contains("Petite")) return 11;
        if (mapSize != null && mapSize.contains("Grande")) return 19;
        return 15;
    }

    /** Longueur maximale d'un message de chat. */
    private static final int MAX_CHAT_LENGTH = 200;

    /** Diffuse un message de chat à la room, signé du pseudo connu du serveur. */
    public void handleChat(ClientHandler sender, String content) {
        if (content == null) return;
        String text = content.strip();
        if (text.isEmpty()) return;
        if (text.length() > MAX_CHAT_LENGTH) text = text.substring(0, MAX_CHAT_LENGTH);
        String pseudo = sender.getPseudo() != null ? sender.getPseudo() : "Joueur #" + sender.getClientId();
        broadcast(new ChatMessage(pseudo, text));
    }

    /** Applique l'action d'un joueur à la partie. Un client ne commande que son propre personnage. */
    public void handlePlayerAction(ClientHandler sender, ActionType action) {
        Game current = this.game;
        if (current == null || action == null) return;

        if (action != ActionType.PLACE_BOMB) {
            long now = System.currentTimeMillis();
            Long last = lastMoveMs.get(sender.getClientId());
            if (last != null && now - last < MIN_MOVE_INTERVAL_MS) return;
            lastMoveMs.put(sender.getClientId(), now);
        }
        current.handleAction(sender.getClientId(), action);
    }

    /** Fin de partie : libère les joueurs, qui peuvent rejoindre ou créer une autre room. */
    private void closeRoom() {
        synchronized (listClient) {
            for (ClientHandler client : listClient) {
                client.setRoom(null);
                client.setReady(false);
            }
            listClient.clear();
        }
        ServerManager.getInstance().removeRoom(this);
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
        Game current = this.game;
        if (current != null) {
            // Un joueur qui part en pleine partie est éliminé, la partie continue pour les autres
            current.eliminatePlayer(client.getClientId());
        }
        if (client == host) {
            // L'hôte est parti : le plus ancien joueur restant prend sa place
            synchronized (listClient) {
                host = listClient.isEmpty() ? null : listClient.get(0);
            }
        }
        if(this.listClient.isEmpty()) {
            ServerManager.getInstance().removeRoom(this);
            // La room est vide : on arrête sa boucle pour ne pas laisser le thread
            // tourner dans le vide (broadcast à personne toutes les 200 ms).
            this.stopWaiting();
            if (current != null) current.stop();
        }
    }

}