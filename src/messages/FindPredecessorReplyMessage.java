package messages;

import java.util.UUID;

public class FindPredecessorReplyMessage extends Message {

	private int fpId;
	
	public FindPredecessorReplyMessage(UUID queryId, int fpId) {
		super(queryId, Message.MessageType.FIND_PREDECESSOR_REPLY);
		this.fpId = fpId;
	}

	public int getFpId() {
		return this.fpId;
	}
	
}
