package messages;

import java.util.UUID;

public class FindSuccessorMessage extends Message {

	private int targetId;
	
	public FindSuccessorMessage(UUID queryId, int targetId) {
		super(queryId, Message.MessageType.FIND_SUCCESSOR);
		this.targetId = targetId;
	}
	
	public int getTargetId() {
		return this.targetId;
	}
	
}
