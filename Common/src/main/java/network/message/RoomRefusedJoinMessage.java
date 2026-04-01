package network.message;

import org.json.JSONObject;

public class RoomRefusedJoinMessage implements Message {
	
	private int idRoom;
	
	public RoomRefusedJoinMessage(int idRoom) {
		this.idRoom=idRoom;
	}
	
	public RoomRefusedJoinMessage(JSONObject dataJson) {
		this.idRoom = dataJson.getInt("idRoom");
	}
	
	@Override
	public JSONObject getData() {
		JSONObject obj = new JSONObject();
	    obj.put("idRoom", this.idRoom);
	    return obj;
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.REFUSED_ROOM_JOIN;
	}

	public int getIdRoom() {
		return idRoom;
	}

	public void setIdRoom(int idRoom) {
		this.idRoom = idRoom;
	}

}
