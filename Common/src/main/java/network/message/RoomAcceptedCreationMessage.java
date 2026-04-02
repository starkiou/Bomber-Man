package network.message;

import org.json.JSONObject;

public class RoomAcceptedCreationMessage implements Message {
	
	private int idRoom;
	
	public RoomAcceptedCreationMessage(int idRoom) {
		this.idRoom=idRoom;
	}
	
	public RoomAcceptedCreationMessage(JSONObject dataJson) {
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
		return MessageType.ACCEPTED_ROOM_CREATION;
	}

	public int getIdRoom() {
		return idRoom;
	}

	public void setIdRoom(int idRoom) {
		this.idRoom = idRoom;
	}

}
