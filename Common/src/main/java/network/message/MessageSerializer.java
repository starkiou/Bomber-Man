package network.message;

import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

public class MessageSerializer {
	public byte[] serialize(Message message) {

		String stringMessage = message.getMessageType().name() + "|" + message.getData().toString();
        return stringMessage.getBytes(StandardCharsets.UTF_8);
		
	}
	
	public Message deserialize(byte[] bytedObject) {
		String messageString = new String(bytedObject, StandardCharsets.UTF_8);
        String[] partition = messageString.split("\\|", 2);
		String typeString = partition[0];
        String dataString = partition[1];
        JSONObject dataJson = new JSONObject(dataString);
        
        switch(MessageType.valueOf(typeString)) {
			case BOMB_PLACE:
				break;
			case CHAT:
				break;
			case CONNECTION:
				return new ConnectionMessage(dataJson);
			case GAME_STATE:
				break;
			case LOBBY_UPDATE:
				break;
			case MOVE:
				break;
        	
        }
        return null;
		
	};
	
	

}
