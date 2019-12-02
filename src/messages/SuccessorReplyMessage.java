package messages;

public class SuccessorReplyMessage extends Message {
	private int sId;
	
	public SuccessorReplyMessage(int senderId, int sId) {
		super(senderId, Message.MessageType.SUCCESSOR_REPLY);
		this.sId = sId;
	}
	
	public int getSId() {
		return this.sId;
	}
	
}
