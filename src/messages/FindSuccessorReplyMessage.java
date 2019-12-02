package messages;

import java.util.UUID;

public class FindSuccessorReplyMessage extends Message {

	private int fsId;
	
	public FindSuccessorReplyMessage(UUID queryId, int fsId) {
		super(queryId, Message.MessageType.FIND_SUCCESSOR_REPLY);
		this.fsId = fsId;
	}
	
	public int getFsId() {
		return this.fsId;
	}

}
