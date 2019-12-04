package messages;

import java.util.UUID;

public class PredecessorReplyMessage extends Message {
	
	private int pId;

	public PredecessorReplyMessage(UUID queryId, int pId) {
		super(queryId, Message.MessageType.PREDECESSOR_REPLY);
		this.pId = pId;
	}
	
	public int getPId() {
		return this.pId;
	}

}
