package network.message;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class LobbyMessage implements Message {
	private List<String> listeSalon;
	private List<String> listeJoueur;
	

	public LobbyMessage(List<String> listeSalon, List<String> listeJoueur) {
		this.listeSalon = listeSalon;
		this.listeJoueur = listeJoueur;
	}
	public LobbyMessage(JSONObject dataJson) {
        this.listeSalon = new ArrayList<>();
        this.listeJoueur = new ArrayList<>();
        
        JSONArray salonsArray = dataJson.getJSONArray("listeSalon");
       
        for (int i = 0; i < salonsArray.length(); i++) {
            this.listeSalon.add(salonsArray.getString(i));
        }
        
     
        JSONArray joueursArray = dataJson.getJSONArray("listeJoueur");
        for (int i = 0; i < joueursArray.length(); i++) {
            this.listeJoueur.add(joueursArray.getString(i));
        }
    }
	
	public List<String> getListeSalon() {
		return listeSalon;
	}
	
	public List<String> getListeJoueur() {
		return listeJoueur;
	}
	
	@Override
	public JSONObject getData() {
		JSONObject obj = new JSONObject();
	    obj.put("listeSalon", this.listeSalon);
	    obj.put("listeJoueur", this.listeJoueur);
	    
	    return obj;
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.LOBBY_UPDATE;
	}

}
