package network.message;

import org.json.JSONObject;

/** Envoyé par un joueur pour lancer la partie ; broadcasté à tous les membres de la room. */
public class LaunchGameMessage implements Message {

    public LaunchGameMessage() {}
    public LaunchGameMessage(JSONObject json) {}

    @Override
    public JSONObject getData() { return new JSONObject(); }

    @Override
    public MessageType getMessageType() { return MessageType.LAUNCH_GAME; }
}
