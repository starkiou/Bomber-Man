package network.message;

import org.json.JSONObject;

public class ChatMessage implements Message {
	private String sender;
	private String content;
	
	

	public ChatMessage(String sender, String content) {
		this.sender = sender;
		this.content = content;
	}
	
	public ChatMessage(JSONObject dataJson) {
		this.sender = dataJson.getString("sender");
		this.content = dataJson.getString("content");
	}


	public String getSender() {
		return sender;
	}

	public String getContent() {
		return content;
	}

	@Override
	public JSONObject getData() {
		 JSONObject obj = new  JSONObject();
		 obj.put("content", this.content);
		 obj.put("sender", this.sender);
		return obj;
	}

	@Override
	public MessageType getMessageType() {
		
		return MessageType.CHAT;
	}

}
