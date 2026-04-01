package network.message;

import org.json.JSONObject;

public class RoomJoiningMessage implements Message {
	
	private int idRoom;
	
	public RoomJoiningMessage(int idRoom) {
		this.idRoom=idRoom;
	}
	
	public RoomJoiningMessage(JSONObject dataJson) {
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
		return MessageType.ROOM_JOIN;
	}

	public int getIdRoom() {
		return idRoom;
	}

	public void setIdRoom(int idRoom) {
		this.idRoom = idRoom;
	}

}
