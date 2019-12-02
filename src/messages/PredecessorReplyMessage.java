package messages;

public class PredecessorReplyMessage extends Message {
	
	private int pId;

	public PredecessorReplyMessage(int senderId, int pId) {
		super(senderId, Message.MessageType.PREDECESSOR_REPLY);
	}
	
	public int getPId() {
		return this.pId;
	}

}
