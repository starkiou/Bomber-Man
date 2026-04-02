package network.message;


import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GameStateMessage implements Message {
	private List<PlayerDTO> players;
    private List<BombDTO> bombs;

   
    public GameStateMessage(List<PlayerDTO> players, List<BombDTO> bombs) {
		this.bombs = bombs;
		this.players = players;
	}

	public GameStateMessage(JSONObject dataJson) {
        this.players = new ArrayList<>();
        this.bombs = new ArrayList<>();
        
        JSONArray playersArray = dataJson.getJSONArray("players");
        
        for (int i = 0; i < playersArray.length(); i++) {
            JSONObject playerJson = playersArray.getJSONObject(i);
            
            PlayerDTO p = new PlayerDTO(playerJson);
            
            this.players.add(p);
        }
        
        JSONArray bombsArray = dataJson.getJSONArray("bombs");
        
        for (int i = 0; i < bombsArray.length(); i++) {
            JSONObject bombsJson = bombsArray.getJSONObject(i);
            
            BombDTO p = new BombDTO(bombsJson);
            
            this.bombs.add(p);
        }
        
        
    }

    public List<PlayerDTO> getPlayers() {
		return players;
	}

	public List<BombDTO> getBombs() {
		return bombs;
	}

	
    @Override
    public MessageType getMessageType() {
        return MessageType.GAME_STATE; 
    }

    @Override
    public JSONObject getData() {
        JSONObject obj = new JSONObject();
        
        JSONArray playersArray = new JSONArray();
        for (PlayerDTO p : this.players) {
            playersArray.put(p.toJson());
        }
        obj.put("players", playersArray);
        
        
        JSONArray bombsArray = new JSONArray();
        for (BombDTO p : this.bombs) {
        	bombsArray.put(p.toJson());
        }
        obj.put("bombs", bombsArray);
       
        
        return obj;
    }

}
