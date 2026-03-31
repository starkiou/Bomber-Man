package network.message;

import java.nio.charset.StandardCharsets;

public class MessageSerializer {
	public byte[] serialize(Message message) {

		String stringMessage = message.getMessageType().name() + "|" + message.getData();
		byte[] tabByte = stringMessage.getBytes(StandardCharsets.UTF_8);
	    return tabByte;
		
	}
	
	public Message deserialize(byte[] bytedObject) {
		String message = new String(bytedObject, StandardCharsets.UTF_8);
		System.out.println(message);
		return null;
		
	};
	
	public static void main(String args[]) {
		MessageSerializer ser = new MessageSerializer();
		ser.deserialize(ser.serialize(new ConnectionMessage("Coucou")));
		
	}

}
