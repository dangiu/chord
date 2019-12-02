package messages;

import java.util.UUID;

public class ClosestPrecedingFingerReplyMessage extends Message {
	
	private int cpfId;

	public ClosestPrecedingFingerReplyMessage(UUID queryId, int cpfId) {
		super(queryId, Message.MessageType.CLOSEST_PRECEDING_FINGER_REPLY);
		this.cpfId = cpfId;
	}

	public int getCpfId() {
		return this.cpfId;
	}
	
}
