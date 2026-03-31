package network.message;
import org.json.JSONObject;

public interface Message {
	
	public JSONObject getData();
	
	public MessageType getMessageType();
}
