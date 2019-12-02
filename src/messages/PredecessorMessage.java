package messages;

public class PredecessorMessage extends Message {

	public PredecessorMessage(int senderId) {
		super(senderId, Message.MessageType.PREDECESSOR);
	}
	
}
