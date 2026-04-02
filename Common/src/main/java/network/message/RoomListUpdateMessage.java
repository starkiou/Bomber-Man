// LobbyMessage.java
package network.message;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RoomListUpdateMessage implements Message {
    private List<RoomInfoDTO> rooms;

    public RoomListUpdateMessage(List<RoomInfoDTO> rooms) {
        this.rooms = rooms;
    }

    public RoomListUpdateMessage(JSONObject dataJson) {
        this.rooms = new ArrayList<>();
        JSONArray roomsArray = dataJson.getJSONArray("rooms");
        for (int i = 0; i < roomsArray.length(); i++) {
            this.rooms.add(new RoomInfoDTO(roomsArray.getJSONObject(i)));
        }
    }

    public List<RoomInfoDTO> getRooms() {
        return rooms;
    }

    @Override
    public JSONObject getData() {
        JSONObject obj = new JSONObject();
        JSONArray array = new JSONArray();
        for (RoomInfoDTO room : this.rooms) {
            array.put(room.toJson());
        }
        obj.put("rooms", array);
        return obj;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.ROOM_LIST_UPDATE;
    }
}