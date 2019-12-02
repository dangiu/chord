package messages;

import java.util.UUID;

public class SuccessorReplyMessage extends Message {
	private int sId;
	
	public SuccessorReplyMessage(UUID queryId, int sId) {
		super(queryId, Message.MessageType.SUCCESSOR_REPLY);
		this.sId = sId;
	}
	
	public int getSId() {
		return this.sId;
	}
	
}
