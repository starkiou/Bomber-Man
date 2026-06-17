package network.message;

import org.json.JSONObject;

/**
 * Base commune aux messages dont la charge utile est un unique identifiant de
 * room (clé JSON {@code "idRoom"}). Les sous-classes ne fournissent que leur
 * {@link MessageType}.
 */
public abstract class AbstractRoomIdMessage implements Message {

    protected int idRoom;

    protected AbstractRoomIdMessage(int idRoom) {
        this.idRoom = idRoom;
    }

    protected AbstractRoomIdMessage(JSONObject dataJson) {
        this.idRoom = dataJson.getInt("idRoom");
    }

    @Override
    public JSONObject getData() {
        JSONObject obj = new JSONObject();
        obj.put("idRoom", this.idRoom);
        return obj;
    }

    public int getIdRoom() {
        return idRoom;
    }
}
