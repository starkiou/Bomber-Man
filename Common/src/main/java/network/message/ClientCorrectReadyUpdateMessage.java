package network.message;

import org.json.JSONObject;

public class ClientCorrectReadyUpdateMessage implements Message {
	
	
	public ClientCorrectReadyUpdateMessage() {
		
	}
	
	public ClientCorrectReadyUpdateMessage(JSONObject dataJson) {

	}
	
	@Override
	public JSONObject getData() {
	    return new JSONObject();
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.CLIENT_READY_CORRECT_UPDATE;
	}

}
