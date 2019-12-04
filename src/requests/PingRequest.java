package requests;

import java.util.UUID;

public class PingRequest extends Request {
	
	private int queriedId;

	public PingRequest(double creationTick, UUID queryID, int requesterId, int queriedId) {
		super(creationTick, queryID, requesterId, Request.RequestType.PING);
		this.queriedId = queriedId;
	}
	
	public int getQueriedId() {
		return this.queriedId;
	}

}
