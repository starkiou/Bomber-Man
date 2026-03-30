package network.message;

public interface Message {
	public byte[] serialize();
	
	public Message deserialize(byte[] bytedObject);
	
	public MessageType getMessageType();
}
