package messages;

import java.util.UUID;

public class ClosestPrecedingFingerMessage extends Message {
	
	private int targetId;

	public ClosestPrecedingFingerMessage(UUID queryId, int targetId) {
		super(queryId, Message.MessageType.CLOSEST_PRECEDING_FINGER);
		this.targetId = targetId;
	}
	
	public int getTargetId() {
		return this.targetId;
	}

}
