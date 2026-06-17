package network.message;

import org.json.JSONObject;

/**
 * Base commune aux messages "signal" sans charge utile (corps JSON vide).
 * Les sous-classes ne fournissent que leur {@link MessageType}.
 */
public abstract class AbstractEmptyMessage implements Message {

    @Override
    public JSONObject getData() {
        return new JSONObject();
    }
}
