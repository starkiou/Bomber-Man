package network.message;

import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class MessageSerializer {
	private MessageFactory messageFactory = new MessageFactory();
	
	
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

        return messageFactory.make(type, dataJson);
    
		
	};
	
	

}
