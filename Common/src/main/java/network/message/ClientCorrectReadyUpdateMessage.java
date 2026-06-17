package network.message;

import org.json.JSONObject;

public class ClientCorrectReadyUpdateMessage extends AbstractEmptyMessage {

	public ClientCorrectReadyUpdateMessage() {
	}

	public ClientCorrectReadyUpdateMessage(JSONObject dataJson) {
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.CLIENT_READY_CORRECT_UPDATE;
	}
}
