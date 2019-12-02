package messages;

public class SuccessorMessage extends Message {
	
	public SuccessorMessage(int senderId) {
		super(senderId, Message.MessageType.SUCCESSOR);
	}
	
}
