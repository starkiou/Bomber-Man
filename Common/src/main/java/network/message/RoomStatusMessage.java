// LobbyMessage.java
package network.message;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RoomStatusMessage implements Message {
    private List<ClientInfoDTO> clients;
    private int roomId;
    private boolean isWaiting;
    private long countdown;
    /** Identifiant du client hôte de la room, ou -1 s'il est inconnu. */
    private int hostId = -1;

    public RoomStatusMessage(int roomId, boolean isWaiting, List<ClientInfoDTO> clients, long countdown) {
        this.roomId = roomId;
        this.clients = clients;
        this.isWaiting = isWaiting;
        this.countdown = countdown;
    }

    public RoomStatusMessage(JSONObject dataJson) {
        this.clients = new ArrayList<>();
        JSONArray clientsArray = dataJson.getJSONArray("clients");
        this.roomId = dataJson.getInt("roomId");
        this.isWaiting = dataJson.optBoolean("isWaiting", true);
        this.hostId = dataJson.optInt("hostId", -1);
        for (int i = 0; i < clientsArray.length(); i++) {
            this.clients.add(new ClientInfoDTO(clientsArray.getJSONObject(i)));
        }
        
        this.countdown = dataJson.getLong("countdown");
            
    }

    public List<ClientInfoDTO> getClients() {
        return clients;
    }

    @Override
    public JSONObject getData() {
        JSONObject obj = new JSONObject();
        obj.put("roomId", this.roomId);
        obj.put("isWaiting", this.isWaiting);
        obj.put("hostId", this.hostId);
        JSONArray array = new JSONArray();
        for (ClientInfoDTO client : this.clients) {
            array.put(client.toJson());
        }
        obj.put("clients", array);
        obj.put("countdown", this.countdown);
        return obj;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.ROOM_STATUS_UPDATE;
    }
    
    public long getCountdown() {
        return countdown;
    }

    public int getHostId() {
        return hostId;
    }

    public boolean isWaiting() {
        return isWaiting;
    }
}