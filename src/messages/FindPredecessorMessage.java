package messages;

import java.util.UUID;

public class FindPredecessorMessage extends Message {

	private int targetId;
	
	public FindPredecessorMessage(UUID queryId, int targetId) {
		super(queryId, Message.MessageType.FIND_PREDECESSOR);
		this.targetId = targetId;
	}
	
	public int getTargetId() {
		return this.targetId;
	}

}
