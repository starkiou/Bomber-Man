package network.message;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

public class MessageSerializer {
	public byte[] serialize(Message message) {
		byte typeByte = message.getMessageType().getId();

		byte[] dataBytes = message.getData().toString().getBytes(StandardCharsets.UTF_8);
		
		ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + dataBytes.length);
		buffer.put(typeByte);
		buffer.putInt(dataBytes.length);
        buffer.put(dataBytes); 
		
        return buffer.array();
		
	}
	
	public Message deserialize(byte[] byteObject) {
		ByteBuffer buffer = ByteBuffer.wrap(byteObject);

        byte typeByte = buffer.get();
        int length = buffer.getInt();

        byte[] dataBytes = new byte[length];
        buffer.get(dataBytes);

        JSONObject dataJson = new JSONObject(new String(dataBytes, StandardCharsets.UTF_8));
        MessageType type = MessageType.fromId(typeByte);

        switch(type) {
            case CONNECTION:
                return new ConnectionMessage(dataJson);
            case CHAT:
            	break;

            case MOVE:
            	break;

            case BOMB_PLACE:
            	break;

            case GAME_STATE:
            	break;

            case LOBBY_UPDATE:
            	break;

            default:
                throw new RuntimeException("Type de message inconnu : " + type);
        }
		return null;
    
		
	};
	
	

}
