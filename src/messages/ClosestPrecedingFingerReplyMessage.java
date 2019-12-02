package messages;

public class ClosestPrecedingFingerReplyMessage extends Message {
	
	private int cpfId;

	public ClosestPrecedingFingerReplyMessage(int senderId, int cpfId) {
		super(senderId, Message.MessageType.CLOSEST_PRECEDING_FINGER_REPLY);
		this.cpfId = cpfId;
	}

	public int getCpfId() {
		return this.cpfId;
	}
	
}
