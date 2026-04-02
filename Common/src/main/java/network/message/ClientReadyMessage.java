package network.message;

import org.json.JSONObject;

public class ClientReadyMessage implements Message {
	
	private boolean isReady;
	
	public ClientReadyMessage(boolean isReady) {
		this.isReady=isReady;
	}
	
	public ClientReadyMessage(JSONObject dataJson) {
		this.isReady = dataJson.getBoolean("isReady");

	}
	
	@Override
	public JSONObject getData() {
		JSONObject obj = new JSONObject();
	    obj.put("isReady", this.isReady);
	    return obj;
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.READY_CLIENT;
	}

	public boolean isReady() {
		return isReady;
	}
	

}
