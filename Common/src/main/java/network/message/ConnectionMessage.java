package network.message;

public class ConnectionMessage implements Message {
	
	private String pseudo;
	
	public ConnectionMessage(String pseudo) {
		this.pseudo=pseudo;
	}
	
	public void setPseudo(String pseudo) {
		this.pseudo = pseudo;
	}
	
	public String getPseudo() {
		return this.pseudo;
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.CONNECTION;N
	}

	@Override
	public String getData() {
		return "Pseudo: "+this.getPseudo();
	}

}
