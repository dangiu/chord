package messages;

public class FindSuccessorReplyMessage extends Message {

	private int fsId;
	
	public FindSuccessorReplyMessage(int senderId, int fsId) {
		super(senderId, Message.MessageType.FIND_SUCCESSOR_REPLY);
		this.fsId = fsId;
	}
	
	public int getFsId() {
		return this.fsId;
	}

}
