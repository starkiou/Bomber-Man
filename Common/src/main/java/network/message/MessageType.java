package network.message;

public enum MessageType {
    CONNECTION(1),
    CHAT(2),
    MOVE(3),
    BOMB_PLACE(4),
    GAME_STATE(5),
    GET_ROOM_UPDATE_REQUEST(6),
    ROOM_LIST_UPDATE(7),
	GET_ROOM_LIST_UPDATE(8),
	READY_UPDATE(9),
	ROOM_CREATION(10);

    private final byte id;

    MessageType(int id) {
        this.id = (byte) id;
    }

    public byte getId() {
        return id;
    }

    public static MessageType fromId(byte id) {
        for (MessageType type : values()) {
            if (type.getId() == id) return type;
        }
        throw new IllegalArgumentException("ID de MessageType inconnu : " + id);
    }
}