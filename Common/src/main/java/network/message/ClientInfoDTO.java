package network.message;

import org.json.JSONObject;

public class ClientInfoDTO {
    private int clientId;
    private boolean isReady;
    private String pseudo;
    private int characterId; // Ajout de l'attribut

    public ClientInfoDTO(int clientId, boolean isReady, String pseudo, int characterId) {
        this.clientId = clientId;
        this.isReady = isReady;
        this.pseudo = pseudo;
        this.characterId = characterId;
    }

    public ClientInfoDTO(JSONObject json) {
        this.clientId = json.getInt("clientId");
        this.isReady = json.getBoolean("isReady");
        this.pseudo = json.getString("pseudo");
        // Lecture du JSON (avec une valeur par défaut à 1 au cas où)
        this.characterId = json.optInt("characterId", 1);
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("clientId", this.clientId);
        json.put("pseudo", this.pseudo);
        json.put("isReady", this.isReady);
        json.put("characterId", this.characterId); // Écriture dans le JSON
        return json;
    }

    public int getClientId() { return clientId; }
    public boolean isReady() { return isReady; }
    public String getPseudo() { return pseudo; }
    public int getCharacterId() { return characterId; }
}