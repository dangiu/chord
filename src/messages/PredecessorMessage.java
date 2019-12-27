package messages;

import java.util.UUID;

public class PredecessorMessage extends Message {

	public PredecessorMessage(UUID queryId) {
		super(queryId, Message.MessageType.PREDECESSOR);
	}
	
}
