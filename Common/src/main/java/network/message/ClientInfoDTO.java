package network.message;

import org.json.JSONObject;

public class ClientInfoDTO {
    private int clientId;
    private boolean isReady;
    private String pseudo;

    public ClientInfoDTO(int clientId, boolean isReady, String pseudo) {
        this.clientId = clientId;
        this.isReady = isReady;
        this.pseudo = pseudo;
    }

    public ClientInfoDTO(JSONObject json) {
    	this.clientId = json.getInt("clientId");
        this.isReady = json.getBoolean("isReady");
        this.pseudo = json.getString("pseudo");
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("clientId", this.clientId);
        json.put("pseudo", this.pseudo);
        json.put("isReady", this.isReady);
        return json;
    }

    public int getClientId() {
    	return clientId;
    }
    
}