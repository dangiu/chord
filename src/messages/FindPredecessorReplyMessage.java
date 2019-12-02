package messages;

public class FindPredecessorReplyMessage extends Message {

	private int fpId;
	
	public FindPredecessorReplyMessage(int senderId, int fpId) {
		super(senderId, Message.MessageType.FIND_PREDECESSOR_REPLY);
		this.fpId = fpId;
	}

	public int getFpId() {
		return this.fpId;
	}
	
}
