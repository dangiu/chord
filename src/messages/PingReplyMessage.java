package messages;

import java.util.UUID;

public class PingReplyMessage extends Message {
	public PingReplyMessage(UUID queryId) {
		super(queryId, Message.MessageType.PING_REPLY);
	}
}
