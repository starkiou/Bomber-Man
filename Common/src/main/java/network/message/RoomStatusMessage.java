// LobbyMessage.java
package network.message;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class RoomStatusMessage implements Message {
    private List<ClientInfoDTO> clients;
    private int roomId;
    private boolean isWaiting;

    public RoomStatusMessage(int roomId, boolean isWaiting, List<ClientInfoDTO> clients) {
    	this.roomId=roomId;
        this.clients = clients;
        this.isWaiting = isWaiting;
    }

    public RoomStatusMessage(JSONObject dataJson) {
        this.clients = new ArrayList<>();
        JSONArray clientsArray = dataJson.getJSONArray("clients");
        this.roomId = dataJson.getInt("roomId");
        for (int i = 0; i < clientsArray.length(); i++) {
            this.clients.add(new ClientInfoDTO(clientsArray.getJSONObject(i)));
        }
    }

    public List<ClientInfoDTO> getClients() {
        return clients;
    }

    @Override
    public JSONObject getData() {
        JSONObject obj = new JSONObject();
        obj.put("roomId", this.roomId);
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
}