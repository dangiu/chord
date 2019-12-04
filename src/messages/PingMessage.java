package messages;

import java.util.UUID;

public class PingMessage extends Message {

	public PingMessage(UUID queryId) {
		super(queryId, Message.MessageType.PING);
	}

}
