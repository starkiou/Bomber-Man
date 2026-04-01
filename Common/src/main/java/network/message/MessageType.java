package network.message;

public enum MessageType {
    CONNECTION(1),
    CHAT(2),
    MOVE(3),
    BOMB_PLACE(4),
    GAME_STATE(5),
    ROOM_UPDATE(6),
    ROOM_LIST_UPDATE(7),
	GET_ROOM_LIST_UPDATE(8),
	ROOM_CREATION(9),
	ROOM_JOIN(10),
	REFUSED_ROOM_JOIN(11),
	ACCEPTED_ROOM_JOIN(12),
	ROOM_STATUS_UPDATE(13),
	REFUSED_ROOM_CREATION(14),
	ACCEPTED_ROOM_CREATION(15),
	ROOM_QUIT(16),
	ROOM_CORRECT_QUIT(17), 
	READY_CLIENT(18),
	CLIENT_READY_CORRECT_UPDATE(19);

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