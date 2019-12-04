package messages;

import java.util.UUID;

public abstract class Message {
	public enum MessageType{
		SUCCESSOR,
		SUCCESSOR_REPLY,
		CLOSEST_PRECEDING_FINGER,
		CLOSEST_PRECEDING_FINGER_REPLY,
		FIND_SUCCESSOR,
		FIND_SUCCESSOR_REPLY,
		FIND_PREDECESSOR,
		FIND_PREDECESSOR_REPLY,
		PREDECESSOR,
		PREDECESSOR_REPLY,
		NOTIFY,
		PING,
		PING_REPLY
		};
	
	private double processingTick;
	private UUID queryId;
	private int senderId;
	private MessageType type;
	
	public Message(UUID queryId, MessageType type) {
		this.queryId = queryId;
		this.type = type;
	}
	
	public void setProcessingTick(double tick) {
		this.processingTick = tick;
	}
	
	public double getProcessingTick() {
		return this.processingTick;
	}
	
	public void setSenderId(int senderId) {
		this.senderId = senderId;
	}
	
	public UUID getQueryId() {
		return this.queryId;
	}
	
	public int getSender() {
		return this.senderId;
	}
	
	public MessageType getType() {
		return this.type;
	}
	
}
