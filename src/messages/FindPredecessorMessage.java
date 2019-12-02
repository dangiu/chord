package messages;

public class FindPredecessorMessage extends Message {

	private int targetId;
	
	public FindPredecessorMessage(int senderId, int targetId) {
		super(senderId, Message.MessageType.FIND_PREDECESSOR);
		this.targetId = targetId;
	}
	
	public int getTargetId() {
		return this.targetId;
	}

}
