package network.message;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class RoomStatusMessage implements Message {
    private List<ClientInfoDTO> clients;
    private int roomId;
    private String roomName;
    private String mapName;
    private String difficulty;
    private boolean isWaiting;

    // Constructeur pour le Serveur (Création du message)
    public RoomStatusMessage(int roomId, String roomName, String mapName, String difficulty, boolean isWaiting, List<ClientInfoDTO> clients) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.mapName = mapName;
        this.difficulty = difficulty;
        this.clients = clients;
        this.isWaiting = isWaiting;
    }

    // Constructeur pour le Client (Réception via JSON)
    public RoomStatusMessage(JSONObject dataJson) {
        this.clients = new ArrayList<>();
        this.roomId = dataJson.getInt("roomId");
        this.roomName = dataJson.optString("roomName", "Inconnue"); // optString évite les crashs si absent
        this.mapName = dataJson.optString("mapName", "Standard");
        this.difficulty = dataJson.optString("difficulty", "Normale");
        this.isWaiting = dataJson.getBoolean("isWaiting");

        JSONArray clientsArray = dataJson.getJSONArray("clients");
        for (int i = 0; i < clientsArray.length(); i++) {
            this.clients.add(new ClientInfoDTO(clientsArray.getJSONObject(i)));
        }
    }

    @Override
    public JSONObject getData() {
        JSONObject obj = new JSONObject();
        obj.put("roomId", this.roomId);
        obj.put("roomName", this.roomName);
        obj.put("mapName", this.mapName);
        obj.put("difficulty", this.difficulty);
        obj.put("isWaiting", this.isWaiting);

        JSONArray array = new JSONArray();
        for (ClientInfoDTO client : this.clients) {
            array.put(client.toJson());
        }
        obj.put("clients", array);
        return obj;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.ROOM_STATUS_UPDATE;
    }

    // --- Getters ---
    public List<ClientInfoDTO> getClients() { return clients; }
    public int getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public String getMapName() { return mapName; }
    public String getDifficulty() { return difficulty; }
    public boolean isWaiting() { return isWaiting; }
}