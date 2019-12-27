package messages;

import java.util.UUID;

public class SuccessorReplyMessage extends Message {
	private int[] successors;
	
	public SuccessorReplyMessage(UUID queryId, int[] successors) {
		super(queryId, Message.MessageType.SUCCESSOR_REPLY);
		this.successors = successors.clone();
	}
	
	public int getSId() {
		return this.successors[0];
	}
	
	public int[] getSuccessors() {
		return this.successors.clone();
	}
	
}
