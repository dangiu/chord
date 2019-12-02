package messages;

import java.util.UUID;

public class NotifyMessage extends Message {
	
	private int id;

	public NotifyMessage(UUID queryId, int id) {
		super(queryId, Message.MessageType.NOTIFY);
		this.id = id;
	}
	
	public int getId() {
		return this.id;
	}

}
