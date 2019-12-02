package messages;

public class FindSuccessorMessage extends Message {

	private int targetId;
	
	public FindSuccessorMessage(int senderId, int targetId) {
		super(senderId, Message.MessageType.FIND_SUCCESSOR);
		this.targetId = targetId;
	}
	
	public int getTargetId() {
		return this.targetId;
	}
	
}
