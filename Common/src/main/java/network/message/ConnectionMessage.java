package network.message;

import org.json.JSONObject;

public class ConnectionMessage implements Message {
	
	private String pseudo;
	
	public ConnectionMessage(String pseudo) {
		this.pseudo=pseudo;
	}
	
	public ConnectionMessage(JSONObject dataJson) {
		this.pseudo = dataJson.getString("pseudo");
	}

	public void setPseudo(String pseudo) {
		this.pseudo = pseudo;
	}
	
	public String getPseudo() {
		return this.pseudo;
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.CONNECTION;
	}

	@Override
	public JSONObject getData() {
		JSONObject obj = new JSONObject();
	    obj.put("pseudo", this.getPseudo());
	    return obj;
	}


}
