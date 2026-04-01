package network.message;

import org.json.JSONObject;

public class ConnectionMessage implements Message {
	
	private String pseudo;
	
	private int skinId;
	
	public ConnectionMessage(String pseudo, int skinId) {
		this.pseudo=pseudo;
		this.setSkinId(skinId);
	}
	
	public ConnectionMessage(JSONObject dataJson) {
		this.pseudo = dataJson.getString("pseudo");
		this.setSkinId(dataJson.getInt("skinId"));
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
	    obj.put("skinId", this.getSkinId());
	    return obj;
	}

	public int getSkinId() {
		return skinId;
	}

	public void setSkinId(int skinId) {
		this.skinId = skinId;
	}


}
