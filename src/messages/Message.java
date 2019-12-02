package messages;

import java.util.UUID;

public abstract class Message {
	enum MessageType{
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
		NOTIFY
		};
	
	private double processingTick;
	private UUID queryId;
	private int senderId;
	private MessageType type;
	
	public Message(int senderId, MessageType type) {
		this.senderId = senderId;
		this.type = type;
		this.queryId = UUID.randomUUID();
	}
	
	public void setProcessingTick(double tick) {
		this.processingTick = tick;
	}
	
	public double getProcessingTick() {
		return this.processingTick;
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
