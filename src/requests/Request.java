package requests;

import java.util.UUID;

public abstract class Request {
	enum RequestType {
		FIND_SUCCESSOR,
		FIND_PREDECESSOR,
		JOIN,
		GET_MORE_SUCCESSORS,
		STABILIZE,
		FIX_FINGER
	};
	
	private double creationTick;
	private UUID queryId;
	private int requesterId;
	private RequestType type;
	
	public Request(double creationTick, UUID queryID, int requesterId, RequestType type) {
		this.creationTick = creationTick;
		this.queryId = queryID;
		this.requesterId = requesterId;
		this.type = type;
	}
	
	public double getCreationTick() {
		return this.creationTick;
	}
	
	public UUID getQueryId() {
		return this.queryId;
	}
	
	public int getRequesterId() {
		return this.requesterId;
	}
	
	public RequestType getType() {
		return this.type;
	}
	
}
