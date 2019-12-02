package messages;

import java.util.UUID;

public class SuccessorMessage extends Message {
	
	public SuccessorMessage(UUID queryId) {
		super(queryId, Message.MessageType.SUCCESSOR);
	}
	
}
