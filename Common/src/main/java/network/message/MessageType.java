package network.message;

public enum MessageType {
    CONNECTION(1),
    CHAT(2),
    MOVE(3),
    BOMB_PLACE(4),
    GAME_STATE(5),
    ROOM_UPDATE(6),
    GET_ROOM_UPDATE(7),
    ROOM_LIST_UPDATE(8),
	GET_ROOM_LIST_UPDATE(9),
	READY_UPDATE(10),
	ROOM_CREATION(11),
	ROOM_JOIN(12),
	REFUSED_ROOM_JOIN(13), 
	ROOM_STATUS_UPDATE(14);

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