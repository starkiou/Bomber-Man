package network.message;

import org.json.JSONObject;

public class PlayActionMessage implements Message  {
	private int playerID;
	private ActionType actionType;
	
	
	

	public PlayActionMessage(int playerID, ActionType actionType) {
		this.playerID = playerID;
		this.actionType = actionType;
	}
	
	public PlayActionMessage(JSONObject dataJson) {
		
		this.playerID = dataJson.getInt("playerID");
		
		String actionTexte = dataJson.getString("actionType");
		this.actionType = actionType.valueOf(actionTexte);
	}
	
	public int getPlayerID() {
		return playerID;
	}
	
	public ActionType getActionType() {
		return actionType;
	}

	@Override
	public JSONObject getData() {
		JSONObject obj = new JSONObject();
	    obj.put("playerID", this.playerID);
	    obj.put("actionType", this.actionType.name());
	    return obj;
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.MOVE;
	}

}
