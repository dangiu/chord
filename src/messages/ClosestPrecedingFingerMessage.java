package messages;

public class ClosestPrecedingFingerMessage extends Message {
	
	private int targetId;

	public ClosestPrecedingFingerMessage(int senderId, int targetId) {
		super(senderId, Message.MessageType.CLOSEST_PRECEDING_FINGER);
		this.targetId = targetId;
	}
	
	public int getTargetId() {
		return this.targetId;
	}

}
