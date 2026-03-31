package network.message;
import org.json.JSONObject;

public interface Message {
	
	public String getData();
	
	public MessageType getMessageType();
}
