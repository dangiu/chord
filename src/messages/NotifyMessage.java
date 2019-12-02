package messages;

public class NotifyMessage extends Message {
	
	private int id;

	public NotifyMessage(int senderId, int id) {
		super(senderId, Message.MessageType.NOTIFY);
		this.id = id;
	}
	
	public int getId() {
		return this.id;
	}

}
