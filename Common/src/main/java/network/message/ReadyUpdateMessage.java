package network.message;

import org.json.JSONObject;

/** Envoyé par le client pour mettre à jour son état prêt/pas prêt dans la room. */
public class ReadyUpdateMessage implements Message {
    private boolean ready;

    public ReadyUpdateMessage(boolean ready) {
        this.ready = ready;
    }

    public ReadyUpdateMessage(JSONObject json) {
        this.ready = json.getBoolean("ready");
    }

    @Override
    public JSONObject getData() {
        JSONObject obj = new JSONObject();
        obj.put("ready", ready);
        return obj;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.READY_UPDATE;
    }

    public boolean isReady() { return ready; }
}
